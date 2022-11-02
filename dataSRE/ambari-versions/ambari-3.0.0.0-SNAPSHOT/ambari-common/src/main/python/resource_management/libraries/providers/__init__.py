#!/usr/bin/env python
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

Ambari Agent

"""

PROVIDERS = dict(
  amazon=dict(
    Repository="resource_management.libraries.providers.repository.RhelRepositoryProvider",
  ),
  redhat=dict(
    Repository="resource_management.libraries.providers.repository.RhelRepositoryProvider",
  ),
  suse=dict(
    Repository="resource_management.libraries.providers.repository.SuseRepositoryProvider",
  ),
  ubuntu=dict(
    Repository="resource_management.libraries.providers.repository.UbuntuRepositoryProvider",
  ),
  winsrv=dict(
    Msi="resource_management.libraries.providers.msi.MsiProvider"
  ),
  default=dict(
    ExecuteHadoop="resource_management.libraries.providers.execute_hadoop.ExecuteHadoopProvider",
    TemplateConfig="resource_management.libraries.providers.template_config.TemplateConfigProvider",
    XmlConfig="resource_management.libraries.providers.xml_config.XmlConfigProvider",
    PropertiesFile="resource_management.libraries.providers.properties_file.PropertiesFileProvider",
    MonitorWebserver="resource_management.libraries.providers.monitor_webserver.MonitorWebserverProvider",
    HdfsResource="resource_management.libraries.providers.hdfs_resource.HdfsResourceProvider",
    ModifyPropertiesFile="resource_management.libraries.providers.modify_properties_file.ModifyPropertiesFileProvider"
  ),
)
