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
from shared_initialization import *

class BeforeAnyHook(Hook):

  def hook(self, env):
    import params
    env.set_params(params)

    setup_users()
    if params.has_namenode or params.dfs_type == 'HCFS':
      setup_hadoop_env()
    setup_java()

if __name__ == "__main__":
  BeforeAnyHook().execute()

