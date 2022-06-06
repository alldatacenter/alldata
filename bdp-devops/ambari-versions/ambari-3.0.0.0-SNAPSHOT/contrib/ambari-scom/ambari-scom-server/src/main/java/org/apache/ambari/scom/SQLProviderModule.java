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

package org.apache.ambari.scom;

import org.apache.ambari.msi.AbstractResourceProvider;
import org.apache.ambari.msi.ClusterDefinition;
import org.apache.ambari.msi.StateProvider;
import org.apache.ambari.scom.utilities.SCOMMetricHelper;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.DefaultProviderModule;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider module used to install PropertyProviders required for ambari-scom.
 */
public class SQLProviderModule extends DefaultProviderModule implements HostInfoProvider, StateProvider {
  private final ClusterDefinition clusterDefinition;

  // TODO : these elements should be injected...
  private final ConnectionFactory connectionFactory = SinkConnectionFactory.instance();
  private final ComponentSSLConfiguration sslConfiguration = ComponentSSLConfiguration.instance();
  private final URLStreamProvider urlStreamProvider = new URLStreamProvider(5000, 10000,
          sslConfiguration.getTruststorePath(), sslConfiguration.getTruststorePassword(), sslConfiguration.getTruststoreType());


  // ----- Constants ---------------------------------------------------------

  private static Map<String, String> serviceNames = new HashMap<String, String>();

  private void initServiceNames() {
    Integer majorStackVersion = clusterDefinition.getMajorStackVersion();
    Integer minorStackVersion = clusterDefinition.getMinorStackVersion();
    if(majorStackVersion != null) {
      serviceNames.put("HIVE_SERVER", majorStackVersion == 1 ? "hiveserver" : "hiveserver2");
      if(minorStackVersion != null) {
        serviceNames.put("HISTORYSERVER", majorStackVersion > 1 && minorStackVersion > 0 ? "jobhistoryserver" : "historyserver");
      }
    }
    serviceNames.put("NAMENODE", "namenode");
    serviceNames.put("SECONDARY_NAMENODE", "secondarynamenode");
    serviceNames.put("JOBTRACKER", "jobtracker");
    serviceNames.put("HIVE_METASTORE", "metastore");
    serviceNames.put("HIVE_CLIENT", "hwi");
    serviceNames.put("OOZIE_SERVER", "oozieservice");
    serviceNames.put("FLUME_SERVER", "flumagent");
    serviceNames.put("HBASE_MASTER", "master");
    serviceNames.put("HBASE_REGIONSERVER", "regionserver");
    serviceNames.put("ZOOKEEPER_SERVER", "zkServer");
    serviceNames.put("ZKFC", "zkfc");
    serviceNames.put("DATANODE", "datanode");
    serviceNames.put("TASKTRACKER", "tasktracker");
    serviceNames.put("WEBHCAT_SERVER", "templeton");
    serviceNames.put("NODEMANAGER", "nodemanager");
    serviceNames.put("RESOURCEMANAGER", "resourcemanager");
    serviceNames.put("JOURNALNODE", "journalnode");
  }

  private static final String STATE_PREFIX = "STATE              : ";


  // ----- Constructor -------------------------------------------------------

  public SQLProviderModule() {
    clusterDefinition = new ClusterDefinition(this, ClusterDefinitionProvider.instance(), this);
    initServiceNames();
  }


  // ----- AbstractProviderModule --------------------------------------------

  @Override
  protected ResourceProvider createResourceProvider(Resource.Type type) {
    return AbstractResourceProvider.getResourceProvider(type, clusterDefinition);
  }

  @Override
  protected void createPropertyProviders(Resource.Type type) {

    List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

    if (type.equals(Resource.Type.Component)) {
      providers.add(new JMXPropertyProvider(
              SCOMMetricHelper.getJMXPropertyIds(type),
              urlStreamProvider,
              this,
              this,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
              null,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"),
              PropertyHelper.getPropertyId("ServiceComponentInfo", "state")));

      providers.add(new SQLPropertyProvider(
              SCOMMetricHelper.getSqlServerPropertyIds(type),
              this,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name"),
              null,
              PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"),
              PropertyHelper.getPropertyId("ServiceComponentInfo", "service_name"),
              connectionFactory));
    } else if (type.equals(Resource.Type.HostComponent)) {
      providers.add(new JMXPropertyProvider(
              SCOMMetricHelper.getJMXPropertyIds(type),
              urlStreamProvider,
              this,
              this,
              PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
              PropertyHelper.getPropertyId("HostRoles", "host_name"),
              PropertyHelper.getPropertyId("HostRoles", "component_name"),
              PropertyHelper.getPropertyId("HostRoles", "state")));

      providers.add(new SQLPropertyProvider(
              SCOMMetricHelper.getSqlServerPropertyIds(type),
              this,
              PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
              PropertyHelper.getPropertyId("HostRoles", "host_name"),
              PropertyHelper.getPropertyId("HostRoles", "component_name"),
              PropertyHelper.getPropertyId("HostRoles", "service_name"),
              connectionFactory));
    }
    putPropertyProviders(type, providers);
  }

