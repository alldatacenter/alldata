# !/usr/bin/env python

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
import getpass
import os
import random
import shlex
from ambari_commons import subprocess32
import sys
import tempfile
import time
import string

import ctypes

import msvcrt

import pywintypes
import win32api
import win32con
import win32event
import win32file
import win32net
import win32netcon
import win32process
import win32security
import win32service
import win32serviceutil
import winerror
import winioctlcon
import wmi

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg

SERVICE_STATUS_UNKNOWN = "unknown"
SERVICE_STATUS_STARTING = "starting"
SERVICE_STATUS_RUNNING = "running"
SERVICE_STATUS_STOPPING = "stopping"
SERVICE_STATUS_STOPPED = "stopped"
SERVICE_STATUS_NOT_INSTALLED = "not installed"

WHOAMI_GROUPS = "whoami /groups"
ADMIN_ACCOUNT = "BUILTIN\\Administrators"

#
# os.symlink is not implemented in Windows. Patch it.
#
__CSL = None
def symlink(source, link_name):
  '''symlink(source, link_name)
     Creates a symbolic link pointing to source named link_name'''
  global __CSL
  if __CSL is None:
    csl = ctypes.windll.kernel32.CreateSymbolicLinkW
    csl.argtypes = (ctypes.c_wchar_p, ctypes.c_wchar_p, ctypes.c_uint32)
    csl.restype = ctypes.c_ubyte
    __CSL = csl
  flags = 0
  if source is not None and os.path.isdir(source):
    flags = 1
  if __CSL(link_name, source, flags) == 0:
    raise ctypes.WinError()

os.symlink = symlink

# Win32file doesn't seem to have this attribute.
FILE_ATTRIBUTE_REPARSE_POINT = 1024
# To make things easier.
REPARSE_FOLDER = (win32file.FILE_ATTRIBUTE_DIRECTORY | FILE_ATTRIBUTE_REPARSE_POINT)

# For the parse_reparse_buffer function
SYMBOLIC_LINK = 'symbolic'
MOUNTPOINT = 'mountpoint'
GENERIC = 'generic'

def islink(fpath):
  """ Windows islink implementation. """
  if win32file.GetFileAttributes(fpath) & REPARSE_FOLDER == REPARSE_FOLDER:
    return True
  return False

os.path.islink = islink

def _parse_reparse_buffer(original, reparse_type=SYMBOLIC_LINK):
  """ Implementing the below in Python:

  typedef struct _REPARSE_DATA_BUFFER {
      ULONG  ReparseTag;
      USHORT ReparseDataLength;
      USHORT Reserved;
      union {
          struct {
              USHORT SubstituteNameOffset;
              USHORT SubstituteNameLength;
              USHORT PrintNameOffset;
              USHORT PrintNameLength;
              ULONG Flags;
              WCHAR PathBuffer[1];
          } SymbolicLinkReparseBuffer;
          struct {
              USHORT SubstituteNameOffset;
              USHORT SubstituteNameLength;
              USHORT PrintNameOffset;
              USHORT PrintNameLength;
              WCHAR PathBuffer[1];
          } MountPointReparseBuffer;
          struct {
              UCHAR  DataBuffer[1];
          } GenericReparseBuffer;
      } DUMMYUNIONNAME;
  } REPARSE_DATA_BUFFER, *PREPARSE_DATA_BUFFER;

  """
  # Size of our data types
  SZULONG = 4 # sizeof(ULONG)
  SZUSHORT = 2 # sizeof(USHORT)

  # Our structure.
  # Probably a better way to iterate a dictionary in a particular order,
  # but I was in a hurry, unfortunately, so I used pkeys.
  buffer = {
    'tag' : SZULONG,
    'data_length' : SZUSHORT,
    'reserved' : SZUSHORT,
    SYMBOLIC_LINK : {
      'substitute_name_offset' : SZUSHORT,
      'substitute_name_length' : SZUSHORT,
      'print_name_offset' : SZUSHORT,
      'print_name_length' : SZUSHORT,
      'flags' : SZULONG,
      'buffer' : u'',
      'pkeys' : [
        'substitute_name_offset',
        'substitute_name_length',
        'print_name_offset',
        'print_name_length',
        'flags',
        ]
    },
    MOUNTPOINT : {
      'substitute_name_offset' : SZUSHORT,
      'substitute_name_length' : SZUSHORT,
      'print_name_offset' : SZUSHORT,
      'print_name_length' : SZUSHORT,
      'buffer' : u'',
      'pkeys' : [
        'substitute_name_offset',
        'substitute_name_length',
        'print_name_offset',
        'print_name_length',
        ]
    },
    GENERIC : {
      'pkeys' : [],
      'buffer': ''
    }
  }

  # Header stuff
  buffer['tag'] = original[:SZULONG]
  buffer['data_length'] = original[SZULONG:SZUSHORT]
  buffer['reserved'] = original[SZULONG+SZUSHORT:SZUSHORT]
  original = original[8:]

  # Parsing
  k = reparse_type
  for c in buffer[k]['pkeys']:
    if type(buffer[k][c]) == int:
      sz = buffer[k][c]
      bytes = original[:sz]
      buffer[k][c] = 0
      for b in bytes:
        n = ord(b)
        if n:
          buffer[k][c] += n
      original = original[sz:]

  # Using the offset and length's grabbed, we'll set the buffer.
  buffer[k]['buffer'] = original
  return buffer

