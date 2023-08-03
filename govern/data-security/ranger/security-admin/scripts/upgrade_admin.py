#!/usr/bin/python
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from __future__ import print_function
try:
	from StringIO import StringIO
except ImportError:
	from io import StringIO
try:
	from ConfigParser import ConfigParser
except ImportError:
	from configparser import ConfigParser
import xml.etree.ElementTree as ET
import os,sys,getopt
from os import listdir
from os.path import isfile, join, dirname, basename
from time import strftime, localtime
import shutil

debugLevel = 1
generateXML = 0
installPropFileName = 'install.properties'

tempLibFolder = "./upgrade-temp"

def showUsage():
	print("upgrade_admin.py [-g] [-h]")
	print("This script will generate %s based on currently installed ranger (v0.4.*) configuration." % (installPropFileName))
	print(" -g option will generate ranger-admin-site.xml in the current directory.")
	print(" -h will display help text.")

try:
	opts, args = getopt.getopt(sys.argv[1:],"gh")
except getopt.GetoptError:
	showUsage()
	sys.exit(2)
for opt,arg in opts:
	if (opt == '-g'):
		generateXML = 1
	elif (opt == '-h'):
		showUsage()
		sys.exit(0)
#
# configDirectory: where OLD (champlain) configuration exists and NEW (dal) configuration is written to
#
configDirectory = '/etc/ranger/admin/conf' 
rangerJAASDirectoryName = join(configDirectory,'ranger_jaas')

xaSystemPropFile = 'xa_system.properties' 
ldapPropFile = 'xa_ldap.properties'
rangerJAASPropFile = 'unixauth.properties' 
securityContextFile  = 'security-applicationContext.xml' 
webserverConfigFile = 'ranger_webserver.properties'

rangerSiteXMLFile = "ranger-admin-site.xml"


#
# xmlTemplateDirectory: directory where all of the xml templates are kept here
#
templateDirectoryName = './templates-upgrade'
rangerSiteTemplateXMLFile = "ranger-admin-site-template.xml"

