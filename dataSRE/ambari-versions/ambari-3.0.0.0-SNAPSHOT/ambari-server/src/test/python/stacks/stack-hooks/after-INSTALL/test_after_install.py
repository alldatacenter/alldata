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

from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select
from resource_management.libraries.script import Script

@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("os.path.isfile", new = MagicMock(return_value=False))
class TestHookAfterInstall(RMFTestCase):
  CONFIG_OVERRIDES = {"serviceName":"HIVE", "role":"HIVE_SERVER"}
  def setUp(self):
    Logger.initialize_logger()

    Script.config = dict()
    Script.config.update( { "configurations" : { "cluster-env" : {} }, "clusterLevelParams": {} } )
    Script.config["configurations"]["cluster-env"]["stack_packages"] = RMFTestCase.get_stack_packages()
    Script.config["clusterLevelParams"] = { "stack_name" : "HDP" }


  def test_hook_default(self):

    self.executeScript("after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       config_file="default.json",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_overrides = self.CONFIG_OVERRIDES
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['core-site'],
                              only_if="ls /etc/hadoop/conf",
                              xml_include_file=None)
    self.assertResourceCalled('Directory',
                              '/usr/lib/ambari-logsearch-logfeeder/conf',
                              mode = 0755,
                              cd_access = 'a',
                              create_parents = True)
    self.assertNoMoreResources()

  @patch("os.path.isdir", new = MagicMock(return_value = True))
  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_default_conf_select(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock):

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False):
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterLevelParams']['stack_version'] = "2.3"

    self.executeScript("after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES)


    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.3.0.0-1234'),
      sudo = True)

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = "/usr/hdp/2.3.0.0-1234/hadoop/conf",
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site'],
      only_if="ls /usr/hdp/2.3.0.0-1234/hadoop/conf",
      xml_include_file=None)

    self.assertResourceCalled('Directory',
                              '/usr/lib/ambari-logsearch-logfeeder/conf',
                              mode = 0755,
                              cd_access = 'a',
                              create_parents = True)

    package_dirs = conf_select.get_package_dirs();
    for package, dir_defs in package_dirs.iteritems():
      for dir_def in dir_defs:
        conf_dir = dir_def['conf_dir']
        conf_backup_dir = conf_dir + ".backup"
        current_dir = dir_def['current_dir']
        self.assertResourceCalled('Execute', ('cp', '-R', '-p', conf_dir, conf_backup_dir),
            not_if = 'test -e ' + conf_backup_dir,
            sudo = True,)

        self.assertResourceCalled('Directory', conf_dir, action = ['delete'],)
        self.assertResourceCalled('Link', conf_dir, to = current_dir,)

    self.assertNoMoreResources()

  @patch("os.path.isdir", new = MagicMock(return_value = True))
  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_default_conf_select_with_error(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock):

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False, ignore_errors = False):
      if arg2 == "pig" and not dry_run:
        if not ignore_errors:
          raise Exception("whoops")
        else:
          return None
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select
    conf_select_select_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterLevelParams']['stack_version'] = "2.3"

    self.executeScript("after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES)


    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.3.0.0-1234'),
      sudo = True)

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = "/usr/hdp/2.3.0.0-1234/hadoop/conf",
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site'],
      only_if="ls /usr/hdp/2.3.0.0-1234/hadoop/conf",
      xml_include_file=None)

    self.assertResourceCalled('Directory',
                              '/usr/lib/ambari-logsearch-logfeeder/conf',
                              mode = 0755,
                              cd_access = 'a',
                              create_parents = True)

    package_dirs = conf_select.get_package_dirs();
    for package, dir_defs in package_dirs.iteritems():
      for dir_def in dir_defs:
        conf_dir = dir_def['conf_dir']
        conf_backup_dir = conf_dir + ".backup"
        current_dir = dir_def['current_dir']
        self.assertResourceCalled('Execute', ('cp', '-R', '-p', conf_dir, conf_backup_dir),
            not_if = 'test -e ' + conf_backup_dir,
            sudo = True,)

        self.assertResourceCalled('Directory', conf_dir, action = ['delete'],)
        self.assertResourceCalled('Link', conf_dir, to = current_dir,)

    self.assertNoMoreResources()


  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_default_stack_select_specific_version(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock):
    """
    Tests that <stack-selector-tool> set all on a specific version, not a 2.3* wildcard is used when
    installing a component when the cluster version is already set.

    :param rmtree_mock:
    :param symlink_mock:
    :param conf_select_select_mock:
    :param conf_select_create_mock:
    :return:
    """

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False):
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterLevelParams']['stack_version'] = "2.3"

    self.executeScript("after-INSTALL/scripts/hook.py",
      classname="AfterInstallHook",
      command="hook",
      target=RMFTestCase.TARGET_STACK_HOOKS,
      config_dict = json_content,
      config_overrides = self.CONFIG_OVERRIDES)

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-server2', '2.3.0.0-1234'),
      sudo = True)

  @patch("os.path.isdir", new = MagicMock(return_value = True))
  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_default_conf_select_suspended(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock):

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False):
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterLevelParams']['stack_version'] = "2.3"
    json_content['roleParams']['upgrade_suspended'] = "true"

    self.executeScript("after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES)

    # same assertions as test_hook_default_conf_select, but skip hdp-select set all

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = "/usr/hdp/2.3.0.0-1234/hadoop/conf",
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site'],
      only_if="ls /usr/hdp/2.3.0.0-1234/hadoop/conf",
      xml_include_file=None)

    self.assertResourceCalled('Directory',
                              '/usr/lib/ambari-logsearch-logfeeder/conf',
                              mode = 0755,
                              cd_access = 'a',
                              create_parents = True)

    package_dirs = conf_select.get_package_dirs();
    for package, dir_defs in package_dirs.iteritems():
      for dir_def in dir_defs:
        conf_dir = dir_def['conf_dir']
        conf_backup_dir = conf_dir + ".backup"
        current_dir = dir_def['current_dir']
        self.assertResourceCalled('Execute', ('cp', '-R', '-p', conf_dir, conf_backup_dir),
            not_if = 'test -e ' + conf_backup_dir,
            sudo = True,)

        self.assertResourceCalled('Directory', conf_dir, action = ['delete'],)
        self.assertResourceCalled('Link', conf_dir, to = current_dir,)

    self.assertNoMoreResources()


  @patch("resource_management.core.Logger.warning")
  @patch("shared_initialization.load_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.conf_select.create")
  @patch("resource_management.libraries.functions.conf_select.select")
  @patch("os.symlink")
  @patch("shutil.rmtree")
  def test_hook_setup_stack_symlinks_skipped(self, rmtree_mock, symlink_mock, conf_select_select_mock, conf_select_create_mock, logger_warning_mock):
    """
    Tests that <stack-selector-tool> set all is not called on sys_prepped hosts
    :return:
    """

    def mocked_conf_select(arg1, arg2, arg3, dry_run = False):
      return "/etc/{0}/{1}/0".format(arg2, arg3)

    conf_select_create_mock.side_effect = mocked_conf_select

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterLevelParams']['stack_version'] = "2.3"
    json_content['ambariLevelParams']['host_sys_prepped'] = "true"

    self.executeScript("after-INSTALL/scripts/hook.py",
                       classname="AfterInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES)

    logger_warning_mock.assert_any_call('Skipping running stack-selector-tool because this is a sys_prepped host. This may cause symlink pointers not to be created for HDP components installed later on top of an already sys_prepped host')
