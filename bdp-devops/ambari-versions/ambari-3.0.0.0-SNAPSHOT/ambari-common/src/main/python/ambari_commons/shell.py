# !/usr/bin/env python

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

import logging
import os
import string
import signal
from ambari_commons import subprocess32 as subprocess
import threading
from contextlib import contextmanager
import copy

import time

import sys

from ambari_commons import OSConst
from ambari_commons.buffered_queue import BufferedQueue
from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl

_logger = logging.getLogger()

PIPE_PTY = -3  # Popen stdout-only mode implemented via PopenEx

# default timeout for async invoked processes
__TIMEOUT_SECONDS = 300

__PACKAGE_MANAGER_LOCK_ACQUIRED_MSG = "Cannot obtain lock for Package manager. Retrying after {0} seconds. Reason: {1}"
__PACKAGE_MANAGER_REPO_ERROR_MSG = "Cannot download the package due to repository unavailability. Retrying after {0} seconds. Reason: {1}"


"""
Module hits:
 - subprocess_executor - executing application and returning `SubprocessCallResult`
 - process_executor - context manager, targeted for application output reading instantly and
   allow data processing in the same time as output from application still coming


Testing ints:
 - universal way to block any real application execution is to mock `launch_subprocess` call, which
   return Popen-like object
  
"""


class ReaderStrategy(object):
  BufferedQueue = 0
  BufferedChunks = 1


class SubprocessCallResult(object):
  """
  Holds response from subprocess call
  """

  def __init__(self, out="", error="", code=0):
    """
    :type out str
    :type error str
    :type code int
    """
    self.out = out
    self.error = error
    self.code = code

  @property
  def all_out(self):
    r = ""
    if self.out:
      r += self.out
    if self.error:
      r += self.error
    return r


class RepoCallContext(object):
  ignore_errors = True
  __retry_count = 2
  retry_sleep = 30

  retry_on_repo_unavailability = False
  retry_on_locked = True

  log_output = True

  use_repos = None
  skip_repos = None
  is_upgrade = False
  action_force = False  # currently only for install action

  def __init__(self, ignore_errors=True, retry_count=2, retry_sleep=30, retry_on_repo_unavailability=False,
               retry_on_locked=True, log_output=True, use_repos=None, skip_repos=None,
               is_upgrade=False, action_force=False):
    """
    :type ignore_errors bool
    :type retry_count int
    :type retry_sleep int
    :type retry_on_repo_unavailability bool
    :type retry_on_locked bool
    :type log_output bool
    :type use_repos dict
    :type skip_repos set|list
    :type is_upgrade bool
    """
    self.ignore_errors = ignore_errors
    self.retry_count = retry_count
    self.retry_sleep = retry_sleep
    self.retry_on_repo_unavailability = retry_on_repo_unavailability
    self.retry_on_locked = retry_on_locked
    self.log_output = log_output
    self.use_repos = use_repos
    self.skip_repos = skip_repos
    self.is_upgrade = is_upgrade
    self.action_force = action_force

  @property
  def retry_count(self):
    return self.__retry_count

  @retry_count.setter
  def retry_count(self, value):
    """
    :type value int
    """
    # at least do one retry, to run after repository is cleaned
    self.__retry_count = 2 if value < 2 else value


def __set_winsize(fd, row, col):
  """
  Set geometry of terminal, fd should be a tty or pty or ioctl will fail.
  """
  import termios
  import struct
  import fcntl
  winsize = struct.pack("HHHH", row, col, 0, 0)
  fcntl.ioctl(fd, termios.TIOCSWINSZ, winsize)


def __terminal_width(fd=1):
  """
  Checking terminal width in the way yum output.py doing it, existing for debug purposes

  Source:
    https://github.com/rpm-software-management/yum/blob/master/output.py#L65
  """
  import termios
  import struct
  import fcntl
  try:
    buf = 'abcdefgh'
    buf = fcntl.ioctl(fd, termios.TIOCGWINSZ, buf)
    ret = struct.unpack('hhhh', buf)[1]
    if ret == 0:
      return 80
    # Add minimum too?
    return ret
  except:   # IOError
    return 80


