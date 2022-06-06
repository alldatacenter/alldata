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
import json
import os
from ambari_commons import subprocess32
import select

from stacks.utils.RMFTestCase import *
from mock.mock import patch, MagicMock
from resource_management.core.base import Resource
from resource_management.core.exceptions import Fail
from resource_management.libraries.script import Script

OLD_VERSION_STUB = '2.1.0.0-400'
VERSION_STUB_WITHOUT_BUILD_NUMBER = '2.2.0.1'
VERSION_STUB = '2.2.0.1-885'

subproc_mock = MagicMock()
subproc_mock.return_value = MagicMock()
subproc_stdout = MagicMock()
subproc_mock.return_value.stdout = subproc_stdout


@patch.object(os, "read", new=MagicMock(return_value=None))
@patch.object(select, "select", new=MagicMock(return_value=([subproc_stdout], None, None)))
@patch("pty.openpty", new = MagicMock(return_value=(1,5)))
@patch.object(os, "close", new=MagicMock())
@patch.object(subprocess32, "Popen", new=subproc_mock)
class TestInstallPackages(RMFTestCase):

  def setUp(self):
    self.maxDiff = None

  @staticmethod
  def _add_packages(*args, **kwargs):
    return [
      ["pkg1", "1.0", "repo"],
      ["pkg2", "2.0", "repo2"]
    ]

  @staticmethod
  def _add_packages_available(*args, **kwargs):
    return [
      ["hadoop_2_2_0_1_885", "1.0", "HDP-2.2"],
      ["hadooplzo_2_2_0_1_885", "1.0", "HDP-2.2"],
      ["hadoop_2_2_0_1_885-libhdfs", "1.0", "HDP-2.2"]
    ]

  @staticmethod
  def _add_packages_lookUpYum(*args, **kwargs):
    return TestInstallPackages._add_packages_available(*args, **kwargs)

  def test_get_installed_package_version(self):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory
    from ambari_commons.shell import SubprocessCallResult

    r = SubprocessCallResult("3.1.0.0-54.el7.centos", "", 0)

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)
    with patch("ambari_commons.shell.subprocess_executor") as checked_call_mock:
      checked_call_mock.return_value = r
      expected_version = pkg_manager.get_installed_package_version("test")
      self.assertEquals("3.1.0.0-54", expected_version)


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_normal_flow_rhel(self,
                            subprocess_with_timeout,
                            write_actual_version_to_history_file_mock,
                            read_actual_version_from_history_file_mock,
                            stack_versions_mock,
                            put_structured_out_mock,
                            get_provider):
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      available_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      installed_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      check_existence.return_value = False

      get_provider.return_value = pkg_manager

      repo_file_name = 'ambari-hdp-1'
      use_repos = { 'HDP-UTILS-1.1.0.20': repo_file_name, 'HDP-2.2': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_file="install_packages_config.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
      )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', 'HDP-2.2',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_no_repos(self,
                            subprocess_with_timeout,
                            write_actual_version_to_history_file_mock,
                            read_actual_version_from_history_file_mock,
                            stack_versions_mock,
                            put_structured_out_mock, get_provider):
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repositories'] = []

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages:
      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available

      get_provider.return_value = pkg_manager

      try:
        self.executeScript("scripts/install_packages.py",
                           classname="InstallPackages",
                           command="actionexecute",
                           config_dict = command_json,
                           target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                           os_type=('Redhat', '6.4', 'Final'),
        )
      except Fail as e:
        self.assertEquals(e.message, "Failed to distribute repositories/install packages")
      else:
        self.assertFalse("Packages can't be installed without repos")

      self.assertNoMoreResources()

  @patch("ambari_commons.os_check.OSCheck.is_suse_family")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_normal_flow_sles(self, subprocess_with_timeout, write_actual_version_to_history_file_mock,
                            read_actual_version_from_history_file_mock,
                            stack_versions_mock, put_structured_out_mock,
                            get_provider, is_suse_family_mock):
    is_suse_family_mock = True
    Script.stack_version_from_distro_select = VERSION_STUB
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory
    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager

      repo_file_name = 'ambari-hdp-1'
      use_repos = { 'HDP-UTILS-1.1.0.20': repo_file_name, 'HDP-2.2': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_file="install_packages_config.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Suse', '11', 'SP1'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', 'HDP-2.2',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()

  @patch("ambari_commons.os_check.OSCheck.is_redhat_family")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_exclude_existing_repo(self, subprocess_with_timeout, write_actual_version_to_history_file_mock,
                                 read_actual_version_from_history_file_mock,
                                 stack_versions_mock,
                                 get_provider, put_structured_out_mock,
                                 is_redhat_family_mock):
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]
    Script.stack_version_from_distro_select = VERSION_STUB
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      available_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      installed_packages.side_effect = TestInstallPackages._add_packages_lookUpYum
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      is_redhat_family_mock.return_value = True
      repo_file_name = 'ambari-hdp-1'
      use_repos = { 'HDP-UTILS-1.1.0.20': repo_file_name, 'HDP-2.2': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_file="install_packages_config.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
      )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', 'HDP-2.2',
                                base_url='http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()


  _install_failed = False

  @staticmethod
  def _add_packages_with_fail(*args, **kwargs):
    arg = []
    arg.append(["pkg1_2_2_0_1_885_pack", "1.0", "repo"])
    arg.append(["pkg2_2_2_0_1_885_pack2", "2.0", "repo2"])
    if TestInstallPackages._install_failed:
      arg.append(["should_not_be_removed_pkg1", "1.0", "repo"])
      arg.append(["hadoop_2_2_0_1_885fake_pkg", "1.0", "repo"])
      arg.append(["snappy__2_2_0_1_885_fake_pkg", "3.0", "repo2"])
      arg.append(["ubuntu-like-2-2-0-1-885-fake-pkg", "3.0", "repo2"])
      arg.append(["should_not_be_removed_pkg2", "3.0", "repo2"])

    return arg

  @staticmethod
  def _new_with_exception(cls, name, env=None, provider=None, **kwargs):
    if (name != "snappy-devel"):
      return Resource.__new__(cls, name, env, provider, **kwargs)
    else:
      TestInstallPackages._install_failed = True
      raise Exception()

  @patch("ambari_commons.os_check.OSCheck.is_redhat_family")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.core.resources.packaging.Package.__new__")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_fail(self, subprocess_with_timeout, put_structured_out_mock, Package__mock, get_provider,
                is_redhat_family_mock):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages:
      all_packages.side_effect = TestInstallPackages._add_packages_with_fail
      available_packages.side_effect = TestInstallPackages._add_packages_with_fail
      installed_packages.side_effect = TestInstallPackages._add_packages_with_fail

      get_provider.return_value = pkg_manager

      is_redhat_family_mock.return_value = True

      def side_effect(retcode):
        TestInstallPackages._install_failed = True
        raise Exception()

      Package__mock.side_effect = side_effect
      self.assertRaises(Fail, self.executeScript, "scripts/install_packages.py",
                        classname="InstallPackages",
                        command="actionexecute",
                        config_file="install_packages_config.json",
                        target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                        os_type=('Redhat', '6.4', 'Final'))

      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'repository_version_id': 1,
                        'package_installation_result': 'FAIL'})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=u'ambari-hdp-1',
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', 'HDP-2.2',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=u'ambari-hdp-1',
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()

      TestInstallPackages._install_failed = False


  @patch("ambari_commons.os_check.OSCheck.is_suse_family")
  @patch("resource_management.core.resources.packaging.Package")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_format_package_name(self, subprocess_with_timeout, write_actual_version_to_history_file_mock,
                               read_actual_version_from_history_file_mock,
                               stack_versions_mock,
                               get_provider, put_structured_out_mock,
                               package_mock, is_suse_family_mock):
    Script.stack_version_from_distro_select = VERSION_STUB
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]
    read_actual_version_from_history_file_mock.return_value = VERSION_STUB
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:
      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      is_suse_family_mock.return_value = True
      repo_file_name = 'ambari-hdp-1'
      use_repos = { 'HDP-UTILS-1.1.0.20': repo_file_name, 'HDP-2.2': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_file="install_packages_config.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Suse', '11', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', 'HDP-2.2',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()



  @patch("ambari_commons.os_check.OSCheck.is_suse_family")
  @patch("resource_management.core.resources.packaging.Package")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_format_package_name_via_repositoryFile(self, subprocess_with_timeout, write_actual_version_to_history_file_mock,
                                                  read_actual_version_from_history_file_mock,
                                                  stack_versions_mock,
                                                  get_provider, put_structured_out_mock,
                                                  package_mock, is_suse_family_mock):
    Script.stack_version_from_distro_select = VERSION_STUB
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]
    read_actual_version_from_history_file_mock.return_value = VERSION_STUB
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:
      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      is_suse_family_mock.return_value = True


      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_repository_file.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['repositoryFile']['repoVersion'] = '2.2.0.1-990'

      repo_file_name = 'ambari-hdp-4'
      use_repos = { 'HDP-UTILS-1.1.0.20-repo-4': repo_file_name, 'HDP-2.2-repo-4': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Suse', '11', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 4,
                         'actual_version': VERSION_STUB})
      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20-repo-4',
                                base_url=u'http://repo1/HDP-UTILS/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', 'HDP-2.2-repo-4',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template=u'[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
                                )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__build_number_defined(self, subprocess_with_timeout,
                                                         write_actual_version_to_history_file_mock,
                                                         read_actual_version_from_history_file_mock,
                                                         stack_versions_mock,
                                                         put_structured_out_mock, get_provider):
    stack_versions_mock.side_effect = [
      [OLD_VERSION_STUB],  # before installation attempt
      [OLD_VERSION_STUB, VERSION_STUB]
    ]

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repoVersion'] = VERSION_STUB

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertTrue(write_actual_version_to_history_file_mock.called)
      self.assertEquals(write_actual_version_to_history_file_mock.call_args[0], (VERSION_STUB_WITHOUT_BUILD_NUMBER, VERSION_STUB))

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again
      stack_versions_mock.side_effect = [
        [OLD_VERSION_STUB, VERSION_STUB],
        [OLD_VERSION_STUB, VERSION_STUB]
      ]
      read_actual_version_from_history_file_mock.return_value = VERSION_STUB

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['repositoryFile']['repoVersion'] = VERSION_STUB

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("os.path.exists")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__build_number_not_defined_stack_root_present__no_components_installed(self,
                                                                            subprocess_with_timeout,
                                                                            exists_mock,
                                                                            write_actual_version_to_history_file_mock,
                                                                            read_actual_version_from_history_file_mock,
                                                                            stack_versions_mock,
                                                                            put_structured_out_mock, get_provider):
    exists_mock.return_value = True
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      []
    ]
    read_actual_version_from_history_file_mock.return_value = None

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)


    command_json['roleParams']['repository_version'] = VERSION_STUB_WITHOUT_BUILD_NUMBER

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager

      try:
        self.executeScript("scripts/install_packages.py",
                           classname="InstallPackages",
                           command="actionexecute",
                           config_dict=command_json,
                           target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                           os_type=('Redhat', '6.4', 'Final'),
                           )
        self.fail("Should throw exception")
      except Fail:
        pass  # Expected


      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                        { 'actual_version': '2.2.0.1-885',
                          'package_installation_result': 'FAIL',
                          'repository_version_id': 1})

      self.assertFalse(write_actual_version_to_history_file_mock.called)

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("os.path.exists")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__build_number_not_defined_stack_root_absent(self,
                                                                        subprocess_with_timeout,
                                                                        exists_mock,
                                                                        write_actual_version_to_history_file_mock,
                                                                        read_actual_version_from_history_file_mock,
                                                                        stack_versions_mock,
                                                                        put_structured_out_mock, get_provider):
    exists_mock.return_value = False
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      []
    ]
    read_actual_version_from_history_file_mock.return_value = None

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repoVersion'] = VERSION_STUB_WITHOUT_BUILD_NUMBER

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages
      available_packages.side_effect = TestInstallPackages._add_packages
      installed_packages.side_effect = TestInstallPackages._add_packages
      check_existence.return_value = False

      get_provider.return_value = pkg_manager

      try:
        self.executeScript("scripts/install_packages.py",
                           classname="InstallPackages",
                           command="actionexecute",
                           config_dict=command_json,
                           target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                           os_type=('Redhat', '6.4', 'Final'),
                           )
        self.fail("Should throw exception")
      except Fail:
        pass  # Expected

      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                        {'package_installation_result': 'FAIL',
                         'repository_version_id': 1})

      self.assertFalse(write_actual_version_to_history_file_mock.called)

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again  (correct build number, provided by other nodes, is now received from server)

      stack_versions_mock.side_effect = [
        [],  # before installation attempt
        []
      ]
      read_actual_version_from_history_file_mock.return_value = None

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['repositoryFile']['repoVersion'] = VERSION_STUB

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      try:
        self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
        self.fail("Should throw exception")
      except Fail:
        pass  # Expected

      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'FAIL',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__build_number_not_defined_stack_root_present(self,
                                                                    subprocess_with_timeout,
                                                                    write_actual_version_to_history_file_mock,
                                                                    read_actual_version_from_history_file_mock,
                                                                    stack_versions_mock,
                                                                    put_structured_out_mock, get_provider):
    stack_versions_mock.side_effect = [
      [OLD_VERSION_STUB],  # before installation attempt
      [OLD_VERSION_STUB, VERSION_STUB]
    ]

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['roleParams']['repository_version'] = VERSION_STUB_WITHOUT_BUILD_NUMBER

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertTrue(write_actual_version_to_history_file_mock.called)
      self.assertEquals(write_actual_version_to_history_file_mock.call_args[0], (VERSION_STUB_WITHOUT_BUILD_NUMBER, VERSION_STUB))

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again
      stack_versions_mock.side_effect = [
        [OLD_VERSION_STUB, VERSION_STUB],
        [OLD_VERSION_STUB, VERSION_STUB]
      ]
      read_actual_version_from_history_file_mock.return_value = VERSION_STUB

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['repositoryFile']['repoVersion'] = VERSION_STUB_WITHOUT_BUILD_NUMBER

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__wrong_build_number_specified_stack_root_present(self,
                                                                        subprocess_with_timeout,
                                                                        write_actual_version_to_history_file_mock,
                                                                        read_actual_version_from_history_file_mock,
                                                                        stack_versions_mock,
                                                                        put_structured_out_mock, get_provider):
    stack_versions_mock.side_effect = [
      [OLD_VERSION_STUB],  # before installation attempt
      [OLD_VERSION_STUB, VERSION_STUB]
    ]

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repoVersion'] = '2.2.0.1-500'  # User specified wrong build number

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertTrue(write_actual_version_to_history_file_mock.called)
      self.assertEquals(write_actual_version_to_history_file_mock.call_args[0], ('2.2.0.1', VERSION_STUB))

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again
      stack_versions_mock.side_effect = [
        [OLD_VERSION_STUB, VERSION_STUB],
        [OLD_VERSION_STUB, VERSION_STUB]
      ]
      read_actual_version_from_history_file_mock.return_value = VERSION_STUB

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['roleParams']['repository_version'] = '2.2.0.1-500'  # User specified wrong build number

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)


  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("os.path.exists")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting__wrong_build_number_specified_stack_root_absent(self,
                                                                            subprocess_with_timeout,
                                                                            exists_mock,
                                                                            write_actual_version_to_history_file_mock,
                                                                            read_actual_version_from_history_file_mock,
                                                                            stack_versions_mock,
                                                                            put_structured_out_mock, get_provider):
    exists_mock.return_value = False
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      []
    ]
    read_actual_version_from_history_file_mock.return_value = None

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repoVersion'] = VERSION_STUB_WITHOUT_BUILD_NUMBER

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      try:
        self.executeScript("scripts/install_packages.py",
                           classname="InstallPackages",
                           command="actionexecute",
                           config_dict=command_json,
                           target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                           os_type=('Redhat', '6.4', 'Final'),
                           )
        self.fail("Should throw exception")
      except Fail:
        pass  # Expected

      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args_list[-1][0][0],
                        {'package_installation_result': 'FAIL',
                         'repository_version_id': 1})

      self.assertFalse(write_actual_version_to_history_file_mock.called)

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again (correct build number, provided by other nodes, is now received from server)

      stack_versions_mock.side_effect = [
        [],  # before installation attempt
        []
      ]
      read_actual_version_from_history_file_mock.return_value = None

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['roleParams']['repository_version'] = VERSION_STUB

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      try:
        self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
        self.fail("Should throw exception")
      except Fail:
        pass  # Expected

      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'FAIL',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_version_reporting_with_repository_version(self,
                                                     subprocess_with_timeout,
                                                     write_actual_version_to_history_file_mock,
                                                     read_actual_version_from_history_file_mock,
                                                     stack_versions_mock,
                                                     put_structured_out_mock, get_provider):
    stack_versions_mock.side_effect = [
      [OLD_VERSION_STUB],  # before installation attempt
      [OLD_VERSION_STUB, VERSION_STUB]
    ]

    config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['roleParams']['repository_version'] = VERSION_STUB
    command_json['roleParams']['repository_version_id'] = '2'

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})
      self.assertTrue(write_actual_version_to_history_file_mock.called)
      self.assertEquals(write_actual_version_to_history_file_mock.call_args[0], (VERSION_STUB_WITHOUT_BUILD_NUMBER, VERSION_STUB))

      stack_versions_mock.reset_mock()
      write_actual_version_to_history_file_mock.reset_mock()
      put_structured_out_mock.reset_mock()

      # Test retrying install again
      stack_versions_mock.side_effect = [
        [OLD_VERSION_STUB, VERSION_STUB],
        [OLD_VERSION_STUB, VERSION_STUB]
      ]
      read_actual_version_from_history_file_mock.return_value = VERSION_STUB

      config_file = self.get_src_folder() + "/test/python/custom_actions/configs/install_packages_config.json"
      with open(config_file, "r") as f:
        command_json = json.load(f)

      command_json['roleParams']['repository_version'] = VERSION_STUB
      command_json['roleParams']['repository_version_id'] = '2'

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_dict=command_json,
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
                         )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 1,
                         'actual_version': VERSION_STUB})

      self.assertFalse(write_actual_version_to_history_file_mock.called)

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("resource_management.libraries.functions.stack_select.get_stack_versions")
  @patch("resource_management.libraries.functions.repo_version_history.read_actual_version_from_history_file")
  @patch("resource_management.libraries.functions.repo_version_history.write_actual_version_to_history_file")
  @patch("ambari_commons.shell.launch_subprocess")
  def test_normal_flow_rhel_with_command_repo(self,
                                              subprocess_with_timeout,
                                              write_actual_version_to_history_file_mock,
                                              read_actual_version_from_history_file_mock,
                                              stack_versions_mock,
                                              put_structured_out_mock,
                                              get_provider):
    stack_versions_mock.side_effect = [
      [],  # before installation attempt
      [VERSION_STUB]
    ]

    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages, \
      patch.object(pkg_manager, "available_packages") as available_packages, \
      patch.object(pkg_manager, "installed_packages") as installed_packages, \
      patch.object(pkg_manager, "_check_existence") as check_existence:

      all_packages.side_effect = TestInstallPackages._add_packages_available
      available_packages.side_effect = TestInstallPackages._add_packages_available
      installed_packages.side_effect = TestInstallPackages._add_packages_available
      check_existence.return_value = False

      get_provider.return_value = pkg_manager
      repo_file_name = 'ambari-hdp-4'
      use_repos = { 'HDP-UTILS-1.1.0.20-repo-4': repo_file_name, 'HDP-2.2-repo-4': repo_file_name }
      self.executeScript("scripts/install_packages.py",
                         classname="InstallPackages",
                         command="actionexecute",
                         config_file="install_packages_repository_file.json",
                         target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                         os_type=('Redhat', '6.4', 'Final'),
      )
      self.assertTrue(put_structured_out_mock.called)
      self.assertEquals(put_structured_out_mock.call_args[0][0],
                        {'package_installation_result': 'SUCCESS',
                         'repository_version_id': 4,
                         'actual_version': VERSION_STUB})

      self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20-repo-4',
                                base_url=u'http://repo1/HDP-UTILS/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP-UTILS', 'main'],
                                repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', 'HDP-2.2-repo-4',
                                base_url=u'http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
                                action=['prepare'],
                                components=[u'HDP', 'main'],
                                repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
                                repo_file_name=repo_file_name,
                                mirror_list=None,
      )
      self.assertResourceCalled('Repository', None,
                                action=['create'],
      )
      self.assertNoMoreResources()

  def test_os_family_check_with_inheritance(self):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory
    self.assertEquals(
      ManagerFactory.get_new_instance(OSConst.DEBIAN_FAMILY).__class__,
      ManagerFactory.get_new_instance(OSConst.UBUNTU_FAMILY).__class__)