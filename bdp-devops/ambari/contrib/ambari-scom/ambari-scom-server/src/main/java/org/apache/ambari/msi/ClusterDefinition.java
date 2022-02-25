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

package org.apache.ambari.msi;

import org.apache.ambari.scom.ClusterDefinitionProvider;
import org.apache.ambari.scom.HostInfoProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the cluster created by the MSI.
 */
public class ClusterDefinition {

  private static final String COMMENT_TAG = "#";
  private static final String HA_PROPERTY_INDICATOR = "HA";
  private static Boolean HA_ENABLE = Boolean.FALSE;

  private final Set<String> services = new HashSet<String>();
  private final Set<String> hosts = new HashSet<String>();
  private final Map<String, Set<String>> components = new HashMap<String, Set<String>>();
  private final Map<String, Map<String, Set<String>>> hostComponents = new HashMap<String, Map<String, Set<String>>>();
  private final Map<Integer, StateProvider.Process> processes = new HashMap<Integer, StateProvider.Process>();

  private final Set<Resource> requestResources = new HashSet<Resource>();
  private final Set<Resource> taskResources    = new HashSet<Resource>();


  private final StateProvider stateProvider;
  private final ClusterDefinitionProvider definitionProvider;
  private final HostInfoProvider hostInfoProvider;
  private String clusterName;
  private String versionId;

  private int nextRequestId = 1;
  private int nextTaskId = 1;

  /**
   * Client only components
   */
  private final Set<String> clientOnlyComponents = new HashSet<String>(){{
    add("PIG");
    add("SQOOP");
    add("YARN_CLIENT");
    add("MAPREDUCE2_CLIENT");
  }};

  private Boolean isClientOnlyComponent(String componentName) {
    return clientOnlyComponents.contains(componentName);
  }

  /**
   * Client only services
   */
  private final Set<String> clientOnlyServices = new HashSet<String>(){{
    add("PIG");
    add("SQOOP");
  }};

  private Boolean isClientOnlyService(String serviceName) {
    return clientOnlyServices.contains(serviceName);
  }

  /**
   * Component name mapping to account for differences in what is provided by the MSI
   * and what is expected by the Ambari providers.
   */
  private final Map<String, Set<String>> componentNameMap = new HashMap<String, Set<String>>();

  private void initComponentNameMap() {
    componentNameMap.put("NAMENODE_HOST",           Collections.singleton("NAMENODE"));
    componentNameMap.put("SECONDARY_NAMENODE_HOST", Collections.singleton("SECONDARY_NAMENODE"));
    componentNameMap.put("OOZIE_SERVER_HOST",       Collections.singleton("OOZIE_SERVER"));
    componentNameMap.put("WEBHCAT_HOST",            Collections.singleton("WEBHCAT_SERVER"));
    componentNameMap.put("FLUME_HOSTS",             Collections.singleton("FLUME_SERVER"));
    componentNameMap.put("HBASE_MASTER",            Collections.singleton("HBASE_MASTER"));
    componentNameMap.put("HBASE_REGIONSERVERS",     Collections.singleton("HBASE_REGIONSERVER"));
    componentNameMap.put("ZOOKEEPER_HOSTS",         Collections.singleton("ZOOKEEPER_SERVER"));

    Set<String> slaveComponents = new HashSet<String>();
    slaveComponents.add("DATANODE");
    componentNameMap.put("SLAVE_HOSTS", slaveComponents);

    Set<String> hiveComponents = new HashSet<String>();
    hiveComponents.add("HIVE_SERVER");
    hiveComponents.add("HIVE_METASTORE");
    hiveComponents.add("HIVE_CLIENT");
    componentNameMap.put("HIVE_SERVER_HOST", hiveComponents);

    Integer majorStackVersion = getMajorStackVersion();
    Integer minorStackVersion = getMinorStackVersion();
    if(majorStackVersion != null) {
      if(majorStackVersion == 1) {
        Set<String> mapReduceComponents = new HashSet<String>();
        mapReduceComponents.add("JOBTRACKER");
        mapReduceComponents.add("HISTORYSERVER");
        componentNameMap.put("JOBTRACKER_HOST", mapReduceComponents);

        slaveComponents.add("TASKTRACKER");
      }
      if(majorStackVersion == 2) {
        componentNameMap.put("JOURNALNODE_HOST", Collections.singleton("JOURNALNODE"));
        componentNameMap.put(minorStackVersion > 0 ? "NN_HA_JOURNALNODE_HOSTS" : "HA_JOURNALNODE_HOSTS", Collections.singleton("JOURNALNODE"));

        Set<String> haNamenodeComponents = new HashSet<String>();
        haNamenodeComponents.add("NAMENODE");
        haNamenodeComponents.add("ZKFC");
        componentNameMap.put(minorStackVersion > 0 ? "NN_HA_STANDBY_NAMENODE_HOST" : "HA_NAMENODE_HOST", haNamenodeComponents);

        Set<String> mapReduce2Components = new HashSet<String>();
        mapReduce2Components.add("HISTORYSERVER");
        mapReduce2Components.add("RESOURCEMANAGER");
        componentNameMap.put("RESOURCEMANAGER_HOST", mapReduce2Components);

        componentNameMap.put("RM_HA_STANDBY_RESOURCEMANAGER_HOST", Collections.singleton("RESOURCEMANAGER"));

        slaveComponents.add("NODEMANAGER");
        //hiveComponents.add("MYSQL_SERVER");

        Set<String> clientHosts = new HashSet<String>();
        clientHosts.add("PIG");
        clientHosts.add("SQOOP");
        clientHosts.add("YARN_CLIENT");
        clientHosts.add("MAPREDUCE2_CLIENT");
        componentNameMap.put("CLIENT_HOSTS", clientHosts);
      }
    }
  }

