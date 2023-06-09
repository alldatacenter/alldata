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

package org.apache.drill.exec.physical.config;

import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;

@JsonTypeName("rowkey-join")
public class RowKeyJoinPOP extends AbstractBase {

  public static final String OPERATOR_TYPE = "ROWKEY_JOIN";


  private final PhysicalOperator left;
  private final PhysicalOperator right;

  @JsonProperty("subScanForRowKeyJoin")
  private SubScan subScanForRowKeyJoin;

  @JsonCreator
  public RowKeyJoinPOP(
      @JsonProperty("left") PhysicalOperator left,
      @JsonProperty("right") PhysicalOperator right
      ) {
    this.left = left;
    this.right = right;
    this.subScanForRowKeyJoin = null;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitRowKeyJoin(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.size() == 2);
    RowKeyJoinPOP rj = new RowKeyJoinPOP(children.get(0), children.get(1));
    rj.setSubScanForRowKeyJoin(this.getSubScanForRowKeyJoin());
    return rj;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Iterators.forArray(left, right);
  }

  public PhysicalOperator getLeft() {
    return left;
  }

  public PhysicalOperator getRight() {
    return right;
  }

  @JsonProperty("subScanForRowKeyJoin")
  public SubScan getSubScanForRowKeyJoin() {
    return subScanForRowKeyJoin;
  }

  public void setSubScanForRowKeyJoin(SubScan subScan) {
    this.subScanForRowKeyJoin = subScan;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
