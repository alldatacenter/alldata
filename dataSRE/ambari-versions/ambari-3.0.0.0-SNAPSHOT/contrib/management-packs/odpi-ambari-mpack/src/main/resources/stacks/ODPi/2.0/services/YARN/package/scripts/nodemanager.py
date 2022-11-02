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

import nodemanager_upgrade

from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from yarn import yarn
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class Nodemanager(Script):
  def install(self, env):
    self.install_packages(env)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service('nodemanager',action='stop')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    service('nodemanager',action='start')

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="nodemanager")


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class NodemanagerWindows(Nodemanager):
  def status(self, env):
    service('nodemanager', action='status')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NodemanagerDefault(Nodemanager):
  def get_component_name(self):
    return "hadoop-yarn-nodemanager"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing NodeManager Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select("hadoop-yarn-nodemanager", params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing NodeManager Stack Upgrade post-restart")
    import params
    env.set_params(params)

    nodemanager_upgrade.post_upgrade_check()

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.nodemanager_pid_file)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"yarn.timeline-service.http-authentication.type": "kerberos",
                           "yarn.acl.enable": "true"}
      props_empty_check = ["yarn.nodemanager.principal",
                           "yarn.nodemanager.keytab",
                           "yarn.nodemanager.webapp.spnego-principal",
                           "yarn.nodemanager.webapp.spnego-keytab-file"]

      props_read_check = ["yarn.nodemanager.keytab",
                          "yarn.nodemanager.webapp.spnego-keytab-file"]
      yarn_site_props = build_expectations('yarn-site', props_value_check, props_empty_check,
                                           props_read_check)

      yarn_expectations ={}
      yarn_expectations.update(yarn_site_props)

      security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                   {'yarn-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, yarn_site_props)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'yarn-site' not in security_params
               or 'yarn.nodemanager.keytab' not in security_params['yarn-site']
               or 'yarn.nodemanager.principal' not in security_params['yarn-site']) \
            or 'yarn.nodemanager.webapp.spnego-keytab-file' not in security_params['yarn-site'] \
            or 'yarn.nodemanager.webapp.spnego-principal' not in security_params['yarn-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.yarn_user,
                                security_params['yarn-site']['yarn.nodemanager.keytab'],
                                security_params['yarn-site']['yarn.nodemanager.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.yarn_user,
                                security_params['yarn-site']['yarn.nodemanager.webapp.spnego-keytab-file'],
                                security_params['yarn-site']['yarn.nodemanager.webapp.spnego-principal'],
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
    return params.yarn_log_dir
  
  def get_user(self):
    import params
    return params.yarn_user

if __name__ == "__main__":
  Nodemanager().execute()
