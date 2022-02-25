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
from resource_management.libraries.functions import format
from resource_management import File, StaticFile
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.libraries.script import Script
from resource_management.libraries.resources.xml_config import XmlConfig

class OneFsClient(Script):

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    self.setup_config(env)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def setup_config(self, env):
    import params
    env.set_params(params)
    XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.hdfs_user,
            group=params.user_group
    )
    XmlConfig("core-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['core-site'],
            configuration_attributes=params.config['configuration_attributes']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0644
    )

    File(format("{params.ambari_libs_dir}/fast-hdfs-resource.jar"),
         mode=0644,
         content=StaticFile("/var/lib/ambari-agent/cache/stack-hooks/before-START/files/fast-hdfs-resource.jar")
         )

if __name__ == "__main__":
  OneFsClient().execute()

