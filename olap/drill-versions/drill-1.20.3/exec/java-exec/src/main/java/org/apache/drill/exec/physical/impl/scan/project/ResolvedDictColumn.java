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
package org.apache.drill.exec.physical.impl.scan.project;

import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedDict;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedSingleDict;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedDictArray;
import org.apache.drill.exec.record.MaterializedField;

public class ResolvedDictColumn extends ResolvedColumn {

  private final MaterializedField schema;
  private final ResolvedTuple parent;
  private final ResolvedDict members;

  public ResolvedDictColumn(ResolvedTuple parent, String name) {
    super(parent, -1);
    schema = MaterializedField.create(name,
        Types.required(MinorType.DICT));
    this.parent = parent;
    members = new ResolvedSingleDict(this);
    parent.addChild(members);
  }

  public ResolvedDictColumn(ResolvedTuple parent,
                           MaterializedField schema, int sourceIndex) {
    super(parent, sourceIndex);
    this.schema = schema;
    this.parent = parent;

    assert schema.getType().getMinorType() == MinorType.DICT;
    if (schema.getType().getMode() == DataMode.REPEATED) {
      members = new ResolvedDictArray(this);
    } else {
      members = new ResolvedSingleDict(this);
    }
    parent.addChild(members);
  }

  public ResolvedTuple parent() {
    return parent;
  }

  @Override
  public String name() {
    return schema.getName();
  }

  public ResolvedTuple members() {
    return members;
  }

  @Override
  public void project(ResolvedTuple dest) {
    dest.addVector(members.buildVector());
  }

  @Override
  public MaterializedField schema() {
    return schema;
  }
}
