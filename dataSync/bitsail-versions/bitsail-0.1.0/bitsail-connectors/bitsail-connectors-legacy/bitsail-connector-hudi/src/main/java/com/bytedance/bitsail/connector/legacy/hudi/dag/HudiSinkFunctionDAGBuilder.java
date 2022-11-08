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

package com.bytedance.bitsail.connector.legacy.hudi.dag;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.sink.transform.RowToHoodieFunction;
import com.bytedance.bitsail.connector.legacy.hudi.sink.utils.Pipelines;
import com.bytedance.bitsail.connector.legacy.hudi.util.AvroSchemaConverter;
import com.bytedance.bitsail.connector.legacy.hudi.util.SchemaUtils;
import com.bytedance.bitsail.flink.core.writer.FlinkDataWriterDAGBuilder;

import lombok.Getter;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.hudi.common.model.HoodieRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HudiSinkFunctionDAGBuilder<OUT extends Row> extends FlinkDataWriterDAGBuilder<OUT> {
  private static final Logger LOG = LoggerFactory.getLogger(HudiSinkFunctionDAGBuilder.class);

  @Getter
  private Configuration conf;

  @Getter
  private BitSailConfiguration jobConf;

  /**
   * Flink RowType to convert RowData to HoodieRecord.
   * Caution:
   * RowData itself was indexed but do not have any schema info.
   * Bytedance spark doesn't allow upper case schema.
   * Make sure the sink schema always has lower case.
   */
  @Getter
  private RowType sinkRowType;

  public static Map<String, String> extractHudiProperties(BitSailConfiguration jobConf) {
    Map<String, String> connectorConf = jobConf.getFlattenMap(WriterOptions.WRITER_PREFIX);
    LOG.info("Final properties: {}", connectorConf);
    return connectorConf;
  }

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration writerConfiguration) throws Exception {
    jobConf = execution.getGlobalConfiguration();
    Map<String, String> properties = extractHudiProperties(jobConf);
    conf = FlinkOptions.fromMap(properties);
    sinkRowType = SchemaUtils.getRowTypeFromColumnInfo(jobConf.get(WriterOptions.BaseWriterOptions.COLUMNS));
    conf.setString(FlinkOptions.SOURCE_AVRO_SCHEMA, AvroSchemaConverter.convertToSchema(sinkRowType).toString());
  }

  @Override
  public void addWriter(DataStream<OUT> dataStream, int parallelism) throws Exception {
    long ckpTimeout = dataStream.getExecutionEnvironment()
        .getCheckpointConfig().getCheckpointTimeout();
    conf.setLong(FlinkOptions.WRITE_COMMIT_ACK_TIMEOUT, ckpTimeout);

    DataStream<HoodieRecord> hoodieDataStream = dataStream
        .map(new RowToHoodieFunction<>(conf, jobConf), TypeInformation.of(HoodieRecord.class))
        .setParallelism(parallelism);

    Pipelines.hoodieStreamWrite(conf, parallelism, hoodieDataStream);
  }

  @Override
  public String getWriterName() {
    return "hudi_sink";
  }
}
