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

def hcat_service_check():
    import params

    unique = get_unique_id_and_date()
    output_file = format("/apps/hive/warehouse/hcatsmoke{unique}")
    test_cmd = format("fs -test -e {output_file}")

    if params.security_enabled:
      kinit_cmd = format(
        "{kinit_path_local} -kt {smoke_user_keytab} {smokeuser}; ")
    else:
      kinit_cmd = ""

    File('/tmp/hcatSmoke.sh',
         content=StaticFile("hcatSmoke.sh"),
         mode=0755
    )

    prepare_cmd = format("{kinit_cmd}sh /tmp/hcatSmoke.sh hcatsmoke{unique} prepare")

    Execute(prepare_cmd,
            tries=3,
            user=params.smokeuser,
            try_sleep=5,
            path=['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
            logoutput=True)

    ExecuteHadoop(test_cmd,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir)

    cleanup_cmd = format("{kinit_cmd}sh /tmp/hcatSmoke.sh hcatsmoke{unique} cleanup")

    Execute(cleanup_cmd,
            tries=3,
            user=params.smokeuser,
            try_sleep=5,
            path=['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin'],
            logoutput=True
    )
