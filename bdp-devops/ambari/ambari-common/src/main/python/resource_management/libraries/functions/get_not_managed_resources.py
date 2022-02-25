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
__all__ = ["get_not_managed_resources"]

import json
from resource_management.libraries.script import Script
from resource_management.core.logger import Logger
from resource_management.libraries.functions.default import default

def get_not_managed_resources():
  """
  Returns a list of not managed hdfs paths.
  The result contains all paths from clusterLevelParams/not_managed_hdfs_path_list
  except config values from cluster-env/managed_hdfs_resource_property_names
  """
  config = Script.get_config()
  not_managed_hdfs_path_list = json.loads(config['clusterLevelParams']['not_managed_hdfs_path_list'])[:]
  if 'managed_hdfs_resource_property_names' in config['configurations']['cluster-env']:
    managed_hdfs_resource_property_names = config['configurations']['cluster-env']['managed_hdfs_resource_property_names']
    managed_hdfs_resource_property_list = filter(None, [property.strip() for property in managed_hdfs_resource_property_names.split(',')])

    for property_name in managed_hdfs_resource_property_list:
      property_value = default('/configurations/' + property_name, None)

      if property_value == None:
        Logger.warning(("Property {0} from cluster-env/managed_hdfs_resource_property_names not found in configurations. "
                     "Management of this DFS resource will not be forced.").format(property_name))
      else:
        while property_value in not_managed_hdfs_path_list:
          not_managed_hdfs_path_list.remove(property_value)

  return not_managed_hdfs_path_list