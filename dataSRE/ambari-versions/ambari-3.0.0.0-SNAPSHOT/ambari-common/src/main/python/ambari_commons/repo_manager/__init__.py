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
__all__ = ["ManagerFactory", "GenericManager"]

import threading
from ambari_commons import OSCheck, OSConst

from .generic_manager import GenericManager
from .apt_manager import AptManager
from .yum_manager import YumManager
from .zypper_manager import ZypperManager
from .choco_manager import ChocoManager


class ManagerFactory(object):
  __lock = threading.Lock()

  __repo_manager = None
  """:type GenericManager"""

  @classmethod
  def get(cls):
    """
    Return Repository Manager object for current OS in safe manner.

    :rtype GenericManager
    """
    if not cls.__repo_manager:
      with cls.__lock:
        cls.__repo_manager = cls.get_new_instance(OSCheck.get_os_family())

    return cls.__repo_manager

  @classmethod
  def get_new_instance(cls, os_family=None):
    """
    Construct new instance of Repository Manager object. Call is not thread-safe

    :param os_family:  os family string; best used in combination with `OSCheck.get_os_family()`
    :type os_family str
    :rtype GenericManager
    """
    if not os_family:
      os_family = OSCheck.get_os_family()

    if OSCheck.is_in_family(os_family, OSConst.UBUNTU_FAMILY):
      return AptManager()
    if OSCheck.is_in_family(os_family, OSConst.SUSE_FAMILY):
      return ZypperManager()
    if OSCheck.is_in_family(os_family, OSConst.REDHAT_FAMILY):
      return YumManager()
    if OSCheck.is_in_family(os_family, OSConst.WINSRV_FAMILY):
      return ChocoManager()

    raise RuntimeError("Not able to create Repository Manager object for unsupported OS family {0}".format(os_family))