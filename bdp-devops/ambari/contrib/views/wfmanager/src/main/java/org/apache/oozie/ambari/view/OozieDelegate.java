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
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.oozie.ambari.view.exception.ErrorCode;
import org.apache.oozie.ambari.view.exception.WfmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OozieDelegate {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(OozieDelegate.class);
  private static final String OOZIEPARAM_PREFIX = "oozieparam.";
  private static final int OOZIEPARAM_PREFIX_LENGTH = OOZIEPARAM_PREFIX
    .length();
  private static final String EQUAL_SYMBOL = "=";
  private static final String OOZIE_WF_RERUN_FAILNODES_CONF_KEY = "oozie.wf.rerun.failnodes";
  private static final String OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY = "oozie.use.system.libpath";
  private static final String USER_NAME_HEADER = "user.name";
  private static final String USER_OOZIE_SUPER = "oozie";
  private static final String DO_AS_HEADER = "doAs";
  private static final String SERVICE_URI_PROP = "oozie.service.uri";
  private static final String DEFAULT_SERVICE_URI = "http://sandbox.hortonworks.com:11000/oozie";

  private ViewContext viewContext;

  private OozieUtils oozieUtils = new OozieUtils();
  private final Utils utils = new Utils();
  private final AmbariIOUtil ambariIOUtil;

  public OozieDelegate(ViewContext viewContext) {
    super();
    this.viewContext = viewContext;
    this.ambariIOUtil = new AmbariIOUtil(viewContext);
  }

  public String submitWorkflowJobToOozie(HttpHeaders headers,
                                         String filePath, MultivaluedMap<String, String> queryParams,
                                         JobType jobType) {
	String nameNode = viewContext.getProperties().get("webhdfs.url");
	if (nameNode == null) {
		LOGGER.error("Name Node couldn't be determined automatically.");
		throw new RuntimeException("Name Node couldn't be determined automatically.");
	}

    if (!queryParams.containsKey("config.nameNode")) {
      ArrayList<String> nameNodes = new ArrayList<String>();
      LOGGER.info("Namenode===" + nameNode);
      nameNodes.add(nameNode);
      queryParams.put("config.nameNode", nameNodes);
    }

    Map<String, String> workflowConigs = getWorkflowConfigs(filePath,
      queryParams, jobType, nameNode);
    String configXMl = oozieUtils.generateConfigXml(workflowConigs);
    LOGGER.info("Config xml==" + configXMl);
    HashMap<String, String> customHeaders = new HashMap<String, String>();
    customHeaders.put("Content-Type", "application/xml;charset=UTF-8");
    Response serviceResponse = consumeService(headers, getServiceUri()
        + "/v2/jobs?" + getJobSumbitOozieParams(queryParams),
      HttpMethod.POST, configXMl, customHeaders);

    LOGGER.info("Resp from oozie status entity=="
      + serviceResponse.getEntity());
    String oozieResp=null;
    if (serviceResponse.getEntity() instanceof String) {
      oozieResp= (String) serviceResponse.getEntity();
    } else {
      oozieResp= serviceResponse.getEntity().toString();
    }
    if (oozieResp != null && oozieResp.trim().startsWith("{")) {
      return  oozieResp;
    }else{
      throw new WfmException(oozieResp,ErrorCode.OOZIE_SUBMIT_ERROR);
    }
  }

  public Response consumeService(HttpHeaders headers, String path,
                                 MultivaluedMap<String, String> queryParameters, String method,
                                 String body) throws Exception {
    return consumeService(headers, this.buildUri(path, queryParameters),
      method, body, null);
  }

  private Response consumeService(HttpHeaders headers, String urlToRead,
                                  String method, String body, Map<String, String> customHeaders) {
    Response response = null;
    InputStream stream = readFromOozie(headers, urlToRead, method, body,
      customHeaders);
    String stringResponse = null;
    try {
      stringResponse = IOUtils.toString(stream);
    } catch (IOException e) {
      LOGGER.error("Error while converting stream to string", e);
      throw new RuntimeException(e);
    }
    if (stringResponse.contains(Response.Status.BAD_REQUEST.name())) {
      response = Response.status(Response.Status.BAD_REQUEST)
        .entity(stringResponse).type(MediaType.TEXT_PLAIN).build();
    } else {
      response = Response.status(Response.Status.OK)
        .entity(stringResponse)
        .type(utils.deduceType(stringResponse)).build();
    }
    return response;
  }

  public InputStream readFromOozie(HttpHeaders headers, String urlToRead,
                                   String method, String body, Map<String, String> customHeaders) {

    Map<String, String> newHeaders = utils.getHeaders(headers);
    newHeaders.put(USER_NAME_HEADER, USER_OOZIE_SUPER);

    newHeaders.put(DO_AS_HEADER, viewContext.getUsername());
    newHeaders.put("Accept", MediaType.APPLICATION_JSON);
    if (customHeaders != null) {
      newHeaders.putAll(customHeaders);
    }
    LOGGER.info(String.format("Proxy request for url: [%s] %s", method,
      urlToRead));

    return ambariIOUtil.readFromUrl(urlToRead, method, body, newHeaders);
  }

  private Map<String, String> getWorkflowConfigs(String filePath,
                                                 MultivaluedMap<String, String> queryParams, JobType jobType,
                                                 String nameNode) {
    HashMap<String, String> workflowConigs = new HashMap<String, String>();
    if (queryParams.containsKey("resourceManager")
      && "useDefault".equals(queryParams.getFirst("resourceManager"))) {
	  String jobTrackerNode = viewContext.getProperties()
	      .get("yarn.resourcemanager.address");
      LOGGER.info("jobTrackerNode===" + jobTrackerNode);
      workflowConigs.put("resourceManager", jobTrackerNode);
      workflowConigs.put("jobTracker", jobTrackerNode);
    }
    if (queryParams != null) {
      for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
        if (entry.getKey().startsWith("config.")) {
          if (entry.getValue() != null && entry.getValue().size() > 0) {
            workflowConigs.put(entry.getKey().substring(7), entry
              .getValue().get(0));
          }
        }
      }
    }

    if (queryParams.containsKey("oozieconfig.useSystemLibPath")) {
      String useSystemLibPath = queryParams
        .getFirst("oozieconfig.useSystemLibPath");
      workflowConigs.put(OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY,
        useSystemLibPath);
    } else {
      workflowConigs.put(OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY, "true");
    }
    if (queryParams.containsKey("oozieconfig.rerunOnFailure")) {
      String rerunFailnodes = queryParams
        .getFirst("oozieconfig.rerunOnFailure");
      workflowConigs.put(OOZIE_WF_RERUN_FAILNODES_CONF_KEY,
        rerunFailnodes);
    } else {
      workflowConigs.put(OOZIE_WF_RERUN_FAILNODES_CONF_KEY, "true");
    }
    workflowConigs.put("user.name", viewContext.getUsername());
    workflowConigs.put(oozieUtils.getJobPathPropertyKey(jobType), nameNode
      + filePath);
    return workflowConigs;
  }

  private String getJobSumbitOozieParams(
    MultivaluedMap<String, String> queryParams) {
    StringBuilder query = new StringBuilder();
    if (queryParams != null) {
      for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
        if (entry.getKey().startsWith(OOZIEPARAM_PREFIX)) {
          if (entry.getValue() != null && entry.getValue().size() > 0) {
            for (String val : entry.getValue()) {
              query.append(
                entry.getKey().substring(
                  OOZIEPARAM_PREFIX_LENGTH))
                .append(EQUAL_SYMBOL).append(val)
                .append("&");
            }
          }
        }
      }
    }
    return query.toString();
  }

  private String getServiceUri() {
    String serviceURI = viewContext.getProperties().get(SERVICE_URI_PROP) != null ? viewContext
      .getProperties().get(SERVICE_URI_PROP) : DEFAULT_SERVICE_URI;
    return serviceURI;
  }

  private String buildUri(String absolutePath,
                          MultivaluedMap<String, String> queryParameters) {
    int index = absolutePath.indexOf("proxy/") + 5;
    absolutePath = absolutePath.substring(index);
    String serviceURI = getServiceUri();
    serviceURI += absolutePath;
    MultivaluedMap<String, String> params = addOrReplaceUserName(queryParameters);
    return serviceURI + utils.convertParamsToUrl(params);
  }

  private MultivaluedMap<String, String> addOrReplaceUserName(
    MultivaluedMap<String, String> parameters) {
    for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
      if ("user.name".equals(entry.getKey())) {
        ArrayList<String> vals = new ArrayList<String>(1);
        vals.add(viewContext.getUsername());
        entry.setValue(vals);
      }
    }
    return parameters;
  }

  public String getDagUrl(String jobid) {
    return getServiceUri() + "/v2/job/" + jobid + "?show=graph";
  }
}
