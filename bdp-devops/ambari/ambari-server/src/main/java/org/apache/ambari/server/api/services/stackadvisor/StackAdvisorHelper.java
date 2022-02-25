/*
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

package org.apache.ambari.server.api.services.stackadvisor;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationDependenciesRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class StackAdvisorHelper {

  protected static Log LOG = LogFactory.getLog(StackAdvisorHelper.class);

  private File recommendationsDir;
  private String recommendationsArtifactsLifetime;
  private int recommendationsArtifactsRolloverMax;
  private final AmbariMetaInfo metaInfo;
  private final AmbariServerConfigurationHandler ambariServerConfigurationHandler;
  private final Gson gson;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private StackAdvisorRunner saRunner;

  private Map<String, JsonNode> hostInfoCache = new ConcurrentHashMap<>();
  private Map<String, RecommendationResponse> configsRecommendationResponse = new ConcurrentHashMap<>();


  @Inject
  public StackAdvisorHelper(Configuration conf, StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo,
                            AmbariServerConfigurationHandler ambariServerConfigurationHandler, Gson gson) throws IOException {
    this.recommendationsDir = conf.getRecommendationsDir();
    this.recommendationsArtifactsLifetime = conf.getRecommendationsArtifactsLifetime();
    this.recommendationsArtifactsRolloverMax = conf.getRecommendationsArtifactsRolloverMax();
    this.saRunner = saRunner;
    this.metaInfo = metaInfo;
    this.ambariServerConfigurationHandler = ambariServerConfigurationHandler;
    this.gson = gson;
  }

  /**
   * Returns validation (component-layout or configurations) result for the
   * request.
   * 
   * @param request the validation request
   * @return {@link ValidationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized ValidationResponse validate(StackAdvisorRequest request)
      throws StackAdvisorException {
      requestId = generateRequestId();

    // TODO, need frontend to pass the Service Name that was modified.
    // For now, hardcode.
    // Once fixed, change StackAdvisorHelperTest.java to use the actual service name.
    String serviceName = "ZOOKEEPER";
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);
    StackAdvisorCommand<ValidationResponse> command = createValidationCommand(serviceName, request);

    return command.invoke(request, serviceAdvisorType);
  }

  StackAdvisorCommand<ValidationResponse> createValidationCommand(String serviceName, StackAdvisorRequest request) throws StackAdvisorException {
    StackAdvisorRequestType requestType = request.getRequestType();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);

    StackAdvisorCommand<ValidationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutValidationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationValidationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler);
    } else {
      throw new StackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  /**
   * Returns recommendation (component-layout or configurations) based on the
   * request.
   * 
   * @param request the recommendation request
   * @return {@link RecommendationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized RecommendationResponse recommend(StackAdvisorRequest request)
      throws StackAdvisorException, AmbariException {
      requestId = generateRequestId();

    // TODO, need to pass the service Name that was modified.
    // For now, hardcode
    String serviceName = "ZOOKEEPER";

    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);
    StackAdvisorCommand<RecommendationResponse> command = createRecommendationCommand(serviceName, request);

    StackAdvisorRequestType requestType = request.getRequestType();
    RecommendationResponse response = null;
    if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      String hash = getHash(request);
      LOG.info(String.format("Calling stack advisor with hash: %s, service: %s", hash, request.getServiceName()));
      response = configsRecommendationResponse.computeIfAbsent(hash, h -> {
        try {
          LOG.info(String.format("Invoking configuration stack advisor command with hash: %s, service: %s", hash, request.getServiceName()));
          return command.invoke(request, serviceAdvisorType);
        } catch (StackAdvisorException e) {
          return null;
        }
      });
    }

    return response == null ? command.invoke(request, serviceAdvisorType) : response;
  }

  protected String getHash(StackAdvisorRequest request) {
    String json = gson.toJson(request);
    String generatedPassword = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update("".getBytes("UTF-8"));
      byte[] bytes = md.digest(json.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
      }
      generatedPassword = sb.toString();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return generatedPassword;
  }

  StackAdvisorCommand<RecommendationResponse> createRecommendationCommand(String serviceName, StackAdvisorRequest request) throws StackAdvisorException {
    StackAdvisorRequestType requestType = request.getRequestType();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);

    StackAdvisorCommand<RecommendationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutRecommendationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS, recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler, hostInfoCache);
    } else if (requestType == StackAdvisorRequestType.SSO_CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_SSO, recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler, null);
    } else if (requestType == StackAdvisorRequestType.LDAP_CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_LDAP, recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler, null);
    } else if (requestType == StackAdvisorRequestType.KERBEROS_CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_KERBEROS, recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler, null);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES) {
      command = new ConfigurationDependenciesRecommendationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo, ambariServerConfigurationHandler);
    } else {
      throw new StackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  /**
   * Get the Service Advisor type that the service defines for the specified stack and version. If an error, return null.
   * @param stackName Stack Name
   * @param stackVersion Stack Version
   * @param serviceName Service Name
   * @return Service Advisor type for that Stack, Version, and Service
   */
  private ServiceInfo.ServiceAdvisorType getServiceAdvisorType(String stackName, String stackVersion, String serviceName) {
    try {
      ServiceInfo service = metaInfo.getService(stackName, stackVersion, serviceName);
      ServiceInfo.ServiceAdvisorType serviceAdvisorType = service.getServiceAdvisorType();

      return serviceAdvisorType;
    } catch (AmbariException e) {
      ;
    }
    return null;
  }

  /**
   * Returns an incremented requestId. Rollsover back to 0 in case the requestId >= recommendationsArtifactsrollovermax
   * @return {int requestId}
   */
  private int generateRequestId(){
      requestId += 1;
      return requestId % recommendationsArtifactsRolloverMax;

  }

  public void clearCaches(String hostName) {
    configsRecommendationResponse.clear();
    hostInfoCache.remove(hostName);
    LOG.info("Clear stack advisor caches, host: " + hostName);
  }

  public void clearCaches(Set<String> hostNames) {
    if (hostNames != null && !hostNames.isEmpty()) {
      configsRecommendationResponse.clear();
      for (String hostName : hostNames) {
        hostInfoCache.remove(hostName);
      }
    }
    LOG.info("Clear stack advisor caches, hosts: " + hostNames.toString());
  }

}
