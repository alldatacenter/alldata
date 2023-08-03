#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#

import os
import sys
import urllib2
import base64
import httplib
import json
import time
from xml.etree import ElementTree as ET
import datetime
from subprocess import Popen, PIPE
import re


def port_ranger_installation_to_ambari():
	print('preparing advanged configurations for ranger')
	flag_ranger_admin_present, ranger_admin_properties_from_file = get_ranger_admin_install_properties()
	if flag_ranger_admin_present:
		print('ranger admin service is installed, making configurations as required by ambari.')
		if create_ranger_service_in_ambari():
			print('ranger service is added sucessfully in ambari')
			if create_ranger_service_components_in_ambari('RANGER_ADMIN'):
				print('ranger service component is added successfully in ambari')
				if register_ranger_admin_host_in_ambari():
					print('ranger admin host is registered successfully in ambari')
					if add_advanced_ranger_configurations(1, ranger_admin_properties_from_file):
						print('ranger-admin advanced configurations added successfully in ambari, kindly run ranger-usersync to complete ranger service install')
					else:
						print('ranger advanced configurations added failed in ambari')
				else:
					print('ranger admin host registration failed in ambari')
			else:
				print('ranger service component add failed in ambari')
		else:
			print('ranger service add failed in ambari')
	else:
		print('ranger admin and usersync services are not installed, not importing configurations to ambari.')


def port_ranger_usersync_installation_to_ambari():
	print ('preparing configurations for ranger user-sync')
	flag_ranger_usersync_present, ranger_usersync_properties_from_file = get_ranger_usersync_install_properties()
	if flag_ranger_usersync_present:
		print('ranger usersync service is installed, making configurations as required by ambari.')
		if create_ranger_service_components_in_ambari('RANGER_USERSYNC'):
			print('ranger service component is added successfully in ambari')
			if register_ranger_usersync_host_in_ambari():
				print('ranger usersync host is registered successfully in ambari')
				if add_advanced_ranger_configurations(2, ranger_usersync_properties_from_file):
					print('ranger advanced configurations added successfully in ambari')
					if call_ranger_installed():
						print('ranger service installed successfully in ambari.')
					else:
						print('ranger service install failed in ambari')
				else:
					print('ranger advanced configurations added failed in ambari')
			else:
				print('ranger usersync host registration failed in ambari')
		else:
			print('ranger service add failed in ambari')
	else:
		print('ranger admin and usersync services are not installed, not importing configurations to ambari.')


def create_ranger_service_in_ambari():
	print('creating ranger service in ambari')
	ranger_create_url = ambari_service_url + '/' + ranger_service_name
	request_result = call_ambari_api(ranger_create_url, 'POST', ambari_username_password, '')
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if (response_code == 201 and response is not None):
			print('ranger service created successfully in ambari.')
			return True
		elif (response_code == 409 and response is not None):
			print('ranger service is already created in ambari.')
			return True
		else:
			print('ranger service creation failed in ambari.')
			return False

def create_ranger_service_components_in_ambari(ranger_service_component_name):
	print('adding ranger servcie components in ambari')
	ranger_service_components = '{"components":[{"ServiceComponentInfo":{"component_name":"' + ranger_service_component_name + '"}}]}'
	print('creating ranger service in ambari')
	ranger_service_component_create_url = ambari_service_url + '?ServiceInfo/service_name=' + ranger_service_name
	request_result = call_ambari_api(ranger_service_component_create_url, 'POST', ambari_username_password, ranger_service_components)
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if (response_code == 201 and response is not None):
			print('ranger service component : ' + ranger_service_component_name + ', created successfully in ambari.')
			return True
		elif (response_code == 409 and response is not None):
			print('ranger service component : ' + ranger_service_component_name + ',  is already present in ambari.')
			return True
		else:
			print('ranger service component creation for : ' + ranger_service_component_name + ',  failed in ambari.')
			return False

def register_ranger_admin_host_in_ambari():
	print('adding ranger servcie components in ambari')
	print('creating ranger admin service in ambari')
	ranger_service_component_create_url = ambari_cluster_url + '/hosts/' + ranger_admin_fqdn + '/host_components/' + admin_component_name
	request_result = call_ambari_api(ranger_service_component_create_url, 'POST', ambari_username_password, '')
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if response_code == 201 and response is not None:
			print('ranger admin host registered successfully in ambari.')
			return True
		elif (response_code == 409 and response is not None):
			print('ranger admin host is already registered ambari.')
			return True
		else:
			print('ranger admin host registration failed in ambari.')
		return False


def register_ranger_usersync_host_in_ambari():
	print('adding ranger servcie components in ambari')
	print('creating ranger admin service in ambari')
	ranger_host_register_url = ambari_cluster_url + '/hosts/' + ranger_admin_fqdn + '/host_components/' + usersync_component_name
	request_result = call_ambari_api(ranger_host_register_url, 'POST', ambari_username_password, '')
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if (response_code == 201 and response is not None):
			print('ranger usersync host registered successfully in ambari.')
			return True
		elif (response_code == 409 and response is not None):
			print('ranger usersync host is already registered ambari.')
			return True
		else:
			print('ranger usersync host registration failed in ambari.')
			return False


