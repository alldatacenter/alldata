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


from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from ambari_commons import OSCheck, OSConst
if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
from setup_ranger_hive import setup_ranger_hive
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger

import hive_server_upgrade
from hive import hive
from hive_service import hive_service


class HiveServer(Script):
  def install(self, env):
    import params
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hive(name='hiveserver2')


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerWindows(HiveServer):
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    hive_service('hiveserver2', action='start')

  def stop(self, env):
    import params
    env.set_params(params)
    hive_service('hiveserver2', action='stop')

  def status(self, env):
    import status_params
    check_windows_service_status(status_params.hive_server_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerDefault(HiveServer):
  def get_component_name(self):
    return "hive-server2"

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY

    setup_ranger_hive(upgrade_type=upgrade_type)
    hive_service('hiveserver2', action = 'start', upgrade_type=upgrade_type)

    # only perform this if upgrading and rolling; a non-rolling upgrade doesn't need
    # to do this since hive is already down
    if upgrade_type == UPGRADE_TYPE_ROLLING:
      hive_server_upgrade.post_upgrade_deregister()


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # During rolling upgrade, HiveServer2 should not be stopped before new server is available.
    # Once new server is started, old one is stopped by the --deregister command which is 
    # invoked by the 'hive_server_upgrade.post_upgrade_deregister()' method
    if upgrade_type != UPGRADE_TYPE_ROLLING:
      hive_service( 'hiveserver2', action = 'stop' )


  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = format("{hive_pid_dir}/{hive_pid}")

    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Hive Server Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):

      stack_select.select("hive-server2", params.version)

      # Copy mapreduce.tar.gz and tez.tar.gz to HDFS
      resource_created = copy_to_hdfs(
        "mapreduce",
        params.user_group,
        params.hdfs_user,
        host_sys_prepped=params.host_sys_prepped)

      resource_created = copy_to_hdfs(
        "tez",
        params.user_group,
        params.hdfs_user,
        host_sys_prepped=params.host_sys_prepped) or resource_created

      if resource_created:
        params.HdfsResource(None, action="execute")


  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"hive.server2.authentication": "KERBEROS",
                           "hive.metastore.sasl.enabled": "true",
                           "hive.security.authorization.enabled": "true"}
      props_empty_check = ["hive.server2.authentication.kerberos.keytab",
                           "hive.server2.authentication.kerberos.principal",
                           "hive.server2.authentication.spnego.principal",
                           "hive.server2.authentication.spnego.keytab"]

      props_read_check = ["hive.server2.authentication.kerberos.keytab",
                          "hive.server2.authentication.spnego.keytab"]
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
            or 'hive.server2.authentication.kerberos.keytab' not in security_params['hive-site'] \
            or 'hive.server2.authentication.kerberos.principal' not in security_params['hive-site']  \
            or 'hive.server2.authentication.spnego.keytab' not in security_params['hive-site'] \
            or 'hive.server2.authentication.spnego.principal' not in security_params['hive-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hive_user,
                                security_params['hive-site']['hive.server2.authentication.kerberos.keytab'],
                                security_params['hive-site']['hive.server2.authentication.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hive_user,
                                security_params['hive-site']['hive.server2.authentication.spnego.keytab'],
                                security_params['hive-site']['hive.server2.authentication.spnego.principal'],
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

  def get_log_folder(self):
    import params
    return params.hive_log_dir
  
  def get_user(self):
    import params
    return params.hive_user

if __name__ == "__main__":
  HiveServer().execute()
