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
__all__ = ["setup_ranger_plugin", "get_audit_configs", "generate_ranger_service_config"]

import os
import ambari_simplejson as json
from datetime import datetime
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.resources import File, Directory, Execute
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.core.logger import Logger
from resource_management.core.source import DownloadSource, InlineTemplate
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2
from resource_management.core.utils import PasswordString
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

def setup_ranger_plugin(component_select_name, service_name, previous_jdbc_jar,
                        component_downloaded_custom_connector, component_driver_curl_source,
                        component_driver_curl_target, java_home,
                        repo_name, plugin_repo_dict,
                        ranger_env_properties, plugin_properties,
                        policy_user, policymgr_mgr_url,
                        plugin_enabled, conf_dict, component_user, component_group,
                        cache_service_list, plugin_audit_properties, plugin_audit_attributes,
                        plugin_security_properties, plugin_security_attributes,
                        plugin_policymgr_ssl_properties, plugin_policymgr_ssl_attributes,
                        component_list, audit_db_is_enabled, credential_file,
                        xa_audit_db_password, ssl_truststore_password,
                        ssl_keystore_password, api_version=None, stack_version_override = None, skip_if_rangeradmin_down = True,
                        is_security_enabled = False, is_stack_supports_ranger_kerberos = False,
                        component_user_principal = None, component_user_keytab = None, cred_lib_path_override = None, cred_setup_prefix_override = None):

  if audit_db_is_enabled and component_driver_curl_source is not None and not component_driver_curl_source.endswith("/None"):
    if previous_jdbc_jar and os.path.isfile(previous_jdbc_jar):
      File(previous_jdbc_jar, action='delete')

    File(component_downloaded_custom_connector,
      content = DownloadSource(component_driver_curl_source),
      mode = 0644
    )

    Execute(('cp', '--remove-destination', component_downloaded_custom_connector, component_driver_curl_target),
      path=["/bin", "/usr/bin/"],
      sudo=True
    )

    File(component_driver_curl_target, mode=0644)

  if policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  if stack_version_override is None:
    stack_version = get_stack_version(component_select_name)
  else:
    stack_version = stack_version_override

  component_conf_dir = conf_dict

  if plugin_enabled:

    service_name_exist = get_policycache_service_name(service_name, repo_name, cache_service_list)

    if not service_name_exist:
      if api_version is not None and api_version == 'v2':
        ranger_adm_obj = RangeradminV2(url=policymgr_mgr_url, skip_if_rangeradmin_down=skip_if_rangeradmin_down)
        ranger_adm_obj.create_ranger_repository(service_name, repo_name, plugin_repo_dict,
                                                ranger_env_properties['ranger_admin_username'], ranger_env_properties['ranger_admin_password'],
                                                ranger_env_properties['admin_username'], ranger_env_properties['admin_password'],
                                                policy_user, is_security_enabled, is_stack_supports_ranger_kerberos, component_user,
                                                component_user_principal, component_user_keytab)
      else:
        ranger_adm_obj = Rangeradmin(url=policymgr_mgr_url, skip_if_rangeradmin_down=skip_if_rangeradmin_down)
        ranger_adm_obj.create_ranger_repository(service_name, repo_name, plugin_repo_dict,
                                              ranger_env_properties['ranger_admin_username'], ranger_env_properties['ranger_admin_password'],
                                              ranger_env_properties['admin_username'], ranger_env_properties['admin_password'],
                                              policy_user)

    current_datetime = datetime.now()

    File(format('{component_conf_dir}/ranger-security.xml'),
      owner = component_user,
      group = component_group,
      mode = 0644,
      content = InlineTemplate(format('<ranger>\n<enabled>{current_datetime}</enabled>\n</ranger>'))
    )

    Directory([os.path.join('/etc', 'ranger', repo_name), os.path.join('/etc', 'ranger', repo_name, 'policycache')],
      owner = component_user,
      group = component_group,
      mode=0775,
      create_parents = True,
      cd_access = 'a'
    )

    for cache_service in cache_service_list:
      File(os.path.join('/etc', 'ranger', repo_name, 'policycache', format('{cache_service}_{repo_name}.json')),
        owner = component_user,
        group = component_group,
        mode = 0644
      )

    # remove plain-text password from xml configs
    plugin_audit_password_property = 'xasecure.audit.destination.db.password'
    plugin_audit_properties_copy = {}
    plugin_audit_properties_copy.update(plugin_audit_properties)

    if plugin_audit_password_property in plugin_audit_properties_copy:
      plugin_audit_properties_copy[plugin_audit_password_property] = "crypted"

    XmlConfig(format('ranger-{service_name}-audit.xml'),
      conf_dir=component_conf_dir,
      configurations=plugin_audit_properties_copy,
      configuration_attributes=plugin_audit_attributes,
      owner = component_user,
      group = component_group,
      mode=0744)

    XmlConfig(format('ranger-{service_name}-security.xml'),
      conf_dir=component_conf_dir,
      configurations=plugin_security_properties,
      configuration_attributes=plugin_security_attributes,
      owner = component_user,
      group = component_group,
      mode=0744)

    # remove plain-text password from xml configs
    plugin_password_properties = ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']
    plugin_policymgr_ssl_properties_copy = {}
    plugin_policymgr_ssl_properties_copy.update(plugin_policymgr_ssl_properties)

    for prop in plugin_password_properties:
      if prop in plugin_policymgr_ssl_properties_copy:
        plugin_policymgr_ssl_properties_copy[prop] = "crypted"

    if str(service_name).lower() == 'yarn' :
      XmlConfig("ranger-policymgr-ssl-yarn.xml",
        conf_dir=component_conf_dir,
        configurations=plugin_policymgr_ssl_properties_copy,
        configuration_attributes=plugin_policymgr_ssl_attributes,
        owner = component_user,
        group = component_group,
        mode=0744)
    else:
      XmlConfig("ranger-policymgr-ssl.xml",
        conf_dir=component_conf_dir,
        configurations=plugin_policymgr_ssl_properties_copy,
        configuration_attributes=plugin_policymgr_ssl_attributes,
        owner = component_user,
        group = component_group,
        mode=0744)

    setup_ranger_plugin_keystore(service_name, audit_db_is_enabled, stack_version, credential_file,
              xa_audit_db_password, ssl_truststore_password, ssl_keystore_password,
              component_user, component_group, java_home, cred_lib_path_override, cred_setup_prefix_override)

  else:
    File(format('{component_conf_dir}/ranger-security.xml'),
      action="delete"
    )

