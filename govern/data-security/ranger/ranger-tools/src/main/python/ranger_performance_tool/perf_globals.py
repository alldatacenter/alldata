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
global variables for performance tool.
creates repository to reusable objects.
Use these variables to access configs, object stores, ranger client etc.
"""

import os

from apache_ranger.client.ranger_client import RangerClient

from ranger_performance_tool.ranger_perf_utils.config_utils import ConfigReader
from ranger_performance_tool.ranger_perf_object_stores.base_object_stores import RangerAPIObjectStore

BASE_DIR = os.path.realpath(os.path.join(os.path.dirname(__file__), '../'))

CONFIG_DIR = os.path.join(BASE_DIR, 'config/')

PRIMARY_CONFIG_PATH = os.path.join(CONFIG_DIR, 'primary_config.json')

SECONDARY_CONFIG_PATH = os.path.join(CONFIG_DIR, 'secondary_config.json')

CONFIG_READER = ConfigReader(PRIMARY_CONFIG_PATH, SECONDARY_CONFIG_PATH)

RANGER_CLIENT = RangerClient(CONFIG_READER.get_config_value("primary", "ranger_url"),
                             tuple(CONFIG_READER.get_config_value("primary", "ranger_auth")))

OBJECT_STORE = RangerAPIObjectStore()

OUTPUT_DIR = os.path.join(BASE_DIR, 'outputs/')

