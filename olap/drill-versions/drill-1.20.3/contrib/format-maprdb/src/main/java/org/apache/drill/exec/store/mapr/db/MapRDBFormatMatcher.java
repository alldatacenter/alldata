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
package org.apache.drill.exec.store.mapr.db;

import java.io.IOException;

import com.mapr.fs.MapRFileStatus;
import com.mapr.db.index.IndexDesc;
import com.mapr.fs.tables.TableProperties;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.MapRDBIndexDescriptor;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.FormatSelection;

import org.apache.drill.exec.store.mapr.TableFormatMatcher;
import org.apache.drill.exec.store.mapr.TableFormatPlugin;

import org.apache.drill.exec.store.mapr.db.binary.MapRDBBinaryTable;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

public class MapRDBFormatMatcher extends TableFormatMatcher {

  public MapRDBFormatMatcher(TableFormatPlugin plugin) {
    super(plugin);
  }

  @Override
  protected boolean isSupportedTable(MapRFileStatus status) throws IOException {
    return !getFormatPlugin()
        .getMaprFS()
        .getTableBasicAttrs(status.getPath())
        .getIsMarlinTable();
  }


  /**
   * Get an instance of DrillTable for a particular native secondary index
   * @param fs
   * @param selection
   * @param fsPlugin
   * @param storageEngineName
   * @param userName
   * @param secondaryIndexDesc
   * @return
   * @throws IOException
   */
  public DrillTable isReadableIndex(DrillFileSystem fs,
                                    FileSelection selection, FileSystemPlugin fsPlugin,
                                    String storageEngineName, String userName,
                                    IndexDescriptor secondaryIndexDesc) throws IOException {
    FileStatus status = selection.getFirstPath(fs);

    if (!isFileReadable(fs, status)) {
      return null;
    }

    MapRDBFormatPlugin fp = (MapRDBFormatPlugin) getFormatPlugin();
    DrillTable dt = new DynamicDrillTable(fsPlugin,
        storageEngineName,
        userName,
        new FormatSelection(fp.getConfig(),
            selection));

    // TODO:  Create groupScan using index descriptor
    dt.setGroupScan(fp.getGroupScan(userName,
        selection,
        null /* columns */,
        (IndexDesc) ((MapRDBIndexDescriptor) secondaryIndexDesc).getOriginalDesc(),
        null /* metadataProviderManager */));

    return dt;
  }

  @Override
  public DrillTable isReadable(DrillFileSystem fs,
                               FileSelection selection, FileSystemPlugin fsPlugin,
                               String storageEngineName, SchemaConfig schemaConfig) throws IOException {
    if (isFileReadable(fs, selection.getFirstPath(fs))) {
      MapRDBFormatPlugin mapRDBFormatPlugin = (MapRDBFormatPlugin) getFormatPlugin();
      String tableName = mapRDBFormatPlugin.getTableName(selection);
      TableProperties props = mapRDBFormatPlugin.getMaprFS().getTableProperties(new Path(tableName));
      if (props.getAttr().getJson()) {
        return new DynamicDrillTable(fsPlugin, storageEngineName, schemaConfig.getUserName(),
            new FormatSelection(mapRDBFormatPlugin.getConfig(), selection));
      } else {
        FormatSelection formatSelection = new FormatSelection(mapRDBFormatPlugin.getConfig(), selection);
        return new MapRDBBinaryTable(storageEngineName, fsPlugin, mapRDBFormatPlugin, formatSelection);
      }
    }
    return null;
  }

}
