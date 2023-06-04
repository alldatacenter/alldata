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

package com.bytedance.bitsail.connector.legacy.ftp.client;

import com.bytedance.bitsail.connector.legacy.ftp.common.FtpConfig;
import com.bytedance.bitsail.connector.legacy.ftp.util.SetupUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;

import java.io.IOException;
import java.util.Collections;

import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.SUCCESS_TAG;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.UPLOAD;

public class FtpHandlerITCase {

  private FakeFtpServer ftpServer;
  private IFtpHandler ftpHandler;

  @Before
  public void setup() {
    SetupUtil setupUtil = new SetupUtil();
    ftpServer = setupUtil.getFTP();
    ftpServer.start();

    FtpConfig config = setupUtil.initCommonConfig(ftpServer.getServerControlPort());
    config.setConnectPattern(FtpConfig.ConnectPattern.valueOf("PORT"));

    ftpHandler = FtpHandlerFactory.createFtpHandler(FtpConfig.Protocol.FTP);
    ftpHandler.loginFtpServer(config);
  }

  @After
  public void teardown() throws IOException {
    ftpHandler.logoutFtpServer();
    ftpServer.stop();
  }

  @Test
  public void getFilesTest() {
    Assert.assertEquals(3, ftpHandler.getFiles(UPLOAD).size());
    Assert.assertEquals(Collections.singletonList(UPLOAD + "test1.csv"), ftpHandler.getFiles(UPLOAD + "test1.csv"));
    Assert.assertEquals(Collections.emptyList(), ftpHandler.getFiles(UPLOAD + "badname"));
  }

  @Test
  public void getFilesSizeTest() {
    Assert.assertEquals(104L, ftpHandler.getFilesSize(UPLOAD));
    Assert.assertEquals(0L, ftpHandler.getFilesSize(UPLOAD + SUCCESS_TAG));
  }
}
