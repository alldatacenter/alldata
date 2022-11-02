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

import os

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import InlineTemplate, StaticFile
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


class MicrosoftRServerServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class MicrosoftRServerServiceCheckLinux(MicrosoftRServerServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    try:
      params.HdfsResource(params.revo_share_hdfs_folder,
                          type="directory",
                          action="create_on_execute",
                          owner=params.hdfs_user,
                          mode=0777)
      params.HdfsResource(None, action="execute")
    except Exception as exception:
        Logger.warning("Could not check the existence of /user/RevoShare on HDFS, exception: {0}".format(str(exception)))

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
      Execute(kinit_cmd,
        user=params.smokeuser
      )

    output_file = format('{tmp_dir}/microsoft_r_server_serviceCheck.out')

    File( format("{tmp_dir}/microsoft_r_server_serviceCheck.r"),
      content = StaticFile("microsoft_r_server_serviceCheck.r"),
      mode = 0755
    )

    Execute( format("Revo64 --no-save  < {tmp_dir}/microsoft_r_server_serviceCheck.r | tee {output_file}"),
      tries     = 1,
      try_sleep = 1,
      path      = format('/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'),
      user      = params.smokeuser,
      logoutput = True
    )

    # Verify correct output
    from resource_management.core import sudo
    output_content = sudo.read_file(format('{output_file}'))
    import re
    values_list = re.findall(r"\s(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\s+(\d*)", output_content)
    if 7 != len(values_list):
      Logger.info("Unable to verify output of service check run")
      raise Fail("Unable to verify output of service check run")
    dayCountDictionary = {'Monday': '97975', 'Tuesday': '77725', 'Wednesday': '78875', 'Thursday': '81304', 'Friday': '82987', 'Saturday': '86159', 'Sunday': '94975'}
    for (day, count) in values_list:
      if count != dayCountDictionary[day]:
        Logger.info("Service check produced incorrect output for {0}. Was expecting {1} but encountered {2}".format(day, dayCountDictionary[day], count))
        raise Fail("Service check produced incorrect output for {0}. Was expecting {1} but encountered {2}".format(day, dayCountDictionary[day], count))


if __name__ == "__main__":
  MicrosoftRServerServiceCheck().execute()
