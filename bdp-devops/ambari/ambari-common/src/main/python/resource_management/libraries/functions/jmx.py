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
import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions.get_user_call_output import get_user_call_output

def get_value_from_jmx(qry, property, security_enabled, run_user, is_https_enabled, last_retry=True):
  try:
    if security_enabled:
      cmd = ['curl', '--negotiate', '-u', ':', '-s']
    else:
      cmd = ['curl', '-s']

    if is_https_enabled:
      cmd.append("-k")

    cmd.append(qry)

    _, data, _ = get_user_call_output(cmd, user=run_user, quiet=False)

    if data:
      data_dict = json.loads(data)
      return data_dict["beans"][0][property]
  except:
    if last_retry:
      Logger.logger.exception("Getting jmx metrics from NN failed. URL: " + str(qry))
    return None
