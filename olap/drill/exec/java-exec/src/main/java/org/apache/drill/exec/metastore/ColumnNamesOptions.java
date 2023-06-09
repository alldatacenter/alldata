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
package org.apache.drill.exec.metastore;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;

import java.util.StringJoiner;

/**
 * Holds system / session options that are used for obtaining partition / implicit / special column names.
 */
public class ColumnNamesOptions {
  private final String fullyQualifiedName;
  private final String partitionColumnNameLabel;
  private final String rowGroupIndex;
  private final String rowGroupStart;
  private final String rowGroupLength;
  private final String lastModifiedTime;
  private final String projectMetadataColumn;

  public ColumnNamesOptions(OptionManager optionManager) {
    this.fullyQualifiedName = optionManager.getOption(ExecConstants.IMPLICIT_FQN_COLUMN_LABEL_VALIDATOR);
    this.partitionColumnNameLabel = optionManager.getOption(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL_VALIDATOR);
    this.rowGroupIndex = optionManager.getOption(ExecConstants.IMPLICIT_ROW_GROUP_INDEX_COLUMN_LABEL_VALIDATOR);
    this.rowGroupStart = optionManager.getOption(ExecConstants.IMPLICIT_ROW_GROUP_START_COLUMN_LABEL_VALIDATOR);
    this.rowGroupLength = optionManager.getOption(ExecConstants.IMPLICIT_ROW_GROUP_LENGTH_COLUMN_LABEL_VALIDATOR);
    this.lastModifiedTime = optionManager.getOption(ExecConstants.IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL_VALIDATOR);
    this.projectMetadataColumn = optionManager.getOption(ExecConstants.IMPLICIT_PROJECT_METADATA_COLUMN_LABEL_VALIDATOR);
  }

  public String partitionColumnNameLabel() {
    return partitionColumnNameLabel;
  }

  public String fullyQualifiedName() {
    return fullyQualifiedName;
  }

  public String rowGroupIndex() {
    return rowGroupIndex;
  }

  public String rowGroupStart() {
    return rowGroupStart;
  }

  public String rowGroupLength() {
    return rowGroupLength;
  }

  public String lastModifiedTime() {
    return lastModifiedTime;
  }

  public String projectMetadataColumn() {
    return projectMetadataColumn;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ColumnNamesOptions.class.getSimpleName() + "[", "]")
        .add("fullyQualifiedName='" + fullyQualifiedName + "'")
        .add("partitionColumnNameLabel='" + partitionColumnNameLabel + "'")
        .add("rowGroupIndex='" + rowGroupIndex + "'")
        .add("rowGroupStart='" + rowGroupStart + "'")
        .add("rowGroupLength='" + rowGroupLength + "'")
        .add("lastModifiedTime='" + lastModifiedTime + "'")
        .add("projectMetadataColumn='" + projectMetadataColumn + "'")
        .toString();
  }
}
