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


def port_ranger_admin_installation_to_ambari():
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


def port_ranger_kms_installation_to_ambari():
	print('preparing advanged configurations for ranger-kms')
	flag_ranger_kms_present, ranger_kms_properties_from_file = get_ranger_kms_install_properties()
	if flag_ranger_kms_present:
		print('ranger kms service is installed, making configurations as required by ambari.')
		if create_ranger_kms_service_in_ambari():
			print('ranger kms service is added sucessfully in ambari')
			if create_kms_service_components_in_ambari('RANGER_KMS_SERVER'):
				print('ranger kms service component is added successfully in ambari')
				if register_ranger_kms_host_in_ambari():
					print('ranger kms host is registered successfully in ambari')
					if add_advanced_ranger_kms_configurations(ranger_kms_properties_from_file):
						print('ranger kms advanced configurations added successfully in ambari')
						if call_ranger_kms_installed():
							print('ranger kms service installed successfully in ambari.')
						else:
							print('ranger kms service install failed in ambari')
					else:
						print('ranger kms advanced configurations added failed in ambari')
				else:
					print('ranger kms host registration failed in ambari')
			else:
				print('ranger kms service component add failed in ambari')
		else:
			print('ranger kms service add failed in ambari')
	else:
		print('ranger kms service is not installed, not importing configurations to ambari.')


def create_ranger_service_in_ambari():
	print('creating ranger admin service in ambari')
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


def create_ranger_kms_service_in_ambari():
	print('creating ranger kms service in ambari')
	ranger_create_url = ambari_service_url + '/RANGER_KMS'
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
	print('adding ranger service components in ambari')
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

def create_kms_service_components_in_ambari(ranger_service_component_name):
	print('adding ranger service components in ambari')
	ranger_service_components = '{"components":[{"ServiceComponentInfo":{"component_name":"' + ranger_service_component_name + '"}}]}'
	print('creating ranger kms service in ambari -> ' + str(ranger_service_components))	
	ranger_service_component_create_url = ambari_service_url + '?ServiceInfo/service_name=RANGER_KMS'
	print('ranger_service_component_create_url -> ' + str(ranger_service_component_create_url))
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

def register_ranger_kms_host_in_ambari():
	print('adding ranger kms host in ambari')
	print('creating ranger kms service in ambari')
	ranger_service_component_create_url = ambari_cluster_url + '/hosts/' + ranger_admin_fqdn + '/host_components/' + "RANGER_KMS_SERVER"
	request_result = call_ambari_api(ranger_service_component_create_url, 'POST', ambari_username_password, '')
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
		if response_code == 201 and response is not None:
			print('ranger kms host registered successfully in ambari.')
			return True
		elif (response_code == 409 and response is not None):
			print('ranger kms host is already registered ambari.')
			return True
		else:
			print('ranger kms host registration failed in ambari.')
		return False


