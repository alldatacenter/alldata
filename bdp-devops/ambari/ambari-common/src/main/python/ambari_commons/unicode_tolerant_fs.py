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

def get_encoded_string(data):
  try:
    return data.encode("utf8")
  except UnicodeDecodeError:
    return data

def unicode_walk(top, topdown=True, onerror=None, followlinks=False):
  """
  Unicode tolerant version of os.walk. Can(and must) be used environments with messed locales(and other encoding-related
  problems) to traverse directories trees with unicode names in files and directories. All others function seems like
  to accept utf-8 encoded strings, so result of `unicode_walk` can be used without a problems.
  """
  import os.path
  import os

  islink, join, isdir = os.path.islink, os.path.join, os.path.isdir

  top = get_encoded_string(top)

  try:
    # Note that listdir and error are globals in this module due
    # to earlier import-*.
    names = os.listdir(top)
  except os.error, err:
    if onerror is not None:
      onerror(err)
    return

  dirs, nondirs = [], []
  for name in names:
    name = get_encoded_string(name)
    if isdir(join(top, name)):
      dirs.append(name)
    else:
      nondirs.append(name)

  if topdown:
    yield top, dirs, nondirs
  for name in dirs:
    name = get_encoded_string(name)
    new_path = join(top, name)
    if followlinks or not islink(new_path):
      for x in unicode_walk(new_path, topdown, onerror, followlinks):
        yield x
  if not topdown:
    yield top, dirs, nondirs
