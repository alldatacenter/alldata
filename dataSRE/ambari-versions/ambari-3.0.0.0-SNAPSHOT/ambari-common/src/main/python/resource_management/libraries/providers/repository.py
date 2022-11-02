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

Ambari Agent

"""

import os
import filecmp
import tempfile
from ambari_commons import OSCheck, os_utils
from resource_management.core.resources import Execute
from resource_management.core.resources import File
from resource_management.core.providers import Provider
from resource_management.core.source import InlineTemplate
from resource_management.core.source import StaticFile
from resource_management.libraries.functions.format import format
from resource_management.core.environment import Environment
from resource_management.core.shell import checked_call, call
from resource_management.core import sudo
from resource_management.core.logger import Logger
from resource_management.core.exceptions import ExecutionFailed
import re
from collections import defaultdict

REPO_TEMPLATE_FOLDER = 'data'

class RepositoryProvider(Provider):
  repo_files_content = defaultdict(lambda:'')

  def action_create(self):
    with tempfile.NamedTemporaryFile() as tmpf:
      with tempfile.NamedTemporaryFile() as old_repo_tmpf:
        for repo_file_path, repo_file_content in RepositoryProvider.repo_files_content.iteritems():
          repo_file_content = repo_file_content.strip()

          File(tmpf.name,
               content=repo_file_content,
               owner=os_utils.current_user(),
          )

          if os.path.isfile(repo_file_path):
            # a copy of old repo file, which will be readable by current user
            File(old_repo_tmpf.name,
                 content=StaticFile(repo_file_path),
                 owner=os_utils.current_user(),
            )

          if not os.path.isfile(repo_file_path) or not filecmp.cmp(tmpf.name, old_repo_tmpf.name):
            Logger.info(format("Rewriting {repo_file_path} since it has changed."))
            File(repo_file_path,
                 content = StaticFile(tmpf.name)
            )

            try:
              self.update(repo_file_path)
            except:
              # remove created file or else ambari will consider that update was successful and skip repository operations
              File(repo_file_path,
                   action = "delete",
              )
              raise
            
    RepositoryProvider.repo_files_content.clear()

class RhelRepositoryProvider(RepositoryProvider):
  def action_prepare(self):
    repo_file_name = self.resource.repo_file_name
    repo_dir = self.get_repo_dir()
    new_content = InlineTemplate(self.resource.repo_template, repo_id=self.resource.repo_id, repo_file_name=self.resource.repo_file_name,
                           base_url=self.resource.base_url, mirror_list=self.resource.mirror_list).get_content() + '\n'
    repo_file_path = format("{repo_dir}/{repo_file_name}.repo")

    RepositoryProvider.repo_files_content[repo_file_path] += new_content
  
  def action_remove(self):
    repo_file_name = self.resource.repo_file_name
    repo_dir = self.get_repo_dir()

    File(format("{repo_dir}/{repo_file_name}.repo"),
         action="delete")
    
  def get_repo_dir(self):
    return '/etc/yum.repos.d'

  def update(self, repo_file_path):
    # Centos will usually update automatically. Don't need to waste deploy time.
    # Also in cases of failure of package install 'yum clean metadata' and retry is ran anyway.
    pass

class SuseRepositoryProvider(RhelRepositoryProvider):
  update_cmd = ['zypper', 'clean', '--all']

  def get_repo_dir(self):
    return '/etc/zypp/repos.d'

  def update(self, repo_file_path):
    Logger.info("Flushing package manager cache since repo file content is about to change")
    checked_call(self.update_cmd, sudo=True)

class UbuntuRepositoryProvider(RepositoryProvider):
  package_type = "deb"
  repo_dir = "/etc/apt/sources.list.d"
  update_cmd = ['apt-get', 'update', '-qq', '-o', 'Dir::Etc::sourcelist=sources.list.d/{repo_file_name}', '-o', 'Dir::Etc::sourceparts=-', '-o', 'APT::Get::List-Cleanup=0']
  missing_pkey_regex = "The following signatures couldn't be verified because the public key is not available: NO_PUBKEY ([A-Z0-9]+)"
  app_pkey_cmd_prefix = ('apt-key', 'adv', '--recv-keys', '--keyserver', 'keyserver.ubuntu.com')

  def action_prepare(self):
    repo_file_name = format("{repo_file_name}.list",repo_file_name=self.resource.repo_file_name)
    repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir=self.repo_dir)

    new_content = InlineTemplate(self.resource.repo_template, package_type=self.package_type,
                                  base_url=self.resource.base_url,
                                  components=' '.join(self.resource.components)).get_content() + '\n'

    RepositoryProvider.repo_files_content[repo_file_path] += new_content
  
  def action_remove(self):
    repo_file_name = format("{repo_file_name}.list", repo_file_name=self.resource.repo_file_name)
    repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir=self.repo_dir)
    
    if os.path.isfile(repo_file_path):
      File(repo_file_path,
           action = "delete")
      
      # this is time expensive
      update_cmd_formatted = [format(x) for x in self.update_cmd]
      Execute(update_cmd_formatted)

  def update(self, repo_file_path):
    repo_file_name = os.path.basename(repo_file_path)
    update_cmd_formatted = [format(x) for x in self.update_cmd]
    update_failed_exception = None

    try:
      # this is time expensive
      retcode, out = call(update_cmd_formatted, sudo=True, quiet=False)
    except ExecutionFailed as ex:
      out = ex.out
      update_failed_exception = ex

    missing_pkeys = set(re.findall(self.missing_pkey_regex, out))

    # failed but NOT due to missing pubkey
    if update_failed_exception and not missing_pkeys:
      raise update_failed_exception

    for pkey in missing_pkeys:
      # add public keys for new repos
      Execute(self.app_pkey_cmd_prefix + (pkey,),
              timeout = 15, # in case we are on the host w/o internet (using localrepo), we should ignore hanging
              ignore_failures = True,
              sudo = True,
      )