def add_advanced_ranger_configurations(add_admin_or_usersync, ranger_service_properties_from_file):
	print('creating advanced configurations to be added to ambari.')
	ranger_config_data = ''
	advanced_admin_properties = dict()
	advanced_ranger_site_properties = dict()
	advanced_ranger_env_properties = dict()
	advanced_user_sync_properties = dict()
	advanced_ranger_admin_site_properties = dict()
	date_time_stamp = getDateTimeNow()

	if (add_admin_or_usersync == 1):
		if not ((str(ranger_service_properties_from_file['db_root_password']).strip() == '') or
			        (str(ranger_service_properties_from_file['db_root_user']).strip() == '' )) :
			advanced_admin_properties['DB_FLAVOR'] = ranger_service_properties_from_file['DB_FLAVOR']
			advanced_admin_properties['SQL_CONNECTOR_JAR'] = ranger_service_properties_from_file['SQL_CONNECTOR_JAR']
			advanced_admin_properties['db_root_user'] = ranger_service_properties_from_file['db_root_user']
			advanced_admin_properties['db_root_password'] = ranger_service_properties_from_file['db_root_password']
			advanced_admin_properties['db_host'] = ranger_service_properties_from_file['db_host']
			advanced_admin_properties['db_name'] = ranger_service_properties_from_file['db_name']
			advanced_admin_properties['db_user'] = ranger_service_properties_from_file['ranger.jpa.jdbc.user']
			advanced_admin_properties['db_password'] = ranger_service_properties_from_file['ranger.jpa.jdbc.password']
			advanced_admin_properties['audit_db_name'] = ranger_service_properties_from_file['audit_db_name']
			advanced_admin_properties['audit_db_user'] = ranger_service_properties_from_file['ranger.jpa.audit.jdbc.user']
			advanced_admin_properties['audit_db_password'] = ranger_service_properties_from_file['ranger.jpa.audit.jdbc.password']
			advanced_admin_properties['policymgr_external_url'] = ranger_service_properties_from_file['ranger.externalurl']

			advanced_ranger_env_properties['ranger_user'] = 'ranger'
			advanced_ranger_env_properties['ranger_group'] = 'ranger'
			advanced_ranger_env_properties['ranger_admin_log_dir'] = '/var/log/ranger/admin'
			advanced_ranger_env_properties['ranger_usersync_log_dir'] = '/var/log/ranger/usersync'
			advanced_ranger_env_properties['ranger_admin_username'] = 'amb_ranger_admin'
			advanced_ranger_env_properties['ranger_admin_password'] = 'ambari123'
			advanced_ranger_env_properties['admin_username'] = 'admin'
			advanced_ranger_env_properties['admin_password'] = 'admin'
			advanced_ranger_env_properties['ranger_pid_dir'] = '/var/run/ranger'
			advanced_ranger_env_properties['create_db_dbuser'] = 'true'
			advanced_ranger_env_properties['xml_configurations_supported'] = 'true'
			
			advanced_ranger_admin_site_properties['ranger.service.host'] = ranger_service_properties_from_file['ranger.service.host']
			advanced_ranger_admin_site_properties['ranger.service.http.enabled'] = ranger_service_properties_from_file.get('ranger.service.http.enabled','true')
			advanced_ranger_admin_site_properties['ranger.service.http.port'] = ranger_service_properties_from_file.get('ranger.service.http.port','6080')
			advanced_ranger_admin_site_properties['ranger.service.https.port'] = ranger_service_properties_from_file.get('ranger.service.https.port','6182')
			advanced_ranger_admin_site_properties['ranger.service.https.attrib.ssl.enabled'] = ranger_service_properties_from_file.get('ranger.service.https.attrib.ssl.enabled','false')
			advanced_ranger_admin_site_properties['ranger.service.https.attrib.clientAuth'] = ranger_service_properties_from_file.get('ranger.service.https.attrib.clientAuth','want')
			advanced_ranger_admin_site_properties['ranger.service.https.attrib.keystore.keyalias'] = ranger_service_properties_from_file.get('ranger.service.https.attrib.keystore.keyalias','rangeradmin')
			advanced_ranger_admin_site_properties['ranger.service.https.attrib.keystore.pass'] = ranger_service_properties_from_file.get('ranger.service.https.attrib.keystore.pass','xasecure')
			advanced_ranger_admin_site_properties['ranger.https.attrib.keystore.file'] = ranger_service_properties_from_file.get('ranger.https.attrib.keystore.file','/etc/ranger/admin/conf/ranger-admin-keystore.jks')
			advanced_ranger_admin_site_properties['ranger.externalurl'] = ranger_service_properties_from_file.get('ranger.externalurl','http://localhost:6080')
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.driver'] = ranger_service_properties_from_file.get('ranger.jpa.jdbc.driver','com.mysql.jdbc.Driver')
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.url'] = ranger_service_properties_from_file.get('ranger.jpa.jdbc.url','jdbc:mysql://localhost')
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.user'] = ranger_service_properties_from_file['ranger.jpa.jdbc.user']
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.password'] = ranger_service_properties_from_file['ranger.jpa.jdbc.password']
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.credential.alias'] = ranger_service_properties_from_file.get('ranger.jpa.jdbc.credential.alias','rangeradmin')
			advanced_ranger_admin_site_properties['ranger.credential.provider.path'] = ranger_service_properties_from_file.get('ranger.credential.provider.path','/etc/ranger/admin/rangeradmin.jceks')
			advanced_ranger_admin_site_properties['ranger.audit.source.type'] = ranger_service_properties_from_file.get('ranger.audit.source.type','db')
			advanced_ranger_admin_site_properties['ranger.audit.solr.urls'] = ranger_service_properties_from_file.get('ranger.audit.solr.urls','http://solr_host:6083/solr/ranger_audits')
			advanced_ranger_admin_site_properties['ranger.authentication.method'] = ranger_service_properties_from_file.get('ranger.authentication.method','UNIX')
			advanced_ranger_admin_site_properties['ranger.ldap.url'] = ranger_service_properties_from_file.get('ranger.ldap.url','ldap://71.127.43.33:389')
			advanced_ranger_admin_site_properties['ranger.ldap.user.dnpattern'] = ranger_service_properties_from_file.get('ranger.ldap.user.dnpattern','uid={0},ou=users,dc=xasecure,dc=net')
			advanced_ranger_admin_site_properties['ranger.ldap.group.searchbase'] = ranger_service_properties_from_file.get('ranger.ldap.group.searchbase','ou=groups,dc=xasecure,dc=net')
			advanced_ranger_admin_site_properties['ranger.ldap.group.searchfilter'] = ranger_service_properties_from_file.get('ranger.ldap.group.searchfilter','(member=uid={0},ou=users,dc=xasecure,dc=net)')
			advanced_ranger_admin_site_properties['ranger.ldap.group.roleattribute'] = ranger_service_properties_from_file.get('ranger.ldap.group.roleattribute','cn')
			advanced_ranger_admin_site_properties['ranger.ldap.ad.domain'] = ranger_service_properties_from_file.get('ranger.ldap.ad.domain','localhost')
			advanced_ranger_admin_site_properties['ranger.ldap.ad.url'] = ranger_service_properties_from_file.get('ranger.ldap.ad.url','ldap://ad.xasecure.net:389')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.driver'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.driver','{{ranger_jdbc_driver}}')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.url'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.url','{{audit_jdbc_url}}')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.user'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.user','{{ranger_audit_db_user}}')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.password'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.password','_')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.credential.alias'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.credential.alias','rangeraudit')
			advanced_ranger_admin_site_properties['ranger.unixauth.remote.login.enabled'] = ranger_service_properties_from_file.get('ranger.unixauth.remote.login.enabled','true')
			advanced_ranger_admin_site_properties['ranger.unixauth.service.hostname'] = ranger_service_properties_from_file.get('ranger.unixauth.service.hostname','localhost')
			advanced_ranger_admin_site_properties['ranger.unixauth.service.port'] = ranger_service_properties_from_file.get('ranger.unixauth.service.port','5151')
			advanced_ranger_admin_site_properties['ranger.jpa.jdbc.dialect'] = ranger_service_properties_from_file.get('ranger.jpa.jdbc.dialect','{{jdbc_dialect}}')
			advanced_ranger_admin_site_properties['ranger.jpa.audit.jdbc.dialect'] = ranger_service_properties_from_file.get('ranger.jpa.audit.jdbc.dialect','{{jdbc_dialect}')
			advanced_ranger_admin_site_properties['ranger.audit.solr.zookeepers'] = ranger_service_properties_from_file.get('ranger.audit.solr.zookeepers','NONE')
			advanced_ranger_admin_site_properties['ranger.audit.solr.username'] = ranger_service_properties_from_file.get('ranger.audit.solr.username','ranger_solr')
			advanced_ranger_admin_site_properties['ranger.audit.solr.password'] = ranger_service_properties_from_file.get('ranger.audit.solr.password','NONE')
			
			ranger_config_data = '[{"Clusters":{"desired_config":[{"type":"admin-properties", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_admin_properties) + ', "properties_attributes": {"final": "true"}},{"type":"ranger-site", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_ranger_site_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-env", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_env_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-admin-site", "service_config_version_note": "Initial configuration for Ranger Admin service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_ranger_admin_site_properties) + ', "properties_attributes": {"final": "false"}}]}}]'


			print ('####################### admin_properties configuration :')
			for each_key in advanced_admin_properties:
				print str(each_key) + ' = ' + str(advanced_admin_properties[each_key])

			print ('####################### ranger_site_properties configuration :')
			for each_key in advanced_ranger_site_properties:
				print str(each_key) + ' = ' + str(advanced_ranger_site_properties[each_key])

			print ('####################### ranger_env_properties configuration :')
			for each_key in advanced_ranger_env_properties:
				print str(each_key) + ' = ' + str(advanced_ranger_env_properties[each_key])
			
			print ('####################### ranger_admin_site_properties configuration :')
			for each_key in advanced_ranger_env_properties:
				print str(each_key) + ' = ' + str(advanced_ranger_env_properties[each_key])


		else:
			print('either db_root_user or db_root_password value is missing from ranger_admin_install.properties file, please set appropriate value and run the script again.')
			sys.exit(1)

	elif (add_admin_or_usersync == 2):
		advanced_user_sync_properties['ranger.usersync.port'] = ranger_service_properties_from_file['ranger.usersync.port']
		advanced_user_sync_properties['ranger.usersync.ssl'] = ranger_service_properties_from_file['ranger.usersync.ssl']
		advanced_user_sync_properties['ranger.usersync.keystore.file'] = ranger_service_properties_from_file['ranger.usersync.keystore.file']
		advanced_user_sync_properties['ranger.usersync.keystore.password'] = ranger_service_properties_from_file.get('ranger.usersync.keystore.password','UnIx529p')
		advanced_user_sync_properties['ranger.usersync.truststore.file'] = ranger_service_properties_from_file.get('ranger.usersync.truststore.file','/usr/hdp/current/ranger-usersync/conf/mytruststore.jks')
		advanced_user_sync_properties['ranger.usersync.truststore.password'] = ranger_service_properties_from_file.get('ranger.usersync.truststore.password','changeit')
		advanced_user_sync_properties['ranger.usersync.passwordvalidator.path'] = ranger_service_properties_from_file['ranger.usersync.passwordvalidator.path']
		advanced_user_sync_properties['ranger.usersync.sink.impl.class'] = ranger_service_properties_from_file['ranger.usersync.sink.impl.class']
		advanced_user_sync_properties['ranger.usersync.policymanager.baseURL'] = ranger_service_properties_from_file['ranger.usersync.policymanager.baseURL']
		advanced_user_sync_properties['ranger.usersync.policymanager.maxrecordsperapicall'] = ranger_service_properties_from_file['ranger.usersync.policymanager.maxrecordsperapicall']
		advanced_user_sync_properties['ranger.usersync.policymanager.mockrun'] = ranger_service_properties_from_file['ranger.usersync.policymanager.mockrun']
		advanced_user_sync_properties['ranger.usersync.unix.minUserId'] = ranger_service_properties_from_file['ranger.usersync.unix.minUserId']
		advanced_user_sync_properties['ranger.usersync.sleeptimeinmillisbetweensynccycle'] = ranger_service_properties_from_file['ranger.usersync.sleeptimeinmillisbetweensynccycle']
		advanced_user_sync_properties['ranger.usersync.source.impl.class'] = ranger_service_properties_from_file['ranger.usersync.source.impl.class']
		advanced_user_sync_properties['ranger.usersync.filesource.file'] = ranger_service_properties_from_file.get('ranger.usersync.filesource.file','/tmp/usergroup.txt')
		advanced_user_sync_properties['ranger.usersync.filesource.text.delimiter'] = ranger_service_properties_from_file.get('ranger.usersync.filesource.text.delimiter',',')
		advanced_user_sync_properties['ranger.usersync.ldap.url'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.url','ldap://localhost:389')
		advanced_user_sync_properties['ranger.usersync.ldap.binddn'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.binddn','cn=admin,dc=xasecure,dc=net')
		advanced_user_sync_properties['ranger.usersync.ldap.ldapbindpassword'] = ranger_service_properties_from_file['ranger.usersync.ldap.ldapbindpassword']
		advanced_user_sync_properties['ranger.usersync.ldap.bindalias'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.bindalias','testldapalias')
		advanced_user_sync_properties['ranger.usersync.ldap.bindkeystore'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.bindkeystore','')
		advanced_user_sync_properties['ranger.usersync.ldap.searchBase'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.searchBase','dc=hadoop,dc=apache,dc=org')
		advanced_user_sync_properties['ranger.usersync.ldap.user.searchbase'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.searchbase','ou=users,dc=xasecure,dc=net')
		advanced_user_sync_properties['ranger.usersync.ldap.user.searchscope'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.searchscope','sub')
		advanced_user_sync_properties['ranger.usersync.ldap.user.objectclass'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.objectclass','person')
		advanced_user_sync_properties['ranger.usersync.ldap.user.searchfilter'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.searchfilter','empty')
		advanced_user_sync_properties['ranger.usersync.ldap.user.nameattribute'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.nameattribute','cn')
		advanced_user_sync_properties['ranger.usersync.ldap.user.groupnameattribute'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.user.groupnameattribute','memberof, ismemberof')
		advanced_user_sync_properties['ranger.usersync.ldap.username.caseconversion'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.username.caseconversion','none')
		advanced_user_sync_properties['ranger.usersync.ldap.groupname.caseconversion'] = ranger_service_properties_from_file.get('ranger.usersync.ldap.groupname.caseconversion','none')
		advanced_user_sync_properties['ranger.usersync.logdir'] = ranger_service_properties_from_file.get('ranger.usersync.logdir','/var/log/ranger/usersync')
		advanced_user_sync_properties['ranger.usersync.group.searchenabled'] = ranger_service_properties_from_file.get('ranger.usersync.group.searchenabled','false')
		advanced_user_sync_properties['ranger.usersync.group.searchbase'] = ranger_service_properties_from_file.get('ranger.usersync.group.searchbase',' ')
		advanced_user_sync_properties['ranger.usersync.group.searchscope'] = ranger_service_properties_from_file.get('ranger.usersync.group.searchscope',' ')
		advanced_user_sync_properties['ranger.usersync.group.objectclass'] = ranger_service_properties_from_file.get('ranger.usersync.group.objectclass',' ')
		advanced_user_sync_properties['ranger.usersync.group.searchfilter'] = ranger_service_properties_from_file.get('ranger.usersync.group.searchfilter',' ')
		advanced_user_sync_properties['ranger.usersync.group.nameattribute'] = ranger_service_properties_from_file.get('ranger.usersync.group.nameattribute',' ')
		advanced_user_sync_properties['ranger.usersync.group.memberattributename'] = ranger_service_properties_from_file.get('ranger.usersync.group.memberattributename',' ')
		advanced_user_sync_properties['ranger.usersync.pagedresultsenabled'] = ranger_service_properties_from_file.get('ranger.usersync.pagedresultsenabled','true')
		advanced_user_sync_properties['ranger.usersync.pagedresultssize'] = ranger_service_properties_from_file.get('ranger.usersync.pagedresultssize','500')
		advanced_user_sync_properties['ranger.usersync.credstore.filename'] = ranger_service_properties_from_file.get('ranger.usersync.credstore.filename','/usr/hdp/current/ranger-usersync/conf/ugsync.jceks')

		ranger_config_data = '[{"Clusters":{"desired_config":[{"type":"ranger-ugsync-site", "service_config_version_note": "Initial configuration for Ranger Usersync service" ,"tag":"' + str(
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


def add_advanced_ranger_kms_configurations(ranger_kms_properties_from_file):
	print('creating advanced configurations to be added to ')
	advanced_kms_env_properties = dict()
	advanced_kms_properties = dict()
	advanced_dbks_site_properties = dict()
	advanced_kms_site_properties = dict()
	advanced_ranger_kms_site_properties = dict()
	advanced_ranger_kms_audit_properties = dict()
	advanced_ranger_kms_policymgr_ssl_properties = dict()
	advanced_ranger_kms_security_properties = dict()
	advanced_kms_log4j_properties = dict()	
	date_time_stamp = getDateTimeNow()
	
	advanced_kms_env_properties['kms_user'] = 'kms'
	advanced_kms_env_properties['kms_group'] = 'kms'
	advanced_kms_env_properties['kms_log_dir'] = '/var/log/ranger/kms'
	advanced_kms_env_properties['kms_port'] = '9292'
	
	advanced_kms_properties['REPOSITORY_CONFIG_USERNAME'] = 'keyadmin'
	advanced_kms_properties['REPOSITORY_CONFIG_PASSWORD'] = 'keyadmin'
	advanced_kms_properties['DB_FLAVOR'] = ranger_kms_properties_from_file.get('DB_FLAVOR','MYSQL')
	advanced_kms_properties['SQL_CONNECTOR_JAR'] = ranger_kms_properties_from_file.get('SQL_CONNECTOR_JAR','/usr/share/java/mysql-connector-java.jar')
	advanced_kms_properties['db_root_user'] = ranger_kms_properties_from_file.get('db_root_user','root')
	advanced_kms_properties['db_root_password'] = ranger_kms_properties_from_file.get('db_root_password','')
	advanced_kms_properties['db_host'] = ranger_kms_properties_from_file.get('db_host','localhost')
	advanced_kms_properties['db_name'] = ranger_kms_properties_from_file.get('db_name','rangerkms')
	advanced_kms_properties['db_user'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.user','rangerkms')
	advanced_kms_properties['db_password'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.password','')
	advanced_kms_properties['KMS_MASTER_KEY_PASSWD'] = ranger_kms_properties_from_file.get('KMS_MASTER_KEY_PASSWD','')
	

	advanced_dbks_site_properties['hadoop.kms.blacklist.DECRYPT_EEK'] = ranger_kms_properties_from_file.get('hadoop.kms.blacklist.DECRYPT_EEK','hdfs')
	advanced_dbks_site_properties['ranger.db.encrypt.key.password'] = ranger_kms_properties_from_file.get('ranger.db.encrypt.key.password','_')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.url'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.url','{{db_jdbc_url}}')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.user'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.user','{{db_user}}')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.password'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.password','_')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.credential.provider.path'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.credential.provider.path','/etc/ranger/kms/rangerkms.jceks')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.credential.alias'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.credential.alias','ranger.ks.jdbc.password')
	advanced_dbks_site_properties['ranger.ks.masterkey.credential.alias'] = ranger_kms_properties_from_file.get('ranger.ks.masterkey.credential.alias','ranger.ks.masterkey.password')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.dialect'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.dialect','{{jdbc_dialect}}')
	advanced_dbks_site_properties['ranger.ks.jpa.jdbc.driver'] = ranger_kms_properties_from_file.get('ranger.ks.jpa.jdbc.driver','{{db_jdbc_driver}}')
	advanced_dbks_site_properties['ranger.ks.jdbc.sqlconnectorjar'] = ranger_kms_properties_from_file.get('ranger.ks.jdbc.sqlconnectorjar','{{driver_curl_target}}')


	advanced_kms_site_properties['hadoop.kms.key.provider.uri'] = ranger_kms_properties_from_file.get('hadoop.kms.key.provider.uri','dbks://http@localhost:9292/kms')
	advanced_kms_site_properties['hadoop.security.keystore.JavaKeyStoreProvider.password'] = ranger_kms_properties_from_file.get('hadoop.security.keystore.JavaKeyStoreProvider.password','none')
	advanced_kms_site_properties['hadoop.kms.cache.enable'] = ranger_kms_properties_from_file.get('hadoop.kms.cache.enable','true')
	advanced_kms_site_properties['hadoop.kms.cache.timeout.ms'] = ranger_kms_properties_from_file.get('hadoop.kms.cache.timeout.ms','600000')
	advanced_kms_site_properties['hadoop.kms.current.key.cache.timeout.ms'] = ranger_kms_properties_from_file.get('hadoop.kms.current.key.cache.timeout.ms','30000')
	advanced_kms_site_properties['hadoop.kms.audit.aggregation.window.ms'] = ranger_kms_properties_from_file.get('hadoop.kms.audit.aggregation.window.ms','10000')
	advanced_kms_site_properties['hadoop.kms.authentication.type'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.type','simple')
	advanced_kms_site_properties['hadoop.kms.authentication.kerberos.keytab'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.kerberos.keytab','${user.home}/kms.keytab')
	advanced_kms_site_properties['hadoop.kms.authentication.kerberos.principal'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.kerberos.principal','HTTP/localhost')
	advanced_kms_site_properties['hadoop.kms.authentication.kerberos.name.rules'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.kerberos.name.rules','DEFAULT')
	advanced_kms_site_properties['hadoop.kms.authentication.signer.secret.provider'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.signer.secret.provider','random')
	advanced_kms_site_properties['hadoop.kms.authentication.signer.secret.provider.zookeeper.path'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.signer.secret.provider.zookeeper.path','/hadoop-kms/hadoop-auth-signature-secret')
	advanced_kms_site_properties['hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type','kerberos')
	advanced_kms_site_properties['hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab'] = ranger_kms_properties_from_file.get('/etc/hadoop/conf/kms.keytab','/etc/hadoop/conf/kms.keytab')
	advanced_kms_site_properties['hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal'] = ranger_kms_properties_from_file.get('hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal','kms/#HOSTNAME#')
	advanced_kms_site_properties['hadoop.kms.security.authorization.manager'] = ranger_kms_properties_from_file.get('hadoop.kms.security.authorization.manager','org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer')
	
	
	advanced_ranger_kms_site_properties['ranger.service.host'] = ranger_kms_properties_from_file.get('ranger.service.host','{{kms_host}}')
	advanced_ranger_kms_site_properties['ranger.service.http.port'] = ranger_kms_properties_from_file.get('ranger.service.http.port','{{kms_port}}')
	advanced_ranger_kms_site_properties['ranger.service.https.port'] = ranger_kms_properties_from_file.get('ranger.service.https.port','9393')
	advanced_ranger_kms_site_properties['ranger.service.shutdown.port'] = ranger_kms_properties_from_file.get('ranger.service.shutdown.port','7085')
	advanced_ranger_kms_site_properties['ranger.contextName'] = ranger_kms_properties_from_file.get('ranger.contextName','/kms')
	advanced_ranger_kms_site_properties['xa.webapp.dir'] = ranger_kms_properties_from_file.get('xa.webapp.dir','./webapp')
	advanced_ranger_kms_site_properties['ranger.service.https.attrib.ssl.enabled'] = ranger_kms_properties_from_file.get('ranger.service.https.attrib.ssl.enabled','false')


	advanced_ranger_kms_audit_properties['xasecure.audit.is.enabled'] = ranger_kms_properties_from_file.get('xasecure.audit.is.enabled','true')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db'] = ranger_kms_properties_from_file.get('xasecure.audit.db.is.enabled','false')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db.jdbc.url'] = ranger_kms_properties_from_file.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db.user'] = ranger_kms_properties_from_file.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db.password'] = ranger_kms_properties_from_file.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = ranger_kms_properties_from_file.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
	advanced_ranger_kms_audit_properties['xasecure.audit.credential.provider.file'] = ranger_kms_properties_from_file.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/ranger/kms/audit/db/spool')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.hdfs'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.hdfs','true')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.hdfs.dir'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/ranger/kms/audit/hdfs/spool')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.solr'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.solr','true')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.solr.urls'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.solr.zookeepers'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.solr.zookeepers','none')
	advanced_ranger_kms_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = ranger_kms_properties_from_file.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/ranger/kms/audit/solr/spool')
	advanced_ranger_kms_audit_properties['xasecure.audit.provider.summary.enabled'] = ranger_kms_properties_from_file.get('xasecure.audit.provider.summary.enabled','false')
	
	
	advanced_ranger_kms_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = ranger_kms_properties_from_file.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/ranger-kms/conf/ranger-plugin-keystore.jks')
	advanced_ranger_kms_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = ranger_kms_properties_from_file.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/ranger-kms/conf/ranger-plugin-truststore.jks')
	advanced_ranger_kms_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = ranger_kms_properties_from_file.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
	advanced_ranger_kms_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = ranger_kms_properties_from_file.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')
	
	
	advanced_ranger_kms_security_properties['ranger.plugin.kms.service.name'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.service.name','{{repo_name}}')
	advanced_ranger_kms_security_properties['ranger.plugin.kms.policy.source.impl'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
	advanced_ranger_kms_security_properties['ranger.plugin.kms.policy.rest.url'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.policy.rest.url','{{policymgr_mgr_url}}')
	advanced_ranger_kms_security_properties['ranger.plugin.kms.policy.rest.ssl.config.file'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.policy.rest.ssl.config.file','/etc/ranger/kms/conf/ranger-policymgr-ssl.xml')
	advanced_ranger_kms_security_properties['ranger.plugin.kms.policy.pollIntervalMs'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.policy.pollIntervalMs','30000')
	advanced_ranger_kms_security_properties['ranger.plugin.kms.policy.cache.dir'] = ranger_kms_properties_from_file.get('ranger.plugin.kms.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
	
	advanced_kms_log4j_properties['content'] = ranger_kms_properties_from_file.get('kms.log4j.properties','')
	

	kms_config_data = '[{"Clusters":{"desired_config":[{"type":"kms-properties", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_kms_properties) + ', "properties_attributes": {"final": "true"}},{"type":"kms-site", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(
				advanced_kms_site_properties) + ', "properties_attributes": {"final": "false"}},{"type":"kms-env", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_kms_env_properties) + ', "properties_attributes": {"final": "false"}},{"type":"dbks-site", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_dbks_site_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-kms-site", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_kms_site_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-kms-audit", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_kms_audit_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-kms-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_kms_policymgr_ssl_properties) + ', "properties_attributes": {"final": "false"}},{"type":"kms-log4j", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_kms_log4j_properties) + ', "properties_attributes": {"final": "false"}},{"type":"ranger-kms-security", "service_config_version_note": "Initial configuration for Ranger KMS service" ,"tag":"' + str(
				date_time_stamp) + '","properties":' + json.dumps(advanced_ranger_kms_security_properties) + ', "properties_attributes": {"final": "false"}}]}}]'
	

	print ('####################### kms_properties configuration :')
	for each_key in advanced_kms_properties:
		print str(each_key) + ' = ' + str(advanced_kms_properties[each_key])

	print ('####################### kms_site_properties configuration :')
	for each_key in advanced_kms_site_properties:
		print str(each_key) + ' = ' + str(advanced_kms_site_properties[each_key])

	print ('####################### kms_env_properties configuration :')
	for each_key in advanced_kms_env_properties:
		print str(each_key) + ' = ' + str(advanced_kms_env_properties[each_key])

	print ('####################### ranger_kms_site_properties configuration :')
	for each_key in advanced_ranger_kms_site_properties:
		print str(each_key) + ' = ' + str(advanced_ranger_kms_site_properties[each_key])

	print ('####################### kms_dbks_site_properties configuration :')
	for each_key in advanced_dbks_site_properties:
		print str(each_key) + ' = ' + str(advanced_dbks_site_properties[each_key])

	print ('####################### ranger_kms_audit_properties configuration :')
	for each_key in advanced_ranger_kms_audit_properties:
		print str(each_key) + ' = ' + str(advanced_ranger_kms_audit_properties[each_key])
	
	print ('####################### ranger_kms_policymgr_ssl_properties configuration :')
	for each_key in advanced_ranger_kms_policymgr_ssl_properties:
		print str(each_key) + ' = ' + str(advanced_ranger_kms_policymgr_ssl_properties[each_key])
	
	
	print ('####################### ranger_kms_security_properties configuration :')
	for each_key in advanced_ranger_kms_security_properties:
		print str(each_key) + ' = ' + str(advanced_ranger_kms_security_properties[each_key])
	
	print ('####################### ranger_kms_log4j_properties configuration :')
	for each_key in advanced_kms_log4j_properties:
		print str(each_key) + ' = ' + str(advanced_kms_log4j_properties[each_key])

	
	confirm_configurations = raw_input('please confirm the above configuration values y/n (n) : ')
	if(confirm_configurations == ''):
		confirm_configurations = 'n'
	print ('input registered as ' + str(confirm_configurations))

	if(confirm_configurations.lower() == 'y'):
		ranger_config_request_url = ambari_cluster_url
		request_result = call_ambari_api(ranger_config_request_url, 'PUT', ambari_username_password, str(kms_config_data))
		if request_result is not None:
			response_code = request_result.getcode()
			response = json.loads(json.JSONEncoder().encode(request_result.read()))
			if response_code == 200 and response is not None:
				print('ranger kms advanced configuration added successfully in ambari.')
				return True
			else:
				print('ranger kms advanced configuration add failed in ambari.')
				return False
		else:
			print('ranger kms advanced configuration add failed in ambari.')
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

def call_ranger_kms_installed():
	print('changing state of ranger services from init to installed.')
	ranger_state_change_request = '{"RequestInfo":{"context":"Install Ranger KMS Service","operation_level":{"level":"CLUSTER","cluster_name":"' + str(
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
							print('ranger kms install is pending in ambari.')
							flag_ranger_installed = True
						if (response_code == 200 and ('PENDING' in response_status or 'INTERNAL_REQUEST' in response_status)):
							print('ranger kms install is pending in ambari.')
							flag_ranger_installed = True
						if (response_code == 200 and 'COMPLETED' in response_status):
							print('ranger kms installed successfully in ambari.')
							flag_ranger_installed = False
						else:
							flag_ranger_installed = True
				return not flag_ranger_installed
		else:
			print('ranger kms state changed to install failed in ambari.')
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

def get_ranger_kms_install_properties():
	ranger_kms_current_dir = os.path.join(hdp_current_dir, 'ranger-kms')
	flag_hadoop_present = check_hadoop_dir_present(hdp_current_dir)
	flag_ranger_kms_present = False
	ranger_kms_properties_from_file = dict()

	if flag_hadoop_present:
		print('hadoop directory is present, checking ranger kms installation.')
		flag_ranger_kms_present, ranger_kms_installed_version = check_ranger_kms_install(ranger_kms_current_dir)

		if flag_ranger_kms_present:
			print('ranger kms is present.getting existing configurations to port to ambari.')
			if os.path.isfile("ranger_admin_install.properties"):
				print('ranger_install_properties_path exists, getting existing properties for ambari port')
				ranger_kms_properties_from_file = import_properties_from_file("ranger_admin_install.properties")
				if not (ranger_kms_properties_from_file['db_root_user'] == '' or ranger_kms_properties_from_file['db_root_password'] == ''):
					print 'db_root_username and db_root_password are not blank.'
					print 'value for db_root_user = ' + str(ranger_kms_properties_from_file['db_root_user'])
					print 'value for db_root_password = ' + str(ranger_kms_properties_from_file['db_root_password'])
				else:
					print 'db_root_username or db_root_password are blank, please provide proper values in ranger_admin_install.properties. exiting installation without any changes.'
					sys.exit(1)

				print('got ranger admin values from ranger_admin_install.properties file, need to configure ambari for ranger service.')
				print('getting additional properties required by ranger services')
				ranger_kms_properties_from_file = get_additional_properties_for_kms(ranger_kms_properties_from_file)

	return flag_ranger_kms_present, ranger_kms_properties_from_file


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


def check_ranger_kms_install(ranger_kms_current_dir):
	flag_ranger_dir_present = False
	ranger_current_installed_version = ''
	print('checking ranger kms service path folder')
	if os.path.isdir(ranger_kms_current_dir):
		print('ranger kms is installed.')
		if os.path.islink(ranger_kms_current_dir):
			flag_ranger_dir_present = True
			print('ranger kms link found getting current version from link.')
			ranger_home_path = os.path.realpath(ranger_kms_current_dir)
			ranger_current_installed_version = ranger_home_path.split('/')[4]
	return flag_ranger_dir_present, ranger_current_installed_version


def get_additional_properties_for_admin(ranger_admin_properties_from_file):
	ranger_conf_path = '/etc/ranger/admin/conf'
	ranger_admin_default_site_xml_properties = os.path.join(ranger_conf_path,'ranger-admin-default-site.xml')
	ranger_admin_site_xml_properties = os.path.join(ranger_conf_path,'ranger-admin-site.xml')
        try:
        	
		ranger_admin_properties_from_file =  import_properties_from_xml(ranger_admin_default_site_xml_properties, ranger_admin_properties_from_file)
		ranger_admin_properties_from_file =  import_properties_from_xml(ranger_admin_site_xml_properties, ranger_admin_properties_from_file)
        except Exception, e:
            print "Error loading ranger-admin properties from xml files : ", str(e)

	print('getting db flavor, library and command invoker')
	ranger_jpa_jdbc_dialect = ranger_admin_properties_from_file['ranger.jpa.jdbc.dialect']
	print('ranger_jpa_jdbc_dialect = ' + ranger_jpa_jdbc_dialect)
	ranger_jpa_jdbc_url = ranger_admin_properties_from_file['ranger.jpa.jdbc.url']
	print('ranger_jpa_jdbc_url = ' + ranger_jpa_jdbc_url)
	if ('mysql'.lower() in ranger_jpa_jdbc_dialect.lower() and 'mysql'.lower() in ranger_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as MYSQL setting db_flavour and sql command invoker as mysql')
		ranger_admin_properties_from_file['DB_FLAVOR'] = 'MYSQL'
		ranger_admin_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/mysql-connector-java.jar'
	elif ('oracle'.lower() in ranger_jpa_jdbc_dialect and 'oracle'.lower() in ranger_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as Oracle setting db_flavour and sql command invoker as oracle')
		ranger_admin_properties_from_file['DB_FLAVOR'] = 'ORACLE'
		ranger_admin_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/ojdbc6.jar'
	elif ('postgres'.lower() in ranger_jpa_jdbc_dialect and 'postgres'.lower() in ranger_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as postgres setting db_flavour and sql command invoker as postgres')
		ranger_admin_properties_from_file['DB_FLAVOR'] = 'POSTGRES'
		ranger_admin_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/postgresql.jar'
	else:
		print('found unsupported DB_FLAVOUR, please configure as MYSQL, ORACLE or Postgres which are supported for now.exitting for now')
		sys.exit(1)

	ranger_jpa_jdbc_url = ranger_admin_properties_from_file['ranger.jpa.jdbc.url']
	print('found jdbc url configured as : ' + str(ranger_jpa_jdbc_url) + ' , getting db host from configured jdbc url')
	ranger_database_host_name = ranger_jpa_jdbc_url.split(':')
	ranger_database_host = ranger_database_host_name[3].split('/')[2]
	ranger_database_name = ranger_database_host_name[3].split('/')[3]
	print('found db host as : ' + str(ranger_database_host))
	print('found db name as : ' + str(ranger_database_name))
	ranger_admin_properties_from_file['db_host'] = ranger_database_host
	ranger_admin_properties_from_file['db_name'] = ranger_database_name

	ranger_audit_jdbc_url = ranger_admin_properties_from_file['ranger.jpa.audit.jdbc.url']
	print('found audit jdbc url configured as : ' + str(ranger_audit_jdbc_url) + ' , getting db host from configured jdbc url')
	ranger_audit_database_host_name = ranger_audit_jdbc_url.split(':')

	ranger_audit_database_host = ranger_audit_database_host_name[3].split('/')[2]
	ranger_audit_database_name = ranger_audit_database_host_name[3].split('/')[3]
	print('found ranger_audit_database_name as : ' + str(ranger_audit_database_name))
	ranger_admin_properties_from_file['audit_db_host'] = ranger_audit_database_host
	ranger_admin_properties_from_file['audit_db_name'] = ranger_audit_database_name

	ranger_db_password = ''
	ranger_audit_db_password = ''

	libpath = os.path.join(hdp_version_dir, 'ranger-admin', 'cred', 'lib', '*')
	aliasKey = 'ranger.db.password'
	aliasValue = ''
	filepath = ranger_admin_properties_from_file['ranger.credential.provider.path'] 
	if (filepath is None or len(filepath) == 0):
		filepath = os.path.join(hdp_version_dir, 'ranger-admin', 'ews', 'webapp', 'WEB-INF', 'classes', 'conf', '.jceks', 'rangeradmin.jceks')

	getorcreateorlist = 'get'

	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
	if statuscode == 0:
		ranger_db_password = value.strip()

	aliasKey = 'ranger.auditdb.password'
	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
	if statuscode == 0:
		ranger_audit_db_password = value.strip()

	if str(ranger_admin_properties_from_file['ranger.jpa.jdbc.password']) == '_' or str(ranger_admin_properties_from_file['ranger.jpa.jdbc.password']).lower() == 'crypted':
		ranger_admin_properties_from_file['ranger.jpa.jdbc.password'] = ranger_db_password
	if str(ranger_admin_properties_from_file['ranger.jpa.audit.jdbc.password']) == '_' or str(ranger_admin_properties_from_file['ranger.jpa.audit.jdbc.password']).lower() == 'crypted':	
		ranger_admin_properties_from_file['ranger.jpa.audit.jdbc.password'] = ranger_audit_db_password
	
	return ranger_admin_properties_from_file


def get_additional_properties_for_kms(ranger_kms_properties_from_file):
	ranger_conf_path = '/etc/ranger/kms/conf'	
	kms_dbks_site_properties = os.path.join(ranger_conf_path,'dbks-site.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(kms_dbks_site_properties, ranger_kms_properties_from_file)	
	kms_site_properties = os.path.join(ranger_conf_path,'kms-site.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(kms_site_properties, ranger_kms_properties_from_file)	
	ranger_kms_site_properties = os.path.join(ranger_conf_path,'ranger-kms-site.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(ranger_kms_site_properties, ranger_kms_properties_from_file)	
	ranger_kms_audit_properties = os.path.join(ranger_conf_path,'ranger-kms-audit.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(ranger_kms_audit_properties, ranger_kms_properties_from_file)
	ranger_kms_security_properties = os.path.join(ranger_conf_path,'ranger-kms-security.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(ranger_kms_security_properties, ranger_kms_properties_from_file)	
	ranger_policy_mgr_ssl_properties = os.path.join(ranger_conf_path,'ranger-policymgr-ssl.xml')
	ranger_kms_properties_from_file = import_properties_from_xml(ranger_policy_mgr_ssl_properties, ranger_kms_properties_from_file)
	kms_log4j_properties = os.path.join(ranger_conf_path,'kms-log4j.properties')
	ranger_kms_properties_from_file['kms.log4j.properties'] = read_properties_file(kms_log4j_properties)
	
	kms_jpa_jdbc_dialect = ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.dialect']
	print('kms_jpa_jdbc_dialect = ' + str(kms_jpa_jdbc_dialect))
	kms_jpa_jdbc_url = ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.url']
	if ('mysql'.lower() in kms_jpa_jdbc_dialect.lower() and 'mysql'.lower() in kms_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as MYSQL setting db_flavour and sql command invoker as mysql')
		ranger_kms_properties_from_file['DB_FLAVOR'] = 'MYSQL'
		ranger_kms_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/mysql-connector-java.jar'
	elif ('oracle'.lower() in kms_jpa_jdbc_dialect and 'oracle'.lower() in kms_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as Oracle setting db_flavour and sql command invoker as oracle')
		ranger_kms_properties_from_file['DB_FLAVOR'] = 'ORACLE'
		ranger_kms_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/ojdbc6.jar'
	elif ('postgres'.lower() in kms_jpa_jdbc_dialect and 'postgres'.lower() in kms_jpa_jdbc_url.lower()):
		print('db dialect and jdbc url are set as postgres setting db_flavour and sql command invoker as postgres')
		ranger_kms_properties_from_file['DB_FLAVOR'] = 'POSTGRES'
		ranger_kms_properties_from_file['SQL_CONNECTOR_JAR'] = '/usr/share/java/postgresql.jar'
	else:
		print('found unsupported DB_FLAVOUR, please configure as MYSQL, ORACLE or Postgres which are supported for now.exitting for now')
		sys.exit(1)
	
	kms_jpa_db_hostname = kms_jpa_jdbc_url.split(':')
	
	kms_jpa_jdbc_hostname = kms_jpa_db_hostname[3].split('/')[2]
	kms_jpa_jdbc_db_name = kms_jpa_db_hostname[3].split('/')[3]
	print('found db hostname = ' + kms_jpa_jdbc_hostname )
	print('found db name = ' + kms_jpa_jdbc_db_name )
	
	ranger_kms_properties_from_file['db_host'] = kms_jpa_jdbc_hostname
	ranger_kms_properties_from_file['db_name'] = kms_jpa_jdbc_db_name
	
	kms_database_password = ''
	kms_keystore_masterkey_password = ''
	
	libpath = os.path.join(hdp_version_dir, 'ranger-kms', 'cred', 'lib', '*')
	aliasKey = ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.credential.alias']
	if(aliasKey is None or len(aliasKey) == 0):
		aliasKey = 'ranger.ks.jdbc.password'
	aliasValue = ''
	filepath = ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.credential.provider.path']
	if (filepath is None or len(filepath) == 0) :
		filepath = os.path.join(hdp_version_dir, 'ranger-kms', 'ews', 'webapp', 'WEB-INF', 'classes', 'conf', '.jceks', 'rangerkms.jceks')

	getorcreateorlist = 'get'

	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)
	if statuscode == 0:
		kms_database_password = value.strip()
	print('kms_database_password = ' + kms_database_password)
	aliasKey = ranger_kms_properties_from_file['ranger.ks.masterkey.credential.alias']
	if(aliasKey is None or len(aliasKey) == 0):
		aliasKey = 'ranger.ks.masterkey.password'
	statuscode, value = call_keystore(libpath, aliasKey, aliasValue, filepath, getorcreateorlist)

	if statuscode == 0:
		kms_keystore_masterkey_password = value.strip()
	
	print('kms_keystore_masterkey_password = ' + kms_keystore_masterkey_password)

	if str(ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.password']) == '_' or str(ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.password']).lower() == 'crypted':
		ranger_kms_properties_from_file['ranger.ks.jpa.jdbc.password'] = kms_database_password
	ranger_kms_properties_from_file['KMS_MASTER_KEY_PASSWD'] = kms_keystore_masterkey_password
	ranger_kms_properties_from_file['ranger.db.encrypt.key.password'] = kms_keystore_masterkey_password
	
	return ranger_kms_properties_from_file
	

def get_additional_properties_for_usersync(ranger_usersync_properties_from_file):
	ranger_conf_path = '/etc/ranger/usersync/conf'
	ranger_ugsync_default_site_xml_properties = os.path.join(ranger_conf_path, 'ranger-ugsync-default.xml')
	ranger_ugsync_site_xml_properties = os.path.join(ranger_conf_path, 'ranger-ugsync-site.xml')
	ranger_usersync_properties_from_file = import_properties_from_xml(ranger_ugsync_default_site_xml_properties, ranger_usersync_properties_from_file)
	ranger_usersync_properties_from_file = import_properties_from_xml(ranger_ugsync_site_xml_properties, ranger_usersync_properties_from_file)
	if (('unix'.lower()) in str(ranger_usersync_properties_from_file['ranger.usersync.source.impl.class']).lower()):
		print('sync_source is unix')
		ranger_usersync_properties_from_file['SYNC_SOURCE'] = 'unix'
	if (('ldap'.lower()) in str(ranger_usersync_properties_from_file['ranger.usersync.source.impl.class']).lower()):
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
		advanced_ranger_hdfs_audit_properties = dict()
		advanced_ranger_hdfs_policymgr_ssl_properties = dict()
		advanced_ranger_hdfs_security_properties = dict()

		advanced_ranger_hdfs_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hdfs_plugin_properties['hadoop.rpc.protection'] = ''
		advanced_ranger_hdfs_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_hdfs_plugin_properties['ranger-hdfs-plugin-enabled'] = 'Yes'
		advanced_ranger_hdfs_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hadoop'
		advanced_ranger_hdfs_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hadoop'
		
		advanced_ranger_hdfs_audit_properties['xasecure.audit.is.enabled'] = hdfs_plugin_install_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db'] = hdfs_plugin_install_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db.jdbc.url'] = hdfs_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db.user'] = hdfs_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db.password'] = hdfs_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = hdfs_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.credential.provider.file'] = hdfs_plugin_install_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/hadoop/hdfs/audit/db/spool')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.hdfs'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.hdfs.dir'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/hadoop/hdfs/audit/hdfs/spool')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.solr'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.solr.urls'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.solr.zookeepers'] = hdfs_plugin_install_properties.get('xasecure.audit.is.enabled','none')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = hdfs_plugin_install_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/hadoop/hdfs/audit/solr/spool')
		advanced_ranger_hdfs_audit_properties['xasecure.audit.provider.summary.enabled'] = hdfs_plugin_install_properties.get('xasecure.audit.provider.summary.enabled','false')
		
		
		advanced_ranger_hdfs_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = hdfs_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/hadoop-client/conf/ranger-plugin-keystore.jks')
		advanced_ranger_hdfs_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = hdfs_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/hadoop-client/conf/ranger-plugin-truststore.jks')
		advanced_ranger_hdfs_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = hdfs_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_hdfs_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = hdfs_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')
		
		
		
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.service.name'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.service.name','{{repo_name}}')
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.policy.source.impl'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.policy.rest.url'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.policy.rest.ssl.config.file'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.policy.rest.ssl.config.file','/etc/hadoop/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.policy.pollIntervalMs'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.policy.pollIntervalMs','30000')
		advanced_ranger_hdfs_security_properties['ranger.plugin.hdfs.policy.cache.dir'] = hdfs_plugin_install_properties.get('ranger.plugin.hdfs.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
		advanced_ranger_hdfs_security_properties['xasecure.add-hadoop-authorization'] = hdfs_plugin_install_properties.get('xasecure.add-hadoop-authorization','true')
		

		date_time_stamp = getDateTimeNow()

		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hdfs-site", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hdfs_site_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hdfs-plugin-properties", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hdfs_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hdfs-audit", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hdfs_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hdfs-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hdfs_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hdfs-security", "service_config_version_note": "Initial configuration for Ranger HDFS plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hdfs_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### hdfs_site_xml configuration :')
		for each_key in hdfs_site_xml_properties:
			print str(each_key) + ' = ' + str(hdfs_site_xml_properties[each_key])

		print ('####################### ranger_hdfs_plugin_properties configuration :')
		for each_key in advanced_ranger_hdfs_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hdfs_plugin_properties[each_key])

		print ('####################### ranger_hdfs_audit_properties configuration :')
		for each_key in advanced_ranger_hdfs_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hdfs_audit_properties[each_key])

		print ('####################### ranger_hdfs_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_hdfs_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hdfs_policymgr_ssl_properties[each_key])

		print ('####################### ranger_hdfs_security_properties configuration :')
		for each_key in advanced_ranger_hdfs_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hdfs_security_properties[each_key])

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

def port_ranger_yarn_plugin_to_ambari():
	print('Trying to add ranger yarn plugin.')
	flag_yarn_plugin_installed,yarn_plugin_installed_properties,yarn_site_xml_properties = get_yarn_plugin_configuration()
	if( flag_yarn_plugin_installed and yarn_plugin_installed_properties is not None and yarn_site_xml_properties is not None):
		print('yarn plugin is installed and enabled, adding to configurations')
		advanced_ranger_yarn_plugin_properties = dict()
		advanced_ranger_yarn_audit_properties = dict()
		advanced_ranger_yarn_policymgr_ssl_properties = dict()
		advanced_ranger_yarn_security_properties = dict()
		
		advanced_ranger_yarn_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_yarn_plugin_properties['hadoop.rpc.protection'] = ''
		advanced_ranger_yarn_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_yarn_plugin_properties['ranger-yarn-plugin-enabled'] = 'Yes'
		advanced_ranger_yarn_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'yarn'
		advanced_ranger_yarn_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'yarn'


		advanced_ranger_yarn_audit_properties['xasecure.audit.is.enabled'] = yarn_plugin_installed_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db'] = yarn_plugin_installed_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db.jdbc.url'] = yarn_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db.user'] = yarn_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db.password'] = yarn_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = yarn_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_yarn_audit_properties['xasecure.audit.credential.provider.file'] = yarn_plugin_installed_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/hadoop/yarn/audit/db/spool')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.hdfs'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.hdfs.dir'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/hadoop/yarn/audit/hdfs/spool')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.solr'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.solr.urls'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.solr.zookeepers'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_yarn_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = yarn_plugin_installed_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/hadoop/yarn/audit/solr/spool')
		advanced_ranger_yarn_audit_properties['xasecure.audit.provider.summary.enabled'] = yarn_plugin_installed_properties.get('xasecure.audit.provider.summary.enabled','false')

		advanced_ranger_yarn_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = yarn_plugin_installed_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/hadoop-client/conf/ranger-yarn-plugin-keystore.jks')
		advanced_ranger_yarn_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = yarn_plugin_installed_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/hadoop-client/conf/ranger-yarn-plugin-truststore.jks')
		advanced_ranger_yarn_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = yarn_plugin_installed_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_yarn_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = yarn_plugin_installed_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')


		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.service.name'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.service.name','{{repo_name}}')
		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.policy.source.impl'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.policy.rest.url'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.policy.rest.ssl.config.file'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.policy.rest.ssl.config.file','/etc/hadoop/conf/ranger-policymgr-ssl-yarn.xml')
		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.policy.pollIntervalMs'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.policy.pollIntervalMs','30000')
		advanced_ranger_yarn_security_properties['ranger.plugin.yarn.policy.cache.dir'] = yarn_plugin_installed_properties.get('ranger.plugin.yarn.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
		
		date_time_stamp = getDateTimeNow()
		
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"yarn-site", "service_config_version_note": "Initial configuration for Ranger YARN plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(yarn_site_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-yarn-plugin-properties", "service_config_version_note": "Initial configuration for Ranger YARN plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_yarn_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-yarn-audit", "service_config_version_note": "Initial configuration for Ranger YARN plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_yarn_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-yarn-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger YARN plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_yarn_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-yarn-security", "service_config_version_note": "Initial configuration for Ranger YARN plugin" , "tag": "' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_yarn_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'
		
		print ('####################### yarn_site_xml configuration :')
		for each_key in yarn_site_xml_properties:
			print str(each_key) + ' = ' + str(yarn_site_xml_properties[each_key])

		print ('####################### ranger_yarn_plugin_properties configuration :')
		for each_key in advanced_ranger_yarn_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_yarn_plugin_properties[each_key])

		print ('####################### ranger_yarn_audit_properties configuration :')
		for each_key in advanced_ranger_yarn_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_yarn_audit_properties[each_key])

		print ('####################### ranger_yarn_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_yarn_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_yarn_policymgr_ssl_properties[each_key])

		print ('####################### ranger_hdfs_security_properties configuration :')
		for each_key in advanced_ranger_yarn_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_yarn_security_properties[each_key])
		
		
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
					print('ranger yarn plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger yarn plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger yarn plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger yarn plugin configuration add failed in ambari.')
		return False
		

def port_ranger_hive_plugin_to_ambari():
	print('Trying to add ranger hive plugin.')
	flag_hive_plugin_installed, hive_plugin_install_properties, hive_server2_xml_properties = get_hive_plugin_configuration()
	hive_env_properties, hive_site_properties = get_hive_configs_from_ambari()
	if flag_hive_plugin_installed and hive_plugin_install_properties is not None and hive_server2_xml_properties is not None and hive_env_properties is not None and hive_site_properties is not None:
		hive_server2_xml_properties['hive.security.authorization.enabled'] = 'true'
		hive_server2_xml_properties['hive.security.authorization.manager'] = 'org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory'
		hive_server2_xml_properties['hive.security.authenticator.manager'] = 'org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator'
		hive_server2_xml_properties['hive.conf.restricted.list'] = 'hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager'
		
		print('hive plugin is installed and enabled, adding to configurations')
		advanced_ranger_hive_plugin_properties = dict()
		advanced_ranger_hive_audit_properties = dict()
		advanced_ranger_hive_policymgr_ssl_properties = dict()
		advanced_ranger_hive_security_properties = dict()

		advanced_ranger_hive_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hive_plugin_properties['jdbc.driverClassName'] = 'org.apache.hive.jdbc.HiveDriver'
		advanced_ranger_hive_plugin_properties['common.name.for.certificate'] = ''
# 		advanced_ranger_hive_plugin_properties['ranger-hive-plugin-enabled'] = 'Yes'
		advanced_ranger_hive_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hive'
		advanced_ranger_hive_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hive'
		
		advanced_ranger_hive_audit_properties['xasecure.audit.is.enabled'] = hive_plugin_install_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db'] = hive_plugin_install_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db.jdbc.url'] = hive_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db.user'] = hive_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db.password'] = hive_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = hive_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_hive_audit_properties['xasecure.audit.credential.provider.file'] = hive_plugin_install_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = hive_plugin_install_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/hive/audit/db/spool')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.hdfs'] = hive_plugin_install_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.hdfs.dir'] = hive_plugin_install_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = hive_plugin_install_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/hive/audit/hdfs/spool')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.solr'] = hive_plugin_install_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.solr.urls'] = hive_plugin_install_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.solr.zookeepers'] = hive_plugin_install_properties.get('xasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_hive_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = hive_plugin_install_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/hive/audit/solr/spool')
		advanced_ranger_hive_audit_properties['xasecure.audit.provider.summary.enabled'] = hive_plugin_install_properties.get('xasecure.audit.provider.summary.enabled','false')
		
		
		advanced_ranger_hive_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = hive_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/hive-server2/conf/ranger-plugin-keystore.jks')
		advanced_ranger_hive_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = hive_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/hive-server2/conf/ranger-plugin-truststore.jks')
		advanced_ranger_hive_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = hive_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_hive_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = hive_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')
		
		
		advanced_ranger_hive_security_properties['ranger.plugin.hive.service.name'] = hive_plugin_install_properties.get('ranger.plugin.hive.service.name','{{repo_name}}')
		advanced_ranger_hive_security_properties['ranger.plugin.hive.policy.source.impl'] = hive_plugin_install_properties.get('ranger.plugin.hive.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_hive_security_properties['ranger.plugin.hive.policy.rest.url'] = hive_plugin_install_properties.get('ranger.plugin.hive.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_hive_security_properties['ranger.plugin.hive.policy.rest.ssl.config.file'] = hive_plugin_install_properties.get('ranger.plugin.hive.policy.rest.ssl.config.file','/usr/hdp/current/hive-server2/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_hive_security_properties['ranger.plugin.hive.policy.pollIntervalMs'] = hive_plugin_install_properties.get('ranger.plugin.hive.policy.pollIntervalMs','30000')
		advanced_ranger_hive_security_properties['ranger.plugin.hive.policy.cache.dir'] = hive_plugin_install_properties.get('ranger.plugin.hive.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
		advanced_ranger_hive_security_properties['xasecure.hive.update.xapolicies.on.grant.revoke'] = hive_plugin_install_properties.get('xasecure.hive.update.xapolicies.on.grant.revoke','true')
		
		hive_env_properties['hive_security_authorization'] = 'Ranger'
		hive_site_properties['hive.server2.enable.doAs'] = 'false'
		

		date_time_stamp = getDateTimeNow()

		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hiveserver2-site", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hive_server2_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type":"hive-site", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hive_site_properties)) + ', "properties_attributes": {"final": "false"}},{"type":"hive-env", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hive_env_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hive-plugin-properties", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hive_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hive-audit", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hive_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hive-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hive_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hive-security", "service_config_version_note": "Initial configuration for Ranger HIVE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hive_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'


		print ('####################### hive_server2_xml configuration :')
		for each_key in hive_server2_xml_properties:
			print str(each_key) + ' = ' + str(hive_server2_xml_properties[each_key])

		print ('####################### ranger_hive_plugin_properties configuration :')
		for each_key in advanced_ranger_hive_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hive_plugin_properties[each_key])

		print ('####################### ranger_hive_audit_properties configuration :')
		for each_key in advanced_ranger_hive_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hive_audit_properties[each_key])

		print ('####################### ranger_hive_policymgr_ssl configuration :')
		for each_key in advanced_ranger_hive_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hive_policymgr_ssl_properties[each_key])

		print ('####################### ranger_hive_security_properties configuration :')
		for each_key in advanced_ranger_hive_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hive_security_properties[each_key])

		print ('####################### ranger_hive_env_properties configuration :')
		for each_key in hive_env_properties:
			print str(each_key) + ' = ' + str(hive_env_properties[each_key])

		print ('####################### ranger_hive_site_properties configuration :')
		for each_key in hive_site_properties:
			print str(each_key) + ' = ' + str(hive_site_properties[each_key])


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
		advanced_ranger_hbase_audit_properties = dict()
		advanced_ranger_hbase_policymgr_ssl_properties = dict()
		advanced_ranger_hbase_security_properties = dict()

		advanced_ranger_hbase_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_hbase_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_hbase_plugin_properties['ranger-hbase-plugin-enabled'] = 'Yes'
		advanced_ranger_hbase_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'hbase'
		advanced_ranger_hbase_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'hbase'
		
	
		advanced_ranger_hbase_audit_properties['xasecure.audit.is.enabled'] = hbase_plugin_install_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db'] = hbase_plugin_install_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db.jdbc.url'] = hbase_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db.user'] = hbase_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db.password'] = hbase_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = hbase_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_hbase_audit_properties['xasecure.audit.credential.provider.file'] = hbase_plugin_install_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = hbase_plugin_install_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/hbase/audit/db/spool')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.hdfs'] = hbase_plugin_install_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.hdfs.dir'] = hbase_plugin_install_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = hbase_plugin_install_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/hbase/audit/hdfs/spool')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.solr'] = hbase_plugin_install_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.solr.urls'] = hbase_plugin_install_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.solr.zookeepers'] = hbase_plugin_install_properties.get('xasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_hbase_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = hbase_plugin_install_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/hbase/audit/solr/spool')
		advanced_ranger_hbase_audit_properties['xasecure.audit.provider.summary.enabled'] = hbase_plugin_install_properties.get('xasecure.audit.provider.summary.enabled','true')
		
		advanced_ranger_hbase_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = hbase_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/hbase-client/conf/ranger-plugin-keystore.jks')
		advanced_ranger_hbase_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = hbase_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/hbase-client/conf/ranger-plugin-truststore.jks')
		advanced_ranger_hbase_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = hbase_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_hbase_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = hbase_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')
		
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.service.name'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.service.name','{{repo_name}}')
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.policy.source.impl'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.policy.rest.url'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.policy.rest.ssl.config.file'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.policy.rest.ssl.config.file','/etc/hbase/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.policy.pollIntervalMs'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.policy.pollIntervalMs','30000')
		advanced_ranger_hbase_security_properties['ranger.plugin.hbase.policy.cache.dir'] = hbase_plugin_install_properties.get('ranger.plugin.hbase.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
		advanced_ranger_hbase_security_properties['xasecure.hbase.update.xapolicies.on.grant.revoke'] = hbase_plugin_install_properties.get('xasecure.hbase.update.xapolicies.on.grant.revoke','true')

		
		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type":"hbase-site", "service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(date_time_stamp) + '","properties":' + str(
			json.dumps(hbase_site_xml_properties)) + ', "properties_attributes": {"final": "false"}},{"type": "ranger-hbase-plugin-properties","service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hbase_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hbase-audit","service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hbase_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hbase-policymgr-ssl","service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hbase_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-hbase-security","service_config_version_note": "Initial configuration for Ranger HBASE plugin" ,"tag":"' + str(
			date_time_stamp) + '", "properties":' + json.dumps(advanced_ranger_hbase_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### hbase_site_xml configuration :')
		for each_key in hbase_site_xml_properties:
			print str(each_key) + ' = ' + str(hbase_site_xml_properties[each_key])

		print ('####################### ranger_hbase_plugin_properties configuration :')
		for each_key in advanced_ranger_hbase_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hbase_plugin_properties[each_key])

		print ('####################### ranger_hbase_audit_properties configuration :')
		for each_key in advanced_ranger_hbase_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hbase_audit_properties[each_key])

		print ('####################### ranger_hbase_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_hbase_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hbase_policymgr_ssl_properties[each_key])

		print ('####################### ranger_hbase_security_properties configuration :')
		for each_key in advanced_ranger_hbase_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_hbase_security_properties[each_key])

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
		advanced_ranger_knox_audit_properties = dict()
		advanced_ranger_knox_policymgr_ssl_properties = dict()
		advanced_ranger_knox_security_properties = dict()

		advanced_ranger_knox_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_knox_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_knox_plugin_properties['ranger-knox-plugin-enabled'] = 'Yes'
		advanced_ranger_knox_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'admin'
		advanced_ranger_knox_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'admin-password'
		advanced_ranger_knox_plugin_properties['KNOX_HOME'] = '/usr/hdp/current/knox-server'

		advanced_ranger_knox_audit_properties['xasecure.audit.is.enabled'] = knox_plugin_install_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db'] = knox_plugin_install_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db.jdbc.url'] = knox_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db.user'] = knox_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db.password'] = knox_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = knox_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_knox_audit_properties['xasecure.audit.credential.provider.file'] = knox_plugin_install_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = knox_plugin_install_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/knox/audit/db/spool')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.hdfs'] = knox_plugin_install_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.hdfs.dir'] = knox_plugin_install_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = knox_plugin_install_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/knox/audit/hdfs/spool')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.solr'] = knox_plugin_install_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.solr.urls'] = knox_plugin_install_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.solr.zookeepers'] = knox_plugin_install_properties.get('xasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_knox_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = knox_plugin_install_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/knox/audit/solr/spool')
		advanced_ranger_knox_audit_properties['xasecure.audit.provider.summary.enabled'] = knox_plugin_install_properties.get('xasecure.audit.provider.summary.enabled','false')

		advanced_ranger_knox_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = knox_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/knox-server/conf/ranger-plugin-keystore.jks')
		advanced_ranger_knox_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = knox_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/knox-server/conf/ranger-plugin-truststore.jks')
		advanced_ranger_knox_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = knox_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_knox_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = knox_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')
		
		advanced_ranger_knox_security_properties['ranger.plugin.knox.service.name'] = knox_plugin_install_properties.get('ranger.plugin.knox.service.name','{{repo_name}}')
		advanced_ranger_knox_security_properties['ranger.plugin.knox.policy.source.impl'] = knox_plugin_install_properties.get('ranger.plugin.knox.policy.source.impl','org.apache.ranger.admin.client.RangerAdminJersey2RESTClient')
		advanced_ranger_knox_security_properties['ranger.plugin.knox.policy.rest.url'] = knox_plugin_install_properties.get('ranger.plugin.knox.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_knox_security_properties['ranger.plugin.knox.policy.rest.ssl.config.file'] = knox_plugin_install_properties.get('ranger.plugin.knox.policy.rest.ssl.config.file','/usr/hdp/current/knox-server/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_knox_security_properties['ranger.plugin.knox.policy.pollIntervalMs'] = knox_plugin_install_properties.get('ranger.plugin.knox.policy.pollIntervalMs','30000')
		advanced_ranger_knox_security_properties['ranger.plugin.knox.policy.cache.dir'] = knox_plugin_install_properties.get('ranger.plugin.knox.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
	

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type": "ranger-knox-plugin-properties", "service_config_version_note": "Initial configuration for Ranger KNOX plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_knox_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-knox-audit", "service_config_version_note": "Initial configuration for Ranger KNOX plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_knox_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-knox-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger KNOX plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_knox_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-knox-security", "service_config_version_note": "Initial configuration for Ranger KNOX plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_knox_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### ranger_knox_plugin_properties configuration :')
		for each_key in advanced_ranger_knox_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_knox_plugin_properties[each_key])

		print ('####################### ranger_knox_audit_properties configuration :')
		for each_key in advanced_ranger_knox_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_knox_audit_properties[each_key])

		print ('####################### ranger_knox_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_knox_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_knox_policymgr_ssl_properties[each_key])

		print ('####################### ranger_knox_security_properties configuration :')
		for each_key in advanced_ranger_knox_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_knox_security_properties[each_key])

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
		storm_site_properties = get_storm_configs_from_ambari()
		storm_site_properties['nimbus.authorizer'] = 'org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer'
		
		advanced_ranger_storm_plugin_properties = dict()
		advanced_ranger_storm_audit_properties = dict()
		advanced_ranger_storm_policymgr_ssl_properties = dict()
		advanced_ranger_storm_security_properties = dict()

		advanced_ranger_storm_plugin_properties['policy_user'] = 'storm'
		advanced_ranger_storm_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_storm_plugin_properties['ranger-storm-plugin-enabled'] = 'Yes'
		advanced_ranger_storm_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'stormtestuser@EXAMPLE.COM'
		advanced_ranger_storm_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'stormtestuser'


		advanced_ranger_storm_audit_properties['xasecure.audit.is.enabled'] = storm_plugin_install_properties.get('xasecure.audit.db.is.enabled','true')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db'] = storm_plugin_install_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db.jdbc.url'] = storm_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db.user'] = storm_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db.password'] = storm_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = storm_plugin_install_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_storm_audit_properties['xasecure.audit.credential.provider.file'] = storm_plugin_install_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = storm_plugin_install_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/storm/audit/db/spool')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.hdfs'] = storm_plugin_install_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.hdfs.dir'] = storm_plugin_install_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = storm_plugin_install_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/storm/audit/hdfs/spool')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.solr'] = storm_plugin_install_properties.get('xasecure.audit.destination.solr','false')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.solr.urls'] = storm_plugin_install_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.solr.zookeepers'] = storm_plugin_install_properties.get('xasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_storm_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = storm_plugin_install_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/storm/audit/solr/spool')
		advanced_ranger_storm_audit_properties['xasecure.audit.provider.summary.enabled'] = storm_plugin_install_properties.get('xasecure.audit.provider.summary.enabled','false')
		
		
		advanced_ranger_storm_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = storm_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/storm-client/conf/ranger-plugin-keystore.jks')
		advanced_ranger_storm_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = storm_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/storm-client/conf/ranger-plugin-truststore.jks')
		advanced_ranger_storm_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = storm_plugin_install_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file{{credential_file}}')
		advanced_ranger_storm_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = storm_plugin_install_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file{{credential_file}}')


		advanced_ranger_storm_security_properties['ranger.plugin.storm.service.name'] = storm_plugin_install_properties.get('ranger.plugin.storm.service.name','{{repo_name}}')
		advanced_ranger_storm_security_properties['ranger.plugin.storm.policy.source.impl'] = storm_plugin_install_properties.get('ranger.plugin.storm.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_storm_security_properties['ranger.plugin.storm.policy.rest.url'] = storm_plugin_install_properties.get('ranger.plugin.storm.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_storm_security_properties['ranger.plugin.storm.policy.rest.ssl.config.file'] = storm_plugin_install_properties.get('ranger.plugin.storm.policy.rest.ssl.config.file','/usr/hdp/current/storm-client/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_storm_security_properties['ranger.plugin.storm.policy.pollIntervalMs'] = storm_plugin_install_properties.get('ranger.plugin.storm.policy.pollIntervalMs','30000')
		advanced_ranger_storm_security_properties['ranger.plugin.storm.policy.cache.dir'] = storm_plugin_install_properties.get('ranger.plugin.storm.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')


		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type": "ranger-storm-plugin-properties", "service_config_version_note": "Initial configuration for Ranger STORM plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_storm_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-storm-audit", "service_config_version_note": "Initial configuration for Ranger STORM plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_storm_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-storm-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger STORM plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_storm_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-storm-security", "service_config_version_note": "Initial configuration for Ranger STORM plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_storm_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		print ('####################### ranger_storm_plugin_properties configuration :')
		for each_key in advanced_ranger_storm_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_storm_plugin_properties[each_key])

		print ('####################### ranger_storm_audit_properties configuration :')
		for each_key in advanced_ranger_storm_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_storm_audit_properties[each_key])

		print ('####################### ranger_storm_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_storm_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_storm_policymgr_ssl_properties[each_key])

		print ('####################### ranger_storm_security_properties configuration :')
		for each_key in advanced_ranger_storm_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_storm_security_properties[each_key])

		

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

