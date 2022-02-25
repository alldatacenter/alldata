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

import re

"""
 This class should be used to compare module(service) versions.
 Base method which should be used is parse(..), This method will validate and parse
 version which you will pass as parameter, and return object of current class with
 parsed version. Same thing you should do with another version, with which you are
 planning to compare previous one. After that, use "==", "<", ">" to get final result.
"""
class ModuleVersion(object):
  __module_version_pattern = "(?P<aMajor>[0-9]+).(?P<aMinor>[0-9]+).(?P<iMinor>[0-9]+).(?P<iMaint>[0-9]+)(-h(?P<hotfix>[0-9]+))*-b(?P<build>[0-9]+)"
  __module_version_regex = re.compile(__module_version_pattern)

  def __init__(self, apache_major, apache_minor, internal_minor, internal_maint, hotfix, build):
    """
    :type apache_major int
    :type apache_minor int
    :type internal_maint int
    :type internal_minor int
    :type hotfix int
    :type build int
    """
    self.__apache_major = int(apache_major)
    self.__apache_minor = int(apache_minor)
    self.__internal_maint = int(internal_maint)
    self.__internal_minor = int(internal_minor)
    self.__hotfix = int(hotfix) if hotfix else 0  # hotfix is optional group
    self.__build = int(build)

  def __repr__(self):
    return "{0}.{1}.{2}.{3}-h{4}-b{5}".format(*self.to_list())

  def to_list(self):
    """
    Return version elements as list

    :rtype list
    """
    return [
      self.__apache_major,
      self.__apache_minor,
      self.__internal_minor,
      self.__internal_maint,
      self.__hotfix,
      self.__build
    ]

  def __cmp__(self, other):
    """
    :type other ModuleVersion

    :raise TypeError
    """
    if other and not isinstance(other, self.__class__):
      raise TypeError("Operand type is different from {0}".format(self.__class__.__name__))

    r = 0
    x = self.to_list()
    y = other.to_list()

    for i in range(0, len(x)):
      r = x[i] - y[i]
      if r != 0:
        break

    return 1 if r > 0 else -1 if r < 0 else 0

  @classmethod
  def parse(cls, module_version):
    """
      Parse string to module version

      :type module_version str
      :rtype ModuleVersion
      """
    matcher = cls.validate(module_version)
    return ModuleVersion(
      matcher.group("aMajor"),
      matcher.group("aMinor"),
      matcher.group("iMinor"),
      matcher.group("iMaint"),
      matcher.group("hotfix"),
      matcher.group("build")
    )

  @classmethod
  def validate(cls, module_version):
    """
    Check if provided version is valid. If version is valid will return match object
    or will raise exception.

    :param module_version version to check
    :type module_version str

    :rtype __Match[T] | None

    :raise ValueError
    """

    if not module_version:
      raise ValueError("Module version can't be empty or null")

    version = module_version.strip()

    if not version:
      raise ValueError("Module version can't be empty or null")

    matcher = cls.__module_version_regex.match(version)

    if not matcher:
      raise ValueError("{0} is not a valid {1}".format(version, cls.__name__))

    return matcher

  @property
  def apache_major(self):
    return self.__apache_major

  @property
  def apache_minor(self):
    return self.__apache_minor

  @property
  def internal_minor(self):
    return self.__internal_minor

  @property
  def internal_maint(self):
    return self.__internal_maint

  @property
  def hotfix(self):
    return self.__hotfix

  @property
  def build(self):
    return self.__build



