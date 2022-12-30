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
import com.bytedance.bitsail.connector.legacy.hudi.utils.TestConfigurations;
import com.bytedance.bitsail.connector.legacy.hudi.utils.TestData;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;

import org.apache.flink.configuration.Configuration;
import org.apache.hudi.common.model.HoodieTableType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HudiCompactionITCase extends TestWriteBase {

  private static final Logger LOG = LoggerFactory.getLogger(HudiCompactionITCase.class);

  protected Configuration conf;

  @TempDir
  public File tempFile;

  private static final String WRITER_PREFIX = "job.writer.";
  private static final String READER_PREFIX = "job.reader.";

  @BeforeEach
  public void before() {
    conf = TestConfigurations.getDefaultConf(tempFile.getAbsolutePath());
    conf.setString(FlinkOptions.TABLE_TYPE, HoodieTableType.MERGE_ON_READ.name());
    conf.setBoolean(FlinkOptions.COMPACTION_ASYNC_ENABLED, false);
    conf.setBoolean(FlinkOptions.COMPACTION_SCHEDULE_ENABLED, true);
    conf.setInteger(FlinkOptions.COMPACTION_DELTA_COMMITS, 1);
  }

  private void setCompactionConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(CommonOptions.JOB_TYPE, "BATCH");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);

    // set reader
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiCompactSourceDAGBuilder");
    jobConfiguration.set(READER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());

    // set writer
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiCompactSinkDAGBuilder");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.TABLE_NAME.key(), "TestHoodieTable");
  }

  @Test
  public void testCompaction() throws Exception {
    // write test data and schedule a compaction
    TestHarness.instance().preparePipeline(tempFile, conf)
        .consume(TestData.DATA_SET_INSERT)
        .assertEmptyDataFiles()
        .checkpoint(1)
        .assertNextEvent()
        .checkpointComplete(1)
        // upsert another data buffer
        .consume(TestData.DATA_SET_UPDATE_INSERT)
        // the data is not flushed yet
        .checkWrittenData(EXPECTED1)
        .checkpoint(2)
        .assertNextEvent()
        .checkpointComplete(2)
        .checkWrittenData(EXPECTED2)
        .end();

    // run BitSail job to execute the compaction
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    setCompactionConfiguration(jobConf);
    EmbeddedFlinkCluster.submitJob(jobConf);

    // delta_commit -> compaction -> delta_commit
    // check data written as expected
    TestHarness.instance().preparePipeline(tempFile, conf)
        .checkWrittenDataCow(tempFile, EXPECTED1, 4)
        .checkWrittenDataMor(tempFile, EXPECTED2, 4);
  }
}