def port_ranger_kafka_plugin_to_ambari():
	print('Trying to add ranger kafka plugin.')
	flag_kafka_plugin_installed,kafka_plugin_installed_properties = get_kafka_plugin_configuration()
	if flag_kafka_plugin_installed and kafka_plugin_installed_properties is not None:
		advanced_ranger_kafka_plugin_properties = dict()
		advanced_ranger_kafka_audit_properties = dict()
		advanced_ranger_kafka_policymgr_ssl_properties = dict()
		advanced_ranger_kafka_security_properties = dict()
		advanced_kafka_log4j_properties = dict()
		
		print('Kafka plugin is installed and enabled, adding to configurations')
		advanced_kafka_broker_properties = get_kafka_configs_from_ambari()
		advanced_kafka_broker_properties['authorizer.class.name'] = 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer'
		
		
		advanced_ranger_kafka_plugin_properties['policy_user'] = 'ambari-qa'
		advanced_ranger_kafka_plugin_properties['hadoop.rpc.protection'] = ''
		advanced_ranger_kafka_plugin_properties['common.name.for.certificate'] = ''
		advanced_ranger_kafka_plugin_properties['zookeeper.connect'] = 'localhost:2181'
		advanced_ranger_kafka_plugin_properties['ranger-kafka-plugin-enabled'] = 'Yes'
		advanced_ranger_kafka_plugin_properties['REPOSITORY_CONFIG_USERNAME'] = 'kafka'
		advanced_ranger_kafka_plugin_properties['REPOSITORY_CONFIG_PASSWORD'] = 'kafka'
		
		
		advanced_ranger_kafka_audit_properties['xasecure.audit.is.enabled'] = kafka_plugin_installed_properties.get('xasecure.audit.is.enabled','true')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db'] = kafka_plugin_installed_properties.get('xasecure.audit.db.is.enabled','false')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db.jdbc.url'] = kafka_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.url','{{audit_jdbc_url}}')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db.user'] = kafka_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.user','{{xa_audit_db_user}}')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db.password'] = kafka_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.password','crypted')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db.jdbc.driver'] = kafka_plugin_installed_properties.get('xasecure.audit.jpa.javax.persistence.jdbc.driver','{{jdbc_driver}}')
		advanced_ranger_kafka_audit_properties['xasecure.audit.credential.provider.file'] = kafka_plugin_installed_properties.get('xasecure.audit.credential.provider.file','jceks://file{{credential_file}}')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.db.batch.filespool.dir'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.db.batch.filespool.dir','/var/log/kafka/audit/db/spool')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.hdfs'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.hdfs','true')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.hdfs.dir'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.hdfs.dir','hdfs://NAMENODE_HOSTNAME:8020/ranger/audit')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.hdfs.batch.filespool.dir'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.hdfs.batch.filespool.dir','/var/log/kafka/audit/hdfs/spool')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.solr'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.solr','true')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.solr.urls'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.solr.urls','{{ranger_audit_solr_urls}}')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.solr.zookeepers'] = kafka_plugin_installed_properties.get('xasecure.audit.db.is.enabledxasecure.audit.destination.solr.zookeepers','none')
		advanced_ranger_kafka_audit_properties['xasecure.audit.destination.solr.batch.filespool.dir'] = kafka_plugin_installed_properties.get('xasecure.audit.destination.solr.batch.filespool.dir','/var/log/kafka/audit/solr/spool')
		advanced_ranger_kafka_audit_properties['xasecure.audit.provider.summary.enabled'] = kafka_plugin_installed_properties.get('xasecure.audit.db.is.enabled','true')
		
		advanced_ranger_kafka_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore'] = kafka_plugin_installed_properties.get('xasecure.policymgr.clientssl.keystore','/usr/hdp/current/kafka-broker/config/ranger-plugin-keystore.jks')
		advanced_ranger_kafka_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore'] = kafka_plugin_installed_properties.get('xasecure.policymgr.clientssl.truststore','/usr/hdp/current/kafka-broker/config/ranger-plugin-truststore.jks')
		advanced_ranger_kafka_policymgr_ssl_properties['xasecure.policymgr.clientssl.keystore.credential.file'] = kafka_plugin_installed_properties.get('xasecure.policymgr.clientssl.keystore.credential.file','jceks://file/{{credential_file}}')
		advanced_ranger_kafka_policymgr_ssl_properties['xasecure.policymgr.clientssl.truststore.credential.file'] = kafka_plugin_installed_properties.get('xasecure.policymgr.clientssl.truststore.credential.file','jceks://file/{{credential_file}}')
		
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.service.name'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.service.name','{{repo_name}}')
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.policy.source.impl'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.policy.source.impl','org.apache.ranger.admin.client.RangerAdminRESTClient')
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.policy.rest.url'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.policy.rest.url','{{policymgr_mgr_url}}')
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.policy.rest.ssl.config.file'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.policy.rest.ssl.config.file','/etc/kafka/conf/ranger-policymgr-ssl.xml')
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.policy.pollIntervalMs'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.policy.pollIntervalMs','30000')
		advanced_ranger_kafka_security_properties['ranger.plugin.kafka.policy.cache.dir'] = kafka_plugin_installed_properties.get('ranger.plugin.kafka.policy.cache.dir','/etc/ranger/{{repo_name}}/policycache')
		
		advanced_kafka_log4j_properties['content'] = kafka_plugin_installed_properties.get('kafka.log4j.properties','') 

		date_time_stamp = getDateTimeNow()
		plugin_configuration_data = '[{"Clusters":{"desired_config":[{"type": "kafka-broker", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_kafka_broker_properties) + ',"properties_attributes": {"final": "false"}},{"type": "kafka-log4j", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_kafka_log4j_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-kafka-plugin-properties", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_kafka_plugin_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-kafka-audit", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_kafka_audit_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-kafka-policymgr-ssl", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_kafka_policymgr_ssl_properties) + ',"properties_attributes": {"final": "false"}},{"type": "ranger-kafka-security", "service_config_version_note": "Initial configuration for Ranger KAFKA plugin" ,"tag":"' + str(date_time_stamp) + '", "properties":' + json.dumps(
			advanced_ranger_kafka_security_properties) + ',"properties_attributes": {"final": "false"}}]}}]'

		
		print ('####################### kafka_broker_properties configuration :')
		for each_key in advanced_kafka_broker_properties:
			print str(each_key) + ' = ' + str(advanced_kafka_broker_properties[each_key])

		print ('####################### kafka_log4j_properties configuration :')
		for each_key in advanced_kafka_log4j_properties:
			print str(each_key) + ' = ' + str(advanced_kafka_log4j_properties[each_key])

		
		print ('####################### ranger_kafka_plugin_properties configuration :')
		for each_key in advanced_ranger_kafka_plugin_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_kafka_plugin_properties[each_key])

		print ('####################### ranger_kafka_audit_properties configuration :')
		for each_key in advanced_ranger_kafka_audit_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_kafka_audit_properties[each_key])

		print ('####################### ranger_kafka_policymgr_ssl_properties configuration :')
		for each_key in advanced_ranger_kafka_policymgr_ssl_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_kafka_policymgr_ssl_properties[each_key])

		print ('####################### ranger_kafka_security_properties configuration :')
		for each_key in advanced_ranger_kafka_security_properties:
			print str(each_key) + ' = ' + str(advanced_ranger_kafka_security_properties[each_key])

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
					print('ranger kafka plugin configuration added successfully in ambari.')
					return True
				else:
					print('ranger kafka plugin configuration add failed in ambari.')
					return False
			else:
				print('ranger kafka plugin configuration add failed in ambari.')
				return False
		else:
			print ('exiting installation without configuration !')
			sys.exit(0)
	else:
		print('ranger kafka plugin configuration add failed in ambari.')
		return False



