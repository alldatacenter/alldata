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
import os
import fnmatch
import imp
import socket
import sys
import traceback

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"

class MICROSOFT_R_SERVER805ServiceAdvisor(service_advisor.ServiceAdvisor):

  def colocateService(self, hostsComponentsMap, serviceComponents):
    # colocate R_NODE_CLIENT with NODEMANAGERs and YARN_CLIENTs
    rClientComponent = [component for component in serviceComponents if component["StackServiceComponents"]["component_name"] == "MICROSOFT_R_NODE_CLIENT"]
    traceback.print_tb(None)
    rClientComponent = rClientComponent[0]
    if not self.isComponentHostsPopulated(rClientComponent):
      for hostName in hostsComponentsMap.keys():
        hostComponents = hostsComponentsMap[hostName]
        if ({"name": "NODEMANAGER"} in hostComponents or {"name": "YARN_CLIENT"} in hostComponents) \
            and {"name": "MICROSOFT_R_NODE_CLIENT"} not in hostComponents:
          hostsComponentsMap[hostName].append({ "name": "MICROSOFT_R_NODE_CLIENT" })
        if ({"name": "NODEMANAGER"} not in hostComponents and {"name": "YARN_CLIENT"} not in hostComponents) \
            and {"name": "MICROSOFT_R_NODE_CLIENT"} in hostComponents:
          hostsComponentsMap[hostName].remove({"name": "MICROSOFT_R_NODE_CLIENT"})

  def getServiceComponentLayoutValidations(self, services, hosts):
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item["StackServiceComponents"] for sublist in componentsListList for item in sublist]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    hostsCount = len(hostsList)

    rClientHosts = self.getHosts(componentsList, "MICROSOFT_R_NODE_CLIENT")
    expectedrClientHosts = set(self.getHosts(componentsList, "NODEMANAGER")) | set(self.getHosts(componentsList, "YARN_CLIENT"))

    items = []

    # Generate WARNING if any R_NODE_CLIENT is not colocated with NODEMANAGER or YARN_CLIENT
    mismatchHosts = sorted(expectedrClientHosts.symmetric_difference(set(rClientHosts)))
    if len(mismatchHosts) > 0:
      hostsString = ', '.join(mismatchHosts)
      message = "Microsoft R Node Client must be installed on NodeManagers and YARN Clients. " \
                "The following {0} host(s) do not satisfy the colocation recommendation: {1}".format(len(mismatchHosts), hostsString)
      items.append( { "type": 'host-component', "level": 'WARN', "message": message, "component-name": 'MICROSOFT_R_NODE_CLIENT' } )

    return items
