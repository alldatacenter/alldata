#!/usr/bin/env ambari-python-wrap

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

import sys
sys.path.append("/usr/lib/ambari-server/lib/") # this file can be run with python2.7 that why we need this

# On Linux, the bootstrap process is supposed to run on hosts that may have installed Python 2.4 and above (CentOS 5).
# Hence, the whole bootstrap code needs to comply with Python 2.4 instead of Python 2.6. Most notably, @-decorators and
# {}-format() are to be avoided.

import time
import logging
import pprint
import os
from ambari_commons import subprocess32
import threading
import traceback
import re
from datetime import datetime
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

if OSCheck.is_windows_family():
  from ambari_commons.os_utils import run_os_command, run_in_shell
else:
  from resource_management.core.shell import quote_bash_args

AMBARI_PASSPHRASE_VAR_NAME = "AMBARI_PASSPHRASE"
HOST_BOOTSTRAP_TIMEOUT = 300
# how many parallel bootstraps may be run at a time
MAX_PARALLEL_BOOTSTRAPS = 20
# How many seconds to wait between polling parallel bootstraps
POLL_INTERVAL_SEC = 1
DEBUG = False
DEFAULT_AGENT_TEMP_FOLDER = "/var/lib/ambari-agent/tmp"
DEFAULT_AGENT_DATA_FOLDER = "/var/lib/ambari-agent/data"
DEFAULT_AGENT_LIB_FOLDER = "/var/lib/ambari-agent"
PYTHON_ENV="env PYTHONPATH=$PYTHONPATH:" + DEFAULT_AGENT_TEMP_FOLDER
SERVER_AMBARI_SUDO = os.getenv('ROOT','/').rstrip('/') + "/var/lib/ambari-server/ambari-sudo.sh"
CREATE_PYTHON_WRAP_SCRIPT = os.getenv('ROOT','/').rstrip('/') + "/var/lib/ambari-server/create-python-wrap.sh"
REMOTE_CREATE_PYTHON_WRAP_SCRIPT = os.path.join(DEFAULT_AGENT_TEMP_FOLDER, 'create-python-wrap.sh')
AMBARI_SUDO = os.path.join(DEFAULT_AGENT_TEMP_FOLDER, 'ambari-sudo.sh')

class HostLog:
  """ Provides per-host logging. """

  def __init__(self, log_file):
    self.log_file = log_file

  def write(self, log_text):
    """
     Writes log to file. Closes file after each write to make content accessible
     for poller in ambari-server
    """
    logFile = open(self.log_file, "a+")
    text = str(log_text)
    if not text.endswith("\n"):
      text += "\n"
    logFile.write(text)
    logFile.close()


