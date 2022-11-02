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
from resource_management.core.logger import Logger
from resource_management.core.resources import File

def setup_ranger_nifi(upgrade_type=None):
    import params, os

    if params.has_ranger_admin and params.enable_ranger_nifi:

        stack_version = None
        if upgrade_type is not None:
            stack_version = params.version

        if params.retryAble:
            Logger.info("nifi: Setup ranger: command retry enables thus retrying if ranger admin is down !")
        else:
            Logger.info("nifi: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")


        api_version=None
        if params.stack_supports_ranger_kerberos:
            api_version='v2'
        from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin
        setup_ranger_plugin('nifi', 'nifi', params.previous_jdbc_jar,
                            params.downloaded_custom_connector, params.driver_curl_source,
                            params.driver_curl_target, params.java_home,
                            params.repo_name, params.nifi_ranger_plugin_repo,
                            params.ranger_env, params.ranger_plugin_properties,
                            params.policy_user, params.policymgr_mgr_url,
                            params.enable_ranger_nifi, conf_dict=params.nifi_config_dir,
                            component_user=params.nifi_user, component_group=params.nifi_group, cache_service_list=['nifi'],
                            plugin_audit_properties=params.config['configurations']['ranger-nifi-audit'], plugin_audit_attributes=params.config['configuration_attributes']['ranger-nifi-audit'],
                            plugin_security_properties=params.config['configurations']['ranger-nifi-security'], plugin_security_attributes=params.config['configuration_attributes']['ranger-nifi-security'],
                            plugin_policymgr_ssl_properties=params.config['configurations']['ranger-nifi-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configuration_attributes']['ranger-nifi-policymgr-ssl'],
                            component_list=[], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                            credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password,
                            ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                            stack_version_override = stack_version, skip_if_rangeradmin_down= not params.retryAble,api_version=api_version,
                            is_security_enabled = params.security_enabled,
                            is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                            component_user_principal=params.ranger_nifi_principal if params.security_enabled else None,
                            component_user_keytab=params.ranger_nifi_keytab if params.security_enabled else None)
                            
        #change permissions of ranger xml that were written to 0400
        File(os.path.join(params.nifi_config_dir, 'ranger-nifi-audit.xml'), owner=params.nifi_user, group=params.nifi_group, mode=0400)
        File(os.path.join(params.nifi_config_dir, 'ranger-nifi-security.xml'), owner=params.nifi_user, group=params.nifi_group, mode=0400)
        File(os.path.join(params.nifi_config_dir, 'ranger-policymgr-ssl.xml'), owner=params.nifi_user, group=params.nifi_group, mode=0400)        

    else:
        Logger.info('Ranger admin not installed')