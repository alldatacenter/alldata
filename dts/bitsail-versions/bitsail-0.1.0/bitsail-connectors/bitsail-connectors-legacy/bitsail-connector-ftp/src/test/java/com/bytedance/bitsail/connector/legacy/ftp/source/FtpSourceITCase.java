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

package com.bytedance.bitsail.connector.legacy.ftp.source;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.ftp.common.FtpConfig;
import com.bytedance.bitsail.connector.legacy.ftp.option.FtpReaderOptions;
import com.bytedance.bitsail.connector.legacy.ftp.util.SetupUtil;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;

public class FtpSourceITCase {

  protected FakeFtpServer ftpServer;

  @Before
  public void setup() {
    SetupUtil setupUtil = new SetupUtil();
    ftpServer = setupUtil.getFTP();
    ftpServer.start();
  }

  @After
  public void teardown() {
    ftpServer.stop();
  }

  @Test
  public void testFtpSource() throws Exception {
    BitSailConfiguration globalConfiguration = JobConfUtils.fromClasspath("scripts/ftp_to_print.json");
    globalConfiguration.set(FtpReaderOptions.PROTOCOL, FtpConfig.Protocol.FTP.name());
    globalConfiguration.set(FtpReaderOptions.PORT, ftpServer.getServerControlPort());
    EmbeddedFlinkCluster.submitJob(globalConfiguration);
  }

}
