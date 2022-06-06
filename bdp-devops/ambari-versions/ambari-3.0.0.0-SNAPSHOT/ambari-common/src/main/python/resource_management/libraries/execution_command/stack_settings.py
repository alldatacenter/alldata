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

__all__ = ["StackSettings"]

class StackSettings(object):

    # Stack related configs from stack's stack_settings.json
    STACK_NAME_SETTING = "stack_name"
    STACK_TOOLS_SETTING = "stack_tools"
    STACK_FEATURES_SETTING = "stack_features"
    STACK_PACKAGES_SETTING = "stack_packages"
    STACK_ROOT_SETTING = "stack_root"
    STACK_SELECT_SETTING = "stack_select"

    """
    This class maps to "configurations->cluster-env" in command.json which includes cluster setting information of a cluster
    """

    def __init__(self, stackSettings):
        self.__stack_settings = stackSettings

    def __get_value(self, key):
        """
        Get corresponding value from the key
        :param key:
        :return: value if key exist else None
        """
        return self.__stack_settings.get(key)

    def get_mpack_name(self):
        """
        Retrieve mpack name from command.json/cluster-env, i.e "stack_name": "HDPCORE"
        :return: mpack name string
        """
        return self.__get_value("stack_name")

    '''  TODO : Open it when we can serve them from stackSettings.
    def get_mpack_version(self):
        """
        Retrieve mpack version from command.json, i.e "stack_version": "1.0.0-b224"
        :return: mpack version string
        """
        return self.__get_value("stack_version")
    
    def get_user_groups(self):
        """
        Retrieve a list of ambari server user groups, i.e "user_groups": "{\"zookeeper\":[\"hadoop\"],\"ambari-qa\":[\"hadoop\"]}"
        :return: String, as a user group dict object
        """
        return self.__get_value("user_groups")
    '''

    def get_group_list(self):
        """
        Retrieve a list of user groups from command.json, i.e "group_list": "[\"hadoop\"]"
        :return: a list of groups
        """
        group_list = self.__get_value("group_list")
        if not group_list:
            group_list = "[]"
        return group_list

    def get_user_list(self):
        """
        Retrieve a list of users from command.json, i.e "user_list": "[\"zookeeper\",\"ambari-qa\"]"
        :return: a list of users
        """
        user_list = self.__get_value("user_list")
        if not user_list:
            user_list = "[]"
        return user_list

    def get_stack_features(self):
        """
        Retrieve a string represents a dict of stack features
        :return: String, can be loaded as python dict
        """
        return self.__get_value("stack_features")

    def get_stack_packages(self):
        """
        Retrieve a string represents a list of packages can be installed from the stack
        :return: String, can be loaded as python dict
        """
        return self.__get_value("stack_packages")

    def get_stack_tools(self):
        """
        Retrieve a list of stack select tools
        :return: String, can be loaded as python dict
        """
        return self.__get_value("stack_tools")