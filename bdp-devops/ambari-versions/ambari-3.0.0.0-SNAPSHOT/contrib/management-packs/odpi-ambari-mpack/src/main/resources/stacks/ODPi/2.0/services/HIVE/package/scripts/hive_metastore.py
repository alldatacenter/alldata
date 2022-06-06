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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Directory
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML
from resource_management.core.resources.system import File

from hive import hive
from hive import jdbc_connector
from hive_service import hive_service
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

# the legacy conf.server location in previous stack versions
LEGACY_HIVE_SERVER_CONF = "/etc/hive/conf.server"

class HiveMetastore(Script):
  def install(self, env):
    import params
    self.install_packages(env)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # writing configurations on start required for securtity
    self.configure(env)

    hive_service('metastore', action='start', upgrade_type=upgrade_type)


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    hive_service('metastore', action='stop', upgrade_type=upgrade_type)


  def configure(self, env):
    import params
    env.set_params(params)
    hive(name = 'metastore')


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveMetastoreWindows(HiveMetastore):
  def status(self, env):
    import status_params
    from resource_management.libraries.functions import check_windows_service_status
    check_windows_service_status(status_params.hive_metastore_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveMetastoreDefault(HiveMetastore):
  def get_component_name(self):
    return "hive-metastore"


  def status(self, env):
    import status_params
    from resource_management.libraries.functions import check_process_status

    env.set_params(status_params)
    pid_file = format("{hive_pid_dir}/{hive_metastore_pid}")
    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Metastore Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    is_upgrade = params.upgrade_direction == Direction.UPGRADE

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select("hive-metastore", params.version)

    if is_upgrade and params.stack_version_formatted_major and \
            check_stack_feature(StackFeature.HIVE_METASTORE_UPGRADE_SCHEMA, params.stack_version_formatted_major):
      self.upgrade_schema(env)


  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"hive.server2.authentication": "KERBEROS",
                           "hive.metastore.sasl.enabled": "true",
                           "hive.security.authorization.enabled": "true"}
      props_empty_check = ["hive.metastore.kerberos.keytab.file",
                           "hive.metastore.kerberos.principal"]

      props_read_check = ["hive.metastore.kerberos.keytab.file"]
      hive_site_props = build_expectations('hive-site', props_value_check, props_empty_check,
                                            props_read_check)

      hive_expectations ={}
      hive_expectations.update(hive_site_props)

      security_params = get_params_from_filesystem(status_params.hive_conf_dir,
                                                   {'hive-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, hive_expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if 'hive-site' not in security_params \
            or 'hive.metastore.kerberos.keytab.file' not in security_params['hive-site'] \
            or 'hive.metastore.kerberos.principal' not in security_params['hive-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hive_user,
                                security_params['hive-site']['hive.metastore.kerberos.keytab.file'],
                                security_params['hive-site']['hive.metastore.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)

          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        except Exception as e:
          self.put_structured_out({"securityState": "ERROR"})
          self.put_structured_out({"securityStateErrorInfo": str(e)})
      else:
        issues = []
        for cf in result_issues:
          issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
        self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
        self.put_structured_out({"securityState": "UNSECURED"})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})


  def upgrade_schema(self, env):
    """
    Executes the schema upgrade binary.  This is its own function because it could
    be called as a standalone task from the upgrade pack, but is safe to run it for each
    metastore instance. The schema upgrade on an already upgraded metastore is a NOOP.

    The metastore schema upgrade requires a database driver library for most
    databases. During an upgrade, it's possible that the library is not present,
    so this will also attempt to copy/download the appropriate driver.

    This function will also ensure that configurations are written out to disk before running
    since the new configs will most likely not yet exist on an upgrade.

    Should not be invoked for a DOWNGRADE; Metastore only supports schema upgrades.
    """
    Logger.info("Upgrading Hive Metastore Schema")
    import status_params
    import params
    env.set_params(params)

    # ensure that configurations are written out before trying to upgrade the schema
    # since the schematool needs configs and doesn't know how to use the hive conf override
    self.configure(env)

    if params.security_enabled:
      cached_kinit_executor(status_params.kinit_path_local,
        status_params.hive_user,
        params.hive_metastore_keytab_path,
        params.hive_metastore_principal,
        status_params.hostname,
        status_params.tmp_dir)
      
    # ensure that the JDBC drive is present for the schema tool; if it's not
    # present, then download it first
    if params.hive_jdbc_driver in params.hive_jdbc_drivers_list:
      target_directory = format("{stack_root}/{version}/hive/lib")

      # download it if it does not exist
      if not os.path.exists(params.source_jdbc_file):
        jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)

      target_directory_and_filename = os.path.join(target_directory, os.path.basename(params.source_jdbc_file))

      if params.sqla_db_used:
        target_native_libs_directory = format("{target_directory}/native/lib64")

        Execute(format("yes | {sudo} cp {jars_in_hive_lib} {target_directory}"))

        Directory(target_native_libs_directory, create_parents = True)

        Execute(format("yes | {sudo} cp {libs_in_hive_lib} {target_native_libs_directory}"))

        Execute(format("{sudo} chown -R {hive_user}:{user_group} {hive_lib}/*"))
      else:
        # copy the JDBC driver from the older metastore location to the new location only
        # if it does not already exist
        if not os.path.exists(target_directory_and_filename):
          Execute(('cp', params.source_jdbc_file, target_directory),
            path=["/bin", "/usr/bin/"], sudo = True)

      File(target_directory_and_filename, mode = 0644)

    # build the schema tool command
    binary = format("{hive_schematool_ver_bin}/schematool")

    # the conf.server directory changed locations between stack versions
    # since the configurations have not been written out yet during an upgrade
    # we need to choose the original legacy location
    schematool_hive_server_conf_dir = params.hive_server_conf_dir
    if not(check_stack_feature(StackFeature.CONFIG_VERSIONING, params.version_for_stack_feature_checks)):
      schematool_hive_server_conf_dir = LEGACY_HIVE_SERVER_CONF

    env_dict = {
      'HIVE_CONF_DIR': schematool_hive_server_conf_dir
    }

    command = format("{binary} -dbType {hive_metastore_db_type} -upgradeSchema")
    Execute(command, user=params.hive_user, tries=1, environment=env_dict, logoutput=True)
    
  def get_log_folder(self):
    import params
    return params.hive_log_dir

  def get_user(self):
    import params
    return params.hive_user


if __name__ == "__main__":
  HiveMetastore().execute()
