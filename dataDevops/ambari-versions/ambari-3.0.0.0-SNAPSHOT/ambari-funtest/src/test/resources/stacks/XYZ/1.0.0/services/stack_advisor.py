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

from stack_advisor import StackAdvisor

class XYZ100StackAdvisor(StackAdvisor):

  def recommendConfigurations(self, services, hosts):
    stackName = services["Versions"]["stack_name"]
    stackVersion = services["Versions"]["stack_version"]
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    servicesList = [service["StackServices"]["service_name"] for service in services["services"]]

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
      calculation = self.recommendServiceConfigurations(service)
      if calculation is not None:
        calculation(configurations)

    return recommendations

  def recommendServiceConfigurations(self, service):
    return {
      "YARN": self.recommendYARNConfigurations,
    }.get(service, None)

  def putProperty(self, config, configType):
    config[configType] = {"properties": {}}
    def appendProperty(key, value):
      config[configType]["properties"][key] = str(value)
    return appendProperty

  def recommendYARNConfigurations(self, configurations):
    putYarnProperty = self.putProperty(configurations, "yarn-site")
    putYarnProperty('yarn.nodemanager.resource.memory-mb', "-Xmx100m")

