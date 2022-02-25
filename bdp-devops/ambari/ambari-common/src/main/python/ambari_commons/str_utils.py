#!/usr/bin/env python

'''
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
'''


def compress_backslashes(s):
  s1 = s
  while (-1 != s1.find('\\\\')):
    s1 = s1.replace('\\\\', '\\')
  return s1


def ensure_double_backslashes(s):
  s1 = compress_backslashes(s)
  s2 = s1.replace('\\', '\\\\')
  return s2


def cbool(obj):
  """
  Interprets an object as a boolean value.

  :rtype: bool
  """
  if isinstance(obj, str):
    obj = obj.strip().lower()
    if obj in ('true', 'yes', 'on', 'y', 't', '1'):
      return True
    if obj in ('false', 'no', 'off', 'n', 'f', '0'):
      return False
    raise ValueError('Unable to interpret value "%s" as boolean' % obj)
  return bool(obj)


def cint(obj):
  """
  Interprets an object as a integer value.
  :param obj:
  :return:
  """
  if isinstance(obj, str):
    obj = obj.strip().lower()
    try:
      return int(obj)
    except ValueError:
      raise ValueError('Unable to interpret value "%s" as integer' % obj)
  elif obj is None:
    return obj

  return int(obj)

def split_on_chunks(text, chunk_max_size):
  """
  This function splits text on a chunks of size ~ chunk_max_size.
  Function tries not to split a single line between multiple chunks.
  This will only in one case when len(line) > chunk_max_size.
  """
  lines = text.splitlines()
  chunks = []
  chunk = ""
  while len(lines) != 0:
    if len(chunk) + len(lines[0]) > chunk_max_size:
      chunks.append(chunk[:-1])
      chunk = ""
      # if a single line itself is bigger than the max chunk size, split the line.
      if len(lines[0]) > chunk_max_size:
        line_fragments = [lines[0][x:x+chunk_max_size] for x in xrange(0, len(lines[0]), chunk_max_size)]
        for line_fragment in line_fragments:
          chunks.append(line_fragment)
        lines.pop(0)
    else:
      chunk += lines.pop(0) + "\n"
  chunks.append(chunk[:-1])
  return chunks


def string_set_intersection(set_a, set_b, ignore_case=True, sep=","):
  """
  Return intersection of two coma-separated sets

  :type set_a str
  :type set_b str
  :type ignore_case bool
  :type sep str

  :rtype set
  """
  if set_a is None or set_b is None:
    return set()

  if ignore_case:
    set_a = set_a.lower()
    set_b = set_b.lower()

  set_a = set(set_a.split(sep))
  set_b = set(set_b.split(sep))

  return set_a & set_b


def string_set_equals(set_a, set_b, ignore_case=True, sep=","):
  """
  Return True or False based on result of comparison of two string sets

  :type set_a str
  :type set_b str
  :type ignore_case bool
  :type sep str

  :rtype bool
  """
  if set_a is None or set_b is None:
    return False

  if ignore_case:
    set_a = set_a.lower()
    set_b = set_b.lower()

  set_a = set(set_a.split(sep))
  set_b = set(set_b.split(sep))

  return len(set_b) == len(set_a) == len(set_a & set_b)
