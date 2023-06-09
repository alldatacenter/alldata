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
package org.apache.drill.exec.physical.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.hadoop.fs.Path;

import java.util.List;

/**
 *  The type of scan operator, which allows to scan schemaless tables ({@link org.apache.drill.exec.planner.logical.DynamicDrillTable} with null selection)
 */
@JsonTypeName("schemaless-scan")
public class SchemalessScan extends AbstractFileGroupScan implements SubScan {

  private final Path selectionRoot;

  @JsonCreator
  public SchemalessScan(@JsonProperty("userName") String userName,
                        @JsonProperty("selectionRoot") Path selectionRoot,
                        @JsonProperty("columns") List<SchemaPath> columns) {
    this(userName, selectionRoot);
  }

  public SchemalessScan(@JsonProperty("userName") String userName,
                        @JsonProperty("selectionRoot") Path selectionRoot) {
    super(userName);
    this.selectionRoot = selectionRoot;
  }

  public SchemalessScan(final SchemalessScan that) {
    super(that);
    this.selectionRoot = that.selectionRoot;
  }

  @JsonProperty
  public Path getSelectionRoot() {
    return selectionRoot;
  }

  @Override
  public void applyAssignments(List<CoordinationProtos.DrillbitEndpoint> endpoints) {
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return this;
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public String toString() {
    String pattern = "SchemalessScan [selectionRoot = %s]";
    return String.format(pattern, selectionRoot);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new SchemalessScan(this);
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return this;
  }

  @Override
  public ScanStats getScanStats() {
    return ScanStats.ZERO_RECORD_TABLE;
  }


  @Override
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return false;
  }

  @Override
  public boolean supportsPartitionFilterPushdown() {
    return false;
  }

}
