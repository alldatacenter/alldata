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

package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for client config resources.
 */
public class ClientConfigResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  protected static final String COMPONENT_CLUSTER_NAME_PROPERTY_ID = "ServiceComponentInfo/cluster_name";
  protected static final String COMPONENT_SERVICE_NAME_PROPERTY_ID = "ServiceComponentInfo/service_name";
  protected static final String COMPONENT_COMPONENT_NAME_PROPERTY_ID = "ServiceComponentInfo/component_name";
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID =
          PropertyHelper.getPropertyId("HostRoles", "host_name");

  private final Gson gson;

  /**
   * The key property ids for a ClientConfig resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, COMPONENT_CLUSTER_NAME_PROPERTY_ID)
      .put(Resource.Type.Service, COMPONENT_SERVICE_NAME_PROPERTY_ID)
      .put(Resource.Type.Component, COMPONENT_COMPONENT_NAME_PROPERTY_ID)
      .put(Resource.Type.Host, HOST_COMPONENT_HOST_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a ClientConfig resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      COMPONENT_CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_SERVICE_NAME_PROPERTY_ID,
      COMPONENT_COMPONENT_NAME_PROPERTY_ID,
      HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

  private MaintenanceStateHelper maintenanceStateHelper;
  private static final Logger LOG = LoggerFactory.getLogger(ClientConfigResourceProvider.class);

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  @AssistedInject
  ClientConfigResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.ClientConfig, propertyIds, keyPropertyIds, managementController);
    gson = new Gson();

    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.HOST_VIEW_CONFIGS, RoleAuthorization.SERVICE_VIEW_CONFIGS, RoleAuthorization.CLUSTER_VIEW_CONFIGS));
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
          throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    throw new SystemException("The request is not supported");
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = new HashSet<>();

    final Set<ServiceComponentHostRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceComponentHostResponse> responses = null;
    try {
      responses = getResources(new Command<Set<ServiceComponentHostResponse>>() {
        @Override
        public Set<ServiceComponentHostResponse> invoke() throws AmbariException {
          return getManagementController().getHostComponents(requests);
        }
      });
    } catch (Exception e) {
      throw new SystemException("Failed to get components ", e);
    }

    Map<String,ServiceComponentHostResponse> componentMap = new HashMap<>();

    // reduce set of sch responses to one sch response for every service component
    for (ServiceComponentHostResponse resp: responses) {
      String componentName = resp.getComponentName();
      if (!componentMap.containsKey(componentName)) {
        componentMap.put(resp.getComponentName(),resp);
      }
    }

    ServiceComponentHostRequest schRequest =  requests.iterator().next();
    String requestComponentName = schRequest.getComponentName();
    String requestServiceName = schRequest.getServiceName();
    String requestHostName =  schRequest.getHostname();

    Map<String,List<ServiceComponentHostResponse>> serviceToComponentMap = new HashMap<>();

    // sch response for the service components that have configFiles defined in the stack definition of the service
    List <ServiceComponentHostResponse> schWithConfigFiles = new ArrayList<>();

    Configuration configs = new Configuration();
    Map<String, String> configMap = configs.getConfigsMap();
    String TMP_PATH = configMap.get(Configuration.SERVER_TMP_DIR.getKey());
    String pythonCmd = configMap.get(Configuration.AMBARI_PYTHON_WRAP.getKey());
    List<String> pythonCompressFilesCmds = new ArrayList<>();
    List<File> commandFiles = new ArrayList<>();

    for (ServiceComponentHostResponse response : componentMap.values()){

      AmbariManagementController managementController = getManagementController();
      ConfigHelper configHelper = managementController.getConfigHelper();
      Cluster cluster = null;
      Clusters clusters = managementController.getClusters();
      try {
        cluster = clusters.getCluster(response.getClusterName());

        String serviceName = response.getServiceName();
        String componentName = response.getComponentName();
        String hostName = response.getHostname();
        String publicHostName = response.getPublicHostname();
        ComponentInfo componentInfo = null;
        String packageFolder = null;

        Service service = cluster.getService(serviceName);
        ServiceComponent component = service.getServiceComponent(componentName);
        StackId stackId = component.getDesiredStackId();

        componentInfo = managementController.getAmbariMetaInfo().
          getComponent(stackId.getStackName(), stackId.getStackVersion(), serviceName, componentName);

        packageFolder = managementController.getAmbariMetaInfo().
          getService(stackId.getStackName(), stackId.getStackVersion(), serviceName).getServicePackageFolder();

        String commandScript = componentInfo.getCommandScript().getScript();
        List<ClientConfigFileDefinition> clientConfigFiles = componentInfo.getClientConfigFiles();

        if (clientConfigFiles == null) {
          if (componentMap.size() == 1) {
            throw new SystemException("No configuration files defined for the component " + componentInfo.getName());
          } else {
            LOG.debug("No configuration files defined for the component {}", componentInfo.getName());
            continue;
          }
        }

        // service component hosts that have configFiles defined in the stack definition of the service
        schWithConfigFiles.add(response);

        if (serviceToComponentMap.containsKey(response.getServiceName())) {
          List <ServiceComponentHostResponse> schResponseList =  serviceToComponentMap.get(serviceName);
          schResponseList.add(response);
        } else {
          List <ServiceComponentHostResponse> schResponseList = new ArrayList<>();
          schResponseList.add(response);
          serviceToComponentMap.put(serviceName,schResponseList);
        }

        String resourceDirPath = configs.getResourceDirPath();
        String packageFolderAbsolute = resourceDirPath + File.separator + packageFolder;

        String commandScriptAbsolute = packageFolderAbsolute + File.separator + commandScript;


        Map<String, Map<String, String>> configurations = new TreeMap<>();
        Map<String, Long> configVersions = new TreeMap<>();
        Map<String, Map<PropertyType, Set<String>>> configPropertiesTypes = new TreeMap<>();
        Map<String, Map<String, Map<String, String>>> configurationAttributes = new TreeMap<>();

        Map<String, DesiredConfig> desiredClusterConfigs = cluster.getDesiredConfigs();

        //Get configurations and configuration attributes
        for (Map.Entry<String, DesiredConfig> desiredConfigEntry : desiredClusterConfigs.entrySet()) {

          String configType = desiredConfigEntry.getKey();
          DesiredConfig desiredConfig = desiredConfigEntry.getValue();
          Config clusterConfig = cluster.getConfig(configType, desiredConfig.getTag());

          if (clusterConfig != null) {
            Map<String, String> props = new HashMap<>(clusterConfig.getProperties());

            // Apply global properties for this host from all config groups
            Map<String, Map<String, String>> allConfigTags = null;
            allConfigTags = configHelper
              .getEffectiveDesiredTags(cluster, schRequest.getHostname());

            Map<String, Map<String, String>> configTags = new HashMap<>();

            for (Map.Entry<String, Map<String, String>> entry : allConfigTags.entrySet()) {
              if (entry.getKey().equals(clusterConfig.getType())) {
                configTags.put(clusterConfig.getType(), entry.getValue());
              }
            }

            Map<String, Map<String, String>> properties = configHelper
              .getEffectiveConfigProperties(cluster, configTags);

          if (!properties.isEmpty()) {
            for (Map<String, String> propertyMap : properties.values()) {
              props.putAll(propertyMap);
            }
          }

            configurations.put(clusterConfig.getType(), props);
            configVersions.put(clusterConfig.getType(), clusterConfig.getVersion());
            configPropertiesTypes.put(clusterConfig.getType(), clusterConfig.getPropertiesTypes());

            Map<String, Map<String, String>> attrs = new TreeMap<>();
            configHelper.cloneAttributesMap(clusterConfig.getPropertiesAttributes(), attrs);

            Map<String, Map<String, Map<String, String>>> attributes = configHelper
              .getEffectiveConfigAttributes(cluster, configTags);
            for (Map<String, Map<String, String>> attributesMap : attributes.values()) {
              configHelper.cloneAttributesMap(attributesMap, attrs);
            }
            configurationAttributes.put(clusterConfig.getType(), attrs);
          }
        }

        ConfigHelper.processHiddenAttribute(configurations, configurationAttributes, componentName, true);

        for (Map.Entry<String, Map<String, Map<String, String>>> configurationAttributesEntry : configurationAttributes.entrySet()) {
          Map<String, Map<String, String>> attrs = configurationAttributesEntry.getValue();
          // remove internal attributes like "hidden"
          attrs.remove("hidden");
        }

        // replace passwords on password references
        for (Map.Entry<String, Map<String, String>> configEntry : configurations.entrySet()) {
          String configType = configEntry.getKey();
          Map<String, String> configProperties = configEntry.getValue();
          Long configVersion = configVersions.get(configType);
          Map<PropertyType, Set<String>> propertiesTypes = configPropertiesTypes.get(configType);
          SecretReference.replacePasswordsWithReferences(propertiesTypes, configProperties, configType, configVersion);
        }

        Map<String, Set<String>> clusterHostInfo = null;
        ServiceInfo serviceInfo = null;
        String osFamily = null;
        clusterHostInfo = StageUtils.getClusterHostInfo(cluster);
        serviceInfo = managementController.getAmbariMetaInfo().getService(stackId.getStackName(),
          stackId.getStackVersion(), serviceName);
        try {
          clusterHostInfo = StageUtils.substituteHostIndexes(clusterHostInfo);
        } catch (AmbariException e) {
          // Before moving substituteHostIndexes to StageUtils, a SystemException was thrown in the
          // event an index could not be mapped to a host.  After the move, this was changed to an
          // AmbariException for consistency in the StageUtils class. To keep this method consistent
          // with how it behaved in the past, if an AmbariException is thrown, it is caught and
          // translated to a SystemException.
          throw new SystemException(e.getMessage(), e);
        }
        osFamily = clusters.getHost(hostName).getOsFamily();

        // Write down os specific info for the service
        ServiceOsSpecific anyOs = null;
        if (serviceInfo.getOsSpecifics().containsKey(AmbariMetaInfo.ANY_OS)) {
          anyOs = serviceInfo.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
        }

        ServiceOsSpecific hostOs = populateServicePackagesInfo(serviceInfo, osFamily);

        // Build package list that is relevant for host
        List<ServiceOsSpecific.Package> packages =
          new ArrayList<>();
        if (anyOs != null) {
          packages.addAll(anyOs.getPackages());
        }

        if (hostOs != null) {
          packages.addAll(hostOs.getPackages());
        }
        String packageList = gson.toJson(packages);

        String jsonConfigurations = null;
        Map<String, Object> commandParams = new HashMap<>();
        List<Map<String, String>> xmlConfigs = new LinkedList<>();
        List<Map<String, String>> envConfigs = new LinkedList<>();
        List<Map<String, String>> propertiesConfigs = new LinkedList<>();

        //Fill file-dictionary configs from metainfo
        for (ClientConfigFileDefinition clientConfigFile : clientConfigFiles) {
          Map<String, String> fileDict = new HashMap<>();
          fileDict.put(clientConfigFile.getFileName(), clientConfigFile.getDictionaryName());
          if (clientConfigFile.getType().equals("xml")) {
            xmlConfigs.add(fileDict);
          } else if (clientConfigFile.getType().equals("env")) {
            envConfigs.add(fileDict);
          } else if (clientConfigFile.getType().equals("properties")) {
            propertiesConfigs.add(fileDict);
          }
        }

        TreeMap<String, String> clusterLevelParams = null;
        TreeMap<String, String> ambariLevelParams = null;
        TreeMap<String, String> topologyCommandParams = new TreeMap<>();
        if (getManagementController() instanceof AmbariManagementControllerImpl){
          AmbariManagementControllerImpl controller = ((AmbariManagementControllerImpl)getManagementController());
          clusterLevelParams = controller.getMetadataClusterLevelParams(cluster, stackId);
          ambariLevelParams = controller.getMetadataAmbariLevelParams();

          Service s = cluster.getService(serviceName);
          ServiceComponent sc = s.getServiceComponent(componentName);
          ServiceComponentHost sch = sc.getServiceComponentHost(response.getHostname());

          topologyCommandParams = controller.getTopologyCommandParams(cluster.getClusterId(), serviceName, componentName, sch);
        }
        TreeMap<String, String> agentLevelParams = new TreeMap<>();
        agentLevelParams.put("hostname", hostName);
        agentLevelParams.put("public_hostname", publicHostName);

        commandParams.put(PACKAGE_LIST, packageList);
        commandParams.put("xml_configs_list", xmlConfigs);
        commandParams.put("env_configs_list", envConfigs);
        commandParams.put("properties_configs_list", propertiesConfigs);
        commandParams.put("output_file", componentName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
        commandParams.putAll(topologyCommandParams);

        Map<String, Object> jsonContent = new TreeMap<>();
        jsonContent.put("configurations", configurations);
        jsonContent.put("configurationAttributes", configurationAttributes);
        jsonContent.put("commandParams", commandParams);
        jsonContent.put("clusterHostInfo", clusterHostInfo);
        jsonContent.put("ambariLevelParams", ambariLevelParams);
        jsonContent.put("clusterLevelParams", clusterLevelParams);
        jsonContent.put("agentLevelParams", agentLevelParams);
        jsonContent.put("hostname", hostName);
        jsonContent.put("public_hostname", publicHostName);
        jsonContent.put("clusterName", cluster.getClusterName());
        jsonContent.put("serviceName", serviceName);
        jsonContent.put("role", componentName);
        jsonContent.put("componentVersionMap", cluster.getComponentVersionMap());

        jsonConfigurations = gson.toJson(jsonContent);

        File tmpDirectory = new File(TMP_PATH);
        if (!tmpDirectory.exists()) {
          try {
            tmpDirectory.mkdirs();
            tmpDirectory.setWritable(true, true);
            tmpDirectory.setReadable(true, true);
          } catch (SecurityException se) {
            throw new SystemException("Failed to get temporary directory to store configurations", se);
          }
        }
        File jsonFile = File.createTempFile(componentName, "-configuration.json", tmpDirectory);
        try {
          jsonFile.setWritable(true, true);
          jsonFile.setReadable(true, true);
        } catch (SecurityException e) {
          throw new SystemException("Failed to set permission", e);
        }

        PrintWriter printWriter = null;
        try {
          printWriter = new PrintWriter(jsonFile.getAbsolutePath());
          printWriter.print(jsonConfigurations);
          printWriter.close();
        } catch (FileNotFoundException e) {
          throw new SystemException("Failed to write configurations to json file ", e);
        }

        String cmd = pythonCmd + " " + commandScriptAbsolute + " generate_configs " + jsonFile.getAbsolutePath() + " " +
          packageFolderAbsolute + " " + TMP_PATH + File.separator + "structured-out.json" + " INFO " + TMP_PATH;

        commandFiles.add(jsonFile);
        pythonCompressFilesCmds.add(cmd);

      } catch (IOException e) {
        throw new SystemException("Controller error ", e);
      }
    }

    if (schWithConfigFiles.isEmpty()) {
      throw new SystemException("No configuration files defined for any component" );
    }

    Integer totalCommands = pythonCompressFilesCmds.size() * 2;
    Integer threadPoolSize = Math.min(totalCommands,configs.getExternalScriptThreadPoolSize());
    ExecutorService processExecutor = Executors.newFixedThreadPool(threadPoolSize);

    // put all threads that starts process to compress each component config files in the executor
    try {
      List<CommandLineThreadWrapper> pythonCmdThreads = executeCommands(processExecutor, pythonCompressFilesCmds);

      // wait for all threads to finish
      Integer timeout = configs.getExternalScriptTimeout();
      waitForAllThreadsToJoin(processExecutor, pythonCmdThreads, timeout);
    } finally {
      for (File each : commandFiles) {
        each.delete();
      }
    }

    if (StringUtils.isEmpty(requestComponentName)) {
      TarUtils tarUtils;
      String fileName;
      List <ServiceComponentHostResponse> schToTarConfigFiles = schWithConfigFiles;
      if (StringUtils.isNotEmpty(requestHostName)) {
        fileName = requestHostName + "(" + Resource.InternalType.Host.toString().toUpperCase()+")";
      } else if (StringUtils.isNotEmpty(requestServiceName)) {
        fileName = requestServiceName + "(" + Resource.InternalType.Service.toString().toUpperCase()+")";
        schToTarConfigFiles = serviceToComponentMap.get(requestServiceName);
      } else {
        fileName = schRequest.getClusterName() + "(" + Resource.InternalType.Cluster.toString().toUpperCase()+")";
      }
      tarUtils = new TarUtils(TMP_PATH, fileName, schToTarConfigFiles);
      tarUtils.tarConfigFiles();
    }

    Resource resource = new ResourceImpl(Resource.Type.ClientConfig);
    resources.add(resource);
    return resources;
  }

  /**
   *  Execute all external script commands
   * @param processExecutor {@link ExecutorService} executes the process when threads are available in the pool
   * @param commandLines List of {String} commands that starts the python process to compress config files
   * @return {@link CommandLineThreadWrapper}
   * @throws SystemException
   */
  private List<CommandLineThreadWrapper> executeCommands(final ExecutorService processExecutor, List<String> commandLines)
    throws SystemException {
    List <CommandLineThreadWrapper> commandLineThreadWrappers = new ArrayList<>();
    try {
      for (String commandLine : commandLines) {
        CommandLineThreadWrapper commandLineThreadWrapper = executeCommand(processExecutor,commandLine);
        commandLineThreadWrappers.add(commandLineThreadWrapper);
      }
    } catch (IOException e) {
      LOG.error("Failed to run generate client configs script for components");
      processExecutor.shutdownNow();
      throw new SystemException("Failed to run generate client configs script for components");
    }
    return  commandLineThreadWrappers;
  }


  /**
   * Execute external script command
   * @param processExecutor {@link ExecutorService} executes the process when threads are available in the pool
   * @param commandLine {String} command that starts the python process to compress config files
   * @return {@link CommandLineThreadWrapper}
   * @throws IOException
   */
  private CommandLineThreadWrapper executeCommand(final ExecutorService processExecutor, final String commandLine)
    throws IOException {
    ProcessBuilder builder = new ProcessBuilder(Arrays.asList(commandLine.split("\\s+")));
    builder.redirectErrorStream(true);
    Process process = builder.start();
    CommandLineThread commandLineThread = new CommandLineThread(process);
    LogStreamReader logStream = new LogStreamReader(process.getInputStream());
    Thread logStreamThread = new Thread(logStream, "LogStreamReader");
    // log collecting thread should be always put first in the executor
    processExecutor.execute(logStreamThread);
    processExecutor.execute(commandLineThread);
    return  new CommandLineThreadWrapper(commandLine, commandLineThread,
      logStreamThread, logStream, process);
  }


  /**
   * Waits for all threads to join that have started python process to tar config files for component
   * @param processExecutor {@link ExecutorService} executes the process when threads are available in the pool
   * @param pythonCmdThreads list of {@link CommandLineThreadWrapper}
   * @param timeout {Integer} time to wait for the threads to join
   * @throws SystemException
   */
  private void waitForAllThreadsToJoin(ExecutorService  processExecutor, List <CommandLineThreadWrapper> pythonCmdThreads, Integer timeout)
    throws SystemException {
    processExecutor.shutdown();
    try {
      processExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
      processExecutor.shutdownNow();
      for (CommandLineThreadWrapper commandLineThreadWrapper: pythonCmdThreads) {
        CommandLineThread commandLineThread = commandLineThreadWrapper.getCommandLineThread();
        try {
          Integer returnCode = commandLineThread.getReturnCode();
          if (returnCode == null) {
            throw new TimeoutException();
          } else if (returnCode != 0) {
            throw new ExecutionException(String.format("Execution of \"%s\" returned %d.", commandLineThreadWrapper.getCommandLine(), returnCode),
              new Throwable(commandLineThreadWrapper.getLogStream().getOutput()));
          }
        } catch (TimeoutException e) {
          LOG.error("Generate client configs script was killed due to timeout ", e);
          throw new SystemException("Generate client configs script was killed due to timeout ", e);
        } catch (ExecutionException e) {
          LOG.error(e.getMessage(), e);
          throw new SystemException(e.getMessage() + " " + e.getCause());
        } finally {
          commandLineThreadWrapper.getProcess().destroy();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      processExecutor.shutdownNow();
      LOG.error("Failed to run generate client configs script for components");
      throw new SystemException("Failed to run generate client configs script for components");
    }
  }


  /**
   * wrapper class that holds all information and references to the thread and python process
   * started to create compressed configuration config files
   */
  private static class CommandLineThreadWrapper {

    private String commandLine;

    private CommandLineThread commandLineThread;

    private Thread logStreamThread;

    private LogStreamReader logStream;

    private Process process;

    private CommandLineThreadWrapper(String commandLine, CommandLineThread commandLineThread,
                                     Thread logStreamThread, LogStreamReader logStream, Process process) {
      this.commandLine = commandLine;
      this.commandLineThread = commandLineThread;
      this.logStreamThread = logStreamThread;
      this.logStream  =  logStream;
      this.process = process;
    }

    /**
     * Returns commandLine that starts pyton process
     * @return {@link #commandLine}
     */
    private String getCommandLine() {
      return commandLine;
    }

    /**
     * Sets {@link #commandLine}
     * @param commandLine {String}
     */
    private void setCommandLine(String commandLine) {
      this.commandLine = commandLine;
    }

    /**
     * Returns thread that starts and waits for python process to complete
     * @return {@link #commandLineThread}
     */
    private CommandLineThread getCommandLineThread() {
      return commandLineThread;
    }

    /**
     * Sets {@link #commandLineThread}
     * @param commandLineThread {@link CommandLineThread}
     */
    private void setCommandLineThread(CommandLineThread commandLineThread) {
      this.commandLineThread = commandLineThread;
    }

    /**
     * Returns thread that starts and waits to get the output and error log stream from the python process
     * @return {@link #logStreamThread}
     */
    private Thread getLogStreamThread() {
      return logStreamThread;
    }

    /**
     * Sets {@link #logStreamThread}
     * @param logStreamThread {@link Thread}
     */
    private void setLogStreamThread(Thread logStreamThread) {
      this.logStreamThread = logStreamThread;
    }

    /**
     * Returns log stream from the python subprocess
     * @return {@link #logStream}
     */
    private LogStreamReader getLogStream() {
      return logStream;
    }

    /**
     * Sets {@link #logStream}
     * @param logStream {@link LogStreamReader}
     */
    private void setLogStream(LogStreamReader logStream) {
      this.logStream = logStream;
    }

    /**
     * Returns python process
     * @return {@link #process}
     */
    private Process getProcess() {
      return process;
    }

    /**
     *  Sets {@link #process}
     * @param process {@link Process}
     */
    private void setProcess(Process process) {
      this.process = process;
    }
  }

  /**
   * Class to run python process to compress config files as seperate thread
   */
  private static class CommandLineThread extends Thread {
    private final Process process;
    private Integer returnCode;

    private void setReturnCode(Integer exit) {
      returnCode = exit;
    }

    private Integer getReturnCode() {
      return returnCode;
    }

    private CommandLineThread(Process process) {
      this.process = process;
    }


    @Override
    public void run() {
      try {
        setReturnCode(process.waitFor());
      } catch (InterruptedException ignore) {
        return;
      }
    }

  }

  /**
   * Class to collect output and error stream of python subprocess
   */
  private class LogStreamReader implements Runnable {

    private BufferedReader reader;
    private StringBuilder output;

    public LogStreamReader(InputStream is) {
      reader = new BufferedReader(new InputStreamReader(is));
      output = new StringBuilder("");
    }

    public String getOutput() {
      return output.toString();
    }

    @Override
    public void run() {
      try {
        String line = reader.readLine();
        while (line != null) {
          output.append(line);
          output.append("\n");
          line = reader.readLine();
        }
        reader.close();
      } catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
  }


  /**
   * This is the utility class to do further compression related operations
   * on already compressed component configuration files
   */
  protected static class TarUtils {

    /**
     * temporary dir where tar files are saved on ambari server
     */
    private String tmpDir;

    /**
     * name of the compressed file that should be created
     */
    private String fileName;


    private List<ServiceComponentHostResponse> serviceComponentHostResponses;

    /**
     * Constructor sets all the fields of the class
     * @param tmpDir {String}
     * @param fileName {String}
     * @param serviceComponentHostResponses {List}
     */
    TarUtils(String tmpDir, String fileName, List<ServiceComponentHostResponse> serviceComponentHostResponses) {
      this.tmpDir = tmpDir;
      this.fileName = fileName;
      this.serviceComponentHostResponses = serviceComponentHostResponses;
    }

    /**
     * creates single compressed file from the list of existing compressed file
     * @throws SystemException
     */
    protected void tarConfigFiles()
      throws SystemException {

      try {
        File compressedOutputFile = new File(tmpDir, fileName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
        FileOutputStream fOut = new FileOutputStream(compressedOutputFile);
        BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
        TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);
        tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

        try {
          for (ServiceComponentHostResponse schResponse : serviceComponentHostResponses) {
            String componentName = schResponse.getComponentName();
            File compressedInputFile = new File(tmpDir, componentName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
            FileInputStream fin = new FileInputStream(compressedInputFile);
            BufferedInputStream bIn = new BufferedInputStream(fin);
            GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bIn);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);
            TarArchiveEntry entry = null;
            try {
              while ((entry = tarIn.getNextTarEntry()) != null) {
                entry.setName(componentName + File.separator + entry.getName());
                tOut.putArchiveEntry(entry);
                if (entry.isFile()) {
                  IOUtils.copy(tarIn, tOut);
                }
                tOut.closeArchiveEntry();
              }
            } catch (Exception e) {
              throw new SystemException(e.getMessage(), e);
            } finally {
              tarIn.close();
              gzIn.close();
              bIn.close();
              fin.close();
            }
          }
        } finally {
          tOut.finish();
          tOut.close();
        }
      } catch (Exception e) {
        throw new SystemException(e.getMessage(), e);
      }
    }
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("The request is not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("The request is not supported");
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties the predicate
   * @return the component request object
   */

  private ServiceComponentHostRequest getRequest(Map<String, Object> properties) {
    return new ServiceComponentHostRequest(
            (String) properties.get(COMPONENT_CLUSTER_NAME_PROPERTY_ID),
            (String) properties.get(COMPONENT_SERVICE_NAME_PROPERTY_ID),
            (String) properties.get(COMPONENT_COMPONENT_NAME_PROPERTY_ID),
            (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
            null);
  }


  protected ServiceOsSpecific populateServicePackagesInfo(ServiceInfo serviceInfo, String osFamily) {
    ServiceOsSpecific hostOs = new ServiceOsSpecific(osFamily);
    List<ServiceOsSpecific> foundedOSSpecifics = getOSSpecificsByFamily(serviceInfo.getOsSpecifics(), osFamily);
    if (!foundedOSSpecifics.isEmpty()) {
      for (ServiceOsSpecific osSpecific : foundedOSSpecifics) {
        hostOs.addPackages(osSpecific.getPackages());
      }
    }

    return hostOs;
  }

  private List<ServiceOsSpecific> getOSSpecificsByFamily(Map<String, ServiceOsSpecific> osSpecifics, String osFamily) {
    List<ServiceOsSpecific> foundedOSSpecifics = new ArrayList<>();
    for (Map.Entry<String, ServiceOsSpecific> osSpecific : osSpecifics.entrySet()) {
      if (osSpecific.getKey().indexOf(osFamily) != -1) {
        foundedOSSpecifics.add(osSpecific.getValue());
      }
    }
    return foundedOSSpecifics;
  }

}
