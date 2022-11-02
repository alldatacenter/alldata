#!/usr/bin/ambari-python-wrap
'''
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
'''

import os
import ConfigParser

PROPERTIES_TO_REWRITE = [
  ('heartbeat', 'dirs'),
  ('heartbeat', 'state_interval')
]
SECTIONS_TO_REMOVE = [
  'stack', 'puppet', 'command', 'python'
]

CONFIG_FILE_BACKUP = '/etc/ambari-agent/conf/ambari-agent.ini.old'
CONFIG_FILE = '/etc/ambari-agent/conf/ambari-agent.ini'

if os.path.isfile(CONFIG_FILE_BACKUP):
  if os.path.isfile(CONFIG_FILE):
    print "Upgrading configs in {0}".format(CONFIG_FILE)
    print "Values will be updated from {0} except the following list: {1}, {2}".format(CONFIG_FILE_BACKUP, PROPERTIES_TO_REWRITE, SECTIONS_TO_REMOVE)

    agent_config_backup = ConfigParser.ConfigParser()
    agent_config_backup.read(CONFIG_FILE_BACKUP)

    agent_config = ConfigParser.ConfigParser()
    agent_config.read(CONFIG_FILE)

    for section in agent_config_backup.sections():
      for (property_name, property_val) in agent_config_backup.items(section):
        if section not in SECTIONS_TO_REMOVE and (section, property_name) not in PROPERTIES_TO_REWRITE:
          try:
            agent_config.set(section, property_name, property_val)
          except ConfigParser.NoSectionError:
            agent_config.add_section(section)
            agent_config.set(section, property_name, property_val)

    with (open(CONFIG_FILE, "wb")) as new_agent_config:
      agent_config.write(new_agent_config)
  else:
    print "Values are not updated, configs {0} is not found".format(CONFIG_FILE)
else:
  print "Values are not updated, backup {0} is not found".format(CONFIG_FILE_BACKUP)