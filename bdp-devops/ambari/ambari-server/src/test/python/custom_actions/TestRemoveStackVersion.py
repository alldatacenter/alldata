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
from stacks.utils.RMFTestCase import *

OLD_VERSION_STUB = '2.1.0.0-400'
VERSION_STUB = '2.2.0.1-885'


@patch.object(Logger, 'logger', new=MagicMock())
class TestRemoveStackVersion(RMFTestCase):

  @staticmethod
  def _add_packages():
    return [
      ["pkg12_1_0_0_400", "1.0", "repo"],
      ["pkg22_1_0_1_885", "2.0", "repo2"],
      ["hdp-select2_1_0_1_885", "2.0", "repo2"]
    ]

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool_package", new = MagicMock(return_value="hdp-select"))
  @patch("os.listdir", new = MagicMock(return_value=["somefile"]))
  def test_normal_flow(self,
                       write_actual_version_to_history_file_mock,
                       read_actual_version_from_history_file_mock,
                       stack_versions_mock,
                       put_structured_out_mock, get_provider_mock):
    m = MagicMock()
    m.all_installed_packages.side_effect = TestRemoveStackVersion._add_packages
    get_provider_mock.return_value = m
    stack_versions_mock.return_value = [VERSION_STUB, OLD_VERSION_STUB]

    self.executeScript("scripts/remove_previous_stacks.py",
                       classname="RemovePreviousStacks",
                       command="actionexecute",
                       config_file="remove_previous_stacks.json",
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final')
                       )
    self.assertTrue(stack_versions_mock.called)
    self.assertEquals(stack_versions_mock.call_args[0][0], '/usr/hdp')

    self.assertResourceCalled('Package', "pkg12_1_0_0_400", action=["remove"])
    self.assertTrue(put_structured_out_mock.called)
    self.assertEquals(put_structured_out_mock.call_args[0][0],
                      {'remove_previous_stacks': {'exit_code': 0,
                       'message': 'Stack version 2.1.0.0-400 successfully removed!'}})
    self.assertResourceCalled('Execute', ('rm', '-f', '/usr/hdp2.1.0.0-400'),
                              sudo = True,
                              )
    self.assertNoMoreResources()

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool_package", new = MagicMock(return_value="hdp-select"))
  @patch("os.listdir", new = MagicMock(return_value=["somefile"]))
  def test_without_versions(self,
                       write_actual_version_to_history_file_mock,
                       read_actual_version_from_history_file_mock,
                       stack_versions_mock,
                       put_structured_out_mock, get_provider_mock):

    stack_versions_mock.return_value = [VERSION_STUB]

    m = MagicMock()
    m.all_installed_packages.side_effect = TestRemoveStackVersion._add_packages
    get_provider_mock.return_value = m

    self.executeScript("scripts/remove_previous_stacks.py",
                       classname="RemovePreviousStacks",
                       command="actionexecute",
                       config_file="remove_previous_stacks.json",
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final')
                       )
    self.assertTrue(stack_versions_mock.called)
    self.assertEquals(stack_versions_mock.call_args[0][0], '/usr/hdp')
    self.assertNoMoreResources()

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool_package", new = MagicMock(return_value="hdp-select"))
  @patch("os.listdir", new = MagicMock(return_value=["somefile" + OLD_VERSION_STUB]))
  def test_symlink_exist(self,
                       write_actual_version_to_history_file_mock,
                       read_actual_version_from_history_file_mock,
                       stack_versions_mock,
                       put_structured_out_mock, get_provider_mock):

    stack_versions_mock.return_value = [VERSION_STUB, OLD_VERSION_STUB]

    m = MagicMock()
    m.all_installed_packages.side_effect = TestRemoveStackVersion._add_packages
    get_provider_mock.return_value = m

    try:
      self.executeScript("scripts/remove_previous_stacks.py",
                       classname="RemovePreviousStacks",
                       command="actionexecute",
                       config_file="remove_previous_stacks.json",
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final')
                       )
      self.fail("Should throw exception")
    except Fail, e:
      self.assertEquals(str(e), '/usr/hdp/current/ contains symlink to version for remove! 2.1.0.0-400')
      pass  # Expected

    self.assertTrue(stack_versions_mock.called)
    self.assertEquals(stack_versions_mock.call_args[0][0], '/usr/hdp')
    self.assertNoMoreResources()