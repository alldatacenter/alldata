#!/usr/bin/python
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
import re
import sys
import errno
import shlex
import logging
import subprocess
import platform
import fileinput
import getpass
import shutil
from xml.etree import ElementTree as ET
from os.path import basename
from subprocess import Popen,PIPE
from datetime import date
from datetime import datetime
try: input = raw_input
except NameError: pass
globalDict = {}

os_name = platform.system()
os_name = os_name.upper()

def check_output(query):
	if os_name == "LINUX":
		p = subprocess.Popen(shlex.split(query), stdout=subprocess.PIPE)
	elif os_name == "WINDOWS":	
		p = subprocess.Popen(query, stdout=subprocess.PIPE, shell=True)
	output = p.communicate ()[0]
	return output

def log(msg,type):
	if type == 'info':
		logging.info(" %s",msg)
	if type == 'debug':
		logging.debug(" %s",msg)
	if type == 'warning':
		logging.warning(" %s",msg)
	if type == 'exception':
		logging.exception(" %s",msg)
	if type == 'error':
		logging.error(" %s",msg)

def import_properties_from_xml(xml_path, properties_from_xml=None):
	log('[I] Getting values from file : ' + str(xml_path), "info")
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
		log('[E] XML file not found at path : ' + str(xml_path), "error")
	return properties_from_xml

def write_properties_to_xml(xml_path, property_name='', property_value=''):
	if(os.path.isfile(xml_path)):
		xml = ET.parse(xml_path)
		root = xml.getroot()
		for child in root.findall('property'):
			name = child.find("name").text.strip()
			if name == property_name:
				child.find("value").text = property_value
		xml.write(xml_path)
		return 0
	else:
		return -1

def main(argv):
	global globalDict
	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)

	CFG_FILE=os.path.join(os.getcwd(),'conf','ranger-tagsync-site.xml')
	if os.path.isfile(CFG_FILE):
		pass
	else:
		log("[E] Required file not found: ["+CFG_FILE+"]","error")
		sys.exit(1)

	if os.environ['JAVA_HOME'] == "":
		log("[E] ---------- JAVA_HOME environment property not defined, aborting installation. ----------", "error")
		sys.exit(1)
	JAVA_BIN=os.path.join(os.environ['JAVA_HOME'],'bin','java')
	if os_name == "WINDOWS" :
		JAVA_BIN = JAVA_BIN+'.exe'
	if os.path.isfile(JAVA_BIN):
		pass
	else:
		while os.path.isfile(JAVA_BIN) == False:
			log("Enter java executable path: :","info")
			JAVA_BIN=input()
	log("[I] Using Java:" + str(JAVA_BIN),"info")

	globalDict=import_properties_from_xml(CFG_FILE,globalDict)

	ENDPOINT=''
	KEYSTORE_FILENAME=''
	KEYSTORE_FILENAME_PROMPT=''
	ALIAS = ''
	USERNAME=''
	PASSWORD=''
	USERNAME_PROPERTY_NAME=''
	FILENAME_PROPERTY_NAME=''
	if len(argv) == 4:
		ENDPOINT=argv[1]
		USERNAME=argv[2]
		PASSWORD=argv[3]

	while ENDPOINT == "" or not (ENDPOINT == "ATLAS" or ENDPOINT == "RANGER"):
		sys.stdout.write('Enter Destination NAME (Ranger/Atlas):')
		sys.stdout.flush()
		ENDPOINT=input()
		ENDPOINT = ENDPOINT.upper()

	if ENDPOINT == "ATLAS":
		USERNAME_PROPERTY_NAME='ranger.tagsync.source.atlasrest.username'
		FILENAME_PROPERTY_NAME='ranger.tagsync.source.atlasrest.keystore.filename'
		ALIAS="atlas.user.password"
		KEYSTORE_FILENAME_PROMPT='RANGER_TAGSYNC_ATLAS_KEYSTORE_FILENAME'

	elif ENDPOINT == "RANGER":
		USERNAME_PROPERTY_NAME='ranger.tagsync.dest.ranger.username'
		FILENAME_PROPERTY_NAME='ranger.tagsync.keystore.filename'
		ALIAS="tagadmin.user.password"
		KEYSTORE_FILENAME_PROMPT='RANGER_TAGSYNC_RANGER_KEYSTORE_FILENAME'

	else:
		log("[E] Unsupported ENDPOINT[" + ENDPOINT + "]")
		return

	KEYSTORE_FILENAME = globalDict[FILENAME_PROPERTY_NAME]

	if KEYSTORE_FILENAME == "":
		log("[E] " + FILENAME_PROPERTY_NAME + " is not specified.","error")
		return

	log("[D] " + KEYSTORE_FILENAME_PROMPT + ":" + str(KEYSTORE_FILENAME),"debug")

	unix_user = "ranger"
	unix_group = "ranger"

	USERNAME = globalDict[USERNAME_PROPERTY_NAME]

	if USERNAME == "":
		if ENDPOINT == "RANGER":
			USERNAME = 'rangertagsync'
		else:
			USERNAME = 'admin'
	while PASSWORD == "":
		PASSWORD=getpass.getpass("Enter " + " password for " + ENDPOINT + " user " + USERNAME + ":")

	if KEYSTORE_FILENAME != "" or USERNAME != "" or PASSWORD != "":
		log("[I] Storing password for " + ENDPOINT + " user " + USERNAME + " in credential store:","info")
		cmd="%s -cp lib/* org.apache.ranger.credentialapi.buildks create %s -value %s  -provider jceks://file%s" %(JAVA_BIN,ALIAS,PASSWORD,KEYSTORE_FILENAME)
		ret=subprocess.call(shlex.split(cmd))
		if ret == 0:
			cmd="chown %s:%s %s" %(unix_user,unix_group,KEYSTORE_FILENAME)
			ret=subprocess.call(shlex.split(cmd))
			if ret == 0:
				if os.path.isfile(CFG_FILE):
					write_properties_to_xml(CFG_FILE,USERNAME_PROPERTY_NAME,USERNAME)
					write_properties_to_xml(CFG_FILE,FILENAME_PROPERTY_NAME,KEYSTORE_FILENAME)
				else:
					log("[E] Required file not found: ["+CFG_FILE+"]","error")
			else:
				log("[E] unable to execute command ["+cmd+"]","error")
		else:
			log("[E] unable to execute command ["+cmd+"]","error")
	else:
		log("[E] Input Error","error")

main(sys.argv)
