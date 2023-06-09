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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.exec.metastore.analyze.AnalyzeInfoProvider;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.planner.physical.PlannerSettings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.hadoop.fs.Path;

public abstract class AbstractGroupScan extends AbstractBase implements GroupScan {

  public AbstractGroupScan(String userName) {
    super(userName);
  }

  public AbstractGroupScan(AbstractGroupScan that) {
    super(that);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    return Collections.emptyList();
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E{
    return physicalVisitor.visitGroupScan(this, value);
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    throw new UnsupportedOperationException(String.format(
        "%s does not implement clone(columns) method!", this.getClass().getCanonicalName()));
  }

  @Override
  @JsonIgnore
  public boolean isDistributed() {
    return getMaxParallelizationWidth() > 1;
  }

  @Override
  @JsonIgnore
  public int getMinParallelizationWidth() {
    return 1;
  }

  @Override
  public ScanStats getScanStats(PlannerSettings settings) {
    return getScanStats();
  }

  @Override
  public ScanStats getScanStats(RelMetadataQuery mq) {
    return getScanStats();
  }

  @JsonIgnore
  public ScanStats getScanStats() {
    throw new UnsupportedOperationException("This should be implemented.");
  }

  @Override
  @JsonIgnore
  @Deprecated
  public boolean enforceWidth() {
    return getMinParallelizationWidth() > 1;
  }

  @Override
  @JsonIgnore
  public long getInitialAllocation() {
    return 0;
  }

  @Override
  @JsonIgnore
  public long getMaxAllocation() {
    return 0;
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return false;
  }

  @Override
  @JsonIgnore
  public boolean supportsPartitionFilterPushdown() {
    return false;
  }

  /**
   * By default, throw exception, since group scan does not have exact column value count.
   */
  @Override
  public long getColumnValueCount(SchemaPath column) {
    throw new UnsupportedOperationException(String.format(
        "%s does not have exact column value count!", getClass().getCanonicalName()));
  }

  @Override
  public String getOperatorType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SchemaPath> getColumns() {
    return GroupScan.ALL_COLUMNS;
  }

  @Override
  public List<SchemaPath> getPartitionColumns() {
    return Lists.newArrayList();
  }

  /**
   * Default is not to support limit pushdown.
   */
  @Override
  @JsonIgnore
  public boolean supportsLimitPushdown() {
    return false;
  }

  /**
   * By default, return null to indicate row count based prune is not supported.
   * Each group scan subclass should override, if it supports row count based prune.
   */
  @Override
  @JsonIgnore
  public GroupScan applyLimit(int maxRecords) {
    return null;
  }

  @Override
  public boolean hasFiles() {
    return false;
  }

  @Override
  public Path getSelectionRoot() {
    return null;
  }

  @Override
  public Collection<Path> getFiles() {
    return null;
  }

  @Override
  public DistributionAffinity getDistributionAffinity() {
    return DistributionAffinity.SOFT;
  }

  @Override
  public LogicalExpression getFilter() {
    return null;
  }

  @Override
  public GroupScan applyFilter(LogicalExpression filterExpr, UdfUtilities udfUtilities,
      FunctionImplementationRegistry functionImplementationRegistry, OptionManager optionManager) {
    return null;
  }

  @Override
  public TableMetadataProvider getMetadataProvider() {
    return null;
  }

  @Override
  public TableMetadata getTableMetadata() {
    return null;
  }

  @Override
  public boolean usedMetastore() {
    return false;
  }

  @Override
  public AnalyzeInfoProvider getAnalyzeInfoProvider() {
    return null;
  }

  @Override
  public boolean supportsFilterPushDown() {
    return false;
  }
}
