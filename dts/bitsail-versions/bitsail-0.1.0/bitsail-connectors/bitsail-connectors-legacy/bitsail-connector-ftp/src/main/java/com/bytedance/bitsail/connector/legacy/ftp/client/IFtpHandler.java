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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IFtpHandler {
  /**
   * login server
   *
   * @param ftpConfig ftp config
   */
  void loginFtpServer(FtpConfig ftpConfig);

  /**
   * logout server
   *
   * @throws IOException logout error
   */
  void logoutFtpServer() throws IOException;

  /**
   * check if directory exist
   *
   * @param directoryPath directory path
   * @return true if exist else false
   */
  boolean isDirExist(String directoryPath);

  /**
   * check if file exist
   *
   * @param filePath file path
   * @return true if exist else false
   */
  boolean isFileExist(String filePath);

  /**
   * check if path exist
   *
   * @param path path
   * @return true if exist else false
   */
  boolean isPathExist(String path);

  /**
   * get file input strem
   *
   * @param filePath file path
   * @return InputStream
   */
  InputStream getInputStream(String filePath);

  /**
   * get files under path
   *
   * @param path path
   * @return list of files
   */
  List<String> getFiles(String path);

  /**
   * find total sizes of file under the path
   *
   * @param path
   * @return total sizes of files
   */
  long getFilesSize(String path);
}
