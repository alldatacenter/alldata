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

from ambari_commons.os_check import OSCheck
from resource_management.libraries.resources.repository import Repository
from resource_management.libraries.functions.repository_util import CommandRepository, UBUNTU_REPO_COMPONENTS_POSTFIX
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
import ambari_simplejson as json


def _alter_repo(action, repo_dicts, repo_template):
  """
  @param action: "delete" or "create"
  @param repo_dicts: e.g. "[{\"baseUrl\":\"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0\",\"osType\":\"centos6\",\"repoId\":\"HDP-2.0._\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0\"}]"
  """
  if not isinstance(repo_dicts, list):
    repo_dicts = [repo_dicts]

  if 0 == len(repo_dicts):
    Logger.info("Repository list is empty. Ambari may not be managing the repositories.")
  else:
    Logger.info("Initializing {0} repositories".format(str(len(repo_dicts))))

  for repo in repo_dicts:
    if not 'baseUrl' in repo:
      repo['baseUrl'] = None
    if not 'mirrorsList' in repo:
      repo['mirrorsList'] = None

    ubuntu_components = [ repo['distribution'] if 'distribution' in repo and repo['distribution'] else repo['repoName'] ] \
                        + [repo['components'].replace(",", " ") if 'components' in repo and repo['components'] else UBUNTU_REPO_COMPONENTS_POSTFIX]

    Repository(repo['repoId'],
               action = "prepare",
               base_url = repo['baseUrl'],
               mirror_list = repo['mirrorsList'],
               repo_file_name = repo['repoName'],
               repo_template = repo_template,
               components = ubuntu_components) # ubuntu specific

  Repository(None, action = "create")


def install_repos():
  import params
  if params.host_sys_prepped:
    return

  # use this newer way of specifying repositories, if available
  if params.repo_file is not None:
    Script.repository_util.create_repo_files()
    return

  template = params.repo_rhel_suse if OSCheck.is_suse_family() or OSCheck.is_redhat_family() else params.repo_ubuntu

  _alter_repo("create", params.repo_info, template)

  if params.service_repo_info:
    _alter_repo("create", params.service_repo_info, template)
