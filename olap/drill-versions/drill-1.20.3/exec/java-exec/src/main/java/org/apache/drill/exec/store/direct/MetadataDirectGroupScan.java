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
package org.apache.drill.exec.store.direct;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.store.RecordReader;
import org.apache.hadoop.fs.Path;

import java.util.List;

/**
 * Represents direct scan based on metadata information.
 * For example, for parquet files it can be obtained from parquet footer (total row count)
 * or from parquet metadata files (column counts).
 * Contains reader, statistics and list of scanned files if present.
 */
@JsonTypeName("metadata-direct-scan")
public class MetadataDirectGroupScan extends DirectGroupScan {

  @JsonProperty
  private final Path selectionRoot;
  @JsonProperty
  private final int numFiles;
  @JsonProperty
  private final boolean usedMetadataSummaryFile;
  @JsonProperty
  private final boolean usedMetastore;

  @JsonCreator
  public MetadataDirectGroupScan(@JsonProperty("reader") RecordReader reader,
      @JsonProperty("selectionRoot") Path selectionRoot,
      @JsonProperty("numFiles") int numFiles,
      @JsonProperty("stats") ScanStats stats,
      @JsonProperty("usedMetadataSummaryFile") boolean usedMetadataSummaryFile,
      @JsonProperty("usedMetastore") boolean usedMetastore) {
    super(reader, stats);
    this.selectionRoot = selectionRoot;
    this.numFiles = numFiles;
    this.usedMetadataSummaryFile = usedMetadataSummaryFile;
    this.usedMetastore = usedMetastore;
  }

  @Override
  public Path getSelectionRoot() {
    return selectionRoot;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    assert children == null || children.isEmpty();
    return new MetadataDirectGroupScan(reader, selectionRoot, numFiles, stats, usedMetadataSummaryFile, usedMetastore);
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return this;
  }

  /**
   * <p>
   * Returns string representation of group scan data.
   * Includes selection root, number of files, if metadata summary file was used,
   * such data is present.
   * </p>
   *
   * <p>
   * Example: [selectionRoot = [/tmp/users], numFiles = 1, usedMetadataSummaryFile = false, usedMetastore = true]
   * </p>
   *
   * @return string representation of group scan data
   */
  @Override
  public String getDigest() {
    StringBuilder builder = new StringBuilder();
    if (selectionRoot != null) {
      builder.append("selectionRoot = ").append(selectionRoot).append(", ");
    }
    builder.append("numFiles = ").append(numFiles).append(", ");
    builder.append("usedMetadataSummaryFile = ").append(usedMetadataSummaryFile).append(", ");
    builder.append("usedMetastore = ").append(usedMetastore).append(", ");
    builder.append(super.getDigest());
    return builder.toString();
  }
}