#
# Install Properties To Ranger Properties
#
config2xmlMAP = {
	'service.host':'ranger.service.host',
	'http.enabled':'ranger.service.http.enabled',
	'http.service.port':'ranger.service.http.port',
	'service.shutdownPort':'ranger.service.shutdown.port',
	'service.shutdownCommand':'ranger.service.shutdown.command',
	'https.service.port':'ranger.service.https.port',
	'https.attrib.SSLEnabled':'ranger.service.https.attrib.ssl.enabled',
	'https.attrib.sslProtocol':'ranger.service.https.attrib.ssl.protocol',
	'https.attrib.clientAuth':'ranger.service.https.attrib.client.auth',
	'https.attrib.keyAlias':'ranger.service.https.attrib.keystore.keyalias',
	'https.attrib.keystorePass':'ranger.service.https.attrib.keystore.pass',
	'https.attrib.keystoreFile':'ranger.https.attrib.keystore.file',
	'accesslog.dateformat':'ranger.accesslog.dateformat',
	'accesslog.pattern':'ranger.accesslog.pattern',
	'xa.webapp.url.root':'ranger.externalurl',
	'xa.webapp.contextName':'ranger.contextName',
	'xa.jpa.showsql':'ranger.jpa.showsql',
	'xa.env.local':'ranger.env.local',
	'jdbc.dialect':'ranger.jpa.jdbc.dialect',
	'jdbc.driver':'ranger.jpa.jdbc.driver',
	'jdbc.url':'ranger.jpa.jdbc.url',
	'jdbc.user':'ranger.jpa.jdbc.user',
	'jdbc.password':'ranger.jpa.jdbc.password',
	'jdbc.maxPoolSize':'ranger.jpa.jdbc.maxpoolsize',
	'jdbc.minPoolSize':'ranger.jpa.jdbc.minpoolsize',
	'jdbc.initialPoolSize':'ranger.jpa.jdbc.initialpoolsize',
	'jdbc.maxIdleTime':'ranger.jpa.jdbc.maxidletime',
	'jdbc.maxStatements':'ranger.jpa.jdbc.maxstatements',
	'jdbc.preferredTestQuery':'ranger.jpa.jdbc.preferredtestquery',
	'jdbc.idleConnectionTestPeriod':'ranger.jpa.jdbc.idleconnectiontestperiod',
	'xaDB.jdbc.credential.alias':'ranger.jpa.jdbc.credential.alias',
	'xaDB.jdbc.credential.provider.path':'ranger.jpa.jdbc.credential.provider.path',
	'xa.logs.base.dir':'ranger.logs.base.dir',
	'xa.scheduler.enabled':'ranger.scheduler.enabled',
	'xa.audit.store':'ranger.audit.source.type',
	'audit_elasticsearch_urls':'ranger.audit.elasticsearch.urls',
	'audit_elasticsearch_port':'ranger.audit.elasticsearch.port',
	'audit_elasticsearch_user':'ranger.audit.elasticsearch.user',
	'audit_elasticsearch_password':'ranger.audit.elasticsearch.password',
	'audit_cloudwatch_region':'ranger.audit.amazon_cloudwatch.region',
	'audit_cloudwatch_log_group':'ranger.audit.amazon_cloudwatch.log_group',
	'audit_cloudwatch_log_stream_prefix':'ranger.audit.amazon_cloudwatch.log_stream_prefix',
	'audit_solr_urls':'ranger.audit.solr.urls',
	'auditDB.jdbc.dialect':'ranger.jpa.audit.jdbc.dialect',
	'auditDB.jdbc.driver':'ranger.jpa.audit.jdbc.driver',
	'auditDB.jdbc.url':'ranger.jpa.audit.jdbc.url',
	'auditDB.jdbc.user':'ranger.jpa.audit.jdbc.user',
	'auditDB.jdbc.password':'ranger.jpa.audit.jdbc.password',
	'auditDB.jdbc.credential.alias':'ranger.jpa.audit.jdbc.credential.alias',
	'auditDB.jdbc.credential.provider.path':'ranger.jpa.audit.jdbc.credential.provider.path',
	'authentication_method':'ranger.authentication.method',
	'xa_ldap_url':'ranger.ldap.url',
	'xa_ldap_userDNpattern':'ranger.ldap.user.dnpattern',
	'xa_ldap_groupSearchBase':'ranger.ldap.group.searchbase',
	'xa_ldap_groupSearchFilter':'ranger.ldap.group.searchfilter',
	'xa_ldap_groupRoleAttribute':'ranger.ldap.group.roleattribute',
	'xa_ldap_ad_domain':'ranger.ldap.ad.domain',
	'xa_ldap_ad_url':'ranger.ldap.ad.url' } 

def archiveFile(originalFileName):
		archiveDir = dirname(originalFileName)
		archiveFileName = "." + basename(originalFileName) + "." + (strftime("%d%m%Y%H%M%S", localtime()))
		movedFileName = join(archiveDir,archiveFileName)
		print("INFO: moving [%s] to [%s] ......." % (originalFileName,movedFileName))
		os.rename(originalFileName, movedFileName)

def getPropertiesConfigMap(configFileName):
	ret = {}
	config = StringIO()
	config.write('[dummysection]\n')
	config.write(open(configFileName).read())
	config.seek(0,os.SEEK_SET)
	fcp = ConfigParser()
	fcp.optionxform = str
	fcp.readfp(config)
	for k,v in fcp.items('dummysection'):
		ret[k] = v
	return ret

def getPropertiesKeyList(configFileName):
	ret = []
	config = StringIO()
	config.write('[dummysection]\n')
	config.write(open(configFileName).read())
	config.seek(0,os.SEEK_SET)
	fcp = ConfigParser()
	fcp.optionxform = str
	fcp.readfp(config)
	for k,v in fcp.items('dummysection'):
		ret.append(k)
	return ret

def	readFromJCKSFile(jcksFileName,propName):
	fn = jcksFileName
	cmd = "java -cp './cred/lib/*' org.apache.ranger.credentialapi.buildks get '" + propName + "' -provider jceks://file" + fn + " 2> /dev/null"
	pwd = os.popen(cmd).read()
	pwd = pwd.strip()
	return pwd

