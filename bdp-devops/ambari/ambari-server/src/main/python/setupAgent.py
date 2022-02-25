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

import socket
import time
import sys
import logging
import os
from ambari_commons import subprocess32

from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_utils import get_ambari_repo_file_full_name

if OSCheck.is_windows_family():
  import urllib2

  from ambari_commons.exceptions import FatalException
  from ambari_commons.os_utils import run_os_command


AMBARI_PASSPHRASE_VAR = "AMBARI_PASSPHRASE"
PROJECT_VERSION_DEFAULT = "DEFAULT"

INSTALL_DRIVE =  os.path.splitdrive(__file__.replace('/', os.sep))[0]
AMBARI_INSTALL_ROOT = os.path.join(INSTALL_DRIVE, os.sep, "ambari")
AMBARI_AGENT_INSTALL_SYMLINK = os.path.join(AMBARI_INSTALL_ROOT, "ambari-agent")

def _ret_init(ret):
  if not ret:
    ret = {'exitstatus': 0, 'log': ('', '')}
  return ret

def _ret_append_stdout(ret, stdout):
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stdout:
    if temp_stdout:
      temp_stdout += os.linesep
    temp_stdout += stdout
  ret['log'] = (temp_stdout, temp_stderr)

def _ret_append_stderr(ret, stderr):
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stderr:
    if temp_stderr:
      temp_stderr += os.linesep
    temp_stderr += stderr
  ret['log'] = (temp_stdout, temp_stderr)

def _ret_merge(ret, retcode, stdout, stderr):
  ret['exitstatus'] = retcode
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stdout:
    if temp_stdout:
      temp_stdout += os.linesep
    temp_stdout += stdout
  if stderr:
    if temp_stderr:
      temp_stderr += os.linesep
    temp_stderr += stderr
  ret['log'] = (temp_stdout, temp_stderr)
  return ret

def _ret_merge2(ret, ret2):
  return _ret_merge(ret, ret2['exitstatus'], ret['log'][0], ret['log'][1])


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def execOsCommand(osCommand, tries=1, try_sleep=0, ret=None, cwd=None):
  ret = _ret_init(ret)

  for i in range(0, tries):
    if i > 0:
      time.sleep(try_sleep)
      _ret_append_stderr(ret, "Retrying " + str(osCommand))

    retcode, stdout, stderr = run_os_command(osCommand, cwd=cwd)
    _ret_merge(ret, retcode, stdout, stderr)
    if retcode == 0:
      break

  return ret

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def execOsCommand(osCommand, tries=1, try_sleep=0, ret=None, cwd=None):
  ret = _ret_init(ret)

  for i in range(0, tries):
    if i>0:
      time.sleep(try_sleep)

    osStat = subprocess32.Popen(osCommand, stdout=subprocess32.PIPE, cwd=cwd)
    log = osStat.communicate(0)
    ret = {"exitstatus": osStat.returncode, "log": log}

    if ret['exitstatus'] == 0:
      break

  return ret