def add_advanced_ranger_configurations(add_admin_or_usersync, ranger_service_properties_from_file):
	print('creating advanced configurations to be added to ambari.')
	ranger_config_data = ''
	advanced_admin_properties = dict()
	advanced_ranger_site_properties = dict()
	advanced_ranger_env_properties = dict()
	advanced_user_sync_properties = dict()
	date_time_stamp = getDateTimeNow()

	if (add_admin_or_usersync == 1):
		if not ((str(ranger_service_properties_from_file['db_root_password']).strip() == '') or
			        (str(ranger_service_properties_from_file['db_root_user']).strip() == '' )) :
			advanced_admin_properties['DB_FLAVOR'] = ranger_service_properties_from_file['DB_FLAVOR']
			advanced_admin_properties['SQL_COMMAND_INVOKER'] = ranger_service_properties_from_file['SQL_COMMAND_INVOKER']
			advanced_admin_properties['SQL_CONNECTOR_JAR'] = ranger_service_properties_from_file['SQL_CONNECTOR_JAR']
			advanced_admin_properties['db_root_user'] = ranger_service_properties_from_file['db_root_user']
			advanced_admin_properties['db_root_password'] = ranger_service_properties_from_file['db_root_password']
			advanced_admin_properties['db_host'] = ranger_service_properties_from_file['db_host']
			advanced_admin_properties['db_name'] = ranger_service_properties_from_file['db_name']
			advanced_admin_properties['db_user'] = ranger_service_properties_from_file['jdbc.user']
			advanced_admin_properties['db_password'] = ranger_service_properties_from_file['jdbc.password']
			advanced_admin_properties['audit_db_name'] = ranger_service_properties_from_file['audit_db_name']
			advanced_admin_properties['audit_db_user'] = ranger_service_properties_from_file['auditDB.jdbc.user']
			advanced_admin_properties['audit_db_password'] = ranger_service_properties_from_file['auditDB.jdbc.password']
			advanced_admin_properties['policymgr_external_url'] = ranger_service_properties_from_file['xa.webapp.url.root']
			advanced_admin_properties['policymgr_http_enabled'] = ranger_service_properties_from_file['http.enabled']
			advanced_admin_properties['authentication_method'] = get_authentication_method()
                        advanced_admin_properties['remoteLoginEnabled'] = ranger_service_properties_from_file.get('remoteLoginEnabled','false')
                        advanced_admin_properties['authServiceHostName'] = ranger_service_properties_from_file.get('authServiceHostName','localhost')
                        advanced_admin_properties['authServicePort'] = ranger_service_properties_from_file.get('authServicePort','5151')
			advanced_admin_properties['xa_ldap_url'] = ranger_service_properties_from_file['xa_ldap_url']
			advanced_admin_properties['xa_ldap_userDNpattern'] = ranger_service_properties_from_file['xa_ldap_userDNpattern']
			advanced_admin_properties['xa_ldap_groupSearchBase'] = ranger_service_properties_from_file['xa_ldap_groupSearchBase']
			advanced_admin_properties['xa_ldap_groupSearchFilter'] = ranger_service_properties_from_file['xa_ldap_groupSearchFilter']
			advanced_admin_properties['xa_ldap_groupRoleAttribute'] = ranger_service_properties_from_file['xa_ldap_groupRoleAttribute']
			advanced_admin_properties['xa_ldap_ad_domain'] = ranger_service_properties_from_file['xa_ldap_ad_domain']
			advanced_admin_properties['xa_ldap_ad_url'] = ranger_service_properties_from_file['xa_ldap_ad_url']

			advanced_ranger_site_properties['HTTP_SERVICE_PORT'] = ranger_service_properties_from_file['http.service.port']
			advanced_ranger_site_properties['HTTPS_SERVICE_PORT'] = ranger_service_properties_from_file['https.service.port']
			advanced_ranger_site_properties['HTTPS_KEYSTORE_FILE'] = ranger_service_properties_from_file['https.attrib.keystoreFile']
			advanced_ranger_site_properties['HTTPS_KEYSTORE_PASS'] = ranger_service_properties_from_file['https.attrib.keystorePass']
			advanced_ranger_site_properties['HTTPS_KEY_ALIAS'] = ranger_service_properties_from_file['https.attrib.keyAlias']
			advanced_ranger_site_properties['HTTPS_CLIENT_AUTH'] = ranger_service_properties_from_file['https.attrib.clientAuth']
			advanced_ranger_site_properties['HTTP_ENABLED'] = ranger_service_properties_from_file['http.enabled']

			advanced_ranger_env_properties['ranger_user'] = 'ranger'
			advanced_ranger_env_properties['ranger_group'] = 'ranger'
			advanced_ranger_env_properties['ranger_admin_log_dir'] = '/var/log/ranger/admin'
			advanced_ranger_env_properties['ranger_usersync_log_dir'] = '/var/log/ranger/usersync'
			advanced_ranger_env_properties['ranger_admin_username'] = 'amb_ranger_admin'
			advanced_ranger_env_properties['ranger_admin_password'] = 'ambari123'
			advanced_ranger_env_properties['admin_password'] = 'admin'

			ranger_config_data = '[{"Clusters":{"desired_config":[{"type":"admin-properties", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_admin_properties) + ', "properties_attributes": {"final": "true"}},{"type":"ranger-site", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_ranger_site_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-env", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_env_properties) + ', "properties_attributes": {"final": "false"}}]}}]'


			print ('####################### admin_properties configuration :')
			for each_key in advanced_admin_properties:
				print str(each_key) + ' = ' + str(advanced_admin_properties[each_key])

			print ('####################### ranger_site_properties configuration :')
			for each_key in advanced_ranger_site_properties:
				print str(each_key) + ' = ' + str(advanced_ranger_site_properties[each_key])

			print ('####################### ranger_env_properties configuration :')
			for each_key in advanced_ranger_env_properties:
				print str(each_key) + ' = ' + str(advanced_ranger_env_properties[each_key])


		else:
			print('either db_root_user or db_root_password value is missing from ranger_admin_install.properties file, please set appropriate value and run the script again.')
			sys.exit(1)

	elif (add_admin_or_usersync == 2):
		advanced_user_sync_properties['SYNC_SOURCE'] = ranger_service_properties_from_file['SYNC_SOURCE']
		advanced_user_sync_properties['MIN_UNIX_USER_ID_TO_SYNC'] = ranger_service_properties_from_file['usergroupSync.unix.minUserId']
		advanced_user_sync_properties['SYNC_INTERVAL'] = ranger_service_properties_from_file['usergroupSync.sleepTimeInMillisBetweenSyncCycle']
		advanced_user_sync_properties['SYNC_LDAP_URL'] = ranger_service_properties_from_file['ldapGroupSync.ldapUrl'] \
			if str(ranger_service_properties_from_file['SYNC_SOURCE']).strip().lower() == 'ldap'  else ' '
		advanced_user_sync_properties['SYNC_LDAP_BIND_DN'] = ranger_service_properties_from_file['ldapGroupSync.ldapBindDn'] \
			if str(ranger_service_properties_from_file['SYNC_SOURCE']).strip().lower() == 'ldap'  else ' '
		advanced_user_sync_properties['SYNC_LDAP_BIND_PASSWORD'] = ranger_service_properties_from_file['ldapGroupSync.ldapBindPassword'] \
			if str(ranger_service_properties_from_file['SYNC_SOURCE']).strip().lower() == 'ldap'  else ' '
		advanced_user_sync_properties['CRED_KEYSTORE_FILENAME'] = ranger_service_properties_from_file['ldapGroupSync.ldapBindKeystore']
		advanced_user_sync_properties['SYNC_LDAP_USER_SEARCH_BASE'] = ranger_service_properties_from_file['ldapGroupSync.userSearchBase'] \
			if str(ranger_service_properties_from_file['SYNC_SOURCE']).strip().lower() == 'ldap'  else ' '
		advanced_user_sync_properties['SYNC_LDAP_USER_SEARCH_SCOPE'] = ranger_service_properties_from_file['ldapGroupSync.userSearchScope']
		advanced_user_sync_properties['SYNC_LDAP_USER_OBJECT_CLASS'] = ranger_service_properties_from_file['ldapGroupSync.userObjectClass']
		advanced_user_sync_properties['SYNC_LDAP_USER_SEARCH_FILTER'] = ranger_service_properties_from_file['ldapGroupSync.userSearchFilter'] \
			if str(ranger_service_properties_from_file['SYNC_SOURCE']).strip().lower() == 'ldap'  else ' '
		advanced_user_sync_properties['SYNC_LDAP_USER_NAME_ATTRIBUTE'] = ranger_service_properties_from_file['ldapGroupSync.userNameAttribute']
		advanced_user_sync_properties['SYNC_LDAP_USER_GROUP_NAME_ATTRIBUTE'] = ranger_service_properties_from_file['ldapGroupSync.userGroupNameAttribute']
		advanced_user_sync_properties['SYNC_LDAP_USERNAME_CASE_CONVERSION'] = ranger_service_properties_from_file['ldapGroupSync.username.caseConversion']
		advanced_user_sync_properties['SYNC_LDAP_GROUPNAME_CASE_CONVERSION'] = ranger_service_properties_from_file['ldapGroupSync.groupname.caseConversion']
		advanced_user_sync_properties['logdir'] = ranger_service_properties_from_file['logdir']

		ranger_config_data = '[{"Clusters":{"desired_config":[{"type":"usersync-properties", "service_config_version_note": "Initial configuration for Ranger Usersync service" ,"tag":"' + str(
			date_time_stamp) + '","properties":' + json.dumps(advanced_user_sync_properties) + ', "properties_attributes": {"final": "false"}}]}}]'

		print ('####################### user_sync_properties configuration :')
		for each_key in advanced_user_sync_properties:
			print str(each_key) + ' = ' + str(advanced_user_sync_properties[each_key])

	else:
		print ('invalid option for to add configuration to ranger.')
		sys.exit(1)


	confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
	if(confirm_configurations == ''):
		confirm_configurations = 'n'
	print ('input registered as ' + str(confirm_configurations))

	if(confirm_configurations.lower() == 'y'):
		ranger_config_request_url = ambari_cluster_url
		request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(ranger_config_data))
		if request_result is not None:
			response_code = request_result.getcode()
			response = json.loads(json.JSONEncoder().encode(request_result.read()))
			if response_code == 200 and response is not None:
				print('ranger advanced configuration added successfully in ambari.')
				return True
			else:
				print('ranger advanced configuration add failed in ambari.')
				return False
		else:
			print('ranger advanced configuration add failed in ambari.')
			return False
	else:
		print ('exiting installation without configuration !')
		sys.exit(0)


