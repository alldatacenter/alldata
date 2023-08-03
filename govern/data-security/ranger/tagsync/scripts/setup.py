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

try:
	from StringIO import StringIO
except ImportError:
	from io import StringIO
try:
	from ConfigParser import ConfigParser
except ImportError:
	from configparser import ConfigParser
try:
	from urlparse import urlparse
except ImportError:
	from urllib.parse import urlparse
import re
import xml.etree.ElementTree as ET
import os,errno,sys,getopt
from os import listdir
from os.path import isfile, join, dirname, basename
from time import gmtime, strftime, localtime
from xml import etree
import shutil
import pwd, grp

if (not 'JAVA_HOME' in os.environ):
	print("ERROR: JAVA_HOME environment variable is not defined. Please define JAVA_HOME before running this script")
	sys.exit(1)

debugLevel = 1
generateXML = 0
installPropDirName = '.'
logFolderName = '/var/log/ranger'
initdDirName = '/etc/init.d'

rangerBaseDirName = '/etc/ranger'
tagsyncBaseDirName = 'tagsync'
confBaseDirName = 'conf'
confDistBaseDirName = 'conf.dist'

outputFileName = 'ranger-tagsync-site.xml'
installPropFileName = 'install.properties'
logbackFileName          = 'logback.xml'
install2xmlMapFileName = 'installprop2xml.properties'
templateFileName = 'ranger-tagsync-template.xml'
initdProgramName = 'ranger-tagsync'
atlasApplicationPropFileName = 'atlas-application.properties'

installTemplateDirName = join(installPropDirName,'templates')
confDistDirName = join(installPropDirName, confDistBaseDirName)
tagsyncLogFolderName = join(logFolderName, 'tagsync')
tagsyncBaseDirFullName = join(rangerBaseDirName, tagsyncBaseDirName)
confFolderName = join(tagsyncBaseDirFullName, confBaseDirName)
localConfFolderName = join(installPropDirName, confBaseDirName)

credUpdateClassName =  'org.apache.ranger.credentialapi.buildks'
defaultKeyStoreFileName = '/etc/ranger/tagsync/conf/rangertagsync.jceks'

unixUserProp = 'unix_user'
unixGroupProp = 'unix_group'

logFolderPermMode = 0o777
rootOwnerId = 0
initPrefixList = ['S99', 'K00']

TAGSYNC_ATLAS_KAFKA_ENDPOINTS_KEY = 'TAG_SOURCE_ATLAS_KAFKA_BOOTSTRAP_SERVERS'
TAGSYNC_ATLAS_ZOOKEEPER_ENDPOINT_KEY = 'TAG_SOURCE_ATLAS_KAFKA_ZOOKEEPER_CONNECT'
TAGSYNC_ATLAS_CONSUMER_GROUP_KEY = 'TAG_SOURCE_ATLAS_KAFKA_ENTITIES_GROUP_ID'

TAG_SOURCE_ATLAS_KAKFA_SERVICE_NAME_KEY = 'TAG_SOURCE_ATLAS_KAFKA_SERVICE_NAME'
TAG_SOURCE_ATLAS_KAFKA_SECURITY_PROTOCOL_KEY = 'TAG_SOURCE_ATLAS_KAFKA_SECURITY_PROTOCOL'
TAG_SOURCE_ATLAS_KERBEROS_PRINCIPAL_KEY = 'TAG_SOURCE_ATLAS_KERBEROS_PRINCIPAL'
TAG_SOURCE_ATLAS_KERBEROS_KEYTAB_KEY = 'TAG_SOURCE_ATLAS_KERBEROS_KEYTAB'
TAGSYNC_ATLAS_TO_RANGER_SERVICE_MAPPING = 'ranger.tagsync.atlas.to.ranger.service.mapping'
TAGSYNC_INSTALL_PROP_PREFIX_FOR_ATLAS_RANGER_MAPPING = 'ranger.tagsync.atlas.'
TAGSYNC_ATLAS_CLUSTER_IDENTIFIER = '.instance.'
TAGSYNC_INSTALL_PROP_SUFFIX_FOR_ATLAS_RANGER_MAPPING = '.ranger.service'

TAG_SOURCE_ATLAS_ENABLED_KEY = 'TAG_SOURCE_ATLAS_ENABLED'
TAG_SOURCE_ATLAS_ENABLED = 'ranger.tagsync.source.atlas'

TAG_SOURCE_ATLASREST_ENABLED_KEY = 'TAG_SOURCE_ATLASREST_ENABLED'
TAG_SOURCE_ATLASREST_ENABLED = 'ranger.tagsync.source.atlasrest'

