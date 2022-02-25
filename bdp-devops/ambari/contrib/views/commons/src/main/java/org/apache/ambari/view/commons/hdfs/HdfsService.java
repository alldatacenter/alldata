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

package org.apache.ambari.view.commons.hdfs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.exceptions.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base Hdfs service
 */
public abstract class HdfsService {

  protected static final Logger logger = LoggerFactory.getLogger(HdfsService.class);

  protected final ViewContext context;
  private Map<String, String> customProperties;

  /**
   * Constructor
   * @param context View Context instance
   */
  public HdfsService(ViewContext context) {
    this.context = context;
  }

  public HdfsService(ViewContext context, Map<String, String> customProperties) {
    this.context = context;
    this.customProperties = customProperties;
  }

  /**
   * Wrapper for json mapping of result of Multi Remove Request
   */
  @XmlRootElement
  public static class FileOperationResult {
    public boolean success;
    public String message;
    public List<String> succeeded;
    public List<String> failed;
    public List<String> unprocessed;

    public FileOperationResult(boolean success) {
      this.success = success;
    }

    public FileOperationResult(boolean success, String message) {
      this(success);
      this.message = message;
    }

    public FileOperationResult(boolean success, String message, List<String> succeeded, List<String> failed, List<String> unprocessed) {
      this(success, message);
      this.succeeded = succeeded;
      this.failed = failed;
      this.unprocessed = unprocessed;
    }

  }

  private HdfsApi _api = null;

  /**
   * Ger HdfsApi instance
   * @return HdfsApi business delegate
   */
  public HdfsApi getApi() {
    if (_api == null) {
      try {
        if(this.customProperties != null){
          _api = HdfsUtil.connectToHDFSApi(context, customProperties);
        }else{
          _api = HdfsUtil.connectToHDFSApi(context);
        }
      } catch (Exception ex) {
        logger.error("Exception while connecting to hdfs : {}", ex.getMessage(), ex);
        throw new ServiceFormattedException("HdfsApi connection failed. Check \"webhdfs.url\" property", ex);
      }
    }
    return _api;
  }

  private static Map<String, String> getHdfsAuthParams(ViewContext context) {
    String auth = context.getProperties().get("webhdfs.auth");
    Map<String, String> params = new HashMap<String, String>();
    if (auth == null || auth.isEmpty()) {
      auth = "auth=SIMPLE";
    }
    for(String param : auth.split(";")) {
      String[] keyvalue = param.split("=");
      if (keyvalue.length != 2) {
        logger.error("Can not parse authentication param " + param + " in " + auth);
        continue;
      }
      params.put(keyvalue[0], keyvalue[1]);
    }
    return params;
  }

  /**
   * Get doAs username to use in HDFS
   * @param context View Context instance
   * @return user name
   */
  public String getDoAsUsername(ViewContext context) {
    String username = context.getProperties().get("webhdfs.username");
    if (username == null || username.isEmpty())
      username = context.getUsername();
    return username;
  }

  /**
   * Checks connection to HDFS
   * @param context View Context
   */
  public static void hdfsSmokeTest(ViewContext context) {
    try {
      HdfsApi api = HdfsUtil.connectToHDFSApi(context);
      api.getStatus();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Get proxyuser username to use in HDFS
   * @param context View Context instance
   * @return user name
   */
  public String getRealUsername(ViewContext context) {
    String username = context.getProperties().get("webhdfs.proxyuser");
    if (username == null || username.isEmpty())
      try {
        username = UserGroupInformation.getCurrentUser().getShortUserName();
      } catch (IOException e) {
        throw new ServiceFormattedException("HdfsApi connection failed. Can't get current user", e);
      }
    return username;
  }
}
