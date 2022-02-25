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

# basic base output cleanup patterns
ANSI_ESCAPE = re.compile(r'\x1B\[[0-?]*[ -/]*[@-~]')  # exclude bash control sequences
EXCLUDE_CHARS = [ord("\r"), ord("\n"), ord("\t")]
REMOVE_CHARS = "".join([chr(i) for i in range(0, 31) if i not in EXCLUDE_CHARS])  # exclude control characters


class GenericParser(object):
  """
   Base for the any custom parser. New parser rules:
   - no subprocess calls
   - as input should be provided iterable text
   - result should be returned as ready to consume object, preferably as generator

   Samples available for zypper, yum, apt
  """

  @staticmethod
  def config_reader(stream):
    """
    :type stream collections.Iterable
    :rtype collections.Iterable
    :return tuple(key, value)
    """
    raise NotImplementedError()

  @staticmethod
  def packages_reader(stream):
    """
    :type stream collections.Iterable
    :rtype collections.Iterable
    :return tuple(package name, version, parsed repo list file)
    """
    raise NotImplementedError()

  @staticmethod
  def packages_installed_reader(stream):
    """
      :type stream collections.Iterable
      :rtype collections.Iterable
      :return tuple(package name, version)
    """
    raise NotImplementedError()
