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

import time
import os
import tempfile
import shutil
import stat
import errno
import random
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from ambari_commons.unicode_tolerant_fs import unicode_walk
from ambari_commons import subprocess32

from resource_management.core.utils import attr_to_bitmask

if os.geteuid() == 0:
  def chown(path, owner, group):
    uid = owner.pw_uid if owner else -1
    gid = group.gr_gid if group else -1
    if uid != -1 or gid != -1:
      return os.chown(path, uid, gid)

  def chown_recursive(path, owner, group, follow_links=False):
    uid = owner.pw_uid if owner else -1
    gid = group.gr_gid if group else -1

    if uid == -1 and gid == -1:
      return

    for root, dirs, files in unicode_walk(path, followlinks=True):
      for name in files + dirs:
        try:
          if follow_links:
            os.chown(os.path.join(root, name), uid, gid)
          else:
            os.lchown(os.path.join(root, name), uid, gid)
        except OSError as ex:
          # Handle race condition: file was deleted/moved while iterating by
          # ignoring OSError: [Errno 2] No such file or directory
          if ex.errno != errno.ENOENT:
            raise


  def chmod(path, mode):
    """
    Wrapper around python function

    :type path str
    :type mode int
    """
    return os.chmod(path, mode)


  def chmod_extended(path, mode):
    """
    :type path str
    :type mode str
    """
    st = os.stat(path)
    os.chmod(path, attr_to_bitmask(mode, initial_bitmask=st.st_mode))

  def chmod_recursive(path, recursive_mode_flags, recursion_follow_links=False):
    """
    Change recursively permissions on directories or files

    :type path str
    :type recursive_mode_flags
    :type recursion_follow_links bool
    """
    dir_attrib = recursive_mode_flags["d"] if "d" in recursive_mode_flags else None
    files_attrib = recursive_mode_flags["f"] if "d" in recursive_mode_flags else None

    for root, dirs, files in unicode_walk(path, followlinks=recursion_follow_links):
      if dir_attrib is not None:
        for dir_name in dirs:
          full_dir_path = os.path.join(root, dir_name)
          chmod(full_dir_path, attr_to_bitmask(dir_attrib, initial_bitmask=os.stat(full_dir_path).st_mode))

      if files_attrib is not None:
        for file_name in files:
          full_file_path = os.path.join(root, file_name)
          chmod(full_file_path, attr_to_bitmask(files_attrib, initial_bitmask=os.stat(full_file_path).st_mode))

  def move(src, dst):
    shutil.move(src, dst)

  def copy(src, dst):
    shutil.copy(src, dst)

  def makedirs(path, mode):
    try:
      os.makedirs(path, mode)
    except OSError as ex:
      if ex.errno == errno.ENOENT:
        dirname = os.path.dirname(ex.filename)
        if os.path.islink(dirname) and not os.path.exists(dirname):
          raise Fail("Cannot create directory '{0}' as '{1}' is a broken symlink".format(path, dirname))
      elif ex.errno == errno.ENOTDIR:
        dirname = os.path.dirname(ex.filename)
        if os.path.isfile(dirname):
          raise Fail("Cannot create directory '{0}' as '{1}' is a file".format(path, dirname))
      elif ex.errno == errno.ELOOP:
        dirname = os.path.dirname(ex.filename)
        if os.path.islink(dirname) and not os.path.exists(dirname):
          raise Fail("Cannot create directory '{0}' as '{1}' is a looped symlink".format(path, dirname))

      raise

  def makedir(path, mode):
    os.mkdir(path)

  def symlink(source, link_name):
    os.symlink(source, link_name)

  def link(source, link_name):
    os.link(source, link_name)

  def unlink(path):
    os.unlink(path)

  def rmtree(path):
    shutil.rmtree(path)

  def create_file(filename, content, encoding=None, on_file_created=None):
    _create_file(filename, content, encoding=encoding, sudo=False, on_file_created=on_file_created)

  def read_file(filename, encoding=None):
    with open(filename, "rb") as fp:
      content = fp.read()

    content = content.decode(encoding) if encoding else content
    return content

  def path_exists(path):
    return os.path.exists(path)

  def path_isdir(path):
    return os.path.isdir(path)

  def path_islink(path):
    return os.path.islink(path)

  def path_lexists(path):
    return os.path.lexists(path)

  def readlink(path):
    return os.readlink(path)

  def path_isfile(path):
    return os.path.isfile(path)

  def stat(path):
    class Stat:
      def __init__(self, path):
        stat_val = os.stat(path)
        self.st_uid, self.st_gid, self.st_mode = stat_val.st_uid, stat_val.st_gid, stat_val.st_mode & 07777
    return Stat(path)

  def kill(pid, signal):
    os.kill(pid, signal)

  def listdir(path):
    return os.listdir(path)

