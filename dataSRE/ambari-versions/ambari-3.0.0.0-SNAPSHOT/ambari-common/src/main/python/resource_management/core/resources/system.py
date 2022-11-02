#!/usr/bin/env python
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

__all__ = ["File", "Directory", "Link", "Execute", "ExecuteScript", "Mount"]

from ambari_commons import subprocess32
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class File(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  backup = ResourceArgument()
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  content = ResourceArgument()
  # whether to replace files with different content
  replace = ResourceArgument(default=True)
  encoding = ResourceArgument()
  """
  Grants x-bit for all the folders up-to the file
  
  u - user who is owner
  g - user from group
  o - other users
  a - all
  
  The letters can be combined together.
  """
  cd_access = ResourceArgument()

  actions = Resource.actions + ["create", "delete"]


class Directory(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  follow = BooleanArgument(default=True)  # follow links?
  """
  Example:
  Directory('/a/b/c/d', create_parents=False) # will fail unless /a/b/c exists
  Directory('/a/b/c/d', create_parents=True) # will succeed if /a/b/c doesn't exists
  """
  create_parents = BooleanArgument(default=False)
  """
  Grants x-bit for all the folders up-to the directory
  
  u - user who is owner
  g - user from group
  o - other users
  a - all
  
  The letters can be combined together.
  """
  cd_access = ResourceArgument()
  """
  If True sets the user and group mentioned in arguments, for all the contents of folder and its subfolder.
  
  CAUNTION: THIS IS NO RECOMENDED TO USE THIS, please treat such a usages as last resort hacks. 
  The problem with using recursive permissions setting is that the system can be damaged badly by doing this,
  imagine recursively setting for '/' or '/etc' (it can break the system) or other important for user folder.
  This is partially fixed by 'safemode_folders' feature, but still is risky.
  
  See also: safemode_folders, recursion_follow_links
  """
  recursive_ownership = BooleanArgument(default=False)
  
  """
  A dictionary, which gives the mode flags which should be set for files in key 'f', and for
  directories in key 'd'.
  
  The format of those flags should be symbolic format used in chmod. From chmod man page:
  the format of a symbolic mode is [ugoa...][[+-=][perms...]...], 
  where perms is either zero or more letters from the set rwxXst, or a single letter from the set ugo.  
  Multiple symbolic modes can be given, separated  by commas.
  
  u - user who is owner
  g - user from group
  o - other users
  a - all
  
  Example:
  recursive_mode_flags = {
    'd': 'u+rwx,g+rx' # for subfolders, enforce 'rwx' (read,write,execute) for owner of the file, and 'rx' (read,execute) for group, don't change 'others' permissions
    'f': 'u+rw,go+r' # for files in the directory, enforce 'rw' (read, write) for owner of the file, and 'r' (read) for group and others.
  }
  
  recursive_mode_flags = {
    'd': 'a+rwx' # for subfolders, enforce rwxrwxrwx (777) permisons. 
    'f': 'a+rw' # for files in the directory, enforce adding 'rw' (read,write) to original permissions. If file had 'x' bit before it will stay there.
  }
  
  CAUNTION: THIS IS NO RECOMENDED TO USE THIS, please treat such a usages as last resort hacks. 
  The problem with using recursive permissions setting is that the system can be damaged badly by doing this,
  imagine recursively setting permissions for '/' or '/etc' (it can break the system) or other important for user folder.
  This is partially fixed by 'safemode_folders' feature, but still is risky. 
  
  See also: safemode_folders, recursion_follow_links   
  """
  recursive_mode_flags = ResourceArgument(default=None)
  
  """
  This is the list folder which are not allowed to be recursively chmod-ed or chown-ed. (recursive_ownership and recursive_mode_flags).
  Fail exception will appear if tried.
  
  Example of a dangerous code, which will not be succeed:
  Directory("/",
    owner="my_user", 
    recursive_ownership=True
  )
  
  This aims to the resolve the problem of mistakenly doing recursive actions for system necessary folders.
  which results in damaging the operating system.
  """
  safemode_folders =  ForcedListArgument(default=["/", "/bin", "/sbin", "/etc", "/dev",
                                                  "/proc", "/var", "/usr", "/home", "/boot", "/lib", "/opt",
                                                  "/mnt", "/media", "/srv", "/root", "/sys" ])
  
  """
  If True while recursive chown/chmod is done (recursive_ownership or recursive_mode_flags),
  symlinks will be followed, duing recursion walking, also 
  this will also make chmod/chown to set permissions for symlink targets, not for symlink itself.
  
  Note: if recursion_follow_links=False chmod will not set permissions nor on symlink neither on targets. 
  As according to chmod man: 'This is not a problem since the permissions of symbolic links are never used'. 
  """
  recursion_follow_links = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Link(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  to = ResourceArgument(required=True)
  hard = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Execute(Resource):
  action = ForcedListArgument(default="run")
  
  """
  Recommended:
  command = ('rm','-f','myfile')
  Not recommended:
  command = 'rm -f myfile'
  
  The first one helps to stop escaping issues
  """
  command = ResourceArgument(default=lambda obj: obj.name)
  
  creates = ResourceArgument()
  """
  cwd won't work for:
  - commands run as sudo
  - commands run as user (which uses sudo as well)
  
  This is because non-interactive sudo commands doesn't support that.
  """
  cwd = ResourceArgument()
  # this runs command with a specific env variables, env={'JAVA_HOME': '/usr/jdk'}
  environment = ResourceArgument(default={})
  user = ResourceArgument()
  returns = ForcedListArgument(default=0)
  tries = ResourceArgument(default=1)
  try_sleep = ResourceArgument(default=0) # seconds
  path = ForcedListArgument(default=[])
  actions = Resource.actions + ["run"]
  # TODO: handle how this is logged / tested?
  """
  A one-argument function, which will be executed,
  once new line comes into command output.
  
  The only parameter of this function is a new line which comes to output.
  """
  on_new_line = ResourceArgument()
  """
  True           -  log it in INFO mode
  False          -  never log it
  None (default) -  log it in DEBUG mode
  """
  logoutput = ResourceArgument(default=None)

  """
  if on_timeout is not set leads to failing after x seconds,
  otherwise calls on_timeout
  """
  timeout = ResourceArgument() # seconds
  on_timeout = ResourceArgument()
  """
  Wait for command to finish or not. 
  
  NOTE:
  In case of False, since any command results are skipped, it disables some functionality: 
  - non-zero return code failure
  - logoutput
  - tries
  - try_sleep
  """
  wait_for_finish = BooleanArgument(default=True)
  """
  For calling more advanced commands use as_sudo(command) option.
  Example:
  command1 = as_sudo(["cat,"/etc/passwd"]) + " | grep user"
  command2 = as_sudo(["ls", "/root/example.txt") + " && " + as_sudo(["rm","-f","example.txt"])
  """
  sudo = BooleanArgument(default=False)
  """
  subprocess32.PIPE - enable output gathering
  None - disable output to gathering, and output to Python out straightly (even if logoutput is False)
  subprocess32.STDOUT - redirect to stdout (not valid as value for stdout agument)
  {int fd} - redirect to file with descriptor.
  {string filename} - redirects to a file with name.
  """
  stdout = ResourceArgument(default=subprocess32.PIPE)
  stderr = ResourceArgument(default=subprocess32.STDOUT)

  """
  This argument takes TerminateStrategy constants. Import it as shown below:
  from resource_management.core.signal_utils import TerminateStrategy

  Possible values are:
  TerminateStrategy.TERMINATE_PARENT - kill parent process with SIGTERM (is perfect if all children handle SIGTERM signal)
  TerminateStrategy.KILL_PROCESS_GROUP - kill process GROUP with SIGTERM and if not effective with SIGKILL
  TerminateStrategy.KILL_PROCESS_TREE - send SIGTERM to every process in the tree
  """
  timeout_kill_strategy = ResourceArgument(default=TerminateStrategy.TERMINATE_PARENT)

class ExecuteScript(Resource):
  action = ForcedListArgument(default="run")
  code = ResourceArgument(required=True)
  cwd = ResourceArgument()
  environment = ResourceArgument()
  interpreter = ResourceArgument(default="/bin/bash")
  user = ResourceArgument()
  group = ResourceArgument()

  actions = Resource.actions + ["run"]


class Mount(Resource):
  action = ForcedListArgument(default="mount")
  mount_point = ResourceArgument(default=lambda obj: obj.name)
  device = ResourceArgument()
  fstype = ResourceArgument()
  options = ResourceArgument(default=["defaults"])
  dump = ResourceArgument(default=0)
  passno = ResourceArgument(default=2)

  actions = Resource.actions + ["mount", "umount", "remount", "enable",
                                "disable"]