TAG_SOURCE_FILE_ENABLED_KEY = 'TAG_SOURCE_FILE_ENABLED'
TAG_SOURCE_FILE_ENABLED = 'ranger.tagsync.source.file'

hadoopConfFileName = 'core-site.xml'
ENV_HADOOP_CONF_FILE = "ranger-tagsync-env-hadoopconfdir.sh"
ENV_PID_FILE = 'ranger-tagsync-env-piddir.sh'

globalDict = {}
configure_security = False

RANGER_TAGSYNC_HOME = os.getenv("RANGER_TAGSYNC_HOME")
if RANGER_TAGSYNC_HOME is None:
	RANGER_TAGSYNC_HOME = os.getcwd()

def populate_global_dict():
    global globalDict
    read_config_file = open(os.path.join(RANGER_TAGSYNC_HOME,'install.properties'))
    for each_line in read_config_file.read().split('\n') :
        each_line = each_line.strip()
        if len(each_line) == 0:
            continue
        elif each_line[0] == "#":
            continue

        if re.search('=', each_line):
            key , value = each_line.split("=",1)
            key = key.strip()
            value = value.strip()
            globalDict[key] = value

def archiveFile(originalFileName):
    archiveDir = dirname(originalFileName)
    archiveFileName = "." + basename(originalFileName) + "." + (strftime("%d%m%Y%H%M%S", localtime()))
    movedFileName = join(archiveDir,archiveFileName)
    print("INFO: moving [%s] to [%s] ......." % (originalFileName,movedFileName))
    os.rename(originalFileName, movedFileName)

def getXMLConfigKeys(xmlFileName):
    ret = []
    tree = ET.parse(xmlFileName)
    root = tree.getroot()
    for config in root.iter('property'):
        name = config.find('name').text
        ret.append(name)
    return ret

def getXMLConfigMap(xmlFileName):
    ret = {}
    tree = ET.parse(xmlFileName)
    root = tree.getroot()
    for config in root.findall('property'):
        name = config.find('name').text
        val = config.find('value').text
        ret[name] = val
    return ret


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

def writeXMLUsingProperties(xmlTemplateFileName,prop,xmlOutputFileName):
	tree = ET.parse(xmlTemplateFileName)
	root = tree.getroot()
	for config in root.findall('property'):
		name = config.find('name').text
		if (name in list(prop)):
			if (name == TAGSYNC_ATLAS_TO_RANGER_SERVICE_MAPPING):
				# Expected value is 'clusterName,componentName,serviceName;clusterName,componentName,serviceName' ...
				# Blanks are not supported anywhere in the value.
				valueString = str(prop[name])
				if valueString and valueString.strip():
					multiValues = valueString.split(';')
					listLen = len(multiValues)
					index = 0
					while index < listLen:
						parts = multiValues[index].split(',')
						if len(parts) == 3:
							newConfig = ET.SubElement(root, 'property')
							newName = ET.SubElement(newConfig, 'name')
							newValue = ET.SubElement(newConfig, 'value')
							newName.text = TAGSYNC_INSTALL_PROP_PREFIX_FOR_ATLAS_RANGER_MAPPING + str(parts[1]) + TAGSYNC_ATLAS_CLUSTER_IDENTIFIER + str(parts[0]) + TAGSYNC_INSTALL_PROP_SUFFIX_FOR_ATLAS_RANGER_MAPPING
							newValue.text = str(parts[2])
						else:
							print("ERROR: incorrect syntax for %s, value=%s" % (TAGSYNC_ATLAS_TO_RANGER_SERVICE_MAPPING, multiValues[index]))
						index += 1
				root.remove(config)
			else:
				config.find('value').text = str(prop[name])
		#else:
		#    print "ERROR: key not found: %s" % (name)
	if isfile(xmlOutputFileName):
		archiveFile(xmlOutputFileName)
	tree.write(xmlOutputFileName)

def updatePropertyInJCKSFile(jcksFileName,propName,value):
	fn = jcksFileName
	if (value == ''):
		value = ' '
	cmd = "java -cp './lib/*' %s create '%s' -value '%s' -provider jceks://file%s 2>&1" % (credUpdateClassName,propName,value,fn)
	ret = os.system(cmd)
	if (ret != 0):
		print("ERROR: Unable to update the JCKSFile (%s) for aliasName (%s)" % (fn,propName))
		sys.exit(1)
	return ret