class SCP:
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, user, sshPort, sshkey_file, host, inputFile, remote, bootdir, host_log):
    self.user = user
    self.sshPort = sshPort
    self.sshkey_file = sshkey_file
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.bootdir = bootdir
    self.host_log = host_log
    pass


  def run(self):
    scpcommand = ["scp",
                  "-r",
                  "-o", "ConnectTimeout=60",
                  "-o", "BatchMode=yes",
                  "-o", "StrictHostKeyChecking=no", "-P", self.sshPort,
                  "-i", self.sshkey_file, self.inputFile, self.user + "@" +
                                                         self.host + ":" + self.remote]
    if DEBUG:
      self.host_log.write("Running scp command " + ' '.join(scpcommand))
    self.host_log.write("==========================")
    self.host_log.write("\nCommand start time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    scpstat = subprocess32.Popen(scpcommand, stdout=subprocess32.PIPE,
                               stderr=subprocess32.PIPE)
    log = scpstat.communicate()
    errorMsg = log[1]
    log = log[0] + "\n" + log[1]
    self.host_log.write(log)
    self.host_log.write("scp " + self.inputFile)
    self.host_log.write("host=" + self.host + ", exitcode=" + str(scpstat.returncode) )
    self.host_log.write("Command end time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    return {"exitstatus": scpstat.returncode, "log": log, "errormsg": errorMsg}


class SSH:
  """ Ssh implementation of this """
  def __init__(self, user, sshPort, sshkey_file, host, command, bootdir, host_log, errorMessage = None):
    self.user = user
    self.sshPort = sshPort
    self.sshkey_file = sshkey_file
    self.host = host
    self.command = command
    self.bootdir = bootdir
    self.errorMessage = errorMessage
    self.host_log = host_log
    pass


  def run(self):
    sshcommand = ["ssh",
                  "-o", "ConnectTimeOut=60",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "BatchMode=yes",
                  "-tt", # Should prevent "tput: No value for $TERM and no -T specified" warning
                  "-i", self.sshkey_file, "-p", self.sshPort,
                  self.user + "@" + self.host, self.command]
    if DEBUG:
      self.host_log.write("Running ssh command " + ' '.join(sshcommand))
    self.host_log.write("==========================")
    self.host_log.write("\nCommand start time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    sshstat = subprocess32.Popen(sshcommand, stdout=subprocess32.PIPE,
                               stderr=subprocess32.PIPE)
    log = sshstat.communicate()
    errorMsg = log[1]
    if self.errorMessage and sshstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg
    log = log[0] + "\n" + errorMsg
    self.host_log.write(log)
    self.host_log.write("SSH command execution finished")
    self.host_log.write("host=" + self.host + ", exitcode=" + str(sshstat.returncode))
    self.host_log.write("Command end time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    return  {"exitstatus": sshstat.returncode, "log": log, "errormsg": errorMsg}


class PSR:
  """ PowerShell Remoting implementation of this """
  def __init__(self, command, host, host_log, params=None, errorMessage=None):
    self.command = command
    self.host = host
    self.host_log = host_log
    self.params = params
    self.errorMessage = errorMessage
    pass

  def run(self):
    #os.environ['COMSPEC'] = 'c:\\System32\\WindowsPowerShell\\v1.0\\powershell.exe'
    psrcommand = ["powershell.exe",
                  "-NoProfile",
                  "-InputFormat", "Text",
                  "-ExecutionPolicy", "unrestricted",
                  "-Command", self.command]
    if self.params:
      psrcommand.extend([self.params])
    if DEBUG:
      self.host_log.write("Running PowerShell command " + ' '.join(psrcommand))
    self.host_log.write("==========================")
    self.host_log.write("\nCommand start time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    retcode, stdout, stderr = run_os_command(psrcommand)
    errorMsg = stderr
    if self.errorMessage and retcode != 0:
      errorMsg = self.errorMessage + "\n" + stderr
    log = stdout + "\n" + errorMsg
    self.host_log.write(log)
    self.host_log.write("PowerShell command execution finished")
    self.host_log.write("host=" + self.host + ", exitcode=" + str(retcode))
    self.host_log.write("Command end time " + datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    return {"exitstatus": retcode, "log": log, "errormsg": errorMsg}


class Bootstrap(threading.Thread):
  """ Bootstrap the agent on a separate host"""
  SETUP_SCRIPT_FILENAME = "setupAgent.py"
  AMBARI_REPO_FILENAME = "ambari"

  def __init__(self, host, shared_state):
    threading.Thread.__init__(self)
    self.host = host
    self.shared_state = shared_state
    self.status = {
      "start_time": None,
      "return_code": None,
    }
    log_file = os.path.join(self.shared_state.bootdir, self.host + ".log")
    self.host_log = HostLog(log_file)
    self.daemon = True

    if OSCheck.is_ubuntu_family():
      self.AMBARI_REPO_FILENAME = self.AMBARI_REPO_FILENAME + ".list"
    else:
      self.AMBARI_REPO_FILENAME = self.AMBARI_REPO_FILENAME + ".repo"

  # This method is needed  to implement the descriptor protocol (make object
  # to pass self reference to mockups)
  def __get__(self, obj, objtype):
    def _call(*args, **kwargs):
      self(obj, *args, **kwargs)
    return _call

  def try_to_execute(self, action):
    last_retcode = {"exitstatus": 177, "log":"Try to execute '{0}'".format(str(action)), "errormsg":"Execute of '{0}' failed".format(str(action))}
    try:
      retcode = action()
      if isinstance(retcode, int):
        last_retcode["exitstatus"] = retcode
      else:
        last_retcode = retcode
    except Exception:
      self.host_log.write("Traceback: " + traceback.format_exc())
    return last_retcode

  def getAmbariVersion(self):
    ambari_version = self.shared_state.ambari_version
    if ambari_version is None or ambari_version == "null":
      return ""
    else:
      return ambari_version

  def createDoneFile(self, retcode):
    """ Creates .done file for current host. These files are later read from Java code.
    If .done file for any host is not created, the bootstrap will hang or fail due to timeout"""
    params = self.shared_state
    doneFilePath = os.path.join(params.bootdir, self.host + ".done")
    if not os.path.exists(doneFilePath):
      doneFile = open(doneFilePath, "w+")
      doneFile.write(str(retcode))
      doneFile.close()

  def getStatus(self):
    return self.status

  def interruptBootstrap(self):
    """
    Thread is not really interrupted (moreover, Python seems to have no any
    stable/portable/official api to do that: _Thread__stop only marks thread
    as stopped). The bootstrap thread is marked as a daemon at init, and will
    exit when the main parallel bootstrap thread exits.
    All we need to do now is a proper logging and creating .done file
    """
    self.host_log.write("Automatic Agent registration timed out (timeout = {0} seconds). " \
                        "Check your network connectivity and retry registration," \
                        " or use manual agent registration.".format(HOST_BOOTSTRAP_TIMEOUT))
    self.createDoneFile(199)



@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class BootstrapWindows(Bootstrap):
  CREATE_REMOTING_DIR_SCRIPT_NAME = "Create-RemotingDir.ps1"
  SEND_REMOTING_FILE_SCRIPT_NAME = "Send-RemotingFile.ps1"
  UNZIP_REMOTING_SCRIPT_NAME = "Unzip-RemotingFile.ps1"
  RUN_REMOTING_SCRIPT_NAME = "Run-RemotingScript.ps1"
  CONFIGURE_CHOCOLATEY_SCRIPT_NAME = "Configure-Chocolatey.ps1"

  BOOTSTRAP_ARCHIVE_NAME = "bootstrap.zip"
  CHOCOLATEY_INSTALL_VAR_NAME = "ChocolateyInstall"
  CHOCOLATEY_CONFIG_DIR = "config"
  CHOCOLATEY_CONFIG_FILENAME = "chocolatey.config"
  chocolateyConfigName = "chocolatey.config"

  def getTempFolder(self):
    installationDrive = os.path.splitdrive(self.shared_state.script_dir)[0]
    return os.path.join(installationDrive, os.sep, "var", "temp", "bootstrap", self.getAmbariVersion())

  def createTargetDir(self):
    # Creating target dir
    self.host_log.write("==========================\n")
    self.host_log.write("Creating target directory...")
    command = os.path.join(self.shared_state.script_dir, self.CREATE_REMOTING_DIR_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} {1}".format(self.host, self.getTempFolder()))
    retcode = psr.run()
    self.host_log.write("\n")
    return retcode

  def unzippingBootstrapArchive(self):
    # Unzipping bootstrap archive
    zipFile = os.path.join(self.getTempFolder(), self.BOOTSTRAP_ARCHIVE_NAME)
    self.host_log.write("==========================\n")
    self.host_log.write("Unzipping bootstrap archive...")
    command = os.path.join(self.shared_state.script_dir, self.UNZIP_REMOTING_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} {1} {2}".format(self.host, zipFile, self.getTempFolder()))
    result = psr.run()
    self.host_log.write("\n")
    return result

  def copyBootstrapArchive(self):
    # Copying the bootstrap archive file
    fileToCopy = os.path.join(self.shared_state.script_dir, os.path.dirname(self.shared_state.script_dir), self.shared_state.setup_agent_file)
    target = os.path.join(self.getTempFolder(), self.BOOTSTRAP_ARCHIVE_NAME)
    self.host_log.write("==========================\n")
    self.host_log.write("Copying bootstrap archive...")
    command = os.path.join(self.shared_state.script_dir, self.SEND_REMOTING_FILE_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} {1} {2}".format(self.host, fileToCopy, target))
    result = psr.run()
    self.host_log.write("\n")
    return result

  def copyChocolateyConfig(self):
    # Copying chocolatey.config file
    fileToCopy = getConfigFile()
    target = os.path.join(self.getTempFolder(), self.CHOCOLATEY_CONFIG_FILENAME)
    self.host_log.write("==========================\n")
    self.host_log.write("Copying chocolatey config file...")
    command = os.path.join(self.shared_state.script_dir, self.SEND_REMOTING_FILE_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} {1} {2}".format(self.host, fileToCopy, target))
    result = psr.run()
    self.host_log.write("\n")
    return result

  def configureChocolatey(self):
    self.host_log.write("==========================\n")
    self.host_log.write("Running configure chocolatey script...")
    tmpConfig = os.path.join(self.getTempFolder(), self.CHOCOLATEY_CONFIG_FILENAME)
    command = os.path.join(self.shared_state.script_dir, self.CONFIGURE_CHOCOLATEY_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} {1}".format(self.host, tmpConfig))
    result = psr.run()
    self.host_log.write("\n")
    return result

  def getRunSetupCommand(self, expected_hostname):
    setupFile = os.path.join(self.getTempFolder(), self.SETUP_SCRIPT_FILENAME)
    passphrase = os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    user_run_as = self.shared_state.user_run_as
    server = self.shared_state.ambari_server
    version = self.getAmbariVersion()
    return ' '.join(['python', setupFile, expected_hostname, passphrase, server, user_run_as, version])

  def runSetupAgent(self):
    self.host_log.write("==========================\n")
    self.host_log.write("Running setup agent script...")
    command = os.path.join(self.shared_state.script_dir, self.RUN_REMOTING_SCRIPT_NAME)
    psr = PSR(command, self.host, self.host_log, params="{0} \"{1}\"".format(self.host, self.getRunSetupCommand(self.host)))
    retcode = psr.run()
    self.host_log.write("\n")
    return retcode

  def getConfigFile(self):
    return os.path.join(os.environ[self.CHOCOLATEY_INSTALL_VAR_NAME], self.CHOCOLATEY_CONFIG_DIR, self.CHOCOLATEY_CONFIG_FILENAME)

  def run(self):
    """ Copy files and run commands on remote host """
    self.status["start_time"] = time.time()
    # Population of action queue
    action_queue = [self.createTargetDir,
                    self.copyBootstrapArchive,
                    self.unzippingBootstrapArchive,
                    self.copyChocolateyConfig,
                    self.configureChocolatey,
                    self.runSetupAgent
    ]

    last_retcode = 0

    if os.path.exists(getConfigFile()) :
    # Checking execution result   # Execution of action queue
      while action_queue and last_retcode == 0:
        action = action_queue.pop(0)
        ret = self.try_to_execute(action)
        last_retcode = ret["exitstatus"]
        err_msg = ret["errormsg"]
        std_out = ret["log"]
    else:
      # If config file is not found, then assume that the hosts have
      # already been provisioned. Attempt to run the setupAgent script alone.
      ret = self.try_to_execute(self.runSetupAgent)
      last_retcode = ret["exitstatus"]
      err_msg = ret["errormsg"]
      std_out = ret["log"]
      pass
    if last_retcode != 0:
      message = "ERROR: Bootstrap of host {0} fails because previous action " \
                "finished with non-zero exit code ({1})\nERROR MESSAGE: {2}\nSTDOUT: {3}".format(self.host, last_retcode, err_msg, std_out)
      self.host_log.write(message)
      logging.error(message)

    self.createDoneFile(last_retcode)
    self.status["return_code"] = last_retcode



@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class BootstrapDefault(Bootstrap):
  ambari_commons="/usr/lib/ambari-server/lib/ambari_commons"
  TEMP_FOLDER = DEFAULT_AGENT_TEMP_FOLDER
  OS_CHECK_SCRIPT_FILENAME = "os_check_type.py"
  PASSWORD_FILENAME = "host_pass"

  def getRemoteName(self, filename):
    full_name = os.path.join(self.TEMP_FOLDER, filename)
    remote_files = self.shared_state.remote_files
    if not remote_files.has_key(full_name):
      remote_files[full_name] = self.generateRandomFileName(full_name)
    return remote_files[full_name]

  def generateRandomFileName(self, filename):
    if filename is None:
      return self.getUtime()
    else:
      name, ext = os.path.splitext(filename)
      return str(name) + str(self.getUtime()) + str(ext)
  
  def getRepoDir(self):
    if OSCheck.is_redhat_family():
      return "/etc/yum.repos.d"
    elif OSCheck.is_suse_family():
      return "/etc/zypp/repos.d"
    elif OSCheck.is_ubuntu_family():
      return "/etc/apt/sources.list.d"
    else:
      raise Exception("Unsupported OS family '{0}'".format(OSCheck.get_os_family()))

  def getRepoFile(self):
    """ Ambari repo file for Ambari."""
    return os.path.join(self.getRepoDir(), self.AMBARI_REPO_FILENAME)

  def getOsCheckScript(self):
    return os.path.join(self.shared_state.script_dir, self.OS_CHECK_SCRIPT_FILENAME)

  def getOsCheckScriptRemoteLocation(self):
    return self.getRemoteName(self.OS_CHECK_SCRIPT_FILENAME)

  def getCommonFunctionsRemoteLocation(self):
    return self.TEMP_FOLDER;

  def getUtime(self):
    return int(time.time())

  def getPasswordFile(self):
    return self.getRemoteName(self.PASSWORD_FILENAME)

  def hasPassword(self):
    password_file = self.shared_state.password_file
    return password_file is not None and password_file != 'null'
  
  def copyAmbariSudo(self):
    # Copying the os check script file
    fileToCopy = SERVER_AMBARI_SUDO
    target = self.TEMP_FOLDER
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Copying ambari sudo script...")
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    result = scp.run()
    self.host_log.write("\n")
    return result

  def copyCreatePythonWrapScript(self):
    # Copying the script which will create python wrap
    fileToCopy = CREATE_PYTHON_WRAP_SCRIPT
    target = self.TEMP_FOLDER
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Copying create-python-wrap script...")
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    result = scp.run()
    self.host_log.write("\n")
    return result

  def copyOsCheckScript(self):
    # Copying the os check script file
    fileToCopy = self.getOsCheckScript()
    target = self.getOsCheckScriptRemoteLocation()
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Copying OS type check script...")
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    result = scp.run()
    self.host_log.write("\n")
    return result

  def copyCommonFunctions(self):
    # Copying the os check script file
    fileToCopy = self.ambari_commons
    target = self.getCommonFunctionsRemoteLocation()
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Copying common functions script...")
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    result = scp.run()
    self.host_log.write("\n")
    result["exitstatus"] = 0 # ignore not copied *.pyc files with permission denied. 
    return result

  def getMoveRepoFileWithPasswordCommand(self, targetDir):
    return "{sudo} -S mv ".format(sudo=AMBARI_SUDO) + str(self.getRemoteName(self.AMBARI_REPO_FILENAME)) \
           + " " + os.path.join(str(targetDir), self.AMBARI_REPO_FILENAME) + \
           " < " + str(self.getPasswordFile())

  def getMoveRepoFileWithoutPasswordCommand(self, targetDir):
    return "{sudo} mv ".format(sudo=AMBARI_SUDO) + str(self.getRemoteName(self.AMBARI_REPO_FILENAME)) \
           + " " + os.path.join(str(targetDir), self.AMBARI_REPO_FILENAME)

  def getMoveRepoFileCommand(self, targetDir):
    if self.hasPassword():
      return self.getMoveRepoFileWithPasswordCommand(targetDir)
    else:
      return self.getMoveRepoFileWithoutPasswordCommand(targetDir)

  def getAptUpdateCommand(self):
    return "%s apt-get update -o Dir::Etc::sourcelist=\"%s/%s\" -o API::Get::List-Cleanup=\"0\" --no-list-cleanup" %\
          (AMBARI_SUDO, "sources.list.d", self.AMBARI_REPO_FILENAME)
          
  def getRepoFileChmodCommand(self):
    return "{0} chmod 644 {1}".format(AMBARI_SUDO, self.getRepoFile())

  def copyNeededFiles(self):
    # get the params
    params = self.shared_state

    # Copying the files
    fileToCopy = self.getRepoFile()
    target = self.getRemoteName(self.AMBARI_REPO_FILENAME)

    if (os.path.exists(fileToCopy)):
      self.host_log.write("==========================\n")
      self.host_log.write("Copying repo file to 'tmp' folder...")
      scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
                target, params.bootdir, self.host_log)
      retcode1 = scp.run()
      self.host_log.write("\n")

      # Move file to repo dir
      self.host_log.write("==========================\n")
      self.host_log.write("Moving file to repo dir...")
      targetDir = self.getRepoDir()
      command = self.getMoveRepoFileCommand(targetDir)
      ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
                params.bootdir, self.host_log)
      retcode2 = ssh.run()
      self.host_log.write("\n")

      # Change permissions on ambari.repo
      self.host_log.write("==========================\n")
      self.host_log.write("Changing permissions for ambari.repo...")
      command = self.getRepoFileChmodCommand()
      ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
                params.bootdir, self.host_log)
      retcode4 = ssh.run()
      self.host_log.write("\n")

      # Update repo cache for ubuntu OS
      if OSCheck.is_ubuntu_family():
        self.host_log.write("==========================\n")
        self.host_log.write("Update apt cache of repository...")
        command = self.getAptUpdateCommand()
        ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
                  params.bootdir, self.host_log)
        retcode2 = ssh.run()
        self.host_log.write("\n")

      retcode = max(retcode1["exitstatus"], retcode2["exitstatus"], retcode4["exitstatus"])
    else:
      self.host_log.write("==========================\n")
      self.host_log.write("Copying required files...")
      self.host_log.write("Ambari repo file not found: {0}".format(self.getRepoFile()))
      retcode = -1
      pass

    # copy the setup script file
    self.host_log.write("==========================\n")
    self.host_log.write("Copying setup script file...")
    fileToCopy = params.setup_agent_file
    target = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    retcode3 = scp.run()
    self.host_log.write("\n")

    return max(retcode, retcode3["exitstatus"])

  def getAmbariPort(self):
    server_port = self.shared_state.server_port
    if server_port is None or server_port == "null":
      return "null"
    else:
      return server_port

  def getRunSetupWithPasswordCommand(self, expected_hostname):
    setupFile = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase = os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server = self.shared_state.ambari_server
    user_run_as = self.shared_state.user_run_as
    version = self.getAmbariVersion()
    port = self.getAmbariPort()
    passwordFile = self.getPasswordFile()
    return "{sudo} -S python ".format(sudo=AMBARI_SUDO) + str(setupFile) + " " + str(expected_hostname) + \
           " " + str(passphrase) + " " + str(server)+ " " + quote_bash_args(str(user_run_as)) + " " + str(version) + \
           " " + str(port) + " < " + str(passwordFile)

  def getRunSetupWithoutPasswordCommand(self, expected_hostname):
    setupFile=self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase=os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server=self.shared_state.ambari_server
    user_run_as = self.shared_state.user_run_as
    version=self.getAmbariVersion()
    port=self.getAmbariPort()
    return "{sudo} python ".format(sudo=AMBARI_SUDO) + str(setupFile) + " " + str(expected_hostname) + \
           " " + str(passphrase) + " " + str(server)+ " " + quote_bash_args(str(user_run_as)) + " " + str(version) + \
           " " + str(port)

  def runCreatePythonWrapScript(self):
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Running create-python-wrap script...")

    command = "chmod a+x {script} && {sudo} {script}".format(sudo=AMBARI_SUDO, script=REMOTE_CREATE_PYTHON_WRAP_SCRIPT)

    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("\n")
    return retcode

  def runOsCheckScript(self):
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Running OS type check...")

    command = "chmod a+x %s && %s %s %s" % \
              (self.getOsCheckScriptRemoteLocation(),
               PYTHON_ENV, self.getOsCheckScriptRemoteLocation(), params.cluster_os_type)

    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("\n")
    return retcode

  def checkSudoPackage(self):
    """ Checking 'sudo' package on remote host """
    self.host_log.write("==========================\n")
    self.host_log.write("Checking 'sudo' package on remote host...")
    params = self.shared_state
    if OSCheck.is_ubuntu_family():
      command = "dpkg --get-selections|grep -e '^sudo\s*install'"
    else:
      command = "rpm -qa | grep -e '^sudo\-'"
    command = "[ \"$EUID\" -eq 0 ] || " + command
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log,
              errorMessage="Error: Sudo command is not available. "
                           "Please install the sudo command.")
    retcode = ssh.run()
    self.host_log.write("\n")
    return retcode

  def copyPasswordFile(self):
    # Copy the password file
    self.host_log.write("Copying password file to 'tmp' folder...")
    params = self.shared_state
    scp = SCP(params.user, params.sshPort, params.sshkey_file, self.host, params.password_file,
              self.getPasswordFile(), params.bootdir, self.host_log)
    retcode1 = scp.run()

    self.copied_password_file = True

    # Change password file mode to 600
    self.host_log.write("Changing password file mode...")
    command = "chmod 600 " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode2 = ssh.run()

    self.host_log.write("Copying password file finished")
    return max(retcode1["exitstatus"], retcode2["exitstatus"])

  def changePasswordFileModeOnHost(self):
    # Change password file mode to 600
    self.host_log.write("Changing password file mode...")
    params = self.shared_state
    command = "chmod 600 " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Change password file mode on host finished")
    return retcode

  def deletePasswordFile(self):
    # Deleting the password file
    self.host_log.write("Deleting password file...")
    params = self.shared_state
    command = "rm " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Deleting password file finished")
    return retcode

  def createTargetDir(self):
    # Creating target dir
    self.host_log.write("==========================\n")
    self.host_log.write("Creating target directory...")
    params = self.shared_state
    user = params.user

    command = "SUDO=$([ \"$EUID\" -eq 0 ] && echo || echo sudo) ; $SUDO mkdir -p {0} ; $SUDO chown -R {1} {0} ; $SUDO chmod 755 {3} ; $SUDO chmod 755 {2} ; $SUDO chmod 1777 {0}".format(
      self.TEMP_FOLDER, quote_bash_args(params.user), DEFAULT_AGENT_DATA_FOLDER, DEFAULT_AGENT_LIB_FOLDER)

    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("\n")
    return retcode

  def getRunSetupCommand(self, expected_hostname):
    if self.hasPassword():
      return self.getRunSetupWithPasswordCommand(expected_hostname)
    else:
      return self.getRunSetupWithoutPasswordCommand(expected_hostname)

  def runSetupAgent(self):
    params = self.shared_state
    self.host_log.write("==========================\n")
    self.host_log.write("Running setup agent script...")
    command = self.getRunSetupCommand(self.host)
    ssh = SSH(params.user, params.sshPort, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("\n")
    return retcode

  def run(self):
    """ Copy files and run commands on remote host """
    self.status["start_time"] = time.time()
    # Population of action queue
    action_queue = [self.createTargetDir,
                    self.copyAmbariSudo,
                    self.copyCommonFunctions,
                    self.copyCreatePythonWrapScript,
                    self.copyOsCheckScript,
                    self.runCreatePythonWrapScript,
                    self.runOsCheckScript,
                    self.checkSudoPackage
    ]
    if self.hasPassword():
      action_queue.extend([self.copyPasswordFile,
                           self.changePasswordFileModeOnHost])
    action_queue.extend([
      self.copyNeededFiles,
      self.runSetupAgent,
    ])

    # Execution of action queue
    last_retcode = 0
    while action_queue and last_retcode == 0:
      action = action_queue.pop(0)
      ret = self.try_to_execute(action)
      last_retcode = ret["exitstatus"]
      err_msg = ret["errormsg"]
      std_out = ret["log"]
    # Checking execution result
    if last_retcode != 0:
      message = "ERROR: Bootstrap of host {0} fails because previous action " \
        "finished with non-zero exit code ({1})\nERROR MESSAGE: {2}\nSTDOUT: {3}".format(self.host, last_retcode, err_msg, std_out)
      self.host_log.write(message)
      logging.error(message)
    # Try to delete password file
    if self.hasPassword() and self.copied_password_file:
      retcode = self.try_to_execute(self.deletePasswordFile)
      if retcode["exitstatus"] != 0:
        message = "WARNING: failed to delete password file " \
          "at {0}. Please delete it manually".format(self.getPasswordFile())
        self.host_log.write(message)
        logging.warn(message)

    self.createDoneFile(last_retcode)
    self.status["return_code"] = last_retcode



class PBootstrap:
  """ BootStrapping the agents on a list of hosts"""
  def __init__(self, hosts, sharedState):
    self.hostlist = hosts
    self.sharedState = sharedState
    pass

  def run_bootstrap(self, host):
    bootstrap = Bootstrap(host, self.sharedState)
    bootstrap.start()
    return bootstrap

  def run(self):
    """ Run up to MAX_PARALLEL_BOOTSTRAPS at a time in parallel """
    logging.info("Executing parallel bootstrap")
    queue = list(self.hostlist)
    queue.reverse()
    running_list = []
    finished_list = []
    while queue or running_list: # until queue is not empty or not all parallel bootstraps are
      # poll running bootstraps
      for bootstrap in running_list:
        if bootstrap.getStatus()["return_code"] is not None:
          finished_list.append(bootstrap)
        else:
          starttime = bootstrap.getStatus()["start_time"]
          elapsedtime = time.time() - starttime
          if elapsedtime > HOST_BOOTSTRAP_TIMEOUT:
            # bootstrap timed out
            logging.warn("Bootstrap at host {0} timed out and will be "
                            "interrupted".format(bootstrap.host))
            bootstrap.interruptBootstrap()
            finished_list.append(bootstrap)
      # Remove finished from the running list
      running_list[:] = [b for b in running_list if not b in finished_list]
      # Start new bootstraps from the queue
      free_slots = MAX_PARALLEL_BOOTSTRAPS - len(running_list)
      for i in range(free_slots):
        if queue:
          next_host = queue.pop()
          bootstrap = self.run_bootstrap(next_host)
          running_list.append(bootstrap)
      time.sleep(POLL_INTERVAL_SEC)
    logging.info("Finished parallel bootstrap")


class SharedState:
  def __init__(self, user, sshPort, sshkey_file, script_dir, boottmpdir, setup_agent_file,
               ambari_server, cluster_os_type, ambari_version, server_port,
               user_run_as, password_file = None):
    self.hostlist_to_remove_password_file = None
    self.user = user
    self.sshPort = sshPort
    self.sshkey_file = sshkey_file
    self.bootdir = boottmpdir
    self.script_dir = script_dir
    self.setup_agent_file = setup_agent_file
    self.ambari_server = ambari_server
    self.cluster_os_type = cluster_os_type
    self.ambari_version = ambari_version
    self.user_run_as = user_run_as
    self.password_file = password_file
    self.statuses = None
    self.server_port = server_port
    self.remote_files = {}
    self.ret = {}
    pass


def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  onlyargs = argv[1:]
  if len(onlyargs) < 3:
    sys.stderr.write("Usage: <comma separated hosts> "
                     "<tmpdir for storage> <user> <sshPort> <sshkey_file> <agent setup script>"
                     " <ambari-server name> <cluster os type> <ambari version> <ambari port> <user_run_as> <passwordFile>\n")
    sys.exit(2)
    pass
  

  #Parse the input
  hostList = onlyargs[0].split(",")
  bootdir =  onlyargs[1]
  user = onlyargs[2]
  sshPort = onlyargs[3]
  sshkey_file = onlyargs[4]
  setupAgentFile = onlyargs[5]
  ambariServer = onlyargs[6]
  cluster_os_type = onlyargs[7]
  ambariVersion = onlyargs[8]
  server_port = onlyargs[9]
  user_run_as = onlyargs[10]
  passwordFile = onlyargs[11]

  if not OSCheck.is_windows_family():
    # ssh doesn't like open files
    subprocess32.Popen(["chmod", "600", sshkey_file], stdout=subprocess32.PIPE)

    if passwordFile is not None and passwordFile != 'null':
      subprocess32.Popen(["chmod", "600", passwordFile], stdout=subprocess32.PIPE)

  logging.info("BootStrapping hosts " + pprint.pformat(hostList) +
               " using " + scriptDir + " cluster primary OS: " + cluster_os_type +
               " with user '" + user + "'with ssh Port '" + sshPort + "' sshKey File " + sshkey_file + " password File " + passwordFile +\
               " using tmp dir " + bootdir + " ambari: " + ambariServer +"; server_port: " + server_port +\
               "; ambari version: " + ambariVersion+"; user_run_as: " + user_run_as)
  sharedState = SharedState(user, sshPort, sshkey_file, scriptDir, bootdir, setupAgentFile,
                       ambariServer, cluster_os_type, ambariVersion,
                       server_port, user_run_as, passwordFile)
  pbootstrap = PBootstrap(hostList, sharedState)
  pbootstrap.run()
  return 0 # Hack to comply with current usage

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)