  // ----- HostProvider ------------------------------------------------------

  @Override
  public String getHostName(String clusterName, String componentName) throws SystemException {
    return getClusterNodeName(super.getHostName(clusterName, componentName));
  }

  @Override
  public Set<String> getHostNames(String clusterName, String componentName) {
    return super.getHostNames(clusterName, componentName);
  }

  @Override
  public String getHostName(String id) throws SystemException {
    return getClusterNodeName(id);
  }

  @Override
  public String getHostAddress(String id) throws SystemException {
    return getClusterHostAddress(id);
  }


  // ----- StateProvider -----------------------------------------------------

  @Override
  public State getRunningState(String hostName, String componentName) {
    String serviceName = getServiceName(componentName);
    if (serviceName != null) {
      String[] cmdStrings = {"sc", "\\\\" + hostName, "query", "\"" + serviceName + "\""}; // Windows specific command

      java.lang.Process process = runProcess(cmdStrings);

      if (process.exitValue() == 0) {

        String response = getProcessResponse(process.getInputStream());

        int i = response.indexOf(STATE_PREFIX);
        if (i >= 0) {
          int state = Integer.parseInt(response.substring(i + STATE_PREFIX.length(), i + STATE_PREFIX.length() + 1));
          switch (state) {
            case (1): // service stopped
              return State.Stopped;
            case (4): // service started
              return State.Running;
          }
        }
      }
    }
    return State.Unknown;
  }

  @Override
  public Process setRunningState(String hostName, String componentName, State state) {
    String serviceName = getServiceName(componentName);
    if (serviceName != null) {
      String command = state == State.Running ? "start" : "stop";
      String[] cmdStrings = {"sc", "\\\\" + hostName, command, "\"" + serviceName + "\""};  // Windows specific command

      return new StateProcess(runProcess(cmdStrings));
    }
    return null;
  }


  // ----- utility methods ---------------------------------------------------

  // get the hostname
  private String getClusterNodeName(String hostname) throws SystemException {
    try {
      if (hostname.equalsIgnoreCase("localhost")) {
        return InetAddress.getLocalHost().getCanonicalHostName();
      }
      return InetAddress.getByName(hostname).getCanonicalHostName();
    } catch (Exception e) {
      throw new SystemException("Error getting hostname.", e);
    }
  }

  // get the hostname
  private String getClusterHostAddress(String hostname) throws SystemException {
    try {
      if (hostname.equalsIgnoreCase("localhost")) {
        return InetAddress.getLocalHost().getHostAddress();
      }
      return InetAddress.getByName(hostname).getHostAddress();
    } catch (Exception e) {
      throw new SystemException("Error getting ip address.", e);
    }
  }

  // get the Windows service name from the given component name
  private String getServiceName(String componentName) {
    return serviceNames.get(componentName);
  }

  // run a process specified by the given command strings
  private java.lang.Process runProcess(String... commands) {
    Runtime runtime = Runtime.getRuntime();
    java.lang.Process process;
    try {
      process = runtime.exec(commands);

      process.waitFor();
    } catch (Exception e) {
      return null;
    }
    return process;
  }

  // get the response text from a completed process stream
  private static String getProcessResponse(InputStream stream) {

    StringBuilder sb = new StringBuilder();
    BufferedReader stdInput = new BufferedReader(new InputStreamReader(stream));

    try {

      String line;

      while ((line = stdInput.readLine()) != null) {
        sb.append(line);
      }

    } catch (Exception e) {
      return null;
    }
    return sb.toString();
  }


  // ----- inner class : StateProcess ----------------------------------------

  public static class StateProcess implements Process {
    private final java.lang.Process process;
    private String output = null;
    private String error = null;

    public StateProcess(java.lang.Process process) {
      this.process = process;
    }

    @Override
    public boolean isRunning() {
      try {
        process.exitValue();
      } catch (IllegalThreadStateException e) {
        return true;
      }
      return false;
    }

    @Override
    public int getExitCode() {
      return process.exitValue();
    }

    @Override
    public String getOutput() {
      if (output != null) {
        return output;
      }

      String processResponse = getProcessResponse(process.getInputStream());

      if (!isRunning()) {
        output = processResponse;
      }

      return processResponse;
    }

    @Override
    public String getError() {
      if (error != null) {
        return error;
      }

      String processResponse = getProcessResponse(process.getErrorStream());

      if (!isRunning()) {
        error = processResponse;
      }

      return processResponse;
    }
  }
}