  /**
   * Component service mapping .
   */
  private final Map<String, String> componentServiceMap = new HashMap<String, String>();

  private void initComponentServiceMap() {
    componentServiceMap.put("NAMENODE",           "HDFS");
    componentServiceMap.put("DATANODE",           "HDFS");
    componentServiceMap.put("SECONDARY_NAMENODE", "HDFS");
    componentServiceMap.put("HIVE_SERVER",        "HIVE");
    componentServiceMap.put("HIVE_METASTORE",     "HIVE");
    componentServiceMap.put("HIVE_CLIENT",        "HIVE");
    componentServiceMap.put("OOZIE_SERVER",       "OOZIE");
    componentServiceMap.put("WEBHCAT_SERVER",     "HIVE");
    componentServiceMap.put("FLUME_SERVER",       "FLUME");
    componentServiceMap.put("HBASE_MASTER",       "HBASE");
    componentServiceMap.put("HBASE_REGIONSERVER", "HBASE");
    componentServiceMap.put("ZOOKEEPER_SERVER",   "ZOOKEEPER");

    Integer majorStackVersion = getMajorStackVersion();
    if(majorStackVersion != null) {
      if(majorStackVersion == 1) {
        componentServiceMap.put("JOBTRACKER",         "MAPREDUCE");
        componentServiceMap.put("HISTORYSERVER",     "MAPREDUCE");
        componentServiceMap.put("TASKTRACKER",        "MAPREDUCE");
      }
      if(majorStackVersion == 2) {
        componentServiceMap.put("PIG",                "PIG");
        componentServiceMap.put("SQOOP",              "SQOOP");
        componentServiceMap.put("HISTORYSERVER",      "MAPREDUCE2");
        componentServiceMap.put("MAPREDUCE2_CLIENT",  "MAPREDUCE2");
        componentServiceMap.put("JOURNALNODE",        "HDFS");
        componentServiceMap.put("NODEMANAGER",        "YARN");
        componentServiceMap.put("RESOURCEMANAGER",    "YARN");
        componentServiceMap.put("YARN_CLIENT",        "YARN");
        componentServiceMap.put("ZKFC",               "HDFS");
        //componentServiceMap.put("MYSQL_SERVER",       "HIVE");
      }
    }
  }

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a cluster definition.
   *
   * @param stateProvider  the state provider
   */
  public ClusterDefinition(StateProvider stateProvider,
                           ClusterDefinitionProvider definitionProvider,
                           HostInfoProvider hostInfoProvider) {
    this.stateProvider      = stateProvider;
    this.definitionProvider = definitionProvider;
    this.hostInfoProvider   = hostInfoProvider;
    this.clusterName        = definitionProvider.getClusterName();
    this.versionId          = definitionProvider.getVersionId();

    init();

    try {
      readClusterDefinition();
      haEnableSetup();
    } catch (IOException e) {
      String msg = "Caught exception reading cluster definition file.";
      throw new IllegalStateException(msg, e);
    }
  }

