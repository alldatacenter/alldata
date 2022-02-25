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

from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, patch
from resource_management import Hook
import itertools

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("os.path.exists", new = MagicMock(return_value=True))
@patch.object(Hook, "run_custom_hook")
class TestHookBeforeSetKeytab(RMFTestCase):
  def test_hook_default(self, run_custom_hook_mock):
    self.executeScript("before-SET_KEYTAB/scripts/hook.py",
                       classname="BeforeSetKeytabHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_file="default.json",
                       call_mocks=itertools.cycle([(0, "1000")])
    )

    run_custom_hook_mock.assert_called_with('before-ANY')