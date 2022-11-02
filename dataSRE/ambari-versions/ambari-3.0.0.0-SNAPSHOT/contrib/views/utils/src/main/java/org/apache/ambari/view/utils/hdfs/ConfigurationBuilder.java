/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.hdfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builds the Configuration of HDFS based on ViewContext.
 * Supports both directly specified properties and cluster associated
 * properties loading.
 */
public class ConfigurationBuilder {
  protected static final Logger LOG = LoggerFactory.getLogger(ConfigurationBuilder.class);
  public static final String CORE_SITE = "core-site";
  public static final String HDFS_SITE = "hdfs-site";

  public static final String DEFAULT_FS_INSTANCE_PROPERTY = "webhdfs.url";
  public static final String DEFAULT_FS_CLUSTER_PROPERTY = "fs.defaultFS";

  public static final String NAMESERVICES_INSTANCE_PROPERTY = "webhdfs.nameservices";
  public static final String NAMESERVICES_CLUSTER_PROPERTY = "dfs.nameservices";
  public static final String HA_NAMENODES_INSTANCE_PROPERTY = "webhdfs.ha.namenodes.list";

  public static final String HA_NAMENODES_CLUSTER_PROPERTY = "dfs.ha.namenodes.%s";
  public static final String NAMENODE_RPC_NN_INSTANCE_PROPERTY = "webhdfs.ha.namenode.rpc-address.list";
  public static final String NAMENODE_RPC_NN_CLUSTER_PROPERTY = "dfs.namenode.rpc-address.%s.%s";

  public static final String NAMENODE_HTTP_NN_INSTANCE_PROPERTY = "webhdfs.ha.namenode.http-address.list";
  public static final String NAMENODE_HTTP_NN_CLUSTER_PROPERTY = "dfs.namenode.http-address.%s.%s";

  public static final String NAMENODE_HTTPS_NN_INSTANCE_PROPERTY = "webhdfs.ha.namenode.https-address.list";
  public static final String NAMENODE_HTTPS_NN_CLUSTER_PROPERTY = "dfs.namenode.https-address.%s.%s";

  public static final String FAILOVER_PROXY_PROVIDER_INSTANCE_PROPERTY = "webhdfs.client.failover.proxy.provider";
  public static final String FAILOVER_PROXY_PROVIDER_CLUSTER_PROPERTY = "dfs.client.failover.proxy.provider.%s";

  public static final String UMASK_CLUSTER_PROPERTY = "fs.permissions.umask-mode";
  public static final String UMASK_INSTANCE_PROPERTY = "hdfs.umask-mode";

  public static final String DFS_WEBHDFS_ENABLED = "dfs.webhdfs.enabled";
  public static final String DFS_HTTP_POLICY = "dfs.http.policy";
  public static final String DFS_HTTP_POLICY_HTTPS_ONLY = "HTTPS_ONLY";

  public static final String DFS_NAMENODE_HTTP_ADDERSS = "dfs.namenode.http-address";
  public static final String DFS_NAMENODE_HTTPS_ADDERSS = "dfs.namenode.https-address";


  protected Configuration conf = new Configuration();
  private ViewContext context;
  private AuthConfigurationBuilder authParamsBuilder;
  private Map<String, String> authParams;
  private URI defaultFsUri;
  private Map<String, String> customProperties;

  /**
   * Constructor of ConfigurationBuilder based on ViewContext
   * @param context ViewContext
   */
  public ConfigurationBuilder(ViewContext context) {
    this.context = context;
    this.authParamsBuilder = new AuthConfigurationBuilder(context);
  }

  /**
   * takes context and any extra custom properties that needs to be included into config
   * @param context
   * @param customProperties
   */
  public ConfigurationBuilder(ViewContext context, Map<String, String> customProperties) {
    this.context = context;
    this.authParamsBuilder = new AuthConfigurationBuilder(context);
    this.customProperties = customProperties;
  }

  private void parseProperties() throws HdfsApiException {
    String defaultFS = getDefaultFS(context);

    try {

      if (isHAEnabled(defaultFS)) {
        copyHAProperties(defaultFS);

        LOG.info("HA HDFS cluster found.");
      } else {
        if (defaultFS.startsWith("webhdfs://") && !hasPort(defaultFS)) {
          defaultFS = addPortIfMissing(defaultFS);
        }
      }

      defaultFsUri = new URI(defaultFS);

    } catch (URISyntaxException e) {
      throw new HdfsApiException("HDFS060 Invalid " + DEFAULT_FS_INSTANCE_PROPERTY +
        "='" + defaultFS + "' URI", e);
    }

    conf.set("fs.defaultFS", defaultFS);
    LOG.info(String.format("HdfsApi configured to connect to defaultFS='%s'", defaultFS));

    //Exposing KMS configuration for Ambari Files View Instance with a "Local" cluster configuration
    if(context.getCluster() != null) {
      String encryptionKeyProviderUri = getEncryptionKeyProviderUri();
      if(encryptionKeyProviderUri != null) {
        conf.set("dfs.encryption.key.provider.uri", encryptionKeyProviderUri);
      }
    }
  }

