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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FtpHandler implements IFtpHandler {
  private FTPClient ftpClient = null;
  private static final String SP = "/";

  public FTPClient getFtpClient() {
    return ftpClient;
  }

  @Override
  public void loginFtpServer(FtpConfig ftpConfig) {
    ftpClient = new FTPClient();
    try {
      ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
      ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
      ftpClient.setConnectTimeout(ftpConfig.getTimeout());
      ftpClient.setDataTimeout(ftpConfig.getTimeout());
      if (ftpConfig.getConnectPattern() == FtpConfig.ConnectPattern.PASV) {
        ftpClient.enterRemotePassiveMode();
        ftpClient.enterLocalPassiveMode();
      } else if (ftpConfig.getConnectPattern() == FtpConfig.ConnectPattern.PORT) {
        ftpClient.enterLocalActiveMode();
      } else {
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONFIG_ERROR,
              String.format("Unsupported connect pattern %s, ftp server only support pasv or port connect pattern.", ftpConfig.getConnectPattern()));
      }
      int reply = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        ftpClient.disconnect();
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
                String.format("Connect ftp server failed, please check username and password is correct [message:host = %s,username = %s,port = %s].",
                        ftpConfig.getHost(), ftpConfig.getUsername(), ftpConfig.getPort()));
      }
      String fileEncoding = System.getProperty("file.encoding");
      ftpClient.setControlEncoding(fileEncoding);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public void logoutFtpServer() throws IOException {
    if (ftpClient.isConnected()) {
      try {
        ftpClient.logout();
      } finally {
        if (ftpClient.isConnected()) {
          ftpClient.disconnect();
        }
      }
    } else {
      log.warn("Log out ftp server failed due to ftp client is disconnect.");
    }
  }

  @Override
  public boolean isDirExist(String directoryPath) {
    String originDir = null;
    try {
      originDir = ftpClient.printWorkingDirectory();
      return ftpClient.changeWorkingDirectory(new String(directoryPath.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
    } catch (IOException e) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, String.format("Check ftp directory %s is existed failed", directoryPath), e);
    } finally {
      if (originDir != null) {
        try {
          ftpClient.changeWorkingDirectory(originDir);
        } catch (IOException e) {
          log.error(e.getMessage());
        }
      }
    }
  }

  @Override
  public boolean isFileExist(String filePath) {
    boolean isExitFlag = false;
    try {
      FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
      if (ftpFiles.length == 1 && ftpFiles[0].isFile()) {
        isExitFlag = true;
      }
    } catch (IOException e) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, String.format("Check ftp file %s is existed failed", filePath), e);
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
  public List<String> getFiles(String path) {
    List<String> sources = new ArrayList<>();
    if (isDirExist(path)) {
      if (!path.endsWith(SP)) {
        path = path + SP;
      }
      try {
        FTPFile[] ftpFiles = ftpClient.listFiles(new String(path.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
        if (ftpFiles != null) {
          for (FTPFile ftpFile : ftpFiles) {
            sources.addAll(getFiles(path + ftpFile.getName()));
          }
        }
      } catch (IOException e) {
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, String.format("Get Ftp files failed due to:", e.getMessage()), e);
      }
    } else if (isFileExist(path)) {
      sources.add(path);
    }
    return sources;
  }

  @Override
  public long getFilesSize(String path) {
    long ftpFileSize = 0;
    try {
      if (isDirExist(path)) {
        if (!path.endsWith(SP)) {
          path = path + SP;
        }
        FTPFile[] ftpFiles = ftpClient.listFiles(new String(path.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
        if (ftpFiles != null) {
          for (FTPFile ftpFile : ftpFiles) {
            ftpFileSize += getFilesSize(path + ftpFile.getName());
          }
        }
      } else if (isFileExist(path)) {
        FTPFile[] ftpFiles = ftpClient.listFiles(new String(path.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
        ftpFileSize = ftpFiles[0].getSize();
      }
    } catch (IOException e) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR, String.format("Get ftp files size failed due to:", e.getMessage()), e);
    }
    return ftpFileSize;
  }

  @Override
  public InputStream getInputStream(String filePath) {
    try {
      InputStream is = ftpClient.retrieveFileStream(new String(filePath.getBytes(StandardCharsets.UTF_8), FTP.DEFAULT_CONTROL_ENCODING));
      return is;
    } catch (IOException e) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.CONNECTION_ERROR,
              String.format("Read file:[%s] failed, please check file exist and user has permission to read", filePath), e);
    }
  }
}
