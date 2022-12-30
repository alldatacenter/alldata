/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.hudi;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.sink.utils.TestWriteBase;
import com.bytedance.bitsail.connector.legacy.hudi.util.AvroSchemaConverter;
import com.bytedance.bitsail.connector.legacy.hudi.utils.TestConfigurations;
import com.bytedance.bitsail.connector.legacy.hudi.utils.TestData;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;

import org.apache.flink.configuration.Configuration;
import org.apache.hudi.common.model.HoodieTableType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

public class HudiSource2PrintITCase extends TestWriteBase {

  protected Configuration conf;

  @TempDir
  public File tempFile;

  private static final String READER_PREFIX = "job.reader.";

  @BeforeEach
  public void before() {
    conf = TestConfigurations.getDefaultConf(tempFile.getAbsolutePath());
    conf.removeConfig(FlinkOptions.SOURCE_AVRO_SCHEMA_PATH);
    conf.setString(FlinkOptions.SOURCE_AVRO_SCHEMA,
        AvroSchemaConverter.convertToSchema(TestConfigurations.ROW_TYPE_SIMPLE).toString());
  }

  private void setCOWConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(CommonOptions.JOB_TYPE, "BATCH");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
    jobConfiguration.set(CommonOptions.JOB_ID, -1L);
    jobConfiguration.set(CommonOptions.INSTANCE_ID, -1L);

    // set reader
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiSourceFunctionDAGBuilder");
    jobConfiguration.set(READER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());
    jobConfiguration.set(READER_PREFIX + FlinkOptions.TABLE_TYPE.key(), FlinkOptions.TABLE_TYPE_COPY_ON_WRITE);
    jobConfiguration.set(READER_PREFIX + FlinkOptions.QUERY_TYPE.key(), FlinkOptions.QUERY_TYPE_READ_OPTIMIZED);

    // set writer
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.print.sink" +
        ".PrintSink");
  }

  private void setMORConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(CommonOptions.JOB_TYPE, "BATCH");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
    jobConfiguration.set(CommonOptions.JOB_ID, -1L);
    jobConfiguration.set(CommonOptions.INSTANCE_ID, -1L);

    // set reader
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiSourceFunctionDAGBuilder");
    jobConfiguration.set(READER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());
    jobConfiguration.set(READER_PREFIX + FlinkOptions.TABLE_TYPE.key(), FlinkOptions.TABLE_TYPE_MERGE_ON_READ);

    // set writer
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.print.sink" +
        ".PrintSink");
  }

  @Test
  public void testHudiCOWSource() throws Exception {
    // write test data and schedule a compaction
    conf.setString(FlinkOptions.TABLE_TYPE, HoodieTableType.COPY_ON_WRITE.name());
    TestWriteBase.TestHarness.instance().preparePipeline(tempFile, conf)
        .consume(TestData.DATA_SET_INSERT_SIMPLE)
        .assertEmptyDataFiles()
        .checkpoint(1)
        .assertNextEvent()
        .checkpointComplete(1)
        // upsert another data buffer
        .consume(TestData.DATA_SET_UPDATE_INSERT_SIMPLE)
        // the data is not flushed yet
        .checkWrittenData(EXPECTED1)
        .checkpoint(2)
        .assertNextEvent()
        .checkpointComplete(2)
        .checkWrittenData(EXPECTED2)
        .end();

    // run BitSail job to load the read optimize view
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    setCOWConfiguration(jobConf);
    EmbeddedFlinkCluster.submitJob(jobConf);
  }

  @Test
  public void testHudiMORSource() throws Exception {
    // write the first commit with compaction
    conf.setString(FlinkOptions.TABLE_TYPE, HoodieTableType.MERGE_ON_READ.name());
    conf.setBoolean(FlinkOptions.COMPACTION_ASYNC_ENABLED, true);
    conf.setBoolean(FlinkOptions.COMPACTION_SCHEDULE_ENABLED, true);
    conf.setInteger(FlinkOptions.COMPACTION_DELTA_COMMITS, 1);

    TestWriteBase.TestHarness.instance().preparePipeline(tempFile, conf)
        .consume(TestData.DATA_SET_INSERT_SIMPLE)
        .assertEmptyDataFiles()
        .checkpoint(1)
        .assertNextEvent()
        .checkpointComplete(1)
        .checkWrittenData(EXPECTED1)
        .end();

    // write the second commit without compaction
    conf.setBoolean(FlinkOptions.COMPACTION_ASYNC_ENABLED, false);
    TestWriteBase.TestHarness.instance().preparePipeline(tempFile, conf)
        .consume(TestData.DATA_SET_UPDATE_INSERT_SIMPLE)
        .checkpoint(2)
        .assertNextEvent()
        .checkpointComplete(2)
        .checkWrittenData(EXPECTED2)
        .end();

    // load read optimize view has 1 commit with 8 record
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    setMORConfiguration(jobConf);
    jobConf.set(READER_PREFIX + FlinkOptions.QUERY_TYPE.key(), FlinkOptions.QUERY_TYPE_READ_OPTIMIZED);
    EmbeddedFlinkCluster.submitJob(jobConf);

    // load snapshot view has 2 commits with 11 record
    jobConf = BitSailConfiguration.newDefault();
    setMORConfiguration(jobConf);
    jobConf.set(READER_PREFIX + FlinkOptions.QUERY_TYPE.key(), FlinkOptions.QUERY_TYPE_SNAPSHOT);
    EmbeddedFlinkCluster.submitJob(jobConf);
    // TODO: add count assertion
  }

}