class PopenEx(subprocess.Popen):
  """
  Same nice Popen with stdout handles hack to allow pty instead of pipe. This will allow to control terminal geometry
  to eliminate some applications bugs with output formatting according to terminal width.

  TODO: move the code directly to subprocess32.py
  """

  def _get_handles(self, stdin, stdout, stderr):
    """
    Replace Pipe with pty fd for stdout
    """
    import pty
    import tty

    is_extended_pipe = stdout == PIPE_PTY
    fd_read, fd_write = None, None

    if is_extended_pipe:
      fd_read, fd_write = pty.openpty()
      tty.setraw(fd_read)   # do not interpret control characters
      tty.setraw(fd_write)  # do not interpret control characters
      stdout = fd_write

    handles = super(PopenEx, self)._get_handles(stdin, stdout, stderr)
    if len(handles) == 2:  # python 2.7+
      handles, to_close = handles
    else:
      to_close = None

    p2cread, p2cwrite, c2pread, c2pwrite, errread, errwrite = handles

    if is_extended_pipe:
      c2pread = fd_read
      if to_close:
        to_close.add(fd_read)

    handles = p2cread, p2cwrite, c2pread, c2pwrite, errread, errwrite

    if to_close:
      return handles, to_close

    return handles


def quote_bash_args(command):
  """
  Copied from resource_manager shell module to remove dependency between modules
  """
  if not command:
    return "''"

  if not isinstance(command, basestring):
    raise ValueError("Command should be a list of strings, found '{0}' in command list elements".format(str(command)))

  valid = set(string.ascii_letters + string.digits + '@%_-+=:,./')
  for char in command:
    if char not in valid:
      return "'" + command.replace("'", "'\"'\"'") + "'"
  return command


def string_cmd_from_args_list(command, auto_escape=True):
  """
  Copied from resource_manager shell module to remove dependency between modules
  """
  if auto_escape:
    return ' '.join(quote_bash_args(x) for x in command)
  else:
    return ' '.join(command)


def is_under_root():
  """
  Check if current script is executed with root permissions

  :rtype bool
  :return checks state
  """
  return os.geteuid() == 0


def launch_subprocess(command, term_geometry=(42, 255), env=None):
  """
  Process launch helper

  :param command Command to execute
  :type command list[str]|str
  :type term_geometry set|list
  :type env dict
  :rtype PopenEx
  """
  def _geometry_helper():
    """
    Setting proper terminal geometry
    """
    if term_geometry:
      __set_winsize(sys.stdout.fileno(), *term_geometry)
    # check term geometry
    # print "terminal_width: ", __terminal_width()

  # no way to execute shell command with bash pipes under sudo, it is fully dev responsibility
  if not is_under_root() and not isinstance(command, str):
    command = "{0} -H -E {1}".format(AMBARI_SUDO_BINARY, string_cmd_from_args_list(command))  # core.shell.as_sudo
  elif not is_under_root() and isinstance(command, str):
    _logger.debug("Warning, command  \"{0}\" doesn't support sudo appending".format(command))

  is_shell = not isinstance(command, (list, tuple))
  environ = copy.deepcopy(os.environ)

  if env:
    environ.update(env)

  return PopenEx(command, stdout=PIPE_PTY, stderr=subprocess.PIPE,
                 shell=is_shell, preexec_fn=_geometry_helper, close_fds=True, env=environ)


