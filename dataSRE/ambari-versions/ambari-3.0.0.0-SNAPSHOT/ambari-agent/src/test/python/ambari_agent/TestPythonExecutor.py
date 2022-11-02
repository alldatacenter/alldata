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

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread
import os

from ambari_agent.PythonExecutor import PythonExecutor
from ambari_agent.AmbariConfig import AmbariConfig
from mock.mock import MagicMock, patch
from ambari_commons import OSCheck
from only_for_platform import os_distro_value

@patch.object(PythonExecutor, "open_subprocess32_files", new=MagicMock(return_value =("", "")))
class TestPythonExecutor(TestCase):

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("ambari_commons.shell.kill_process_with_children")
  def test_watchdog_1(self, kill_process_with_children_mock):
    """
    Tests whether watchdog works
    """
    subproc_mock = self.subprocess32_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    _, tmpstrucout = tempfile.mkstemp()
    PYTHON_TIMEOUT_SECONDS = 0.1
    kill_process_with_children_mock.side_effect = lambda pid : subproc_mock.terminate()

    def launch_python_subprocess32_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.launch_python_subprocess32 = launch_python_subprocess32_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = None
    callback_method = MagicMock()
    thread = Thread(target =  executor.run_file, args = ("fake_puppetFile",
      ["arg1", "arg2"], tmpoutfile, tmperrfile,
      PYTHON_TIMEOUT_SECONDS, tmpstrucout, callback_method, '1'))
    thread.start()
    time.sleep(0.1)
    subproc_mock.finished_event.wait()
    self.assertEquals(subproc_mock.was_terminated, True, "subprocess32 should be terminated due to timeout")
    self.assertTrue(callback_method.called)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_watchdog_2(self):
    """
    Tries to catch false positive watchdog invocations
    """
    subproc_mock = self.subprocess32_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    _, tmpstrucout = tempfile.mkstemp()
    PYTHON_TIMEOUT_SECONDS =  5

    def launch_python_subprocess32_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.launch_python_subprocess32 = launch_python_subprocess32_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    callback_method = MagicMock()
    thread = Thread(target =  executor.run_file, args = ("fake_puppetFile", ["arg1", "arg2"],
                                                      tmpoutfile, tmperrfile,
                                                      PYTHON_TIMEOUT_SECONDS, tmpstrucout,
                                                      callback_method, "1-1"))
    thread.start()
    time.sleep(0.1)
    subproc_mock.should_finish_event.set()
    subproc_mock.finished_event.wait()
    self.assertEquals(subproc_mock.was_terminated, False, "subprocess32 should not be terminated before timeout")
    self.assertEquals(subproc_mock.returncode, 0, "subprocess32 should not be terminated before timeout")
    self.assertTrue(callback_method.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_execution_results(self):
    subproc_mock = self.subprocess32_mockup()
    executor = PythonExecutor("/tmp", AmbariConfig())
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    
    tmp_file = tempfile.NamedTemporaryFile()    # the structured out file should be preserved across calls to the hooks and script.
    tmpstructuredoutfile = tmp_file.name
    tmp_file.close()

    PYTHON_TIMEOUT_SECONDS = 5

    def launch_python_subprocess32_method(command, tmpout, tmperr):
      subproc_mock.tmpout = tmpout
      subproc_mock.tmperr = tmperr
      return subproc_mock
    executor.launch_python_subprocess32 = launch_python_subprocess32_method
    runShellKillPgrp_method = MagicMock()
    runShellKillPgrp_method.side_effect = lambda python : python.terminate()
    executor.runShellKillPgrp = runShellKillPgrp_method
    subproc_mock.returncode = 0
    subproc_mock.should_finish_event.set()
    callback_method = MagicMock()
    result = executor.run_file("file", ["arg1", "arg2"],
                               tmpoutfile, tmperrfile, PYTHON_TIMEOUT_SECONDS,
                               tmpstructuredoutfile, callback_method, "1-1")
    self.assertEquals(result, {'exitcode': 0, 'stderr': '', 'stdout': '',
                               'structuredOut': {}})
    self.assertTrue(callback_method.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_is_successfull(self):
    executor = PythonExecutor("/tmp", AmbariConfig())

    executor.python_process_has_been_killed = False
    self.assertTrue(executor.is_successful(0))
    self.assertFalse(executor.is_successful(1))

    executor.python_process_has_been_killed = True
    self.assertFalse(executor.is_successful(0))
    self.assertFalse(executor.is_successful(1))


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_python_command(self):
    executor = PythonExecutor("/tmp", AmbariConfig())
    command = executor.python_command("script", ["script_param1"])
    self.assertEqual(3, len(command))
    self.assertTrue("python" in command[0].lower())
    self.assertEquals("script", command[1])
    self.assertEquals("script_param1", command[2])

  @patch.object(os.path, "isfile")
  @patch.object(os, "rename")
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_back_up_log_file_if_exists(self, rename_mock, isfile_mock):
    # Test case when previous log file is absent
    isfile_mock.return_value = False
    log_file = "/var/lib/ambari-agent/data/output-13.txt"
    executor = PythonExecutor("/tmp", AmbariConfig())
    executor.back_up_log_file_if_exists(log_file)
    self.assertEquals(isfile_mock.called, True)
    self.assertEquals(rename_mock.called, False)

    isfile_mock.reset_mock()

    # Test case when 3 previous log files are absent
    isfile_mock.side_effect = [True, True, True, False]
    log_file = "/var/lib/ambari-agent/data/output-13.txt"
    executor = PythonExecutor("/tmp", AmbariConfig())
    executor.back_up_log_file_if_exists(log_file)
    self.assertEquals(isfile_mock.called, True)
    self.assertEquals(rename_mock.call_args_list[0][0][0], "/var/lib/ambari-agent/data/output-13.txt")
    self.assertEquals(rename_mock.call_args_list[0][0][1], "/var/lib/ambari-agent/data/output-13.txt.2")
    pass


  class subprocess32_mockup():
    """
    It's not trivial to use PyMock instead of class here because we need state
    and complex logics
    """

    returncode = 0

    started_event = threading.Event()
    should_finish_event = threading.Event()
    finished_event = threading.Event()
    was_terminated = False
    tmpout = None
    tmperr = None
    pid=-1

    def communicate(self):
      self.started_event.set()

      self.should_finish_event.wait()
      self.finished_event.set()
      pass

    def terminate(self):
      self.was_terminated = True
      self.returncode = 17
      self.should_finish_event.set()

