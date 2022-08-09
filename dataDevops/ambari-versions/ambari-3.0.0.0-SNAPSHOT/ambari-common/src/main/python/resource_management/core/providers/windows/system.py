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

Ambari Agent

"""
from ambari_commons.os_windows import UserHelper

from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management.core import ExecuteTimeoutException
import time
import os
from ambari_commons import subprocess32
import shutil
from resource_management.libraries.script import Script
import win32con
from win32security import *
from win32api import *
from winerror import ERROR_INVALID_HANDLE
from win32profile import CreateEnvironmentBlock
from win32process import GetExitCodeProcess, STARTF_USESTDHANDLES, STARTUPINFO, CreateProcessAsUser
from win32event import WaitForSingleObject, INFINITE
from win32security import *
import msvcrt
import tempfile

def _create_tmp_files(env=None):
  dirname = None
  if env is None:
    env = os.environ

  for env_var_name in 'TMPDIR', 'TEMP', 'TMP':
    if env.has_key(env_var_name):
      dirname = env[env_var_name]
      if dirname and os.path.exists(dirname):
        break

  if dirname is None:
    for dirname2 in r'c:\temp', r'c:\tmp', r'\temp', r'\tmp':
      try:
        os.makedirs(dirname2)
        dirname = dirname2
        break
      except:
        pass

  if dirname is None:
    raise Exception('Unable to create temp dir. Insufficient access rights.')

  out_file = tempfile.TemporaryFile(mode="r+b", dir=dirname)
  err_file = tempfile.TemporaryFile(mode="r+b", dir=dirname)
  return (msvcrt.get_osfhandle(out_file.fileno()),
          msvcrt.get_osfhandle(err_file.fileno()),
          out_file,
          err_file)


def _get_files_output(out, err):
  out.seek(0)
  err.seek(0)
  return out.read().strip(), err.read().strip()


def _safe_duplicate_handle(h):
  try:
    h = DuplicateHandle(GetCurrentProcess(),
                        h,
                        GetCurrentProcess(),
                        0,
                        True,
                        win32con.DUPLICATE_SAME_ACCESS)
    return True, h
  except Exception as exc:
    if exc.winerror == ERROR_INVALID_HANDLE:
      return True, None
  return False, None


def _merge_env(env1, env2, merge_keys=['PYTHONPATH']):
  """
  Merge env2 into env1. Also current python instance variables from merge_keys list taken into account and they will be
  merged with equivalent keys from env1 and env2 using system path separator.
  :param env1: first environment, usually returned by CreateEnvironmentBlock
  :param env2: custom environment
  :param merge_keys: env variables to merge as PATH
  :return: merged environment
  """
  env1 = dict(env1)  # copy to new dict in case env1 is os.environ
  if env2:
    for key, value in env2.iteritems():
      if not key in merge_keys:
        env1[key] = value
  # strnsform keys and values to str(windows can not accept unicode)
  result_env = {}
  for key, value in env1.iteritems():
    if not key in merge_keys:
      result_env[str(key)] = str(value)
  #merge keys from merge_keys
  def put_values(key, env, result):
    if env and key in env:
      result.extend(env[key].split(os.pathsep))

  for key in merge_keys:
    all_values = []
    for env in [env1, env2, os.environ]:
      put_values(key, env, all_values)
    result_env[str(key)] = str(os.pathsep.join(set(all_values)))
  return result_env

def AdjustPrivilege(htoken, priv, enable = 1):
  # Get the ID for the privilege.
  privId = LookupPrivilegeValue(None, priv)
  # Now obtain the privilege for this token.
  # Create a list of the privileges to be added.
  privState = SE_PRIVILEGE_ENABLED if enable else 0
  newPrivileges = [(privId, privState)]
  # and make the adjustment.
  AdjustTokenPrivileges(htoken, 0, newPrivileges)

def QueryPrivilegeState(hToken, priv):
  # Get the ID for the privilege.
  privId = LookupPrivilegeValue(None, priv)
  privList = GetTokenInformation(hToken, TokenPrivileges)
  privState = 0
  for (id, attr) in privList:
    if id == privId:
      privState = attr
  Logger.debug('Privilege state: {0}={1} ({2}) Enabled={3}'.format(privId, priv, LookupPrivilegeDisplayName(None, priv), privState))
  return privState

# Execute command. As windows hdp stack heavily relies on proper environment it is better to reload fresh environment
# on every execution. env variable will me merged with fresh environment for user.
def _call_command(command, logoutput=False, cwd=None, env=None, wait_for_finish=True, timeout=None, user=None):
  # TODO implement timeout, wait_for_finish
  Logger.info("Executing %s" % (command))
  if user:
    domain, username = UserHelper.parse_user_name(user, ".")

    proc_token = OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY | TOKEN_ADJUST_PRIVILEGES)

    old_states = []

    privileges = [
      SE_ASSIGNPRIMARYTOKEN_NAME,
      SE_INCREASE_QUOTA_NAME,
    ]

    for priv in privileges:
      old_states.append(QueryPrivilegeState(proc_token, priv))
      AdjustPrivilege(proc_token, priv)
      QueryPrivilegeState(proc_token, priv)

    user_token = LogonUser(username, domain, Script.get_password(user), win32con.LOGON32_LOGON_SERVICE,
                           win32con.LOGON32_PROVIDER_DEFAULT)
    env_token = DuplicateTokenEx(user_token, SecurityIdentification, TOKEN_QUERY, TokenPrimary)
    # getting updated environment for impersonated user and merge it with custom env
    current_env = CreateEnvironmentBlock(env_token, False)
    current_env = _merge_env(current_env, env)

    si = STARTUPINFO()
    out_handle, err_handle, out_file, err_file = _create_tmp_files(current_env)
    ok, si.hStdInput = _safe_duplicate_handle(GetStdHandle(STD_INPUT_HANDLE))
    if not ok:
      raise Exception("Unable to create StdInput for child process")
    ok, si.hStdOutput = _safe_duplicate_handle(out_handle)
    if not ok:
      raise Exception("Unable to create StdOut for child process")
    ok, si.hStdError = _safe_duplicate_handle(err_handle)
    if not ok:
      raise Exception("Unable to create StdErr for child process")

    Logger.debug("Redirecting stdout to '{0}', stderr to '{1}'".format(out_file.name, err_file.name))

    si.dwFlags = win32con.STARTF_USESTDHANDLES
    si.lpDesktop = ""

    try:
      info = CreateProcessAsUser(user_token, None, command, None, None, 1, win32con.CREATE_NO_WINDOW, current_env, cwd, si)
      hProcess, hThread, dwProcessId, dwThreadId = info
      hThread.Close()

      try:
        WaitForSingleObject(hProcess, INFINITE)
      except KeyboardInterrupt:
        pass
      out, err = _get_files_output(out_file, err_file)
      code = GetExitCodeProcess(hProcess)
    finally:
      for priv in privileges:
        old_state = old_states.pop(0)
        AdjustPrivilege(proc_token, priv, old_state)
  else:
    # getting updated environment for current process and merge it with custom env
    cur_token = OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY)
    current_env = CreateEnvironmentBlock(cur_token, False)
    current_env = _merge_env(current_env, env)
    proc = subprocess32.Popen(command, stdout=subprocess32.PIPE, stderr=subprocess32.STDOUT,
                            cwd=cwd, env=current_env, shell=False)
    out, err = proc.communicate()
    code = proc.returncode

  if logoutput and out:
    Logger.info(out)
  if logoutput and err:
    Logger.info(err)
  return code, out, err


# see msdn Icacls doc for rights
def _set_file_acl(file, user, rights):
  acls_modify_cmd = "icacls {0} /grant {1}:{2}".format(file, user, rights)
  acls_remove_cmd = "icacls {0} /remove {1}".format(file, user)
  code, out, err = _call_command(acls_remove_cmd)
  if code != 0:
    raise Fail("Can not remove rights for path {0} and user {1}".format(file, user))
  code, out, err = _call_command(acls_modify_cmd)
  if code != 0:
    raise Fail("Can not set rights {0} for path {1} and user {2}".format(file, user))
  else:
    return


class FileProvider(Provider):
  def action_create(self):
    path = self.resource.path

    if os.path.isdir(path):
      raise Fail("Applying %s failed, directory with name %s exists" % (self.resource, path))

    dirname = os.path.dirname(path)
    if not os.path.isdir(dirname):
      raise Fail("Applying %s failed, parent directory %s doesn't exist" % (self.resource, dirname))

    write = False
    content = self._get_content()
    if not os.path.exists(path):
      write = True
      reason = "it doesn't exist"
    elif self.resource.replace:
      if content is not None:
        with open(path, "rb") as fp:
          old_content = fp.read()
        if content != old_content:
          write = True
          reason = "contents don't match"
          if self.resource.backup:
            self.resource.env.backup_file(path)

    if write:
      Logger.info("Writing %s because %s" % (self.resource, reason))
      with open(path, "wb") as fp:
        if content:
          fp.write(content)

    if self.resource.owner and self.resource.mode:
      _set_file_acl(self.resource.path, self.resource.owner, self.resource.mode)

  def action_delete(self):
    path = self.resource.path

    if os.path.isdir(path):
      raise Fail("Applying %s failed, %s is directory not file!" % (self.resource, path))

    if os.path.exists(path):
      Logger.info("Deleting %s" % self.resource)
      os.unlink(path)

  def _get_content(self):
    content = self.resource.content
    if content is None:
      return None
    elif isinstance(content, basestring):
      return content
    elif hasattr(content, "__call__"):
      return content()
    raise Fail("Unknown source type for %s: %r" % (self, content))


class ExecuteProvider(Provider):
  def action_run(self):
    if self.resource.creates:
      if os.path.exists(self.resource.creates):
        return

    Logger.debug("Executing %s" % self.resource)

    if self.resource.path != []:
      if not self.resource.environment:
        self.resource.environment = {}

      self.resource.environment['PATH'] = os.pathsep.join(self.resource.path)

    for i in range(0, self.resource.tries):
      try:
        code, _, _ = _call_command(self.resource.command, logoutput=self.resource.logoutput,
                                   cwd=self.resource.cwd, env=self.resource.environment,
                                   wait_for_finish=self.resource.wait_for_finish,
                                   timeout=self.resource.timeout, user=self.resource.user)
        if code != 0 and not self.resource.ignore_failures:
          raise Fail("Failed to execute " + self.resource.command)
        break
      except Fail as ex:
        if i == self.resource.tries - 1:  # last try
          raise ex
        else:
          Logger.info("Retrying after %d seconds. Reason: %s" % (self.resource.try_sleep, str(ex)))
          time.sleep(self.resource.try_sleep)
      except ExecuteTimeoutException:
        err_msg = ("Execution of '%s' was killed due timeout after %d seconds") % (
          self.resource.command, self.resource.timeout)

        if self.resource.on_timeout:
          Logger.info("Executing '%s'. Reason: %s" % (self.resource.on_timeout, err_msg))
          _call_command(self.resource.on_timeout)
        else:
          raise Fail(err_msg)


class DirectoryProvider(Provider):
  def action_create(self):
    path = DirectoryProvider._trim_uri(self.resource.path)
    if not os.path.exists(path):
      Logger.info("Creating directory %s" % self.resource)
      if self.resource.recursive:
        os.makedirs(path)
      else:
        dirname = os.path.dirname(path)
        if not os.path.isdir(dirname):
          raise Fail("Applying %s failed, parent directory %s doesn't exist" % (self.resource, dirname))

        os.mkdir(path)

    if not os.path.isdir(path):
      raise Fail("Applying %s failed, file %s already exists" % (self.resource, path))

    if self.resource.owner and self.resource.mode:
      _set_file_acl(path, self.resource.owner, self.resource.mode)

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      if not os.path.isdir(path):
        raise Fail("Applying %s failed, %s is not a directory" % (self.resource, path))

      Logger.info("Removing directory %s and all its content" % self.resource)
      shutil.rmtree(path)

  @staticmethod
  def _trim_uri(file_uri):
    if file_uri.startswith("file:///"):
      return file_uri[8:].replace("/", os.sep)
    return file_uri
    # class res: pass
    # resource = res()
    # resource.creates = None
    # resource.path =[]
    # resource.tries = 1
    # resource.logoutput = True
    # resource.cwd = None
    # resource.environment = None
    # resource.wait_for_finish = True
    # resource.timeout = None
    # resource.command = "cmd /C echo 1 & echo 2"
    # provider = ExecuteProvider(resource)
    # provider.action_run()
    # pass
    # _set_file_acl("C:\\lol.txt", "Administrator","f")
    # pass
    # pass