#!/usr/bin/env ambari-python-wrap
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import socket

class StackAdvisor(object):
  """
  Abstract class implemented by all stack advisors. Stack advisors advise on stack specific questions. 

  Currently stack advisors provide following abilities:
  - Recommend where services should be installed in cluster
  - Recommend configurations based on host hardware
  - Validate user selection of where services are installed on cluster
  - Validate user configuration values 

  Each of the above methods is passed in parameters about services and hosts involved as described below.

    @type services: dictionary
    @param services: Dictionary containing all information about services selected by the user. 
      Example: {
      "services": [
        {
          "StackServices": {
            "service_name" : "HDFS",
            "service_version" : "2.6.0.2.2",
          },
          "components" : [ 
            {
              "StackServiceComponents" : {
                "cardinality" : "1+",
                "component_category" : "SLAVE",
                "component_name" : "DATANODE",
                "display_name" : "DataNode",
                "service_name" : "HDFS",
                "hostnames" : []
              },
              "dependencies" : []
            }, {
              "StackServiceComponents" : {
                "cardinality" : "1-2",
                "component_category" : "MASTER",
                "component_name" : "NAMENODE",
                "display_name" : "NameNode",
                "service_name" : "HDFS",
                "hostnames" : []
              },
              "dependencies" : []
            },
            ...
          ]
        },
        ...
      ]
    }
  @type hosts: dictionary
  @param hosts: Dictionary containing all information about hosts in this cluster
    Example: {
      "items": [
        {
          Hosts: {
            "host_name": "c6401.ambari.apache.org",
            "public_host_name" : "c6401.ambari.apache.org",
            "ip": "192.168.1.101",
            "cpu_count" : 1,
            "disk_info" : [
              {
              "available" : "4564632",
              "used" : "5230344",
              "percent" : "54%",
              "size" : "10319160",
              "type" : "ext4",
              "mountpoint" : "/"
              },
              {
              "available" : "1832436",
              "used" : "0",
              "percent" : "0%",
              "size" : "1832436",
              "type" : "tmpfs",
              "mountpoint" : "/dev/shm"
              }
            ],
            "host_state" : "HEALTHY",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "total_mem" : 3664872
          }
        },
        ...
      ]
    }

    Each of the methods can either return recommendations or validations.

    Recommendations are made in a Ambari Blueprints friendly format. 
    Validations are an array of validation objects.
  """

  def recommendComponentLayout(self, services, hosts):
    """
    Returns recommendation of which hosts various service components should be installed on.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to generate hostname assignments to various components
    of each service.

    @type services: dictionary
    @param services: Dictionary containing all information about services selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example: {
          "resources" : [
            {
              "hosts" : [
                "c6402.ambari.apache.org",
                "c6401.ambari.apache.org"
              ],
              "services" : [
                "HDFS"
              ],
              "recommendations" : {
                "blueprint" : {
                  "host_groups" : [
                    {
                      "name" : "host-group-2",
                      "components" : [
                        { "name" : "JOURNALNODE" },
                        { "name" : "ZKFC" },
                        { "name" : "DATANODE" },
                        { "name" : "SECONDARY_NAMENODE" }
                      ]
                    },
                    {
                      "name" : "host-group-1",
                      "components" : [
                        { "name" : "HDFS_CLIENT" },
                        { "name" : "NAMENODE" },
                        { "name" : "JOURNALNODE" },
                        { "name" : "ZKFC" },
                        { "name" : "DATANODE" }
                      ]
                    }
                  ]
                },
                "blueprint_cluster_binding" : {
                  "host_groups" : [
                    {
                      "name" : "host-group-1",
                      "hosts" : [ { "fqdn" : "c6401.ambari.apache.org" } ]
                    },
                    {
                      "name" : "host-group-2",
                      "hosts" : [ { "fqdn" : "c6402.ambari.apache.org" } ]
                    }
                  ]
                }
              }
            }
          ]
        }
    """
    pass

  def validateComponentLayout(self, services, hosts):
    """
    Returns array of Validation issues with service component layout on hosts

    This function takes as input all details about services being installed along with
    hosts the components are being installed on (hostnames property is populated for 
    each component).  

    @type services: dictionary
    @param services: Dictionary containing information about services and host layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
          "items": [
            {
              "type" : "host-group",
              "level" : "ERROR",
              "message" : "NameNode and Secondary NameNode should not be hosted on the same machine",
              "component-name" : "NAMENODE",
              "host" : "c6401.ambari.apache.org" 
            },
            ...
          ]
        }  
    """
    pass

  def recommendConfigurations(self, services, hosts):
    """
    Returns recommendation of service configurations based on host-specific layout of components.

    This function takes as input all details about services being installed, and hosts
    they are being installed into, to recommend host-specific configurations.

    @type services: dictionary
    @param services: Dictionary containing all information about services and component layout selected by the user.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Layout recommendation of service components on cluster hosts in Ambari Blueprints friendly format. 
        Example: {
         "services": [
          "HIVE", 
          "TEZ", 
          "YARN"
         ], 
         "recommendations": {
          "blueprint": {
           "host_groups": [], 
           "configurations": {
            "yarn-site": {
             "properties": {
              "yarn.scheduler.minimum-allocation-mb": "682", 
              "yarn.scheduler.maximum-allocation-mb": "2048", 
              "yarn.nodemanager.resource.memory-mb": "2048"
             }
            }, 
            "tez-site": {
             "properties": {
              "tez.am.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC", 
              "tez.am.resource.memory.mb": "682"
             }
            }, 
            "hive-site": {
             "properties": {
              "hive.tez.container.size": "682", 
              "hive.tez.java.opts": "-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC", 
              "hive.auto.convert.join.noconditionaltask.size": "238026752"
             }
            }
           }
          }, 
          "blueprint_cluster_binding": {
           "host_groups": []
          }
         }, 
         "hosts": [
          "c6401.ambari.apache.org", 
          "c6402.ambari.apache.org", 
          "c6403.ambari.apache.org" 
         ] 
        }
    """
    pass

  def validateConfigurations(self, services, hosts):
    """"
    Returns array of Validation issues with configurations provided by user

    This function takes as input all details about services being installed along with
    configuration values entered by the user. These configurations can be validated against
    service requirements, or host hardware to generate validation issues. 

    @type services: dictionary
    @param services: Dictionary containing information about services and user configurations.
    @type hosts: dictionary
    @param hosts: Dictionary containing all information about hosts in this cluster
    @rtype: dictionary
    @return: Dictionary containing array of validation items
        Example: {
         "items": [
          {
           "config-type": "yarn-site", 
           "message": "Value is less than the recommended default of 682", 
           "type": "configuration", 
           "config-name": "yarn.scheduler.minimum-allocation-mb", 
           "level": "WARN"
          }
         ]
       }
    """
    pass






