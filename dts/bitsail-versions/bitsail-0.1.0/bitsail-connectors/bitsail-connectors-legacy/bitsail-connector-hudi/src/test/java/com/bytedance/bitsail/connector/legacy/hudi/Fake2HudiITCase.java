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

package com.bytedance.bitsail.connector.legacy.hudi;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.fake.option.FakeReaderOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Fake2HudiITCase {
  private static final Logger LOG = LoggerFactory.getLogger(Fake2HudiITCase.class);
  private static final String TEST_SCHEMA =
      "[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"text\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"string\"}]";

  private static final String WRITER_PREFIX = "job.writer.";
  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @TempDir
  File tempFile;

  @BeforeEach
  public void before() {
    environmentVariables.set("HADOOP_USER_NAME", "haoke");
  }

  protected void setStreamingConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(CommonOptions.JOB_TYPE, "batch");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
    jobConfiguration.set(CommonOptions.INSTANCE_ID, 1L);
  }

  protected void setReaderConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.fake.source" +
        ".FakeSource");
    jobConfiguration.set(FakeReaderOptions.USE_BITSAIL_TYPE, false);
    jobConfiguration.set(FakeReaderOptions.TOTAL_COUNT, 300);
    jobConfiguration.set(FakeReaderOptions.RATE, 1000);
    jobConfiguration.set(FakeReaderOptions.RANDOM_NULL_RATE, 0d);
    jobConfiguration.set(ReaderOptions.BaseReaderOptions.COLUMNS, JsonSerializer.parseToList(TEST_SCHEMA, ColumnInfo.class));
  }

  protected void setWriterConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiSinkFunctionDAGBuilder");

    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.RECORD_KEY_FIELD.key(), "id");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.INDEX_KEY_FIELD.key(), "id");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.PRECOMBINE_FIELD.key(), "timestamp");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.TABLE_NAME.key(), "test_table");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.TABLE_TYPE.key(), "MERGE_ON_READ");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.OPERATION.key(), "upsert");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.INDEX_TYPE.key(), "BUCKET");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.BUCKET_INDEX_NUM_BUCKETS.key(), "4");
    jobConfiguration.set(WriterOptions.BaseWriterOptions.COLUMNS, JsonSerializer.parseToList(TEST_SCHEMA, ColumnInfo.class));
  }

  @Test
  public void testFake2Hudi() throws Exception {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    setStreamingConfiguration(jobConf);
    setReaderConfiguration(jobConf);
    setWriterConfiguration(jobConf);
    EmbeddedFlinkCluster.submitJob(jobConf);
  }
}