def readlink(fpath):
  """ Windows readlink implementation. """
  # This wouldn't return true if the file didn't exist, as far as I know.
  if not islink(fpath):
    return None

  try:
    # Open the file correctly depending on the string type.
    if type(fpath) == unicode:
      handle = win32file.CreateFileW(fpath, win32file.GENERIC_READ, 0, None, win32file.OPEN_EXISTING, win32file.FILE_FLAG_OPEN_REPARSE_POINT | win32file.FILE_FLAG_BACKUP_SEMANTICS, 0)
    else:
      handle = win32file.CreateFile(fpath, win32file.GENERIC_READ, 0, None, win32file.OPEN_EXISTING, win32file.FILE_FLAG_OPEN_REPARSE_POINT | win32file.FILE_FLAG_BACKUP_SEMANTICS, 0)

    # MAXIMUM_REPARSE_DATA_BUFFER_SIZE = 16384 = (16*1024)
    buffer = win32file.DeviceIoControl(handle, winioctlcon.FSCTL_GET_REPARSE_POINT, None, 16*1024)
    # Above will return an ugly string (byte array), so we'll need to parse it.

    # But first, we'll close the handle to our file so we're not locking it anymore.
    win32file.CloseHandle(handle)

    # Minimum possible length (assuming that the length of the target is bigger than 0)
    if len(buffer) < 9:
      return None
    # Parse and return our result.
    result = _parse_reparse_buffer(buffer)
    offset = result[SYMBOLIC_LINK]['substitute_name_offset']
    ending = offset + result[SYMBOLIC_LINK]['substitute_name_length']
    rpath = result[SYMBOLIC_LINK]['buffer'][offset:ending].replace('\x00','')
    if len(rpath) > 4 and rpath[0:4] == '\\??\\':
      rpath = rpath[4:]
    return rpath
  except pywintypes.error, e:
    raise OSError(e.winerror, e.strerror, fpath)

os.readlink = readlink


class OSVERSIONINFOEXW(ctypes.Structure):
    _fields_ = [('dwOSVersionInfoSize', ctypes.c_ulong),
                ('dwMajorVersion', ctypes.c_ulong),
                ('dwMinorVersion', ctypes.c_ulong),
                ('dwBuildNumber', ctypes.c_ulong),
                ('dwPlatformId', ctypes.c_ulong),
                ('szCSDVersion', ctypes.c_wchar*128),
                ('wServicePackMajor', ctypes.c_ushort),
                ('wServicePackMinor', ctypes.c_ushort),
                ('wSuiteMask', ctypes.c_ushort),
                ('wProductType', ctypes.c_byte),
                ('wReserved', ctypes.c_byte)]

def get_windows_version():
  """
  Gets the OS major and minor versions.  Returns a tuple of
  (OS_MAJOR, OS_MINOR).
  """
  os_version = OSVERSIONINFOEXW()
  os_version.dwOSVersionInfoSize = ctypes.sizeof(os_version)
  retcode = ctypes.windll.Ntdll.RtlGetVersion(ctypes.byref(os_version))
  if retcode != 0:
    raise Exception("Failed to get OS version")

  return os_version.dwMajorVersion, os_version.dwMinorVersion, os_version.dwBuildNumber

