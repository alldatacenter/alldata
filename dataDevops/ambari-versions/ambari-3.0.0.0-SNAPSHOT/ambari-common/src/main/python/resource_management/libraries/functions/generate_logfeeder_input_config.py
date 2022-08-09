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

"""
import os
from resource_management.core.logger import Logger
from ambari_commons.constants import LOGFEEDER_CONF_DIR
from resource_management.core.resources import File, Directory

__all__ = ["generate_logfeeder_input_config"]

def generate_logfeeder_input_config(type, content):
  """
  :param type: type of the logfeeder input config (most likely a service name: hdfs),
  it will be generated as input.config-<type>.json in logfeeder config folder
  :param content: generated template for the input config json file (you can use Template or InlineTemplate)
  """
  import params
  Directory(LOGFEEDER_CONF_DIR,
            mode=0755,
            cd_access='a',
            create_parents=True
            )
  input_file_name = 'input.config-' + type + '.json'
  Logger.info("Generate Log Feeder config file: " + os.path.join(LOGFEEDER_CONF_DIR, input_file_name))
  File(os.path.join(LOGFEEDER_CONF_DIR, input_file_name),
       content=content,
       mode=0644
       )