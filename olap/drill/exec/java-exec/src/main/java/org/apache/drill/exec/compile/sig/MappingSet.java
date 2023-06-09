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
package org.apache.drill.exec.compile.sig;

import java.util.Arrays;

import org.apache.drill.exec.expr.DirectExpression;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;


public class MappingSet {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MappingSet.class);

  private GeneratorMapping constant;
  private GeneratorMapping[] mappings;
  private int mappingIndex;
  private GeneratorMapping current;
  private DirectExpression readIndex;
  private DirectExpression writeIndex;
  private DirectExpression incoming;
  private DirectExpression outgoing;
  private DirectExpression workspace;
  private DirectExpression workspaceIndex;

  public MappingSet(GeneratorMapping mapping) {
    this("inIndex", "outIndex", new GeneratorMapping[] { mapping, mapping });
  }

  public boolean hasEmbeddedConstant() {
    return constant == current;
  }

  public MappingSet(String readIndex, String writeIndex, String workspaceIndex, String incoming, String outgoing,
      String workspace, GeneratorMapping... mappings) {
    this(readIndex, writeIndex, incoming, outgoing, mappings);
    this.workspaceIndex = DirectExpression.direct(workspaceIndex);
    this.workspace = DirectExpression.direct(workspace);
  }

  public MappingSet(GeneratorMapping... mappings) {
    this("inIndex", "outIndex", mappings);
  }

  public MappingSet(String readIndex, String writeIndex, GeneratorMapping... mappings) {
    this(readIndex, writeIndex, "incoming", "outgoing", mappings);
  }

  public MappingSet(String readIndex, String writeIndex, String incoming, String outgoing, GeneratorMapping... mappings) {
    this.readIndex = DirectExpression.direct(readIndex);
    this.writeIndex = DirectExpression.direct(writeIndex);
    this.incoming = DirectExpression.direct(incoming);
    this.outgoing = DirectExpression.direct(outgoing);
    Preconditions.checkArgument(mappings.length >= 2);
    this.constant = mappings[0];

    // Make sure the constant GM is different from other GM. If it is identical, clone another copy.
    for (int i = 1; i < mappings.length; i++) {
      if (mappings[0] == mappings[i]) {
        this.constant = new GeneratorMapping(mappings[0]);
        break;
      }
    }

    this.mappings = Arrays.copyOfRange(mappings, 1, mappings.length);
    this.current = this.mappings[0];
  }

  public void enterConstant() {
    assert constant != current;
    current = constant;
  }

  public void exitConstant() {
    assert constant == current;
    current = mappings[mappingIndex];
  }

  public boolean isWithinConstant() {
    return constant == current;
  }

  public void enterChild() {
    assert current == mappings[mappingIndex];
    mappingIndex++;
    if (mappingIndex >= mappings.length) {
      throw new IllegalStateException("This generator does not support mappings beyond");
    }
    current = mappings[mappingIndex];
  }

  public void exitChild() {
    assert current == mappings[mappingIndex];
    mappingIndex--;
    if (mappingIndex < 0) {
      throw new IllegalStateException("You tried to traverse higher than the provided mapping provides.");
    }
    current = mappings[mappingIndex];
  }

  public GeneratorMapping getCurrentMapping() {
    return current;
  }

  public DirectExpression getValueWriteIndex() {
    return writeIndex;
  }

  public DirectExpression getValueReadIndex() {
    return readIndex;
  }

  public DirectExpression getOutgoing() {
    return outgoing;
  }

  public DirectExpression getIncoming() {
    return incoming;
  }

  public DirectExpression getWorkspaceIndex() {
    return workspaceIndex;
  }

  public DirectExpression getWorkspace() {
    return workspace;
  }

  public boolean isHashAggMapping() {
    return workspace != null;
  }

}
