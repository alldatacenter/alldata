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
from ambari_commons import subprocess32
from mock.mock import MagicMock
from unittest import TestCase
from mock.mock import patch
import sys

from ambari_commons import OSCheck
from only_for_platform import get_platform, not_for_platform, only_for_platform, os_distro_value, PLATFORM_WINDOWS
from mock.mock import MagicMock, patch, ANY, Mock

with patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value)):
  setup_agent = __import__('setupAgent')

class TestSetupAgent(TestCase):

  @patch("sys.exit")
  @patch("socket.socket")
  def test_checkServerReachability(self, socket_mock, exit_mock):
    ret = setup_agent.checkServerReachability("localhost", 8080)
    self.assertTrue(socket_mock.called)

    s = socket_mock.return_value
    s.connect = MagicMock()
    def side_effect():
      raise Exception(1, "socket is closed")
    s.connect.side_effect = side_effect
    ret = setup_agent.checkServerReachability("localhost", 8080)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    self.assertTrue("log" in ret)
    pass


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'execOsCommand')
  def test_configureAgent(self, execOsCommand_mock):
    # Test if expected_hostname is passed
    hostname = "test.hst"
    setup_agent.configureAgent(hostname, "root")
    cmdStr = str(execOsCommand_mock.call_args_list[0][0])
    self.assertTrue(hostname in cmdStr)
    pass


  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(setup_agent, 'execOsCommand')
  @patch("os.environ")
  @patch("subprocess32.Popen")
  @patch("time.sleep")
  def test_runAgent(self, sleep_mock, popen_mock, environ_mock, execOsCommand_mock):
    expected_hostname = "test.hst"
    passphrase = "passphrase"
    agent_status = MagicMock()
    agent_status.returncode = 0
    popen_mock.return_value = agent_status
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 0}
    # Test if expected_hostname is passed
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertFalse('-v' in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertTrue(sleep_mock.called)
    self.assertEqual(execOsCommand_mock.call_count, 1)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()
    sleep_mock.reset_mock()

    # Test if verbose=True
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", True)
    self.assertTrue(expected_hostname in cmdStr)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue('-v' in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertTrue(sleep_mock.called)
    self.assertEqual(execOsCommand_mock.call_count, 1)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()
    sleep_mock.reset_mock()

    # Key 'log' not found
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertEqual(execOsCommand_mock.call_count, 3)

    execOsCommand_mock.reset_mock()
    popen_mock.reset_mock()

    # Retcode id not 0
    agent_status.returncode = 2
    execOsCommand_mock.return_value = {'log': 'log', 'exitstatus': 2}
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    cmdStr = str(popen_mock.call_args_list[0][0])
    self.assertTrue(expected_hostname in cmdStr)
    self.assertEqual(ret["exitstatus"], 2)
    execOsCommand_mock.reset_mock()
    pass

  @only_for_platform(PLATFORM_WINDOWS)
  @patch.object(setup_agent, 'run_os_command')
  @patch("os.environ")
  @patch("time.sleep")
  def test_runAgent(self, sleep_mock, environ_mock, run_os_command_mock):
    expected_hostname = "test.hst"
    passphrase = "passphrase"
    run_os_command_mock.return_value = (0, "log", "")
    # Test if expected_hostname is passed
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    self.assertEqual(run_os_command_mock.call_count, 1)
    cmdStr = str(run_os_command_mock.call_args_list[0][0])
    self.assertEqual(cmdStr, str((["cmd", "/c", "ambari-agent.cmd", "restart"],)))
    self.assertFalse('-v' in cmdStr)
    self.assertEqual(ret["exitstatus"], 0)
    self.assertFalse(sleep_mock.called)

    run_os_command_mock.reset_mock()
    sleep_mock.reset_mock()

    # Retry command
    run_os_command_mock.side_effect = [(2, "log", "err"), (0, "log", "")]
    ret = setup_agent.runAgent(passphrase, expected_hostname, "root", False)
    self.assertEqual(run_os_command_mock.call_count, 2)
    self.assertTrue("Retrying" in ret['log'][1])
    self.assertEqual(ret["exitstatus"], 0)
    self.assertTrue(sleep_mock.called)
    pass

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_suse(self, findNearestAgentPackageVersion_method, is_ubuntu_family_method,
                                                       is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = True
    is_ubuntu_family_method.return_value = False

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_ubuntu(self, findNearestAgentPackageVersion_method, is_ubuntu_family_method,
                                                       is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = True

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @only_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial_on_suse(self, findNearestAgentPackageVersion_method,
                                                       getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest_on_suse(self, findNearestAgentPackageVersion_method,
                                                       is_ubuntu_family_method,
                                                       is_suse_family_method):
    is_suse_family_method.return_value = True
    is_ubuntu_family_method.return_value = False

    projectVersion = ""
    nearest_version = projectVersion + "1.1.1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_nearest_on_ubuntu(self, findNearestAgentPackageVersion_method,
                                                       is_ubuntu_family_method,
                                                       is_suse_family_method):
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = True

    projectVersion = ""
    nearest_version = projectVersion + "1.1.1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": [nearest_version, ""]
    }

    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @only_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  def test_returned_optimal_version_is_nearest_on_windows(self, findNearestAgentPackageVersion_method,
                                                          getAvailableAgentPackageVersions_method):
    projectVersion = ""
    nearest_version = projectVersion + "1.1.1"
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": [nearest_version, ""]
    }
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": nearest_version}

    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["exitstatus"] == 1)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_initial(self, findNearestAgentPackageVersion_method,
                                               is_ubuntu_family_method,
                                               is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = False

    projectVersion = "1.1.1"
    result_version = setup_agent.getOptimalVersion(projectVersion)
    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertTrue(result_version["log"] == projectVersion)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'getAvailableAgentPackageVersions')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'findNearestAgentPackageVersion')
  def test_returned_optimal_version_is_default(self, findNearestAgentPackageVersion_method,
                                               is_ubuntu_family_method,
                                               is_suse_family_method, getAvailableAgentPackageVersions_method):
    getAvailableAgentPackageVersions_method.return_value = {"exitstatus": 0, "log": "1.1.1"}
    is_suse_family_method.return_value = False
    is_ubuntu_family_method.return_value = False
    findNearestAgentPackageVersion_method.return_value = {
      "exitstatus": 0,
      "log": ["1.1.1.1", ""]
    }

    projectVersion = "1.1.2"
    result_version = setup_agent.getOptimalVersion(projectVersion)

    self.assertTrue(findNearestAgentPackageVersion_method.called)
    self.assertEqual(result_version["exitstatus"], 1)
    pass

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(subprocess32, 'Popen')
  def test_execOsCommand(self, Popen_mock):
    self.assertIsNone(setup_agent.execOsCommand("hostname -f"))

  @only_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(subprocess32, 'Popen')
  def test_execOsCommand(self, Popen_mock):
    p = MagicMock()
    p.communicate.return_value = ("", "")
    p.returncode = 0
    Popen_mock.return_value = p
    retval = setup_agent.execOsCommand("hostname -f")
    self.assertEqual(retval["exitstatus"], 0)
    pass

  @patch("os.path.exists")
  @patch.object(setup_agent, "get_ambari_repo_file_full_name")
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'checkVerbose')
  @patch.object(setup_agent, 'isAgentPackageAlreadyInstalled')
  @patch.object(setup_agent, 'runAgent')
  @patch.object(setup_agent, 'configureAgent')
  @patch.object(setup_agent, 'installAgent')
  @patch('ambari_commons.OSCheck.is_suse_family')
  @patch('ambari_commons.OSCheck.is_ubuntu_family')
  @patch.object(setup_agent, 'getOptimalVersion')
  @patch.object(setup_agent, 'checkServerReachability')
  @patch("sys.exit")
  @patch("os.path.dirname")
  @patch("os.path.realpath")
  def test_setup_agent_main(self, dirname_mock, realpath_mock, exit_mock, checkServerReachability_mock,
                            getOptimalVersion_mock, is_ubuntu_family_mock, is_suse_family_mock,
                            installAgent_mock, configureAgent_mock, runAgent_mock,
                            isAgentPackageAlreadyInstalled_mock, checkVerbose_mock,
                            get_ambari_repo_file_full_name_mock, os_path_exists_mock):
    checkServerReachability_mock.return_value = {'log': 'log', 'exitstatus': 0}
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    configureAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    runAgent_mock.return_value = {'log': 'log', 'exitstatus': 0}
    getOptimalVersion_mock.return_value = {'log': '1.1.2, 1.1.3, ', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertTrue(checkVerbose_mock.called)
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    self.assertTrue(getOptimalVersion_mock.called)

    # A version of ambari-agent was installed or available in the repo file
    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()

    getOptimalVersion_mock.return_value = {'log': '1.1.1', 'exitstatus': 0}
    isAgentPackageAlreadyInstalled_mock.return_value = False
    is_suse_family_mock.return_value = True
    is_ubuntu_family_mock.return_value = False
    get_ambari_repo_file_full_name_mock.return_value = "Ambari_Repo_File"

    def os_path_exists_side_effect(*args, **kwargs):
      # Ambari repo file exists
      if (args[0] == get_ambari_repo_file_full_name_mock()):
        return True
      else:
        return False

    os_path_exists_mock.side_effect = os_path_exists_side_effect
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue(isAgentPackageAlreadyInstalled_mock.called)
    self.assertTrue(installAgent_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    getOptimalVersion_mock.return_value = {'log': '', 'exitstatus': 0}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertFalse(isAgentPackageAlreadyInstalled_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = False
    getOptimalVersion_mock.return_value = {'log': '1.1.1', 'exitstatus': 0}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue(isAgentPackageAlreadyInstalled_mock.called)
    self.assertTrue(installAgent_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","{ambariVersion}","8080"))
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = False
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","null","null"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 0)

    exit_mock.reset_mock()
    #if "yum -y install --nogpgcheck ambari-agent" return not 0 result
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)

    installAgent_mock.reset_mock()
    exit_mock.reset_mock()
    #if suse
    is_suse_family_mock.return_value = True
    is_ubuntu_family_mock.return_value = False
    #if "zypper install -y ambari-agent" return not 0 result
    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)

    exit_mock.reset_mock()
    #if ubuntu
    is_suse_family_mock.return_value = False
    is_ubuntu_family_mock.return_value = True

    installAgent_mock.return_value = {'log': 'log', 'exitstatus': 1}
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)

    # A version of ambari-agent was installed and ambari repo file was not found
    exit_mock.reset_mock()
    installAgent_mock.reset_mock()
    getOptimalVersion_mock.reset_mock()
    os_path_exists_mock.side_effect = None
    os_path_exists_mock.return_value = False #ambari repo file was not found
    getOptimalVersion_mock.return_value = {'log': '1.1.1', 'exitstatus': 0}
    isAgentPackageAlreadyInstalled_mock.return_value = False
    is_suse_family_mock.return_value = True
    is_ubuntu_family_mock.return_value = False
    get_ambari_repo_file_full_name_mock.return_value = "Ambari_Repo_File"

    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertTrue(isAgentPackageAlreadyInstalled_mock.called)
    self.assertFalse(installAgent_mock.called) # ambari repo file is missing, so installAgent() will not be called
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 2) # ambari repo file not found error code
    self.assertTrue(get_ambari_repo_file_full_name_mock() in ret["log"]) # ambari repo file not found message

    # ambari-agent was not installed and ambari repo file was not found
    exit_mock.reset_mock()
    os_path_exists_mock.side_effect = None
    os_path_exists_mock.return_value = False # ambari repo file not found
    getOptimalVersion_mock.reset_mock()
    isAgentPackageAlreadyInstalled_mock.reset_mock()
    is_suse_family_mock.reset_mock()
    is_ubuntu_family_mock.reset_mock()
    installAgent_mock.reset_mock()

    getOptimalVersion_mock.return_value = {'log': '', 'exitstatus': 1}     #ambari-agent not installed
    ret = setup_agent.main(("setupAgent.py","agents_host","password", "server_hostname","1.1.1","8080"))
    self.assertFalse(exit_mock.called)
    self.assertTrue(getOptimalVersion_mock.called)
    self.assertFalse(isAgentPackageAlreadyInstalled_mock.called)
    self.assertFalse(is_suse_family_mock.called)
    self.assertFalse(is_ubuntu_family_mock.called)
    self.assertTrue("exitstatus" in ret)
    self.assertEqual(ret["exitstatus"], 1)
    self.assertTrue(get_ambari_repo_file_full_name_mock() in ret["log"]) # ambari repo file not found message
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_findNearestAgentPackageVersion(self, execOsCommand_mock):
    setup_agent.findNearestAgentPackageVersion("1.1.1")
    self.assertTrue(execOsCommand_mock.called)
    execOsCommand_mock.reset_mock()
    setup_agent.findNearestAgentPackageVersion("")
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_isAgentPackageAlreadyInstalled(self, execOsCommand_mock):
    execOsCommand_mock.return_value = {"exitstatus": 0, "log": "1.1.1"}
    self.assertTrue(setup_agent.isAgentPackageAlreadyInstalled("1.1.1"))
    self.assertTrue(execOsCommand_mock.called)
    execOsCommand_mock.reset_mock()
    execOsCommand_mock.return_value = {"exitstatus": 1, "log": "1.1.1"}
    self.assertFalse(setup_agent.isAgentPackageAlreadyInstalled("1.1.1"))
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(setup_agent, 'execOsCommand')
  def test_getAvailableAgentPackageVersions(self, execOsCommand_mock):
    setup_agent.getAvailableAgentPackageVersions()
    self.assertTrue(execOsCommand_mock.called)
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(setup_agent, 'execOsCommand')
  def test_installAgent(self, execOsCommand_mock):
    setup_agent.installAgent("1.1.1")
    self.assertTrue(execOsCommand_mock.called)
    pass
