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
package org.apache.drill.exec.store.mapr;

import java.io.IOException;

import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.FormatMatcher;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.hadoop.fs.FileStatus;

import com.mapr.fs.MapRFileStatus;

public abstract class TableFormatMatcher extends FormatMatcher {

  private final TableFormatPlugin plugin;

  public TableFormatMatcher(TableFormatPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean supportDirectoryReads() {
    return false;
  }

  @Override
  public DrillTable isReadable(DrillFileSystem fs,
      FileSelection selection, FileSystemPlugin fsPlugin,
      String storageEngineName, SchemaConfig schemaConfig) throws IOException {
    FileStatus status = selection.getFirstPath(fs);
    if (!isFileReadable(fs, status)) {
      return null;
    }

    return new DynamicDrillTable(fsPlugin, storageEngineName, schemaConfig.getUserName(),
        new FormatSelection(getFormatPlugin().getConfig(), selection));
  }

  @Override
  public boolean isFileReadable(DrillFileSystem fs, FileStatus status) throws IOException {
    return (status instanceof MapRFileStatus)
        && ((MapRFileStatus) status).isTable()
        && isSupportedTable((MapRFileStatus) status);
  }

  @Override
  public TableFormatPlugin getFormatPlugin() {
    return plugin;
  }

  /**
   * Returns true if the path pointed by the MapRFileStatus is a supported table
   * by this format plugin. The path must point to a MapR table.
   */
  protected abstract boolean isSupportedTable(MapRFileStatus status) throws IOException;
}
