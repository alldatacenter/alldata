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
import os

from resource_management import *
import sys
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster, setup_atlas_hook
from ambari_commons.constants import SERVICE


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hcat():
  import params

  XmlConfig("hive-site.xml",
            conf_dir = params.hive_conf_dir,
            configurations = params.config['configurations']['hive-site'],
            owner=params.hive_user,
            configuration_attributes=params.config['configuration_attributes']['hive-site']
  )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hcat():
  import params

  Directory(params.hive_conf_dir,
            create_parents = True,
            owner=params.hcat_user,
            group=params.user_group,
  )


  Directory(params.hcat_conf_dir,
            create_parents = True,
            owner=params.hcat_user,
            group=params.user_group,
  )

  Directory(params.hcat_pid_dir,
            owner=params.webhcat_user,
            create_parents = True
  )

  XmlConfig("hive-site.xml",
            conf_dir=params.hive_client_conf_dir,
            configurations=params.config['configurations']['hive-site'],
            configuration_attributes=params.config['configuration_attributes']['hive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0644)

  File(format("{hcat_conf_dir}/hcat-env.sh"),
       owner=params.hcat_user,
       group=params.user_group,
       content=InlineTemplate(params.hcat_env_sh_template)
  )

  # Generate atlas-application.properties.xml file
  if has_atlas_in_cluster():
    atlas_hook_filepath = os.path.join(params.hive_config_dir, params.atlas_hook_filename)
    setup_atlas_hook(SERVICE.HIVE, params.hive_atlas_application_properties, atlas_hook_filepath, params.hive_user, params.user_group)