  protected String getEncryptionKeyProviderUri() {
    //If KMS is configured, this value will not be empty
    String encryptionKeyProviderUri = getProperty("hdfs-site", "dfs.encryption.key.provider.uri");
    return encryptionKeyProviderUri;
  }

  private String getDefaultFS(ViewContext context) throws HdfsApiException {
    String defaultFS = getProperty(CORE_SITE, DEFAULT_FS_CLUSTER_PROPERTY, DEFAULT_FS_INSTANCE_PROPERTY);

    if (defaultFS == null || defaultFS.isEmpty())
      throw new HdfsApiException("HDFS070 fs.defaultFS is not configured");

    defaultFS = addProtocolIfMissing(defaultFS);

    if (context.getCluster() != null) {
      try {
        URI fsUri = new URI(defaultFS);
        String protocol = fsUri.getScheme();
        String hostWithPort = defaultFS.substring(protocol.length() + 3);

        Boolean webHdfsEnabled = Boolean.valueOf(getProperty(HDFS_SITE, DFS_WEBHDFS_ENABLED));
        Boolean isHttps = DFS_HTTP_POLICY_HTTPS_ONLY.equals(getProperty(HDFS_SITE, DFS_HTTP_POLICY));

        boolean isHA = isHAEnabled(defaultFS);

        if (webHdfsEnabled && isHttps && "hdfs".equals(protocol)) {
          protocol = "swebhdfs";
          String httpAddr = getProperty(HDFS_SITE, DFS_NAMENODE_HTTPS_ADDERSS);
          if (!isHA && httpAddr != null) hostWithPort = httpAddr;
        } else if (webHdfsEnabled && "hdfs".equals(protocol)) {
          protocol = "webhdfs";
          String httpsAddr = getProperty(HDFS_SITE, DFS_NAMENODE_HTTP_ADDERSS);
          if (!isHA) hostWithPort = httpsAddr;
        }

        return protocol + "://" + hostWithPort;
      } catch (URISyntaxException e) {
        throw new HdfsApiException("Invalid URI format." + e.getMessage(), e);
      }
    }
    return defaultFS;
  }

  private String getProperty(String type, String key, String instanceProperty) {
    String value;

    if (context.getCluster() != null) {
      value = getProperty(type, key);
    } else {
      value = getViewProperty(instanceProperty);
    }
    return value;
  }

  private String getViewProperty(String instanceProperty) {
    return context.getProperties().get(instanceProperty);
  }

  private String getProperty(String type, String key) {
    if (context.getCluster() != null) {
      return context.getCluster().getConfigurationValue(type, key);
    }
    return null;
  }

  private void copyPropertyIfExists(String type, String key) {
    String value;

    if (context.getCluster() != null) {
      value = context.getCluster().getConfigurationValue(type, key);
      if (value != null) {
        conf.set(key, value);
        LOG.debug("set " + key + " = " + value);
      } else {
        LOG.debug("No such property " + type + "/" + key);
      }
    } else {
      LOG.debug("No such property " + type + "/" + key);
    }
  }

  private void copyPropertiesBySite(String type) {
    if (context.getCluster() != null) {
      Map<String, String> configs = context.getCluster().getConfigByType(type);
      LOG.debug("configs from core-site : {}", configs);
      copyProperties(configs);
    } else {
      LOG.error("Cannot find cluster.");
    }
  }

  private void copyProperties(Map<String, String> configs) {
    if (null != configs) {
      for(Map.Entry<String, String> entry : configs.entrySet()){
        String key = entry.getKey();
        String value = entry.getValue();
        conf.set(key, value);
      }
    } else {
      LOG.error("configs were null.");
    }
  }

