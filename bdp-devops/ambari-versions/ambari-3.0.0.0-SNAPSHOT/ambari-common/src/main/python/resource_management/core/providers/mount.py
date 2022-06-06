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

import os
import re
from ambari_commons.subprocess32 import Popen, PIPE, STDOUT

from resource_management.core.base import Fail
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger


def get_mounted():
  """
  :return: Return a list of mount objects (dictionary type) that contain the device, mount point, and other options.
  """
  p = Popen("mount", stdout=PIPE, stderr=STDOUT, shell=True)
  out = p.communicate()[0]
  if p.wait() != 0:
    raise Fail("Getting list of mounts (calling mount) failed")

  mounts = [x.split(' ') for x in out.strip().split('\n')]

  results = []
  for m in mounts:
    # Example of m:
    # /dev/sda1 on / type ext4 (rw,barrier=0)
    # /dev/sdb on /grid/0 type ext4 (rw,discard)
    if len(m) >= 6 and m[1] == "on" and m[3] == "type":
      x = dict(
        device=m[0],
        mount_point=m[2],
        fstype=m[4],
        options=m[5][1:-1].split(',') if len(m[5]) >= 2 else []
      )
      results.append(x)

  return results


def get_fstab(self):
  """
  :return: Return a list of objects (dictionary type) representing the file systems table.
  """
  mounts = []
  with open("/etc/fstab", "r") as fp:
    for line in fp:
      line = line.split('#', 1)[0].strip()
      mount = re.split('\s+', line)
      if len(mount) == 6:
        mounts.append(dict(
          device=mount[0],
          mount_point=mount[1],
          fstype=mount[2],
          options=mount[3].split(","),
          dump=int(mount[4]),
          passno=int(mount[5]),
          ))
  return mounts


class MountProvider(Provider):
  def action_mount(self):
    if not os.path.exists(self.resource.mount_point):
      os.makedirs(self.resource.mount_point)

    if self.is_mounted():
      Logger.debug("%s already mounted" % self)
    else:
      args = ["mount"]
      if self.resource.fstype:
        args += ["-t", self.resource.fstype]
      if self.resource.options:
        args += ["-o", ",".join(self.resource.options)]
      if self.resource.device:
        args.append(self.resource.device)
      args.append(self.resource.mount_point)

      check_call(args)

      Logger.info("%s mounted" % self)

  def action_umount(self):
    if self.is_mounted():
      check_call(["umount", self.resource.mount_point])

      Logger.info("%s unmounted" % self)
    else:
      Logger.debug("%s is not mounted" % self)

  def action_enable(self):
    if self.is_enabled():
      Logger.debug("%s already enabled" % self)
    else:
      if not self.resource.device:
        raise Fail("[%s] device not set but required for enable action" % self)
      if not self.resource.fstype:
        raise Fail("[%s] fstype not set but required for enable action" % self)

      with open("/etc/fstab", "a") as fp:
        fp.write("%s %s %s %s %d %d\n" % (
          self.resource.device,
          self.resource.mount_point,
          self.resource.fstype,
          ",".join(self.resource.options or ["defaults"]),
          self.resource.dump,
          self.resource.passno,
        ))

      Logger.info("%s enabled" % self)

  def action_disable(self):
    pass # TODO

  def is_mounted(self):
    if not os.path.exists(self.resource.mount_point):
      return False

    if self.resource.device and not os.path.exists(self.resource.device):
      raise Fail("%s Device %s does not exist" % (self, self.resource.device))

    mounts = get_mounted()
    for m in mounts:
      if m['mount_point'] == self.resource.mount_point:
        return True

    return False

  def is_enabled(self):
    mounts = get_fstab()
    for m in mounts:
      if m['mount_point'] == self.resource.mount_point:
        return True

    return False

