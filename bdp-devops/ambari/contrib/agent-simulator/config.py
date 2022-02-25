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

import ConfigParser
import os


class Config:
    ATTRIBUTES = {}

    AMBARI_AGENT_VM = "ambari_agent_vm"
    AMBARI_SERVER_VM = "ambari_server_vm"
    SERVICE_SERVER_VM = "service_server_vm"

    RELATIVE_CONFIG_FILE_PATH = "config/config.ini"

    def __init__(self):
        pass

    @staticmethod
    def load():
        """
        load configuration from file, add all configuration to the map ATTRIBUTES
        :return: None
        """
        config = ConfigParser.RawConfigParser()
        # keep file case sensitive
        config.optionxform = str
        config.read(Config.RELATIVE_CONFIG_FILE_PATH)
        for section in config.sections():
            for key in config.options(section):
                Config.ATTRIBUTES[key] = config.get(section, key)

        # set output file path
        for key in config.options("output"):
            if key == "output_folder":
                # create the folder
                if not os.path.exists(Config.ATTRIBUTES["output_folder"]):
                    os.makedirs(Config.ATTRIBUTES["output_folder"])
            else:
                Config.ATTRIBUTES[key] = Config.ATTRIBUTES["output_folder"] + "/" + Config.ATTRIBUTES[key]

    @staticmethod
    def update(section, key, value):
        """
        Update the key value of the configuration file
        :param section: section is inside []
        :param key: the key
        :param value: the value
        :return: None
        """
        config = ConfigParser.RawConfigParser()
        config.read(Config.RELATIVE_CONFIG_FILE_PATH)
        config.set(section, key, value)
        with open(Config.RELATIVE_CONFIG_FILE_PATH, 'wb') as configfile:
                config.write(configfile)
