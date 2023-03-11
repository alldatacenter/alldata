/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.flink.write.FlinkSink;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.ProviderContext;
import org.apache.flink.table.connector.sink.DataStreamSinkProvider;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.abilities.SupportsOverwrite;
import org.apache.flink.table.connector.sink.abilities.SupportsPartitioning;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * Flink table api that generates sink operators.
 */
public class ArcticDynamicSink implements DynamicTableSink, SupportsPartitioning, SupportsOverwrite {

  public static final Logger LOG = LoggerFactory.getLogger(ArcticDynamicSink.class);

  private final ArcticTableLoader tableLoader;
  private final CatalogTable flinkTable;
  private final boolean primaryKeyExisted;
  private boolean overwrite = false;

  ArcticDynamicSink(
      CatalogTable flinkTable,
      ArcticTableLoader tableLoader,
      boolean primaryKeyExisted) {
    this.tableLoader = tableLoader;
    this.flinkTable = flinkTable;
    this.primaryKeyExisted = primaryKeyExisted;
  }

  @Override
  public ChangelogMode getChangelogMode(ChangelogMode changelogMode) {
    ChangelogMode.Builder builder = ChangelogMode.newBuilder().addContainedKind(RowKind.INSERT);
    if (primaryKeyExisted) {
      builder.addContainedKind(RowKind.UPDATE_BEFORE)
          .addContainedKind(RowKind.UPDATE_AFTER)
          .addContainedKind(RowKind.DELETE);
    }
    return builder.build();
  }

  @Override
  public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
    ArcticTable table = ArcticUtils.loadArcticTable(tableLoader);

    return new DataStreamSinkProvider() {
      @Override
      public DataStreamSink<?> consumeDataStream(
          ProviderContext providerContext, DataStream<RowData> dataStream) {
        DataStreamSink<?> ds = FlinkSink
            .forRowData(dataStream)
            .context(providerContext)
            .table(table)
            .flinkSchema(flinkTable.getSchema())
            .tableLoader(tableLoader)
            .overwrite(overwrite)
            .build();
        UserGroupInformation.reset();
        LOG.info("ugi reset");
        return ds;
      }
    };
  }

  @Override
  public DynamicTableSink copy() {
    return this;
  }

  @Override
  public String asSummaryString() {
    return "arctic";
  }

  @Override
  public void applyStaticPartition(Map<String, String> map) {
    //ignore
  }

  @Override
  public void applyOverwrite(boolean newOverwrite) {
    this.overwrite = newOverwrite;
  }

}