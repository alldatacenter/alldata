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
"""
Use this script to reinitialize config files for performance analyser. Call this only once for a particular client machine.
Running this script will override your existing config files. Take back up your existing config files before running this script.

usage: python3 setup_performance_analyzer.py
"""

import os
import json

from ranger_performance_tool.ranger_perf_assets.path import DEFAULT_PRIMARY_CONFIG_PATH, DEFAULT_SECONDARY_CONFIG_PATH

CONFIG_DIR = os.path.realpath(os.path.join(os.path.dirname(__file__), 'config/'))

to_write_primary_config = os.path.join(CONFIG_DIR, 'primary_config.json')
to_write_secondary_config = os.path.join(CONFIG_DIR, 'secondary_config.json')


def overwrite_config(config_file, config_dict):
    with open(config_file, 'w') as f:
        json.dump(config_dict, f, indent=4)


def read_config(config_file):
    with open(config_file, 'r') as f:
        return json.load(f)


print("Reading default primary config file from {}".format(DEFAULT_PRIMARY_CONFIG_PATH))
print("Reading default secondary config file from {}".format(DEFAULT_SECONDARY_CONFIG_PATH))
print("Writing to primary config path: {}".format(to_write_primary_config))
print("Writing to secondary config path: {}".format(to_write_secondary_config))

default_primary_json = read_config(DEFAULT_PRIMARY_CONFIG_PATH)
overwrite_config(to_write_primary_config, default_primary_json)

default_secondary_json = read_config(DEFAULT_SECONDARY_CONFIG_PATH)
overwrite_config(to_write_secondary_config, default_secondary_json)