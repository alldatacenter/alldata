#!/usr/bin/env python
# -*- coding: utf-8 -*-

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
from contextlib import contextmanager

import unittest
import signal
from mock.mock import patch, MagicMock, call
from ambari_commons import shell
from ambari_commons import OSCheck
from StringIO import StringIO

ROOT_PID = 10
ROOT_PID_CHILDREN = [10, 11, 12, 13]
shell.logger = MagicMock()  # suppress any log output

__proc_fs = {
  "/proc/10/task/10/children": "11 12",
  "/proc/10/comm": "a",
  "/proc/10/cmdline": "",

  "/proc/11/task/11/children": "13",
  "/proc/11/comm": "b",
  "/proc/11/cmdline": "",

  "/proc/12/task/12/children": "",
  "/proc/12/comm": "c",
  "/proc/12/cmdline": "",

  "/proc/13/task/13/children": "",
  "/proc/13/comm": "d",
  "/proc/13/cmdline": ""
}

__proc_fs_yum = {
  "/proc/10/task/10/children": "11",
  "/proc/10/comm": "a",
  "/proc/10/cmdline": "",

  "/proc/11/task/11/children": "",
  "/proc/11/comm": "yum",
  "/proc/11/cmdline": "yum install something"
}

# Remove any wait delay, no need for tests
__old_waiter = shell.wait_for_process_list_kill


def __wait_for_process_list_kill(pids, timeout=5, check_step_time=0.1):
  return __old_waiter(pids, 0, check_step_time)


shell.wait_for_process_list_kill = __wait_for_process_list_kill


class FakeSignals(object):
  SIGTERM = signal.SIG_IGN
  SIGKILL = signal.SIG_IGN


@contextmanager
def _open_mock(path, open_mode):
  if path in __proc_fs:
    yield StringIO(__proc_fs[path])
  else:
    yield StringIO("")


@contextmanager
def _open_mock_yum(path, open_mode):
  if path in __proc_fs:
    yield StringIO(__proc_fs_yum[path])
  else:
    yield StringIO("")


class TestShell(unittest.TestCase):

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock))
  def test_get_all_children(self):

    pid_list = [item[0] for item in shell.get_all_children(ROOT_PID)]

    self.assertEquals(len(ROOT_PID_CHILDREN), len(pid_list))
    self.assertEquals(ROOT_PID, pid_list[0])

    for i in ROOT_PID_CHILDREN:
      self.assertEquals(True, i in pid_list)

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock))
  @patch.object(OSCheck, "get_os_family", new=MagicMock(return_value="redhat"))
  @patch.object(shell, "signal", new_callable=FakeSignals)
  @patch("os.listdir")
  @patch("os.kill")
  def test_kill_process_with_children(self, os_kill_mock, os_list_dir_mock, fake_signals):
    pid_list = [item[0] for item in shell.get_all_children(ROOT_PID)]
    pid_list_str = [str(i) for i in ROOT_PID_CHILDREN]
    reverse_pid_list = sorted(pid_list, reverse=True)
    os_list_dir_mock.side_effect = [pid_list_str, [], [], []]

    shell.kill_process_with_children(ROOT_PID)

    # test pid kill by SIGTERM
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), len(pid_list))
    self.assertEquals(reverse_pid_list, os_kill_pids)

    os_kill_mock.reset_mock()
    os_list_dir_mock.reset_mock()

    os_list_dir_mock.side_effect = [pid_list_str, pid_list_str, pid_list_str, pid_list_str, [], []]
    shell.kill_process_with_children(ROOT_PID)

    # test pid kill by SIGKILL
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), len(pid_list)*2)
    self.assertEquals(reverse_pid_list + reverse_pid_list, os_kill_pids)

  @patch("__builtin__.open", new=MagicMock(side_effect=_open_mock_yum))
  @patch.object(OSCheck, "get_os_family", new=MagicMock(return_value="redhat"))
  @patch.object(shell, "signal", new_callable=FakeSignals)
  @patch("os.listdir")
  @patch("os.kill")
  def test_kill_process_with_children_except_yum(self, os_kill_mock, os_list_dir_mock, fake_signals):
    os_list_dir_mock.side_effect = [["10", "12", "20"], [], [], []]
    shell.kill_process_with_children(ROOT_PID)

    # test clean pid by SIGTERM
    os_kill_pids = [item[0][0] for item in os_kill_mock.call_args_list]
    self.assertEquals(len(os_kill_pids), 1)
    self.assertEquals([10], os_kill_pids)
