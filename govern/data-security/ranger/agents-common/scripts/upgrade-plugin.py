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

import xml.etree.ElementTree as ET
import os,errno,sys
from os import listdir
from os.path import isfile, join, dirname
try:
	from urllib.parse import urlparse
except ImportError:
	from urlparse import urlparse

debugLevel = 1

SUPPORTED_COMPONENTS = [ "hdfs", "hive", "hbase", "knox", "storm" ]

#
# xmlTemplateDirectory: directory where all of the xml templates are kept here
#

xmlTemplateDirectory = './install/conf.templates/enable'

def showUsage():
	print("This script must be run with a <componentName> as parameter")
	print("USAGE: upgrade-plugin.py <componentName>")
	print(" <componentName> could be any one of the following: %s" % (SUPPORTED_COMPONENTS))

if (len(sys.argv) == 1):
	showUsage()
	sys.exit(1)

componentName = sys.argv[1]

if (componentName not in SUPPORTED_COMPONENTS):
	print("Invalid componentName passed as parameter: %s" % (componentName))
	showUsage()
	sys.exit(1)

#
# For hdfs, the componentName is hadoop (for path calculation)
#

if (componentName == 'hdfs'):
	configPath = 'hadoop'
else:
	configPath = componentName

#
# configDirectory: where OLD (champlain) configuration exists and NEW (dal) configuration is written to
#
configDirectory = '/etc/' + configPath + '/conf' 



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
	for config in root.iter('property'):
		name = config.find('name').text
		val = config.find('value').text 
		ret[name] = val
	return ret

def writeXMLUsingProperties(xmlTemplateFileName,prop,xmlOutputFileName):
	tree = ET.parse(xmlTemplateFileName)
	root = tree.getroot()
	for config in root.iter('property'):
		name = config.find('name').text
		if (name in prop):
			config.find('value').text = prop[name]
	tree.write(xmlOutputFileName)

def rewriteConfig(props,newProps):
	if (debugLevel > 0):
		for k,v in props.items():
			print("old config[%s] = [%s]" % (k,v))
	#
	# Derived fields
	#
	pmUrl = props['xasecure.' + componentName + '.policymgr.url']
	url = urlparse(pmUrl)
	restUrl = url[0] + "://" + url[1]
	serviceName = url[2].split("/")[-1]
	props['ranger.plugin.' + componentName + '.policy.rest.url'] = restUrl
	props['ranger.plugin.' + componentName + '.service.name'] = serviceName
	props['ranger.plugin.' + componentName + '.policy.pollIntervalMs'] = props['xasecure.' + componentName + '.policymgr.url.reloadIntervalInMillis']
	#props['ranger.plugin.' + componentName + '.policy.rest.ssl.config.file'] = props['y']
	fileLoc = props['xasecure.' + componentName + '.policymgr.url.laststoredfile']
	props['ranger.plugin.' + componentName + '.policy.cache.dir'] = dirname(fileLoc)
	if ( 'xasecure.policymgr.sslconfig.filename' in props ):
		props['ranger.plugin.' + componentName + '.policy.rest.ssl.config.file'] = props['xasecure.policymgr.sslconfig.filename']
	else:
		sslConfigFileName = join(configDirectory,'ranger-policymgr-ssl.xml') 
		props['ranger.plugin.' + componentName + '.policy.rest.ssl.config.file'] = sslConfigFileName
	#
	# Fix for KNOX ssl (missing) configuration
	#
	if ('xasecure.policymgr.clientssl.keystore.credential.file' not in props):
		props['xasecure.policymgr.clientssl.keystore.credential.file'] = 'jceks://file/tmp/keystore-' + serviceName + '-ssl.jceks'
	if ( 'xasecure.policymgr.clientssl.truststore.credential.file' not in props):
		props['xasecure.policymgr.clientssl.truststore.credential.file'] = 'jceks://file/tmp/keystore-' + serviceName + '-ssl.jceks'

	for fn in listdir(xmlTemplateDirectory):
		file = join(xmlTemplateDirectory,fn)
		if isfile(file) and fn.startswith("ranger-") and fn.endswith(".xml") :
			newConfigFile = join(configDirectory, fn)
			writeXMLUsingProperties(file, props, newConfigFile)

def main():
	props = {}
	newProps = {}
	foundFiles = []
	for fn in listdir(configDirectory):
		file = join(configDirectory,fn)
		if isfile(file) and fn.startswith("xasecure-") and fn.endswith(".xml") :
			foundFiles.append(file)
			r = getXMLConfigMap(file)
			props.update(r)
	if (len(foundFiles) == 0):
		print("INFO: Previous version of ranger is not enabled/configured for component [%s]" % (componentName))
		sys.exit(0)
	if (len(foundFiles) != 3):
		print("ERROR: Expected to find three files matching xasecure-*.xml files under the folder (%s) - found %s" % (configDirectory,foundFiles))
		sys.exit(1)
	for fn in listdir(xmlTemplateDirectory):
		file = join(xmlTemplateDirectory,fn)
		if isfile(file) and fn.startswith("ranger-") and fn.endswith(".xml") :
			r = getXMLConfigMap(file)
			newProps.update(r)
			newConfigFile = join(configDirectory,fn)
			if isfile(newConfigFile):
				print("ERROR: new config file [%s] already exists. Upgrade script can not overwrite an existing config file." % (newConfigFile))
				sys.exit(1)
	rewriteConfig(props,newProps)

main()
