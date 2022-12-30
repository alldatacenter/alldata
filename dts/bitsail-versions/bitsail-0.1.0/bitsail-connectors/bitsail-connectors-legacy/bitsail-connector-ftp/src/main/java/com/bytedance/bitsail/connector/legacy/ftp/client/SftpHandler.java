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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.connector.legacy.ftp.common.FtpConfig;
import com.bytedance.bitsail.connector.legacy.ftp.error.FtpInputFormatErrorCode;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SftpHandler implements IFtpHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SftpHandler.class);
  private Session session = null;
  private ChannelSftp channelSftp = null;
  private static final String DOT = ".";
  private static final String DOT_DOT = "..";
  private static final String SP = "/";
  private static final String PATH_NOT_EXIST_ERR = "no such file";
  private static final String MSG_AUTH_FAIL = "Auth fail";

  @Override
  public void loginFtpServer(FtpConfig ftpConfig) {
    try {
      JSch jsch = new JSch();

      session = jsch.getSession(ftpConfig.getUsername(), ftpConfig.getHost(), ftpConfig.getPort());
      if (session == null) {
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
                "login failed. Please check if username and password are correct");
      }
      // use password to login, private key is not supported currently
      session.setPassword(ftpConfig.getPassword());

      Properties config = new Properties();
      // skip ssh public key check, may have security concern
      config.put("StrictHostKeyChecking", "no");
      // skip Kerberos authentication
      config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
      session.setConfig(config);
      session.setTimeout(ftpConfig.getTimeout());
      session.connect();

      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();
    } catch (JSchException e) {
      if (null != e.getCause()) {
        String cause = e.getCause().toString();
        String unknownHostException = "java.net.UnknownHostException: " + ftpConfig.getHost();
        String illegalArgumentException = "java.lang.IllegalArgumentException: port out of range:" + ftpConfig.getPort();
        String wrongPort = "java.net.ConnectException: Connection refused";
        if (unknownHostException.equals(cause)) {
          throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
                  String.format("please check sftp server address[%s] is correct, can't connect to sftp server", ftpConfig.getHost()));
        } else if (illegalArgumentException.equals(cause) || wrongPort.equals(cause)) {
          throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
                  String.format("please check sftp server port is correct, incorrect port is %s", ftpConfig.getPort()));
        } else {
          LOG.error(e.getMessage());
          throw new RuntimeException(e);
        }
      } else {
        if (MSG_AUTH_FAIL.equals(e.getMessage())) {
          String message = String.format("Connect sftp server failed, please check username and password is correct [host = %s,username = %s,port = %s].",
                  ftpConfig.getHost(), ftpConfig.getUsername(), ftpConfig.getPort());
          LOG.error(message);
          throw new RuntimeException(message, e);
        } else {
          String message = String.format("Connect sftp server failed,[host = %s,username = %s,port = %s].",
                  ftpConfig.getHost(), ftpConfig.getUsername(), ftpConfig.getPort());
          LOG.error(message);
          throw new RuntimeException(message, e);
        }
      }
    }
  }

  @Override
  public void logoutFtpServer() {
    if (channelSftp != null) {
      channelSftp.disconnect();
    }
    if (session != null) {
      session.disconnect();
    }
  }

  @Override
  public boolean isDirExist(String directoryPath) {
    try {
      SftpATTRS sftpAttrs = channelSftp.lstat(directoryPath);
      return sftpAttrs.isDir();
    } catch (SftpException e) {
      if (e.getMessage().toLowerCase().equals(PATH_NOT_EXIST_ERR)) {
        LOG.warn("{}", e.getMessage());
        return false;
      }
      String message = String.format("Check sftp directory %s is existed failed", directoryPath);
      LOG.error(message);
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, message);
    }
  }

  @Override
  public boolean isFileExist(String filePath) {
    boolean isExitFlag = false;
    try {
      SftpATTRS sftpAttrs = channelSftp.lstat(filePath);
      if (sftpAttrs.getSize() >= 0) {
        isExitFlag = true;
      }
    } catch (SftpException e) {
      if (e.getMessage().toLowerCase().equals(PATH_NOT_EXIST_ERR)) {
        LOG.warn("{}", e.getMessage());
        return false;
      } else {
        String message = "Occurred I/O exception when get file: [%s] attributes, please check connection to sftp server is alive";
        LOG.error(message);
        throw new RuntimeException(message, e);
      }
    }
    return isExitFlag;
  }

  @Override
  public boolean isPathExist(String path) {
    if (isDirExist(path) || isFileExist(path)) {
      return true;
    }
    return false;
  }

  @Override
  public InputStream getInputStream(String filePath) {
    try {
      return channelSftp.get(filePath);
    } catch (SftpException e) {
      String message = String.format("Read file:[%s] failed, please check file exist and user has permission to read", filePath);
      LOG.error(message);
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, message, e);
    }
  }

  @Override
  public List<String> getFiles(String path) {
    if (StringUtils.isBlank(path)) {
      path = SP;
    }
    List<String> sources = new ArrayList<>();
    if (isDirExist(path)) {
      if (path.equals(DOT)) {
        return sources;
      }
      if (!path.endsWith(SP)) {
        path = path + SP;
      }
      try {
        Vector vector = channelSftp.ls(path);
        for (int i = 0; i < vector.size(); ++i) {
          ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) vector.get(i);
          String strName = le.getFilename();
          // skip . and .. from ls result
          if (!strName.equals(DOT) && !strName.equals(DOT_DOT)) {
            String filePath = path + strName;
            sources.addAll(getFiles(filePath));
          }
        }
      } catch (SftpException e) {
        LOG.error("", e);
      }

    } else if (isFileExist(path)) {
      sources.add(path);
      return sources;
    }
    return sources;
  }

  @Override
  public long getFilesSize(String path) {
    long ftpFileSize = 0;
    if (StringUtils.isBlank(path)) {
      path = SP;
    }
    List<String> sources = new ArrayList<>();
    if (isDirExist(path)) {
      if (path.equals(DOT)) {
        return 0;
      }
      if (!path.endsWith(SP)) {
        path = path + SP;
      }
      try {
        Vector vector = channelSftp.ls(path);
        for (int i = 0; i < vector.size(); ++i) {
          ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) vector.get(i);
          String strName = le.getFilename();
          if (!strName.equals(DOT) && !strName.equals(DOT_DOT)) {
            String filePath = path + strName;
            ftpFileSize += getFilesSize(filePath);
          }
        }
      } catch (SftpException e) {
        LOG.error("Get sftp file size failed due to {}", e);
      }

    } else if (isFileExist(path)) {
      try {
        SftpATTRS sftpATTRS = channelSftp.lstat(path);
        ftpFileSize += sftpATTRS.getSize();
      } catch (Exception e) {
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
                String.format("Get sftp file size failed due to %s", e.getMessage()));
      }
    }
    return ftpFileSize;
  }
}
