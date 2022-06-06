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

__all__ = ["System"]

import os
import sys
import platform
from resource_management.core import shell
from resource_management.core.utils import lazy_property
from resource_management.core.exceptions import Fail
from ambari_commons import OSCheck

class System(object):
  @lazy_property
  def os(self):
    """
    Return values:
    linux, unknown
    
    In case cannot detect raises 'unknown'
    """
    platform = sys.platform
    if platform.startswith('linux'):
      return "linux"
    else:
      return "unknown"
    
  @lazy_property
  def os_version(self):
    """
    Example return value:
    "6.3" for "Centos 6.3"
    
    In case cannot detect --> Fail
    """
    return OSCheck.get_os_version()

  @lazy_property
  def os_major_version(self):
    """
    Example return value:
    "6" for "Centos 6.3"

    In case cannot detect --> Fail
    """
    return OSCheck.get_os_major_version()
  
  @lazy_property
  def os_release_name(self):
    """
    For Ubuntu 12.04:
    precise
    """
    return OSCheck.get_os_release_name()
                       
  @lazy_property
  def os_type(self):
    """
    Return values:
    redhat, fedora, centos, oraclelinux, ascendos,
    amazon, xenserver, oel, ovs, cloudlinux, slc, scientific, psbm,
    debian, ubuntu, sles, sled, opensuse, suse ... and others
    
    In case cannot detect raises exception.
    """
    return OSCheck.get_os_type()
    
  @lazy_property
  def os_family(self):
    """
    Return values:
    redhat, ubuntu, suse
    
    In case cannot detect raises exception
    """
    return OSCheck.get_os_family()

  @lazy_property
  def ec2(self):
    if not os.path.exists("/proc/xen"):
      return False
    if os.path.exists("/etc/ec2_version"):
      return True
    return False

  @lazy_property
  def vm(self):
    if os.path.exists("/usr/bin/VBoxControl"):
      return "vbox"
    elif os.path.exists("/usr/bin/vmware-toolbox-cmd") or os.path.exists(
      "/usr/sbin/vmware-toolbox-cmd"):
      return "vmware"
    elif os.path.exists("/proc/xen"):
      return "xen"
    return None
  
  @lazy_property
  def arch(self):
    machine = self.machine
    if machine in ("i386", "i486", "i686"):
      return "x86_32"
    return machine

  @lazy_property
  def machine(self):
    code, out = shell.call(["/bin/uname", "-m"])
    return out.strip()

  @lazy_property
  def locales(self):
    code, out = shell.call("locale -a")
    return out.strip().split("\n")

  @classmethod
  def get_instance(cls):
    try:
      return cls._instance
    except AttributeError:
      cls._instance = cls()
    return cls._instance
  
  def unquote(self, val):
    if val[0] == '"':
      val = val[1:-1]
    return val
