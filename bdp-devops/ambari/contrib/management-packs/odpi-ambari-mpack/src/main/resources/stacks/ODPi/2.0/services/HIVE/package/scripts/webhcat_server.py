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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from webhcat import webhcat
from webhcat_service import webhcat_service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class WebHCatServer(Script):
  def install(self, env):
    import params
    self.install_packages(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    webhcat_service(action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    webhcat_service(action='stop')

  def configure(self, env):
    import params
    env.set_params(params)
    webhcat()


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class WebHCatServerWindows(WebHCatServer):
  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_windows_service_status(status_params.webhcat_server_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class WebHCatServerDefault(WebHCatServer):
  def get_component_name(self):
    return "hive-webhcat"

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.webhcat_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing WebHCat Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version): 
      # webhcat has no conf, but uses hadoop home, so verify that regular hadoop conf is set
      stack_select.select("hive-webhcat", params.version)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)

    if status_params.security_enabled:
      expectations ={}
      expectations.update(
        build_expectations(
          'webhcat-site',
          {
            "templeton.kerberos.secret": "secret"
          },
          [
            "templeton.kerberos.keytab",
            "templeton.kerberos.principal"
          ],
          [
            "templeton.kerberos.keytab"
          ]
        )
      )
      expectations.update(
        build_expectations(
          'hive-site',
          {
            "hive.server2.authentication": "KERBEROS",
            "hive.metastore.sasl.enabled": "true",
            "hive.security.authorization.enabled": "true"
          },
          None,
          None
        )
      )

      security_params = {}
      security_params.update(get_params_from_filesystem(status_params.hive_conf_dir,
                                                        {'hive-site.xml': FILE_TYPE_XML}))
      security_params.update(get_params_from_filesystem(status_params.webhcat_conf_dir,
                                                        {'webhcat-site.xml': FILE_TYPE_XML}))
      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if 'webhcat-site' not in security_params \
            or 'templeton.kerberos.keytab' not in security_params['webhcat-site'] \
            or 'templeton.kerberos.principal' not in security_params['webhcat-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.webhcat_user,
                                security_params['webhcat-site']['templeton.kerberos.keytab'],
                                security_params['webhcat-site']['templeton.kerberos.principal'],
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
    return params.hcat_log_dir
  
  def get_user(self):
    import params
    return params.webhcat_user

if __name__ == "__main__":
  WebHCatServer().execute()