def chunks_reader(cmd, kill_timer):
  """
  Low level reader based on os.read.

  Notice: Could stuck, if were killed called process pid without killing children processes, which still holding stdout

  :type cmd PopenEx
  :type kill_timer threading.Timer
  """
  import select
  str_buffer = ""
  read_queue = [cmd.stdout]
  chunk_size = 1024

  while True:
    is_cmd_up = cmd.poll() is None

    if not is_cmd_up:
      kill_timer.cancel()

    ready, _, _ = select.select(read_queue, [], [], 1)

    if not is_cmd_up and not ready:
      for line in str_buffer.strip(os.linesep).split(os.linesep):
        yield line
      break

    # process one chunk of the data
    try:
      data_chunk = os.read(read_queue[0].fileno(), chunk_size)
    except OSError:
      break

    if not data_chunk:
      break
    str_buffer += data_chunk

    if os.linesep in str_buffer:
      copy_offset = str_buffer.rindex(os.linesep)
      buff_lines = str_buffer[:copy_offset]
      str_buffer = str_buffer[copy_offset:]
      for line in buff_lines.strip(os.linesep).split(os.linesep):
        yield line

  kill_timer.cancel()


def queue_reader(cmd, q, timeout, timer):
  """
  Hungry process output reader, which reads stdout as it come and allow consumer to get it by request.

  :arg cmd Popen object
  :arg q read/write queue
  :arg timeout read timeout
  :arg timer kill timer, used to cancel it when process already exited

  :type cmd subprocess.Popen
  :type q BufferedQueue
  :type timeout int
  :type timer threading.Timer
  """

  def _reader():
    try:
      while True:
        data_chunk = cmd.stdout.readline()
        """
        data_chunk could be:
        
        null   - no input received and read were non-blocking, means that parent process died or stdout of
                 parent process was closed
        \n     - empty line
        [data] - any string 
        """
        if not data_chunk:
          break
        q.put(data_chunk)
    except IOError:
      pass
    finally:
      q.notify_end()
      try:
        cmd.stdout.close()  # close stdout if any exception appear before, triggering by this parent process shutdown
      except (IOError, OSError):
        pass

  reader_th = threading.Thread(target=_reader)
  reader_th.start()

  while not q.empty:
    str_line = q.get(timeout)
    if str_line:
      yield str_line.strip(os.linesep)

    if cmd.poll() is not None and timer.is_alive():
      timer.cancel()


def __watchdog_func(event, cmd, exec_timeout):
  """
  Watchdog function for subprocess executors

  :type event Event
  :type cmd Popen
  :type exec_timeout int


  Usage example:
      event = threading.Event()

      cmd = Popen(...)

      thread = threading.Thread(target=watchdog_func, args=(event, cmd, execution_timeout,))
      thread.start()

      ....cmd.communicate() or any another processing....

      event.set()
      thread.join()
      ......result code....
  """
  event.wait(exec_timeout)
  if cmd.returncode is None:
    _logger.error("Task timed out and will be killed")
    kill_process_with_children(cmd.pid)


def subprocess_executor(command, timeout=__TIMEOUT_SECONDS, strategy=ReaderStrategy.BufferedQueue, env=None):
  """
  Run command with limited time for execution, after timeout command would be killed

  :param command Command to execute. non-shell commands able to use sudo automatically, while shell ones not.
                 Prefer tuple or list command format and use shell command style (str) at own risk
  :param timeout execution time limit in seconds. If None will default to TIMEOUT_SECONDS, -1 disable feature
  :param strategy the way how to process output. Available methods listed in ReaderStrategy
  :param env Environment variables for new process

  :type command list[str]|str
  :type timeout int
  :type strategy int
  :type env dict
  :rtype SubprocessCallResult

  """
  r = SubprocessCallResult()

  def _error_handler(_command, _error_log, _exit_code):
    r.error = os.linesep.join(_error_log)
    r.code = _exit_code

  with process_executor(command, timeout, _error_handler, strategy, env=env) as output:
    lines = [line for line in output]

  r.out = os.linesep.join(lines)
  return r


