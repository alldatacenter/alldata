/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.pig.utils;

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.utils.hdfs.ConfigurationBuilder;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ServiceCheck {
  protected static final Logger LOG = LoggerFactory.getLogger(ServiceCheck.class);

  private final ViewContext viewContext;

  public ServiceCheck(ViewContext viewContext){
    this.viewContext = viewContext;
  }

  public static class Policy {
    private boolean checkHdfs = true;
    private boolean checkHomeDirectory = true;
    private boolean checkWebhcat = true;
    private boolean checkStorage = true;

    public Policy() {
    }

    public Policy(boolean checkHdfs, boolean checkHomeDirectory, boolean checkWebhcat, boolean checkStorage) {
      this.checkHdfs = checkHdfs;
      this.checkHomeDirectory = checkHomeDirectory;
      this.checkWebhcat = checkWebhcat;
      this.checkStorage = checkStorage;
    }

    public boolean isCheckHdfs() {
      return checkHdfs;
    }

    public void setCheckHdfs(boolean checkHdfs) {
      this.checkHdfs = checkHdfs;
    }

    public boolean isCheckHomeDirectory() {
      return checkHomeDirectory;
    }

    public void setCheckHomeDirectory(boolean checkHomeDirectory) {
      this.checkHomeDirectory = checkHomeDirectory;
    }

    public boolean isCheckWebhcat() {
      return checkWebhcat;
    }

    public void setCheckWebhcat(boolean checkWebhcat) {
      this.checkWebhcat = checkWebhcat;
    }

    public boolean isCheckStorage() {
      return checkStorage;
    }

    public void setCheckStorage(boolean checkStorage) {
      this.checkStorage = checkStorage;
    }

    @Override
    public String toString() {
      return "Policy{" +
        "checkHdfs=" + checkHdfs +
        ", checkHomeDirectory=" + checkHomeDirectory +
        ", checkWebhcat=" + checkWebhcat +
        ", checkStorage=" + checkStorage +
        '}';
    }
  }

  public Policy getServiceCheckPolicy() throws HdfsApiException {
    Policy policy = new Policy();
    Optional<Map<String, String>> viewConfigs = ViewPropertyHelper.getViewConfigs(viewContext, Constants.VIEW_CONF_KEYVALUES);
    ConfigurationBuilder configBuilder;
    if(viewConfigs.isPresent()) {
      configBuilder = new ConfigurationBuilder(this.viewContext, viewConfigs.get());
    }else{
      configBuilder = new ConfigurationBuilder(this.viewContext);
    }

    Configuration configurations = configBuilder.buildConfig();
    String defaultFS = configurations.get(Constants.DEFAULT_FS);

    URI fsUri = null;
    try {
      fsUri = new URI(defaultFS);
      String protocol = fsUri.getScheme();
      String ambariSkipCheckValues = viewContext.getAmbariProperty(Constants.AMBARI_SKIP_HOME_DIRECTORY_CHECK_PROTOCOL_LIST);
      List<String> protocolSkipList = (ambariSkipCheckValues == null? new LinkedList<String>() : Arrays.asList(ambariSkipCheckValues.split(",")));
      if(null != protocol && protocolSkipList.contains(protocol)){
        policy.setCheckHomeDirectory(false);
        return policy;
      }
    } catch (URISyntaxException e) {
      LOG.error("Error occurred while parsing the defaultFS URI.", e);
      return policy;
    }

    return policy;
  }
}