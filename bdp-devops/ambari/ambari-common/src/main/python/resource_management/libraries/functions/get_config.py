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

from resource_management.core.logger import Logger

__all__ = ["get_config"]

def get_config(config_type, default=None):
  """
  @param config_type: config_type
  """

  import params
  if params.config:
    all_configurations = params.config.get('configurations', default)
    if all_configurations:
      config = all_configurations.get(config_type, default)
      if not config:
        Logger.warning("No configurations for config type {0}. Use default instead.".format(config_type))
      return config
    else:
      Logger.warning("No service configurations available in the \"configurations\" section. Use default instead.".format(config_type))
      return default
  else:
    Logger.warning("No service configurations available. Use default instead.".format(config_type))
    return default