@contextmanager
def process_executor(command, timeout=__TIMEOUT_SECONDS, error_callback=None, strategy=ReaderStrategy.BufferedQueue,
                     env=None, silent=False):
  """
  Context manager for command execution

  :param command Command to execute
  :param timeout execution time limit in seconds. If None will default to TIMEOUT_SECONDS, -1 disable feature
  :param strategy the way how to process output. Available methods listed in ReaderStrategy
  :param env Environment variable for new spawned process
  :param silent no error logging if command execution failed, do not affect `error_callback` param

  :type command list|str
  :type timeout None|int
  :type error_callback func
  :type strategy int
  :type env dict
  :type silent bool

  :rtype stdout collections.Iterable

  Usage example:

   Option 1. Basic
     with process_executor(["ls", "-la]) as stdout:
       for line in stdout:
         print line

   Option 2. Extended
     def error_handler(command, error_log, exit_code):
       print "Command '{}' failed".format(command)
       print "Exit Code: {}   StdOut: {} \n".format(exit_code, "\n".join(error_log))

     with process_executor(["ls", "-la], timeout=10, error_callback=error_handler) as stdout:
       for line in stdout:
         print line

  """

  buff_queue = None
  kill_timer = None
  kill_timer_started = False

  try:
    cmd = launch_subprocess(command, env=env)
    if not isinstance(cmd, PopenEx):  # ToDo: make better solution to avoid issues in UT
      yield []
      return
    kill_timer = threading.Timer(timeout, lambda: cmd.kill())
    kill_timer.daemon = True
    if timeout > -1:
      kill_timer.start()
      kill_timer_started = True

    if strategy == ReaderStrategy.BufferedQueue:
      buff_queue = BufferedQueue()
      yield queue_reader(cmd, buff_queue, timeout, kill_timer)
    elif strategy == ReaderStrategy.BufferedChunks:
      yield chunks_reader(cmd, kill_timer)
    else:
      raise TypeError("Unknown reader strategy selected: {0}".format(strategy))

    _exit_code = cmd.poll()
    if _exit_code is None:
      cmd.terminate()

    if error_callback and cmd.returncode and cmd.returncode > 0:
      error_callback(command, cmd.stderr.readlines(), cmd.returncode)
  except Exception as e:
    if not silent:
      _logger.error("Exception during command '{0}' execution: {1}".format(command, str(e)))
    if error_callback:
      error_callback(command, [str(e)], -1)

    yield []
  finally:
    if buff_queue:
      buff_queue.notify_end()
    if kill_timer and kill_timer_started:
      kill_timer.cancel()
      kill_timer.join()


def get_all_children(base_pid):
  """
  Return all child PIDs of base_pid process
  :param base_pid starting PID to scan for children
  :return tuple of the following: pid, binary name, command line incl. binary
  :type base_pid int
  :rtype list[(int, str, str)]
  """
  parent_pid_path_pattern = "/proc/{0}/task/{0}/children"
  comm_path_pattern = "/proc/{0}/comm"
  cmdline_path_pattern = "/proc/{0}/cmdline"

  def read_children(pid):
    try:
      with open(parent_pid_path_pattern.format(pid), "r") as f:
        return [int(item) for item in f.readline().strip().split(" ")]
    except (IOError, ValueError):
      return []

  def read_command(pid):
    try:
      with open(comm_path_pattern.format(pid), "r") as f:
        return f.readline().strip()
    except IOError:
      return ""

  def read_cmdline(pid):
    try:
      with open(cmdline_path_pattern.format(pid), "r") as f:
        return f.readline().strip()
    except IOError:
      return ""

  pids = []
  scan_pending = [int(base_pid)]

  while scan_pending:
    curr_pid = scan_pending.pop(0)
    children = read_children(curr_pid)

    pids.append((curr_pid, read_command(curr_pid), read_cmdline(curr_pid)))
    scan_pending.extend(children)

  return pids