  private void haEnableSetup() {
    if(HA_ENABLE) {
      Map<String, Set<String>> serviceHostComponents = hostComponents.get(componentServiceMap.get("ZKFC"));
      if (serviceHostComponents != null) {
        for(String host : serviceHostComponents.keySet()) {
          Set<String> hostHostComponents = serviceHostComponents.get(host);
          if(hostHostComponents != null && hostHostComponents.contains("NAMENODE")) {
            hostHostComponents.add("ZKFC");
          }
        }
      }
    }
  }


  // ----- ClusterDefinition -------------------------------------------------

  /**
   * Get the major stack version for this cluster.
   *
   * @return the major stack version
   */
  public Integer getMajorStackVersion() {
    if(StringUtils.isNotEmpty(versionId)) {
      String majorVersion = StringUtils.substring(versionId, 4, 5);
      if(StringUtils.isNotEmpty(majorVersion)) {
        return Integer.parseInt(majorVersion);
      }
    }
    return null;
  }

  /**
   * Get the minor stack version for this cluster.
   *
   * @return the minor stack version
   */
  public Integer getMinorStackVersion() {
    if(StringUtils.isNotEmpty(versionId)) {
      String majorVersion = StringUtils.substring(versionId, 6, 7);
      if(StringUtils.isNotEmpty(majorVersion)) {
        return Integer.parseInt(majorVersion);
      }
    }
    return null;
  }

  /**
   * Get the name of the cluster.
   *
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Get the name of the cluster.
   *
   * @return the cluster name
   */
  public String getVersionId() {
    return versionId;
  }

  /**
   * Get the services for the cluster.
   *
   * @return the set of service names
   */
  public Set<String> getServices() {
    return services;
  }

  /**
   * Get the hosts for the cluster.
   *
   * @return the set of hosts names
   */
  public Set<String> getHosts() {
    return hosts;
  }

  /**
   * Get the host info provider associated with this cluster definition.
   *
   * @return the host info provider
   */
  public HostInfoProvider getHostInfoProvider() {
    return hostInfoProvider;
  }

  /**
   * Get the components for the given service.
   *
   * @param service  the service name
   *
   * @return the set of component names for the given service name
   */
  public Set<String> getComponents(String service) {
    Set<String> componentSet = components.get(service);
    return componentSet == null ? Collections.<String>emptySet() : componentSet;
  }

  /**
   * Get the host components for the given service and host.
   *
   * @param service  the service name
   * @param host     the host name
   *
   * @return the set of host component names for the given service and host names
   */
  public Set<String> getHostComponents(String service, String host) {
    Set<String> resultSet = null;
    Map<String, Set<String>> serviceHostComponents = hostComponents.get(service);
    if (serviceHostComponents != null) {
      resultSet = serviceHostComponents.get(host);
    }
    return resultSet == null ? Collections.<String>emptySet() : resultSet;
  }

  /**
   * Get the host state from the given host name.
   *
   * @param hostName  the host name
   *
   * @return the host state
   */
  public String getHostState(String hostName) {
    for (Map.Entry<String, Map<String, Set<String>>> entry : hostComponents.entrySet()) {
      Map<String, Set<String>> serviceHostComponents = entry.getValue();
      for (Map.Entry<String, Set<String>> hostEntry : serviceHostComponents.entrySet()) {
        if (hostEntry.getKey().equals(hostName)) {
          Set<String> componentNames = hostEntry.getValue();
          for (String componentName : componentNames) {
            if (isClientOnlyComponent(componentName)) continue;
            if (stateProvider.getRunningState(hostName, componentName) != StateProvider.State.Running) {
              return "UNHEALTHY";
            }
          }
        }
      }
    }
    return "HEALTHY";
  }

