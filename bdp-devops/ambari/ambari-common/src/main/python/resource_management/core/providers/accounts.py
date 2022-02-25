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

from __future__ import with_statement

import grp
import pwd
from resource_management.core import shell
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.core.utils import lazy_property
from resource_management.core.exceptions import ExecutionFailed

class UserProvider(Provider):
  USERADD_USER_ALREADY_EXISTS_EXITCODE = 9

  options = dict(
    comment=(lambda self: self.user.pw_gecos, "-c"),
    gid=(lambda self: grp.getgrgid(self.user.pw_gid).gr_name, "-g"),
    uid=(lambda self: self.user.pw_uid, "-u"),
    shell=(lambda self: self.user.pw_shell, "-s"),
    password=(lambda self: self.user.pw_password, "-p"),
    home=(lambda self: self.user.pw_dir, "-d"),
    groups=(lambda self: self.user_groups, "-G")
  )
    
  def action_create(self):
    if not self.user:
      creating_user = True
      command = ['useradd', "-m"]
      Logger.info("Adding user %s" % self.resource)
    else:
      creating_user = False
      command = ['usermod']
      
      for option_name, attributes in self.options.iteritems():
        if getattr(self.resource, option_name) != None and getattr(self.resource, option_name) != attributes[0](self):
          # groups on system contain the one we need
          if attributes[1] == "-G" and set(getattr(self.resource, option_name)).issubset(set(attributes[0](self))):
            continue
          break
      else:
        return
      
      Logger.info("Modifying user %s" % (self.resource.username))

    if self.resource.system and not self.user:
      command.append("--system")
      
    for option_name, attributes in self.options.iteritems():   
      if attributes[1] == "-G":
        groups = self.resource.groups
        if self.user and self.user_groups:
          groups += self.user_groups
        option_value = ",".join(groups) 
      elif attributes[1] == "-u" and self.user and self.user.pw_uid == getattr(self.resource, option_name):
        option_value = None
      else:
        option_value = getattr(self.resource, option_name)
        
      if attributes[1] and option_value:
        command += [attributes[1], str(option_value)]

    # if trying to modify existing user, but no values to modify are provided
    if self.user and len(command) == 1:
      return

    command.append(self.resource.username)
    
    try:
      shell.checked_call(command, sudo=True)
    except ExecutionFailed as ex:
      # this "user already exists" can happen due to race condition when multiple processes create user at the same time
      if creating_user and ex.code == UserProvider.USERADD_USER_ALREADY_EXISTS_EXITCODE and self.user:
        self.action_create() # run modification of the user
      else:
        raise

  def action_remove(self):
    if self.user:
      command = ['userdel', self.resource.username]
      shell.checked_call(command, sudo=True)
      Logger.info("Removed user %s" % self.resource)

  @property
  def user(self):
    try:
      return pwd.getpwnam(self.resource.username)
    except KeyError:
      return None
    
  @lazy_property
  def user_groups(self):
    if self.resource.fetch_nonlocal_groups:
      return [g.gr_name for g in grp.getgrall() if self.resource.username in g.gr_mem]

    with open('/etc/group', 'rb') as fp:
      content = fp.read()

    # Each line should have 4 parts, even with no members (trailing colon)
    # group-name:group-password:group-id:
    # group-name:group-password:group-id:group-members
    groups = []
    for line in content.splitlines():
      entries = line.split(':')

      # attempt to parse the users in the group only if there are 4 parts
      if(len(entries) >= 4):
        group_name = entries[0].strip()
        group_users = entries[3].split(',')
        if self.user in group_users:
          groups.append(group_name)

    return groups

class GroupProvider(Provider):
  options = dict(
    gid=(lambda self: self.group.gr_gid, "-g"),
    password=(lambda self: self.group.gr_passwd, "-p")
  )
  def action_create(self):
    group = self.group
    if not group:
      command = ['groupadd']
      Logger.info("Adding group %s" % self.resource)
    else:
      command = ['groupmod']
      
      for option_name, attributes in self.options.iteritems():
        if getattr(self.resource, option_name) != None and getattr(self.resource, option_name) != attributes[0](self):
          break
      else:
        return
      
      Logger.info("Modifying group %s" % (self.resource.group_name))

    for option_name, attributes in self.options.iteritems():
      option_value = getattr(self.resource, option_name)
      if attributes[1] and option_value:
        command += [attributes[1], str(option_value)]
        
    command.append(self.resource.group_name)

    # if trying to modify existing group, but no values to modify are provided
    if self.group and len(command) == 1:
      return
    
    shell.checked_call(command, sudo=True)

  def action_remove(self):
    if self.group:
      command = ['groupdel', self.resource.group_name]
      shell.checked_call(command, sudo=True)
      Logger.info("Removed group %s" % self.resource)

  @property
  def group(self):
    try:
      return grp.getgrnam(self.resource.group_name)
    except KeyError:
      return None
