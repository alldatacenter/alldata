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

from mock.mock import patch
from mock.mock import MagicMock

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import stack_select
from resource_management.libraries.script import Script

from unittest import TestCase

Logger.initialize_logger()

class TestStackSelect(TestCase):

  def test_missing_role_information_throws_exception(self):
    """
    Tests that missing the service & role throws an excpetion
    :return:
    """
    version = "2.5.9.9-9999"

    command_json = TestStackSelect._get_incomplete_cluster_simple_upgrade_json()
    Script.config = command_json

    self.assertRaises(Fail, stack_select.select_packages, version)

  @patch.object(stack_select, "get_supported_packages")
  @patch("resource_management.libraries.functions.stack_select.select")
  def test_select_package_for_standard_orchestration(self, stack_select_select_mock, get_supported_packages_mock):
    """
    Tests that missing the service & role throws an excpetion
    :return:
    """
    get_supported_packages_mock.return_value = TestStackSelect._get_supported_packages()

    version = "2.5.9.9-9999"

    command_json = TestStackSelect._get_cluster_simple_upgrade_json()

    Script.config = dict()
    Script.config.update(command_json)
    Script.config.update( { "configurations" : { "cluster-env" : {} }, "clusterLevelParams": {} } )
    Script.config["configurations"]["cluster-env"]["stack_packages"] = self._get_stack_packages()
    Script.config["clusterLevelParams"] = { "stack_name" : "HDP" }

    stack_select.select_packages(version)

    self.assertEqual(len(stack_select_select_mock.call_args_list), 2)
    self.assertEqual(stack_select_select_mock.call_args_list[0][0], ("foo-master", version))
    self.assertEqual(stack_select_select_mock.call_args_list[1][0], ("foo-client", version))

  @patch.object(stack_select, "get_supported_packages")
  @patch("resource_management.libraries.functions.stack_select.select")
  def test_select_package_for_patch_orchestration(self, stack_select_select_mock, get_supported_packages_mock):
    """
    Tests that missing the service & role throws an excpetion
    :return:
    """
    get_supported_packages_mock.return_value = TestStackSelect._get_supported_packages()

    version = "2.5.9.9-9999"

    command_json = TestStackSelect._get_cluster_simple_upgrade_json()
    command_json["upgradeSummary"]["orchestration"] = "PATCH"

    Script.config = dict()
    Script.config.update(command_json)
    Script.config.update( { "configurations" : { "cluster-env" : {} }, "clusterLevelParams": {} } )
    Script.config["configurations"]["cluster-env"]["stack_packages"] = self._get_stack_packages()
    Script.config["clusterLevelParams"] = { "stack_name" : "HDP" }

    stack_select.select_packages(version)

    self.assertEqual(len(stack_select_select_mock.call_args_list), 1)
    self.assertEqual(stack_select_select_mock.call_args_list[0][0], ("foo-master", version))

    stack_select_select_mock.reset_mock()

    command_json["upgradeSummary"]["orchestration"] = "MAINT"
    stack_select.select_packages(version)

    self.assertEqual(len(stack_select_select_mock.call_args_list), 1)
    self.assertEqual(stack_select_select_mock.call_args_list[0][0], ("foo-master", version))


  @patch.object(stack_select, "get_supported_packages")
  @patch("resource_management.libraries.functions.stack_select.select")
  def test_legacy_package_fallback(self, stack_select_select_mock, get_supported_packages_mock):
    """
    Tests that if the package specified by the JSON isn't support by the stack-select tool,
    the the fallback legacy value is used.
    :return:
    """
    get_supported_packages_mock.return_value = ["foo-legacy"]

    version = "2.5.9.9-9999"

    command_json = TestStackSelect._get_cluster_simple_upgrade_json()

    Script.config = dict()
    Script.config.update(command_json)
    Script.config.update( { "configurations" : { "cluster-env" : {} }, "clusterLevelParams": {} } )
    Script.config["configurations"]["cluster-env"]["stack_packages"] = self._get_stack_packages_with_legacy()
    Script.config["clusterLevelParams"] = { "stack_name" : "HDP" }

    stack_select.select_packages(version)

    self.assertEqual(len(stack_select_select_mock.call_args_list), 1)
    self.assertEqual(stack_select_select_mock.call_args_list[0][0], ("foo-legacy", version))

  @staticmethod
  def _get_incomplete_cluster_simple_upgrade_json():
    """
    A command missing the role and service name during an upgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "clusterLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "commandParams": {
        "source_stack": "2.4",
        "target_stack": "2.5",
        "upgrade_direction": "upgrade",
        "version": "2.5.9.9-9999"
      },
      "upgradeSummary": {
        "services":{
          "HDFS":{
            "sourceRepositoryId":1,
            "sourceStackId":"HDP-2.4",
            "sourceVersion":"2.4.0.0-1234",
            "targetRepositoryId":2,
            "targetStackId":"HDP-2.5",
            "targetVersion":"2.5.9.9-9999"
          }
        },
        "direction":"UPGRADE",
        "type":"rolling_upgrade",
        "isRevert":False,
        "orchestration":"STANDARD",
        "associatedStackId":"HDP-2.5",
        "associatedVersion":"2.5.9.9-9999",
        "isDowngradeAllowed": True,
        "isSwitchBits": False
      }
    }

  @staticmethod
  def _get_cluster_simple_upgrade_json():
    """
    A restart command during an upgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "serviceName": "FOO_SERVICE",
      "role": "FOO_MASTER",
      "clusterLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "commandParams": {
        "source_stack": "2.4",
        "target_stack": "2.5",
        "upgrade_direction": "upgrade",
        "version": "2.5.9.9-9999"
      },
      "upgradeSummary": {
        "services":{
          "HDFS":{
            "sourceRepositoryId":1,
            "sourceStackId":"HDP-2.4",
            "sourceVersion":"2.4.0.0-1234",
            "targetRepositoryId":2,
            "targetStackId":"HDP-2.5",
            "targetVersion":"2.5.9.9-9999"
          }
        },
        "direction":"UPGRADE",
        "type":"rolling_upgrade",
        "isRevert":False,
        "orchestration":"STANDARD",
        "associatedStackId":"HDP-2.5",
        "associatedVersion":"2.5.9.9-9999",
        "isDowngradeAllowed": True,
        "isSwitchBits": False
      }
    }

  @staticmethod
  def _get_stack_packages():
    import json
    return json.dumps( {
      "HDP": {
        "stack-select": {
          "FOO_SERVICE": {
            "FOO_MASTER": {
              "STACK-SELECT-PACKAGE": "foo-master",
              "INSTALL": [
                "foo-master",
                "foo-client"
              ],
              "PATCH": [
                "foo-master"
              ],
              "STANDARD": [
                "foo-master",
                "foo-client"
              ]
            }
          }
        }
      }
    } )

  @staticmethod
  def _get_stack_packages_with_legacy():
    import json
    return json.dumps( {
      "HDP": {
        "stack-select": {
          "FOO_SERVICE": {
            "FOO_MASTER": {
              "LEGACY":"foo-legacy",
              "STACK-SELECT-PACKAGE": "foo-master",
              "INSTALL": [
                "foo-master"
              ],
              "PATCH": [
                "foo-master"
              ],
              "STANDARD": [
                "foo-master"
              ]
            }
          }
        }
      }
    } )

  @staticmethod
  def _get_supported_packages():
    return ["foo-master", "foo-client"]