def call_ranger_installed():
	print('changing state of ranger services from init to installed.')
	ranger_state_change_request = '{"RequestInfo":{"context":"Install Ranger Service","operation_level":{"level":"CLUSTER","cluster_name":"' + str(
		cluster_name) + '"}},"Body":{"ServiceInfo":{"state":"INSTALLED"}}}'
	ranger_state_change_url = ambari_service_url + '?ServiceInfo/state=INIT'
	request_result = call_ambari_api(ranger_state_change_url, 'PUT', ambari_username_password, ranger_state_change_request)
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if (response_code == 200 and response is not None):
			print('ranger state changed to install successfully in ambari.')
			return True
		if (response_code == 409 and response is not None):
			print('ranger is already installed in ambari.')
			return True
		if response_code == 202 and response is not None:
			print('ranger state changed to install posted in ambari, checking for updated status waiting for 30 seconds')
			parsed_response = json.loads(response)
			response_href_url = parsed_response['href']
			response_request_id = parsed_response['Requests']['id']
			response_status = parsed_response['Requests']['status']
			if response_status != 'Installed':
				print('Received response but status is not installed, verifying installation to be successful.')
				flag_ranger_installed = True
				time.sleep(30)
				while flag_ranger_installed:
					print('checking request status')
					ambari_request_url = ambari_cluster_url + '/requests/' + str(response_request_id)
					request_status_result = call_ambari_api(ambari_request_url, 'GET', ambari_username_password, '')
					if request_status_result is not None:
						response_code = request_status_result.getcode()
						response_status = json.loads(json.JSONEncoder().encode(request_status_result.read()))
						if (response_code == 200 and 'FAILED' in response_status):
							print('ranger install failed in ambari.')
							flag_ranger_installed = True
						if (response_code == 200 ):
							print('ranger install is pending in ambari.')
							flag_ranger_installed = True
						if (response_code == 200 and ('PENDING' in response_status or 'INTERNAL_REQUEST' in response_status)):
							print('ranger install is pending in ambari.')
							flag_ranger_installed = True
						if (response_code == 200 and 'COMPLETED' in response_status):
							print('ranger installed successfully in ambari.')
							flag_ranger_installed = False
						else:
							flag_ranger_installed = True
				return not flag_ranger_installed
		else:
			print('ranger state changed to install failed in ambari.')
			return False


def get_ranger_usersync_install_properties():
	print('preparing advanced configurations for ranger User-sync')
	flag_hadoop_present = check_hadoop_dir_present(hdp_current_dir)
	flag_ranger_usersync_present = False
	ranger_usersync_current_dir = os.path.join(hdp_current_dir, 'ranger-usersync')
	ranger_usersync_properties_from_file = dict()
	print('checking for ranger-usersync service to be present')
	flag_ranger_usersync_present, ranger_usersync__installed_version = check_ranger_usersync_install(ranger_usersync_current_dir)
	if flag_ranger_usersync_present:
		print('ranger usersync is installed, getting existing properties for ambari import')
		print('got ranger usersync values from install.properties file, need to configure ambari for ranger service.')
		ranger_usersync_properties_from_file = get_additional_properties_for_usersync(ranger_usersync_properties_from_file)
	return flag_ranger_usersync_present, ranger_usersync_properties_from_file


def get_ranger_admin_install_properties():
	ranger_admin_current_dir = os.path.join(hdp_current_dir, 'ranger-admin')
	flag_hadoop_present = check_hadoop_dir_present(hdp_current_dir)
	flag_ranger_admin_present = False
	ranger_admin_properties_from_file = dict()

	if flag_hadoop_present:
		print('hadoop directory is present, checking ranger admin installation.')
		flag_ranger_admin_present, ranger_admin_installed_version = check_ranger_admin_install(ranger_admin_current_dir)

		if flag_ranger_admin_present:
			print('ranger admin is present.getting existing configurations to port to ambari.')
			if os.path.isfile("ranger_admin_install.properties"):
				print('ranger_install_properties_path exists, getting existing properties for ambari port')
				ranger_admin_properties_from_file = import_properties_from_file("ranger_admin_install.properties")
				if not (ranger_admin_properties_from_file['db_root_user'] == '' or ranger_admin_properties_from_file['db_root_password'] == ''):
					print 'db_root_username and db_root_password are not blank.'
					print 'value for db_root_user = ' + str(ranger_admin_properties_from_file['db_root_user'])
					print 'value for db_root_password = ' + str(ranger_admin_properties_from_file['db_root_password'])
				else:
					print 'db_root_username or db_root_password are blank, please provide proper values in ranger_admin_install.properties. exiting installation without any changes.'
					sys.exit(1)

				print('got ranger admin values from ranger_admin_install.properties file, need to configure ambari for ranger service.')
				print('getting additional properties required by ranger services')
				ranger_admin_properties_from_file = get_additional_properties_for_admin(ranger_admin_properties_from_file)

	return flag_ranger_admin_present, ranger_admin_properties_from_file


def check_hadoop_dir_present(hdp_current_dir_path):
	flag_hadoop_dir_present = False
	if os.path.isdir(hdp_current_dir_path):
		print('hadoop is installed.')
		flag_hadoop_dir_present = True
	return flag_hadoop_dir_present


def check_ranger_admin_install(ranger_admin_current_dir):
	flag_ranger_dir_present = False
	ranger_current_installed_version = ''
	print('checking ranger service path folder')
	if os.path.isdir(ranger_admin_current_dir):
		print('ranger admin is installed.')
		if os.path.islink(ranger_admin_current_dir):
			flag_ranger_dir_present = True
			print('ranger admin link found getting current version from link.')
			ranger_home_path = os.path.realpath(ranger_admin_current_dir)
			ranger_current_installed_version = ranger_home_path.split('/')[4]
	return flag_ranger_dir_present, ranger_current_installed_version


def check_ranger_usersync_install(ranger_usersync_current_dir):
	flag_ranger_dir_present = False
	ranger_current_installed_version = ''
	print('checking ranger service path folder')
	if os.path.isdir(ranger_usersync_current_dir):
		print('ranger user-sync is installed.')
		if os.path.islink(ranger_usersync_current_dir):
			flag_ranger_dir_present = True
			print('ranger admin link found getting current version from link.')
			ranger_home_path = os.path.realpath(ranger_usersync_current_dir)
			ranger_current_installed_version = ranger_home_path.split('/')[4]
	return flag_ranger_dir_present, ranger_current_installed_version


