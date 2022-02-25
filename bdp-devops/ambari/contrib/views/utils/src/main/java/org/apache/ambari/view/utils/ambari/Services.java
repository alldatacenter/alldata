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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Utilities for specific Hadoop services and util functions for them
 */
public class Services {
  public static final String HTTPS_ONLY = "HTTPS_ONLY";
  public static final String HTTP_ONLY = "HTTP_ONLY";
  public static final String YARN_SITE = "yarn-site";
  public static final String YARN_HTTP_POLICY = "yarn.http.policy";
  public static final String YARN_RESOURCEMANAGER_HA_ENABLED = "yarn.resourcemanager.ha.enabled";
  private static final String YARN_RESOURCEMANAGER_HTTPS_KEY = "yarn.resourcemanager.webapp.https.address";
  private static final String YARN_RESOURCEMANAGER_HTTP_KEY = "yarn.resourcemanager.webapp.address";
  private static final String YARN_RESOURCEMANAGER_HA_RM_IDS_KEY = "yarn.resourcemanager.ha.rm-ids";
  private static final String YARN_RESOURCEMANAGER_HTTP_HA_PARTIAL_KEY = "yarn.resourcemanager.webapp.address.";
  private static final String YARN_RESOURCEMANAGER_HTTPS_HA_PARTIAL_KEY = "yarn.resourcemanager.webapp.https.address.";
  private static final String YARN_RESOURCEMANAGER_HOSTNAME_KEY = "yarn.resourcemanager.hostname";
  private static final String YARN_RESOURCEMANAGER_HOSTNAME_PARTIAL_KEY = YARN_RESOURCEMANAGER_HOSTNAME_KEY + ".";
  private static final String YARN_RESOURCEMANAGER_DEFAULT_HTTP_PORT = "8088";
  private static final String YARN_RESOURCEMANAGER_DEFAULT_HTTPS_PORT = "8090";


  private static final String YARN_ATS_URL = "yarn.ats.url";
  private final static String YARN_TIMELINE_WEBAPP_HTTP_ADDRESS_KEY = "yarn.timeline-service.webapp.address";
  private final static String YARN_TIMELINE_WEBAPP_HTTPS_ADDRESS_KEY = "yarn.timeline-service.webapp.https.address";
  public static final String RM_INFO_API_ENDPOINT = "/ws/v1/cluster/info";
  public static final String TIMELINE_AUTH_TYPE_PROP_KEY = "timeline.http.auth.type";
  public static final String HADOOP_HTTP_AUTH_TYPE_KEY = "hadoop.http.auth.type";

  private final AmbariApi ambariApi;
  private ViewContext context;

  protected final static Logger LOG = LoggerFactory.getLogger(Services.class);

  public Services(AmbariApi ambariApi, ViewContext context) {
    this.ambariApi = ambariApi;
    this.context = context;
  }

  /**
   * Returns URL to Resource Manager.
   * If cluster associated, returns HTTP or HTTPS address based on "yarn.http.policy" property value.
   * If not associated, retrieves RM URL from view instance properties by "yarn.resourcemanager.url" property.
   * @return url of RM
   */
  public String getRMUrl() {
    String url;

    if (context.getCluster() != null) {
      url = getRMUrlFromClusterConfig();
    } else {
      url = getRmUrlFromCustomConfig();
    }
    return removeTrailingSlash(url);
  }