CHECK_FIREWALL_SCRIPT = """[string]$CName = $env:computername
$reg = [Microsoft.Win32.RegistryKey]::OpenRemoteBaseKey("LocalMachine",$computer)
$domain = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\DomainProfile").GetValue("EnableFirewall")
$standart = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\StandardProfile").GetValue("EnableFirewall")
$public = $reg.OpenSubKey("System\CurrentControlSet\Services\SharedAccess\Parameters\FirewallPolicy\PublicProfile").GetValue("EnableFirewall")
Write-Host $domain
Write-Host $standart
Write-Host $public
"""

def _create_tmp_files():
  out_file = tempfile.TemporaryFile(mode="r+b")
  err_file = tempfile.TemporaryFile(mode="r+b")
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
    h = win32api.DuplicateHandle(win32process.GetCurrentProcess(),
                                 h,
                                 win32process.GetCurrentProcess(),
                                 0,
                                 True,
                                 win32con.DUPLICATE_SAME_ACCESS)
    return True, h
  except Exception as exc:
    if exc.winerror == winerror.ERROR_INVALID_HANDLE:
      return True, None
  return False, None


def run_os_command_impersonated(cmd, user, password, domain='.'):
  si = win32process.STARTUPINFO()

  out_handle, err_handle, out_file, err_file = _create_tmp_files()

  ok, si.hStdInput = _safe_duplicate_handle(win32api.GetStdHandle(win32api.STD_INPUT_HANDLE))

  if not ok:
    raise Exception("Unable to create StdInput for child process")
  ok, si.hStdOutput = _safe_duplicate_handle(out_handle)
  if not ok:
    raise Exception("Unable to create StdOut for child process")
  ok, si.hStdError = _safe_duplicate_handle(err_handle)
  if not ok:
    raise Exception("Unable to create StdErr for child process")

  si.dwFlags = win32process.STARTF_USESTDHANDLES
  si.lpDesktop = ""

  user_token = win32security.LogonUser(user, domain, password,
                                       win32con.LOGON32_LOGON_SERVICE, win32con.LOGON32_PROVIDER_DEFAULT)
  primary_token = win32security.DuplicateTokenEx(user_token,
                                                 win32security.SecurityImpersonation, 0, win32security.TokenPrimary)
  info = win32process.CreateProcessAsUser(primary_token, None, cmd, None, None, 1, 0, None, None, si)

  hProcess, hThread, dwProcessId, dwThreadId = info
  hThread.Close()

  try:
    win32event.WaitForSingleObject(hProcess, win32event.INFINITE)
  except KeyboardInterrupt:
    pass

  out, err = _get_files_output(out_file, err_file)
  exitcode = win32process.GetExitCodeProcess(hProcess)

  return exitcode, out, err

def os_run_os_command(cmd, env=None, shell=False, cwd=None):
  if isinstance(cmd,basestring):
    cmd = cmd.replace("\\", "\\\\")
    cmd = shlex.split(cmd)
  process = subprocess32.Popen(cmd,
                             stdout=subprocess32.PIPE,
                             stdin=subprocess32.PIPE,
                             stderr=subprocess32.PIPE,
                             env=env,
                             cwd=cwd,
                             shell=shell
  )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata

# execute powershell script passed in script_content. Script will be in temporary file to avoid different escape
# and formatting problems.
def run_powershell_script(script_content):
  tmp_dir = tempfile.gettempdir()
  random_filename = ''.join(random.choice(string.lowercase) for i in range(10))
  script_file = open(os.path.join(tmp_dir,random_filename+".ps1"),"w")
  script_file.write(script_content)
  script_file.close()
  result = os_run_os_command("powershell  -ExecutionPolicy unrestricted -File {0}".format(script_file.name))
  os.remove(script_file.name)
  return result

def os_change_owner(filePath, user, recursive):
  cmd = ['icacls', filePath, '/setowner', user]
  if recursive:
    cmd = ['icacls', filePath, '/t', '/setowner', user]
  retcode, outdata, errdata = os_run_os_command(cmd)
  return retcode

def os_is_root():
  '''
  Checks whether the current user is a member of the Administrators group
  Returns True if yes, otherwise False
  '''
  retcode, out, err = os_run_os_command(WHOAMI_GROUPS)
  if retcode != 0:
    err_msg = "Unable to check the current user's group memberships. " \
              "Command {0} returned exit code {1} with message: {2}".format(WHOAMI_GROUPS, retcode, err)
    print_warning_msg(err_msg)
    raise FatalException(retcode, err_msg)

  #Check for Administrators group membership
  if -1 != out.find('\n' + ADMIN_ACCOUNT):
    return True

  return False