  /**
   * Get the service state from the given service name.
   *
   * @param serviceName  the service name
   *
   * @return the service state
   */
  public String getServiceState(String serviceName) {
    if (isClientOnlyService(serviceName)) return "INSTALLED";
    Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
    if (serviceHostComponents != null) {

      for (Map.Entry<String, Set<String>> entry : serviceHostComponents.entrySet()) {
        String      hostName       = entry.getKey();
        Set<String> componentNames = entry.getValue();

        for (String componentName : componentNames) {
          if (isClientOnlyComponent(componentName)) continue;
          if (stateProvider.getRunningState(hostName, componentName) != StateProvider.State.Running) {
            return "INSTALLED";
          }
        }
      }
    }

    return "STARTED";
  }

  /**
   * Set the service state for the given service name.
   *
   * @param serviceName  the service name
   *
   * @return the request id
   */
  public int setServiceState(String serviceName, String state) {
    StateProvider.State s = state.equals("STARTED") ? StateProvider.State.Running :
        state.equals("INSTALLED") ? StateProvider.State.Stopped : StateProvider.State.Unknown;

    int requestId = -1;
    if (!isClientOnlyService(serviceName)) {
      // if the state is already set to the desired state or state is unknown then skip it
      if (s != StateProvider.State.Unknown && !state.equals(getServiceState(serviceName))) {
        Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
        if (serviceHostComponents != null) {

          for (Map.Entry<String, Set<String>> entry : serviceHostComponents.entrySet()) {
            String      hostName       = entry.getKey();
            Set<String> componentNames = entry.getValue();

            for (String componentName : componentNames) {
              if(isClientOnlyComponent(componentName) || state.equals(getHostComponentState(hostName, componentName))) continue;
              requestId = recordProcess(stateProvider.setRunningState(hostName, componentName, s), requestId,
                      "Set service " + serviceName + " state to " + s);
            }
          }
        }
      }
    }
    return requestId;
  }

  /**
   * Get the component state from the give service name and component name.
   *
   * @param serviceName    the service name
   * @param componentName  the component name
   *
   * @return the component state
   */
  public String getComponentState(String serviceName, String componentName) {
    Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
    if (serviceHostComponents != null) {

      for (Map.Entry<String, Set<String>> entry : serviceHostComponents.entrySet()) {
        String      hostName       = entry.getKey();
        Set<String> componentNames = entry.getValue();

        for (String name : componentNames) {
          if (name.equals(componentName)) {
            if (isClientOnlyComponent(componentName)) return "INSTALLED";
            if (stateProvider.getRunningState(hostName, componentName) != StateProvider.State.Running) {
              return "INSTALLED";
            }
          }
        }
      }
    }

    return "STARTED";
  }

  /**
   * Set the component state for the given service name.
   *
   * @param serviceName    the service name
   * @param componentName  the component name
   * @param state          the state
   *
   * @return the request id
   */
  public int setComponentState(String serviceName, String componentName, String state) {
    StateProvider.State s = state.equals("STARTED") ? StateProvider.State.Running :
        state.equals("INSTALLED") ? StateProvider.State.Stopped : StateProvider.State.Unknown;

    int requestId = -1;
    if (!isClientOnlyComponent(componentName)) {
      if (s != StateProvider.State.Unknown) {
        Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
        if (serviceHostComponents != null) {

          for (Map.Entry<String, Set<String>> entry : serviceHostComponents.entrySet()) {
            String      hostName       = entry.getKey();
            Set<String> componentNames = entry.getValue();

            for (String name : componentNames) {
              if (name.equals(componentName)) {
                if(state.equals(getHostComponentState(hostName, componentName))) continue;
                requestId = recordProcess(stateProvider.setRunningState(hostName, componentName, s), requestId,
                        "Set component " + componentName + " state to " + s);
              }
            }
          }
        }
      }
    }
    return requestId;
  }

  /**
    * Get the host component state from the given host name and component name.
    *
    * @param hostName       the host name
    * @param componentName  the component name
    *
    * @return the host component state
    */
  public String getHostComponentState(String hostName, String componentName) {
    Boolean healthy = Boolean.FALSE;
    if (!isClientOnlyComponent(componentName))
      healthy = stateProvider.getRunningState(hostName, componentName) == StateProvider.State.Running;
    return healthy ? "STARTED" : "INSTALLED";
  }

