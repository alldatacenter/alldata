#!/usr/bin/env python

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

from unittest import TestCase
from ambari_agent.AmbariConfig import AmbariConfig
import sys

import logging

class TestAmbariConfig(TestCase):
  def setUp(self):
    # save original open() method for later use
    self.original_open = open

  def tearDown(self):
    sys.stdout = sys.__stdout__

  logger = logging.getLogger()

  def test_ambari_config_get(self):
    config = AmbariConfig()
    #default
    self.assertEqual(config.get("security", "keysdir"), "/tmp/ambari-agent")
    #non-default
    config.set("security", "keysdir", "/tmp/non-default-path")
    self.assertEqual(config.get("security", "keysdir"), "/tmp/non-default-path")
    #whitespace handling
    config.set("security", "keysdir", " /tmp/non-stripped")
    self.assertEqual(config.get("security", "keysdir"), "/tmp/non-stripped")

    # test default value
    open_files_ulimit = config.get_ulimit_open_files()
    self.assertEqual(open_files_ulimit, 0)

    # set a value
    open_files_ulimit = 128000
    config.set_ulimit_open_files(open_files_ulimit)
    self.assertEqual(config.get_ulimit_open_files(), open_files_ulimit)

  def test_ambari_config_get_command_file_retention_policy(self):
    config = AmbariConfig()

    # unset value yields, "keep"
    if config.has_option("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY):
      config.remove_option("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY)
    self.assertEqual(config.command_file_retention_policy,
                     AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP)

    config.set("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
               AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP)
    self.assertEqual(config.command_file_retention_policy,
                     AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP)

    config.set("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
               AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE)
    self.assertEqual(config.command_file_retention_policy,
                     AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE)

    config.set("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY,
               AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS)
    self.assertEqual(config.command_file_retention_policy,
                     AmbariConfig.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS)

    # Invalid value yields, "keep"
    config.set("agent", AmbariConfig.COMMAND_FILE_RETENTION_POLICY_PROPERTY, "invalid_value")
    self.assertEqual(config.command_file_retention_policy,
                     AmbariConfig.COMMAND_FILE_RETENTION_POLICY_KEEP)