def os_set_file_permissions(file, mod, recursive, user):
  retcode = 0

  #WARN_MSG = "Command {0} returned exit code {1} with message: {2}"
  #if recursive:
  #  params = " -R "
  #else:
  #  params = ""
  #command = NR_CHMOD_CMD.format(params, mod, file)
  #retcode, out, err = os_run_os_command(command)
  #if retcode != 0:
  #  print_warning_msg(WARN_MSG.format(command, file, err))
  #command = NR_CHOWN_CMD.format(params, user, file)
  #retcode, out, err = os_run_os_command(command)
  #if retcode != 0:
  #  print_warning_msg(WARN_MSG.format(command, file, err))

  # rights = mod
  # acls_remove_cmd = "icacls {0} /remove {1}".format(file, user)
  # retcode, out, err = os_run_os_command(acls_remove_cmd)
  # if retcode == 0:
  #   acls_modify_cmd = "icacls {0} /grant {1}:{2}".format(file, user, rights)
  #   retcode, out, err = os_run_os_command(acls_modify_cmd)
  return retcode


def os_set_open_files_limit(maxOpenFiles):
  # No open files limit in Windows. Not messing around with the System Resource Manager, at least for now.
  pass


def os_getpass(prompt, stream=None):
  """Prompt for password with echo off, using Windows getch()."""
  if sys.stdin is not sys.__stdin__:
    return getpass.fallback_getpass(prompt, stream)

  for c in prompt:
    msvcrt.putch(c)

  pw = ""
  while True:
    c = msvcrt.getch()
    if c == '\r' or c == '\n':
      break
    if c == '\003':
      raise KeyboardInterrupt
    if c == '\b':
      if pw == '':
        pass
      else:
        pw = pw[:-1]
        msvcrt.putch('\b')
        msvcrt.putch(" ")
        msvcrt.putch('\b')
    else:
      pw = pw + c
      msvcrt.putch("*")

  msvcrt.putch('\r')
  msvcrt.putch('\n')
  return pw

#[fbarca] Not used for now, keep it around just in case
def wait_for_pid_wmi(processName, parentPid, pattern, timeout):
  """
    Check pid for existence during timeout
  """
  tstart = time.time()
  pid_live = 0

  c = wmi.WMI(find_classes=False)
  qry = "select * from Win32_Process where Name=\"%s\" and ParentProcessId=%d" % (processName, parentPid)

  while int(time.time() - tstart) <= timeout:
    for proc in c.query(qry):
      cmdLine = proc.CommandLine
      if cmdLine is not None and pattern in cmdLine:
        return pid_live
    time.sleep(1)
  return 0


#need this for redirecting output form python process to file
class SyncStreamWriter(object):
  def __init__(self, stream, hMutexWrite):
    self.stream = stream
    self.hMutexWrite = hMutexWrite

  def write(self, data):
    #Ensure that the output is thread-safe when writing from 2 separate streams into the same file
    #  (typical when redirecting both stdout and stderr to the same file).
    win32event.WaitForSingleObject(self.hMutexWrite, win32event.INFINITE)
    try:
      self.stream.write(data)
      self.stream.flush()
    finally:
      win32event.ReleaseMutex(self.hMutexWrite)

  def __getattr__(self, attr):
    return getattr(self.stream, attr)


class SvcStatusCallback(object):
  def __init__(self, svc):
    self.svc = svc

  def reportStartPending(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_START_PENDING)

  def reportStarted(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_RUNNING)

  def reportStopPending(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)

  def reportStopped(self):
    self.svc.ReportServiceStatus(win32service.SERVICE_STOPPED)


