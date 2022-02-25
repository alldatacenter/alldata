/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.hdfs;


import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

public class HdfsUtil {
  private final static Logger LOG =
      LoggerFactory.getLogger(HdfsUtil.class);

  /**
   * Write string to file with overwriting
   * @param filePath path to file
   * @param content new content of file
   */
  public static void putStringToFile(final HdfsApi hdfs,final String filePath, final String content) throws HdfsApiException {

    try {
      synchronized (hdfs) {
        hdfs.execute(new PrivilegedExceptionAction<Void>() {
          @Override
          public Void run() throws Exception {
            final FSDataOutputStream stream = hdfs.create(filePath, true);
            stream.write(content.getBytes());
            stream.close();
            return null;
          }
        }, true);
      }
    } catch (IOException e) {
      throw new HdfsApiException("HDFS020 Could not write file " + filePath, e);
    } catch (InterruptedException e) {
      throw new HdfsApiException("HDFS021 Could not write file " + filePath, e);
    }
  }

  /**
   * Read string from file
   * @param filePath path to file
   */
  public static String readFile(HdfsApi hdfs, String filePath) throws HdfsApiException {
    FSDataInputStream stream;
    try {
      stream = hdfs.open(filePath);
      return IOUtils.toString(stream);
    } catch (IOException e) {
      throw new HdfsApiException("HDFS060 Could not read file " + filePath, e);
    } catch (InterruptedException e) {
      throw new HdfsApiException("HDFS061 Could not read file " + filePath, e);
    }
  }


  /**
   * Increment index appended to filename until find first unallocated file
   * @param fullPathAndFilename path to file and prefix for filename
   * @param extension file extension
   * @return if fullPathAndFilename="/tmp/file",extension=".txt" then filename will be like "/tmp/file_42.txt"
   */
  public static String findUnallocatedFileName(HdfsApi hdfs, String fullPathAndFilename, String extension)
      throws HdfsApiException {
    int triesCount = 0;
    String newFilePath;
    boolean isUnallocatedFilenameFound;

    try {
      do {
        newFilePath = String.format(fullPathAndFilename + "%s" + extension, (triesCount == 0) ? "" : "_" + triesCount);
        LOG.debug("Trying to find free filename " + newFilePath);

        isUnallocatedFilenameFound = !hdfs.exists(newFilePath);
        if (isUnallocatedFilenameFound) {
          LOG.debug("File created successfully!");
        }

        triesCount += 1;
        if (triesCount > 1000) {
          throw new HdfsApiException("HDFS100 Can't find unallocated file name " + fullPathAndFilename + "...");
        }
      } while (!isUnallocatedFilenameFound);
    } catch (IOException e) {
      throw new HdfsApiException("HDFS030 Error in creation " + fullPathAndFilename + "...", e);
    } catch (InterruptedException e) {
      throw new HdfsApiException("HDFS031 Error in creation " + fullPathAndFilename + "...", e);
    }

    return newFilePath;
  }

  /**
   * takes any custom properties that a view wants to be included into the config
   * @param context
   * @param customViewProperties
   * @return
   * @throws HdfsApiException
   */
  public static synchronized HdfsApi connectToHDFSApi(ViewContext context, Map<String, String> customViewProperties)
    throws HdfsApiException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(HdfsUtil.class.getClassLoader());
    try {
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(context, customViewProperties);
      return getHdfsApi(context, configurationBuilder);
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  /**
   * Factory of HdfsApi for specific ViewContext
   * @param context ViewContext that contains connection credentials
   * @return HdfsApi object
   */
  public static synchronized HdfsApi connectToHDFSApi(ViewContext context) throws HdfsApiException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(HdfsUtil.class.getClassLoader());
    try {
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(context);
      return getHdfsApi(context, configurationBuilder);
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  private static HdfsApi getHdfsApi(ViewContext context, ConfigurationBuilder configurationBuilder) throws HdfsApiException {
    HdfsApi api = null;
    AuthConfigurationBuilder authConfigurationBuilder = new AuthConfigurationBuilder(context);
    Map<String, String> authParams = authConfigurationBuilder.build();
    configurationBuilder.setAuthParams(authParams);
    try {
      api = new HdfsApi(configurationBuilder, getHdfsUsername(context));
      LOG.info("HdfsApi connected OK");
    } catch (IOException e) {
      LOG.error("exception occurred while creating hdfsApi objcet : {}", e.getMessage(), e);
      String message = "HDFS040 Couldn't open connection to HDFS";
      LOG.error(message);
      throw new HdfsApiException(message, e);
    } catch (InterruptedException e) {
      LOG.error("exception occurred while creating hdfsApi objcet : {}", e.getMessage(), e);
      String message = "HDFS041 Couldn't open connection to HDFS";
      LOG.error(message);
      throw new HdfsApiException(message, e);
    }
    return api;
  }

  /**
   * Returns username for HdfsApi from "webhdfs.username" property if set,
   * if not set then current Ambari username
   * @param context ViewContext
   * @return username
   */
  public static String getHdfsUsername(ViewContext context) {
    String userName = context.getProperties().get("webhdfs.username");
    if (userName == null || userName.compareTo("null") == 0 || userName.compareTo("") == 0) {
      userName = context.getUsername();
    }
    return userName;
  }
}