def writeXMLUsingProperties(xmlTemplateFileName,prop,xmlOutputFileName):
	tree = ET.parse(xmlTemplateFileName)
	root = tree.getroot()
	for config in root.iter('property'):
		name = config.find('name').text
		if (name in list(prop)):
			config.find('value').text = prop[name]
		else:
			print("ERROR: key not found: %s" % (name))
	if isfile(xmlOutputFileName):
		archiveFile(xmlOutputFileName)
	tree.write(xmlOutputFileName)

def main():
	installFileName = join(templateDirectoryName, installPropFileName)
	installProps = {}
	rangerprops = {}
	
	xaSystemPropFileName = join(configDirectory, xaSystemPropFile)
	xaSysProps = getPropertiesConfigMap(xaSystemPropFileName)

	ldapPropFileName = join(configDirectory, ldapPropFile) 
	xaLdapProps = getPropertiesConfigMap (ldapPropFileName)
	
	jaasPropFileName = join(rangerJAASDirectoryName, rangerJAASPropFile)
	unixauthProps = getPropertiesConfigMap (jaasPropFileName)

	webserverConfigFileName = join(configDirectory, webserverConfigFile)
	webconfig = getPropertiesConfigMap(webserverConfigFileName)

	for k in list(config2xmlMAP):
		xmlKey = config2xmlMAP[k]
		if (k in list(xaSysProps)):
			xmlVal = xaSysProps[k]
		elif (k in list(xaLdapProps)):
			xmlVal = xaLdapProps[k]
		elif (k in list(unixauthProps)):
			xmlVal = unixauthProps[k]
		elif (k in list(webconfig)):
			xmlVal = webconfig[k]
		else:
			xmlVal = 'Unknown'
		rangerprops[xmlKey] = xmlVal

	jdbcUrl = xaSysProps['jdbc.url']
	auditJcksFileName = xaSysProps['auditDB.jdbc.credential.provider.path']
	jcksFileName = xaSysProps['xaDB.jdbc.credential.provider.path']

	auditJdbcUrl = xaSysProps['auditDB.jdbc.url']
	auditHostTokens = auditJdbcUrl.split("//")
	auditdbTokens = auditHostTokens[1].split("/")

	tokens = jdbcUrl.split(":")
	hostTokens = jdbcUrl.split("//")
	dbTokens = hostTokens[1].split("/")

	libFolderCmd='dirname `readlink -f /usr/bin/ranger-admin`'
	libFolder = os.popen(libFolderCmd).read().strip() + '/webapp/WEB-INF/lib'

	if (tokens[2] == 'mysql'):
		installProps['DB_FLAVOR'] = 'MYSQL'
		installProps['SQL_COMMAND_INVOKER'] = 'mysql'
		installProps['db_host'] = dbTokens[0]
		installProps['db_name'] = dbTokens[1]
		installProps['audit_db_name'] = auditdbTokens[1]
		mysqlConnectorJarFileName = [ f for f in listdir(libFolder) if (isfile(join(libFolder,f)) and f.startswith("mysql") and f.endswith(".jar")) ]
		if (len(mysqlConnectorJarFileName) >  0):
			if not os.path.exists(tempLibFolder):
			    os.makedirs(tempLibFolder)
			tempLibFile=join(tempLibFolder,mysqlConnectorJarFileName[0])
			shutil.copy(join(libFolder,mysqlConnectorJarFileName[0]), tempLibFile)
			installProps['SQL_CONNECTOR_JAR'] = tempLibFile
	elif (tokens[3] == 'odbc'):
		installProps['DB_FLAVOR'] = 'ORACLE'
		installProps['SQL_COMMAND_INVOKER'] = 'sqlplus'
		installProps['db_host'] = dbTokens[0]
		oraConnectorJarFileName = [ f for f in listdir(libFolder) if (isfile(join(libFolder,f)) and f.startswith("ojdbc") and f.endswith(".jar")) ]
		if (len(oraConnectorJarFileName) >  0):
			if not os.path.exists(tempLibFolder):
			    os.makedirs(tempLibFolder)
			tempLibFile=join(tempLibFolder,oraConnectorJarFileName[0])
			shutil.copy(join(libFolder,oraConnectorJarFileName[0]), tempLibFile)
			installProps['SQL_CONNECTOR_JAR'] = tempLibFile
		#
		# TODO: for oracle, need to find out as how to get these values
		#
		installProps['db_name'] = ''
		installProps['audit_db_name'] = ''
	else:
		print("ERROR: Unable to determine the DB_FLAVOR from url [%]" % (jdbcUrl))
		sys.exit(1)

	installProps['db_user'] = xaSysProps['jdbc.user']
	installProps['db_password'] = readFromJCKSFile(jcksFileName, 'policyDB.jdbc.password')
	installProps['db_root_user'] = 'unknown'
	installProps['db_root_password'] = 'unknown'

	installProps['audit_db_user']=xaSysProps['auditDB.jdbc.user'] 
	installProps['audit_db_password']= readFromJCKSFile(auditJcksFileName, 'auditDB.jdbc.password')

	installProps['policymgr_external_url'] = xaSysProps['xa.webapp.url.root']
	installProps['policymgr_http_enabled'] = xaSysProps['http.enabled']

	securityContextFileName = join(configDirectory, securityContextFile)
	tree = ET.parse(securityContextFileName)
	root = tree.getroot()
	ns = {'beans' : 'http://www.springframework.org/schema/beans'}
	if ( len(root.findall(".//*[@id='activeDirectoryAuthenticationProvider']",ns)) > 0):
		installProps['authentication_method'] = 'AD'
		installProps['xa_ldap_ad_domain'] = xaLdapProps['xa_ldap_ad_domain']
		installProps['xa_ldap_ad_url'] = xaLdapProps['xa_ldap_ad_url']
	elif ( len(root.findall(".//*[@id='ldapAuthProvider']",ns)) > 0 ):
		installProps['authentication_method'] = 'LDAP'
		installProps['xa_ldap_url'] = xaLdapProps['xa_ldap_url']
		installProps['xa_ldap_userDNpattern'] = xaLdapProps['xa_ldap_userDNpattern']
		installProps['xa_ldap_groupSearchBase'] = xaLdapProps['xa_ldap_groupSearchBase']
		installProps['xa_ldap_groupSearchFilter'] = xaLdapProps['xa_ldap_groupSearchFilter']
		installProps['xa_ldap_groupRoleAttribute'] = xaLdapProps['xa_ldap_groupRoleAttribute']
	elif ( len(root.findall(".//*[@id='jaasAuthProvider']",ns)) > 0 ):
		installProps['authentication_method'] = 'UNIX'
		installProps['remoteLoginEnabled'] = unixauthProps['remoteLoginEnabled']
		installProps['authServiceHostName'] = unixauthProps['authServiceHostName']
		installProps['authServicePort'] = unixauthProps['authServicePort']
	else:
		installProps['authentication_method'] = 'NONE'

	rangerprops['ranger.authentication.method'] = installProps['authentication_method']

	installProps['cred_keystore_filename'] = jcksFileName

	keylist = getPropertiesKeyList(installFileName)
	defValMap = getPropertiesConfigMap(installFileName)


	for wk,wv in webconfig.items():
		nk = "ranger." + wk
		nk = nk.replace('.','_')
		installProps[nk] = wv
		keylist.append(nk)

	writeToFile(keylist,defValMap,installProps,installPropFileName)

	if (generateXML == 1):
		writeXMLUsingProperties(join(templateDirectoryName,rangerSiteTemplateXMLFile), rangerprops, rangerSiteXMLFile)

def writeToFile(keyList, defValMap, props, outFileName):

	if (isfile(outFileName)):
		archiveFile(outFileName)

	outf = open(outFileName, 'w')

	print("#", file=outf)
	print("# -----------------------------------------------------------------------------------", file=outf)
	print("# This file is generated as part of upgrade script and should be deleted after upgrade", file=outf)
	print("# Generated at %s " % (strftime("%d/%m/%Y %H:%M:%S", localtime())), file=outf)
	print("# -----------------------------------------------------------------------------------", file=outf)
	print("#", file=outf)

	for key in keyList:
		if (key in props):
			print("%s=%s" % (key,props[key]), file=outf)
		else:
			print("# Default value for [%s] is used\n%s=%s\n#---" % (key, key,defValMap[key]), file=outf)

	outf.flush()
	outf.close()


main()