def get_existing_pids(pids):
  """
  Check if process with pid still exists (not counting it real state).
  Optimized to check PID list at once.
  :param pids list of PIDs to filter
  :return list of still existing PID
  :type pids list[int]
  :rtype list[int]
  """

  existing_pid_list = []

  try:
    all_existing_pid_list = [int(item) for item in os.listdir("/proc") if item.isdigit()]
  except (OSError, IOError):
    _logger.debug("Failed to check PIDs existence")
    return existing_pid_list

  for pid_item in pids:
    if pid_item in all_existing_pid_list:
      existing_pid_list.append(pid_item)

  return existing_pid_list


def wait_for_process_list_kill(pids, timeout=5, check_step_time=0.1):
  """
  Process tree waiter
  :type pids list[int]
  :type timeout int|float
  :type check_step_time int|float
  :param pids list of PIDs to watch
  :param timeout how long wait till giving up, seconds. Set 0 for nowait or None for infinite time
  :param check_step_time how often scan for existing PIDs, seconds
  """
  from threading import Thread, Event
  import time

  stop_waiting = Event()

  def _wait_loop():
    while not stop_waiting.is_set() and get_existing_pids(pids):
      time.sleep(check_step_time)

  if timeout == 0:  # no need for loop if no timeout is set
    return

  th = Thread(target=_wait_loop)
  stop_waiting.clear()

  th.start()
  th.join(timeout=timeout)
  stop_waiting.set()

  th.join()


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def kill_process_with_children(base_pid):
  """
  Process tree killer

  :type base_pid int
  """
  from resource_management.core import sudo

  exception_list = ["apt-get", "apt", "yum", "zypper", "zypp"]
  signals_to_post = {
    "SIGTERM": signal.SIGTERM,
    "SIGKILL": signal.SIGKILL
  }
  full_child_pids = get_all_children(base_pid)
  all_child_pids = [item[0] for item in full_child_pids if item[1].lower() not in exception_list and item[0] != os.getpid()]
  error_log = []

  for sig_name, sig in signals_to_post.items():
    # we need to kill processes from the bottom of the tree
    pids_to_kill = sorted(get_existing_pids(all_child_pids), reverse=True)
    for pid in pids_to_kill:
      try:
        sudo.kill(pid, sig)
      except OSError as e:
        error_log.append((sig_name, pid, repr(e)))

    if pids_to_kill:
      wait_for_process_list_kill(pids_to_kill)
      still_existing_pids = get_existing_pids(pids_to_kill)
      if still_existing_pids:
        _logger.warning("These PIDs {0} did not respond to {1} signal. Detailed commands list:\n {2}".format(
          ", ".join([str(i) for i in still_existing_pids]),
          sig_name,
          "\n".join([i[2] for i in full_child_pids if i[0] in still_existing_pids])
        ))

  if get_existing_pids(all_child_pids) and error_log:  # we're unable to kill all requested PIDs
    _logger.warn("Process termination error log:\n")
    for error_item in error_log:
      _logger.warn("PID: {0}, Process: {1}, Exception message: {2}".format(*error_item))


def __handle_retries(cmd, repo_properties, context, call_result, is_first_time, is_last_time):
  """
  :type cmd list
  :type repo_properties ambari_commons.repo_manager.generic_manager.GenericManagerProperties
  :type context RepoCallContext
  :type call_result SubprocessCallResult
  :type is_first_time bool
  :type is_last_time bool
  """
  out = call_result.all_out
  # handle first failure in a special way (update repo metadata after it, so next try has a better chance to succeed)
  if is_first_time and call_result.code and repo_properties.locked_output and repo_properties.locked_output not in out:
    __update_repo_metadata_after_bad_try(cmd, context, repo_properties, call_result.code, call_result.out)
    return False

  err_log_msg = None
  if context.retry_on_locked and repo_properties.locked_output and repo_properties.locked_output in out:
    err_log_msg = __PACKAGE_MANAGER_LOCK_ACQUIRED_MSG.format(context.retry_sleep, call_result.out)
  elif context.retry_on_repo_unavailability and repo_properties.repo_error:
    for err in repo_properties.repo_error:
      if err in call_result.all_out:
        err_log_msg = __PACKAGE_MANAGER_REPO_ERROR_MSG.format(context.retry_sleep, call_result.out)

  if err_log_msg and not is_last_time:
    _logger.info(err_log_msg)

  return is_last_time or not call_result.code or not err_log_msg


