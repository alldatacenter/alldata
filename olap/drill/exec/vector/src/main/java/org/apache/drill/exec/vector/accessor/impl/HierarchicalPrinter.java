/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.vector.accessor.impl;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Prints a complex object structure in a quasi-JSON format for use
 * in debugging. Generally only used in an ad-hoc fashion during
 * debugging sessions; never in production.
 */

public class HierarchicalPrinter implements HierarchicalFormatter {

  public enum State { OBJECT, OBJECT_ATTRIB, ARRAY, OBJECT_ELEMENT }

  private static class ObjState {
    private State state = State.OBJECT;
    private String attrib;
    private int index = -1;
    private int extensions;

    public ObjState(int extensions) {
      this.extensions = extensions;
    }
  }

  private final PrintStream out;
  private Deque<ObjState> stack = new ArrayDeque<ObjState>();
  private int pendingExtensions = 0;
  private ObjState curObject;
  private int level;

  public HierarchicalPrinter() {
    out = System.out;
  }

  @Override
  public void extend() {
    pendingExtensions++;
  }

  @Override
  public HierarchicalFormatter startObject(Object obj) {
    if (curObject != null) {
      stack.push(curObject);
      switch (curObject.state) {
      case OBJECT_ATTRIB:
        startAttrib(curObject.attrib);
        curObject.attrib = null;
        curObject.state = State.OBJECT;
        break;
      case OBJECT:
        startAttrib("missing-attrib");
        curObject.state = State.OBJECT;
        break;
      case OBJECT_ELEMENT:
        startElement(curObject.index);
        curObject.state = State.ARRAY;
        curObject.index = -1;
        break;
      default:
        assert false;
      }
    }

    printObjIdentity(obj);
    out.println(" {");
    level++;
    curObject = new ObjState(pendingExtensions);
    pendingExtensions = 0;
    return this;
  }

  private void printObjIdentity(Object value) {
    out.print(value.getClass().getSimpleName());
    out.print( " (");
    out.print(System.identityHashCode(value) % 1000);
    out.print(")");
  }

  private void startElement(int index) {
    indent();
    out.print("[");
    out.print(index);
    out.print("] = ");
  }

  private void startAttrib(String label) {
    indent();
    out.print(label);
    out.print(" = ");
  }

  @Override
  public HierarchicalFormatter attribute(String label) {
    curObject.attrib = label;
    curObject.state = State.OBJECT_ATTRIB;
    return this;
  }

  @Override
  public HierarchicalFormatter attribute(String label, Object value) {
    attribPrefix();
    startAttrib(label);
    printValue(value);
    out.println();
    return this;
  }

  private void attribPrefix() {
    switch (curObject.state) {
    case OBJECT_ATTRIB:
      startAttrib(curObject.attrib);
      out.println("<Unknown> {}");
      break;
    case OBJECT:
      break;
    default:
      assert false;
    }
  }

  @Override
  public HierarchicalFormatter attributeIdentity(String label, Object obj) {
    attribPrefix();
    startAttrib(label);
    objIdentity(obj);
    out.println();
    return this;
  }

  private void objIdentity(Object obj) {
    if (obj == null) {
      out.print("null");
    } else {
      printObjIdentity(obj);
    }
  }

  private void printValue(Object value) {
    if (value == null) {
      out.print("null");
    } else if (value instanceof String) {
      out.print("\"");
      out.print(value);
      out.print("\"");
    } else {
      out.print(value.toString());
    }
  }

  @Override
  public HierarchicalFormatter endObject() {
    if (level == 0) {
      out.println( "} // Mismatch!");
      return this;
    }
    if (curObject.extensions == 0) {
      level--;
      indent();
      out.println("}");
      if (level == 0) {
        curObject = null;
      } else {
        curObject = stack.pop();
      }
    } else {
      curObject.extensions--;
    }
    return this;
  }

  private void indent() {
    for (int i = 0; i < level; i++) {
      out.print("  ");
    }
  }

  @Override
  public HierarchicalFormatter attributeArray(String label) {
    startAttrib(label);
    out.println("[");
    level++;
    curObject.state = State.ARRAY;
    return this;
  }

  @Override
  public HierarchicalFormatter element(int index, Object value) {
    startElement(index);
    printValue(value);
    out.println();
    return this;
  }

  @Override
  public HierarchicalFormatter element(int index) {
    curObject.index = index;
    curObject.state = State.OBJECT_ELEMENT;
    return this;
  }

  @Override
  public HierarchicalFormatter elementIdentity(int index, Object obj) {
    startElement(index);
    objIdentity(obj);
    out.println();
    return this;
  }

  @Override
  public HierarchicalFormatter endArray() {
    level--;
    indent();
    out.println("]");
    curObject.state = State.OBJECT;
    return this;
  }

}
