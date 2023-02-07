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
package org.apache.drill.exec.store.easy.json.loader;

import org.apache.drill.exec.store.easy.json.parser.ArrayListener;
import org.apache.drill.exec.vector.accessor.ArrayWriter;

/**
 * Base class for scalar and object arrays. Represents the array
 * behavior of a field.
 */
public class SimpleArrayListener implements ArrayListener {

  @Override
  public void onStart() { }

  @Override
  public void onElementStart() { }

  @Override
  public void onElementEnd() { }

  @Override
  public void onEnd() { }

  public static class StructureArrayListener extends SimpleArrayListener {

    protected final ArrayWriter arrayWriter;

    public StructureArrayListener(ArrayWriter arrayWriter) {
      this.arrayWriter = arrayWriter;
    }

    @Override
    public void onElementEnd() {
      arrayWriter.save();
    }
  }

  public static class ListArrayListener extends StructureArrayListener {

    public ListArrayListener(ArrayWriter listWriter) {
      super(listWriter);
    }

    @Override
    public void onElementStart() {
      // For list, must say that the entry is non-null to
      // record an empty list. {a: null} vs. {a: []}.
      arrayWriter.setNull(false);
    }
  }
}