def get_additional_properties_for_admin(ranger_admin_properties_from_file):
	ranger_conf_path = '/etc/ranger/admin/conf'
	ranger_webserver_properties_path = os.path.join(ranger_conf_path, 'ranger_webserver.properties')
	ranger_ldap_properties_path = os.path.join(ranger_conf_path, 'xa_ldap.properties')
	ranger_system_properties_path = os.path.join(ranger_conf_path, 'xa_system.properties')
	ranger_unixauth_properties_path = os.path.join(ranger_conf_path, 'ranger_jaas', 'unixauth.properties')
        try:
	    ranger_admin_properties_from_file = import_properties_from_file(ranger_webserver_properties_path, ranger_admin_properties_from_file)
	    ranger_admin_properties_from_file = import_properties_from_file(ranger_ldap_properties_path, ranger_admin_properties_from_file)
	    ranger_admin_properties_from_file = import_properties_from_file(ranger_system_properties_path, ranger_admin_properties_from_file)
	    ranger_admin_properties_from_file = import_properties_from_file(ranger_unixauth_properties_path, ranger_admin_properties_from_file)
        except Exception, e:
            print "Error loading property files: ", str(e)

	print('getting db flavor, library and command invoker')
	xa_system_properties_db_dialect = ranger_admin_properties_from_file['jdbc.dialect']
	print('xa_system_properties_db_dialect = ' + xa_system_properties_db_dialect)
	xa_system_properties_url = ranger_admin_properties_from_file['jdbc.url']
	print('xa_system_properties_url = ' + xa_system_properties_url)
	if ('mysql'.lower() in xa_system_properties_db_dialect.lower() and 'mysql'.lower() in xa_system_properties_url.lower()):
		print('db dialect and jdbc url are set as MYSQL setting db_flavour and sql command invoker as mysql')
		ranger_admin_properties_from_file['DB_FLAVOR'] = 'MYSQL'
		ranger_admin_properties_from_file['SQL_COMMAND_INVOKER'] = 'mysql'
		ranger_admin_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/mysql-connector-java.jar'
	elif ('oracle'.lower() in xa_system_properties_db_dialect and 'oracle'.lower() in xa_system_properties_url.lower()):
		print('db dialect and jdbc url are set as Oracle setting db_flavour and sql command invoker as oracle')
		ranger_admin_properties_from_file['DB_FLAVOR'] = 'ORACLE'
		ranger_admin_properties_from_file['SQL_COMMAND_INVOKER'] = 'sqlplus'
		ranger_admin_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/ojdbc6.jar'
	else:
		print('found unsupported DB_FLAVOUR, please configure as MYSQL or ORACLE, which are supported for now.exitting for now')
		sys.exit(1)

	xa_system_properties_jdbc_url = ranger_admin_properties_from_file['jdbc.url']
	print('found jdbc url configured as : ' + str(xa_system_properties_jdbc_url) + ' , getting db host from configured jdbc url')
	xa_database_host_name = xa_system_properties_jdbc_url.split(':')
	xa_database_host = xa_database_host_name[3].split('/')[2]
	xa_database_name = xa_database_host_name[3].split('/')[3]
	print('found db host as : ' + str(xa_database_host))
	print('found db name as : ' + str(xa_database_name))
	ranger_admin_properties_from_file['db_host'] = xa_database_host
	ranger_admin_properties_from_file['db_name'] = xa_database_name

	xa_system_properties_audit_jdbc_url = ranger_admin_properties_from_file['auditDB.jdbc.url']
	print('found audit jdbc url configured as : ' + str(xa_system_properties_audit_jdbc_url) + ' , getting db host from configured jdbc url')
	xa_audit_database_host_name = xa_system_properties_audit_jdbc_url.split(':')

	xa_audit_database_host = xa_audit_database_host_name[3].split('/')[2]
	xa_audit_database_name = xa_audit_database_host_name[3].split('/')[3]
	print('found xa_audit_database_name as : ' + str(xa_audit_database_name))
	ranger_admin_properties_from_file['audit_db_host'] = xa_audit_database_host
	ranger_admin_properties_from_file['audit_db_name'] = xa_audit_database_name

	xa_db_password = ''
	xa_audit_db_password = ''

	libpath = os.path.join(hdp_version_dir, 'ranger-admin', 'cred', 'lib', '*')
	aliasKey = 'policydb.jdbc.password'
	aliasValue = ''
	filepath = os.path.join(hdp_version_dir, 'ranger-admin', 'ews', 'webapp', 'WEB-INF', 'classes', 'conf', '.jceks', 'rangeradmin.jceks')

	getorcreateorlist = 'get'

	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
	if statuscode == 0:
		xa_db_password = value.strip()

	aliasKey = 'auditdb.jdbc.password'
	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
	if statuscode == 0:
		xa_audit_db_password = value.strip()

	ranger_admin_properties_from_file['jdbc.password'] = xa_db_password
	ranger_admin_properties_from_file['auditDB.jdbc.password'] = xa_audit_db_password
	return ranger_admin_properties_from_file


def get_additional_properties_for_usersync(ranger_usersync_properties_from_file):
	ranger_conf_path = '/etc/ranger/usersync/conf'
	unix_auth_properties_path = os.path.join(ranger_conf_path, 'unixauthservice.properties')
	ranger_usersync_properties_from_file = import_properties_from_file(unix_auth_properties_path, ranger_usersync_properties_from_file)
	if (('unix'.lower()) in str(ranger_usersync_properties_from_file['usergroupSync.source.impl.class']).lower()):
		print('sync_source is unix')
		ranger_usersync_properties_from_file['SYNC_SOURCE'] = 'unix'
	if (('ldap'.lower()) in str(ranger_usersync_properties_from_file['usergroupSync.source.impl.class']).lower()):
		print('sync source is ldap')
		ranger_usersync_properties_from_file['SYNC_SOURCE'] = 'ldap'
	return ranger_usersync_properties_from_file


def port_ranger_hdfs_plugin_to_ambari():
	print('Trying to add ranger hdfs plugin.')
	flag_hdfs_plugin_installed, hdfs_plugin_install_properties, hdfs_site_xml_properties = get_hdfs_plugin_configuration()
	if flag_hdfs_plugin_installed and hdfs_plugin_install_properties is not None and hdfs_site_xml_properties is not None:
		hdfs_site_xml_properties['dfs.permissions.enabled'] = 'true'
		print('hdfs plugin is installed and enabled, adding to configurations')
		advanced_ranger_hdfs_plugin_properties = dict()

		advanced_ranger_hdfs_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hdfs_plugin_properties['hadoop.rpc.protection'] = '-'
		advanced_ranger_hdfs_plugin_properties['common.name.for.certificate'] = '-'
		advanced_ranger_hdfs_plugin_properties['ranger-hdfs-plugin-enabled'] = 'Yes'
		advanced_ranger_hdfs_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hadoop'
		advanced_ranger_hdfs_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hadoop'
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.DB.IS_ENABLED'] = hdfs_plugin_install_properties['xasecure.audit.db.is.enabled']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.IS_ENABLED'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.is.enabled']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.destination.directory']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.directory']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.directory']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.destination.file']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.destination.flush.interval.seconds']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = hdfs_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.rollover.interval.seconds']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = hdfs_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.open.retry.interval.seconds']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.file']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = hdfs_plugin_install_properties[
			'xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds']
		advanced_ranger_hdfs_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = hdfs_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.max.file.count']
		advanced_ranger_hdfs_plugin_properties['SSL_KEYSTORE_FILE_PATH'] = hdfs_plugin_install_properties['xasecure.policymgr.clientssl.keystore']
		advanced_ranger_hdfs_plugin_properties['SSL_TRUSTSTORE_FILE_PATH'] = hdfs_plugin_install_properties['xasecure.policymgr.clientssl.truststore']

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hdfs-site", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hdfs_site_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hdfs-plugin-properties", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hdfs_plugin_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### hdfs_site_xml configuration :')
		for each_key in hdfs_site_xml_properties:
			print str(each_key) + ' = ' + str(hdfs_site_xml_properties[each_key])

		print ('####################### ranger_hdfs_plugin_properties configuration :')
		for each_key in advanced_ranger_hdfs_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hdfs_plugin_properties[each_key])

		confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
		if(confirm_configurations == ''):
			confirm_configurations = 'n'
		print ('Input registered as ' + str(confirm_configurations))


		if(confirm_configurations.lower() == 'y'):
			ranger_config_request_url = ambari_cluster_url
			request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(plugin_configuration_data))
			if request_result is not None:
				response_code = request_result.getcode()
				response = json.loads(json.JSONEncoder().encode(request_result.read()))
				if response_code == 200 and response is not None:
					print('ranger hdfs plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger hdfs plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger hdfs plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger hdfs plugin configuration add failed in ambari.')
		return False