def convertInstallPropsToXML(props):
	directKeyMap = getPropertiesConfigMap(join(installTemplateDirName,install2xmlMapFileName))
	ret = {}
	atlasOutFn = join(confFolderName, atlasApplicationPropFileName)

	atlasOutFile = open(atlasOutFn, "w")

	atlas_principal = ''
	atlas_keytab = ''

	for k,v in props.items():
		if (k in list(directKeyMap)):
			newKey = directKeyMap[k]
			if (k == TAGSYNC_ATLAS_KAFKA_ENDPOINTS_KEY):
				atlasOutFile.write(newKey + "=" + v + "\n")
			elif (k == TAGSYNC_ATLAS_ZOOKEEPER_ENDPOINT_KEY):
				atlasOutFile.write(newKey + "=" + v + "\n")
			elif (k == TAGSYNC_ATLAS_CONSUMER_GROUP_KEY):
				atlasOutFile.write(newKey + "=" + v + "\n")
			elif (configure_security and k == TAG_SOURCE_ATLAS_KAKFA_SERVICE_NAME_KEY):
				atlasOutFile.write(newKey + "=" + v + "\n")
			elif (configure_security and k == TAG_SOURCE_ATLAS_KAFKA_SECURITY_PROTOCOL_KEY):
				atlasOutFile.write(newKey + "=" + v + "\n")
			elif (configure_security and k == TAG_SOURCE_ATLAS_KERBEROS_PRINCIPAL_KEY):
				atlas_principal = v
			elif (configure_security and k == TAG_SOURCE_ATLAS_KERBEROS_KEYTAB_KEY):
				atlas_keytab = v
			else:
				ret[newKey] = v
		else:
			print("INFO: Direct Key not found:%s" % (k))

	if (configure_security):
		atlasOutFile.write("atlas.jaas.KafkaClient.loginModuleName = com.sun.security.auth.module.Krb5LoginModule" + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.loginModuleControlFlag = required" + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.option.useKeyTab = true" + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.option.storeKey = true" + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.option.serviceName = kafka" + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.option.keyTab = " + atlas_keytab + "\n")
		atlasOutFile.write("atlas.jaas.KafkaClient.option.principal = " + atlas_principal + "\n")

	atlasOutFile.close()

	if (TAG_SOURCE_ATLAS_ENABLED_KEY in ret):
		ret[TAG_SOURCE_ATLAS_ENABLED] = ret[TAG_SOURCE_ATLAS_ENABLED_KEY]
		del ret[TAG_SOURCE_ATLAS_ENABLED_KEY]

	if (TAG_SOURCE_ATLASREST_ENABLED_KEY in ret):
		ret[TAG_SOURCE_ATLASREST_ENABLED] = ret[TAG_SOURCE_ATLASREST_ENABLED_KEY]
		del ret[TAG_SOURCE_ATLASREST_ENABLED_KEY]

	if (TAG_SOURCE_FILE_ENABLED_KEY in ret):
		ret[TAG_SOURCE_FILE_ENABLED] = ret[TAG_SOURCE_FILE_ENABLED_KEY]
		del ret[TAG_SOURCE_FILE_ENABLED_KEY]

	return ret

def createUser(username,groupname):
	cmd = "useradd -g %s %s -m" % (groupname,username)
	ret = os.system(cmd)
	if (ret != 0):
		print("ERROR: os command execution (%s) failed. error code = %d " % (cmd, ret))
		sys.exit(1)
	try:
		ret = pwd.getpwnam(username).pw_uid
		return ret
	except KeyError as e:
		print("ERROR: Unable to create a new user account: %s with group %s - error [%s]" % (username,groupname,e))
		sys.exit(1)

def createGroup(groupname):
	cmd = "groupadd %s" % (groupname)
	ret = os.system(cmd)
	if (ret != 0):
		print("ERROR: os command execution (%s) failed. error code = %d " % (cmd, ret))
		sys.exit(1)
	try:
		ret = grp.getgrnam(groupname).gr_gid
		return ret
	except KeyError as e:
		print("ERROR: Unable to create a new group: %s" % (groupname,e))
		sys.exit(1)

def initializeInitD():
	if (os.path.isdir(initdDirName)):
		fn = join(installPropDirName,initdProgramName)
		initdFn = join(initdDirName,initdProgramName)
		shutil.copy(fn, initdFn)
		os.chmod(initdFn,0o550)
		rcDirList = [ "/etc/rc2.d", "/etc/rc3.d", "/etc/rc.d/rc2.d", "/etc/rc.d/rc3.d" ]
		for rcDir in rcDirList:
			if (os.path.isdir(rcDir)):
				for  prefix in initPrefixList:
					scriptFn = prefix + initdProgramName
					scriptName = join(rcDir, scriptFn)
					if not (isfile(scriptName) or os.path.islink(scriptName)):
						os.symlink(initdFn,scriptName)
					#print "+ ln -sf %s %s" % (initdFn, scriptName)
		tagSyncScriptName = "ranger-tagsync-services.sh"
		localScriptName = os.path.abspath(join(installPropDirName,tagSyncScriptName))
		ubinScriptName = join("/usr/bin",tagSyncScriptName)
		if not (isfile(ubinScriptName) or os.path.islink(ubinScriptName)):
			os.symlink(localScriptName,ubinScriptName)

def write_env_files(exp_var_name, log_path, file_name):
        final_path = "{0}/{1}".format(confBaseDirName,file_name)
        if not os.path.isfile(final_path):
            print("INFO: Creating %s file" % file_name)
        f = open(final_path, "w")
        f.write("export {0}={1}".format(exp_var_name,log_path))
        f.close()

def main():

	global configure_security

	print("\nINFO: Installing ranger-tagsync .....\n")

	populate_global_dict()


	kerberize = globalDict['is_secure']
	if kerberize != "":
		kerberize = kerberize.lower()
		if kerberize == "true" or kerberize == "enabled" or kerberize == "yes":
			configure_security = True


	hadoop_conf = globalDict['hadoop_conf']
	pid_dir_path = globalDict['TAGSYNC_PID_DIR_PATH']
	unix_user = globalDict['unix_user']

	if pid_dir_path == "":
		pid_dir_path = "/var/run/ranger"

	dirList = [ rangerBaseDirName, tagsyncBaseDirFullName, confFolderName ]
	for dir in dirList:
		if (not os.path.isdir(dir)):
			os.makedirs(dir,0o755)

	defFileList = [ logbackFileName ]
	for defFile in defFileList:
		fn = join(confDistDirName, defFile)
		if ( isfile(fn) ):
			shutil.copy(fn,join(confFolderName,defFile))

	#
	# Create JAVA_HOME setting in confFolderName
	#
	java_home_setter_fn = join(confFolderName, 'java_home.sh')
	if isfile(java_home_setter_fn):
		archiveFile(java_home_setter_fn)
	jhf = open(java_home_setter_fn, 'w')
	str = "export JAVA_HOME=%s\n" % os.environ['JAVA_HOME']
	jhf.write(str)
	jhf.close()
	os.chmod(java_home_setter_fn,0o750)


	if (not os.path.isdir(localConfFolderName)):
		os.symlink(confFolderName, localConfFolderName)

	installProps = getPropertiesConfigMap(join(installPropDirName,installPropFileName))
	modifiedInstallProps = convertInstallPropsToXML(installProps)

	mergeProps = {}
	mergeProps.update(modifiedInstallProps)

	localLogFolderName = mergeProps['ranger.tagsync.logdir']
	if (not os.path.isdir(localLogFolderName)):
		if (localLogFolderName != tagsyncLogFolderName):
			os.symlink(tagsyncLogFolderName, localLogFolderName)

	fn = join(installTemplateDirName,templateFileName)
	outfn = join(confFolderName, outputFileName)

	if ( os.path.isdir(logFolderName) ):
		logStat = os.stat(logFolderName)
		logStat.st_uid
		logStat.st_gid
		ownerName = pwd.getpwuid(logStat.st_uid).pw_name
		groupName = pwd.getpwuid(logStat.st_uid).pw_name
	else:
		os.makedirs(logFolderName,logFolderPermMode)

	if (not os.path.isdir(tagsyncLogFolderName)):
		os.makedirs(tagsyncLogFolderName,logFolderPermMode)

	if (not os.path.isdir(pid_dir_path)):
		os.makedirs(pid_dir_path,logFolderPermMode)

	if (unixUserProp in mergeProps):
		ownerName = mergeProps[unixUserProp]
	else:
		mergeProps[unixUserProp] = "ranger"
		ownerName = mergeProps[unixUserProp]

	if (unixGroupProp in mergeProps):
		groupName = mergeProps[unixGroupProp]
	else:
		mergeProps[unixGroupProp] = "ranger"
		groupName = mergeProps[unixGroupProp]

	try:
		groupId = grp.getgrnam(groupName).gr_gid
	except KeyError as e:
		groupId = createGroup(groupName)

	try:
		ownerId = pwd.getpwnam(ownerName).pw_uid
	except KeyError as e:
		ownerId = createUser(ownerName, groupName)

	os.chown(logFolderName,ownerId,groupId)
	os.chown(tagsyncLogFolderName,ownerId,groupId)
	os.chown(rangerBaseDirName,ownerId,groupId)

	initializeInitD()

	tagsyncKSPath = mergeProps['ranger.tagsync.keystore.filename']

	if ('ranger.tagsync.dest.ranger.username' not in mergeProps):
		mergeProps['ranger.tagsync.dest.ranger.username'] = 'rangertagsync'

	if (tagsyncKSPath != ''):
		tagadminPasswd = 'rangertagsync'
		tagadminAlias = 'tagadmin.user.password'
		updatePropertyInJCKSFile(tagsyncKSPath,tagadminAlias,tagadminPasswd)
		os.chown(tagsyncKSPath,ownerId,groupId)

	tagsyncAtlasKSPath = mergeProps['ranger.tagsync.source.atlasrest.keystore.filename']

	if ('ranger.tagsync.source.atlasrest.username' not in mergeProps):
		mergeProps['ranger.tagsync.source.atlasrest.username'] = 'admin'

	if (tagsyncAtlasKSPath != ''):
		if ('ranger.tagsync.source.atlasrest.password' not in mergeProps):
			atlasPasswd = 'admin'
		else:
			atlasPasswd = mergeProps['ranger.tagsync.source.atlasrest.password']
			mergeProps.pop('ranger.tagsync.source.atlasrest.password')

		atlasAlias = 'atlas.user.password'
		updatePropertyInJCKSFile(tagsyncAtlasKSPath,atlasAlias,atlasPasswd)
		os.chown(tagsyncAtlasKSPath,ownerId,groupId)

	writeXMLUsingProperties(fn, mergeProps, outfn)

	fixPermList = [ ".", tagsyncBaseDirName, confFolderName ]

	for dir in fixPermList:
		for root, dirs, files in os.walk(dir):
			os.chown(root, ownerId, groupId)
			os.chmod(root,0o755)
			for obj in dirs:
				dn = join(root,obj)
				os.chown(dn, ownerId, groupId)
				os.chmod(dn, 0o755)
			for obj in files:
				fn = join(root,obj)
				os.chown(fn, ownerId, groupId)
				os.chmod(fn, 0o755)

	write_env_files("RANGER_TAGSYNC_HADOOP_CONF_DIR", hadoop_conf, ENV_HADOOP_CONF_FILE)
	write_env_files("TAGSYNC_PID_DIR_PATH", pid_dir_path, ENV_PID_FILE);
	os.chown(os.path.join(confBaseDirName, ENV_HADOOP_CONF_FILE),ownerId,groupId)
	os.chmod(os.path.join(confBaseDirName, ENV_HADOOP_CONF_FILE),0o755)
	os.chown(os.path.join(confBaseDirName, ENV_PID_FILE),ownerId,groupId)
	os.chmod(os.path.join(confBaseDirName, ENV_PID_FILE),0o755)

	f = open(os.path.join(confBaseDirName, ENV_PID_FILE), "a+")
	f.write("\nexport {0}={1}".format("UNIX_TAGSYNC_USER",unix_user))
	f.close()

	hadoop_conf_full_path = os.path.join(hadoop_conf, hadoopConfFileName)
	tagsync_conf_full_path = os.path.join(tagsyncBaseDirFullName,confBaseDirName,hadoopConfFileName)

	if not isfile(hadoop_conf_full_path):
		print("WARN: core-site.xml file not found in provided hadoop conf path...")
		f = open(tagsync_conf_full_path, "w")
		f.write("<configuration></configuration>")
		f.close()
		os.chown(tagsync_conf_full_path,ownerId,groupId)
		os.chmod(tagsync_conf_full_path,0o750)
	else:
		if os.path.islink(tagsync_conf_full_path):
			os.remove(tagsync_conf_full_path)

	if isfile(hadoop_conf_full_path) and not isfile(tagsync_conf_full_path):
		os.symlink(hadoop_conf_full_path, tagsync_conf_full_path)

	rangerTagsync_password = globalDict['rangerTagsync_password']
	rangerTagsync_name ='rangerTagsync'
	endPoint='RANGER'
	cmd = 'python updatetagadminpassword.py %s %s %s'  %(endPoint, rangerTagsync_name, rangerTagsync_password)
	if rangerTagsync_password != "" :
		output = os.system(cmd)
		if (output == 0):
			print("[I] Successfully updated password of " + rangerTagsync_name +" user")
		else:
			print("[ERROR] Unable to change password of " + rangerTagsync_name +" user.")
	print("\nINFO: Completed ranger-tagsync installation.....\n")

main()
