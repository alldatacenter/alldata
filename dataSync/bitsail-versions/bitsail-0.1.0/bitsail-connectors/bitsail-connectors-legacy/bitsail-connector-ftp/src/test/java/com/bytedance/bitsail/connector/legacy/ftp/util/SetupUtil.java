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

package com.bytedance.bitsail.connector.legacy.ftp.util;

import com.bytedance.bitsail.connector.legacy.ftp.common.FtpConfig;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import java.io.File;

import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.DEFAULT_TIMEOUT;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.FTP_PORT;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.LOCALHOST;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.PASSWORD;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.SFTP_PORT;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.SUCCESS_TAG;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.UPLOAD;
import static com.bytedance.bitsail.connector.legacy.ftp.util.Constant.USER;

public class SetupUtil {

  public FtpConfig initCommonConfig(int port) {
    FtpConfig config = new FtpConfig();
    config.setHost(LOCALHOST);
    config.setPort(port);
    config.setUsername(USER);
    config.setPassword(PASSWORD);
    config.setTimeout(DEFAULT_TIMEOUT);
    return config;
  }

  public FakeFtpServer getFTP() {
    FakeFtpServer fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.addUserAccount(new UserAccount(USER, PASSWORD, UPLOAD));

    UnixFakeFileSystem fakeFileSystem = new UnixFakeFileSystem();
    fakeFileSystem.add(new DirectoryEntry(UPLOAD));
    fakeFileSystem.add(new FileEntry(UPLOAD + "test1.csv", "c0,c1,c2,c3,c4\n111,aaa,1.1,1.12345,true,2022-11-01"));
    fakeFileSystem.add(new FileEntry(UPLOAD + "test2.csv", "c0,c1,c2,c3,c4\n-111,aaa,-1.1,-1.12345,false,2022-11-01"));
    fakeFileSystem.add(new FileEntry(UPLOAD + SUCCESS_TAG));
    fakeFtpServer.setFileSystem(fakeFileSystem);
    fakeFtpServer.setServerControlPort(FTP_PORT);
    return fakeFtpServer;
  }

  public GenericContainer getSFTP() {
    File testDir = new File("src/test/resources/upload");

    return new GenericContainer("atmoz/sftp")
        .withExposedPorts(SFTP_PORT)
        .withFileSystemBind(testDir.getAbsolutePath(), "/home/" + USER + UPLOAD, BindMode.READ_ONLY)
        .withCommand(USER + ":" + PASSWORD + ":1001");
  }

}
