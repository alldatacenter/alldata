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
 This class should be used to compare mpack and stack versions.
 Base method which should be used is parse/parse_stack_version, depends
 on which versions you want to compare. This method will validate and parse
 version which you will pass as parameter, and return object of current class with
 parsed version. Same thing you should do with another version, with which you are
 planning to compare previous one. After that, use "==", ">", "<" to get final result.
"""
class MpackVersion(object):
  __mpack_version_pattern = "(?P<major>[0-9]+).(?P<minor>[0-9]+).(?P<maint>[0-9]+)(-h(?P<hotfix>[0-9]+))*-b(?P<build>[0-9]+)"
  __mpack_legacy_stack_version_pattern = "(?P<major>[0-9]+).(?P<minor>[0-9]+).(?P<maint>[0-9]+).(?P<hotfix>[0-9]+)(-(?P<build>[0-9]+))"
  __mpack_version_regex = re.compile(__mpack_version_pattern)
  __mpack_legacy_stack_version_regex = re.compile(__mpack_legacy_stack_version_pattern)

  def __init__(self, major, minor, maint, hotfix, build):
    """
    :type major int
    :type minor int
    :type maint int
    :type hotfix int
    :type build int
    """
    self.__major = int(major)
    self.__minor = int(minor)
    self.__maint = int(maint)
    self.__hotfix = int(hotfix) if hotfix else 0  # hotfix is optional group
    self.__build = int(build)

  def __repr__(self):
    return "{0}.{1}.{2}-h{3}-b{4}".format(*self.to_list())

  def to_list(self):
    """
    Return version elements as list

    :rtype list
    """
    return [
      self.__major,
      self.__minor,
      self.__maint,
      self.__hotfix,
      self.__build
    ]

  def __cmp__(self, other):
    """
    :type other MpackVersion

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
  def parse(cls, mpack_version):
    """
      Parse string to mpack version

      :type mpack_version str
      :rtype MpackVersion
      """
    matcher = cls.validate(mpack_version)
    return MpackVersion(
      matcher.group("major"),
      matcher.group("minor"),
      matcher.group("maint"),
      matcher.group("hotfix"),
      matcher.group("build")
    )


  @classmethod
  def parse_stack_version(cls, stack_version):
    """
      Parse string to mpack version

      :type stack_version str
      :rtype MpackVersion
      """
    matcher = cls.validate_stack_version(stack_version)
    return MpackVersion(
      matcher.group("major"),
      matcher.group("minor"),
      matcher.group("maint"),
      matcher.group("hotfix"),
      matcher.group("build")
    )


  @classmethod
  def validate_stack_version(cls, stack_version):
    """
    Check if provided version is valid. If version is valid will return match object
    or will raise exception.

    :param stack_version version to check
    :type stack_version str

    :rtype __Match[T] | None

    :raise ValueError
    """

    if not stack_version:
      raise ValueError("Module version can't be empty or null")

    version = stack_version.strip()

    if not version:
      raise ValueError("Module version can't be empty or null")

    matcher = cls.__mpack_version_regex.match(version)

    if not matcher:
      matcher = cls.__mpack_legacy_stack_version_regex.match(version)
      if not matcher:
        raise ValueError("{0} is not a valid {1}".format(version, cls.__name__))
    else:
      if not matcher.group("hotfix"):
        raise ValueError("{0} is not a valid {1}".format(version, cls.__name__))

    return matcher


  @classmethod
  def validate(cls, mpack_version):
    """
    Check if provided version is valid. If version is valid will return match object
    or will raise exception.

    :param module_version version to check
    :type module_version str

    :rtype __Match[T] | None

    :raise ValueError
    """

    if not mpack_version:
      raise ValueError("Module version can't be empty or null")

    version = mpack_version.strip()

    if not version:
      raise ValueError("Module version can't be empty or null")

    matcher = cls.__mpack_version_regex.match(version)

    if not matcher:
      raise ValueError("{0} is not a valid {1}".format(version, cls.__name__))

    return matcher

  @property
  def major(self):
    return self.__major

  @property
  def minor(self):
    return self.__minor

  @property
  def maint(self):
    return self.__maint

  @property
  def hotfix(self):
    return self.__hotfix

  @property
  def build(self):
    return self.__build



