"""
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
"""

import os
from unittest import TestCase

from ambari_agent.models.hooks import HookPrefix
from mock.mock import patch
from ambari_agent.CommandHooksOrchestrator import HookSequenceBuilder, ResolvedHooks, HooksOrchestrator


class TestCommandHooksOrchestrator(TestCase):
  def setUp(self):
    def injector():
      pass

    def file_cache():
      pass

    file_cache.__setattr__("get_hook_base_dir", lambda x: os.path.join("tmp"))
    injector.__setattr__("file_cache", file_cache)

    self._orchestrator = HooksOrchestrator(injector)

  @patch("os.path.isfile")
  def test_check_orchestrator(self, is_file_mock):
    is_file_mock.return_value = True

    ret = self._orchestrator.resolve_hooks({
     "commandType": "EXECUTION_COMMAND",
     "serviceName": "ZOOKEEPER",
     "role": "ZOOKEEPER_SERVER"
    }, "START")

    self.assertTrue(ret)
    self.assertEquals(len(ret.post_hooks), 3)
    self.assertEquals(len(ret.pre_hooks), 3)

  def test_hook_seq_builder(self):
    seq = list(HookSequenceBuilder().build(HookPrefix.pre, "cmd", "srv", "role"))
    seq_rev = list(HookSequenceBuilder().build(HookPrefix.post, "cmd", "srv", "role"))

    # testing base default sequence definition
    check_list = [
      "before-cmd",
      "before-cmd-srv",
      "before-cmd-srv-role"
    ]

    check_list_1 = [
      "after-cmd-srv-role",
      "after-cmd-srv",
      "after-cmd"
    ]

    self.assertEquals(seq, check_list)
    self.assertEquals(seq_rev, check_list_1)

  def test_hook_resolved(self):
    def pre():
      for i in range(1, 5):
        yield i

    def post():
      for i in range(1, 3):
        yield i

    ret = ResolvedHooks(pre(), post())

    self.assertEqual(ret.pre_hooks, list(pre()))
    self.assertEqual(ret.post_hooks, list(post()))