  /**
   * Set the host component state for the given host name and component name.
   *
   * @param hostName       the host name
   * @param componentName  the component name
   *
   * @return the request id
   */
  public int setHostComponentState(String hostName, String componentName, String state) {
    StateProvider.State s = state.equals("STARTED") ? StateProvider.State.Running :
        state.equals("INSTALLED") ? StateProvider.State.Stopped : StateProvider.State.Unknown;

    int requestId = -1;
    if (!isClientOnlyComponent(componentName)) {
      if (s != StateProvider.State.Unknown && !state.equals(getHostComponentState(hostName, componentName))) {
        requestId = recordProcess(stateProvider.setRunningState(hostName, componentName, s), -1,
                "Set host component " + componentName + " state to " + s);
      }
    }
    return requestId;
  }

  /**
   * Return the process that is associated with the given id.
   *
   * @param id  the id
   *
   * @return the process
   */
  public StateProvider.Process getProcess(Integer id) {
    return processes.get(id);
  }

  /**
   * Get the set of request resources.
   *
   * @return the set of request resources
   */
  public Set<Resource> getRequestResources() {
    return requestResources;
  }

  /**
   * Get the set of task resources
   *
   * @return the set of task resources
   */
  public Set<Resource> getTaskResources() {
    return taskResources;
  }


  // ----- helper methods ----------------------------------------------------

  // initialize
  private void init() {
    initComponentNameMap();
    initComponentServiceMap();
  }

  // record a process and create the corresponding request and task resource
  private synchronized int recordProcess(StateProvider.Process process, int requestId, String context) {

    if (requestId == -1) {
      requestId = nextRequestId++;

      Resource request = new ResourceImpl(Resource.Type.Request);

      request.setProperty(RequestProvider.REQUEST_ID_PROPERTY_ID, requestId);
      request.setProperty(RequestProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName);
      request.setProperty(RequestProvider.REQUEST_CONTEXT_ID, context);

      requestResources.add(request);
    }

    Resource task = new ResourceImpl(Resource.Type.Task);
    int taskId = nextTaskId++;

    taskResources.add(task);

    task.setProperty(TaskProvider.TASK_ID_PROPERTY_ID, taskId);
    task.setProperty(TaskProvider.TASK_REQUEST_ID_PROPERTY_ID, requestId);
    task.setProperty(TaskProvider.TASK_CLUSTER_NAME_PROPERTY_ID, clusterName);

    processes.put(taskId, process);
    return requestId;
  }

  /**
   * Read the MSI cluster definition file.
   */
  private void readClusterDefinition() throws IOException {

    InputStream is = definitionProvider.getInputStream();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      String  line;
      while ((line = br.readLine()) != null) {
        line = line.trim();

        if (line.startsWith(COMMENT_TAG)) continue;

        int i = line.indexOf('=');
        if (i == -1) continue;

        String propertyName = line.substring(0, i);
        String propertyValue = line.substring(i + 1);

        if(propertyName.equalsIgnoreCase(HA_PROPERTY_INDICATOR)) {
          HA_ENABLE = propertyValue.equalsIgnoreCase("YES") ? Boolean.TRUE : Boolean.FALSE;
        }

        Set<String> componentNames = componentNameMap.get(propertyName);
        if (componentNames != null) {
          for (String componentName : componentNames) {
            String serviceName = componentServiceMap.get(componentName);
            services.add(serviceName);

            Set<String> serviceComponents = components.get(serviceName);
            if (serviceComponents == null) {
              serviceComponents = new HashSet<String>();
              components.put(serviceName, serviceComponents);
            }
            serviceComponents.add(componentName);

            Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
            if (serviceHostComponents == null) {
              serviceHostComponents = new HashMap<String, Set<String>>();
              hostComponents.put(serviceName, serviceHostComponents);
            }

            String[] hostNames = propertyValue.split(",");
            for (String hostName : hostNames) {
              hostName = hostName.trim();
              Set<String> hostHostComponents = serviceHostComponents.get(hostName);
              if (hostHostComponents == null) {
                hostHostComponents = new HashSet<String>();
                serviceHostComponents.put(hostName, hostHostComponents);
              }
              hostHostComponents.add(componentName);

              hosts.add(hostName);
            }
          }
        }
      }
    } finally {
      is.close();
    }
  }
}