def get_hdfs_plugin_configuration():
	flag_hdfs_plugin_installed = False
	hdfs_plugin_install_properties = dict()
	print('hdfs plugin is present and installed to ranger,getting additional properties from installed files.')
	base_hadoop_conf_path = '/etc/hadoop/conf/'
	hdfs_site_xml_path = os.path.join(base_hadoop_conf_path, 'hdfs-site.xml')
	hdfs_site_xml_properties = import_properties_from_xml(hdfs_site_xml_path)
	ranger_audit_xml_path = os.path.join(base_hadoop_conf_path, 'ranger-hdfs-audit.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, hdfs_plugin_install_properties)
	ranger_hdfs_security_xml_path = os.path.join(base_hadoop_conf_path, 'ranger-hdfs-security.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(ranger_hdfs_security_xml_path, hdfs_plugin_install_properties)
	ranger_policy_ssl_xml_path = os.path.join(base_hadoop_conf_path, 'ranger-policymgr-ssl.xml')
	hdfs_plugin_install_properties = import_properties_from_xml(ranger_policy_ssl_xml_path, hdfs_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hdfs', hdfs_plugin_install_properties)
	if(flag_plugin_installed):
		flag_hdfs_plugin_installed = True
	return flag_hdfs_plugin_installed, hdfs_plugin_install_properties, hdfs_site_xml_properties


def get_yarn_plugin_configuration():
	flag_yarn_plugin_installed = False
	yarn_plugin_installed_properties = dict()
	print('yarn plugin is present and installed to ranger,getting additional properties from installed files.')
	base_yarn_conf_path = '/etc/hadoop/conf/'
	yarn_site_xml_path = os.path.join(base_yarn_conf_path,'yarn-site.xml')
	yarn_site_xml_properties = import_properties_from_xml(yarn_site_xml_path)
	ranger_yarn_audit_properties_path = os.path.join(base_yarn_conf_path,'ranger-yarn-audit.xml')
	yarn_plugin_installed_properties = import_properties_from_xml(ranger_yarn_audit_properties_path, yarn_plugin_installed_properties)
	ranger_yarn_security_properties_path = os.path.join(base_yarn_conf_path,'ranger-yarn-security.xml')
	yarn_plugin_installed_properties = import_properties_from_xml(ranger_yarn_security_properties_path, yarn_plugin_installed_properties)
	ranger_policymgr_ssl_properties_path = os.path.join(base_yarn_conf_path,'ranger-policymgr-ssl.xml')
	yarn_plugin_installed_properties = import_properties_from_xml(ranger_policymgr_ssl_properties_path, yarn_plugin_installed_properties)
	flag_plugin_installed = check_plugin_enabled('yarn', yarn_plugin_installed_properties)
	if(flag_plugin_installed):
		flag_yarn_plugin_installed = True
	return flag_yarn_plugin_installed,yarn_plugin_installed_properties,yarn_site_xml_properties
	

def get_hive_plugin_configuration():
	flag_hive_plugin_installed = False
	hive_plugin_install_properties = dict()
	print('hive plugin is present and installed to ranger, configuring to setup in ambari.')
	base_hive_conf_path = '/etc/hive/conf/conf.server/'
	hive_server2_xml_path = os.path.join(base_hive_conf_path, 'hiveserver2-site.xml')
	hive_server2_xml_properties = import_properties_from_xml(hive_server2_xml_path)
	ranger_audit_xml_path = os.path.join(base_hive_conf_path, 'ranger-hive-audit.xml')
	hive_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, hive_plugin_install_properties)
	ranger_hive_security_xml_path = os.path.join(base_hive_conf_path, 'ranger-hive-security.xml')
	hive_plugin_install_properties = import_properties_from_xml(ranger_hive_security_xml_path, hive_plugin_install_properties)
	ranger_policy_ssl_xml_path = os.path.join(base_hive_conf_path, 'ranger-policymgr-ssl.xml')
	hive_plugin_install_properties = import_properties_from_xml(ranger_policy_ssl_xml_path, hive_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hive', hive_plugin_install_properties)
	
	if(flag_plugin_installed):
		flag_hive_plugin_installed = True
	return flag_hive_plugin_installed, hive_plugin_install_properties, hive_server2_xml_properties

def get_hive_configs_from_ambari():
	desired_config_url = ambari_cluster_url + '?fields=Clusters/desired_configs'
	request_result = call_ambari_api(desired_config_url,'GET',ambari_username_password,None)
	response_code = None
	desired_configs_response = None
   	if request_result is not None:
		response_code = request_result.getcode()
		desired_configs_response = json.loads(json.JSONEncoder().encode(request_result.read()))
	
	hive_site_tag = str(json.loads(desired_configs_response)['Clusters']['desired_configs']['hive-site']['tag'])
	hive_env_tag = str(json.loads(desired_configs_response)['Clusters']['desired_configs']['hive-env']['tag'])
	
	hive_env_properties_url = ambari_cluster_url + '/configurations?type=hive-env&tag='+hive_env_tag
	hive_env_properties_response = None
	print ('hive_env_properties_url = ' + str(hive_env_properties_url))
	request_result = call_ambari_api(hive_env_properties_url,'GET',ambari_username_password,None)
	if request_result is not None:
		response_code = request_result.getcode()
		hive_env_properties_response = json.loads(json.JSONEncoder().encode(request_result.read()))
	print 'hive-env len response = ' , len(json.loads(hive_env_properties_response)['items'])
	hive_env_properties =  json.loads(hive_env_properties_response)['items'][0]['properties']
	print ('hive_env_properties = ' + str(hive_env_properties))
	
	hive_site_properties_url = ambari_cluster_url + '/configurations?type=hive-site&tag='+hive_site_tag
	hive_site_properties_response = None
	request_result = call_ambari_api(hive_site_properties_url,'GET',ambari_username_password,None)
	if request_result is not None:
		response_code = request_result.getcode()
		hive_site_properties_response = json.loads(json.JSONEncoder().encode(request_result.read()))
	print 'hive-site len response = ' , len(json.loads(hive_site_properties_response)['items'])
	hive_site_properties =  json.loads(hive_site_properties_response)['items'][0]['properties']
	print ('hive_site_properties = ' + str(hive_site_properties))
		
	return hive_env_properties,hive_site_properties



def get_hbase_plugin_configuration():
	flag_hbase_plugin_installed = False
	hbase_plugin_install_properties = dict()
	print('hbase plugin is present and installed to ranger, configuring to setup in ambari.')
	base_hbase_conf_path = '/etc/hbase/conf/'
	hbase_site_xml_path = os.path.join(base_hbase_conf_path, 'hbase-site.xml')
	hbase_site_xml_properties = import_properties_from_xml(hbase_site_xml_path)
	ranger_audit_xml_path = os.path.join(base_hbase_conf_path, 'ranger-hbase-audit.xml')
	hbase_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, hbase_plugin_install_properties)
	ranger_hbase_security_xml_path = os.path.join(base_hbase_conf_path, 'ranger-hbase-security.xml')
	hbase_plugin_install_properties = import_properties_from_xml(ranger_hbase_security_xml_path, hbase_plugin_install_properties)
	ranger_policy_ssl_xml_path = os.path.join(base_hbase_conf_path, 'ranger-policymgr-ssl.xml')
	hbase_plugin_install_properties = import_properties_from_xml(ranger_policy_ssl_xml_path, hbase_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('hbase', hbase_plugin_install_properties)
	if(flag_plugin_installed):
		flag_hbase_plugin_installed = True
	return flag_hbase_plugin_installed, hbase_plugin_install_properties, hbase_site_xml_properties


def get_knox_plugin_configuration():
	flag_knox_plugin_installed = False
	knox_plugin_install_properties = dict()
	print('knox plugin is present and installed to ranger, configuring to setup in ambari.')
	base_knox_conf_path = '/etc/knox/conf/'
	ranger_audit_xml_path = os.path.join(base_knox_conf_path, 'ranger-knox-audit.xml')
	knox_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, knox_plugin_install_properties)
	ranger_knox_security_xml_path = os.path.join(base_knox_conf_path, 'ranger-knox-security.xml')
	knox_plugin_install_properties = import_properties_from_xml(ranger_knox_security_xml_path, knox_plugin_install_properties)
	ranger_policy_ssl_xml_path = os.path.join(base_knox_conf_path, 'ranger-policymgr-ssl.xml')
	knox_plugin_install_properties = import_properties_from_xml(ranger_policy_ssl_xml_path, knox_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('knox', knox_plugin_install_properties)
	if(flag_plugin_installed):
		flag_knox_plugin_installed = True
	return flag_knox_plugin_installed, knox_plugin_install_properties



def get_storm_plugin_configuration():
	flag_storm_plugin_installed = False
	storm_plugin_install_properties = dict()
	print('storm plugin is present and installed to ranger, configuring to setup in ambari.')
	base_storm_conf_path = '/etc/storm/conf/'
	ranger_audit_xml_path = os.path.join(base_storm_conf_path, 'ranger-storm-audit.xml')
	storm_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, storm_plugin_install_properties)
	ranger_storm_security_xml_path = os.path.join(base_storm_conf_path, 'ranger-storm-security.xml')
	storm_plugin_install_properties = import_properties_from_xml(ranger_storm_security_xml_path, storm_plugin_install_properties)
	ranger_policy_ssl_xml_path = os.path.join(base_storm_conf_path, 'ranger-policymgr-ssl.xml')
	storm_plugin_install_properties = import_properties_from_xml(ranger_policy_ssl_xml_path, storm_plugin_install_properties)
	flag_plugin_installed = check_plugin_enabled('storm', storm_plugin_install_properties)
	if(flag_plugin_installed):
		flag_storm_plugin_installed = True
	return flag_storm_plugin_installed, storm_plugin_install_properties


def get_storm_configs_from_ambari():
	desired_config_url = ambari_cluster_url + '?fields=Clusters/desired_configs'
	request_result = call_ambari_api(desired_config_url,'GET',ambari_username_password,None)
	response_code = None
	desired_configs_response = None
   	if request_result is not None:
		response_code = request_result.getcode()
		desired_configs_response = json.loads(json.JSONEncoder().encode(request_result.read()))
	
	storm_site_tag = str(json.loads(desired_configs_response)['Clusters']['desired_configs']['storm-site']['tag'])
	print ('storm_site_tag = ' + storm_site_tag)

	kafka_broker_properties_url = ambari_cluster_url + '/configurations?type=storm-site&tag='+storm_site_tag

	request_result = call_ambari_api(kafka_broker_properties_url,'GET',ambari_username_password,None)
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
	print 'storm-site len response = ' , len(json.loads(response)['items'])
	storm_site_properties =  json.loads(response)['items'][0]['properties']
	print ('storm_site_properties = ' + str(storm_site_properties))
	
	return storm_site_properties




def get_kafka_plugin_configuration():
	flag_kafka_plugin_installed = False
	kafka_plugin_install_properties = dict()
	print('kafka plugin is present and installed to ranger configuring to setup ambari')
	base_kafka_conf_path = '/etc/kafka/conf/'
	ranger_audit_xml_path = os.path.join(base_kafka_conf_path,'ranger-kafka-audit.xml')
	kafka_plugin_install_properties = import_properties_from_xml(ranger_audit_xml_path, kafka_plugin_install_properties)
	ranger_kafka_security_xml_path = os.path.join(base_kafka_conf_path,'ranger-kafka-security.xml')
	kafka_plugin_install_properties = import_properties_from_xml(ranger_kafka_security_xml_path, kafka_plugin_install_properties)
	ranger_policymgr_ssl_xml_path = os.path.join(base_kafka_conf_path,'ranger-policymgr-ssl.xml')
	kafka_plugin_install_properties = import_properties_from_xml(ranger_policymgr_ssl_xml_path, kafka_plugin_install_properties)
	kafka_log4j_xml_path = os.path.join(base_kafka_conf_path,'log4j.properties')
	kafka_plugin_install_properties['kafka.log4j.properties'] = read_properties_file(kafka_log4j_xml_path)
	
	flag_plugin_installed = check_plugin_enabled('kafka', kafka_plugin_install_properties)
	if flag_plugin_installed:
		flag_kafka_plugin_installed = True
	return flag_kafka_plugin_installed, kafka_plugin_install_properties

def get_kafka_configs_from_ambari():
	desired_config_url = ambari_cluster_url + '?fields=Clusters/desired_configs'
	request_result = call_ambari_api(desired_config_url,'GET',ambari_username_password,None)
	response_code = None
	desired_configs_response = None
   	if request_result is not None:
		response_code = request_result.getcode()
		desired_configs_response = json.loads(json.JSONEncoder().encode(request_result.read()))

	kafka_broker_tag = str(json.loads(desired_configs_response)['Clusters']['desired_configs']['kafka-broker']['tag'])
	print ('kafka_broker_tag = ' + kafka_broker_tag)

	kafka_broker_properties_url = ambari_cluster_url + '/configurations?type=kafka-broker&tag='+kafka_broker_tag

	request_result = call_ambari_api(kafka_broker_properties_url,'GET',ambari_username_password,None)
	if request_result is not None:
		response_code = request_result.getcode()
		response = json.loads(json.JSONEncoder().encode(request_result.read()))
	print 'kafka-broker len response = ' , len(json.loads(response)['items'])
	kafka_broker_properties =  json.loads(response)['items'][0]['properties']
	print ('kafka_broker_properties = ' + str(kafka_broker_properties))
	
	return kafka_broker_properties

	

def check_plugin_enabled(component_name, component_plugin_install_properties):
	flag_plugin_installed = False
	repository_key = 'ranger.plugin.' + component_name +  '.service.name'
	if not (str(component_plugin_install_properties[repository_key]).strip() == ''):
		repo_base_path = os.path.join('/etc/ranger', component_plugin_install_properties[repository_key])
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

def read_properties_file(properties_file_path):
	file_text = ''
	if(os.path.isfile(properties_file_path)):
		print('property file exists reading file content')
		file_text = open(properties_file_path,'r').read()
	else:
		print('file not found at path : ' + str(properties_file_path))
	return file_text
		


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
		commandtorun = ['java', '-cp', finalLibPath, 'org.apache.ranger.credentialapi.buildks', 'create', aliasKey, '-value', aliasValue, '-provider', finalFilePath]
		p = Popen(commandtorun, stdin=PIPE, stdout=PIPE, stderr=PIPE)
		output, error = p.communicate()
		statuscode = p.returncode
		return statuscode
	elif getorcreateorlist == 'get':
		commandtorun = ['java', '-cp', finalLibPath, 'org.apache.ranger.credentialapi.buildks', 'get', aliasKey, '-provider', finalFilePath]
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
				port_ranger_admin_installation_to_ambari()
			elif function_to_call == 2:
				print('Porting ranger User-sync installation details to ambari.')
				port_ranger_usersync_installation_to_ambari()
			elif function_to_call == 3:
				print('Porting ranger kms installation details to ambari.')
				port_ranger_kms_installation_to_ambari()
			elif function_to_call == 4:
				print('Porting ranger hdfs plugin details to ambari.')
				port_ranger_hdfs_plugin_to_ambari()
			elif function_to_call == 5:
				print('Porting ranger yarn plugin details to ambari.')
				port_ranger_yarn_plugin_to_ambari()
			elif function_to_call == 6:
				print('Porting ranger Hive plugin details to ambari.')
				port_ranger_hive_plugin_to_ambari()
			elif function_to_call == 7:
				print('Porting ranger Hbase plugin details to ambari.')
				port_ranger_hbase_plugin_to_ambari()
			elif function_to_call == 8:
				print('Porting ranger Knox plugin details to ambari.')
				port_ranger_knox_plugin_to_ambari()
			elif function_to_call == 9:
				print('Porting ranger Storm plugin details to ambari.')
				port_ranger_storm_plugin_to_ambari()
			elif function_to_call == 10:
				print('Porting ranger Kafka plugin details to ambari.')
				port_ranger_kafka_plugin_to_ambari()
			else:
				print ('Unsupported option passed for installation, please pass proper supported option')

	else:
		print('Usage :'
		      '\n python import_ranger_to_ambari.py  {install option eg. 1} { ambari server url (eg.  http://100.100.100.100:8080) } {ambari server username password (eg. demo_user:demo_pass) } {cluster name (eg. ambari_cluster)} {FQDN of host having Ranger Admin or Ranger Usersync or plugins installe (eg. ambari.server.com)} '
		      '\n Actual call will be like : python ranger_port_script.py 1 http://100.100.100.100:8080 demo_user:demo_pass ambari_cluster ambari.server.com'
		      '\n Pass first parameter as 1 for Ranger integration with Ambari.'
		      '\n Pass first parameter as 2 for Ranger User-sync integration with Ambari.'
		      '\n Pass first parameter as 3 for Ranger KMS integration with Ambari.'
		      '\n Pass first parameter as 4 for Ranger Hdfs Plugin integration with Ambari.'
		      '\n Pass first parameter as 5 for Ranger Yarn Plugin integration with Ambari.'
		      '\n Pass first parameter as 6 for Ranger Hive Plugin integration with Ambari.'
		      '\n Pass first parameter as 7 for Ranger Hbase Plugin integration with Ambari.'
		      '\n Pass first parameter as 8 for Ranger Knox Plugin integration with Ambari.'
		      '\n Pass first parameter as 9 for Ranger Storm Plugin integration with Ambari.'
		      '\n Pass first parameter as 10 for Ranger Kafka Plugin integration with Ambari.')

	sys.exit(0)

