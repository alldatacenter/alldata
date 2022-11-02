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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.utils.DateUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent for all commands.
 */
public abstract class StackAdvisorCommand<T extends StackAdvisorResponse> extends BaseService {

  /**
   * Type of response object provided by extending classes when
   * {@link #invoke(StackAdvisorRequest, ServiceInfo.ServiceAdvisorType)} is called.
   */
  private Class<T> type;

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorCommand.class);

  private static final String GET_HOSTS_INFO_URI = "/api/v1/hosts"
      + "?fields=Hosts/*&Hosts/host_name.in(%s)";

  private static final String GET_SERVICES_INFO_URI = "/api/v1/stacks/%s/versions/%s/"
      + "?fields=Versions/stack_name,Versions/stack_version,Versions/parent_stack_version"
      + ",services/StackServices/service_name,services/StackServices/service_version"
      + ",services/components/StackServiceComponents,services/components/dependencies/Dependencies/scope"
      + ",services/components/dependencies/Dependencies/type"
      + ",services/components/dependencies/Dependencies/conditions,services/components/auto_deploy"
      + ",services/configurations/StackConfigurations/property_depends_on"
      + ",services/configurations/dependencies/StackConfigurationDependency/dependency_name"
      + ",services/configurations/dependencies/StackConfigurationDependency/dependency_type,services/configurations/StackConfigurations/type"
      + "&services/StackServices/service_name.in(%s)";

  private static final String SERVICES_PROPERTY = "services";
  private static final String SERVICES_COMPONENTS_PROPERTY = "components";
  private static final String CONFIG_GROUPS_PROPERTY = "config-groups";
  private static final String STACK_SERVICES_PROPERTY = "StackServices";
  private static final String COMPONENT_INFO_PROPERTY = "StackServiceComponents";
  private static final String COMPONENT_NAME_PROPERTY = "component_name";
  private static final String COMPONENT_HOSTNAMES_PROPERTY = "hostnames";
  private static final String CONFIGURATIONS_PROPERTY = "configurations";
  private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed-configurations";
  private static final String USER_CONTEXT_PROPERTY = "user-context";
  private static final String GPL_LICENSE_ACCEPTED = "gpl-license-accepted";
  private static final String AMBARI_SERVER_PROPERTIES_PROPERTY = "ambari-server-properties";
  private static final String AMBARI_SERVER_CONFIGURATIONS_PROPERTY = "ambari-server-configuration";

  private final Map<String, JsonNode> hostInfoCache;

  private File recommendationsDir;
  private String recommendationsArtifactsLifetime;
  private ServiceInfo.ServiceAdvisorType serviceAdvisorType;

  private int requestId;
  private File requestDirectory;
  private StackAdvisorRunner saRunner;

  protected ObjectMapper mapper;

  private final AmbariMetaInfo metaInfo;

  private final AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  @SuppressWarnings("unchecked")
  public StackAdvisorCommand(File recommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType, int requestId,
                             StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo, AmbariServerConfigurationHandler ambariServerConfigurationHandler,
                             Map<String, JsonNode> hostInfoCache) {
    this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];

    this.mapper = new ObjectMapper();
    this.mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

    this.recommendationsDir = recommendationsDir;
    this.recommendationsArtifactsLifetime = recommendationsArtifactsLifetime;
    this.serviceAdvisorType = serviceAdvisorType;
    this.requestId = requestId;
    this.saRunner = saRunner;
    this.metaInfo = metaInfo;
    this.ambariServerConfigurationHandler = ambariServerConfigurationHandler;
    this.hostInfoCache = hostInfoCache;
  }
  public StackAdvisorCommand(File recommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType, int requestId,
                             StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo, AmbariServerConfigurationHandler ambariServerConfigurationHandler) {
    this(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, saRunner, metaInfo,
        ambariServerConfigurationHandler, null);
  }

  protected abstract StackAdvisorCommandType getCommandType();

  /**
   * Simple holder for 'hosts.json' and 'services.json' data.
   */
  public static class StackAdvisorData {
    protected String hostsJSON;
    protected String servicesJSON;

    public StackAdvisorData(String hostsJSON, String servicesJSON) {
      this.hostsJSON = hostsJSON;
      this.servicesJSON = servicesJSON;
    }
  }

  /**
   * Name with the result JSON, e.g. "component-layout.json" or
   * "validations.json" .
   *
   * @return the file name
   */
  protected abstract String getResultFileName();

  protected abstract void validate(StackAdvisorRequest request) throws StackAdvisorException;

  protected StackAdvisorData adjust(StackAdvisorData data, StackAdvisorRequest request) {
    try {
      ObjectNode root = (ObjectNode) this.mapper.readTree(data.servicesJSON);

      populateStackHierarchy(root);
      populateComponentHostsMap(root, request.getComponentHostsMap());
      populateServiceAdvisors(root);
      populateConfigurations(root, request);
      populateConfigGroups(root, request);
      populateAmbariServerInfo(root);
      populateAmbariConfiguration(root);
      data.servicesJSON = mapper.writeValueAsString(root);
    } catch (Exception e) {
      // should not happen
      String message = "Error parsing services.json file content: " + e.getMessage();
      LOG.warn(message, e);
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(message).build());
    }

    return data;
  }

  /**
   * Retrieves the Ambari configuration if exists and adds it to services.json
   *
   * @param root The JSON document that will become service.json when passed to the stack advisor engine
   */
  void populateAmbariConfiguration(ObjectNode root) {
    root.put(AMBARI_SERVER_CONFIGURATIONS_PROPERTY, mapper.valueToTree(ambariServerConfigurationHandler.getConfigurations()));
  }

  protected void populateAmbariServerInfo(ObjectNode root) {
    Map<String, String> serverProperties = metaInfo.getAmbariServerProperties();

    if (serverProperties != null && !serverProperties.isEmpty()) {
      JsonNode serverPropertiesNode = mapper.convertValue(serverProperties, JsonNode.class);
      root.put(AMBARI_SERVER_PROPERTIES_PROPERTY, serverPropertiesNode);
    }
  }

  private void populateConfigurations(ObjectNode root,
                                      StackAdvisorRequest request) {
    Map<String, Map<String, Map<String, String>>> configurations =
        request.getConfigurations();
    ObjectNode configurationsNode = root.putObject(CONFIGURATIONS_PROPERTY);
    for (String siteName : configurations.keySet()) {
      ObjectNode siteNode = configurationsNode.putObject(siteName);

      Map<String, Map<String, String>> siteMap = configurations.get(siteName);
      for (String properties : siteMap.keySet()) {
        ObjectNode propertiesNode = siteNode.putObject(properties);

        Map<String, String> propertiesMap = siteMap.get(properties);
        for (String propertyName : propertiesMap.keySet()) {
          String propertyValue = propertiesMap.get(propertyName);
          propertiesNode.put(propertyName, propertyValue);
        }
      }
    }

    JsonNode changedConfigs = mapper.valueToTree(request.getChangedConfigurations());
    root.put(CHANGED_CONFIGURATIONS_PROPERTY, changedConfigs);

    JsonNode userContext = mapper.valueToTree(request.getUserContext());
    root.put(USER_CONTEXT_PROPERTY, userContext);
    root.put(GPL_LICENSE_ACCEPTED, request.getGplLicenseAccepted());
  }

  private void populateConfigGroups(ObjectNode root,
                                    StackAdvisorRequest request) {
    if (request.getConfigGroups() != null &&
        !request.getConfigGroups().isEmpty()) {
      JsonNode configGroups = mapper.valueToTree(request.getConfigGroups());
      root.put(CONFIG_GROUPS_PROPERTY, configGroups);
    }
  }

  protected void populateStackHierarchy(ObjectNode root) {
    ObjectNode version = (ObjectNode) root.get("Versions");
    TextNode stackName = (TextNode) version.get("stack_name");
    TextNode stackVersion = (TextNode) version.get("stack_version");
    ObjectNode stackHierarchy = version.putObject("stack_hierarchy");
    stackHierarchy.put("stack_name", stackName);
    ArrayNode parents = stackHierarchy.putArray("stack_versions");
    for (String parentVersion : metaInfo.getStackParentVersions(stackName.asText(), stackVersion.asText())) {
      parents.add(parentVersion);
    }
  }

  private void populateComponentHostsMap(ObjectNode root, Map<String, Set<String>> componentHostsMap) {
    ArrayNode services = (ArrayNode) root.get(SERVICES_PROPERTY);
    Iterator<JsonNode> servicesIter = services.getElements();

    while (servicesIter.hasNext()) {
      JsonNode service = servicesIter.next();
      ArrayNode components = (ArrayNode) service.get(SERVICES_COMPONENTS_PROPERTY);
      Iterator<JsonNode> componentsIter = components.getElements();

      while (componentsIter.hasNext()) {
        JsonNode component = componentsIter.next();
        ObjectNode componentInfo = (ObjectNode) component.get(COMPONENT_INFO_PROPERTY);
        String componentName = componentInfo.get(COMPONENT_NAME_PROPERTY).getTextValue();

        Set<String> componentHosts = componentHostsMap.get(componentName);
        ArrayNode hostnames = componentInfo.putArray(COMPONENT_HOSTNAMES_PROPERTY);
        if (null != componentHosts) {
          for (String hostName : componentHosts) {
            hostnames.add(hostName);
          }
        }
      }
    }
  }

  private void populateServiceAdvisors(ObjectNode root) {
    ArrayNode services = (ArrayNode) root.get(SERVICES_PROPERTY);
    Iterator<JsonNode> servicesIter = services.getElements();

    ObjectNode version = (ObjectNode) root.get("Versions");
    String stackName = version.get("stack_name").asText();
    String stackVersion = version.get("stack_version").asText();

    while (servicesIter.hasNext()) {
      JsonNode service = servicesIter.next();
      ObjectNode serviceVersion = (ObjectNode) service.get(STACK_SERVICES_PROPERTY);
      String serviceName = serviceVersion.get("service_name").getTextValue();
      try {
        ServiceInfo serviceInfo = metaInfo.getService(stackName, stackVersion, serviceName);
        if (serviceInfo.getAdvisorFile() != null) {
          serviceVersion.put("advisor_name", serviceInfo.getAdvisorName());
          serviceVersion.put("advisor_path", serviceInfo.getAdvisorFile().getAbsolutePath());
        }
      } catch (Exception e) {
        LOG.error("Error adding service advisor information to services.json", e);
      }
    }
  }

  public synchronized T invoke(StackAdvisorRequest request, ServiceInfo.ServiceAdvisorType serviceAdvisorType) throws StackAdvisorException {
    validate(request);
    String hostsJSON = getHostsInformation(request);
    String servicesJSON = getServicesInformation(request);

    StackAdvisorData adjusted = adjust(new StackAdvisorData(hostsJSON, servicesJSON), request);

    try {
      createRequestDirectory();

      FileUtils.writeStringToFile(new File(requestDirectory, "hosts.json"), adjusted.hostsJSON,
              Charset.defaultCharset());
      FileUtils
              .writeStringToFile(new File(requestDirectory, "services.json"), adjusted.servicesJSON,
                      Charset.defaultCharset());

      saRunner.runScript(serviceAdvisorType, getCommandType(), requestDirectory);
      String result = FileUtils.readFileToString(new File(requestDirectory, getResultFileName()),
              Charset.defaultCharset());

      T response = this.mapper.readValue(result, this.type);
      return updateResponse(request, setRequestId(response));
    } catch (StackAdvisorException ex) {
      throw ex;
    } catch (Exception e) {
      String message = "Error occured during stack advisor command invocation: ";
      LOG.warn(message, e);
      throw new StackAdvisorException(message + e.getMessage());
    }
  }

  protected abstract T updateResponse(StackAdvisorRequest request, T response);

  private T setRequestId(T response) {
    response.setId(requestId);
    return response;
  }

  /**
   * Create request id directory for each call
   */
  private void createRequestDirectory() throws IOException {
    if (!recommendationsDir.exists()) {
      if (!recommendationsDir.mkdirs()) {
        throw new IOException("Cannot create " + recommendationsDir);
      }
    }

    cleanupRequestDirectory();

    requestDirectory = new File(recommendationsDir, Integer.toString(requestId));

    if (requestDirectory.exists()) {
      FileUtils.deleteDirectory(requestDirectory);
    }
    if (!requestDirectory.mkdirs()) {
      throw new IOException("Cannot create " + requestDirectory);
    }
  }

  /**
   * Deletes folders older than (now - recommendationsArtifactsLifetime)
   */
  private void cleanupRequestDirectory() throws IOException {
    final Date cutoffDate = DateUtils.getDateSpecifiedTimeAgo(recommendationsArtifactsLifetime); // subdirectories older than this date will be deleted

    String[] oldDirectories = recommendationsDir.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        File file = new File(current, name);
        return file.isDirectory() && !FileUtils.isFileNewer(file, cutoffDate);
      }
    });

    if (oldDirectories.length > 0) {
      LOG.info(String.format("Deleting old directories %s from %s", StringUtils.join(oldDirectories, ", "), recommendationsDir));
    }

    for (String oldDirectory : oldDirectories) {
      FileUtils.deleteQuietly(new File(recommendationsDir, oldDirectory));
    }
  }

  String getHostsInformation(StackAdvisorRequest request) throws StackAdvisorException {
    List<String> hostNames = new ArrayList<>(request.getHosts());

    // retrieve cached info
    List<JsonNode> resultInfos = new ArrayList<>();
    if (hostInfoCache != null && !hostInfoCache.isEmpty()) {
      Iterator<String> hostNamesIterator = hostNames.iterator();
      while(hostNamesIterator.hasNext()) {
        String hostName = hostNamesIterator.next();
        JsonNode node = hostInfoCache.get(hostName);
        if (node != null) {
          resultInfos.add(node);
          hostNamesIterator.remove();
        }
      }
    }
    String hostsJSON = null;

    // get hosts info for not cached hosts only
    if (!hostNames.isEmpty()) {
      LOG.info(String.format("Fire host info request for hosts: " + hostNames.toString()));
      String hostsURI = String.format(GET_HOSTS_INFO_URI, String.join(",", hostNames));

      Response response = handleRequest(null, null, new LocalUriInfo(hostsURI), Request.Type.GET,
          createHostResource());

      if (response.getStatus() != Status.OK.getStatusCode()) {
        String message = String.format(
            "Error occured during hosts information retrieving, status=%s, response=%s",
            response.getStatus(), (String) response.getEntity());
        LOG.warn(message);
        throw new StackAdvisorException(message);
      }

      hostsJSON = (String) response.getEntity();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Hosts information: {}", hostsJSON);
      }
    }

    // when cache is used we should merge cached info with just got
    if (hostInfoCache != null) {
      if (hostsJSON != null && !hostsJSON.isEmpty()) {
        try {
          JsonNode root = mapper.readTree(hostsJSON);
          Iterator<JsonNode> iterator = root.get("items").getElements();
          while (iterator.hasNext()) {
            JsonNode next = iterator.next();
            String hostName = next.get("Hosts").get("host_name").getTextValue();
            hostInfoCache.put(hostName, next);
            resultInfos.add(next);
          }
        } catch (IOException e) {
          throw new StackAdvisorException("Error occured during parsing result host infos", e);
        }
      }

      String fullHostsURI = String.format(GET_HOSTS_INFO_URI, request.getHostsCommaSeparated());
      JsonNodeFactory f = JsonNodeFactory.instance;
      ObjectNode resultRoot = f.objectNode();
      resultRoot.put("href", fullHostsURI);
      ArrayNode resultArray = resultRoot.putArray("items");
      resultArray.addAll(resultInfos);

      hostsJSON = resultRoot.toString();

    }

    Collection<String> unregistered = getUnregisteredHosts(hostsJSON, request.getHosts());
    if (unregistered.size() > 0) {
      String message = String.format("There are unregistered hosts in the request, %s",
          Arrays.toString(unregistered.toArray()));
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    return hostsJSON;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getUnregisteredHosts(String hostsJSON, List<String> hosts)
      throws StackAdvisorException {
    List<String> registeredHosts = new ArrayList<>();

    try {
      JsonNode root = mapper.readTree(hostsJSON);
      Iterator<JsonNode> iterator = root.get("items").getElements();
      while (iterator.hasNext()) {
        JsonNode next = iterator.next();
        String hostName = next.get("Hosts").get("host_name").getTextValue();
        registeredHosts.add(hostName);
      }

      return CollectionUtils.subtract(hosts, registeredHosts);
    } catch (Exception e) {
      throw new StackAdvisorException("Error occured during calculating unregistered hosts", e);
    }
  }

  String getServicesInformation(StackAdvisorRequest request) throws StackAdvisorException {
    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String servicesURI = String.format(GET_SERVICES_INFO_URI, stackName, stackVersion,
        request.getServicesCommaSeparated());

    Response response = handleRequest(null, null, new LocalUriInfo(servicesURI),
        Request.Type.GET, createStackVersionResource(stackName, stackVersion));

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String message = String.format(
          "Error occured during services information retrieving, status=%s, response=%s",
          response.getStatus(), (String) response.getEntity());
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    String servicesJSON = (String) response.getEntity();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Services information: {}", servicesJSON);
    }
    return servicesJSON;
  }

  private ResourceInstance createHostResource() {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    return createResource(Resource.Type.Host, mapIds);
  }

  private ResourceInstance createConfigResource() {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, RootService.AMBARI.name());
    mapIds.put(Resource.Type.RootServiceComponent, RootComponent.AMBARI_SERVER.name());

    return createResource(Resource.Type.RootServiceComponentConfiguration, mapIds);
  }


  private ResourceInstance createStackVersionResource(String stackName, String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

}