def setup_ranger_plugin_jar_symblink(stack_version, service_name, component_list):

  stack_root = Script.get_stack_root()
  jar_files = os.listdir(format('{stack_root}/{stack_version}/ranger-{service_name}-plugin/lib'))

  for jar_file in jar_files:
    for component in component_list:
      Execute(('ln','-sf',format('{stack_root}/{stack_version}/ranger-{service_name}-plugin/lib/{jar_file}'),format('{stack_root}/current/{component}/lib/{jar_file}')),
      not_if=format('ls {stack_root}/current/{component}/lib/{jar_file}'),
      only_if=format('ls {stack_root}/{stack_version}/ranger-{service_name}-plugin/lib/{jar_file}'),
      sudo=True)

def setup_ranger_plugin_keystore(service_name, audit_db_is_enabled, stack_version, credential_file, xa_audit_db_password,
                                ssl_truststore_password, ssl_keystore_password, component_user, component_group, java_home, cred_lib_path_override = None, cred_setup_prefix_override = None):

  stack_root = Script.get_stack_root()
  service_name = str(service_name).lower()

  if cred_lib_path_override is not None:
    cred_lib_path = cred_lib_path_override
  else:
    cred_lib_path = format('{stack_root}/{stack_version}/ranger-{service_name}-plugin/install/lib/*')

  if cred_setup_prefix_override is not None:
    cred_setup_prefix = cred_setup_prefix_override
  else:
    cred_setup_prefix = (format('{stack_root}/{stack_version}/ranger-{service_name}-plugin/ranger_credential_helper.py'), '-l', cred_lib_path)

  if audit_db_is_enabled:
    cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'auditDBCred', '-v', PasswordString(xa_audit_db_password), '-c', '1')
    Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'sslKeyStore', '-v', PasswordString(ssl_keystore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'sslTrustStore', '-v', PasswordString(ssl_truststore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  File(credential_file,
    owner = component_user,
    group = component_group,
    mode = 0640
  )

  dot_jceks_crc_file_path = os.path.join(os.path.dirname(credential_file), "." + os.path.basename(credential_file) + ".crc")

  File(dot_jceks_crc_file_path,
    owner = component_user,
    group = component_group,
    only_if = format("test -e {dot_jceks_crc_file_path}"),
    mode = 0640
  )

def setup_configuration_file_for_required_plugins(component_user, component_group, create_core_site_path,
                                                  configurations={}, configuration_attributes={}, file_name='core-site.xml',
                                                  xml_include_file=None, xml_include_file_content=None):
  XmlConfig(file_name,
    conf_dir = create_core_site_path,
    configurations = configurations,
    configuration_attributes = configuration_attributes,
    owner = component_user,
    group = component_group,
    mode = 0644,
    xml_include_file = xml_include_file
  )

  if xml_include_file_content:
    File(xml_include_file,
         owner=component_user,
         group=component_group,
         content=xml_include_file_content
         )


def get_audit_configs(config):
  xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR'].lower()
  xa_db_host = config['configurations']['admin-properties']['db_host']
  xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')

  if xa_audit_db_flavor == 'mysql':
    jdbc_jar_name = default("/ambariLevelParams/custom_mysql_jdbc_name", None)
    previous_jdbc_jar_name = default("/ambariLevelParams/previous_custom_mysql_jdbc_name", None)
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor == 'oracle':
    jdbc_jar_name = default("/ambariLevelParams/custom_oracle_jdbc_name", None)
    previous_jdbc_jar_name = default("/ambariLevelParams/previous_custom_oracle_jdbc_name", None)
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor == 'postgres':
    jdbc_jar_name = default("/ambariLevelParams/custom_postgres_jdbc_name", None)
    previous_jdbc_jar_name = default("/ambariLevelParams/previous_custom_postgres_jdbc_name", None)
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor == 'mssql':
    jdbc_jar_name = default("/ambariLevelParams/custom_mssql_jdbc_name", None)
    previous_jdbc_jar_name = default("/ambariLevelParams/previous_custom_mssql_jdbc_name", None)
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor == 'sqla':
    jdbc_jar_name = default("/ambariLevelParams/custom_sqlanywhere_jdbc_name", None)
    previous_jdbc_jar_name = default("/ambariLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"
  else: raise Fail(format("'{xa_audit_db_flavor}' db flavor not supported."))

  return jdbc_jar_name, previous_jdbc_jar_name, audit_jdbc_url, jdbc_driver

def generate_ranger_service_config(ranger_plugin_properties):
  custom_service_config_dict = {}
  ranger_plugin_properties_copy = {}
  ranger_plugin_properties_copy.update(ranger_plugin_properties)

  for key, value in ranger_plugin_properties_copy.iteritems():
    if key.startswith("ranger.service.config.param."):
      modify_key_name = key.replace("ranger.service.config.param.","")
      custom_service_config_dict[modify_key_name] = value

  return custom_service_config_dict

def get_policycache_service_name(service_name, repo_name, cache_service_list):
  service_name_exist_flag = False
  policycache_path = os.path.join('/etc', 'ranger', repo_name, 'policycache')
  try:
    for cache_service in cache_service_list:
      policycache_json_file = format('{policycache_path}/{cache_service}_{repo_name}.json')
      if os.path.isfile(policycache_json_file) and os.path.getsize(policycache_json_file) > 0:
        with open(policycache_json_file) as json_file:
          json_data = json.load(json_file)
          if 'serviceName' in json_data and json_data['serviceName'] == repo_name:
            Logger.info("Skipping Ranger API calls, as policy cache file exists for {0}".format(service_name))
            Logger.warning("If service name for {0} is not created on Ranger Admin, then to re-create it delete policy cache file: {1}".format(service_name, policycache_json_file))
            service_name_exist_flag = True
            break
  except Exception, err:
    Logger.error("Error occurred while fetching service name from policy cache file.\nError: {0}".format(err))

  return service_name_exist_flag