def __update_repo_metadata_after_bad_try(cmd, context, repo_properties, code, out):
  """
  :type cmd list
  :type context RepoCallContext
  :type repo_properties ambari_commons.repo_manager.generic_manager.GenericManagerProperties
  :type code str|int
  :type out str
  """
  repo_update_cmd = repo_properties.repo_update_cmd
  _logger.info("Execution of '%s' failed and returned %d. %s" % (string_cmd_from_args_list(cmd), code, out))

  call_result = subprocess_executor(repo_update_cmd, timeout=-1)

  if call_result.code:
    _logger.info("Execution of '%s' returned %d. %s" % (repo_update_cmd, call_result.code, call_result.out))

  _logger.info("Retrying to execute command after %d seconds" % context.retry_sleep)


def repository_manager_executor(cmd, repo_properties, context=RepoCallContext(), env=None):
  """
  Repository Manager execution call for install, remove, update commands with possibility to retry calls

  :type cmd list
  :type repo_properties ambari_commons.repo_manager.generic_manager.GenericManagerProperties | YumManagerProperties | ZypperManagerProperties | AptManagerProperties
  :type context RepoCallContext
  :type env dict

  :rtype SubprocessCallResult
  """
  call_result = None

  for i in range(context.retry_count):
    is_first_time = (i == 0)
    is_last_time = (i == context.retry_count - 1)

    call_result = subprocess_executor(cmd, timeout=-1, env=env)

    should_stop_retries = __handle_retries(cmd, repo_properties, context, call_result, is_first_time, is_last_time)
    if (is_last_time or should_stop_retries) and call_result.code != 0:
      message = "Failed to execute command '{0}', exited with code '{1}', message: '{2}'".format(
        cmd if not isinstance(cmd, (list, tuple)) else " ".join(cmd),
        call_result.code, call_result.error)

      if context.ignore_errors:
        _logger.warning(message)
      else:
        raise RuntimeError(message)
    if should_stop_retries:
      break

    time.sleep(context.retry_sleep)

  return call_result


class shellRunner(object):
  def run(self, script, user=None):
    pass

  def runPowershell(self, f=None, script_block=None, args=set()):
    raise NotImplementedError()


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class shellRunnerWindows(shellRunner):
  # Run any command
  def run(self, script, user=None):
    global _logger
    _logger.warn("user argument ignored on windows")
    code = 0
    if isinstance(script, list):
      cmd = " ".join(script)
    else:
      cmd = script
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    _logger.debug("Exitcode for %s is %d" % (cmd, code))
    return SubprocessCallResult(out=out, error=err, code=code)

  def runPowershell(self, f=None, script_block=None, args=set()):
    global _logger
    _logger.warn("user argument ignored on windows")

    cmd = None
    if f:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-File', f] + list(args)
    elif script_block:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-Command', script_block] + list(args)

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    _logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class shellRunnerLinux(shellRunner):
  _threadLocal = None

  def _change_uid(self):
    try:
      if self._threadLocal is not None:
        os.setuid(self._threadLocal.uid)
    except Exception:
      _logger.warn("can not switch user for running command.")

  # Run any command
  def run(self, script, user=None):
    import pwd

    try:
      if user is not None:
        user = pwd.getpwnam(user)[2]
      else:
        user = os.getuid()
      self._threadLocal.uid = user
    except Exception:
      _logger.warn("can not switch user for RUN_COMMAND.")

    cmd = script

    if isinstance(script, list):
      cmd = " ".join(script)

    cmd_list = ["/bin/bash", "--login", "--noprofile", "-c", cmd]
    p = subprocess.Popen(cmd_list, preexec_fn=self._change_uid, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE, shell=False, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    _logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}