else:
  # os.chown replacement
  def chown(path, owner, group):
    owner = owner.pw_name if owner else ""
    group = group.gr_name if group else ""
    if owner or group:
      shell.checked_call(["chown", owner+":"+group, path], sudo=True)

  def chown_recursive(path, owner, group, follow_links=False):
    owner = owner.pw_name if owner else ""
    group = group.gr_name if group else ""
    if owner or group:
      flags = ["-R"]
      if follow_links:
        flags.append("-L")
      shell.checked_call(["chown"] + flags + [owner+":"+group, path], sudo=True)

  # os.chmod replacement
  def chmod(path, mode):
    shell.checked_call(["chmod", oct(mode), path], sudo=True)

  def chmod_extended(path, mode):
    shell.checked_call(["chmod", mode, path], sudo=True)

  # os.makedirs replacement
  def makedirs(path, mode):
    shell.checked_call(["mkdir", "-p", path], sudo=True)
    chmod(path, mode)

  # os.makedir replacement
  def makedir(path, mode):
    shell.checked_call(["mkdir", path], sudo=True)
    chmod(path, mode)

  # os.symlink replacement
  def symlink(source, link_name):
    shell.checked_call(["ln","-sf", source, link_name], sudo=True)

  # os.link replacement
  def link(source, link_name):
    shell.checked_call(["ln", "-f", source, link_name], sudo=True)

  # os unlink
  def unlink(path):
    shell.checked_call(["rm","-f", path], sudo=True)

  # shutil.rmtree
  def rmtree(path):
    shell.checked_call(["rm","-rf", path], sudo=True)

  # fp.write replacement
  def create_file(filename, content, encoding=None, on_file_created=None):
    _create_file(filename, content, encoding=encoding, sudo=True, on_file_created=on_file_created)

  # fp.read replacement
  def read_file(filename, encoding=None):
    tmpf = tempfile.NamedTemporaryFile()
    shell.checked_call(["cp", "-f", filename, tmpf.name], sudo=True)

    with tmpf:
      with open(tmpf.name, "rb") as fp:
        content = fp.read()

    content = content.decode(encoding) if encoding else content
    return content

  # os.path.exists
  def path_exists(path):
    return (shell.call(["test", "-e", path], sudo=True)[0] == 0)

  # os.path.isdir
  def path_isdir(path):
    return (shell.call(["test", "-d", path], sudo=True)[0] == 0)

  # os.path.islink
  def path_islink(path):
    return (shell.call(["test", "-L", path], sudo=True)[0] == 0)

  # os.path.lexists
  def path_lexists(path):
    return (shell.call(["test", "-e", path], sudo=True)[0] == 0)

  # os.readlink
  def readlink(path):
    return shell.checked_call(["readlink", path], sudo=True)[1].strip()

  # os.path.isfile
  def path_isfile(path):
    return (shell.call(["test", "-f", path], sudo=True)[0] == 0)

  # os.stat
  def stat(path):
    class Stat:
      def __init__(self, path):
        cmd = ["stat", "-c", "%u %g %a", path]
        code, out, err = shell.checked_call(cmd, sudo=True, stderr=subprocess32.PIPE)
        values = out.split(' ')
        if len(values) != 3:
          raise Fail("Execution of '{0}' returned unexpected output. {2}\n{3}".format(cmd, code, err, out))
        uid_str, gid_str, mode_str = values
        self.st_uid, self.st_gid, self.st_mode = int(uid_str), int(gid_str), int(mode_str, 8)

    return Stat(path)

  # os.kill replacement
  def kill(pid, signal):
    try:
      shell.checked_call(["kill", "-"+str(signal), str(pid)], sudo=True)
    except Fail as ex:
      raise OSError(str(ex))

  # shutil.move replacement
  def move(src, dst):
    shell.checked_call(('mv', '-f', src, dst), sudo=True)

  # shutil.copy replacement
  def copy(src, dst):
    shell.checked_call(["cp", "-r", src, dst], sudo=True)

  # os.listdir replacement
  def listdir(path):
    if not path_isdir(path):
      raise Fail("{0} is not a directory. Cannot list files of it.".format(path))

    code, out, err = shell.checked_call(["ls", path], sudo=True, stderr=subprocess32.PIPE)
    files = out.splitlines()
    return files


  def chmod_recursive(path, recursive_mode_flags, recursion_follow_links):
    find_flags = []
    if recursion_follow_links:
      find_flags.append('-L')

    for key, flags in recursive_mode_flags.iteritems():
      shell.checked_call(["find"] + find_flags + [path, "-type", key, "-exec" , "chmod", flags ,"{}" ,";"])

def _create_file(filename, content, encoding, sudo, on_file_created=None):
    """
    Creates file in a temporary location, then set permissions via on_file_created callback (if provided),
    then move to final location.

    Creates empty file if content is None.
    """
    content = content if content else ""
    content = content.encode(encoding) if encoding else content

    tmpf_name = tempfile.gettempdir() + os.sep + tempfile.template + str(time.time()) + "_" + str(random.randint(0, 1000))
    with open(tmpf_name, "wb") as fp:
      fp.write(content)
    if on_file_created:
      on_file_created(tmpf_name)
    shell.checked_call(["mv", "-f", tmpf_name, filename], sudo=sudo)
