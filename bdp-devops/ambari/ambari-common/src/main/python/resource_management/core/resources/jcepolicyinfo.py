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

from resource_management.core import shell
from ambari_commons import subprocess32


class JcePolicyInfo:
  def __init__(self, java_home):
    # ToDo: change hardcoded path to resolved one
    self.command_format = java_home + "/bin/java -jar /var/lib/ambari-agent/tools/jcepolicyinfo.jar {}"
    self.jar = "/var/lib/ambari-agent/tools/jcepolicyinfo.jar"

  def is_unlimited_key_jce_policy(self):
    ret = shell.call(
      self.command_format.format('-tu'), stdout=subprocess32.PIPE, stderr=subprocess32.PIPE, timeout=5, quiet=True)[0]
    return ret == 0
