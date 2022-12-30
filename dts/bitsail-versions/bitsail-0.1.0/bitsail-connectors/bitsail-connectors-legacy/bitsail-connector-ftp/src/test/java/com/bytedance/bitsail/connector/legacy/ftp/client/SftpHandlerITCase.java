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
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.Collections;

import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.SUCCESS_TAG;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.UPLOAD;

public class SftpHandlerITCase {

  private IFtpHandler sftpHandler;
  private static GenericContainer sftpServer;

  @Before
  public void setup() {
    SetupUtil setupUtil = new SetupUtil();
    sftpServer = setupUtil.getSFTP();
    sftpServer.start();

    FtpConfig config = setupUtil.initCommonConfig(sftpServer.getFirstMappedPort());
    sftpHandler = FtpHandlerFactory.createFtpHandler(FtpConfig.Protocol.SFTP);
    sftpHandler.loginFtpServer(config);
  }

  @After
  public void teardown() throws IOException {
    sftpHandler.logoutFtpServer();
    sftpServer.stop();
  }

  @Test
  public void testGetFiles() {
    Assert.assertEquals(3, sftpHandler.getFiles(UPLOAD).size());
    Assert.assertEquals(Collections.singletonList(UPLOAD + "test1.csv"), sftpHandler.getFiles(UPLOAD + "test1.csv"));
    Assert.assertEquals(Collections.emptyList(), sftpHandler.getFiles(UPLOAD + "badname"));
  }

  @Test
  public void testGetFilesSize() {
    Assert.assertEquals(104L, sftpHandler.getFilesSize(UPLOAD));
    Assert.assertEquals(0L, sftpHandler.getFilesSize(UPLOAD + SUCCESS_TAG));
  }
}
