# !/usr/bin/env python

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


from resource_management.core.logger import Logger
from resource_management.libraries.functions import component_version
from resource_management.libraries.script import Script
from unittest import TestCase

Logger.initialize_logger()

class TestComponentVersionMapping(TestCase):

  def test_get_component_versions(self):
    """
    Tests that the component version map can be parsed
    :return:
    """
    command_json = TestComponentVersionMapping._get_component_version_mappings()
    Script.config = command_json

    version = component_version.get_component_repository_version(service_name="HDFS",
      component_name="DATANODE")

    self.assertEqual(version, "2.5.0.0-1234")

    version = component_version.get_component_repository_version(service_name = "ZOOKEEPER",
      component_name = "ZOOKEEPER_SERVER")

    self.assertEqual(version, "2.6.0.0-9999")


  def test_get_component_version_by_service_name(self):
    """
    Tests that the component version map can be parsed using only the service name
    :return:
    """
    command_json = TestComponentVersionMapping._get_component_version_mappings()
    Script.config = command_json

    version = component_version.get_component_repository_version(service_name="HDFS")
    self.assertEqual(version, "2.5.0.0-1234")

    version = component_version.get_component_repository_version(service_name = "ZOOKEEPER")
    self.assertEqual(version, "2.6.0.0-9999")


  @staticmethod
  def _get_component_version_mappings():
    """
    A typical component version mapping structure
    :return:
    """
    return {
      "componentVersionMap": {
        "HDFS": {
          "NAMENODE": "2.5.0.0-1234",
          "SECONDARY_NAMENODE": "2.5.0.0-1234",
          "DATANODE": "2.5.0.0-1234",
          "HDFS_CLIENT": "2.5.0.0-1234"
        },
        "ZOOKEEPER": {
          "ZOOKEEPER_SERVER": "2.6.0.0-9999",
          "ZOOKEEPER_CLIENT": "2.6.0.0-9999"
        }
      },
    }
