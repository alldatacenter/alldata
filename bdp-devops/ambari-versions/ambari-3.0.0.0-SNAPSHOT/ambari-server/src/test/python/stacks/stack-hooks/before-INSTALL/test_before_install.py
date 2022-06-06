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

from mock.mock import MagicMock, call, patch
from resource_management import *
from stacks.utils.RMFTestCase import *
import getpass
import json

@patch.object(getpass, "getuser", new = MagicMock(return_value='some_user'))
@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):

  def test_hook_default(self):
    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Repository', 'HDP-2.6-repo-1',
        base_url = 'http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.6.4.0-60',
        action = ['prepare'],
        components = [u'HDP', 'main'],
        repo_template = '[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
        repo_file_name = None,
        mirror_list = None,
    )
    self.assertResourceCalled('Repository', 'HDP-2.6-GPL-repo-1',
        base_url = 'http://s3.amazonaws.com/dev.hortonworks.com/HDP-GPL/centos6/2.x/BUILDS/2.6.4.0-60',
        action = ['prepare'],
        components = [u'HDP-GPL', 'main'],
        repo_template = '[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
        repo_file_name = None,
        mirror_list = None,
    )
    self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.22-repo-1',
        base_url = 'http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos6',
        action = ['prepare'],
        components = [u'HDP-UTILS', 'main'],
        repo_template = '[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
        repo_file_name = None,
        mirror_list = None,
    )
    self.assertResourceCalled('Repository', None,
                              action=['create'],
    )

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()

  def test_hook_no_repos(self):

    config_file = self.get_src_folder() + "/test/python/stacks/configs/default.json"
    with open(config_file, "r") as f:
      command_json = json.load(f)

    command_json['repositoryFile']['repositories'] = []

    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_dict=command_json)

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()



  def test_hook_default_repository_file(self):
    self.executeScript("before-INSTALL/scripts/hook.py",
                       classname="BeforeInstallHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_file="repository_file.json"
    )
    self.assertResourceCalled('Repository', 'HDP-2.2-repo-4',
        action=['prepare'],
        base_url='http://repo1/HDP/centos5/2.x/updates/2.2.0.0',
        components=['HDP', 'main'],
        mirror_list=None,
        repo_file_name='ambari-hdp-4',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
    )

    self.assertResourceCalled('Repository', 'HDP-UTILS-1.1.0.20-repo-4',
        action=['prepare'],
        base_url='http://repo1/HDP-UTILS/centos5/2.x/updates/2.2.0.0',
        components=['HDP-UTILS', 'main'],
        mirror_list=None,
        repo_file_name='ambari-hdp-4',
        repo_template='[{{repo_id}}]\nname={{repo_id}}\n{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}\n\npath=/\nenabled=1\ngpgcheck=0',
    )
    self.assertResourceCalled('Repository', None,
                              action=['create'],
    )

    self.assertResourceCalled('Package', 'unzip', retry_count=5, retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'curl', retry_count=5, retry_on_repo_unavailability=False)
    self.assertNoMoreResources()
