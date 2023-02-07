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
package org.apache.drill.exec.store.mapr.streams;

import java.io.IOException;
import java.util.List;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatMatcher;
import org.apache.drill.exec.store.mapr.TableFormatPlugin;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class StreamsFormatPlugin extends TableFormatPlugin {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StreamsFormatPlugin.class);
  private StreamsFormatMatcher matcher;

  public StreamsFormatPlugin(String name, DrillbitContext context, Configuration fsConf,
      StoragePluginConfig storageConfig, StreamsFormatPluginConfig formatConfig) {
    super(name, context, fsConf, storageConfig, formatConfig);
    matcher = new StreamsFormatMatcher(this);
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public boolean supportsWrite() {
    return false;
  }

  @Override
  public boolean supportsAutoPartitioning() {
    return false;
  }

  @Override
  public FormatMatcher getMatcher() {
    return matcher;
  }

  @Override
  public AbstractWriter getWriter(PhysicalOperator child, String location,
      List<String> partitionColumns) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection, List<SchemaPath> columns) {
    List<Path> files = selection.getFiles();
    assert (files.size() == 1);
    //TableProperties props = getMaprFS().getTableProperties(new Path(files.get(0)));
    throw UserException.unsupportedError().message("MapR streams can not be querried at this time.").build(logger);
  }

  @Override
  public boolean supportsStatistics() {
    return false;
  }

  @Override
  public DrillStatsTable.TableStatistics readStatistics(FileSystem fs, Path statsTablePath) {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void writeStatistics(DrillStatsTable.TableStatistics statistics, FileSystem fs, Path statsTablePath) {
    throw new UnsupportedOperationException("unimplemented");
  }
}
