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

# Python Imports
import os
import json

from mock.mock import patch
from mock.mock import MagicMock

from stacks.utils.RMFTestCase import experimental_mock
patch('resource_management.libraries.functions.decorator.experimental', experimental_mock).start()

# Module imports
from stacks.utils.RMFTestCase import *
from resource_management import Script, ConfigDictionary
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger
from ambari_commons.os_check import OSCheck
from resource_management.core.environment import Environment
import pprint


def fake_call(command, **kwargs):
  """
  Instead of shell.call, call a command whose output equals the command.
  :param command: Command that will be echoed.
  :return: Returns a tuple of (process output code, output)
  """
  return (0, str(command))

class TestStackSelectSetAll(RMFTestCase):
  def get_custom_actions_dir(self):
    return os.path.join(self.get_src_folder(), "test/resources/custom_actions/")

  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  def setUp(self, error_mock, info_mock):

    Logger.logger = MagicMock()

    # Import the class under test. This is done here as opposed to the rest of the imports because the get_os_type()
    # method needs to be patched first.
    from stack_select_set_all import UpgradeSetAll
    global UpgradeSetAll

  def tearDown(self):
    Logger.logger = None

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_execution(self, family_mock, get_config_mock, call_mock, exists_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["upgradeSummary"] = TestStackSelectSetAll._get_upgrade_summary_no_downgrade()["upgradeSummary"]

    config_dict = ConfigDictionary(json_payload)

    def hdp_select_call(command, **kwargs):
      # return no versions
      if "versions" in command:
        return (0,"2.5.9.9-9999")

      return (0,command)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = hdp_select_call
    exists_mock.return_value = True

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'all', u'2.5.9.9-9999'), sudo=True)


  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_skippable_hosts(self, family_mock, get_config_mock, call_mock, exists_mock):
    """
    Tests that hosts are skippable if they don't have stack components installed
    :return:
    """
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(),
      "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))

    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["upgradeSummary"] = TestStackSelectSetAll._get_upgrade_summary_no_downgrade()["upgradeSummary"]

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = False
    get_config_mock.return_value = config_dict
    exists_mock.return_value = True

    def hdp_select_call(command, **kwargs):
      # return no versions
      if "versions" in command:
        return (0,"")

      return (0,command)

    call_mock.side_effect = hdp_select_call

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(('ambari-python-wrap', u'/usr/bin/hdp-select', 'versions'), sudo = True)
    self.assertEqual(call_mock.call_count, 1)

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_skippable_by_list(self, family_mock, get_config_mock, call_mock, exists_mock):
    """
    Tests that hosts are skippable if they don't have stack components installed
    :return:
    """
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(),
      "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))

    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["upgradeSummary"] = TestStackSelectSetAll._get_upgrade_summary_no_downgrade()["upgradeSummary"]

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = False
    get_config_mock.return_value = config_dict
    exists_mock.return_value = True

    def hdp_select_call(command, **kwargs):
      # return no versions
      if "versions" in command:
        return (0,"2.6.7-123")

      return (0,command)

    call_mock.side_effect = hdp_select_call

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(('ambari-python-wrap', u'/usr/bin/hdp-select', 'versions'), sudo = True)
    self.assertEqual(call_mock.call_count, 1)

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_execution_with_downgrade_allowed(self, family_mock, get_config_mock, call_mock, exists_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))

    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["upgradeSummary"] = TestStackSelectSetAll._get_upgrade_summary_downgrade_allowed()["upgradeSummary"]

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call  # echo the command
    exists_mock.return_value = True

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_not_called()


  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch.object(Script, 'get_config')
  @patch.object(OSCheck, 'is_redhat_family')
  def test_execution_with_patch_upgrade(self, family_mock, get_config_mock, call_mock, exists_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))

    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    json_payload["configurations"]["cluster-env"]["stack_tools"] = self.get_stack_tools()
    json_payload["configurations"]["cluster-env"]["stack_features"] = self.get_stack_features()
    json_payload["upgradeSummary"] = TestStackSelectSetAll._get_upgrade_summary_patch_upgrade()["upgradeSummary"]

    config_dict = ConfigDictionary(json_payload)

    family_mock.return_value = True
    get_config_mock.return_value = config_dict
    call_mock.side_effect = fake_call  # echo the command
    exists_mock.return_value = True

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = UpgradeSetAll()
    ru_execute.actionexecute(None)

    call_mock.assert_not_called()


  @staticmethod
  def _get_upgrade_summary_no_downgrade():
    """
    A STANDARD UPGRADE that cannot be downgraded
    :return:
    """
    return {
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
        "associatedStack": "HDP-2.5",
        "associatedVersion":"2.5.9.9-9999",
        "isDowngradeAllowed": False,
        "isSwitchBits": False
      }
    }

  @staticmethod
  def _get_upgrade_summary_downgrade_allowed():
    """
    A STANDARD UPGRADE that can be downgraded
    :return:
    """
    upgrade_summary = TestStackSelectSetAll._get_upgrade_summary_no_downgrade()
    upgrade_summary["upgradeSummary"]["isDowngradeAllowed"] = True
    return upgrade_summary


  @staticmethod
  def _get_upgrade_summary_patch_upgrade():
    """
    A STANDARD UPGRADE that can be downgraded
    :return:
    """
    upgrade_summary = TestStackSelectSetAll._get_upgrade_summary_no_downgrade()
    upgrade_summary["upgradeSummary"]["orchestration"] = "PATCH"
    return upgrade_summary