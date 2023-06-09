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
package org.apache.drill.exec.store.parquet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractParquetRowGroupScan extends AbstractBase implements SubScan {

  protected final List<RowGroupReadEntry> rowGroupReadEntries;
  protected final List<SchemaPath> columns;
  protected final ParquetReaderConfig readerConfig;
  protected final LogicalExpression filter;
  protected final Path selectionRoot;
  protected final TupleMetadata schema;

  protected AbstractParquetRowGroupScan(String userName,
                                        List<RowGroupReadEntry> rowGroupReadEntries,
                                        List<SchemaPath> columns,
                                        ParquetReaderConfig readerConfig,
                                        LogicalExpression filter,
                                        Path selectionRoot,
                                        TupleMetadata schema) {
    super(userName);
    this.rowGroupReadEntries = rowGroupReadEntries;
    this.columns = columns == null ? GroupScan.ALL_COLUMNS : columns;
    this.readerConfig = readerConfig == null ? ParquetReaderConfig.getDefaultInstance() : readerConfig;
    this.filter = filter;
    this.selectionRoot = selectionRoot;
    this.schema = schema;
  }

  @JsonProperty
  public List<RowGroupReadEntry> getRowGroupReadEntries() {
    return rowGroupReadEntries;
  }

  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty("readerConfig")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  // do not serialize reader config if it contains all default values
  public ParquetReaderConfig getReaderConfigForSerialization() {
    return ParquetReaderConfig.getDefaultInstance().equals(readerConfig) ? null : readerConfig;
  }

  @JsonIgnore
  public ParquetReaderConfig getReaderConfig() {
    return readerConfig;
  }

  @JsonProperty
  public LogicalExpression getFilter() {
    return filter;
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @JsonProperty
  public Path getSelectionRoot() {
    return selectionRoot;
  }

  @JsonProperty
  public TupleMetadata getSchema() { return schema; }

  public abstract AbstractParquetRowGroupScan copy(List<SchemaPath> columns);
  @JsonIgnore
  public abstract Configuration getFsConf(RowGroupReadEntry rowGroupReadEntry) throws IOException;
  public abstract boolean supportsFileImplicitColumns();
  @JsonIgnore
  public abstract List<String> getPartitionValues(RowGroupReadEntry rowGroupReadEntry);

}
