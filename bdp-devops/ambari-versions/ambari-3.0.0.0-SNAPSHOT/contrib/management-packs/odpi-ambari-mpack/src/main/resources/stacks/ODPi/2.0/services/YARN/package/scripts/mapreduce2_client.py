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
# Python imports
import os
import sys

# Local imports
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core.exceptions import ClientComponentHasNoStatus
from yarn import yarn
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.logger import Logger


class MapReduce2Client(Script):
  def install(self, env):
    import params
    self.install_packages(env)
    self.configure(env)

  def configure(self, env, config_dir=None, upgrade_type=None):
    """
    :param env: Python environment
    :param config_dir: During rolling upgrade, which config directory to save configs to.
    """
    import params
    env.set_params(params)
    yarn(config_dir=config_dir)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def stack_upgrade_save_new_config(self, env):
    """
    Because this gets called during a Rolling Upgrade, the new mapreduce configs have already been saved, so we must be
    careful to only call configure() on the directory of the new version.
    :param env:
    """
    import params
    env.set_params(params)

    conf_select_name = "hadoop"
    base_dir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
    config_dir = self.get_config_dir_during_stack_upgrade(env, base_dir, conf_select_name)

    if config_dir:
      Logger.info("stack_upgrade_save_new_config(): Calling conf-select on %s using version %s" % (conf_select_name, str(params.version)))

      # Because this script was called from ru_execute_tasks.py which already enters an Environment with its own basedir,
      # must change it now so this function can find the Jinja Templates for the service.
      env.config.basedir = base_dir
      self.configure(env, config_dir=config_dir)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class MapReduce2ClientWindows(MapReduce2Client):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class MapReduce2ClientDefault(MapReduce2Client):
  def get_component_name(self):
    return "hadoop-client"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select("hadoop-client", params.version)


if __name__ == "__main__":
  MapReduce2Client().execute()