class WinServiceController:
  @staticmethod
  def Start(serviceName, waitSecs=30):
    err = 0
    msg = ''
    try:
      win32serviceutil.StartService(serviceName)
      if waitSecs:
        win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_RUNNING, waitSecs)
    except win32service.error, exc:
      if exc.winerror != 1056:
        msg = "Error starting service: %s" % exc.strerror
        err = exc.winerror
    return err, msg

  @staticmethod
  def Stop(serviceName, waitSecs=30):
    err = 0
    msg = ''
    try:
      if waitSecs:
        win32serviceutil.StopServiceWithDeps(serviceName, waitSecs=waitSecs)
      else:
        win32serviceutil.StopService(serviceName)
      if waitSecs:
        win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_STOPPED, waitSecs)
    except win32service.error, exc:
      if exc.winerror != 1062:
        msg = "Error stopping service: %s (%d)" % (exc.strerror, exc.winerror)
        err = exc.winerror
    return err, msg

  @staticmethod
  def QueryStatus(serviceName):
    statusString = SERVICE_STATUS_UNKNOWN

    try:
      status = win32serviceutil.QueryServiceStatus(serviceName)[1]

      if status == win32service.SERVICE_STOPPED:
        statusString = SERVICE_STATUS_STOPPED
      elif status == win32service.SERVICE_START_PENDING:
        statusString = SERVICE_STATUS_STARTING
      elif status == win32service.SERVICE_RUNNING:
        statusString = SERVICE_STATUS_RUNNING
      elif status == win32service.SERVICE_STOP_PENDING:
        statusString = SERVICE_STATUS_STOPPING
    except win32api.error:
      statusString = SERVICE_STATUS_NOT_INSTALLED
      pass

    return statusString

  @staticmethod
  def EnsureServiceIsStarted(serviceName, waitSecs=30):
    err = 0
    try:
      status = win32serviceutil.QueryServiceStatus(serviceName)[1]
      if win32service.SERVICE_RUNNING != status:
        if win32service.SERVICE_START_PENDING != status:
          win32serviceutil.StartService(serviceName)
        if waitSecs:
          win32serviceutil.WaitForServiceStatus(serviceName, win32service.SERVICE_RUNNING, waitSecs)
    except win32service.error, exc:
      if exc.winerror != 1056:
        err = exc.winerror
    return err


class WinService(win32serviceutil.ServiceFramework):
  # _svc_name_ = The service name
  # _svc_display_name_ = The service display name
  # _svc_description_ = The service description

  _heventSvcStop = win32event.CreateEvent(None, 1, 0, None)
  _hmtxOut = win32event.CreateMutex(None, False, None)  #[fbarca] Python doesn't support critical sections

  def __init__(self, *args):
    win32serviceutil.ServiceFramework.__init__(self, *args)

  def SvcDoRun(self):
    try:
      self.ReportServiceStatus(win32service.SERVICE_RUNNING)
      self.ServiceMain()
    except Exception, x:
      #TODO: Log exception
      self.SvcStop()

  def SvcStop(self):
    self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
    win32event.SetEvent(WinService._heventSvcStop)

  # Service code entry point. Override it to implement the intended functionality.
  def ServiceMain(self):
    #Default implementation, does nothing.
    win32event.WaitForSingleObject(WinService._heventSvcStop, win32event.INFINITE)
    pass

  @staticmethod
  def DefCtrlCHandler():
    print_info_msg("Ctrl+C handler invoked. Stopping.")
    win32event.SetEvent(WinService._heventSvcStop)
    pass

  #username domain\\username : The Username the service is to run under
  #password password : The password for the username
  #startup [manual|auto|disabled|delayed] : How the service starts, default = auto
  #interactive : Allow the service to interact with the desktop.
  #perfmonini file: .ini file to use for registering performance monitor data
  #perfmondll file: .dll file to use when querying the service for performance data, default = perfmondata.dll
  @classmethod
  def Install(cls, classPath = None, startupMode = "auto", username = None, password = None, interactive = False,
              perfMonIni = None, perfMonDll = None):
    installArgs = [sys.argv[0], "--startup=" + startupMode]
    if username is not None and username:
      if username.find('\\') == -1:
        username = '.\\' + username
      installArgs.append("--username=" + username)
      if password is not None and password:
        installArgs.append("--password=" + password)
    if interactive:
      installArgs.append("--interactive")
    if perfMonIni is not None and perfMonIni:
      installArgs.append("--perfmonini=" + perfMonIni)
    if perfMonDll is not None and perfMonDll:
      installArgs.append("--perfmondll=" + perfMonDll)
    installArgs.append("install")

    return win32serviceutil.HandleCommandLine(cls, classPath, installArgs)

  @classmethod
  def Start(cls, waitSecs = 30):
    return WinServiceController.Start(cls._svc_name_, waitSecs)

  @classmethod
  def Stop(cls, waitSecs = 30):
    return WinServiceController.Stop(cls._svc_name_, waitSecs)

  @classmethod
  def QueryStatus(cls):
    return WinServiceController.QueryStatus(cls._svc_name_)

  @classmethod
  def set_ctrl_c_handler(cls, ctrlHandler):
    win32api.SetConsoleCtrlHandler(ctrlHandler, True)
    pass

  def _RedirectOutputStreamsToFile(self, outFilePath):
    outFileDir = os.path.dirname(outFilePath)
    if not os.path.exists(outFileDir):
      os.makedirs(outFileDir)

    out_writer = SyncStreamWriter(file(outFilePath, "w"), self._hmtxOut)
    sys.stderr = out_writer
    sys.stdout = out_writer
    pass

  def CheckForStop(self):
    #Check for stop event to be signaled
    return win32event.WAIT_OBJECT_0 == win32event.WaitForSingleObject(WinService._heventSvcStop, 1)

  def _StopOrWaitForChildProcessToFinish(self, childProcess):
    #Wait for the child process to finish or for the stop event to be signaled
    if(win32event.WAIT_OBJECT_0 == win32event.WaitForMultipleObjects([WinService._heventSvcStop, childProcess._handle],
                                                                     False, win32event.INFINITE)):
      # The OS only detaches the child process when the master process exits.
      # We must kill it manually.
      try:
        #Sending signal.CTRL_BREAK_EVENT doesn't work. It only detaches the child process from the master.
        #  Must brutally terminate the child process. Sorry Java.
        childProcess.terminate()
      except OSError, e:
        print_info_msg("Unable to stop Ambari Server - " + str(e))
        return False

    return True