class DefaultStackAdvisor(StackAdvisor):
  """
  Default stack advisor implementation.
  
  This implementation is used when a stack-version, or its hierarchy does not
  have an advisor. Stack-versions can extend this class to provide their own
  implement
  """

  def recommendComponentLayout(self, services, hosts):
    """Returns Services object with hostnames array populated for components"""

    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

    layoutRecommendations = self.createComponentLayoutRecommendations(services, hosts)

    recommendations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "hosts": hostsList,
      "services": servicesList,
      "recommendations": layoutRecommendations
    }

    return recommendations

  def createComponentLayoutRecommendations(self, services, hosts):

    recommendations = {
      "blueprint": {
        "host_groups": [ ]
      },
      "blueprint_cluster_binding": {
        "host_groups": [ ]
      }
    }

    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]

    hostsComponentsMap = {}
    for hostName in hostsList:
      if hostName not in hostsComponentsMap:
        hostsComponentsMap[hostName] = []

    #extend 'hostsComponentsMap' with MASTER components
    for service in services["services"]:
      masterComponents = [component for component in service["components"] if self.isMasterComponent(component)]
      for component in masterComponents:
        componentName = component["StackServiceComponents"]["component_name"]

        if self.isComponentHostsPopulated(component):
          hostsForComponent = component["StackServiceComponents"]["hostnames"]
        else:
          availableHosts = hostsList
          if len(hostsList) > 1 and self.isComponentNotPreferableOnAmbariServerHost(component):
            availableHosts = [hostName for hostName in hostsList if not self.isLocalHost(hostName)]

          if self.isMasterComponentWithMultipleInstances(component):
            hostsCount = self.getMinComponentCount(component, hosts)
            if hostsCount > 1: # get first 'hostsCount' available hosts
              if len(availableHosts) < hostsCount:
                hostsCount = len(availableHosts)
              hostsForComponent = availableHosts[:hostsCount]
            else:
              hostsForComponent = [self.getHostForComponent(component, availableHosts)]
          else:
            hostsForComponent = [self.getHostForComponent(component, availableHosts)]

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          hostsComponentsMap[hostName].append( { "name":componentName } )

    #extend 'hostsComponentsMap' with Slave and Client Components
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    usedHostsListList = [component["StackServiceComponents"]["hostnames"] for component in componentsList if not self.isComponentNotValuable(component)]
    utilizedHosts = [item for sublist in usedHostsListList for item in sublist]
    freeHosts = [hostName for hostName in hostsList if hostName not in utilizedHosts]

    for service in services["services"]:
      slaveClientComponents = [component for component in service["components"]
                               if self.isSlaveComponent(component) or self.isClientComponent(component)]
      for component in slaveClientComponents:
        componentName = component["StackServiceComponents"]["component_name"]

        if self.isComponentHostsPopulated(component):
          hostsForComponent = component["StackServiceComponents"]["hostnames"]
        elif component["StackServiceComponents"]["cardinality"] == "ALL":
          hostsForComponent = hostsList
        else:
          if len(freeHosts) == 0:
            hostsForComponent = hostsList[-1:]
          else: # len(freeHosts) >= 1
            hostsForComponent = freeHosts
            if self.isClientComponent(component):
              hostsForComponent = freeHosts[0:1]

        #extend 'hostsComponentsMap' with 'hostsForComponent'
        for hostName in hostsForComponent:
          if hostName not in hostsComponentsMap:
            hostsComponentsMap[hostName] = []
          hostsComponentsMap[hostName].append( { "name": componentName } )

    #prepare 'host-group's from 'hostsComponentsMap'
    host_groups = recommendations["blueprint"]["host_groups"]
    bindings = recommendations["blueprint_cluster_binding"]["host_groups"]
    index = 0
    for key in hostsComponentsMap.keys():
      index += 1
      host_group_name = "host-group-{0}".format(index)
      host_groups.append( { "name": host_group_name, "components": hostsComponentsMap[key] } )
      bindings.append( { "name": host_group_name, "hosts": [{ "fqdn": socket.getfqdn(key) }] } )

    return recommendations
  pass

  def createValidationResponse(self, services, validationItems):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]

    validations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "items": validationItems
    }

    return validations

  def validateComponentLayout(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    validationItems = self.getComponentLayoutValidations(services, hosts)
    return self.createValidationResponse(services, validationItems)

  def validateConfigurations(self, services, hosts):
    """Returns array of Validation objects about issues with hostnames components assigned to"""
    validationItems = self.getConfigurationsValidationItems(services, hosts)
    return self.createValidationResponse(services, validationItems)

  def getComponentLayoutValidations(self, services, hosts):
    return []

  def getConfigurationClusterSummary(self, servicesList, hosts, components):
    pass

  def getConfigurationsValidationItems(self, services, hosts):
    return []

  def recommendConfigurations(self, services, hosts):
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]
    components = [component["StackServiceComponents"]["component_name"]
                  for service in services["services"]
                  for component in service["components"]]

    clusterSummary = self.getConfigurationClusterSummary(servicesList, hosts, components)

    recommendations = {
      "Versions": {"stack_name": stackName, "stack_version": stackVersion},
      "hosts": hostsList,
      "services": servicesList,
      "recommendations": {
        "blueprint": {
          "configurations": {},
          "host_groups": []
        },
        "blueprint_cluster_binding": {
          "host_groups": []
        }
      }
    }

    configurations = recommendations["recommendations"]["blueprint"]["configurations"]

    for service in servicesList:
      calculation = self.getServiceConfigurationRecommender(service)
      if calculation is not None:
        calculation(configurations, clusterSummary)

    return recommendations

  def getServiceConfigurationRecommender(self, service):
    return self.getServiceConfigurationRecommenderDict().get(service, None)

  def getServiceConfigurationRecommenderDict(self):
    return {}

  # Recommendation helper methods
  def isComponentHostsPopulated(self, component):
    hostnames = self.getComponentAttribute(component, "hostnames")
    if hostnames is not None:
      return len(hostnames) > 0
    return False

  def isClientComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'CLIENT'

  def isSlaveComponent(self, component):
    return self.getComponentAttribute(component, "component_category") == 'SLAVE'

  def isMasterComponent(self, component):
    return self.getComponentAttribute(component, "is_master")

  def getComponentAttribute(self, component, attribute):
    serviceComponent = component.get("StackServiceComponents", None)
    if serviceComponent is None:
      return None
    return serviceComponent.get(attribute, None)

  def isLocalHost(self, hostName):
    return socket.getfqdn(hostName) == socket.getfqdn()

  def isMasterComponentWithMultipleInstances(self, component):
    componentName = self.getComponentName(component)
    masters = self.getMastersWithMultipleInstances()
    return componentName in masters

  def isComponentNotValuable(self, component):
    componentName = self.getComponentName(component)
    service = self.getNotValuableComponents()
    return componentName in service

  def getMinComponentCount(self, component, hosts):
    componentName = self.getComponentName(component)
    return self.getComponentCardinality(componentName, hosts)["min"]

  # Helper dictionaries
  def getComponentCardinality(self, componentName, hosts):
    return self.getCardinalitiesDict(hosts).get(componentName, {"min": 1, "max": 1})

  def getHostForComponent(self, component, hostsList):
    componentName = self.getComponentName(component)

    if len(hostsList) != 1:
      scheme = self.getComponentLayoutScheme(componentName)
      if scheme is not None:
        for key in scheme.keys():
          if isinstance(key, ( int, long )):
            if len(hostsList) < key:
              return hostsList[scheme[key]]
        return hostsList[scheme['else']]
    return hostsList[0]

  def getComponentLayoutScheme(self, componentName):
    """
    Provides a scheme for laying out given component on different number of hosts.
    """
    return self.getComponentLayoutSchemes().get(componentName, None)

  def getComponentName(self, component):
    return self.getComponentAttribute(component, "component_name")

  def isComponentNotPreferableOnAmbariServerHost(self, component):
    componentName = self.getComponentName(component)
    service = self.getNotPreferableOnServerComponents()
    return componentName in service

  def getMastersWithMultipleInstances(self):
    return []

  def getNotValuableComponents(self):
    return []

  def getNotPreferableOnServerComponents(self):
    return []

  def getCardinalitiesDict(self, hosts):
    return {}

  def getComponentLayoutSchemes(self):
    """
    Provides layout scheme dictionaries for components.

    The scheme dictionary basically maps the number of hosts to
    host index where component should exist.
    """
    return {}
