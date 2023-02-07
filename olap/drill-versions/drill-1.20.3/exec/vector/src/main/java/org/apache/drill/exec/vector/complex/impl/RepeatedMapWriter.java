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
package org.apache.drill.exec.vector.complex.impl;

import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

public class RepeatedMapWriter extends AbstractRepeatedMapWriter<RepeatedMapVector> {

  public RepeatedMapWriter(RepeatedMapVector container, FieldWriter parent, boolean unionEnabled) {
    super(container, parent, unionEnabled);
  }

  public RepeatedMapWriter(RepeatedMapVector container, FieldWriter parent) {
    this(container, parent, false);
  }

  @Override
  public void start() {
    // update the repeated vector to state that there is (current + 1) objects.

    // Make sure that the current vector can support the end position of this list.
    if (container.getValueCapacity() <= idx()) {
      container.getMutator().setValueCount(idx() + 1);
    }

    RepeatedMapHolder h = new RepeatedMapHolder();
    container.getAccessor().get(idx(), h);
    if (h.start >= h.end) {
      container.getMutator().startNewValue(idx());
    }
    currentChildIndex = container.getMutator().add(idx());
    for (FieldWriter w : fields.values()) {
      w.setPosition(currentChildIndex);
    }
  }

  @Override
  public void end() {
    // noop
  }
}
