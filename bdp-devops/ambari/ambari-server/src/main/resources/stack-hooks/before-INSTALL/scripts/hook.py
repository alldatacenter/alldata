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
from resource_management import Hook
from shared_initialization import install_packages
from repo_initialization import install_repos


class BeforeInstallHook(Hook):

  def hook(self, env):
    import params

    self.run_custom_hook('before-ANY')
    env.set_params(params)
    
    install_repos()
    install_packages()


if __name__ == "__main__":
  BeforeInstallHook().execute()
