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

package org.apache.drill.exec.store.xml;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.vector.accessor.TupleWriter;

import java.util.Objects;

public class XMLMap {

  private final String mapName;
  private final TupleWriter mapWriter;

  public XMLMap (String mapName, TupleWriter mapWriter) {
    this.mapName = mapName;
    this.mapWriter = mapWriter;
  }

  public TupleWriter getMapWriter() {
    return mapWriter;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    XMLMap other = (XMLMap) obj;
    return Objects.equals(mapName, other.mapName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mapName);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("Map Name", mapName)
      .toString();
  }
}
