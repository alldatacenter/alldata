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
import shlex
import platform
import logging
import subprocess
from os.path import basename
import time
import socket
import glob
import getpass
globalDict = {}

os_name = platform.system()
os_name = os_name.upper()
os_user = getpass.getuser()
ranger_version=''
jisql_debug=True
retryPatchAfterSeconds=120
stalePatchEntryHoldTimeInMinutes=10
java_patch_regex="^Patch.*?J\d{5}.class$"
is_unix = os_name == "LINUX" or os_name == "DARWIN"
pre_sql_prefix="PatchPreSql_"
post_sql_prefix="PatchPostSql_"
java_patch_version_regex="Patch.*?_J(.*).class"

RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")
if RANGER_ADMIN_HOME is None:
	RANGER_ADMIN_HOME = os.getcwd()

if socket.getfqdn().find('.')>=0:
	client_host=socket.getfqdn()
else:
	client_host=socket.gethostbyaddr(socket.gethostname())[0]

RANGER_ADMIN_CONF = os.getenv("RANGER_ADMIN_CONF")
if RANGER_ADMIN_CONF is None:
	if is_unix:
		RANGER_ADMIN_CONF = RANGER_ADMIN_HOME
	elif os_name == "WINDOWS":
		RANGER_ADMIN_CONF = os.path.join(RANGER_ADMIN_HOME,'bin')

def check_output(query):
	if is_unix:
		p = subprocess.Popen(shlex.split(query), stdout=subprocess.PIPE)
	elif os_name == "WINDOWS":
		p = subprocess.Popen(query, stdout=subprocess.PIPE, shell=True)
	output = p.communicate ()[0]
	return output.decode()

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

def populate_global_dict():
	global globalDict
	if is_unix:
		read_config_file = open(os.path.join(RANGER_ADMIN_CONF,'install.properties'))
	elif os_name == "WINDOWS":
		read_config_file = open(os.path.join(RANGER_ADMIN_CONF,'install_config.properties'))
	for each_line in read_config_file.read().split('\n') :
		each_line = each_line.strip();
		if len(each_line) == 0:
			continue
		elif each_line[0] == "#":
			continue
		if re.search('=', each_line):
			key , value = each_line.split("=",1)
			key = key.strip()
			if 'PASSWORD' in key:
				value = ''
			value = value.strip()
			globalDict[key] = value
		if 'ranger_admin_max_heap_size' not in globalDict:
			globalDict['ranger_admin_max_heap_size']='1g'
		elif 'ranger_admin_max_heap_size' in globalDict:
			ranger_admin_heap_size = globalDict['ranger_admin_max_heap_size']
			if str(ranger_admin_heap_size.lower()).endswith("g"):
				ranger_admin_heap_size_numeric = int(str(ranger_admin_heap_size).lower().rstrip("g"))
				if ranger_admin_heap_size_numeric < 1:
					globalDict['ranger_admin_max_heap_size']='1g'
			if str(ranger_admin_heap_size.lower()).endswith("m"):
				ranger_admin_heap_size_numeric = int(str(ranger_admin_heap_size).lower().rstrip("m"))
				if ranger_admin_heap_size_numeric < 1024:
					globalDict['ranger_admin_max_heap_size']='1g'

def jisql_log(query, db_password):
	if jisql_debug == True:
		if os_name == "WINDOWS":
			query = query.replace(' -p "'+db_password+'"' , ' -p "********"')
			log("[JISQL] "+query, "info")
		else:
			query = query.replace(" -p '"+db_password+"'" , " -p '********'")
			log("[JISQL] "+query, "info")

def password_validation(password):
	if password:
		if re.search("[\\\`'\"]",password):
			log("[E] password contains one of the unsupported special characters like \" ' \ `","error")
			sys.exit(1)

def subprocessCallWithRetry(query):
	retryCount=1
	returnCode = subprocess.call(query)
	while returnCode!=0:
		retryCount=retryCount+1
		time.sleep(1)
		log("[I] SQL statement execution Failed!! retrying attempt "+str(retryCount)+" of total 3" ,"info")
		returnCode = subprocess.call(query)
		if(returnCode!=0 and retryCount>=3):
			break
	return returnCode

def dbversionBasedOnUserName(userName):
	version = ""
	if userName == "admin" :
		version = 'DEFAULT_ADMIN_UPDATE'
	if userName == "rangerusersync" :
		version = 'DEFAULT_RANGER_USERSYNC_UPDATE'
	if userName == "rangertagsync" :
		version = 'DEFAULT_RANGER_TAGSYNC_UPDATE'
	if userName == "keyadmin" :
		version = 'DEFAULT_KEYADMIN_UPDATE'
	return version

def set_env_val(command):
	proc = subprocess.Popen(command, stdout = subprocess.PIPE)
	for line in proc.stdout:
		(key, _, value) = line.partition("=")
		os.environ[key] = value.rstrip()
	proc.communicate()

def run_env_file(path):
	for filename in glob.glob(path):
		log("[I] Env filename : "+filename, "info")
		if not os.path.exists(filename):
			log("[I] File dose not exist : "+filename, "info")
		else:
			command = shlex.split("env -i /bin/bash -c 'source "+filename+" && env'")
			set_env_val(command)

