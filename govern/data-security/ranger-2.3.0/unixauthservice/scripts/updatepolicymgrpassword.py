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
import update_property
from os.path import basename
from subprocess import Popen,PIPE
from datetime import date
from datetime import datetime
try: input = raw_input
except NameError: pass
globalDict = {}
installglobalDict={}

os_name = platform.system()
os_name = os_name.upper()

RANGER_USERSYNC_HOME = os.getenv("RANGER_USERSYNC_HOME")
if RANGER_USERSYNC_HOME is None:
	RANGER_USERSYNC_HOME = os.getcwd()

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

def populate_global_install_dict():
    global installglobalDict
    read_config_file = open(os.path.join(RANGER_USERSYNC_HOME,'install.properties'))
    for each_line in read_config_file.read().split('\n') :
        each_line = each_line.strip()
        if len(each_line) == 0:
            continue
        elif each_line[0] == "#":
            continue
        if re.search('=', each_line):
            key , value = each_line.split("=",1)
            key = key.strip()
            if 'PASSWORD' in key:
                jceks_file_path = os.path.join(RANGER_USERSYNC_HOME, 'jceks','ranger_db.jceks')
                value = ''
            value = value.strip()
            installglobalDict[key] = value

def main(argv):
	global globalDict
	populate_global_install_dict()
	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)

	CFG_FILE=os.path.join(os.getcwd(),'conf','ranger-ugsync-site.xml')
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
	SYNC_LDAP_BIND_KEYSTOREPATH=globalDict['ranger.usersync.credstore.filename']
	log("[I] SYNC_LDAP_BIND_KEYSTOREPATH:" + str(SYNC_LDAP_BIND_KEYSTOREPATH),"info")
	SYNC_POLICY_MGR_ALIAS="ranger.usersync.policymgr.password"
	SYNC_POLICY_MGR_PASSWORD = ''
	SYNC_POLICY_MGR_USERNAME = ''
	unix_user = installglobalDict['unix_user']
	unix_group = installglobalDict['unix_group']

	if len(argv) == 3:
		SYNC_POLICY_MGR_USERNAME=argv[1]
		SYNC_POLICY_MGR_PASSWORD=argv[2]

	while SYNC_POLICY_MGR_USERNAME == "":
		print("Enter policymgr user name:")
		SYNC_POLICY_MGR_USERNAME=input()

	while SYNC_POLICY_MGR_PASSWORD == "":
		SYNC_POLICY_MGR_PASSWORD=getpass.getpass("Enter policymgr user password:")

	if SYNC_LDAP_BIND_KEYSTOREPATH != "" or SYNC_POLICY_MGR_ALIAS != "" or SYNC_POLICY_MGR_USERNAME != "" or SYNC_POLICY_MGR_PASSWORD != "":
		log("[I] Storing policymgr usersync password in credential store:","info")
		cmd="%s -cp lib/* org.apache.ranger.credentialapi.buildks create %s -value %s  -provider jceks://file%s" %(JAVA_BIN,SYNC_POLICY_MGR_ALIAS,SYNC_POLICY_MGR_PASSWORD,SYNC_LDAP_BIND_KEYSTOREPATH)
		ret=subprocess.call(shlex.split(cmd))
		if ret == 0:
			cmd="chown %s:%s %s" %(unix_user,unix_group,SYNC_LDAP_BIND_KEYSTOREPATH)
			ret=subprocess.call(shlex.split(cmd))
			if ret == 0:
				if os.path.isfile(CFG_FILE):
					update_property.write_properties_to_xml(CFG_FILE,"ranger.usersync.policymgr.username",SYNC_POLICY_MGR_USERNAME)
					update_property.write_properties_to_xml(CFG_FILE,"ranger.usersync.policymgr.keystore",SYNC_LDAP_BIND_KEYSTOREPATH)
					update_property.write_properties_to_xml(CFG_FILE,"ranger.usersync.policymgr.alias",SYNC_POLICY_MGR_ALIAS)
				else:
					log("[E] Required file not found: ["+CFG_FILE+"]","error")
			else:
				log("[E] unable to execute command ["+cmd+"]","error")
		else:
			log("[E] unable to execute command ["+cmd+"]","error")
	else:
		log("[E] Input Error","error")

main(sys.argv)
