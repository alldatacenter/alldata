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

"""

from resource_management import *

from hcat_service_check import hcat_service_check

class HiveServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)
    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser};")
      hive_principal_ext = format("principal={hive_metastore_keytab_path}")
      hive_url_ext = format("{hive_url}/\\;{hive_principal_ext}")
      smoke_cmd = format("{kinit_cmd} env JAVA_HOME={java64_home} {smoke_test_path} {hive_url_ext} {smoke_test_sql}")
    else:
      smoke_cmd = format("env JAVA_HOME={java64_home} {smoke_test_path} {hive_url} {smoke_test_sql}")

    File(params.smoke_test_path,
         content=StaticFile('hiveserver2Smoke.sh'),
         mode=0755
    )

    File(params.smoke_test_sql,
         content=StaticFile('hiveserver2.sql')
    )

    Execute(smoke_cmd,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            logoutput=True,
            user=params.smokeuser)

    hcat_service_check()

if __name__ == "__main__":
  HiveServiceCheck().execute()