def port_ranger_hive_plugin_to_ambari():
	print('Trying to add ranger hive plugin.')
	flag_hive_plugin_installed, hive_plugin_install_properties, hive_server2_xml_properties = get_hive_plugin_configuration()
	if flag_hive_plugin_installed and hive_plugin_install_properties is not None and hive_server2_xml_properties is not None:
		hive_server2_xml_properties['hive.security.authorization.enabled'] = 'true'
		hive_server2_xml_properties['hive.security.authorization.manager'] = 'com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory'
		hive_server2_xml_properties['hive.security.authenticator.manager'] = 'org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator'
		hive_server2_xml_properties['hive.conf.restricted.list'] = 'hive.security.authorization.enabled, hive.security.authorization.manager,hive.security.authenticator.manager'
		print('hive plugin is installed and enabled, adding to configurations')
		advanced_ranger_hive_plugin_properties = dict()

		advanced_ranger_hive_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hive_plugin_properties['jdbc.driverClassName'] = 'org.apache.hive.jdbc.HiveDriver'
		advanced_ranger_hive_plugin_properties['common.name.for.certificate'] = '-'
		advanced_ranger_hive_plugin_properties['ranger-hive-plugin-enabled'] = 'Yes'
		advanced_ranger_hive_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hive'
		advanced_ranger_hive_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hive'
		advanced_ranger_hive_plugin_properties['XAAUDIT.DB.IS_ENABLED'] = hive_plugin_install_properties['xasecure.audit.db.is.enabled']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.IS_ENABLED'] = hive_plugin_install_properties['xasecure.audit.hdfs.is.enabled']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.destination.directory']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.directory']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.directory']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.destination.file']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.destination.flush.interval.seconds']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = hive_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.rollover.interval.seconds']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = hive_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.open.retry.interval.seconds']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.file']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = hive_plugin_install_properties[
			'xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds']
		advanced_ranger_hive_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = hive_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.max.file.count']
		advanced_ranger_hive_plugin_properties['SSL_KEYSTORE_FILE_PATH'] = hive_plugin_install_properties['xasecure.policymgr.clientssl.keystore']
		advanced_ranger_hive_plugin_properties['SSL_TRUSTSTORE_FILE_PATH'] = hive_plugin_install_properties['xasecure.policymgr.clientssl.truststore']
		advanced_ranger_hive_plugin_properties['UPDATE_XAPOLICIES_ON_GRANT_REVOKE'] = hive_plugin_install_properties['xasecure.hive.update.xapolicies.on.grant.revoke']

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hiveserver2-site", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hive_server2_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hive-plugin-properties", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hive_plugin_properties) + ',"properties_attributes": {"final": "false"}}]}}]'


		print ('####################### hive_server2_xml configuration :')
		for each_key in hive_server2_xml_properties:
			print str(each_key) + ' = ' + str(hive_server2_xml_properties[each_key])

		print ('####################### ranger_hive_plugin_properties configuration :')
		for each_key in advanced_ranger_hive_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hive_plugin_properties[each_key])

		confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
		if(confirm_configurations == ''):
			confirm_configurations = 'n'
		print ('Input registered as ' + str(confirm_configurations))


		if(confirm_configurations.lower() == 'y'):
			ranger_config_request_url = ambari_cluster_url
			request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(plugin_configuration_data))
			if request_result is not None:
				response_code = request_result.getcode()
				response = json.loads(json.JSONEncoder().encode(request_result.read()))
				if response_code == 200 and response is not None:
					print('ranger hive plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger hive plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger hive plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger hive plugin configuration add failed in ambari.')
		return False



def port_ranger_hbase_plugin_to_ambari():
	print('Trying to add ranger Hbase plugin.')
	flag_hbase_plugin_installed, hbase_plugin_install_properties, hbase_site_xml_properties = get_hbase_plugin_configuration()
	if flag_hbase_plugin_installed and hbase_plugin_install_properties is not None and hbase_site_xml_properties is not None:
		print('Hbase plugin is installed and enabled, adding to configurations')
		advanced_ranger_hbase_plugin_properties = dict()

		advanced_ranger_hbase_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hbase_plugin_properties['common.name.for.certificate'] = '-'
		advanced_ranger_hbase_plugin_properties['ranger-hbase-plugin-enabled'] = 'Yes'
		advanced_ranger_hbase_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hbase'
		advanced_ranger_hbase_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hbase'
		advanced_ranger_hbase_plugin_properties['XAAUDIT.DB.IS_ENABLED'] = hbase_plugin_install_properties['xasecure.audit.db.is.enabled']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.IS_ENABLED'] = hbase_plugin_install_properties['xasecure.audit.hdfs.is.enabled']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.destination.directory']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.directory']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.directory']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.destination.file']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.destination.flush.interval.seconds']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = hbase_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.rollover.interval.seconds']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = hbase_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.open.retry.interval.seconds']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.file']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = hbase_plugin_install_properties[
			'xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds']
		advanced_ranger_hbase_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = hbase_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.max.file.count']
		advanced_ranger_hbase_plugin_properties['SSL_KEYSTORE_FILE_PATH'] = hbase_plugin_install_properties['xasecure.policymgr.clientssl.keystore']
		advanced_ranger_hbase_plugin_properties['SSL_TRUSTSTORE_FILE_PATH'] = hbase_plugin_install_properties['xasecure.policymgr.clientssl.truststore']
		advanced_ranger_hbase_plugin_properties['UPDATE_XAPOLICIES_ON_GRANT_REVOKE'] = hbase_plugin_install_properties['xasecure.hbase.update.xapolicies.on.grant.revoke']

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hbase-site", "service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hbase_site_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hbase-plugin-properties","service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hbase_plugin_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### hbase_site_xml configuration :')
		for each_key in hbase_site_xml_properties:
			print str(each_key) + ' = ' + str(hbase_site_xml_properties[each_key])

		print ('####################### ranger_hbase_plugin_properties configuration :')
		for each_key in advanced_ranger_hbase_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hbase_plugin_properties[each_key])

		confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
		if(confirm_configurations == ''):
			confirm_configurations = 'n'
		print ('Input registered as ' + str(confirm_configurations))


		if(confirm_configurations.lower() == 'y'):
			ranger_config_request_url = ambari_cluster_url
			request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(plugin_configuration_data))
			if request_result is not None:
				response_code = request_result.getcode()
				response = json.loads(json.JSONEncoder().encode(request_result.read()))
				if response_code == 200 and response is not None:
					print('ranger hbase plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger hbase plugin configuration add failed in ambari.')
					return False
			else:
				print ('ranger hbase plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print ('ranger hbase plugin configuration add failed in ambari.')
		return False



def port_ranger_knox_plugin_to_ambari():
	print('trying to add ranger knox plugin.')
	flag_knox_plugin_installed, knox_plugin_install_properties = get_knox_plugin_configuration()
	if flag_knox_plugin_installed and knox_plugin_install_properties is not None:
		print('Knox plugin is installed and enabled, adding to configurations')
		advanced_ranger_knox_plugin_properties = dict()

		advanced_ranger_knox_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_knox_plugin_properties['common.name.for.certificate'] = '-'
		advanced_ranger_knox_plugin_properties['ranger-knox-plugin-enabled'] = 'Yes'
		advanced_ranger_knox_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'admin'
		advanced_ranger_knox_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'admin-password'
		advanced_ranger_knox_plugin_properties['KNOX_HOME'] = '/usr/hdp/current/knox-server'
		advanced_ranger_knox_plugin_properties['XAAUDIT.DB.IS_ENABLED'] = knox_plugin_install_properties['xasecure.audit.db.is.enabled']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.IS_ENABLED'] = knox_plugin_install_properties['xasecure.audit.hdfs.is.enabled']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.destination.directory']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.directory']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.directory']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.destination.file']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.destination.flush.interval.seconds']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = knox_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.rollover.interval.seconds']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = knox_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.open.retry.interval.seconds']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.file']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = knox_plugin_install_properties[
			'xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds']
		advanced_ranger_knox_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = knox_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.max.file.count']

		knox_ssl_keystore_password = ''
		knox_ssl_truststore_password = ''

		libpath = os.path.join(hdp_version_dir, 'ranger-knox-plugin', 'install', 'lib', '*')
		aliasKey = 'sslkeystore'
		aliasValue = ''
		filepath = os.path.join('/etc/ranger', knox_plugin_install_properties['xasecure.audit.repository.name'], 'cred.jceks')
		getorcreateorlist = 'get'

		statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
		if statuscode == 0:
			knox_ssl_keystore_password = value.strip()

		aliasKey = 'ssltruststore'
		statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
		if statuscode == 0:
			knox_ssl_truststore_password = value.strip()

		advanced_ranger_knox_plugin_properties['SSL_KEYSTORE_FILE_PATH'] = knox_plugin_install_properties['xasecure.policymgr.clientssl.keystore']
		advanced_ranger_knox_plugin_properties['SSL_KEYSTORE_PASSWORD'] = knox_ssl_keystore_password
		advanced_ranger_knox_plugin_properties['SSL_TRUSTSTORE_FILE_PATH'] = knox_plugin_install_properties['xasecure.policymgr.clientssl.truststore']
		advanced_ranger_knox_plugin_properties['SSL_TRUSTSTORE_PASSWORD'] = knox_ssl_truststore_password

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type": "ranger-knox-plugin-properties", "service_config_version_note": "Initial configuration for Ranger KNOX plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_knox_plugin_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### ranger_knox_plugin_properties configuration :')
		for each_key in advanced_ranger_knox_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_knox_plugin_properties[each_key])

		confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
		if(confirm_configurations == ''):
			confirm_configurations = 'n'
		print ('input registered as ' + str(confirm_configurations))


		if(confirm_configurations.lower() == 'y'):
			ranger_config_request_url = ambari_cluster_url
			request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(plugin_configuration_data))
			if request_result is not None:
				response_code = request_result.getcode()
				response = json.loads(json.JSONEncoder().encode(request_result.read()))
				if response_code == 200 and response is not None:
					print('ranger knox plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger knox plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger knox plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger knox plugin configuration add failed in ambari.')
		return False