  private String getRMUrlFromClusterConfig() {
    String url;

    String haEnabled = getYarnConfig(YARN_RESOURCEMANAGER_HA_ENABLED);
    String httpPolicy = getYarnConfig(YARN_HTTP_POLICY);

    if (!(HTTP_ONLY.equals(httpPolicy) || HTTPS_ONLY.equals(httpPolicy))) {
      LOG.error(String.format("RA030 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      httpPolicy = HTTP_ONLY;
    }

    if (haEnabled != null && haEnabled.equals("true")) {
      String[] urls = getRMHAUrls(httpPolicy);
      url = getActiveRMUrl(urls);
    } else {
      url =  (httpPolicy.equals(HTTPS_ONLY)) ? getYarnConfig(YARN_RESOURCEMANAGER_HTTPS_KEY)
        : getYarnConfig(YARN_RESOURCEMANAGER_HTTP_KEY);

      if (url == null || url.isEmpty()) {
        url = getYarnConfig(YARN_RESOURCEMANAGER_HOSTNAME_KEY).trim() + ":" + getDefaultRMPort(httpPolicy);
      }

      url = addProtocolIfMissing(url, getProtocol(httpPolicy));
    }
    return url;
  }

  private String[] getRMHAUrls(String httpPolicy) {
    String haRmIds = getYarnConfig(YARN_RESOURCEMANAGER_HA_RM_IDS_KEY);
    String[] ids = haRmIds.split(",");
    int index = 0;
    String[] urls = new String[ids.length];
    for (String id : ids) {
      String url = (HTTPS_ONLY.equals(httpPolicy)) ? getYarnConfig(YARN_RESOURCEMANAGER_HTTPS_HA_PARTIAL_KEY + id)
        : getYarnConfig(YARN_RESOURCEMANAGER_HTTP_HA_PARTIAL_KEY + id);

      if (url == null || url.isEmpty()) {
        url = getYarnConfig(YARN_RESOURCEMANAGER_HOSTNAME_PARTIAL_KEY + id).trim() + ":" + getDefaultRMPort(httpPolicy);
      }

      urls[index++] = addProtocolIfMissing(url.trim(), getProtocol(httpPolicy));
    }
    return urls;
  }

  private String getRmUrlFromCustomConfig() {
    // Comma separated list of URLs for HA and single URL for non HA
    String resourceManagerUrls = context.getProperties().get("yarn.resourcemanager.url");
    if (!StringUtils.isEmpty(resourceManagerUrls)) {
      String[] urls = resourceManagerUrls.split(",");

      if (!hasProtocol(urls)) {
        throw new AmbariApiException(
          "RA070 View is not cluster associated. All Resource Manager URL should contain protocol.");
      }
      return getActiveRMUrl(urls);
    } else {
      throw new AmbariApiException(
        "RA070 View is not cluster associated. 'YARN ResourceManager URL' should be provided");
    }
  }


  private String removeTrailingSlash(String url) {
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  /**
   * Returns active RM URL. All RM Urls for RM HA is passed as an argument. This iterates over the list of RM hosts
   * and gets the cluster info. Breaks out and returns the URL when the 'haStatus' parameter returns "ACTIVE".
   * If only one url is passed, it is considered as ACTIVE and returned. No API call is made in that case.
   * @param urls array of all the RM Urls
   * @return url of the active RM
   */
  private String getActiveRMUrl(String[] urls) {
    if (urls.length == 1)
      return urls[0].trim();
    else {
      for (String url : urls) {
        url = url.trim();
        if (isActiveUrl(url))
          return url;
      }
    }
    LOG.error("All ResourceManagers are not accessible or none seem to be active.");
    throw new AmbariApiException("RA110 All ResourceManagers are not accessible or none seem to be active.");
  }

  /**
   * Queries RM API to check the haState.
   * @param url Resource Manager root url
   * @return true if haState returned is ACTIVE else false
   */

  private boolean isActiveUrl(String url) {
    InputStream inputStream = null;
    try {
      inputStream = context.getURLStreamProvider()
        .readFrom(url + RM_INFO_API_ENDPOINT, "GET", (String) null, new HashMap<String, String>());
      String response = IOUtils.toString(inputStream);
      String haState = getHAStateFromRMResponse(response);

      if (StringUtils.isNotEmpty(haState) && "ACTIVE".equals(haState))
        return true;

    } catch (IOException e) {
      LOG.error("Resource Manager : %s is not accessible. This cannot be a active RM. Returning false.");
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) { /* Noting to do */ }
      }
    }
    return false;
  }

  private String getHAStateFromRMResponse(String response) {
    JSONObject jsonObject = (JSONObject) JSONValue.parse(response);
    JSONObject clusterInfo = (JSONObject) jsonObject.get("clusterInfo");
    return (String) clusterInfo.get("haState");
  }

  /**
   * Returns URL to WebHCat in format like http://<hostname>:<port>/templeton/v1
   * @return url to WebHCat
   */
  public String getWebHCatURL() {
    String host = null;

    if (context.getCluster() != null) {
      List<String> hiveServerHosts = context.getCluster().getHostsForServiceComponent("HIVE","WEBHCAT_SERVER");

      if (!hiveServerHosts.isEmpty()) {
        host = hiveServerHosts.get(0);
        LOG.info("WEBHCAT_SERVER component was found on host " + host);
      } else {
        LOG.warn("No host was found with WEBHCAT_SERVER component. Using hive.host property to get hostname.");
      }
    }

    if (host == null) {
      host = context.getProperties().get("webhcat.hostname");
      if (host == null || host.isEmpty()) {
        throw new AmbariApiException(
          "RA080 Can't determine WebHCat hostname neither by associated cluster nor by webhcat.hostname property.");
      }
    }

    String port = context.getProperties().get("webhcat.port");
    if (port == null || port.isEmpty()) {
      throw new AmbariApiException(
        "RA090 Can't determine WebHCat port neither by associated cluster nor by webhcat.port property.");
    }

    return String.format("http://%s:%s/templeton/v1", host, port);
  }

  /**
   * @return The timeline server url. If the view instance is cluster associated, the value is taken from the
   * yarn-site.xml else it is retrieved from the view configuration.
   */
  public String getTimelineServerUrl() {
    String url = context.getCluster() != null ? getATSUrlFromCluster() : getATSUrlFromCustom();
    return removeTrailingSlash(url);
  }

  private String getATSUrlFromCustom() {
    String atsUrl = context.getProperties().get(YARN_ATS_URL);
    if (!StringUtils.isEmpty(atsUrl)) {
      if (!hasProtocol(atsUrl)) {
        throw new AmbariApiException(
          "RA070 View is not cluster associated. Timeline Server URL should contain protocol.");
      }
      return atsUrl;
    } else {
      throw new AmbariApiException(
        "RA070 View is not cluster associated. 'YARN Timeline Server URL' should be provided");
    }
  }

  /**
   * @return Returns the protocol used by YARN daemons, the value is always taken from the
   * yarn-site.xml.
   */
  public String getYARNProtocol() {
    String httpPolicy = getYarnConfig(YARN_HTTP_POLICY);

    if (!(HTTP_ONLY.equals(httpPolicy) || HTTPS_ONLY.equals(httpPolicy))) {
      LOG.error(String.format("RA030 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      httpPolicy = HTTP_ONLY;
    }

    return getProtocol(httpPolicy);
  }

  private String getATSUrlFromCluster() {
    String url;

    String httpPolicy = getYarnConfig(YARN_HTTP_POLICY);

    if (!(HTTP_ONLY.equals(httpPolicy) || HTTPS_ONLY.equals(httpPolicy))) {
      LOG.error(String.format("RA030 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      httpPolicy = HTTP_ONLY;
    }

    if (httpPolicy.equals(HTTPS_ONLY)) {
      url = getYarnConfig(YARN_TIMELINE_WEBAPP_HTTPS_ADDRESS_KEY);
    } else {
      url = getYarnConfig(YARN_TIMELINE_WEBAPP_HTTP_ADDRESS_KEY);
    }
    url = addProtocolIfMissing(url, getProtocol(httpPolicy));

    return url;
  }

  public static String addProtocolIfMissing(String url, String protocol) throws AmbariApiException {
    if (!hasProtocol(url)) {
      url = protocol + "://" + url;
    }
    return url;
  }


  /**
   * Checks if all the urls in the array contains protocol
   * @param urls Array of urls
   * @return true if all the urls contain protocol
   */
  public static boolean hasProtocol(String[] urls) {
    for (String url : urls) {
      if (!hasProtocol(url))
        return false;
    }
    return true;
  }

  /**
   * Checks if URL has the protocol
   * @param url url
   * @return true if protocol is present
   */
  public static boolean hasProtocol(String url) {
    return url.matches("^[^:]+://.*$");
  }

  private String getProtocol(String yarnHttpPolicyConfig) {
    return HTTPS_ONLY.equals(yarnHttpPolicyConfig) ? "https" : "http";
  }

  private String getYarnConfig(String key) {
    return context.getCluster().getConfigurationValue(YARN_SITE, key);
  }

  /**
   * @param yarnHttpPolicy - The HTTP Policy configured in YARN site file
   * @return The default resource manager port depending on the http policy
   */
  private String getDefaultRMPort(String yarnHttpPolicy) {
    return (HTTPS_ONLY.equals(yarnHttpPolicy)) ? YARN_RESOURCEMANAGER_DEFAULT_HTTPS_PORT : YARN_RESOURCEMANAGER_DEFAULT_HTTP_PORT;
  }

  /**
   * @return The authentication type for RM. Check: https://hadoop.apache.org/docs/r1.2.1/HttpAuthentication.html
   */
  public String getHadoopHttpWebAuthType() {
    return context.getProperties().get(HADOOP_HTTP_AUTH_TYPE_KEY);
  }

  /**
   * @return Authentication used for the timeline server HTTP endpoint. Check: https://hadoop.apache.org/docs/r2.7.1/hadoop-yarn/hadoop-yarn-site/TimelineServer.html
   */
  public String getTimelineServerAuthType() {
    return context.getProperties().get(TIMELINE_AUTH_TYPE_PROP_KEY);
  }
}