def installAgent(projectVersion, ret=None):
  """ Run install and make sure the agent install alright """
  # The command doesn't work with file mask ambari-agent*.rpm, so rename it on agent host
  if OSCheck.is_suse_family():
    Command = ["zypper", "--no-gpg-checks", "install", "-y", "ambari-agent-" + projectVersion]
  elif OSCheck.is_ubuntu_family():
    # add * to end of version in case of some test releases
    Command = ["apt-get", "install", "-y", "--allow-unauthenticated", "ambari-agent=" + projectVersion + "*"]
  elif OSCheck.is_windows_family():
    packageParams = "/AmbariRoot:" + AMBARI_INSTALL_ROOT
    Command = ["cmd", "/c", "choco", "install", "-y", "ambari-agent", "--version=" + projectVersion, "--params=\"" + packageParams + "\""]
  else:
    Command = ["yum", "-y", "install", "--nogpgcheck", "ambari-agent-" + projectVersion]
  return execOsCommand(Command, tries=3, try_sleep=10, ret=ret)


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def configureAgent(server_hostname, user_run_as, ret=None):
  #Customize ambari-agent.ini & register the Ambari Agent service
  agentSetupCmd = ["cmd", "/c", "ambari-agent.cmd", "setup", "--hostname=" + server_hostname]
  return execOsCommand(agentSetupCmd, tries=3, try_sleep=10, cwd=AMBARI_AGENT_INSTALL_SYMLINK, ret=ret)

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def configureAgent(server_hostname, user_run_as, ret=None):
  """ Configure the agent so that it has all the configs knobs properly installed """
  osCommand = ["sed", "-i.bak", "s/hostname=localhost/hostname=" + server_hostname +
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  ret = execOsCommand(osCommand, ret=ret)
  if ret['exitstatus'] != 0:
    return ret
  osCommand = ["sed", "-i.bak", "s/run_as_user=.*$/run_as_user=" + user_run_as +
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  ret = execOsCommand(osCommand, ret=ret)
  return ret

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def runAgent(passPhrase, expected_hostname, user_run_as, verbose, ret=None):
  ret = _ret_init(ret)

  #Invoke ambari-agent restart as a child process
  agentRestartCmd = ["cmd", "/c", "ambari-agent.cmd", "restart"]
  return execOsCommand(agentRestartCmd, tries=3, try_sleep=10, cwd=AMBARI_AGENT_INSTALL_SYMLINK, ret=ret)

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def runAgent(passPhrase, expected_hostname, user_run_as, verbose, ret=None):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  vo = ""
  if verbose:
    vo = " -v"
  cmd = ['su', user_run_as, '-l', '-c', '/usr/sbin/ambari-agent restart --expected-hostname=%1s %2s' % (expected_hostname, vo)]
  log = ""
  p = subprocess32.Popen(cmd, stdout=subprocess32.PIPE)
  p.communicate()
  agent_retcode = p.returncode
  for i in range(3):
    time.sleep(1)
    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"], ret=ret)
    if (0 == ret['exitstatus']):
      try:
        log = ret['log']
      except Exception:
        log = "Log not found"
      print log
      break
  return {"exitstatus": agent_retcode, "log": log}

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def checkVerbose():
  verbose = False
  if os.path.exists(AMBARI_AGENT_INSTALL_SYMLINK):
    agentStatusCmd = ["cmd", "/c", "ambari-agent.cmd", "status"]
    ret = execOsCommand(agentStatusCmd, tries=3, try_sleep=10, cwd=AMBARI_AGENT_INSTALL_SYMLINK)
    if ret["exitstatus"] == 0 and ret["log"][0].find("running") != -1:
      verbose = True
  return verbose

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def checkVerbose():
  verbose = False
  cmds = ["bash", "-c", "ps aux | grep 'AmbariAgent.py' | grep ' \-v'"]
  cmdl = ["bash", "-c", "ps aux | grep 'AmbariAgent.py' | grep ' \--verbose'"]
  if execOsCommand(cmds)["exitstatus"] == 0 or execOsCommand(cmdl)["exitstatus"] == 0:
    verbose = True
  return verbose

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def getOptimalVersion(initialProjectVersion):
  optimalVersion = initialProjectVersion
  ret = findNearestAgentPackageVersion(optimalVersion)
  if ret["exitstatus"] == 0 and ret["log"][0].strip() != "" and initialProjectVersion \
     and ret["log"][0].strip().startswith(initialProjectVersion):
    optimalVersion = ret["log"][0].strip()
    retcode = 0
  else:
    ret = getAvailableAgentPackageVersions()
    retcode = 1
    optimalVersion = ret["log"]

  return {"exitstatus": retcode, "log": optimalVersion}

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def getOptimalVersion(initialProjectVersion):
  optimalVersion = initialProjectVersion
  ret = findNearestAgentPackageVersion(optimalVersion)
  if ret["exitstatus"] == 0 and ret["log"][0].strip() != "" \
     and ret["log"][0].strip() == initialProjectVersion:
    optimalVersion = ret["log"][0].strip()
    retcode = 0
  else:
    ret = getAvailableAgentPackageVersions()
    retcode = 1
    optimalVersion = ret["log"]

  return {"exitstatus": retcode, "log": optimalVersion}


def findNearestAgentPackageVersion(projectVersion):
  if projectVersion == "":
    projectVersion = "  "
  if OSCheck.is_suse_family():
    Command = ["bash", "-c", "zypper --no-gpg-checks --non-interactive -q search -s --match-exact ambari-agent | grep '" + projectVersion +
                                 "' | cut -d '|' -f 4 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  elif OSCheck.is_windows_family():
    listPackagesCommand = ["cmd", "/c", "choco list ambari-agent --pre --all | findstr " + projectVersion + " > agentPackages.list"]
    execOsCommand(listPackagesCommand)
    Command = ["cmd", "/c", "powershell",  "get-content agentPackages.list | select-object -last 1 | foreach-object {$_ -replace 'ambari-agent ', ''}"]
  elif OSCheck.is_ubuntu_family():
    if projectVersion == "  ":
      Command = ["bash", "-c", "apt-cache -q show ambari-agent |grep 'Version\:'|cut -d ' ' -f 2|tr -d '\\n'|sed -s 's/[-|~][A-Za-z0-9]*//'"]
    else:
      Command = ["bash", "-c", "apt-cache -q show ambari-agent |grep 'Version\:'|cut -d ' ' -f 2|grep '" +
               projectVersion + "'|tr -d '\\n'|sed -s 's/[-|~][A-Za-z0-9]*//'"]
  else:
    Command = ["bash", "-c", "yum -q list all ambari-agent | grep '" + projectVersion +
                              "' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  return execOsCommand(Command)

def isAgentPackageAlreadyInstalled(projectVersion):
    if OSCheck.is_ubuntu_family():
      Command = ["bash", "-c", "dpkg-query -W -f='${Status} ${Version}\n' ambari-agent | grep -v deinstall | grep " + projectVersion]
    elif OSCheck.is_windows_family():
      Command = ["cmd", "/c", "choco list ambari-agent --local-only | findstr ambari-agent | findstr " + projectVersion]
    else:
      Command = ["bash", "-c", "rpm -qa | grep ambari-agent-"+projectVersion]
    ret = execOsCommand(Command)
    res = False
    if ret["exitstatus"] == 0 and ret["log"][0].strip() != "":
        res = True
    return res

def getAvailableAgentPackageVersions():
  if OSCheck.is_suse_family():
    Command = ["bash", "-c",
        "zypper --no-gpg-checks --non-interactive -q search -s --match-exact ambari-agent | grep ambari-agent | sed -re 's/\s+/ /g' | cut -d '|' -f 4 | tr '\\n' ', ' | sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  elif OSCheck.is_windows_family():
    Command = ["cmd", "/c", "choco list ambari-agent --pre --all | findstr ambari-agent"]
  elif OSCheck.is_ubuntu_family():
    Command = ["bash", "-c",
        "apt-cache -q show ambari-agent|grep 'Version\:'|cut -d ' ' -f 2| tr '\\n' ', '|sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  else:
    Command = ["bash", "-c",
        "yum -q list all ambari-agent | grep -E '^ambari-agent' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | tr '\\n' ', ' | sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  return execOsCommand(Command)

def checkServerReachability(host, port):
  ret = {}
  s = socket.socket()
  try:
    s.connect((host, port))
    ret = {"exitstatus": 0, "log": ""}
  except Exception:
    ret["exitstatus"] = 1
    ret["log"] = "Host registration aborted. Ambari Agent host cannot reach Ambari Server '" +\
                host+":"+str(port) + "'. " +\
                "Please check the network connectivity between the Ambari Agent host and the Ambari Server"
  return ret

# Command line syntax help
# IsOptional  Index     Description
#               0        Expected host name
#               1        Password
#               2        Host name
#               3        User to run agent as
#      X        4        Project Version (Ambari)
#      X        5        Server port
def parseArguments(argv=None):
  if argv is None:  # make sure that arguments was passed
    return {"exitstatus": 2, "log": "No arguments were passed"}
  args = argv[1:]  # shift path to script
  if len(args) < 3:
    return {"exitstatus": 1, "log": "Not all required arguments were passed"}

  expected_hostname = args[0]
  passPhrase = args[1]
  hostname = args[2]
  user_run_as = args[3]
  projectVersion = ""
  server_port = 8080

  if len(args) > 4:
    projectVersion = args[4]

  if len(args) > 5:
    try:
      server_port = int(args[5])
    except (Exception):
      server_port = 8080

  parsed_args = (expected_hostname, passPhrase, hostname, user_run_as, projectVersion, server_port)
  return {"exitstatus": 0, "log": "", "parsed_args": parsed_args}

def run_setup(argv=None):
  # Parse passed arguments
  retcode = parseArguments(argv)
  if (retcode["exitstatus"] != 0):
    return retcode

  (expected_hostname, passPhrase, hostname, user_run_as, projectVersion, server_port) = retcode["parsed_args"]

  retcode = checkServerReachability(hostname, server_port)
  if (retcode["exitstatus"] != 0):
    return retcode

  if projectVersion == "null" or projectVersion == "{ambariVersion}" or projectVersion == "":
    retcode = getOptimalVersion("")
  else:
    retcode = getOptimalVersion(projectVersion)

  if retcode["exitstatus"] == 0 and retcode["log"] != None and retcode["log"] != "" and retcode["log"][0].strip() != "":
    availableProjectVersion = retcode["log"].strip()
    if not isAgentPackageAlreadyInstalled(availableProjectVersion):
      # Verify that the ambari repo file is available before trying to install ambari-agent
      ambari_repo_file = get_ambari_repo_file_full_name()
      if os.path.exists(ambari_repo_file):
        retcode = installAgent(availableProjectVersion)
        if (not retcode["exitstatus"] == 0):
          return retcode
      else:
        return {"exitstatus": 2, "log": "Ambari repo file not found: {0}".format(ambari_repo_file)}
        pass
  elif retcode["exitstatus"] == 1:
    if retcode["log"] != None and retcode["log"] != "" and retcode["log"][0].strip() != "":
      return {"exitstatus": 1, "log": "Desired version ("+projectVersion+") of ambari-agent package"
                                        " is not available."
                                        " Repository has following "
                                        "versions of ambari-agent:"+retcode["log"][0].strip()}
    else:
      # We are here because ambari-agent is not installed and version cannot be obtained from the repo file
      logmessage = "Desired version ("+projectVersion+") of ambari-agent package is not available."
      ambari_repo_file = get_ambari_repo_file_full_name()
      if not os.path.exists(ambari_repo_file):
        logmessage = logmessage + " " + "Ambari repo file not found: {0}".format(ambari_repo_file)
      return {"exitstatus": retcode["exitstatus"], "log": logmessage}
      pass
  else:
    return retcode

  retcode = configureAgent(hostname, user_run_as)
  if retcode['exitstatus'] != 0:
    return retcode
  return runAgent(passPhrase, expected_hostname, user_run_as, verbose)

def main(argv=None):
  #Check --verbose option if agent already running
  global verbose
  verbose = checkVerbose()
  if verbose:
    exitcode = run_setup(argv)
  else:
    try:
      exitcode = run_setup(argv)
    except Exception, e:
      exitcode = {"exitstatus": -1, "log": str(e)}
  return exitcode

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  ret = main(sys.argv)
  retcode = ret["exitstatus"]
  print ret["log"]
  sys.exit(retcode)