def port_ranger_storm_plugin_to_ambari():
	print('Trying to add ranger storm plugin.')
	flag_storm_plugin_installed, storm_plugin_install_properties = get_storm_plugin_configuration()
	if flag_storm_plugin_installed and storm_plugin_install_properties is not None:
		print('Storm plugin is installed and enabled, adding to configurations')
		advanced_ranger_storm_plugin_properties = dict()

		advanced_ranger_storm_plugin_properties['policy_user'] = 'storm'
		advanced_ranger_storm_plugin_properties['common.name.for.certificate'] = '-'
		advanced_ranger_storm_plugin_properties['ranger-storm-plugin-enabled'] = 'Yes'
		advanced_ranger_storm_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'stormtestuser@EXAMPLE.COM'
		advanced_ranger_storm_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'stormtestuser'
		advanced_ranger_storm_plugin_properties['XAAUDIT.DB.IS_ENABLED'] = storm_plugin_install_properties['xasecure.audit.db.is.enabled']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.IS_ENABLED'] = storm_plugin_install_properties['xasecure.audit.hdfs.is.enabled']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.destination.directory']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.directory']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.directory']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.destination.file']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.destination.flush.interval.seconds']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = storm_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.rollover.interval.seconds']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = storm_plugin_install_properties[
			'xasecure.audit.hdfs.config.destination.open.retry.interval.seconds']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.file']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = storm_plugin_install_properties[
			'xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds']
		advanced_ranger_storm_plugin_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = storm_plugin_install_properties['xasecure.audit.hdfs.config.local.archive.max.file.count']
		advanced_ranger_storm_plugin_properties['SSL_KEYSTORE_FILE_PATH'] = storm_plugin_install_properties['xasecure.policymgr.clientssl.keystore']
		advanced_ranger_storm_plugin_properties['SSL_TRUSTSTORE_FILE_PATH'] = storm_plugin_install_properties['xasecure.policymgr.clientssl.truststore']

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type": "ranger-storm-plugin-properties", "service_config_version_note": "Initial configuration for Ranger STORM plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_storm_plugin_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### ranger_storm_plugin_properties configuration :')
		for each_key in advanced_ranger_storm_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_storm_plugin_properties[each_key])

		confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
		if(confirm_configurations == ''):
			confirm_configurations = 'n'
		print ('Input registered as ' + str(confirm_configurations))


		if(confirm_configurations.lower() == 'y'):
			ranger_config_request_url = ambari_cluster_url
			request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(plugin_configuration_data))
			if request_result is not None:
				response_code = request_result.getcode()
				response = json.loads(json.JSONEncoder().encode(request_result.read()))
				if response_code == 200 and response is not None:
					print('ranger storm plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger storm plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger storm plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger storm plugin configuration add failed in ambari.')
		return False