class BaseDB(object):

	def check_connection(self, db_name, db_user, db_password):
		self.db_user=db_user
		self.db_name=db_name
		self.db_password=db_password
		log("[I] Checking connection..", "info")
		output = self.execute_query(self.get_db_server_status_query())
		if not output.strip('1 |'):
			log("[E] Can't establish connection!! Exiting.." ,"error")
			log("[I] Please run DB setup first or contact Administrator.." ,"info")
			sys.exit(1)
		else:
			log("[I] Checking connection passed.", "info")
			return True

	def apply_patches(self, db_name, db_user, db_password, PATCHES_PATH):
		#first get all patches and then apply each patch
		global globalDict
		xa_db_host = globalDict['db_host']
		if not os.path.exists(PATCHES_PATH):
			log("[I] No patches to apply!","info")
		else:
			# files: coming from os.listdir() sorted alphabetically, thus not numerically
			files = os.listdir(PATCHES_PATH)
			if files:
				sorted_files = sorted(files, key=lambda x: str(x.split('.')[0]))
				for filename in sorted_files:
					currentPatch = os.path.join(PATCHES_PATH, filename)
					pre_dict = {}
					post_dict = {}
					version = filename.split('-')[0]
					prefix_for_preSql_patch=pre_sql_prefix + version
					prefix_for_postSql_patch=post_sql_prefix +version
					#getting Java patch which needs to be run before this DB patch.
					pre_dict = self.get_pre_post_java_patches(prefix_for_preSql_patch)
					if pre_dict:
						log ("[I] ruunig pre java patch:[{}]".format(pre_dict),"info")
						self.execute_java_patches(xa_db_host, db_user, db_password, db_name, pre_dict)
					self.import_db_patches(db_name, db_user, db_password, currentPatch)
					#getting Java patch which needs to be run immediately after this DB patch.
					post_dict = self.get_pre_post_java_patches(prefix_for_postSql_patch)
					if post_dict:
						log ("[I] ruunig post java patch:[{}]".format(post_dict),"info")
						self.execute_java_patches(xa_db_host, db_user, db_password, db_name, post_dict)
				self.update_applied_patches_status(db_name, db_user, db_password, "DB_PATCHES")
			else:
				log("[I] No patches to apply!","info")

	def execute_query(self, query):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(self.db_user, self.db_password, self.db_name)
		if is_unix:
			full_command = get_cmd + CT + " -query \"" + query + "\""
		elif os_name == "WINDOWS":
			full_command = get_cmd + " -query \"" + query + "\" -c ;"
		else:
			raise Exception("This OS is not supported!")
		jisql_log(full_command, self.db_password)
		output = check_output(full_command)
		return output

	def execute_update(self, update):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(self.db_user, self.db_password, self.db_name)
		if is_unix:
			full_command = get_cmd + CT + " -query \"" + update + "\""
			jisql_log(full_command, self.db_password)
			return subprocess.call(shlex.split(full_command))
		elif os_name == "WINDOWS":
			full_command = get_cmd + " -query \"" + update + "\" -c ;"
			jisql_log(full_command, self.db_password)
			ret = subprocess.call(full_command)
		raise Exception("This OS is not supported!")

	def execute_file(self, file_name):
		get_cmd = self.get_jisql_cmd(self.db_user, self.db_password, self.db_name)
		if is_unix:
			full_command = get_cmd + " -input %s " %file_name
			jisql_log(full_command, self.db_password)
			ret = subprocess.call(shlex.split(full_command))
		elif os_name == "WINDOWS":
			full_command = get_cmd + " -input %s " %file_name
			jisql_log(full_command, self.db_password)
			ret = subprocess.call(full_command)
		return ret

	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		if self.XA_DB_FLAVOR == "SQLA":
			self.set_options(db_name, db_user, db_password, TABLE_NAME)
		output = self.execute_query(self.get_check_table_query(TABLE_NAME))
		if output.strip(TABLE_NAME + " |"):
			log("[I] Table " + TABLE_NAME +" already exists in database '" + db_name + "'","info")
			return True
		else:
			log("[I] Table " + TABLE_NAME +" does not exist in database " + db_name + "","info")
			return False

	def create_version_history_table(self, db_name, db_user, db_password, file_name,table_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			isTableExist=self.check_table(db_name, db_user, db_password, table_name)
			if isTableExist==False:
				log("[I] Importing "+table_name+" table schema to database " + db_name + " from file: " + name,"info")
			while(isTableExist==False):
				ret=self.execute_file(file_name)
				if ret == 0:
					log("[I] "+name + " file imported successfully","info")
				else:
					log("[E] "+name + " file import failed!","error")
					time.sleep(30)
				isTableExist=self.check_table(db_name, db_user, db_password, table_name)
		else:
			log("[E] Schema file " + name+ " not found","error")
			sys.exit(1)

	def hasDBnJavaPatchesEntries(self, db_name, db_user, db_password, version):
		output = self.execute_query(self.get_version_query(version,'Y'))
		if output.strip(version + " |"):
			return True
		else:
			return False

	def import_db_file(self, db_name, db_user, db_password, file_name):
		isImported=False
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing DB schema to database " + db_name + " from file: " + name,"info")
			ret=self.execute_file(file_name)
			if ret == 0:
				log("[I] "+name + " file imported successfully","info")
				isImported=True
			else:
				log("[E] "+name + " file import failed!","error")
		else:
			log("[E] DB schema file " + name+ " not found","error")
			sys.exit(1)
		return isImported

	def import_core_db_schema(self, db_name, db_user, db_password, file_name,first_table,last_table):
		version = 'CORE_DB_SCHEMA'
		if os.path.isfile(file_name):
			output = self.execute_query(self.get_version_query(version,'Y'))
			if output.strip(version + " |"):
				log("[I] "+version+" is already imported" ,"info")
			else:
				#before executing the patch first check whether its in progress from any host.
				executePatch=False
				output = self.execute_query(self.get_version_query(version,'N'))
				if output.strip(version + " |"):
					#check whether process is in progress from different host if yes then wait until it finishes.
					output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
					while(output.strip(version + " |")):
						log("[I] "+ version  +" is being applied by some other Host" ,"info")
						time.sleep(retryPatchAfterSeconds)
						output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
					#check whether process is in progress from same host
					output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					while(output.strip(version + " |")):
						log("[I] "+ version  +" is being imported by some other process of the same host" ,"info")
						time.sleep(retryPatchAfterSeconds)
						output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					output = self.execute_query(self.get_stale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					if(output.strip(version + " |")):
						ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
						if ret == 0:
							log("[I] Deleted old entry of patch:"+version, "info")
						output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
						while(output.strip(version + " |")):
							log("[I] "+ version  +" is being applied by some other process" ,"info")
							time.sleep(retryPatchAfterSeconds)
							output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
						if not (output.strip(version + " |")):
							executePatch=True
				else:
					executePatch=True

				if executePatch:
					ret = self.execute_update(self.insert_patch_applied_query(version,ranger_version,'N',client_host))
					if ret != 0:
						log("[E] "+ version +" import failed", "error")
						sys.exit(1)
					isSchemaCreated=False
					countTries = 0
					while(isSchemaCreated==False and countTries<3):
						countTries=countTries+1
						isFirstTableExist = self.check_table(db_name, db_user, db_password, first_table)
						isLastTableExist = self.check_table(db_name, db_user, db_password, last_table)
						isDBPatchesApplied=self.hasDBnJavaPatchesEntries(db_name, db_user, db_password, "DB_PATCHES")
						isJavaPatchesApplied=self.hasDBnJavaPatchesEntries(db_name, db_user, db_password, "JAVA_PATCHES")
						if isFirstTableExist == True and isLastTableExist == True and isDBPatchesApplied ==True and isJavaPatchesApplied ==True:
							isSchemaCreated=True
						else:
							isImported=self.import_db_file(db_name, db_user, db_password, file_name)
							if (isImported):
								ret = self.execute_update(self.update_patch_applied_query(ranger_version,'Y','localhost'))
								if ret == 0:
									log("[I] Patches status entries updated from base ranger version to current installed ranger version:"+ranger_version, "info")
							else:
								log("[I] Unable to create DB schema, Please drop the database and try again" ,"info")
								break

					if isSchemaCreated == True:
						ret = self.execute_update(self.update_patch_status_query(version,'N',client_host))
						if ret == 0:
							log("[I] "+version +" import status has been updated", "info")
						else:
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							log("[E] Updating "+version +" import status failed", "error")
							sys.exit(1)
					else:
						ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
						log("[E] "+version + " import failed!","error")
						sys.exit(1)

	def get_pre_post_java_patches(self, patch_name_prefix):
		my_dict = {}
		version = ""
		className = ""
		app_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp")
		javaFiles = os.path.join(app_home,"WEB-INF","classes","org","apache","ranger","patch")
		if not os.path.exists(javaFiles):
			log("[I] No java patches to apply!","info")
		else:
			files = os.listdir(javaFiles)
			if files:
				for filename in files:
					f = re.match(java_patch_regex,filename)
					if f:
						if patch_name_prefix != "":
							p = re.match(patch_name_prefix, filename)
							if p:
								version = re.match(java_patch_version_regex,filename)
								version = version.group(1)
								key3 = int(version)
								my_dict[key3] = filename
		return my_dict

	def import_db_patches(self, db_name, db_user, db_password, file_name):
		if self.XA_DB_FLAVOR == "POSTGRES":
			self.create_language_plpgsql(db_user, db_password, db_name)
		name = basename(file_name)
		if os.path.isfile(file_name):
			version = name.split('-')[0]
			log("[I] Executing patch on  " + db_name + " from file: " + name,"info")
			output = self.execute_query(self.get_version_query(version,'Y'))
			if output.strip(version + " |"):
				log("[I] Patch "+ name  +" is already applied" ,"info")
			else:
				#before executing the patch first check whether its in progress from any host.
				executePatch=False
				output = self.execute_query(self.get_version_query(version,'N'))
				if output.strip(version + " |"):
					#check whether process is in progress from different host if yes then wait until it finishes.
					output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
					while(output.strip(version + " |")):
						log("[I] Patch "+ name  +" is being applied by some other host" ,"info")
						time.sleep(retryPatchAfterSeconds)
						output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
					#check whether process is in progress from same host and entry was created in less than $stalePatchEntryHoldTimeInMinutes minute from current time
					output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					while(output.strip(version + " |")):
						#wait for process to complete or $stalePatchEntryHoldTimeInMinutes minutes
						log("[I] Patch "+ name  +" is being applied by some other process of current host" ,"info")
						time.sleep(retryPatchAfterSeconds)
						output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					##check whether process is in progress from same host and entry was created before $stalePatchEntryHoldTimeInMinutes minute from current time
					output = self.execute_query(self.get_stale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
					if(output.strip(version + " |")):
						#delete the $stalePatchEntryHoldTimeInMinutes minute old entry and allow the same host to proceed with patch execution
						ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
						if ret == 0:
							log("[I] deleted old entry of patch:"+name, "info")
						#if patch is still in progress from any host then wait for its completion or forever
						output = self.execute_query(self.get_version_query(version,'N'))
						while(output.strip(version + " |")):
							log("[I] Patch "+ name  +" is being applied by some other process" ,"info")
							time.sleep(retryPatchAfterSeconds)
							output = self.execute_query(self.get_version_query(version,'N'))
						#if patch entry does not exist then allow patch execution to the current host.
						if not (output.strip(version + " |")):
							executePatch=True
				else:
					executePatch=True
				##if patch entry does not exist in x_db_version_h table then insert the entry with active='N' and execute the patch.
				if executePatch:
					ret = self.execute_update(self.insert_patch_applied_query(version,ranger_version,'N',client_host))
					if ret == 0:
						log ("[I] Patch "+ name +" is being applied..","info")
					else:
						log("[E] Patch "+ name +" failed", "error")
						sys.exit(1)
					ret=self.execute_file(file_name)
					if ret!=0:
						output = self.execute_query(self.get_version_query(version,'Y'))
						if output.strip(version + " |"):
							ret=0
							log("[I] Patch "+ name  +" has been applied by some other process!" ,"info")
					if ret == 0:
						log("[I] "+name + " patch applied","info")
						ret = self.execute_update(self.update_patch_status_query(version,'N',client_host))
						if ret == 0:
							log("[I] Patch version updated", "info")
						else:
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							log("[E] Updating patch version failed", "error")
							sys.exit(1)
					else:
						ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
						log("[E] "+name + " import failed!","error")
						sys.exit(1)


	def execute_java_patches(self, xa_db_host, db_user, db_password, db_name, my_dict):
		global globalDict
		version = ""
		className = ""
		app_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp")
		ranger_log_dir = globalDict['RANGER_ADMIN_LOG_DIR']
		if ranger_log_dir == "$PWD":
			ranger_log_dir = os.path.join(RANGER_ADMIN_HOME,"ews","logs")
		javaFiles = os.path.join(app_home,"WEB-INF","classes","org","apache","ranger","patch")
		logback_conf_file = globalDict['RANGER_ADMIN_LOGBACK_CONF_FILE']
		if not logback_conf_file:
			logback_conf_file = "file:" + os.path.join(app_home, "WEB-INF", "classes", "conf", "logback.xml")
		else:
			logback_conf_file = "file:" + logback_conf_file
		log("[I] RANGER ADMIN LOG DIR : " + ranger_log_dir, "info")
		log("[I] LOGBACK CONF FILE : " + logback_conf_file, "info")
		if not os.path.exists(javaFiles):
			log("[I] No java patches to apply!","info")
		else:
			files = os.listdir(javaFiles)
			if files:
				if not my_dict:
					for filename in files:
						f = re.match(java_patch_regex,filename)
						if f:
							version = re.match(java_patch_version_regex,filename)
							version = version.group(1)
							key3 = int(version)
							my_dict[key3] = filename

			keylist = list(my_dict)
			keylist.sort()
			for key in keylist:
				#print "%s: %s" % (key, my_dict[key])
				version = str(key)
				className = my_dict[key]
				className = className.strip(".class")
				if version != "":
					output = self.execute_query(self.get_version_query('J'+version,'Y'))
					if output.strip(version + " |"):
						log("[I] Java patch "+ className  +" is already applied" ,"info")
					else:
						#before executing the patch first check whether its in progress from any host.
						executePatch=False
						output = self.execute_query(self.get_version_query('J'+version,'N'))
						if output.strip(version + " |"):
							#check whether process is in progress from different host if yes then wait until it finishes.
							output = self.execute_query(self.get_version_notupdatedby_query('J'+version,'Y',client_host))
							while(output.strip(version + " |")):
								log("[I] Java patch "+ className  +" is being applied by some other host" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_version_notupdatedby_query('J'+version,'Y',client_host))
							#check whether process is in progress from same host
							output = self.execute_query(self.get_unstale_patch_query('J'+version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
							while(output.strip(version + " |")):
								log("[I] Java patch "+ className  +" is being applied by some other process of current host" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_unstale_patch_query('J'+version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
							output = self.execute_query(self.get_stale_patch_query('J'+version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
							if(output.strip(version + " |")):
								ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
								if ret == 0:
									log("[I] deleted old entry of patch:"+className, "info")
								output = self.execute_query(self.get_version_notupdatedby_query('J'+version,'N',client_host))
								while(output.strip(version + " |")):
									log("[I] Java patch "+ className  +" is being applied by some other process" ,"info")
									time.sleep(retryPatchAfterSeconds)
									output = self.execute_query(self.get_version_notupdatedby_query('J'+version,'N',client_host))
								if not (output.strip(version + " |")):
									executePatch=True
						else:
							executePatch=True
						##if patch entry does not exist in x_db_version_h table then insert the entry with active='N' and execute the patch.
						if executePatch:
							ret = self.execute_update(self.insert_patch_applied_query('J'+version,ranger_version,'N',client_host))
							if ret == 0:
								log ("[I] java patch "+ className +" is being applied..","info")
							else:
								log("[E] java patch "+ className +" failed", "error")
								sys.exit(1)
							if is_unix:
								path = os.path.join("%s","WEB-INF","classes","conf:%s","WEB-INF","classes","lib","*:%s","WEB-INF",":%s","META-INF",":%s","WEB-INF","lib","*:%s","WEB-INF","classes",":%s","WEB-INF","classes","META-INF:%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
							elif os_name == "WINDOWS":
								path = os.path.join("%s","WEB-INF","classes","conf;%s","WEB-INF","classes","lib","*;%s","WEB-INF",";%s","META-INF",";%s","WEB-INF","lib","*;%s","WEB-INF","classes",";%s","WEB-INF","classes","META-INF;%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
							get_java_cmd = "%s -XX:MetaspaceSize=100m -XX:MaxMetaspaceSize=200m -Xmx%s -Xms1g -Dlogdir=%s -Dlogback.configurationFile=%s -Duser=%s -Dhostname=%s -cp %s org.apache.ranger.patch.%s"%(self.JAVA_BIN,globalDict['ranger_admin_max_heap_size'],ranger_log_dir,logback_conf_file,os_user,client_host,path,className)
							if is_unix:
								ret = subprocess.call(shlex.split(get_java_cmd))
							elif os_name == "WINDOWS":
								ret = subprocess.call(get_java_cmd)
							if ret == 0:
								ret = self.execute_update(self.update_patch_status_query('J'+version,'N',client_host))
								if ret == 0:
									log ("[I] java patch "+ className +" is applied..","info")
								else:
									ret = self.execute_update(self.delete_version_updatedby_query('J'+version,'N',client_host))
									log("[E] java patch "+ className +" failed", "error")
									sys.exit(1)
							else:
								ret = self.execute_update(self.delete_version_updatedby_query('J'+version,'N',client_host))
								log("[E] applying java patch "+ className +" failed", "error")
								sys.exit(1)

	def change_admin_default_password(self, xa_db_host, db_user, db_password, db_name,userName,oldPassword,newPassword):
		CT=self.commandTerminator
		version = ""
		className = "ChangePasswordUtil"
		version = dbversionBasedOnUserName(userName)
		app_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp")
		ranger_log_dir = globalDict['RANGER_ADMIN_LOG_DIR']
		if ranger_log_dir == "$PWD":
			ranger_log_dir = os.path.join(RANGER_ADMIN_HOME,"ews","logs")
		filePath = os.path.join(app_home,"WEB-INF","classes","org","apache","ranger","patch","cliutil","ChangePasswordUtil.class")
		logback_conf_file = globalDict['RANGER_ADMIN_LOGBACK_CONF_FILE']
		if not logback_conf_file:
			logback_conf_file = "file:" + os.path.join(app_home, "WEB-INF", "classes", "conf", "logback.xml")
		else:
			logback_conf_file = "file:" + logback_conf_file

		log("[I] RANGER ADMIN LOG DIR : " + ranger_log_dir, "info")
		log("[I] LOGBACK CONF FILE : " + logback_conf_file, "info")
		if os.path.exists(filePath):
			if version != "":
				output = self.execute_query(self.get_version_query(version,'Y'))
				if output.strip(version + " |"):
					log("[I] Ranger "+ userName +" default password has already been changed!!","info")
				else:
					#before executing the patch first check whether its in progress from any host.
					executePatch=False
					output = self.execute_query(self.get_version_query(version,'N'))
					if output.strip(version + " |"):
						#check whether process is in progress from different host if yes then wait until it finishes.
						output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
						countTries = 0
						while(output.strip(version + " |")):
							if countTries < 3:
								log("[I] Ranger Password change utility is being executed by some other host" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
								countTries += 1
							else:
								log("[E] Tried updating the password "+ str(countTries) + " times","error")
								log("[E] If Ranger "+  userName +" user password is not being changed by some other host then manually delete the entry from ranger database table x_db_version_h table where version is " + version ,"error")
								sys.exit(1)
						#check whether process is in progress from same host
						output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						while(output.strip(version + " |")):
							log("[I] Ranger Password change utility for user "+ userName  +" is being executed by some other process of current host" ,"info")
							time.sleep(retryPatchAfterSeconds)
							output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						output = self.execute_query(self.get_stale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						if(output.strip(version + " |")):
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							if ret == 0:
								log("[I] deleted old entry for user:"+userName, "info")
							output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
							while(output.strip(version + " |")):
								log("[I] Ranger Password change utility for user "+ userName  +" is being applied by some other process" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
							if not (output.strip(version + " |")):
								executePatch=True
					else:
						executePatch=True

					if executePatch:
						ret = self.execute_update(self.insert_patch_applied_query(version,ranger_version,'N',client_host))
						if ret == 0:
							log ("[I] Ranger "+ userName +" default password change request is in process..","info")
						else:
							log("[E] Ranger "+ userName +" default password change request failed", "error")
							sys.exit(1)
						if is_unix:
							path = os.path.join("%s","WEB-INF","classes","conf:%s","WEB-INF","classes","lib","*:%s","WEB-INF",":%s","META-INF",":%s","WEB-INF","lib","*:%s","WEB-INF","classes",":%s","WEB-INF","classes","META-INF:%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
						elif os_name == "WINDOWS":
							path = os.path.join("%s","WEB-INF","classes","conf;%s","WEB-INF","classes","lib","*;%s","WEB-INF",";%s","META-INF",";%s","WEB-INF","lib","*;%s","WEB-INF","classes",";%s","WEB-INF","classes","META-INF;%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
						get_java_cmd = "%s -Dlogdir=%s -Dlogback.configurationFile=%s -Duser=%s -Dhostname=%s -cp %s org.apache.ranger.patch.cliutil.%s %s %s %s -default"%(self.JAVA_BIN,ranger_log_dir,logback_conf_file,os_user,client_host,path,className,'"'+userName+'"','"'+oldPassword+'"','"'+newPassword+'"')
						if is_unix:
							status = subprocess.call(shlex.split(get_java_cmd))
						elif os_name == "WINDOWS":
							status = subprocess.call(get_java_cmd)
						if status == 0 or status==2:
							ret = self.execute_update(self.update_patch_status_query(version,'N',client_host))
							if ret == 0 and status == 0:
								log ("[I] Ranger "+ userName +" default password change request processed successfully..","info")
							elif ret == 0 and status == 2:
								log ("[I] Ranger "+ userName +" default password change request process skipped!","info")
							else:
								ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
								log("[E] Ranger "+ userName +" default password change request failed", "error")
								sys.exit(1)
						else:
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							log("[E] Ranger "+ userName +" default password change request failed", "error")
							sys.exit(1)

	def change_all_admin_default_password(self, xa_db_host, db_user, db_password, db_name,userPwdArray):
		CT=self.commandTerminator
		userPwdString =""
		if len(userPwdArray)>5:
			for j in range(len(userPwdArray)):
				if str(userPwdArray[j]) == "-pair":
					userPwdString= userPwdString + " \"" + userPwdArray[j+1] + "\" \"" + userPwdArray[j+2] + "\" \"" + userPwdArray[j+3] +"\""
		userName = "all admins"
		className = "ChangePasswordUtil"
		version = "DEFAULT_ALL_ADMIN_UPDATE"
		app_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp")
		ranger_log_dir = globalDict['RANGER_ADMIN_LOG_DIR']
		if ranger_log_dir == "$PWD":
			ranger_log_dir = os.path.join(RANGER_ADMIN_HOME,"ews","logs")
		filePath = os.path.join(app_home,"WEB-INF","classes","org","apache","ranger","patch","cliutil","ChangePasswordUtil.class")
		logback_conf_file = globalDict['RANGER_ADMIN_LOGBACK_CONF_FILE']
		if not logback_conf_file:
			logback_conf_file = "file:" + os.path.join(app_home, "WEB-INF", "classes", "conf", "logback.xml")
		else:
			logback_conf_file = "file:" + logback_conf_file

		log("[I] RANGER ADMIN LOG DIR : " + ranger_log_dir, "info")
		log("[I] LOGBACK CONF FILE : " + logback_conf_file, "info")
		if os.path.exists(filePath):
			if version != "":
				output = self.execute_query(self.get_version_query(version,'Y'))
				if output.strip(version + " |"):
					log("[I] Ranger "+ userName +" default password has already been changed!!","info")
				else:
					#before executing the patch first check whether its in progress from any host.
					executePatch=False
					output = self.execute_query(self.get_version_query(version,'N'))
					if output.strip(version + " |"):
						#check whether process is in progress from different host if yes then wait until it finishes.
						output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
						countTries = 0
						while(output.strip(version + " |")):
							if countTries < 3:
								log("[I] Ranger Password change utility is being executed by some other host" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
								countTries += 1
							else:
								log("[E] Tried updating the password "+ str(countTries) + " times","error")
								log("[E] If Ranger "+  userName +" user password is not being changed by some other host then manually delete the entry from ranger database table x_db_version_h table where version is " + version ,"error")
								sys.exit(1)
						#check whether process is in progress from same host
						output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						while(output.strip(version + " |")):
							log("[I] Ranger Password change utility for "+ userName  +" is being executed by some other process of current host" ,"info")
							time.sleep(retryPatchAfterSeconds)
							output = self.execute_query(self.get_unstale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						output = self.execute_query(self.get_stale_patch_query(version,'N',client_host,stalePatchEntryHoldTimeInMinutes))
						if(output.strip(version + " |")):
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							if ret == 0:
								log("[I] deleted old entry of patch for :"+userName, "info")
							output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
							while(output.strip(version + " |")):
								log("[I] Ranger Password change utility "+ userName  +" is being applied by some other process" ,"info")
								time.sleep(retryPatchAfterSeconds)
								output = self.execute_query(self.get_version_notupdatedby_query(version,'N',client_host))
							if not (output.strip(version + " |")):
								executePatch=True
					else:
						executePatch=True

					if executePatch:
						ret = self.execute_update(self.insert_patch_applied_query(version,ranger_version,'N',client_host))
						if ret == 0:
							log ("[I] Ranger "+ userName +" default password change request is in process..","info")
						else:
							log("[E] Ranger "+ userName +" default password change request failed", "error")
							sys.exit(1)
						if is_unix:
							path = os.path.join("%s","WEB-INF","classes","conf:%s","WEB-INF","classes","lib","*:%s","WEB-INF",":%s","META-INF",":%s","WEB-INF","lib","*:%s","WEB-INF","classes",":%s","WEB-INF","classes","META-INF:%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
						elif os_name == "WINDOWS":
							path = os.path.join("%s","WEB-INF","classes","conf;%s","WEB-INF","classes","lib","*;%s","WEB-INF",";%s","META-INF",";%s","WEB-INF","lib","*;%s","WEB-INF","classes",";%s","WEB-INF","classes","META-INF;%s" )%(app_home ,app_home ,app_home, app_home, app_home, app_home ,app_home ,self.SQL_CONNECTOR_JAR)
						get_java_cmd = "%s -Dlogdir=%s -Dlogback.configurationFile=%s -Duser=%s -Dhostname=%s -cp %s org.apache.ranger.patch.cliutil.%s %s -default"%(self.JAVA_BIN,ranger_log_dir,logback_conf_file,os_user,client_host,path,className, userPwdString)
						if is_unix:
							status = subprocess.call(shlex.split(get_java_cmd))
						elif os_name == "WINDOWS":
							status = subprocess.call(get_java_cmd)
						if status == 0 or status==2:
							ret = self.execute_update(self.update_patch_status_query(version,'N',client_host))
							if ret == 0 and status == 0:
								log ("[I] Ranger "+ userName +" default password change request processed successfully..","info")
							elif ret == 0 and status == 2:
								log ("[I] Ranger "+ userName +" default password change request process skipped!","info")
							else:
								ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
								log("[E] Ranger	"+ userName +" default password change request failed", "error")
								sys.exit(1)
						else:
							ret = self.execute_update(self.delete_version_updatedby_query(version,'N',client_host))
							log("[E] Ranger "+ userName +" default password change request failed", "error")
							sys.exit(1)

	def hasPendingPatches(self, db_name, db_user, db_password, version):
		output = self.execute_query(self.get_patch_status_in_current_version_query(version,ranger_version,'Y'))
		if output.strip(version + " |"):
			return False
		else:
			return True

	def update_applied_patches_status(self,db_name, db_user, db_password,version):
		if self.hasPendingPatches(db_name, db_user, db_password,version) == True:
			ret = self.execute_update(self.insert_patch_applied_query(version,ranger_version,'Y',client_host))
			if ret != 0:
				log("[E] "+ version +" status entry to x_db_version_h table failed", "error")
				sys.exit(1)
			else:
				log("[I] "+ version +" status entry to x_db_version_h table completed", "info")

	def is_new_install(self, xa_db_host, db_user, db_password, db_name):
		output = self.execute_query(self.get_db_server_status_query())
		if not output.strip('1 |'):
			sys.exit(0)
		version="J10001"
		output = self.execute_query(self.get_version_query(version,'Y'))
		if not output.strip(version + " |"):
			sys.exit(0)

	def get_version_query(self, version, isActive):
		return "select version from x_db_version_h where version = '%s' and active = '%s';" % (version,isActive)

	def get_version_updatedby_query(self, version, isActive, client_host):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s';" % (version,isActive,client_host)

	def get_version_notupdatedby_query(self, version, isActive, client_host):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by!='%s';" % (version,isActive,client_host)

	def delete_version_notupdatedby_query(self, version, isActive, client_host):
		return "delete from x_db_version_h where version = '%s' and active = '%s' and updated_by!='%s';" % (version,isActive,client_host)

	def delete_version_updatedby_query(self, version, isActive, client_host):
		return "delete from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s';" % (version,isActive,client_host)

	def update_patch_applied_query(self, ranger_version, isActive, client_host):
		return "update x_db_version_h set inst_by='%s' where active='%s' and updated_by='%s';" %(ranger_version,isActive,client_host)

	def update_patch_status_query(self, version, isActive, client_host):
		return "update x_db_version_h set active='Y' where version='%s' and active='%s' and updated_by='%s';" %(version,isActive,client_host)

	def get_patch_status_in_current_version_query(self, version,ranger_version, isActive):
		return "select version from x_db_version_h where version = '%s' and inst_by = '%s' and active = '%s';" % (version,ranger_version,isActive)

class MysqlConf(BaseDB):
	# Constructor
	def __init__(self, host,SQL_CONNECTOR_JAR,JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,is_db_override_jdbc_connection_string,db_override_jdbc_connection_string):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.db_ssl_enabled=db_ssl_enabled.lower()
		self.db_ssl_required=db_ssl_required.lower()
		self.db_ssl_verifyServerCertificate=db_ssl_verifyServerCertificate.lower()
		self.db_ssl_auth_type=db_ssl_auth_type.lower()
		self.javax_net_ssl_keyStore=javax_net_ssl_keyStore
		self.javax_net_ssl_keyStorePassword=javax_net_ssl_keyStorePassword
		self.javax_net_ssl_trustStore=javax_net_ssl_trustStore
		self.javax_net_ssl_trustStorePassword=javax_net_ssl_trustStorePassword
		self.commandTerminator=" "
		self.XA_DB_FLAVOR = "MYSQL"
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password ,db_name):
		path = RANGER_ADMIN_HOME
		db_ssl_param=''
		db_ssl_cert_param=''
		if self.db_ssl_enabled == 'true':
			db_ssl_param="?useSSL=%s&requireSSL=%s&verifyServerCertificate=%s" %(self.db_ssl_enabled,self.db_ssl_required,self.db_ssl_verifyServerCertificate)
			if self.db_ssl_verifyServerCertificate == 'true':
				if self.db_ssl_auth_type == '1-way':
					db_ssl_cert_param=" -Djavax.net.ssl.trustStore=%s -Djavax.net.ssl.trustStorePassword=%s " %(self.javax_net_ssl_trustStore,self.javax_net_ssl_trustStorePassword)
				else:
					db_ssl_cert_param=" -Djavax.net.ssl.keyStore=%s -Djavax.net.ssl.keyStorePassword=%s -Djavax.net.ssl.trustStore=%s -Djavax.net.ssl.trustStorePassword=%s " %(self.javax_net_ssl_keyStore,self.javax_net_ssl_keyStorePassword,self.javax_net_ssl_trustStore,self.javax_net_ssl_trustStorePassword)
		else:
			if "useSSL" not in db_name:
				db_ssl_param="?useSSL=false"
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver mysqlconj -cstring %s -u '%s' -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN,db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path,self.db_override_jdbc_connection_string,user,password)
			else:
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver mysqlconj -cstring jdbc:mysql://%s/%s%s -u '%s' -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN,db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path,self.host,db_name,db_ssl_param,user,password)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver mysqlconj -cstring %s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN,db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string,user, password)
			else:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver mysqlconj -cstring jdbc:mysql://%s/%s%s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN,db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.host, db_name, db_ssl_param,user, password)
		return jisql_cmd

	def get_check_table_query(self, TABLE_NAME):
		return "show tables like '%s';" % (TABLE_NAME)

	def get_unstale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and TIMESTAMPDIFF(MINUTE,inst_at,CURRENT_TIMESTAMP)<%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def get_stale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and TIMESTAMPDIFF(MINUTE,inst_at,CURRENT_TIMESTAMP)>=%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def insert_patch_applied_query(self, version, ranger_version,isActive, client_host):
		return "insert into x_db_version_h (version, inst_at, inst_by, updated_at, updated_by,active) values ('%s', current_timestamp, '%s', current_timestamp, '%s','%s') ;" %(version,ranger_version,client_host,isActive)

	def get_db_server_status_query(self):
		return "select 1;"

class OracleConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host 
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.commandTerminator=" -c \\; "
		self.XA_DB_FLAVOR = "ORACLE"
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password ,db_name):
		path = RANGER_ADMIN_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if not re.search('-Djava.security.egd=file:///dev/urandom', self.JAVA_BIN):
			self.JAVA_BIN = self.JAVA_BIN + " -Djava.security.egd=file:///dev/urandom "

		#if self.host.count(":") == 2:
		if self.host.count(":") == 2 or self.host.count(":") == 0:
			#jdbc:oracle:thin:@[HOST][:PORT]:SID or #jdbc:oracle:thin:@GL
			cstring="jdbc:oracle:thin:@%s" %(self.host)
		else:
			#jdbc:oracle:thin:@//[HOST][:PORT]/SERVICE
			cstring="jdbc:oracle:thin:@//%s" %(self.host)

		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u '%s' -p '%s' -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string, user, password)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u '%s' -p '%s' -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, cstring, user, password)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string, user, password)
			else:
				jisql_cmd = "%s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, cstring, user, password)
		return jisql_cmd

	def execute_file(self, file_name):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(self.db_user, self.db_password, self.db_name)
		if is_unix:
			full_command = get_cmd + " -input %s " %file_name + CT
			jisql_log(full_command, self.db_password)
			ret = subprocess.call(shlex.split(full_command))
		elif os_name == "WINDOWS":
			full_command = get_cmd + " -input %s " %file_name
			jisql_log(full_command, self.db_password)
			ret = subprocess.call(full_command)
		return ret

	def get_check_table_query(self, TABLE_NAME):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(self.db_user, self.db_password, self.db_name)
		if is_unix:
			query = get_cmd + CT + " -query 'select default_tablespace from user_users;'"
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"select default_tablespace from user_users;\" -c ;"
		jisql_log(query, self.db_password)
		output = check_output(query).strip()
		output = output.strip(' |')
		db_name=self.db_name.upper()
		if (db_name =='' and output is not None and output != ''):
			db_name = output
		#db_name could be given db_name or user's default tablespace name
		return "select UPPER(table_name) from all_tables where UPPER(tablespace_name)=UPPER('%s') and UPPER(table_name)=UPPER('%s');" %(db_name ,TABLE_NAME)

	def get_unstale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and ((sysdate-INST_AT)*1440)<%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def get_stale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and ((sysdate-INST_AT)*1440)>=%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def insert_patch_applied_query(self, version, ranger_version,isActive, client_host):
		return "insert into x_db_version_h (id,version, inst_at, inst_by, updated_at, updated_by,active) values (X_DB_VERSION_H_SEQ.nextval,'%s', sysdate, '%s', sysdate, '%s','%s') ;" %(version,ranger_version,client_host,isActive)

	def get_db_server_status_query(self):
		return "select 1 from dual;"

class PostgresConf(BaseDB):
	# Constructor
	def __init__(self, host,SQL_CONNECTOR_JAR,JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,db_ssl_certificate_file,javax_net_ssl_trustStore_type,javax_net_ssl_keyStore_type,is_db_override_jdbc_connection_string,db_override_jdbc_connection_string):
		self.host = host.lower()
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.db_ssl_enabled=db_ssl_enabled.lower()
		self.db_ssl_required=db_ssl_required.lower()
		self.db_ssl_verifyServerCertificate=db_ssl_verifyServerCertificate.lower()
		self.db_ssl_auth_type=db_ssl_auth_type.lower()
		self.db_ssl_certificate_file=db_ssl_certificate_file
		self.javax_net_ssl_keyStore=javax_net_ssl_keyStore
		self.javax_net_ssl_keyStorePassword=javax_net_ssl_keyStorePassword
		self.javax_net_ssl_keyStore_type=javax_net_ssl_keyStore_type.lower()
		self.javax_net_ssl_trustStore=javax_net_ssl_trustStore
		self.javax_net_ssl_trustStorePassword=javax_net_ssl_trustStorePassword
		self.javax_net_ssl_trustStore_type=javax_net_ssl_trustStore_type.lower()
		self.commandTerminator=" "
		self.XA_DB_FLAVOR = "POSTGRES"
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		path = RANGER_ADMIN_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		db_ssl_param=''
		db_ssl_cert_param=''
		if self.db_ssl_enabled == 'true':
			if self.db_ssl_certificate_file != "":
				db_ssl_param="?ssl=%s&sslmode=verify-full&sslrootcert=%s" %(self.db_ssl_enabled,self.db_ssl_certificate_file)
			elif self.db_ssl_verifyServerCertificate == 'true' or self.db_ssl_required == 'true':
				db_ssl_param="?ssl=%s&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory" %(self.db_ssl_enabled)
				if self.db_ssl_auth_type == '1-way':
					db_ssl_cert_param=" -Djavax.net.ssl.trustStore=%s -Djavax.net.ssl.trustStorePassword=%s  -Djavax.net.ssl.trustStoreType=%s" %(self.javax_net_ssl_trustStore,self.javax_net_ssl_trustStorePassword,self.javax_net_ssl_trustStore_type)
				else:
					db_ssl_cert_param=" -Djavax.net.ssl.keyStore=%s -Djavax.net.ssl.keyStorePassword=%s -Djavax.net.ssl.trustStore=%s -Djavax.net.ssl.trustStorePassword=%s -Djavax.net.ssl.trustStoreType=%s -Djavax.net.ssl.keyStoreType=%s" %(self.javax_net_ssl_keyStore,self.javax_net_ssl_keyStorePassword,self.javax_net_ssl_trustStore,self.javax_net_ssl_trustStorePassword,self.javax_net_ssl_trustStore_type,self.javax_net_ssl_keyStore_type)
			else:
				db_ssl_param="?ssl=%s" %(self.db_ssl_enabled)
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver postgresql -cstring %s -u %s -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path, self.db_override_jdbc_connection_string, user, password)
			else:
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver postgresql -cstring jdbc:postgresql://%s/%s%s -u %s -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path, self.host, db_name, db_ssl_param,user, password)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver postgresql -cstring %s -u %s -p \"%s\" -noheader -trim" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string,user, password)
			else:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver postgresql -cstring jdbc:postgresql://%s/%s%s -u %s -p \"%s\" -noheader -trim" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.host, db_name, db_ssl_param,user, password)
		return jisql_cmd

	def create_language_plpgsql(self,db_user, db_password, db_name):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + CT + " -query \"SELECT 1 FROM pg_catalog.pg_language WHERE lanname='plpgsql';\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT 1 FROM pg_catalog.pg_language WHERE lanname='plpgsql';\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if not output.strip('1 |'):
			if is_unix:
				query = get_cmd + CT + " -query \"CREATE LANGUAGE plpgsql;\""
				jisql_log(query, db_password)
				ret = subprocessCallWithRetry(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -query \"CREATE LANGUAGE plpgsql;\" -c ;"
				jisql_log(query, db_password)
				ret = subprocessCallWithRetry(query)
			if ret == 0:
				log("[I] LANGUAGE plpgsql created successfully", "info")
			else:
				log("[E] LANGUAGE plpgsql creation failed", "error")
				sys.exit(1)

	def get_check_table_query(self, TABLE_NAME):
		return "select * from (select table_name from information_schema.tables where table_catalog='%s' and table_name = '%s') as temp;" % (self.db_name,TABLE_NAME)

	def get_unstale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and current_timestamp - interval '%s minutes' < inst_at;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def get_stale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and current_timestamp - interval '%s minutes' >= inst_at;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def insert_patch_applied_query(self, version, ranger_version,isActive, client_host):
		return "insert into x_db_version_h (version, inst_at, inst_by, updated_at, updated_by,active) values ('%s', current_timestamp, '%s', current_timestamp, '%s','%s') ;" %(version,ranger_version,client_host,isActive)

	def get_db_server_status_query(self):
		return "select 1;"

class SqlServerConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.commandTerminator=" -c \\; "
		self.XA_DB_FLAVOR = "MSSQL"
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		#TODO: User array for forming command
		path = RANGER_ADMIN_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver mssql -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver mssql -cstring jdbc:sqlserver://%s\\;databaseName=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.host,db_name)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver mssql -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver mssql -cstring jdbc:sqlserver://%s;databaseName=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.host,db_name)
		return jisql_cmd

	def get_check_table_query(self, TABLE_NAME):
		return "SELECT TABLE_NAME FROM information_schema.tables where table_name = '%s';" % (TABLE_NAME)

	def get_unstale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and DATEDIFF(MINUTE,inst_at,GETDATE())<%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def get_stale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and DATEDIFF(MINUTE,inst_at,GETDATE())>=%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def insert_patch_applied_query(self, version, ranger_version,isActive, client_host):
		return "insert into x_db_version_h (version, inst_at, inst_by, updated_at, updated_by,active) values ('%s', GETDATE(), '%s', GETDATE(), '%s','%s') ;" %(version,ranger_version,client_host,isActive)

	def get_db_server_status_query(self):
		return "select 1;"

class SqlAnywhereConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.commandTerminator=" -c \\; "
		self.XA_DB_FLAVOR = "SQLA"
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		path = RANGER_ADMIN_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -password '%s' -driver sapsajdbc4 -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path,user, password,self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -password '%s' -driver sapsajdbc4 -cstring jdbc:sqlanywhere:database=%s;host=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path,user, password,db_name,self.host)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -password '%s' -driver sapsajdbc4 -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password,self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -password '%s' -driver sapsajdbc4 -cstring jdbc:sqlanywhere:database=%s;host=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password,db_name,self.host)
		return jisql_cmd

	def get_check_table_query(self, TABLE_NAME):
		return "SELECT name FROM sysobjects where name = '%s' and type='U';" % (TABLE_NAME)

	def get_unstale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and DATEDIFF(MINUTE,inst_at,GETDATE())<%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def get_stale_patch_query(self, version, isActive, client_host,stalePatchEntryHoldTimeInMinutes):
		return "select version from x_db_version_h where version = '%s' and active = '%s' and updated_by='%s' and DATEDIFF(MINUTE,inst_at,GETDATE())>=%s;" % (version,isActive,client_host,stalePatchEntryHoldTimeInMinutes)

	def insert_patch_applied_query(self, version, ranger_version,isActive, client_host):
		return "insert into x_db_version_h (version, inst_at, inst_by, updated_at, updated_by,active) values ('%s', GETDATE(), '%s', GETDATE(), '%s','%s') ;" %(version,ranger_version,client_host,isActive)

	def get_db_server_status_query(self):
		return "select 1;"

	def set_options(self, db_name, db_user, db_password, TABLE_NAME):
		CT=self.commandTerminator
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + CT + " -query \"set option public.reserved_keywords='LIMIT';\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.reserved_keywords='LIMIT';\" -c ;"
		jisql_log(query, db_password)
		ret = subprocessCallWithRetry(shlex.split(query))
		if is_unix:
			query = get_cmd + CT + " -query \"set option public.max_statement_count=0;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.max_statement_count=0;\" -c;"
		jisql_log(query, db_password)
		ret = subprocessCallWithRetry(shlex.split(query))
		if is_unix:
			query = get_cmd + CT + " -query \"set option public.max_cursor_count=0;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.max_cursor_count=0;\" -c;"
		jisql_log(query, db_password)
		ret = subprocessCallWithRetry(shlex.split(query))

def main(argv):
	populate_global_dict()

	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)

	global retryPatchAfterSeconds
	global stalePatchEntryHoldTimeInMinutes
	retryPatchAfterSeconds=120
	stalePatchEntryHoldTimeInMinutes=10
	if 'PATCH_RETRY_INTERVAL' in globalDict:
		interval = globalDict['PATCH_RETRY_INTERVAL']
		try:
			retryPatchAfterSeconds=int(interval)
		except ValueError:
			retryPatchAfterSeconds=120

	if 'STALE_PATCH_ENTRY_HOLD_TIME' in globalDict:
		interval = globalDict['STALE_PATCH_ENTRY_HOLD_TIME']
		try:
			stalePatchEntryHoldTimeInMinutes=int(interval)
		except ValueError:
			stalePatchEntryHoldTimeInMinutes=10

	if (not 'JAVA_HOME' in os.environ) or (os.environ['JAVA_HOME'] == ""):
		log("[E] ---------- JAVA_HOME environment property not defined, aborting installation. ----------", "error")
		sys.exit(1)
	else:
		JAVA_BIN=os.path.join(os.environ['JAVA_HOME'],'bin','java')
	if os_name == "WINDOWS" :
		JAVA_BIN = JAVA_BIN+'.exe'
	if os.path.isfile(JAVA_BIN):
		pass
	else:
		JAVA_BIN=globalDict['JAVA_BIN']
		if os.path.isfile(JAVA_BIN):
			pass
		else:
			log("[E] ---------- JAVA Not Found, aborting installation. ----------", "error")
			sys.exit(1)
	#get ranger version
	global ranger_version
	try:
		lib_home = os.path.join(RANGER_ADMIN_HOME,"ews","webapp","WEB-INF","lib","*")
		get_ranger_version_cmd="%s -cp %s org.apache.ranger.common.RangerVersionInfo"%(JAVA_BIN,lib_home)
		ranger_version = check_output(get_ranger_version_cmd).split("\n")[1]
	except Exception as error:
		ranger_version=''

	try:
		if ranger_version=="" or ranger_version=="ranger-admin - None":
			script_path = os.path.join(RANGER_ADMIN_HOME,"ews","ranger-admin-services.sh")
			ranger_version=check_output(script_path +" version").split("\n")[1]
	except Exception as error:
		ranger_version=''

	try:
		if ranger_version=="" or ranger_version=="ranger-admin - None":
			ranger_version=check_output("ranger-admin version").split("\n")[1]
	except Exception as error:
		ranger_version=''

	if ranger_version=="" or ranger_version is None:
		log("[E] Unable to find ranger version details, Exiting..", "error")
		sys.exit(1)

	XA_DB_FLAVOR=globalDict['DB_FLAVOR']
	XA_DB_FLAVOR = XA_DB_FLAVOR.upper()

	log("[I] DB FLAVOR :" + XA_DB_FLAVOR ,"info")
	xa_db_host = globalDict['db_host']

	mysql_dbversion_catalog = os.path.join('db','mysql','create_dbversion_catalog.sql')
	mysql_core_file = globalDict['mysql_core_file']
	mysql_patches = os.path.join('db','mysql','patches')

	oracle_dbversion_catalog = os.path.join('db','oracle','create_dbversion_catalog.sql')
	oracle_core_file = globalDict['oracle_core_file']
	oracle_patches = os.path.join('db','oracle','patches')

	postgres_dbversion_catalog = os.path.join('db','postgres','create_dbversion_catalog.sql')
	postgres_core_file = globalDict['postgres_core_file']
	postgres_patches = os.path.join('db','postgres','patches')

	sqlserver_dbversion_catalog = os.path.join('db','sqlserver','create_dbversion_catalog.sql')
	sqlserver_core_file = globalDict['sqlserver_core_file']
	sqlserver_patches = os.path.join('db','sqlserver','patches')

	sqlanywhere_dbversion_catalog = os.path.join('db','sqlanywhere','create_dbversion_catalog.sql')
	sqlanywhere_core_file = globalDict['sqlanywhere_core_file']
	sqlanywhere_patches = os.path.join('db','sqlanywhere','patches')

	db_name = globalDict['db_name']
	db_user = globalDict['db_user']
	db_password = globalDict['db_password']

	x_db_version = 'x_db_version_h'
	xa_access_audit = 'xa_access_audit'

	audit_store = None
	if 'audit_store' in globalDict:
		audit_store = globalDict['audit_store']
		audit_store=audit_store.lower()

	db_ssl_enabled='false'
	db_ssl_required='false'
	db_ssl_verifyServerCertificate='false'
	db_ssl_auth_type='2-way'
	javax_net_ssl_keyStore=''
	javax_net_ssl_keyStorePassword=''
	javax_net_ssl_trustStore=''
	javax_net_ssl_trustStorePassword=''
	db_ssl_certificate_file=''
	javax_net_ssl_trustStore_type='bcfks'
	javax_net_ssl_keyStore_type='bcfks'
	is_override_db_connection_string='false'
	db_override_jdbc_connection_string=''

	if XA_DB_FLAVOR == "MYSQL" or XA_DB_FLAVOR == "POSTGRES":
		if 'db_ssl_enabled' in globalDict:
			db_ssl_enabled=globalDict['db_ssl_enabled'].lower()
			if db_ssl_enabled == 'true':
				if 'db_ssl_required' in globalDict:
					db_ssl_required=globalDict['db_ssl_required'].lower()
				if 'db_ssl_verifyServerCertificate' in globalDict:
					db_ssl_verifyServerCertificate=globalDict['db_ssl_verifyServerCertificate'].lower()
				if 'db_ssl_auth_type' in globalDict:
					db_ssl_auth_type=globalDict['db_ssl_auth_type'].lower()
				if 'db_ssl_certificate_file' in globalDict:
					db_ssl_certificate_file=globalDict['db_ssl_certificate_file']
				if 'javax_net_ssl_trustStore' in globalDict:
					javax_net_ssl_trustStore=globalDict['javax_net_ssl_trustStore']
				if 'javax_net_ssl_trustStorePassword' in globalDict:
					javax_net_ssl_trustStorePassword=globalDict['javax_net_ssl_trustStorePassword']
				if 'javax_net_ssl_trustStore_type' in globalDict:
					javax_net_ssl_trustStore_type=globalDict['javax_net_ssl_trustStore_type']
				if db_ssl_verifyServerCertificate == 'true':
					if  db_ssl_certificate_file != "":
						if not os.path.exists(db_ssl_certificate_file):
							log("[E] Invalid file Name! Unable to find certificate file:"+db_ssl_certificate_file,"error")
							sys.exit(1)
					elif db_ssl_auth_type == '1-way' and db_ssl_certificate_file == "" :
						if not os.path.exists(javax_net_ssl_trustStore):
							log("[E] Invalid file Name! Unable to find truststore file:"+javax_net_ssl_trustStore,"error")
							sys.exit(1)
						if javax_net_ssl_trustStorePassword is None or javax_net_ssl_trustStorePassword =="":
							log("[E] Invalid ssl truststore password!","error")
							sys.exit(1)
					if db_ssl_auth_type == '2-way':
						if 'javax_net_ssl_keyStore' in globalDict:
							javax_net_ssl_keyStore=globalDict['javax_net_ssl_keyStore']
						if 'javax_net_ssl_keyStorePassword' in globalDict:
							javax_net_ssl_keyStorePassword=globalDict['javax_net_ssl_keyStorePassword']
						if 'javax_net_ssl_keyStore_type' in globalDict:
							javax_net_ssl_keyStore_type=globalDict['javax_net_ssl_keyStore_type']
						if not os.path.exists(javax_net_ssl_keyStore):
							log("[E] Invalid file Name! Unable to find keystore file:"+javax_net_ssl_keyStore,"error")
							sys.exit(1)
						if javax_net_ssl_keyStorePassword is None or javax_net_ssl_keyStorePassword =="":
							log("[E] Invalid ssl keystore password!","error")
							sys.exit(1)
	if 'is_override_db_connection_string' in globalDict:
		is_override_db_connection_string=globalDict['is_override_db_connection_string'].lower()
	if 'db_override_jdbc_connection_string' in globalDict:
		db_override_jdbc_connection_string=globalDict['db_override_jdbc_connection_string'].strip()


	if XA_DB_FLAVOR == "MYSQL":
		MYSQL_CONNECTOR_JAR=globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = MysqlConf(xa_db_host, MYSQL_CONNECTOR_JAR, JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,is_override_db_connection_string,db_override_jdbc_connection_string)
		xa_db_version_file = os.path.join(RANGER_ADMIN_HOME , mysql_dbversion_catalog)
		xa_db_core_file = os.path.join(RANGER_ADMIN_HOME , mysql_core_file)
		xa_patch_file = os.path.join(RANGER_ADMIN_HOME ,mysql_patches)
		first_table='x_portal_user'
		last_table='x_policy_ref_group'
		
	elif XA_DB_FLAVOR == "ORACLE":
		ORACLE_CONNECTOR_JAR=globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = OracleConf(xa_db_host, ORACLE_CONNECTOR_JAR, JAVA_BIN, is_override_db_connection_string, db_override_jdbc_connection_string)
		xa_db_version_file = os.path.join(RANGER_ADMIN_HOME ,oracle_dbversion_catalog)
		xa_db_core_file = os.path.join(RANGER_ADMIN_HOME ,oracle_core_file)
		xa_patch_file = os.path.join(RANGER_ADMIN_HOME ,oracle_patches)
		first_table='X_PORTAL_USER'
		last_table='X_POLICY_REF_GROUP'

	elif XA_DB_FLAVOR == "POSTGRES":
		POSTGRES_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = PostgresConf(xa_db_host, POSTGRES_CONNECTOR_JAR, JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,db_ssl_certificate_file,javax_net_ssl_trustStore_type,javax_net_ssl_keyStore_type,is_override_db_connection_string,db_override_jdbc_connection_string)
		xa_db_version_file = os.path.join(RANGER_ADMIN_HOME , postgres_dbversion_catalog)
		xa_db_core_file = os.path.join(RANGER_ADMIN_HOME , postgres_core_file)
		xa_patch_file = os.path.join(RANGER_ADMIN_HOME , postgres_patches)
		first_table='x_portal_user'
		last_table='x_policy_ref_group'

	elif XA_DB_FLAVOR == "MSSQL":
		SQLSERVER_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = SqlServerConf(xa_db_host, SQLSERVER_CONNECTOR_JAR, JAVA_BIN, is_override_db_connection_string, db_override_jdbc_connection_string)
		xa_db_version_file = os.path.join(RANGER_ADMIN_HOME ,sqlserver_dbversion_catalog)
		xa_db_core_file = os.path.join(RANGER_ADMIN_HOME , sqlserver_core_file)
		xa_patch_file = os.path.join(RANGER_ADMIN_HOME , sqlserver_patches)
		first_table='x_portal_user'
		last_table='x_policy_ref_group'

	elif XA_DB_FLAVOR == "SQLA":
		if not os_name == "WINDOWS" :
			if os.environ['LD_LIBRARY_PATH'] == "":
				log("[E] ---------- LD_LIBRARY_PATH environment property not defined, aborting installation. ----------", "error")
				sys.exit(1)
		SQLANYWHERE_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = SqlAnywhereConf(xa_db_host, SQLANYWHERE_CONNECTOR_JAR, JAVA_BIN, is_override_db_connection_string, db_override_jdbc_connection_string)
		xa_db_version_file = os.path.join(RANGER_ADMIN_HOME ,sqlanywhere_dbversion_catalog)
		xa_db_core_file = os.path.join(RANGER_ADMIN_HOME , sqlanywhere_core_file)
		xa_patch_file = os.path.join(RANGER_ADMIN_HOME , sqlanywhere_patches)
		first_table='x_portal_user'
		last_table='x_policy_ref_group'

	else:
		log("[E] --------- NO SUCH SUPPORTED DB FLAVOUR!! ---------", "error")
		sys.exit(1)

	log("[I] --------- Verifying Ranger DB connection ---------","info")
	xa_sqlObj.check_connection(db_name, db_user, db_password)

	if len(argv)==1:
		log("[I] --------- Verifying version history table ---------","info")
		output = xa_sqlObj.check_table(db_name, db_user, db_password, x_db_version)
		if output == False:
			xa_sqlObj.create_version_history_table(db_name, db_user, db_password, xa_db_version_file,x_db_version)

		log("[I] --------- Importing Ranger Core DB Schema ---------","info")
		xa_sqlObj.import_core_db_schema(db_name, db_user, db_password, xa_db_core_file,first_table,last_table)

		applyDBPatches=xa_sqlObj.hasPendingPatches(db_name, db_user, db_password, "DB_PATCHES")
		if applyDBPatches == True:
			log("[I] --------- Applying Ranger DB patches ---------","info")
			xa_sqlObj.apply_patches(db_name, db_user, db_password, xa_patch_file)
		else:
			log("[I] DB_PATCHES have already been applied","info")

	if len(argv)>1:
		for i in range(len(argv)):
			if str(argv[i]) == "-javapatch":
				applyJavaPatches=xa_sqlObj.hasPendingPatches(db_name, db_user, db_password, "JAVA_PATCHES")
				if applyJavaPatches == True:
					log("[I] ----------------- Applying java patches ------------", "info")
					my_dict = {}
					xa_sqlObj.execute_java_patches(xa_db_host, db_user, db_password, db_name, my_dict)
					xa_sqlObj.update_applied_patches_status(db_name,db_user, db_password,"JAVA_PATCHES")
				else:
					log("[I] JAVA_PATCHES have already been applied","info")
					if str(argv[i]) == "-checkupgrade":
						xa_sqlObj.is_new_install(xa_db_host, db_user, db_password, db_name)

			if str(argv[i]) == "-changepassword":
				rangerAdminConf="/etc/ranger/admin/conf"
				if os.path.exists(rangerAdminConf):
					RANGER_ADMIN_ENV_PATH = rangerAdminConf
				else:
					RANGER_ADMIN_ENV_PATH = RANGER_ADMIN_CONF
				log("[I] RANGER_ADMIN_ENV_PATH : "+RANGER_ADMIN_ENV_PATH,"info")
				if not os.path.exists(RANGER_ADMIN_ENV_PATH):
					log("[I] path  dose not exist" +RANGER_ADMIN_ENV_PATH,"info")
				else:
					env_file_path = RANGER_ADMIN_ENV_PATH + '/' + 'ranger-admin-env*.sh'
					log("[I] env_file_path : " +env_file_path,"info")
					run_env_file(env_file_path)

				if len(argv)>5:
					isValidPassWord = False
					for j in range(len(argv)):
						if str(argv[j]) == "-pair":
							userName=argv[j+1]
							oldPassword=argv[j+2]
							newPassword=argv[j+3]
							if oldPassword==newPassword:
								log("[E] Old Password and New Password argument are same. Exiting!!", "error")
								sys.exit(1)
							if userName != "" and oldPassword != "" and newPassword != "":
								password_validation(newPassword)
								isValidPassWord=True
					if isValidPassWord == True:
						xa_sqlObj.change_all_admin_default_password(xa_db_host, db_user, db_password, db_name,argv)

				elif len(argv)==5:
					userName=argv[2]
					oldPassword=argv[3]
					newPassword=argv[4]
					if oldPassword==newPassword:
						log("[E] Old Password and New Password argument are same. Exiting!!", "error")
						sys.exit(1)
					if userName != "" and oldPassword != "" and newPassword != "":
						password_validation(newPassword)
						xa_sqlObj.change_admin_default_password(xa_db_host, db_user, db_password, db_name,userName,oldPassword,newPassword)
				else:
					log("[E] Invalid argument list.", "error")
					log("[I] Usage : python db_setup.py -changepassword <loginID> <currentPassword> <newPassword>","info")
					sys.exit(1)

main(sys.argv)
