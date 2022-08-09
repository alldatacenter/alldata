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
from ambari_commons.shell import RepoCallContext
from resource_management import Provider
from ambari_commons.repo_manager import ManagerFactory


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)
    self._pkg_manager = ManagerFactory.get()

  def action_install(self):
    package_name = self.get_package_name_with_version()
    self._pkg_manager.install_package(package_name, self.__create_context())

  def action_upgrade(self):
    package_name = self.get_package_name_with_version()
    self._pkg_manager.upgrade_package(package_name, self.__create_context())

  def action_remove(self):
    package_name = self.get_package_name_with_version()
    self._pkg_manager.remove_package(package_name, self.__create_context())

  def __create_context(self):
    """
    Build RepoCallContext from Resource properties
    """
    return RepoCallContext(
      ignore_errors=self.resource.ignore_failures,
      use_repos=self.resource.use_repos,
      skip_repos=self.resource.skip_repos,
      log_output=True if self.resource.logoutput is None or self.resource.logoutput is True else False,
      retry_count=self.resource.retry_count,
      retry_sleep=self.resource.retry_sleep,
      retry_on_repo_unavailability=self.resource.retry_on_repo_unavailability,
      retry_on_locked=self.resource.retry_on_locked
    )

  def get_package_name_with_version(self):
    if self.resource.version:
      return self.resource.package_name + '-' + self.resource.version
    else:
      return self.resource.package_name