def get_hdfs_plugin_configuration():
	flag_hdfs_plugin_installed = False
	hdfs_plugin_install_properties = dict()
	print('hdfs plugin is present and installed to ranger,getting additional properties from installed files .')
	base_hadoop_conf_path = '/etc/hadoop/conf/'
	hdfs_site_xml_path = os.path.join(base_hadoop_conf_path, 'hdfs-site.xml')
	hdfs_site_xml_properties = import_properties_from_xml(hdfs_site_xml_path)
	xasecure_audit_xml_path = os.path.join(base_hadoop_conf_path, 'xasecure-audit.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(xasecure_audit_xml_path, hdfs_plugin_install_properties)
	xasecure_hdfs_security_xml_path = os.path.join(base_hadoop_conf_path, 'xasecure-hdfs-security.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(xasecure_hdfs_security_xml_path, hdfs_plugin_install_properties)
	xasecure_policy_ssl_xml_path = os.path.join(base_hadoop_conf_path, 'xasecure-policymgr-ssl.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(xasecure_policy_ssl_xml_path, hdfs_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hdfs', hdfs_plugin_install_properties)
	if(flag_plugin_installed):
		flag_hdfs_plugin_installed = True
	return flag_hdfs_plugin_installed, hdfs_plugin_install_properties, hdfs_site_xml_properties


def get_hive_plugin_configuration():
	flag_hive_plugin_installed = False
	hive_plugin_install_properties = dict()
	print('hive plugin is present and installed to ranger, configuring to setup in ambari.')
	base_hive_conf_path = '/etc/hive/conf/'
	hive_server2_xml_path = os.path.join(base_hive_conf_path, 'hiveserver2-site.xml')
	hive_server2_xml_properties = import_properties_from_xml(hive_server2_xml_path)
	xasecure_audit_xml_path = os.path.join(base_hive_conf_path, 'xasecure-audit.xml')
	hive_plugin_install_properties = import_properties_from_xml(xasecure_audit_xml_path, hive_plugin_install_properties)
	xasecure_hive_security_xml_path = os.path.join(base_hive_conf_path, 'xasecure-hive-security.xml')
	hive_plugin_install_properties = import_properties_from_xml(xasecure_hive_security_xml_path, hive_plugin_install_properties)
	xasecure_policy_ssl_xml_path = os.path.join(base_hive_conf_path, 'xasecure-policymgr-ssl.xml')
	hive_plugin_install_properties = import_properties_from_xml(xasecure_policy_ssl_xml_path, hive_plugin_install_properties)
	hive_security_xml_path = os.path.join(base_hive_conf_path, 'xasecure-hive-security.xml')
	hive_plugin_install_properties = import_properties_from_xml(hive_security_xml_path, hive_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hive', hive_plugin_install_properties)
	if(flag_plugin_installed):
		flag_hive_plugin_installed = True
	return flag_hive_plugin_installed, hive_plugin_install_properties, hive_server2_xml_properties


def get_hbase_plugin_configuration():
	flag_hbase_plugin_installed = False
	hbase_plugin_install_properties = dict()
	print('hbase plugin is present and installed to ranger, configuring to setup in ambari.')
	base_hbase_conf_path = '/etc/hbase/conf/'
	hbase_site_xml_path = os.path.join(base_hbase_conf_path, 'hbase-site.xml')
	hbase_site_xml_properties = import_properties_from_xml(hbase_site_xml_path)
	xasecure_audit_xml_path = os.path.join(base_hbase_conf_path, 'xasecure-audit.xml')
	hbase_plugin_install_properties = import_properties_from_xml(xasecure_audit_xml_path, hbase_plugin_install_properties)
	xasecure_hbase_security_xml_path = os.path.join(base_hbase_conf_path, 'xasecure-hbase-security.xml')
	hbase_plugin_install_properties = import_properties_from_xml(xasecure_hbase_security_xml_path, hbase_plugin_install_properties)
	xasecure_policy_ssl_xml_path = os.path.join(base_hbase_conf_path, 'xasecure-policymgr-ssl.xml')
	hbase_plugin_install_properties = import_properties_from_xml(xasecure_policy_ssl_xml_path, hbase_plugin_install_properties)
	hbase_security_xml_path = os.path.join(base_hbase_conf_path, 'xasecure-hbase-security.xml')
	hbase_plugin_install_properties = import_properties_from_xml(hbase_security_xml_path, hbase_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hbase', hbase_plugin_install_properties)
	if(flag_plugin_installed):
		flag_hbase_plugin_installed = True
	return flag_hbase_plugin_installed, hbase_plugin_install_properties, hbase_site_xml_properties


def get_knox_plugin_configuration():
	flag_knox_plugin_installed = False
	knox_plugin_install_properties = dict()
	print('knox plugin is present and installed to ranger, configuring to setup in ambari.')
	base_knox_conf_path = '/etc/knox/conf/'
	xasecure_audit_xml_path = os.path.join(base_knox_conf_path, 'xasecure-audit.xml')
	knox_plugin_install_properties = import_properties_from_xml(xasecure_audit_xml_path, knox_plugin_install_properties)
	xasecure_knox_security_xml_path = os.path.join(base_knox_conf_path, 'xasecure-knox-security.xml')
	knox_plugin_install_properties = import_properties_from_xml(xasecure_knox_security_xml_path, knox_plugin_install_properties)
	xasecure_policy_ssl_xml_path = os.path.join(base_knox_conf_path, 'xasecure-policymgr-ssl.xml')
	knox_plugin_install_properties = import_properties_from_xml(xasecure_policy_ssl_xml_path, knox_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('knox', knox_plugin_install_properties)
	if(flag_plugin_installed):
		flag_knox_plugin_installed = True
	return flag_knox_plugin_installed, knox_plugin_install_properties



def get_storm_plugin_configuration():
	flag_storm_plugin_installed = False
	storm_plugin_install_properties = dict()
	print('storm plugin is present and installed to ranger, configuring to setup in ambari.')
	base_knox_conf_path = '/etc/storm/conf/'
	xasecure_audit_xml_path = os.path.join(base_knox_conf_path, 'xasecure-audit.xml')
	storm_plugin_install_properties = import_properties_from_xml(xasecure_audit_xml_path, storm_plugin_install_properties)
	xasecure_storm_security_xml_path = os.path.join(base_knox_conf_path, 'xasecure-storm-security.xml')
	storm_plugin_install_properties = import_properties_from_xml(xasecure_storm_security_xml_path, storm_plugin_install_properties)
	xasecure_policy_ssl_xml_path = os.path.join(base_knox_conf_path, 'xasecure-policymgr-ssl.xml')
	storm_plugin_install_properties = import_properties_from_xml(xasecure_policy_ssl_xml_path, storm_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('storm', storm_plugin_install_properties)
	if(flag_plugin_installed):
		flag_storm_plugin_installed = True
	return flag_storm_plugin_installed, storm_plugin_install_properties




def check_plugin_enabled(component_name, component_plugin_install_properties):
	flag_plugin_installed = False
	if not (str(component_plugin_install_properties['xasecure.audit.repository.name']).strip() == ''):
		repo_base_path = os.path.join('/etc/ranger', component_plugin_install_properties['xasecure.audit.repository.name'])
		print('repo_base_path = ' + str(repo_base_path))
		if os.path.exists(repo_base_path):
			print('Plugin is installed for component ' + component_name)
			flag_plugin_installed = True
	return flag_plugin_installed


def call_ambari_api(ambari_url, method, username_password, data):
	try:
		url = ambari_url
		base64string = base64.encodestring('{0}'.format(username_password)).replace('\n', '')
		headers = {"X-Requested-By": "ambari"}
		request = urllib2.Request(url, data, headers, 'compressed')
		request.get_method = lambda: method
		request.add_header("Authorization", "Basic {0}".format(base64string))
		result = urllib2.urlopen(request)
		return result
	except urllib2.URLError, e:
		if isinstance(e, urllib2.HTTPError):
			print("HTTP Code: {0}".format(e.code))
			print("HTTP Data: {0}".format(e.read()))
			return e
		else:
			print("Error: {0}".format(e.reason))
			print ('ambari server is not reachable, please make sure valid ambari server url has been provided and ambari server is started.')
			return e
	except httplib.BadStatusLine:
		print("ambari service is not reachable, please restart the service and then try again")
		return None


def import_properties_from_file(install_properties_path, properties_from_file=None):
	if properties_from_file is None:
		print('properties_from_file is none initializing to dict')
		properties_from_file = dict()
	if os.path.isfile(install_properties_path):
		install_properties_file = open(install_properties_path)
		for each_line in install_properties_file.read().split('\n'):
			each_line = each_line.strip()
			if len(each_line) == 0: continue
			if '#https.service.port' in each_line:
				each_line = each_line.strip('#')
			if '#' in each_line: continue
			key, value = each_line.strip().split("=", 1)
			key = key.strip()
			value = value.strip()
			properties_from_file[key] = value
	else:
		print('Property file not found at path : ' + str(install_properties_path))
	return properties_from_file


def import_properties_from_xml(xml_path, properties_from_xml=None):
	print('getting values from file : ' + str(xml_path))
	if os.path.isfile(xml_path):
		xml = ET.parse(xml_path)
		root = xml.getroot()
		if properties_from_xml is None:
			properties_from_xml = dict()
		for child in root.findall('property'):
			name = child.find("name").text.strip()
			value = child.find("value").text.strip() if child.find("value").text is not None  else ""
			properties_from_xml[name] = value
	else:
		print('XML file not found at path : ' + str(xml_path))
	return properties_from_xml


def get_authentication_method():
	print('Getting authentication method for ranger services')
	ranger_conf_path = '/etc/ranger/admin/conf'
	security_appln_context_path = os.path.join(ranger_conf_path,'security-applicationContext.xml')
	print ('security_appln_context_path = ' + security_appln_context_path)
	app_context_xml_tree = ET.parse(security_appln_context_path)
	app_context_xml_root = app_context_xml_tree.getroot()
	reference_auth_method = None
	authentication_method = None
	for child_nodes in app_context_xml_root.getiterator():
		if( ('authentication-provider' in str(child_nodes.tag)) and  not('-ref' in str(child_nodes.attrib)) ):
			reference_auth_method = child_nodes.attrib['ref']

	if( reference_auth_method is not None and 'jaasAuthProvider' in reference_auth_method):
		authentication_method = 'UNIX'
	elif( reference_auth_method is not None and 'activeDirectoryAuthenticationProvider' in reference_auth_method):
		authentication_method = 'ACTIVE_DIRECTORY'
	elif( reference_auth_method is not None and 'ldapAuthProvider' in reference_auth_method):
		authentication_method = 'LDAP'
	else:
		authentication_method = 'NONE'

	return authentication_method



def call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist):
	finalLibPath = libpath.replace('\\', '/').replace('//', '/')
	finalFilePath = 'jceks://file/' + filepath.replace('\\', '/').replace('//', '/')
	if getorcreateorlist == 'create':
		commandtorun = ['java', '-cp', finalLibPath, 'com.hortonworks.credentialapi.buildks', 'create', aliasKey, '-value', aliasValue, '-provider', finalFilePath]
		p = Popen(commandtorun, stdin=PIPE, stdout=PIPE, stderr=PIPE)
		output, error = p.communicate()
		statuscode = p.returncode
		return statuscode
	elif getorcreateorlist == 'get':
		commandtorun = ['java', '-cp', finalLibPath, 'com.hortonworks.credentialapi.buildks', 'get', aliasKey, '-provider', finalFilePath]
		p = Popen(commandtorun, stdin=PIPE, stdout=PIPE, stderr=PIPE)
		output, error = p.communicate()
		statuscode = p.returncode
		return statuscode, output
	elif getorcreateorlist == 'list':
		commandtorun = ['java', '-cp', finalLibPath, 'org.apache.ranger.credentialapi.buildks', 'list', '-provider', finalFilePath]
		p = Popen(commandtorun, stdin=PIPE, stdout=PIPE, stderr=PIPE)
		output, error = p.communicate()
		statuscode = p.returncode
		return statuscode, output
	else:
		print('proper command not received for input need get or create')


def get_hdp_version():
	return_code = -1
	hdp_output = ''
	hdp_version = None
	match = None
	statuscode = -1
	try:
		command_to_run = 'hdp-select status hadoop-client'
		output = Popen(command_to_run, stdin=PIPE, stdout=PIPE, stderr=PIPE, shell=True)
		return_code, error = output.communicate()
		statuscode = output.returncode
	except Exception, e:
		print('Error : ' + str(e))
	if statuscode == 0:
		hdp_version = re.sub('hadoop-client - ', '', return_code)
		hdp_version = hdp_version.rstrip()
		match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', hdp_version)
		print ('hdp_version = ' + hdp_version)
	else:
		print('Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

	if match is None:
		print('Failed to get extracted version')
		return None
	else:
		return hdp_version

def getDateTimeNow():
	return datetime.datetime.now().strftime("%Y%m%d%H%M%S")


if __name__ == '__main__':


	if len(sys.argv) > 1:
		function_to_call = sys.argv[1] if len(sys.argv) > 1  else None
		base_url = sys.argv[2] if len(sys.argv) > 2  else None
		print ('base url = ' + base_url)
		ambari_username_password = sys.argv[3] if len(sys.argv) > 3  else None
		print ('ambari_username_password = ' + ambari_username_password)
		cluster_name = sys.argv[4] if len(sys.argv) > 4  else None
		print ('cluster_name = ' + cluster_name)
		ranger_admin_fqdn = sys.argv[5] if len(sys.argv) > 5 else None
		print ('ranger_admin_fqdn = ' + ranger_admin_fqdn)
		ranger_service_name = 'RANGER'
		admin_component_name = 'RANGER_ADMIN'
		usersync_component_name = 'RANGER_USERSYNC'
		ambari_cluster_url = str(base_url) + '/api/v1/clusters/' + str(cluster_name)
		ambari_service_url = str(ambari_cluster_url) + '/services'
		hdp_dir = os.path.join('/usr', 'hdp')
		hdp_current_dir = os.path.join(hdp_dir, 'current')
		hdp_version = get_hdp_version()
		print('Found hdp_version = ' + str(hdp_version))
		hdp_version_dir = os.path.join(hdp_dir, hdp_version)
		if function_to_call is not None and len(function_to_call) > 0:
			print('Found first argument as : ' + function_to_call)
			function_to_call = int(function_to_call)
			if function_to_call == 1:
				print('Porting ranger admin installation details to ambari.')
				port_ranger_installation_to_ambari()
			elif function_to_call == 2:
				print('Porting ranger User-sync installation details to ambari.')
				port_ranger_usersync_installation_to_ambari()
			elif function_to_call == 3:
				print('Porting ranger hdfs plugin details to ambari.')
				port_ranger_hdfs_plugin_to_ambari()
			elif function_to_call == 4:
				print('Porting ranger Hive plugin details to ambari.')
				port_ranger_hive_plugin_to_ambari()
			elif function_to_call == 5:
				print('Porting ranger Hbase plugin details to ambari.')
				port_ranger_hbase_plugin_to_ambari()
			elif function_to_call == 6:
				print('Porting ranger Knox plugin details to ambari.')
				port_ranger_knox_plugin_to_ambari()
			elif function_to_call == 7:
				print('Porting ranger Storm plugin details to ambari.')
				port_ranger_storm_plugin_to_ambari()
			else:
				print ('Unsupported option passed for installation, please pass proper supported option')

	else:
		print('Usage :'
		      '\n python import_ranger_to_ambari.py  {install option eg. 1} { ambari server url (eg.  http://100.100.100.100:8080) } {ambari server username password (eg. demo_user:demo_pass) } {cluster name (eg. ambari_cluster)} {FQDN of host having Ranger Admin or Ranger Usersync or plugins installe (eg. ambari.server.com)} '
		      '\n Actual call will be like : python ranger_port_script.py 1 http://100.100.100.100:8080 demo_user:demo_pass ambari_cluster ambari.server.com'
		      '\n Pass first parameter as 1 for Ranger integration with Ambari.'
		      '\n Pass first parameter as 2 for Ranger User-sync integration with Ambari.'
		      '\n Pass first parameter as 3 for Ranger Hdfs Plugin integration with Ambari.'
		      '\n Pass first parameter as 4 for Ranger Hive Plugin integration with Ambari.'
		      '\n Pass first parameter as 5 for Ranger Hbase Plugin integration with Ambari.'
		      '\n Pass first parameter as 6 for Ranger Knox Plugin integration with Ambari.'
		      '\n Pass first parameter as 7 for Ranger Storm Plugin integration with Ambari.')

	sys.exit(0)

