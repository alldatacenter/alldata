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

package com.netease.arctic.flink;

import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;

import java.io.Serializable;

public abstract class DynamicTableSourceTestBase implements ScanTableSource,
    SupportsWatermarkPushDown, Serializable {

  public static final long serialVersionUID = 1L;
  private WatermarkStrategy<RowData> watermarkStrategy;

  @Override
  public ChangelogMode getChangelogMode() {
    return ChangelogMode.newBuilder()
        .addContainedKind(RowKind.DELETE)
        .addContainedKind(RowKind.INSERT)
        .addContainedKind(RowKind.UPDATE_BEFORE)
        .addContainedKind(RowKind.UPDATE_AFTER)
        .build();
  }

  @Override
  public ScanTableSource.ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {
    init();
    return SourceFunctionProvider.of(
        new SourceFunction<RowData>() {
          @Override
          public void run(SourceContext<RowData> ctx) {
            WatermarkGenerator<RowData> generator =
                watermarkStrategy.createWatermarkGenerator(() -> null);
            WatermarkOutput output = new TestWatermarkOutput(ctx);
            doRun(generator, output, ctx);
          }

          @Override
          public void cancel() {
          }
        },
        false);
  }

  @Override
  public String asSummaryString() {
    return this.getClass().getSimpleName();
  }

  public void init() {
  }

  public abstract void doRun(WatermarkGenerator<RowData> generator, WatermarkOutput output,
                             SourceFunction.SourceContext<RowData> ctx);

  @Override
  public void applyWatermark(WatermarkStrategy<RowData> watermarkStrategy) {
    this.watermarkStrategy = watermarkStrategy;
  }

  public class TestWatermarkOutput implements WatermarkOutput, Serializable {
    public static final long serialVersionUID = 1L;
    public SourceFunction.SourceContext<RowData> ctx;

    public TestWatermarkOutput(SourceFunction.SourceContext<RowData> ctx) {
      this.ctx = ctx;
    }

    @Override
    public void emitWatermark(Watermark watermark) {
      ctx.emitWatermark(
          new org.apache.flink.streaming.api.watermark.Watermark(
              watermark.getTimestamp()));
    }

    @Override
    public void markIdle() {
    }
  }
}