class SystemWideLock(object):

  def __init__(self, name):
    self._mutex = win32event.CreateMutex(None, 0, name)

  def lock(self, timeout=0):
    result = win32event.WaitForSingleObject(self._mutex, timeout)
    if result in [win32event.WAIT_TIMEOUT, win32event.WAIT_ABANDONED, win32event.WAIT_FAILED]:
      return False
    elif result == win32event.WAIT_OBJECT_0:
      return True

  def unlock(self):
    try:
      win32event.ReleaseMutex(self._mutex)
      return True
    except:
      return False

  def __del__(self):
    win32api.CloseHandle(self._mutex)

class UserHelper(object):
  ACTION_OK = 0
  USER_EXISTS = 1
  ACTION_FAILED = -1

  def __init__(self, userName):
    self.domainName, self.userName = UserHelper.parse_user_name(userName)
    if self.domainName:
      self.dcName = win32net.NetGetDCName(None, self.domainName)
    else:
      self.dcName = None
    self._policy = win32security.LsaOpenPolicy(self.dcName,
                                               win32security.POLICY_CREATE_ACCOUNT | win32security.POLICY_LOOKUP_NAMES)

  @staticmethod
  def parse_user_name(userName, defDomain=None):
    domainName = defDomain
    domainSepIndex = userName.find('\\')
    if domainSepIndex != -1:
      domainName = userName[0:domainSepIndex]
      userName = userName[domainSepIndex + 1:]
      if not domainName or domainName == '.' or domainName == win32api.GetComputerName():
        domainName = defDomain
    return (domainName, userName)

  def create_user(self, password, comment="Ambari user"):
    user_info = {}
    user_info['name'] = self.userName
    user_info['password'] = password
    user_info['priv'] = win32netcon.USER_PRIV_USER
    user_info['comment'] = comment
    user_info['flags'] = win32netcon.UF_NORMAL_ACCOUNT | win32netcon.UF_SCRIPT
    try:
      win32net.NetUserAdd(self.dcName, 1, user_info)
    except pywintypes.error as e:
      if e.winerror == 2224:
        return UserHelper.USER_EXISTS, e.strerror
      else:
        return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "User created."

  def find_user(self):
    try:
      user_info = win32net.NetUserGetInfo(self.dcName, self.userName, 0)
    except pywintypes.error as e:
      if e.winerror == 2221:
        return False
      else:
        raise
    return True

  def add_user_privilege(self, privilege):
    try:
      acc_sid = win32security.LookupAccountName(self.dcName, self.userName)[0]
      win32security.LsaAddAccountRights(self._policy, acc_sid, (privilege,))
    except pywintypes.error as e:
      return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "Privilege added."

  def remove_user_privilege(self, name, privilege):
    try:
      acc_sid = win32security.LookupAccountName(self.dcName, self.userName)[0]
      win32security.LsaRemoveAccountRights(self._policy, acc_sid, 0, (privilege,))
    except pywintypes.error as e:
      return UserHelper.ACTION_FAILED, e.strerror
    return UserHelper.ACTION_OK, "Privilege removed."
