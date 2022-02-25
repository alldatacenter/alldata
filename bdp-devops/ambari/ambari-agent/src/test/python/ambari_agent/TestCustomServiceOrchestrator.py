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
import ConfigParser
from multiprocessing.pool import ThreadPool
import os

import pprint

from ambari_agent.models.commands import CommandStatus
from ambari_commons import shell

from unittest import TestCase
import threading
import tempfile
import time
import traceback
from threading import Thread

from mock.mock import MagicMock, patch
import StringIO
import sys

from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AgentException import AgentException
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.FileCache import FileCache
from ambari_agent.PythonExecutor import PythonExecutor
from ambari_commons import OSCheck
from only_for_platform import get_platform, os_distro_value, PLATFORM_WINDOWS
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ConfigurationBuilder import ConfigurationBuilder

class TestCustomServiceOrchestrator:#(TestCase):


  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out
    # generate sample config
    tmpdir = tempfile.gettempdir()
    exec_tmp_dir = os.path.join(tmpdir, 'tmp')
    self.config = AmbariConfig()
    self.config.config = ConfigParser.RawConfigParser()
    self.config.add_section('agent')
    self.config.set('agent', 'prefix', tmpdir)
    self.config.set('agent', 'cache_dir', "/cachedir")
    self.config.add_section('python')
    self.config.set('python', 'custom_actions_dir', tmpdir)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("ambari_agent.hostname.public_hostname")
  @patch("os.path.isfile")
  @patch("os.unlink")
  @patch.object(FileCache, "__init__")
  def test_dump_command_to_json_with_retry(self, FileCache_mock, unlink_mock,
                                isfile_mock, hostname_mock):
    FileCache_mock.return_value = None
    hostname_mock.return_value = "test.hst"
    command = {
      'commandType': 'EXECUTION_COMMAND',
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'commandId': '1-1',
      'taskId': 3,
      'clusterName': u'cc',
      'serviceName': u'HDFS',
      'configurations':{'global' : {}},
      'configurationTags':{'global' : { 'tag': 'v1' }},
      'clusterHostInfo':{'namenode_host' : ['1'],
                         'slave_hosts'   : ['0', '1'],
                         'all_racks'   : [u'/default-rack:0'],
                         'ambari_server_host' : 'a.b.c',
                         'ambari_server_port' : '123',
                         'ambari_server_use_ssl' : 'false',
                         'all_ipv4_ips'   : [u'192.168.12.101:0'],
                         'all_hosts'     : ['h1.hortonworks.com', 'h2.hortonworks.com'],
                         'all_ping_ports': ['8670:0,1']},
      'hostLevelParams':{}
    }

    tempdir = tempfile.gettempdir()
    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.config.set('agent', 'prefix', tempdir)
    orchestrator = CustomServiceOrchestrator(initializer_module)
    isfile_mock.return_value = True
    # Test dumping EXECUTION_COMMAND
    json_file = orchestrator.dump_command_to_json(command)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    if get_platform() != PLATFORM_WINDOWS:
      self.assertEqual(oct(os.stat(json_file).st_mode & 0777), '0600')
    self.assertTrue(json_file.endswith("command-3.json"))
    os.unlink(json_file)
    # Test dumping STATUS_COMMAND
    json_file = orchestrator.dump_command_to_json(command, True)
    self.assertTrue(os.path.exists(json_file))
    self.assertTrue(os.path.getsize(json_file) > 0)
    if get_platform() != PLATFORM_WINDOWS:
      self.assertEqual(oct(os.stat(json_file).st_mode & 0777), '0600')
    self.assertTrue(json_file.endswith("command-3.json"))
    os.unlink(json_file)
    # Testing side effect of dump_command_to_json
    self.assertNotEquals(command['clusterHostInfo'], {})
    self.assertTrue(unlink_mock.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("os.path.exists")
  @patch.object(FileCache, "__init__")
  def test_resolve_script_path(self, FileCache_mock, exists_mock):
    FileCache_mock.return_value = None
    dummy_controller = MagicMock()
    config = AmbariConfig()
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    # Testing existing path
    exists_mock.return_value = True
    path = orchestrator.\
      resolve_script_path(os.path.join("HBASE", "package"), os.path.join("scripts", "hbase_master.py"))
    self.assertEqual(os.path.join("HBASE", "package", "scripts", "hbase_master.py"), path)
    # Testing not existing path
    exists_mock.return_value = False
    try:
      orchestrator.resolve_script_path("/HBASE",
                                       os.path.join("scripts", "hbase_master.py"))
      self.fail('ExpectedException not thrown')
    except AgentException:
      pass # Expected

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(FileCache, "get_custom_resources_subdir")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(CustomServiceOrchestrator, "resolve_hook_script_path")
  @patch.object(FileCache, "get_host_scripts_base_dir")
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(FileCache, "get_hook_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  def test_runCommand(self, FileCache_mock,
                      run_file_mock, dump_command_to_json_mock,
                      get_hook_base_dir_mock, get_service_base_dir_mock, 
                      get_host_scripts_base_dir_mock, 
                      resolve_hook_script_path_mock, 
                      resolve_script_path_mock,
                      get_custom_resources_subdir_mock, get_configuration_mock):
    
    FileCache_mock.return_value = None
    command = {
      'commandType' : 'EXECUTION_COMMAND',
      'role' : 'REGION_SERVER',
      'clusterLevelParams' : {
        'stack_name' : 'HDP',
        'stack_version' : '2.0.7',
      },
      'ambariLevelParams': {
        'jdk_location' : 'some_location'
      },
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'scripts/hbase_regionserver.py',
        'command_timeout': '600',
        'service_package_folder' : 'HBASE'
      },
      'taskId' : '3',
      'roleCommand': 'INSTALL',
      'clusterId': '-1',
    }
    get_configuration_mock.return_value = command
    
    get_host_scripts_base_dir_mock.return_value = "/host_scripts"
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    resolve_hook_script_path_mock.return_value = \
      ('/hooks_dir/prefix-command/scripts/hook.py',
       '/hooks_dir/prefix-command')
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    orchestrator.commands_in_progress = {command['taskId']: unix_process_id}
    get_hook_base_dir_mock.return_value = "/hooks/"
    # normal run case
    run_file_mock.return_value = {
        'stdout' : 'sss',
        'stderr' : 'eee',
        'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 0)
    self.assertTrue(run_file_mock.called)
    self.assertEqual(run_file_mock.call_count, 3)

    # running a status command
    run_file_mock.reset_mock()
    def return_traceback(*args, **kwargs):
      return {
        'stderr': traceback.format_exc(),
        'stdout': '',
        'exitcode': 0,
      }
    run_file_mock.side_effect = return_traceback

    status_command = dict(command)
    status_command['commandType'] = 'STATUS_COMMAND'
    del status_command['taskId']
    del status_command['roleCommand']
    ret = orchestrator.runCommand(status_command, "out.txt", "err.txt")
    self.assertEqual('None\n', ret['stderr'])

    run_file_mock.reset_mock()

    # Case when we force another command
    run_file_mock.return_value = {
        'stdout' : 'sss',
        'stderr' : 'eee',
        'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt",
              forced_command_name=CustomServiceOrchestrator.SCRIPT_TYPE_PYTHON)
    ## Check that override_output_files was true only during first call
    print run_file_mock
    self.assertEquals(run_file_mock.call_args_list[0][0][8], True)
    self.assertEquals(run_file_mock.call_args_list[1][0][8], False)
    self.assertEquals(run_file_mock.call_args_list[2][0][8], False)
    ## Check that forced_command_name was taken into account
    self.assertEqual(run_file_mock.call_args_list[0][0][1][0],
                                  CustomServiceOrchestrator.SCRIPT_TYPE_PYTHON)

    run_file_mock.reset_mock()

    # unknown script type case
    command['commandParams']['script_type'] = "SOME_TYPE"
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 1)
    self.assertFalse(run_file_mock.called)
    self.assertTrue("Unknown script type" in ret['stdout'])

    #By default returns empty dictionary
    self.assertEqual(ret['structuredOut'], '{}')

    pass

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch("ambari_commons.shell.kill_process_with_children")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(CustomServiceOrchestrator, "resolve_hook_script_path")
  @patch.object(FileCache, "get_host_scripts_base_dir")
  @patch.object(FileCache, "get_service_base_dir")
  @patch.object(FileCache, "get_hook_base_dir")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  def test_cancel_command(self, FileCache_mock,
                      run_file_mock, dump_command_to_json_mock,
                      get_hook_base_dir_mock, get_service_base_dir_mock,
                      get_host_scripts_base_dir_mock,
                      resolve_hook_script_path_mock, resolve_script_path_mock,
                      kill_process_with_children_mock, get_configuration_mock):
    FileCache_mock.return_value = None
    command = {
      'role' : 'REGION_SERVER',
      'clusterLevelParams' : {
        'stack_name' : 'HDP',
        'stack_version' : '2.0.7'
      },
      'ambariLevelParams': {
        'jdk_location' : 'some_location'
      },
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'scripts/hbase_regionserver.py',
        'command_timeout': '600',
        'service_package_folder' : 'HBASE'
      },
      'taskId' : '3',
      'roleCommand': 'INSTALL',
      'clusterId': '-1'
    }
    get_configuration_mock.return_value = command
        
    get_host_scripts_base_dir_mock.return_value = "/host_scripts"
    get_service_base_dir_mock.return_value = "/basedir/"
    resolve_script_path_mock.return_value = "/basedir/scriptpath"
    resolve_hook_script_path_mock.return_value = \
      ('/hooks_dir/prefix-command/scripts/hook.py',
       '/hooks_dir/prefix-command')
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    orchestrator.commands_in_progress = {command['taskId']: unix_process_id}
    get_hook_base_dir_mock.return_value = "/hooks/"
    run_file_mock_return_value = {
      'stdout' : 'killed',
      'stderr' : 'killed',
      'exitcode': 1,
      }
    def side_effect(*args, **kwargs):
      time.sleep(0.2)
      return run_file_mock_return_value
    run_file_mock.side_effect = side_effect

    _, out = tempfile.mkstemp()
    _, err = tempfile.mkstemp()
    pool = ThreadPool(processes=1)
    async_result = pool.apply_async(orchestrator.runCommand, (command, out, err))

    time.sleep(0.1)
    orchestrator.cancel_command(command['taskId'], 'reason')

    ret = async_result.get()

    self.assertEqual(ret['exitcode'], 1)
    self.assertEquals(ret['stdout'], 'killed\nCommand aborted. Reason: \'reason\'')
    self.assertEquals(ret['stderr'], 'killed\nCommand aborted. Reason: \'reason\'')

    self.assertTrue(kill_process_with_children_mock.called)
    self.assertFalse(command['taskId'] in orchestrator.commands_in_progress.keys())
    self.assertTrue(os.path.exists(out))
    self.assertTrue(os.path.exists(err))
    try:
      os.remove(out)
      os.remove(err)
    except:
      pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(CustomServiceOrchestrator, "get_py_executor")
  @patch("ambari_commons.shell.kill_process_with_children")
  @patch.object(FileCache, "__init__")
  @patch.object(CustomServiceOrchestrator, "resolve_script_path")
  @patch.object(CustomServiceOrchestrator, "resolve_hook_script_path")
  def test_cancel_backgound_command(self, resolve_hook_script_path_mock,
                                    resolve_script_path_mock, FileCache_mock, kill_process_with_children_mock,
                                    get_py_executor_mock, get_configuration_mock):
    FileCache_mock.return_value = None
    FileCache_mock.cache_dir = MagicMock()
    resolve_hook_script_path_mock.return_value = None
    dummy_controller = MagicMock()
    cfg = AmbariConfig()
    cfg.set('agent', 'tolerate_download_failures', 'true')
    cfg.set('agent', 'prefix', '.')
    cfg.set('agent', 'cache_dir', 'background_tasks')

    initializer_module = InitializerModule()
    initializer_module.init()
    
    actionQueue = ActionQueue(initializer_module)
    orchestrator = CustomServiceOrchestrator(initializer_module)

    initializer_module.actionQueue = actionQueue
    
    orchestrator.file_cache = MagicMock()
    def f (a, b):
      return ""
    orchestrator.file_cache.get_service_base_dir = f
    actionQueue.customServiceOrchestrator = orchestrator

    import TestActionQueue
    import copy

    pyex = PythonExecutor(actionQueue.customServiceOrchestrator.tmp_dir, actionQueue.customServiceOrchestrator.config)
    TestActionQueue.patch_output_file(pyex)
    pyex.prepare_process_result = MagicMock()
    get_py_executor_mock.return_value = pyex
    orchestrator.dump_command_to_json = MagicMock()

    lock = threading.RLock()
    complete_done = threading.Condition(lock)

    complete_was_called = {}
    def command_complete_w(process_condenced_result, handle):
      with lock:
        complete_was_called['visited']= ''
        complete_done.wait(3)

    actionQueue.on_background_command_complete_callback = TestActionQueue.wraped(actionQueue.on_background_command_complete_callback, command_complete_w, None)
    execute_command = copy.deepcopy(TestActionQueue.TestActionQueue.background_command)
    get_configuration_mock.return_value = execute_command
    
    actionQueue.put([execute_command])
    actionQueue.process_background_queue_safe_empty()

    time.sleep(.1)

    orchestrator.cancel_command(19,'reason')
    self.assertTrue(kill_process_with_children_mock.called)
    kill_process_with_children_mock.assert_called_with(33)

    with lock:
      complete_done.notifyAll()

    with lock:
      self.assertTrue(complete_was_called.has_key('visited'))

    time.sleep(.1)

    runningCommand = actionQueue.commandStatuses.get_command_status(19)
    self.assertTrue(runningCommand is not None)
    self.assertEqual(runningCommand['status'], CommandStatus.failed)


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(AmbariConfig, "get")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(PythonExecutor, "run_file")
  @patch.object(FileCache, "__init__")
  @patch.object(FileCache, "get_custom_actions_base_dir")
  def test_runCommand_custom_action(self, get_custom_actions_base_dir_mock,
                                    FileCache_mock,
                                    run_file_mock, dump_command_to_json_mock, ambari_config_get, get_configuration_mock):
    ambari_config_get.return_value = "0"
    FileCache_mock.return_value = None
    get_custom_actions_base_dir_mock.return_value = "some path"
    _, script = tempfile.mkstemp()
    command = {
      'role' : 'any',
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'some_custom_action.py',
        'command_timeout': '600',
      },
      'ambariLevelParams': {
        'jdk_location' : 'some_location'
      },
      'taskId' : '3',
      'roleCommand': 'ACTIONEXECUTE',
      'clusterId': '-1',
    }
    get_configuration_mock.return_value = command
    
    initializer_module = InitializerModule()
    initializer_module.config = self.config
    initializer_module.init()
    
    
    orchestrator = CustomServiceOrchestrator(initializer_module)
    unix_process_id = 111
    orchestrator.commands_in_progress = {command['taskId']: unix_process_id}
    # normal run case
    run_file_mock.return_value = {
      'stdout' : 'sss',
      'stderr' : 'eee',
      'exitcode': 0,
      }
    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 0)
    self.assertTrue(run_file_mock.called)
    # Hoooks are not supported for custom actions,
    # that's why run_file() should be called only once
    self.assertEqual(run_file_mock.call_count, 1)


  @patch("os.path.isfile")
  @patch.object(FileCache, "__init__")
  def test_resolve_hook_script_path(self, FileCache_mock, isfile_mock):
    FileCache_mock.return_value = None
    dummy_controller = MagicMock()
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    # Testing None param
    res1 = orchestrator.resolve_hook_script_path(None, "prefix", "command",
                                            "script_type")
    self.assertEqual(res1, None)
    # Testing existing hook script
    isfile_mock.return_value = True
    res2 = orchestrator.resolve_hook_script_path("hooks_dir", "prefix", "command",
                                            "script_type")
    self.assertEqual(res2, (os.path.join('hooks_dir', 'prefix-command', 'scripts', 'hook.py'),
                            os.path.join('hooks_dir', 'prefix-command')))
    # Testing not existing hook script
    isfile_mock.return_value = False
    res3 = orchestrator.resolve_hook_script_path("hooks_dir", "prefix", "command",
                                                 "script_type")
    self.assertEqual(res3, None)


  @patch.object(CustomServiceOrchestrator, "runCommand")
  @patch.object(FileCache, "__init__")
  def test_requestComponentStatus(self, FileCache_mock, runCommand_mock):
    FileCache_mock.return_value = None
    status_command = {
      "serviceName" : 'HDFS',
      "commandType" : "STATUS_COMMAND",
      "clusterName" : "",
      "componentName" : "DATANODE",
      'configurations':{}
    }
    dummy_controller = MagicMock()
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)
    # Test alive case
    runCommand_mock.return_value = {
      "exitcode" : 0
    }

    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(runCommand_mock.return_value, status)

    # Test dead case
    runCommand_mock.return_value = {
      "exitcode" : 1
    }
    status = orchestrator.requestComponentStatus(status_command)
    self.assertEqual(runCommand_mock.return_value, status)


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(CustomServiceOrchestrator, "get_py_executor")
  @patch.object(CustomServiceOrchestrator, "dump_command_to_json")
  @patch.object(FileCache, "__init__")
  @patch.object(FileCache, "get_custom_actions_base_dir")
  def test_runCommand_background_action(self, get_custom_actions_base_dir_mock,
                                    FileCache_mock,
                                    dump_command_to_json_mock,
                                    get_py_executor_mock, get_configuration_mock):
    FileCache_mock.return_value = None
    get_custom_actions_base_dir_mock.return_value = "some path"
    _, script = tempfile.mkstemp()
    command = {
      'role' : 'any',
      'commandParams': {
        'script_type': 'PYTHON',
        'script': 'some_custom_action.py',
        'command_timeout': '600',
      },
      'ambariLevelParams': {
        'jdk_location' : 'some_location'
      },
      'clusterId': '-1',
      'taskId' : '13',
      'roleCommand': 'ACTIONEXECUTE',
      'commandType': 'BACKGROUND_EXECUTION_COMMAND',
      '__handle': BackgroundCommandExecutionHandle({'taskId': '13'}, 13,
                                                   MagicMock(), MagicMock())
    }
    initializer_module = InitializerModule()
    initializer_module.init()
    orchestrator = CustomServiceOrchestrator(initializer_module)

    import TestActionQueue
    pyex = PythonExecutor(orchestrator.tmp_dir, orchestrator.config)
    TestActionQueue.patch_output_file(pyex)
    pyex.condense_output = MagicMock()
    get_py_executor_mock.return_value = pyex
    orchestrator.dump_command_to_json = MagicMock()

    ret = orchestrator.runCommand(command, "out.txt", "err.txt")
    self.assertEqual(ret['exitcode'], 777)

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


