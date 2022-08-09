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
import bootstrap
import time
from ambari_commons import subprocess32
import os
import logging
import tempfile
import pprint

from ambari_commons.os_check import OSCheck
from bootstrap import PBootstrap, Bootstrap, BootstrapDefault, SharedState, HostLog, SCP, SSH
from unittest import TestCase
from ambari_commons.subprocess32 import Popen
from bootstrap import AMBARI_PASSPHRASE_VAR_NAME
from mock.mock import MagicMock, call
from mock.mock import patch
from mock.mock import create_autospec
from only_for_platform import not_for_platform, os_distro_value, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestBootstrap:#(TestCase):

  def setUp(self):
    logging.basicConfig(level=logging.ERROR)


  def test_getRemoteName(self):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                      "setupAgentFile", "ambariServer", "centos6", None, "8440", "root")
    res = bootstrap_obj = Bootstrap("hostname", shared_state)
    utime1 = 1234
    utime2 = 12345
    bootstrap_obj.getUtime = MagicMock(return_value=utime1)
    remote1 = bootstrap_obj.getRemoteName("/tmp/setupAgent.sh")
    self.assertEquals(remote1, "/tmp/setupAgent{0}.sh".format(utime1))

    bootstrap_obj.getUtime.return_value=utime2
    remote1 = bootstrap_obj.getRemoteName("/tmp/setupAgent.sh")
    self.assertEquals(remote1, "/tmp/setupAgent{0}.sh".format(utime1))

    remote2 = bootstrap_obj.getRemoteName("/tmp/host_pass")
    self.assertEquals(remote2, "/tmp/host_pass{0}".format(utime2))


  # TODO: Test bootstrap timeout

  # TODO: test_return_error_message_for_missing_sudo_package

  def test_getAmbariPort(self):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.getAmbariPort(),"8440")
    shared_state.server_port = None
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.getAmbariPort(),"null")


  @patch.object(subprocess32, "Popen")
  @patch("sys.stderr")
  @patch("sys.exit")
  @patch.object(PBootstrap, "run")
  @patch("os.path.dirname")
  @patch("os.path.realpath")
  def test_bootstrap_main(self, dirname_mock, realpath_mock, run_mock, exit_mock, stderr_mock, subprocess32_Popen_mock):
    bootstrap.main(["bootstrap.py", "hostname,hostname2", "/tmp/bootstrap", "root", "123", "sshkey_file", "setupAgent.py", "ambariServer", \
                    "centos6", "1.1.1", "8440", "root", "passwordfile"])
    self.assertTrue(run_mock.called)
    run_mock.reset_mock()
    bootstrap.main(["bootstrap.py", "hostname,hostname2", "/tmp/bootstrap", "root", "123", "sshkey_file", "setupAgent.py", "ambariServer", \
                    "centos6", "1.1.1", "8440", "root", None])
    self.assertTrue(run_mock.called)
    run_mock.reset_mock()
    def side_effect(retcode):
      raise Exception(retcode, "sys.exit")
    exit_mock.side_effect = side_effect
    try:
      bootstrap.main(["bootstrap.py","hostname,hostname2", "/tmp/bootstrap"])
      self.fail("sys.exit(2)")
    except Exception:
    # Expected
      pass
    self.assertTrue(exit_mock.called)


  @patch("os.environ")
  def test_getRunSetupWithPasswordCommand(self, environ_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    environ_mock.__getitem__.return_value = "TEST_PASSPHRASE"
    bootstrap_obj = Bootstrap("hostname", shared_state)
    utime = 1234
    bootstrap_obj.getUtime = MagicMock(return_value=utime)
    ret = bootstrap_obj.getRunSetupWithPasswordCommand("hostname")
    expected = "/var/lib/ambari-agent/tmp/ambari-sudo.sh -S python /var/lib/ambari-agent/tmp/setupAgent{0}.py hostname TEST_PASSPHRASE " \
               "ambariServer root  8440 < /var/lib/ambari-agent/tmp/host_pass{0}".format(utime)
    self.assertEquals(ret, expected)


  def test_generateRandomFileName(self):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertTrue(bootstrap_obj.generateRandomFileName(None) == bootstrap_obj.getUtime())



  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(OSCheck, "is_suse_family")
  def test_getRepoDir(self, is_suse_family, is_redhat_family):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Suse
    is_redhat_family.return_value = False
    is_suse_family.return_value = True
    res = bootstrap_obj.getRepoDir()
    self.assertEquals(res, "/etc/zypp/repos.d")
    # non-Suse
    is_suse_family.return_value = False
    is_redhat_family.return_value = True
    res = bootstrap_obj.getRepoDir()
    self.assertEquals(res, "/etc/yum.repos.d")

  def test_getSetupScript(self):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    self.assertEquals(bootstrap_obj.shared_state.script_dir, "scriptDir")


  def test_run_setup_agent_command_ends_with_project_version(self):
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = "1.1.1"
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               version, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    runSetupCommand = bootstrap_obj.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith(version + " 8440"))


  def test_agent_setup_command_without_project_version(self):
    os.environ[AMBARI_PASSPHRASE_VAR_NAME] = ""
    version = None
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               version, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    runSetupCommand = bootstrap_obj.getRunSetupCommand("hostname")
    self.assertTrue(runSetupCommand.endswith(" 8440"))


  # TODO: test_os_check_fail_fails_bootstrap_execution


  def test_host_log(self):
    tmp_file, tmp_filename  = tempfile.mkstemp()
    dummy_log = HostLog(tmp_filename)
    # First write to log
    dummy_log.write("a\nb\nc")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\n"
      self.assertEquals(s, etalon)
    # Next write
    dummy_log.write("Yet another string")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\nYet another string\n"
      self.assertEquals(s, etalon)
    # Should not append line end if it already exists
    dummy_log.write("line break->\n")
    # Read it
    with open(tmp_filename) as f:
      s = f.read()
      etalon = "a\nb\nc\nYet another string\nline break->\n"
      self.assertEquals(s, etalon)
    # Cleanup
    os.unlink(tmp_filename)


  @patch.object(subprocess32, "Popen")
  def test_SCP(self, popenMock):
    params = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                                  "setupAgentFile", "ambariServer", "centos6",
                                  "1.2.1", "8440", "root")
    host_log_mock = MagicMock()
    log = {'text': ""}
    def write_side_effect(text):
      log['text'] = log['text'] + text

    host_log_mock.write.side_effect = write_side_effect
    scp = SCP(params.user, params.sshPort, params.sshkey_file, "dummy-host", "src/file",
              "dst/file", params.bootdir, host_log_mock)
    log_sample = "log_sample"
    error_sample = "error_sample"
    # Successful run
    process = MagicMock()
    popenMock.return_value = process
    process.communicate.return_value = (log_sample, error_sample)
    process.returncode = 0

    retcode = scp.run()

    self.assertTrue(popenMock.called)
    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    command_str = str(popenMock.call_args[0][0])
    self.assertEquals(command_str, "['scp', '-r', '-o', 'ConnectTimeout=60', '-o', "
        "'BatchMode=yes', '-o', 'StrictHostKeyChecking=no', '-P', '123', '-i', 'sshkey_file',"
        " 'src/file', 'root@dummy-host:dst/file']")
    self.assertEqual(retcode["exitstatus"], 0)

    log['text'] = ""
    #unsuccessfull run
    process.returncode = 1

    retcode = scp.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertEqual(retcode["exitstatus"], 1)


  @patch.object(subprocess32, "Popen")
  def test_SSH(self, popenMock):
    params = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                                  "setupAgentFile", "ambariServer", "centos6",
                                  "1.2.1", "8440", "root")
    host_log_mock = MagicMock()
    log = {'text': ""}
    def write_side_effect(text):
      log['text'] = log['text'] + text

    host_log_mock.write.side_effect = write_side_effect
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, "dummy-host", "dummy-command",
              params.bootdir, host_log_mock)
    log_sample = "log_sample"
    error_sample = "error_sample"
    # Successful run
    process = MagicMock()
    popenMock.return_value = process
    process.communicate.return_value = (log_sample, error_sample)
    process.returncode = 0

    retcode = ssh.run()

    self.assertTrue(popenMock.called)
    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    command_str = str(popenMock.call_args[0][0])
    self.assertEquals(command_str, "['ssh', '-o', 'ConnectTimeOut=60', '-o', "
            "'StrictHostKeyChecking=no', '-o', 'BatchMode=yes', '-tt', '-i', "
            "'sshkey_file', '-p', '123', 'root@dummy-host', 'dummy-command']")
    self.assertEqual(retcode["exitstatus"], 0)

    log['text'] = ""
    #unsuccessfull run
    process.returncode = 1

    retcode = ssh.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertEqual(retcode["exitstatus"], 1)

    log['text'] = ""
    # unsuccessful run with error message
    process.returncode = 1

    dummy_error_message = "dummy_error_message"
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, "dummy-host", "dummy-command",
              params.bootdir, host_log_mock, errorMessage= dummy_error_message)
    retcode = ssh.run()

    self.assertTrue(log_sample in log['text'])
    self.assertTrue(error_sample in log['text'])
    self.assertTrue(dummy_error_message in log['text'])
    self.assertEqual(retcode["exitstatus"], 1)


  def test_getOsCheckScript(self):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    ocs = bootstrap_obj.getOsCheckScript()
    self.assertEquals(ocs, "scriptDir/os_check_type.py")


  @patch.object(BootstrapDefault, "getRemoteName")
  def test_getOsCheckScriptRemoteLocation(self, getRemoteName_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    v = "/tmp/os_check_type1374259902.py"
    getRemoteName_mock.return_value = v
    ocs = bootstrap_obj.getOsCheckScriptRemoteLocation()
    self.assertEquals(ocs, v)


  @patch.object(BootstrapDefault, "is_suse")
  def test_getRepoFile(self, is_suse_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    is_suse_mock.return_value = False
    rf = bootstrap_obj.getRepoFile()
    self.assertEquals(rf, "/etc/yum.repos.d/ambari.repo")


  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_createTargetDir(self, write_mock, run_mock,
                            init_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.createTargetDir()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command,
                     "SUDO=$([ \"$EUID\" -eq 0 ] && echo || echo sudo) ; $SUDO mkdir -p /var/lib/ambari-agent/tmp ; "
                     "$SUDO chown -R root /var/lib/ambari-agent/tmp ; "
                     "$SUDO chmod 755 /var/lib/ambari-agent ; "
                     "$SUDO chmod 755 /var/lib/ambari-agent/data ; "
                     "$SUDO chmod 1777 /var/lib/ambari-agent/tmp")

  @patch.object(BootstrapDefault, "getOsCheckScript")
  @patch.object(BootstrapDefault, "getOsCheckScriptRemoteLocation")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(HostLog, "write")
  def test_copyOsCheckScript(self, write_mock, run_mock, init_mock,
                    getOsCheckScriptRemoteLocation_mock, getOsCheckScript_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getOsCheckScript_mock.return_value = "OsCheckScript"
    getOsCheckScriptRemoteLocation_mock.return_value = "OsCheckScriptRemoteLocation"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.copyOsCheckScript()
    self.assertEquals(res, expected)
    input_file = str(init_mock.call_args[0][4])
    remote_file = str(init_mock.call_args[0][5])
    self.assertEqual(input_file, "OsCheckScript")
    self.assertEqual(remote_file, "OsCheckScriptRemoteLocation")


  @patch.object(BootstrapDefault, "getRemoteName")
  @patch.object(BootstrapDefault, "hasPassword")
  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  def test_getRepoFile(self, is_redhat_family, is_ubuntu_family, is_suse_family, hasPassword_mock, getRemoteName_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Without password
    hasPassword_mock.return_value = False
    getRemoteName_mock.return_value = "RemoteName"
    rf = bootstrap_obj.getMoveRepoFileCommand("target")
    self.assertEquals(rf, "/var/lib/ambari-agent/tmp/ambari-sudo.sh mv RemoteName target/ambari.repo")
    # With password
    hasPassword_mock.return_value = True
    getRemoteName_mock.return_value = "RemoteName"
    rf = bootstrap_obj.getMoveRepoFileCommand("target")
    self.assertEquals(rf, "/var/lib/ambari-agent/tmp/ambari-sudo.sh -S mv RemoteName target/ambari.repo < RemoteName")

  @patch("os.path.exists")
  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(BootstrapDefault, "getMoveRepoFileCommand")
  @patch.object(BootstrapDefault, "getRepoDir")
  @patch.object(BootstrapDefault, "getRepoFile")
  @patch.object(BootstrapDefault, "getRemoteName")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_copyNeededFiles(self, write_mock, ssh_run_mock, ssh_init_mock,
                           scp_run_mock, scp_init_mock,
                           getRemoteName_mock, getRepoFile_mock, getRepoDir,
                           getMoveRepoFileCommand, is_redhat_family, is_ubuntu_family, is_suse_family,
                           os_path_exists_mock):
    #
    # Ambari repo file exists
    #
    def os_path_exists_side_effect(*args, **kwargs):
      if args[0] == getRepoFile_mock():
        return True
      else:
        return False

    os_path_exists_mock.side_effect = os_path_exists_side_effect
    os_path_exists_mock.return_value = None

    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getMoveRepoFileCommand.return_value = "MoveRepoFileCommand"
    getRepoDir.return_value  = "RepoDir"
    getRemoteName_mock.return_value = "RemoteName"
    getRepoFile_mock.return_value = "RepoFile"
    expected1 = {"exitstatus": 42, "log": "log42", "errormsg": "errorMsg"}
    expected2 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    expected3 = {"exitstatus": 1, "log": "log1", "errormsg": "errorMsg"}
    expected4 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    scp_init_mock.return_value = None
    ssh_init_mock.return_value = None
    # Testing max retcode return
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2, expected4]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected1["exitstatus"])
    input_file = str(scp_init_mock.call_args[0][4])
    remote_file = str(scp_init_mock.call_args[0][5])
    self.assertEqual(input_file, "setupAgentFile")
    self.assertEqual(remote_file, "RemoteName")
    command = str(ssh_init_mock.call_args[0][4])
    self.assertEqual(command, "/var/lib/ambari-agent/tmp/ambari-sudo.sh chmod 644 RepoFile")
    # Another order
    expected1 = {"exitstatus": 0, "log": "log0", "errormsg": "errorMsg"}
    expected2 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    expected3 = {"exitstatus": 1, "log": "log1", "errormsg": "errorMsg"}
    expected4 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2, expected4]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected2["exitstatus"])
    # yet another order
    expected1 = {"exitstatus": 33, "log": "log33", "errormsg": "errorMsg"}
    expected2 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    expected3 = {"exitstatus": 42, "log": "log42", "errormsg": "errorMsg"}
    expected4 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    scp_run_mock.side_effect = [expected1, expected3]
    ssh_run_mock.side_effect = [expected2, expected4]
    res = bootstrap_obj.copyNeededFiles()
    self.assertEquals(res, expected3["exitstatus"])

    #
    #Ambari repo file does not exist
    #
    os_path_exists_mock.side_effect = None
    os_path_exists_mock.return_value = False

    #Expectations:
    # SSH will not be called at all
    # SCP will be called once for copying the setup script file
    scp_run_mock.reset_mock()
    ssh_run_mock.reset_mock()
    expectedResult = {"exitstatus": 33, "log": "log33", "errormsg": "errorMsg"}
    scp_run_mock.side_effect = [expectedResult]
    res = bootstrap_obj.copyNeededFiles()
    self.assertFalse(ssh_run_mock.called)
    self.assertEquals(res, expectedResult["exitstatus"])

  @patch.object(BootstrapDefault, "getOsCheckScriptRemoteLocation")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_runOsCheckScript(self, write_mock, run_mock,
                            init_mock, getOsCheckScriptRemoteLocation_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getOsCheckScriptRemoteLocation_mock.return_value = "OsCheckScriptRemoteLocation"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.runOsCheckScript()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command,
                     "chmod a+x OsCheckScriptRemoteLocation && "
                     "env PYTHONPATH=$PYTHONPATH:/var/lib/ambari-agent/tmp OsCheckScriptRemoteLocation centos6")


  @patch.object(SSH, "__init__")
  @patch.object(BootstrapDefault, "getRunSetupCommand")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_runSetupAgent(self, write_mock, run_mock,
                         getRunSetupCommand_mock, init_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getRunSetupCommand_mock.return_value = "RunSetupCommand"
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.runSetupAgent()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command, "RunSetupCommand")


  @patch.object(BootstrapDefault, "hasPassword")
  @patch.object(BootstrapDefault, "getRunSetupWithPasswordCommand")
  @patch.object(BootstrapDefault, "getRunSetupWithoutPasswordCommand")
  def test_getRunSetupCommand(self, getRunSetupWithoutPasswordCommand_mock,
                              getRunSetupWithPasswordCommand_mock,
                              hasPassword_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # With password
    hasPassword_mock.return_value = True
    getRunSetupWithPasswordCommand_mock.return_value = "RunSetupWithPasswordCommand"
    getRunSetupWithoutPasswordCommand_mock.return_value = "RunSetupWithoutPasswordCommand"
    res = bootstrap_obj.getRunSetupCommand("dummy-host")
    self.assertEqual(res, "RunSetupWithPasswordCommand")
    # Without password
    hasPassword_mock.return_value = False
    res = bootstrap_obj.getRunSetupCommand("dummy-host")
    self.assertEqual(res, "RunSetupWithoutPasswordCommand")


  @patch.object(HostLog, "write")
  def test_createDoneFile(self, write_mock):
    tmp_dir = tempfile.gettempdir()
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", tmp_dir,
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    done_file = os.path.join(tmp_dir, "hostname.done")
    expected = 42
    bootstrap_obj.createDoneFile(expected)
    with open(done_file) as df:
      res = df.read()
      self.assertEqual(res, str(expected))
    os.unlink(done_file)

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_checkSudoPackage(self, write_mock, run_mock, init_mock, is_redhat_family, is_ubuntu_family, is_suse_family):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    is_redhat_family.return_value = True
    is_ubuntu_family.return_value = False
    is_suse_family.return_value = False
    res = bootstrap_obj.checkSudoPackage()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command, "[ \"$EUID\" -eq 0 ] || rpm -qa | grep -e '^sudo\-'")

  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_checkSudoPackageUbuntu(self, write_mock, run_mock, init_mock,
                                  is_redhat_family, is_ubuntu_family, is_suse_family):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "ubuntu12",
                               None, "8440", "root")
    is_redhat_family.return_value = False
    is_ubuntu_family.return_value = True
    is_suse_family.return_value = False
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.checkSudoPackage()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command, "[ \"$EUID\" -eq 0 ] || dpkg --get-selections|grep -e '^sudo\s*install'")


  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  @patch.object(BootstrapDefault, "getPasswordFile")
  def test_deletePasswordFile(self, getPasswordFile_mock, write_mock, run_mock,
                              init_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    getPasswordFile_mock.return_value = "PasswordFile"
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.deletePasswordFile()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command, "rm PasswordFile")


  @patch.object(BootstrapDefault, "getPasswordFile")
  @patch.object(SCP, "__init__")
  @patch.object(SCP, "run")
  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  def test_copyPasswordFile(self, write_mock, ssh_run_mock,
                            ssh_init_mock, scp_run_mock,
                            scp_init_mock, getPasswordFile_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root", password_file="PasswordFile")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    getPasswordFile_mock.return_value = "PasswordFile"
     # Testing max retcode return
    expected1 = {"exitstatus": 42, "log": "log42", "errormsg": "errorMsg"}
    expected2 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    scp_init_mock.return_value = None
    scp_run_mock.return_value = expected1
    ssh_init_mock.return_value = None
    ssh_run_mock.return_value = expected2
    res = bootstrap_obj.copyPasswordFile()
    self.assertEquals(res, expected1["exitstatus"])
    input_file = str(scp_init_mock.call_args[0][4])
    remote_file = str(scp_init_mock.call_args[0][4])
    self.assertEqual(input_file, "PasswordFile")
    self.assertEqual(remote_file, "PasswordFile")
    command = str(ssh_init_mock.call_args[0][4])
    self.assertEqual(command, "chmod 600 PasswordFile")
    # Another order
    expected1 = {"exitstatus": 0, "log": "log0", "errormsg": "errorMsg"}
    expected2 = {"exitstatus": 17, "log": "log17", "errormsg": "errorMsg"}
    scp_run_mock.return_value = expected1
    ssh_run_mock.return_value = expected2


  @patch.object(SSH, "__init__")
  @patch.object(SSH, "run")
  @patch.object(HostLog, "write")
  @patch.object(BootstrapDefault, "getPasswordFile")
  def test_changePasswordFileModeOnHost(self, getPasswordFile_mock, write_mock,
                                        run_mock, init_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    expected = 42
    getPasswordFile_mock.return_value = "PasswordFile"
    init_mock.return_value = None
    run_mock.return_value = expected
    res = bootstrap_obj.changePasswordFileModeOnHost()
    self.assertEquals(res, expected)
    command = str(init_mock.call_args[0][4])
    self.assertEqual(command, "chmod 600 PasswordFile")


  @patch.object(HostLog, "write")
  def test_try_to_execute(self, write_mock):
    expected = 43
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Normal case
    def act_normal_return_int():
      return 43
    ret = bootstrap_obj.try_to_execute(act_normal_return_int)
    self.assertEqual(ret["exitstatus"], expected)
    self.assertFalse(write_mock.called)
    write_mock.reset_mock()
    def act_normal_return():
        return {"exitstatus": 43}
    ret = bootstrap_obj.try_to_execute(act_normal_return)
    self.assertEqual(ret["exitstatus"], expected)
    self.assertFalse(write_mock.called)
    write_mock.reset_mock()
    # Exception scenario
    def act():
      raise IOError()
    ret = bootstrap_obj.try_to_execute(act)
    self.assertEqual(ret["exitstatus"], 177)
    self.assertTrue(write_mock.called)


  @patch.object(BootstrapDefault, "try_to_execute")
  @patch.object(BootstrapDefault, "hasPassword")
  @patch.object(BootstrapDefault, "createDoneFile")
  @patch.object(HostLog, "write")
  @patch("logging.warn")
  @patch("logging.error")
  def test_run(self, error_mock, warn_mock, write_mock, createDoneFile_mock,
               hasPassword_mock, try_to_execute_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    # Testing workflow without password
    bootstrap_obj.copied_password_file = False
    hasPassword_mock.return_value = False
    try_to_execute_mock.return_value = {"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 10) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 0)

    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow with password
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.return_value = {"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 13) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 0)

    error_mock.reset_mock()
    write_mock.reset_mock()
    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed before copying password
    bootstrap_obj.copied_password_file = False
    hasPassword_mock.return_value = False
    try_to_execute_mock.side_effect = [{"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}, {"exitstatus": 1, "log":"log1", "errormsg":"errormsg1"}]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 2) # <- Adjust if changed
    self.assertTrue("ERROR" in error_mock.call_args[0][0])
    self.assertTrue("ERROR" in write_mock.call_args[0][0])
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 1)

    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed after copying password
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.side_effect = [{"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}, {"exitstatus": 42, "log":"log42", "errormsg":"errormsg42"}, {"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 3) # <- Adjust if changed
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 42)

    error_mock.reset_mock()
    write_mock.reset_mock()
    try_to_execute_mock.reset_mock()
    createDoneFile_mock.reset_mock()
    # Testing workflow when some action failed after copying password and
    # removing password failed too
    bootstrap_obj.copied_password_file = True
    hasPassword_mock.return_value = True
    try_to_execute_mock.side_effect = [{"exitstatus": 0, "log":"log0", "errormsg":"errormsg0"}, {"exitstatus": 17, "log":"log17", "errormsg":"errormsg17"}, {"exitstatus": 19, "log":"log19", "errormsg":"errormsg19"}]
    bootstrap_obj.run()
    self.assertEqual(try_to_execute_mock.call_count, 3) # <- Adjust if changed
    self.assertTrue("ERROR" in write_mock.call_args_list[0][0][0])
    self.assertTrue("ERROR" in error_mock.call_args[0][0])
    self.assertTrue("WARNING" in write_mock.call_args_list[1][0][0])
    self.assertTrue("WARNING" in warn_mock.call_args[0][0])
    self.assertTrue(createDoneFile_mock.called)
    self.assertEqual(bootstrap_obj.getStatus()["return_code"], 17)


  @patch.object(BootstrapDefault, "createDoneFile")
  @patch.object(HostLog, "write")
  def test_interruptBootstrap(self, write_mock, createDoneFile_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    bootstrap_obj = Bootstrap("hostname", shared_state)
    bootstrap_obj.interruptBootstrap()
    self.assertTrue(createDoneFile_mock.called)


  @patch("time.sleep")
  @patch("time.time")
  @patch("logging.warn")
  @patch("logging.info")
  @patch.object(BootstrapDefault, "start")
  @patch.object(BootstrapDefault, "interruptBootstrap")
  @patch.object(BootstrapDefault, "getStatus")
  def test_PBootstrap(self, getStatus_mock, interruptBootstrap_mock, start_mock,
                      info_mock, warn_mock, time_mock, sleep_mock):
    shared_state = SharedState("root", "123", "sshkey_file", "scriptDir", "bootdir",
                               "setupAgentFile", "ambariServer", "centos6",
                               None, "8440", "root")
    n = 180
    time = 100500
    time_mock.return_value = time
    hosts = []
    for i in range(0, n):
      hosts.append("host" + str(i))
    # Testing normal case
    getStatus_mock.return_value = {"return_code": 0,
                                   "start_time": time + 999}
    pbootstrap_obj = PBootstrap(hosts, shared_state)
    pbootstrap_obj.run()
    self.assertEqual(start_mock.call_count, n)
    self.assertEqual(interruptBootstrap_mock.call_count, 0)

    start_mock.reset_mock()
    getStatus_mock.reset_mock()
    # Testing case of timeout
    def fake_return_code_generator():
      call_number = 0
      while True:
        call_number += 1
        if call_number % 5 != 0:   # ~80% of hosts finish successfully
          yield 0
        else:
          yield None

    def fake_start_time_generator():
      while True:
        yield time - bootstrap.HOST_BOOTSTRAP_TIMEOUT - 1

    return_code_generator = fake_return_code_generator()
    start_time_generator = fake_start_time_generator()

    def status_get_item_mock(item):
      if item == "return_code":
        return return_code_generator.next()
      elif item == "start_time":
        return start_time_generator.next()

    dict_mock = MagicMock()
    dict_mock.__getitem__.side_effect = status_get_item_mock
    getStatus_mock.return_value = dict_mock

    pbootstrap_obj.run()
    self.assertEqual(start_mock.call_count, n)
    self.assertEqual(interruptBootstrap_mock.call_count, n / 5)

