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

package com.bytedance.bitsail.connector.hbase.auth;

import com.bytedance.bitsail.common.util.Preconditions;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_JAVA_SECURITY_KRB5_CONF;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_KEYTAB_CONTENT;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_KEYTAB_CONTENT_TMP_FILEPATH;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_KRB5_CONTENT;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_KRB5_CONTENT_TMP_FILEPATH;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_PRINCIPAL_FILE;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_USE_BASE64_CONTENT;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_USE_LOCAL_FILE;

/**
 * For storing kerberos content into local files.
 * @author xubo.huster
 */
public class KerberosFileHandler {
  private static final Logger LOG = LoggerFactory.getLogger(KerberosFileHandler.class);
  private static volatile boolean hbaseConfLoaded;

  /**
   * After storing krb5.conf and keytab into local files, we need to adjust hbase configurations.<br/>
   * 1. principalFile: Path of keytab file.<br/>
   * 2. java.security.krb5.conf: Path of krb5.conf file.<br/>
   * 3. useLocalFile: If to use local krb files for authentication.
   */
  public static void adjustHbaseConf(Map<String, Object> hbaseConfigMap) {
    boolean useBase64Content = MapUtils.getBooleanValue(hbaseConfigMap, KEY_USE_BASE64_CONTENT);
    if (useBase64Content) {
      hbaseConfigMap.put(KEY_PRINCIPAL_FILE, KEY_KEYTAB_CONTENT_TMP_FILEPATH);
      hbaseConfigMap.put(KEY_JAVA_SECURITY_KRB5_CONF, KEY_KRB5_CONTENT_TMP_FILEPATH);
      hbaseConfigMap.put(KEY_USE_LOCAL_FILE, true);
    }
  }

  /**
   * Store kerberos authentication content into local files.
   * @param hbaseConfigMap User defined hbase configuration.
   */
  public static void loadConfContent(Map<String, Object> hbaseConfigMap) throws IOException {
    if (hbaseConfLoaded) {
      LOG.info("Hbase kerberos file already loaded.");
    } else {
      handleAuthenticationBase64Content(hbaseConfigMap);
    }
  }

  /**
   * Return the principal file name defined in configuration.
   * @param config HBase configuration.
   * @return Principal file name.
   */
  public static String getPrincipalFileName(Map<String, Object> config) {
    String fileName = MapUtils.getString(config, "principalFile");
    if (org.apache.commons.lang.StringUtils.isEmpty(fileName)) {
      throw new RuntimeException("[principalFile] must be defined!");
    }

    boolean useLocalFile = MapUtils.getBooleanValue(config, KEY_USE_LOCAL_FILE);
    if (useLocalFile) {
      return fileName;
    } else {
      throw new RuntimeException("Only support local file to authenticate.");
    }
  }

  /**
   * Load krb.conf file.
   * @param kerberosConfig HBase configuration.
   */
  public static void loadKrb5Conf(Map<String, Object> kerberosConfig) {
    String krb5FilePath = MapUtils.getString(kerberosConfig, KEY_JAVA_SECURITY_KRB5_CONF);
    if (StringUtils.isEmpty(krb5FilePath)) {
      LOG.info("krb5 file is empty, will use default file");
      return;
    }

    krb5FilePath = loadFile(kerberosConfig, krb5FilePath);
    kerberosConfig.put(KEY_JAVA_SECURITY_KRB5_CONF, krb5FilePath);
  }

  /**
   * kerberosConfig:
   * <p>{
   * "principalFile":"keytab.keytab",
   * "remoteDir":"/home/admin",
   * "sftpConf":{
   * "path" : "/home/admin",
   * "password" : "******",
   * "port" : "22",
   * "auth" : "1",
   * "host" : "127.0.0.1",
   * "username" : "admin"
   * }
   * }</p>
   */
  public static String loadFile(Map<String, Object> kerberosConfig, String filePath) {
    boolean useLocalFile = MapUtils.getBooleanValue(kerberosConfig, KEY_USE_LOCAL_FILE);
    if (useLocalFile) {
      LOG.info("Will use local file:{}", filePath);
      File file = new File(filePath);
      if (file.exists()) {
        if (file.isDirectory()) {
          throw new RuntimeException("The path is a directory:" + filePath);
        }
      } else {
        throw new RuntimeException("The path does not exist:" + filePath);
      }
    } else {
      throw new RuntimeException("Only support local file to authenticate.");
    }
    return filePath;
  }

  /**
   * If KEY_USE_BASE64_CONTENT=true, then store the content of krb5.conf/keytab into local file firstly.
   */
  private static synchronized void handleAuthenticationBase64Content(Map<String, Object> hbaseConfigMap) throws IOException {
    boolean useBase64Content = MapUtils.getBooleanValue(hbaseConfigMap, KEY_USE_BASE64_CONTENT);
    if (!useBase64Content) {
      return;
    }

    // Double locks support singleton mode.
    if (hbaseConfLoaded) {
      LOG.info("Hbase kerberos file already loaded.");
      return;
    }

    String krb5FilePath = KEY_KRB5_CONTENT_TMP_FILEPATH;
    String keytabFilePath = KEY_KEYTAB_CONTENT_TMP_FILEPATH;

    String krb5Conf = MapUtils.getString(hbaseConfigMap, KEY_KRB5_CONTENT);
    String keytabContent = MapUtils.getString(hbaseConfigMap, KEY_KEYTAB_CONTENT);
    if (StringUtils.isEmpty(krb5Conf) || StringUtils.isEmpty(keytabContent)) {
      throw new RuntimeException("Undefined content: [krb5file_content],[keytab_content]");
    }
    LOG.info("krb5.conf file path: " + krb5FilePath + ", keytab file path: " + keytabFilePath);

    // Decode content of krb5.conf/keytab and write them into local files.
    writeBase64ContentToFile(krb5Conf, krb5FilePath);
    writeBase64ContentToFile(keytabContent, keytabFilePath);
    hbaseConfLoaded = true;
  }

  /**
   * Write base64 encoded content into local files.
   * @param base64Content Base64 encoded content.
   * @param filePath Path of file.
   */
  private static void writeBase64ContentToFile(String base64Content, String filePath) throws IOException {
    // Write data to a temporary file to prevent write failure and ensure the correctness of file contents.
    String tmpFilePath = filePath + ".tmp";
    createTmpPath(tmpFilePath);

    try (FileOutputStream fileStream = new FileOutputStream(tmpFilePath)) {
      fileStream.write(Base64.getDecoder().decode(base64Content));
    } catch (IOException e) {
      LOG.error("Fail to write content to " + filePath);
      throw new IOException(e.toString());
    }

    File tmpFile = new File(tmpFilePath);
    File confFile = new File(filePath);
    if (confFile.exists()) {
      confFile.delete();
    }
    tmpFile.renameTo(confFile);
  }

  /**
   * Create temporary files.
   */
  private static void createTmpPath(String filePath) throws IOException {
    File fp = new File(filePath);
    if (!fp.exists()) {
      fp.getParentFile().mkdirs();
      fp.createNewFile();
    }
    Preconditions.checkState(fp.exists(), "Failed to create file:" + filePath);
  }
}

