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
from ambari_commons.os_family_impl import OsFamilyImpl

class HdfsServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HdfsServiceCheckDefault(HdfsServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique = functions.get_unique_id_and_date()
    dir = params.hdfs_tmp_dir
    tmp_file = format("{dir}/{unique}")

    if params.security_enabled:
      Execute(format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}"),
        user=params.hdfs_user
      )
    params.HdfsResource(dir,
                        type="directory",
                        action="create_on_execute",
                        mode=0777
    )
    params.HdfsResource(tmp_file,
                        type="file",
                        action="delete_on_execute",
    )

    params.HdfsResource(tmp_file,
                        type="file",
                        source="/etc/passwd",
                        action="create_on_execute"
    )
    params.HdfsResource(None, action="execute")


if __name__ == "__main__":
  HdfsServiceCheck().execute()
