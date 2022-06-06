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
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

from mock.mock import patch
from mock.mock import MagicMock

# Module imports
from stacks.utils.RMFTestCase import *
from only_for_platform import not_for_platform, only_for_platform, os_distro_value, PLATFORM_WINDOWS

from resource_management import Script, ConfigDictionary
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileCache import FileCache
from ambari_commons.os_check import OSCheck, OSConst


def fake_call(command, **kwargs):
  """
  Instead of shell.call, call a command whose output equals the command.
  :param command: Command that will be echoed.
  :return: Returns a tuple of (process output code, output)
  """
  return (0, command)


class TestRUExecuteTasks(RMFTestCase):
  def get_custom_actions_dir(self):
    return os.path.normpath(os.path.join(self.get_src_folder(), "test", "resources", "custom_actions"))
  
  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch.object(OSCheck, "get_os_type")
  def setUp(self, os_type_mock, error_mock, info_mock):
    # Must patch the logger and get_os_type function.
    os_type_mock.return_value = "redhat"

    self.CACHE_MOCK_DIR = "/var/lib/ambari-agent/cache/common-services/HDFS/2.1.0.2.0/package"
    self.AGENT_INI_FILE_PATH = os.path.normpath(os.path.join(self.get_src_folder(), "../../ambari-agent/conf/unix/ambari-agent.ini"))

    self.osIndependentSetUp()

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  @patch.object(OSCheck, "get_os_type")
  def setUp(self, os_type_mock, error_mock, info_mock):
    # Must patch the logger and get_os_type function.
    os_type_mock.return_value = "Windows"

    self.CACHE_MOCK_DIR = "/var/lib/ambari-agent/cache/common-services/HDFS/2.1.0.2.0/package"
    self.AGENT_INI_FILE_PATH = os.path.normpath(os.path.join(self.get_src_folder(), "..", "..", "ambari-agent", "conf", "windows", "ambari-agent.ini"))

    self.osIndependentSetUp()

  def osIndependentSetUp(self):
    Logger.logger = MagicMock()

    # Import the class under test. This is done here as opposed to the rest of the imports because the get_os_type()
    # method needs to be patched first.
    from ru_execute_tasks import ExecuteUpgradeTasks
    global ExecuteUpgradeTasks

  def tearDown(self):
    Logger.logger = None

  @patch("resource_management.core.shell.checked_call")
  @patch("os.path.exists")
  @patch.object(AmbariConfig, "getConfigFile")
  @patch.object(Script, 'get_tmp_dir')
  @patch.object(Script, 'get_config')
  @patch.object(FileCache, 'get_service_base_dir')
  def test_execution(self, cache_mock, get_config_mock, get_tmp_dir_mock, get_config_file_mock, os_path_exists_mock, call_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    config_dict = ConfigDictionary(json_payload)

    cache_mock.return_value = self.CACHE_MOCK_DIR
    get_config_mock.return_value = config_dict
    get_tmp_dir_mock.return_value = "/tmp"

    ambari_agent_ini_file_path = self.AGENT_INI_FILE_PATH
    self.assertTrue(os.path.isfile(ambari_agent_ini_file_path))
    get_config_file_mock.return_value = ambari_agent_ini_file_path

    # Mock os calls
    os_path_exists_mock.return_value = True
    call_mock.side_effect = fake_call   # echo the command

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/roleParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, "common-services/HDFS/2.1.0.2.0/package")

    # Begin the test
    ru_execute = ExecuteUpgradeTasks()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with(
        "source /var/lib/ambari-agent/ambari-env.sh ; /usr/bin/ambari-python-wrap /var/lib/ambari-agent/cache/common-services/HDFS/2.1.0.2.0/package" + os.sep +
        "scripts/namenode.py prepare_rolling_upgrade /tmp", logoutput=True, quiet=True)
    pass

  @patch("resource_management.core.shell.checked_call")
  @patch("os.path.exists")
  @patch.object(AmbariConfig, "getConfigFile")
  @patch.object(Script, 'get_tmp_dir')
  @patch.object(Script, 'get_config')
  @patch.object(FileCache, 'get_custom_actions_base_dir')
  def test_execution_custom_action(self, cache_mock, get_config_mock, get_tmp_dir_mock, get_config_file_mock, os_path_exists_mock, call_mock):
    # Mock the config objects
    json_file_path = os.path.join(self.get_custom_actions_dir(), "ru_execute_tasks_namenode_prepare.json")
    self.assertTrue(os.path.isfile(json_file_path))
    with open(json_file_path, "r") as json_file:
      json_payload = json.load(json_file)

    del json_payload['commandParams']['service_package_folder']
    del json_payload['commandParams']['hooks_folder']

    config_dict = ConfigDictionary(json_payload)

    cache_mock.return_value = "/var/lib/ambari-agent/cache/custom_actions"
    get_config_mock.return_value = config_dict
    get_tmp_dir_mock.return_value = "/tmp"

    ambari_agent_ini_file_path = os.path.join(self.get_src_folder(), "../../ambari-agent/conf/unix/ambari-agent.ini")
    self.assertTrue(os.path.isfile(ambari_agent_ini_file_path))
    get_config_file_mock.return_value = ambari_agent_ini_file_path

    # Mock os calls
    os_path_exists_mock.return_value = True
    call_mock.side_effect = fake_call   # echo the command

    # Ensure that the json file was actually read.
    stack_name = default("/clusterLevelParams/stack_name", None)
    stack_version = default("/clusterLevelParams/stack_version", None)
    service_package_folder = default('/commandParams/service_package_folder', None)

    self.assertEqual(stack_name, "HDP")
    self.assertEqual(stack_version, '2.2')
    self.assertEqual(service_package_folder, None)

    # Begin the test
    ru_execute = ExecuteUpgradeTasks()
    ru_execute.actionexecute(None)

    call_mock.assert_called_with("source /var/lib/ambari-agent/ambari-env.sh ; /usr/bin/ambari-python-wrap /var/lib/ambari-agent/cache/custom_actions" + os.sep +
                                 "scripts/namenode.py prepare_rolling_upgrade /tmp", logoutput=True, quiet=True)
    pass
