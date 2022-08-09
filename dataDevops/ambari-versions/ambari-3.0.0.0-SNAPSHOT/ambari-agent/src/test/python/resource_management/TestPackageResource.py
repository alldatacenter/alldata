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

import os
import sys
from unittest import TestCase
from mock.mock import patch, MagicMock, call
from ambari_commons.os_check import OSConst
from ambari_commons.repo_manager import ManagerFactory

from resource_management.core import Environment, Fail
from resource_management.core.system import System
from resource_management.core.resources import Package

from resource_management.core import shell
from ambari_commons import shell as ac_shell
from ambari_commons.repo_manager.apt_manager import replace_underscores

@patch.object(os, "geteuid", new=MagicMock(return_value=1234))
class TestPackageResource(TestCase):

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)))
  def test_action_install_pattern_rhel(self, shell_mock):
    shell_mock.return_value.__enter__.return_value = []
    sys.modules['rpm'] = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value.dbMatch.return_value = [{'name':'some_packag'}]
    with Environment('/') as env:
      Package("some_package*",
        logoutput = False
      )

    self.assertEquals(shell_mock.call_args_list[0][0][0],['/usr/bin/yum', '-d', '0', '-e', '0', '-y', 'install', 'some_package*'])

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)))
  def test_action_install_pattern_installed_rhel(self, shell_mock):
    shell_mock.return_value = (0,'')
    sys.modules['yum'] = MagicMock()
    sys.modules['yum'].YumBase.return_value = MagicMock()
    sys.modules['yum'].YumBase.return_value.rpmdb = MagicMock()
    sys.modules['yum'].YumBase.return_value.rpmdb.simplePkgList.return_value = [('some_package_1_2_3',)]
    with Environment('/') as env:
      Package("some_package*",
              logoutput = False
      )
    self.assertEqual(shell_mock.call_count, 0, "shell.checked_call shouldn't be called")

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.SUSE_FAMILY)))
  def test_action_install_pattern_suse(self, shell_mock, call_mock):
    call_mock.side_effect=[(0, None), (0, "Loading repository data...\nReading installed packages...\n\nS | Name\n--+-----\n  | Pack")]
    with Environment('/') as env:
      Package("some_package*",
              )
    call_mock.assert_has_calls([call("installed_pkgs=`rpm -qa 'some_package*'` ; [ ! -z \"$installed_pkgs\" ]"),
                                call("zypper --non-interactive search --type package --uninstalled-only --match-exact 'some_package*'")])
    self.assertEquals(shell_mock.call_args_list[0][0][0],['/usr/bin/zypper', '--quiet', 'install', '--auto-agree-with-licenses', '--no-confirm', 'some_package*'])

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.SUSE_FAMILY)))
  def test_action_install_pattern_suse(self, shell_mock):
    sys.modules['rpm'] = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value.dbMatch.return_value = [{'name':'some_packagetest'}]
    with Environment('/') as env:
      Package("some_package*",
              )
    self.assertEqual(shell_mock.call_count, 0, "shell.checked_call shouldn't be called")

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)))
  def test_action_install_existent_rhel(self, shell_mock):
    sys.modules['rpm'] = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value.dbMatch.return_value = [{'name':'some_package'}]
    with Environment('/') as env:
      Package("some_package",
              )
    self.assertFalse(shell_mock.call_count > 0)

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.SUSE_FAMILY)))
  def test_action_install_existent_suse(self, shell_mock):
    sys.modules['rpm'] = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value.dbMatch.return_value = [{'name':'some_package'}]
    with Environment('/') as env:
      Package("some_package",
              )
    self.assertFalse(shell_mock.call_count > 0)

  @patch.object(ac_shell, "process_executor")
  @patch.object(ManagerFactory, "get", new=MagicMock(return_value=ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)))
  def test_action_remove_rhel(self, shell_mock):
    sys.modules['rpm'] = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value = MagicMock()
    sys.modules['rpm'].TransactionSet.return_value.dbMatch.return_value = [{'name':'some_package'}]
    with Environment('/') as env:
      Package("some_package",
              action = "remove",
              logoutput = False
      )
    self.assertEquals(shell_mock.call_args_list[0][0][0], ['/usr/bin/yum', '-d', '0', '-e', '0', '-y', 'erase', 'some_package'])


  @replace_underscores
  def func_to_test(self, name):
    return name

  def testReplaceUnderscore(self):
    self.assertEqual("-", self.func_to_test("_"))
    self.assertEqual("hadoop-x-x-x-*", self.func_to_test("hadoop_x_x_x-*"))
    self.assertEqual("hadoop", self.func_to_test("hadoop"))
