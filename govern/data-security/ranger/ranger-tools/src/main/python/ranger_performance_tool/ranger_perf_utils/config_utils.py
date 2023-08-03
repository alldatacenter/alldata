#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import json


class ConfigReader:
    """
    Reads the config files and supports requested values from the user
    Methods:
    get_config_value: returns the value of the config key
    override_with_command_line_args: overrides the config values with command line arguments
    """
    def __init__(self, primary_config_file, secondary_config_file):
        """
        Constructor
        :param primary_config_file: str , path to primary config file
        :param secondary_config_file: str , path to secondary config file
        """
        try:
            with open(primary_config_file, 'r') as f:
                self.primary_config = json.load(f)
        except Exception as e:
            raise Exception("Error reading primary config file: {}. Run reset.py script to reinitialize configs".format(primary_config_file))
        try:
            with open(secondary_config_file, 'r') as f:
                self.secondary_config = json.load(f)
        except Exception as e:
            raise Exception("Error reading secondary config file: {}. Run reset.py script to reinitialize configs".format(secondary_config_file))

    def override_with_command_line_args(self, arg_dict):
        """
        Overrides the config values with command line arguments
        :param arg_dict: dict , command line arguments
        :return: void
        """
        api_name = arg_dict["api"]
        self.primary_config["ranger_url"] = arg_dict["ranger_url"]
        self.primary_config["api_list"] = [api_name]
        self.primary_config["api"][api_name]["num_calls"] = int(arg_dict["calls"])
        self.primary_config["system_logger"]["num_calls"] = int(arg_dict["calls"]) + 5
        self.primary_config["ranger_auth"] = (arg_dict["username"], arg_dict["password"])
        self.primary_config["host_name"] = arg_dict["ssh_host"]
        self.primary_config["user"] = arg_dict["ssh_user"]
        self.primary_config["password"] = arg_dict["ssh_password"]
        self.primary_config["client_ip"] = arg_dict["client_ip"]

    def get_config_value(self, config_file="primary", *key_args):
        """
        Returns the value of the config key
        :param config_file: str , config file type - primary or secondary
        :param key_args: list , list of keys to get the value of. Add multiple to dtech nested config values.
        :return: config_value: str , value corresponding to the config key
        """
        if len(key_args) == 0:
            raise Exception("No config key specified")
        if len(key_args) > 3:
            raise Exception("Too many levels of nesting requested. Maximum of 3 levels allowed.")
        if config_file == "primary":
            config_dict = self.primary_config
        elif config_file == "secondary":
            config_dict = self.secondary_config
        else:
            raise Exception("Invalid config file name. Must be either primary or secondary.")

        if len(key_args) == 1:
            return config_dict[key_args[0]]
        elif len(key_args) == 2:
            return config_dict[key_args[0]][key_args[1]]
        elif len(key_args) == 3:
            return config_dict[key_args[0]][key_args[1]][key_args[2]]