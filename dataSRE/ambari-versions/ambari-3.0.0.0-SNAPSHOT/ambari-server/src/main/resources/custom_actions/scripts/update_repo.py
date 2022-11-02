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
from resource_management import *
from resource_management import Script, Repository, format
from ambari_commons.os_check import OSCheck
import ambari_simplejson as json
from resource_management.core.logger import Logger



class UpdateRepo(Script):

  UBUNTU_REPO_COMPONENTS_POSTFIX = "main"

  def actionexecute(self, env):
    config = Script.get_config()
    structured_output = {}


    try:
      repo_info = config['repositoryFile']

      for item in repo_info["repositories"]:
        base_url = item["baseUrl"]
        repo_name = item["repoName"]
        repo_id = item["repoId"]
        distribution = item["distribution"] if "distribution" in item else None
        components = item["components"] if "components" in item else None

        repo_rhel_suse = config['configurations']['cluster-env']['repo_suse_rhel_template']
        repo_ubuntu = config['configurations']['cluster-env']['repo_ubuntu_template']

        template = repo_rhel_suse if OSCheck.is_suse_family() or OSCheck.is_redhat_family() else repo_ubuntu
        ubuntu_components = [distribution if distribution else repo_name] + \
                            [components.replace(",", " ") if components else self.UBUNTU_REPO_COMPONENTS_POSTFIX]

        Repository(repo_id,
                 action = "prepare",
                 base_url = base_url,
                 mirror_list = None,
                 repo_file_name = repo_name,
                 repo_template = template,
                 components = ubuntu_components, # ubuntu specific
        )
        structured_output["repo_update"] = {"exit_code" : 0, "message": format("Repository files successfully updated!")}
      Repository(None, action="create")
    except Exception, exception:
      Logger.logger.exception("ERROR: There was an unexpected error while updating repositories")
      raise Fail("Failed to update repo files!")

    self.put_structured_out(structured_output)


if __name__ == "__main__":
  UpdateRepo().execute()
