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

_MAX_SUBST = 20

def substitute_vars(raw, config):
  """
  @param raw: str (e.g '${hbase.tmp.dir}/local')
  @param config: dict (e.g {'hbase.tmp.dir': '/hadoop/hbase'})
  """
  result = raw

  pattern = re.compile("\$\{[^\}\$\x0020]+\}")

  for depth in range(0, _MAX_SUBST - 1):
    match = pattern.search(result)

    if match:
      start = match.start()
      end = match.end()

      name = result[start + 2 : end - 1]

      try:
        value = config[name]
      except KeyError:
        return result

      result = result[:start] + value + result[end:]
    else:
      break

  return result
