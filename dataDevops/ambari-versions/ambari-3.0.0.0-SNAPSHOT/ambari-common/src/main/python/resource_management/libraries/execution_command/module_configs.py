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

__all__ = ["ModuleConfigs"]


class ModuleConfigs(object):
    """
    This class maps to "/configurations" and "/configurationAttributes in command.json which includes configuration information of a service
    """

    def __init__(self, configs, configAttributes):
        self.__module_configs = configs
        self.__module_config_attributes = configAttributes

    def get_raw_config_dict(self):
        """
        Sometimes the caller needs to access to module_configs directly
        :return: config dict
        """
        return self.__module_configs

    def get_all_attributes(self, module_name, config_type):
        """
        Retrieve attributes from /configurationAttributes/config_type
        :param module_name:
        :param config_type:
        :return:
        """
        if config_type not in self.__module_config_attributes:
            return {}
        try:
            return self.__module_config_attributes[config_type]
        except:
            return {}

    def get_all_properties(self, module_name, config_type):
        if config_type not in self.__module_configs:
            return {}
        try:
            return self.__module_configs[config_type]
        except:
            return {}

    def get_properties(self, module_name, config_type, property_names, default=None):
        properties = {}
        try:
            for property_name in property_names:
                properties[property_name] = self.get_property_value(module_name, config_type, property_name, default)
        except:
            return {}
        return properties

    def get_property_value(self, module_name, config_type, property_name, default=None):
        if config_type not in self.__module_configs or property_name not in self.__module_configs[config_type]:
            return default
        try:
            value = self.__module_configs[config_type][property_name]
            if value == None:
                value = default
            return value
        except:
            return default