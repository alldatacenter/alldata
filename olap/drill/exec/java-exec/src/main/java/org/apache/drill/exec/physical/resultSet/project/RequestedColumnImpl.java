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
package org.apache.drill.exec.physical.resultSet.project;

import org.apache.drill.common.expression.PathSegment.NameSegment;

/**
 * Represents one name element. Like a {@link NameSegment}, except that this
 * version is an aggregate. If the projection list contains `a.b` and `a.c`,
 * then one name segment exists for a, and contains segments for both b and c.
 */
public class RequestedColumnImpl extends BaseRequestedColumn implements QualifierContainer {

  private int refCount = 1;
  private Qualifier qualifier;

  public RequestedColumnImpl(RequestedTuple parent, String name) {
    super(parent, name);
  }

  protected void bumpRefCount() { refCount++; }

  public int refCount() { return refCount; }

  @Override
  public Qualifier qualifier() { return qualifier; }

  @Override
  public Qualifier requireQualifier() {
    if (qualifier == null) {
      qualifier = new Qualifier();
    }
    return qualifier;
  }

  @Override
  public boolean isWildcard() { return false; }

  @Override
  public boolean isSimple() { return qualifier == null; }

  @Override
  public boolean isTuple() {
    return qualifier != null && qualifier.isTuple();
  }

  @Override
  public RequestedTuple tuple() {
    if (!isTuple()) {
      return ImpliedTupleRequest.ALL_MEMBERS;
    }
    return qualifier.tuple();
  }

  @Override
  public boolean isArray() {
    return qualifier != null && qualifier.isArray();
  }

  @Override
  public boolean hasIndexes() {
    return qualifier != null && qualifier.hasIndexes();
  }

  @Override
  public boolean hasIndex(int index) {
    return qualifier != null && qualifier.hasIndex(index);
  }

  @Override
  public int maxIndex() {
    return qualifier == null ? 0 : qualifier.maxIndex();
  }

  @Override
  public boolean[] indexes() {
    return qualifier == null ? null : qualifier.indexArray();
  }

  /**
   * Convert the projection to a string of the form:
   * {@code a[0,1,4]['*']{b, c d}}.
   * The information here s insufficient to specify a type,
   * it only specifies a pattern to which types are compatible.
   */
  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder()
        .append(name());
    if (qualifier != null) {
      buf.append(qualifier.toString());
    }
    return buf.toString();
  }

  @Override
  public int arrayDims() {
    return qualifier == null ? 0 : qualifier.arrayDims();
  }
}
