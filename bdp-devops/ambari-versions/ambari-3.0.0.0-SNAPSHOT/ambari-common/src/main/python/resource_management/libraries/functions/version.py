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
import re

def _normalize(v, desired_segments=0):
  """
  :param v: Input string of the form "#.#.#" or "#.#.#.#"
  :param desired_segments: If greater than 0, and if v has fewer segments this parameter, will pad v with segments
  containing "0" until the desired segments is reached.
  :return: Returns a list of integers representing the segments of the version
  """
  v_list = v.split(".")
  if desired_segments > 0 and len(v_list) < desired_segments:
    v_list = v_list + ((desired_segments - len(v_list)) * ["0", ])
  return [int(x) for x in v_list]


def format_stack_version(value):
  """
  :param value: Input string, e.g. "2.2" or "GlusterFS", or "2.0.6.GlusterFS", or "2.2.0.1-885"
  :return: Returns a well-formatted HDP stack version of the form #.#.#.# as a string.
  """
  if value:
    if "-" in value:
      first_occurrence = value.find("-")
      last_occurence = value.rfind("-")

      if first_occurrence == last_occurence:
        if value[0].isalpha():
          value = value[first_occurrence + 1:]
        else:
          value = value[:first_occurrence]
      else:
        value = value[first_occurrence + 1:last_occurence]

    value = re.sub(r'^\D+', '', value)
    value = re.sub(r'\D+$', '', value)
    value = value.strip('.')

    strip_dots = value.replace('.', '')
    if strip_dots.isdigit():
      normalized = _normalize(str(value))
      if len(normalized) == 2:
        normalized = normalized + [0, 0]
      elif len(normalized) == 3:
        normalized = normalized + [0, ]
      normalized = [str(x) for x in normalized]   # need to convert each number into a string
      return ".".join(normalized)
  return ""


def compare_versions(version1, version2, format=False):
  """
  Used to compare either Ambari Versions, or Stack versions
  E.g., Ambari version 1.6.1 vs 1.7.0,
  Stack Version 2.0.6.0 vs 2.2.0.0
  :param version1: First parameter for version
  :param version2: Second parameter for version
  :param format: optionally format the versions via format_stack_version before comparing them
  :return: Returns -1 if version1 is before version2, 0 if they are equal, and 1 if version1 is after version2
  """
  v1 = version1 if not format else format_stack_version(version1)
  v2 = version2 if not format else format_stack_version(version2)

  max_segments = max(len(v1.split(".")), len(v2.split(".")))
  return cmp(_normalize(v1, desired_segments=max_segments), _normalize(v2, desired_segments=max_segments))


def get_major_version(full_version):
  """
  :param input: Input string, e.g. "2.1.3.0" or "2.2.0.1-885" and other similar
  :return: Returns a well-formatted HDP major stack version of the form #.# as a string. If can't get major version
  return None
  """
  pattern = re.compile(r'^[0-9]+\.[0-9]+')
  major_version = None

  m = pattern.search(full_version)
  if m:
    major_version = m.group()

  return major_version


def get_current_component_version():
  """
  Returns best available version for the component at different stages (install, start, stop)

  :return best matched version or None
  :rtype str|None
  """
  from resource_management.core.exceptions import Fail
  from resource_management.libraries.functions.default import default
  from resource_management.libraries.functions.stack_select import get_role_component_current_stack_version
  from resource_management.libraries.functions.repository_util import CommandRepository

  version = default("/commandParams/version", None)
  if not version:
    repository = CommandRepository(default("/repositoryFile", {}))
    if not repository.resolved:
      try:
        version = get_role_component_current_stack_version()
      except (Fail, TypeError):
        pass
    else:
      version = repository.version_string

  return version