  @VisibleForTesting
  void copyHAProperties(String defaultFS) throws URISyntaxException, HdfsApiException {
    URI uri = new URI(defaultFS);
    String nameservice = uri.getHost();

    copyClusterProperty(NAMESERVICES_CLUSTER_PROPERTY, NAMESERVICES_INSTANCE_PROPERTY);
    String namenodeIDs = copyClusterProperty(String.format(HA_NAMENODES_CLUSTER_PROPERTY, nameservice),
      HA_NAMENODES_INSTANCE_PROPERTY);

    String[] namenodes = namenodeIDs.split(",");
    //    get the property values from cluster.
    //    If not found then get the values from view instance property.

    List<String> rpcAddresses = new ArrayList<>(namenodes.length);
    List<String> httpAddresses = new ArrayList<>(namenodes.length);
    List<String> httpsAddresses = new ArrayList<>(namenodes.length);
    for (String namenode : namenodes) {

      String rpcAddress = getProperty(HDFS_SITE, String.format(NAMENODE_RPC_NN_CLUSTER_PROPERTY, nameservice, namenode));
      if(!Strings.isNullOrEmpty(rpcAddress)) {
        rpcAddresses.add(rpcAddress);
      }

      String httpAddress = getProperty(HDFS_SITE, String.format(NAMENODE_HTTP_NN_CLUSTER_PROPERTY, nameservice, namenode));
      if(!Strings.isNullOrEmpty(httpAddress)) {
        httpAddresses.add(httpAddress);
      }

      String httpsAddress = getProperty(HDFS_SITE, String.format(NAMENODE_HTTPS_NN_CLUSTER_PROPERTY, nameservice, namenode));
      if(!Strings.isNullOrEmpty(httpsAddress)) {
        httpsAddresses.add(httpsAddress);
      }
    }

    addAddresses(rpcAddresses, NAMENODE_RPC_NN_INSTANCE_PROPERTY);
    addAddresses(httpAddresses, NAMENODE_HTTP_NN_INSTANCE_PROPERTY);
    addAddresses(httpsAddresses, NAMENODE_HTTPS_NN_INSTANCE_PROPERTY);

    for (int i = 0 ; i < namenodes.length ; i++) {
      conf.set( String.format(NAMENODE_RPC_NN_CLUSTER_PROPERTY, nameservice, namenodes[i]), rpcAddresses.get(i));
      conf.set( String.format(NAMENODE_HTTP_NN_CLUSTER_PROPERTY, nameservice, namenodes[i]), httpAddresses.get(i));
      conf.set( String.format(NAMENODE_HTTPS_NN_CLUSTER_PROPERTY, nameservice, namenodes[i]), httpsAddresses.get(i));
    }

    copyClusterProperty(String.format(FAILOVER_PROXY_PROVIDER_CLUSTER_PROPERTY, nameservice),
      FAILOVER_PROXY_PROVIDER_INSTANCE_PROPERTY);
  }

  private void addAddresses(List<String> addressList, String propertyName) {
    if(addressList.isEmpty()){
      //      get property from view instance configs
      String addressString = getViewProperty(propertyName);
      LOG.debug("value of {} in view is : {}", propertyName, addressString);
      if(!Strings.isNullOrEmpty(addressString)){
        addressList.addAll(Arrays.asList(addressString.split(",")));
      }
    }
  }

  private String copyClusterProperty(String propertyName, String instancePropertyName) {
    String value = getProperty(HDFS_SITE, propertyName, instancePropertyName);
    if (!StringUtils.isEmpty(value)) {
      conf.set(propertyName, value);
    }
    LOG.debug("set " + propertyName + " = " + value);
    return value;
  }

  private boolean isHAEnabled(String defaultFS) throws URISyntaxException {
    URI uri = new URI(defaultFS);
    String nameservice = uri.getHost();
    String namenodeIDs = getProperty(HDFS_SITE, String.format(HA_NAMENODES_CLUSTER_PROPERTY, nameservice),
      HA_NAMENODES_INSTANCE_PROPERTY);
    LOG.debug("namenodeIDs " + namenodeIDs);
    return !StringUtils.isEmpty(namenodeIDs);
  }

  private static boolean hasPort(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return uri.getPort() != -1;
  }

  protected static String addPortIfMissing(String defaultFs) throws URISyntaxException {
    if (!hasPort(defaultFs)) {
      defaultFs = defaultFs + ":50070";
    }

    return defaultFs;
  }

  protected static String addProtocolIfMissing(String defaultFs) {
    if (!defaultFs.matches("^[^:]+://.*$")) {
      defaultFs = "webhdfs://" + defaultFs;
    }

    return defaultFs;
  }

  /**
   * Set properties relevant to authentication parameters to HDFS Configuration
   * @param authParams list of auth params of View
   */
  public void setAuthParams(Map<String, String> authParams) {
    String auth = authParams.get("auth");
    if (auth != null) {
      conf.set("hadoop.security.authentication", auth);
    }
  }

  /**
   * Build the HDFS configuration
   * @return configured HDFS Configuration object
   * @throws HdfsApiException if configuration parsing failed
   */
  public Configuration buildConfig() throws HdfsApiException {
    copyPropertiesBySite(CORE_SITE);
    copyPropertiesBySite(HDFS_SITE);
    parseProperties();
    setAuthParams(buildAuthenticationConfig());

    String umask = getViewProperty(UMASK_INSTANCE_PROPERTY);
    if (umask != null && !umask.isEmpty()) conf.set(UMASK_CLUSTER_PROPERTY, umask);

    if(null != this.customProperties){
      copyProperties(this.customProperties);
    }

    if(LOG.isDebugEnabled()){
      LOG.debug("final conf : {}", printConf());
    }

    return conf;
  }

  private String printConf() {
    try {
      StringWriter stringWriter = new StringWriter();
      conf.writeXml(stringWriter);
      stringWriter.close();
      return stringWriter.toString().replace("\n", "");
    } catch (IOException e) {
      LOG.error("error while converting conf to xml : ", e);
      return "";
    }
  }


  /**
   * Builds the authentication configuration
   * @return map of HDFS auth params for view
   * @throws HdfsApiException
   */
  public Map<String, String> buildAuthenticationConfig() throws HdfsApiException {
    if (authParams == null) {
      authParams = authParamsBuilder.build();
    }
    return authParams;
  }
}
