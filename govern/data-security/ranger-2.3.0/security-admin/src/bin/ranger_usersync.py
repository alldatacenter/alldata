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

import sys
import os
import logging
import subprocess
import time
from xml.dom.minidom import getDOMImplementation
import shutil
import re

cmd = sys.argv[0]
app_type = sys.argv[1]

service_entry = '--service' in sys.argv
configure_entry = '--configure' in sys.argv


conf_dict={}

def log(msg,type):
    if type == 'info':
        logging.info(" %s",msg)
    if type == 'debug':
        logging.debug(" %s",msg)
    if type == 'warning':
        logging.warning(" %s",msg)
    if type == 'exception':
        logging.exception(" %s",msg)

def copy_files(source_dir,dest_dir):
    for dir_path, dir_names, file_names in os.walk(source_dir):
        for file_name in file_names:
            target_dir = dir_path.replace(source_dir, dest_dir, 1)
            if not os.path.exists(target_dir):
                os.mkdir(target_dir)
            src_file = os.path.join(dir_path, file_name)
            dest_file = os.path.join(target_dir, file_name)
            shutil.copyfile(src_file, dest_file)



def appendTextElement(name, value):
	elem = xmlDoc.createElement(name)
	elem.appendChild(xmlDoc.createTextNode(value))
	xmlDocRoot.appendChild(elem)

def get_ranger_classpath():
	global conf_dict
	cp = [ os.path.join(conf_dict["INSTALL_DIR"],"dist","*"), os.path.join(conf_dict["INSTALL_DIR"],"lib","*"), os.path.join(conf_dict["INSTALL_DIR"], 'conf')]
	class_path = get_class_path(cp)
	return class_path

def get_jdk_options():
    global conf_dict
    return [os.getenv('RANGER_PROPERTIES', ''), "-Dlogdir="+os.getenv("RANGER_LOG_DIR")]

def init_variables():
	global  INSTALL_DIR,RANGER_USERSYNC_HOME, conf_dict
	# These are set from the Monarch
	conf_dict["HDP_RESOURCES_DIR"] = os.getenv("HDP_RESOURCES_DIR")
	conf_dict["RANGER_ADMIN_HOME"] = os.getenv("RANGER_ADMIN_HOME")
	conf_dict["RANGER_USERSYNC_HOME"] = os.getenv("RANGER_USERSYNC_HOME")
	conf_dict["INSTALL_DIR"] = os.getenv("RANGER_USERSYNC_HOME")
	USERSYNC_HOME = conf_dict['RANGER_USERSYNC_HOME']
	copy_files(os.path.join(USERSYNC_HOME,"conf.dist"), os.path.join(USERSYNC_HOME,"conf"))
	pass

def get_class_path(paths):
    separator = ';' if sys.platform == 'win32' else ':';
    return separator.join(paths)

def get_java_env():
    JAVA_HOME = os.getenv('JAVA_HOME')
    if JAVA_HOME:
        return os.path.join(JAVA_HOME, 'bin', 'java')
    else:
        log('java and jar commands are not available. Please configure JAVA_HOME','exception')
        os.sys.exit(1)


if service_entry:
	try:
		#ranger_install.run_setup(cmd, app_type)
		#init_logfiles()

		init_variables()
		jdk_options = get_jdk_options()
		class_path = get_ranger_classpath()
		java_class = 'org.apache.ranger.authentication.UnixAuthenticationService'
		class_arguments = ''

		dom = getDOMImplementation()
		xmlDoc = dom.createDocument(None, 'service', None)
		xmlDocRoot = xmlDoc.documentElement
		arguments = ' '.join([' '.join(jdk_options), '-cp', class_path, java_class, class_arguments ])
		appendTextElement('id', "ranger-usersync")
		appendTextElement('name', "ranger-usersync")
		appendTextElement('description', 'This service runs ranger-usersync')
		appendTextElement('executable', get_java_env())
		appendTextElement('arguments', arguments)
		uglyXml = xmlDoc.toprettyxml(indent='  ')
		text_re = re.compile('>\n\s+([^<>\s].*?)\n\s+</', re.DOTALL)
		prettyXml = text_re.sub('>\g<1></', uglyXml)

		print(prettyXml)
	except:
		sys.exit(1)

if configure_entry:
    #configure()
    sys.exit(0)
