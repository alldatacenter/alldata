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

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from resource_management.core.source import Template
from resource_management.core.logger import Logger

from install_jars import install_tez_jars
from yarn import yarn
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HistoryServer(Script):
  def install(self, env):
    self.install_packages(env)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service('historyserver', action='stop', serviceName='mapreduce')

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="historyserver")


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HistoryserverWindows(HistoryServer):
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    service('historyserver', action='start', serviceName='mapreduce')

  def status(self, env):
    service('historyserver', action='status')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HistoryServerDefault(HistoryServer):
  def get_component_name(self):
    return "hadoop-mapreduce-historyserver"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select("hadoop-mapreduce-historyserver", params.version)
      # MC Hammer said, "Can't touch this"
      copy_to_hdfs("mapreduce", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)
      copy_to_hdfs("tez", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)
      copy_to_hdfs("slider", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)
      params.HdfsResource(None, action="execute")

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY

    if params.stack_version_formatted_major and check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.stack_version_formatted_major):
      # MC Hammer said, "Can't touch this"
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
      resource_created = copy_to_hdfs(
        "slider",
        params.user_group,
        params.hdfs_user,
        host_sys_prepped=params.host_sys_prepped) or resource_created
      if resource_created:
        params.HdfsResource(None, action="execute")
    else:
      # In stack versions before copy_tarball_to_hdfs support tez.tar.gz was copied to a different folder in HDFS.
      install_tez_jars()

    service('historyserver', action='start', serviceName='mapreduce')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.mapred_historyserver_pid_file)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      expectations = {}
      expectations.update(build_expectations('mapred-site',
                                             None,
                                             [
                                               'mapreduce.jobhistory.keytab',
                                               'mapreduce.jobhistory.principal',
                                               'mapreduce.jobhistory.webapp.spnego-keytab-file',
                                               'mapreduce.jobhistory.webapp.spnego-principal'
                                             ],
                                             None))

      security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                   {'mapred-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'mapred-site' not in security_params or
               'mapreduce.jobhistory.keytab' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.principal' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.webapp.spnego-keytab-file' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.webapp.spnego-principal' not in security_params['mapred-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal not set."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.mapred_user,
                                security_params['mapred-site']['mapreduce.jobhistory.keytab'],
                                security_params['mapred-site']['mapreduce.jobhistory.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.mapred_user,
                                security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-keytab-file'],
                                security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-principal'],
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
    return params.mapred_log_dir

  def get_user(self):
    import params
    return params.mapred_user

if __name__ == "__main__":
  HistoryServer().execute()
