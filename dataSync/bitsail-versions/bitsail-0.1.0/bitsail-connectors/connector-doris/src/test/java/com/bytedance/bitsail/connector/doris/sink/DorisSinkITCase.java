/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.doris.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.doris.option.DorisWriterOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Use local doris to test.
 */
@Ignore
public class DorisSinkITCase {

  @Test
  public void test() throws Exception {
    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("fake_to_doris.json");
    addDorisInfo(jobConf);
    EmbeddedFlinkCluster.submitJob(jobConf);
  }

  /**
   * Add your doris setting to job configuration.
   * Below codes are just example.
   */
  public void addDorisInfo(BitSailConfiguration jobConf) {
    jobConf.set(DorisWriterOptions.FE_HOSTS, "127.0.0.1:1234");
    jobConf.set(DorisWriterOptions.MYSQL_HOSTS, "127.0.0.1:4321");
    jobConf.set(DorisWriterOptions.USER, "test_user");
    jobConf.set(DorisWriterOptions.PASSWORD, "password");
    jobConf.set(DorisWriterOptions.DB_NAME, "test_db");
    jobConf.set(DorisWriterOptions.TABLE_NAME, "test_table");
    jobConf.set(DorisWriterOptions.TABLE_MODEL, "unique");
  }
}
