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

def populate_global_dict():
	global globalDict
	read_config_file = open(os.path.join(os.getcwd(),'install.properties'))
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
				jceks_file_path = os.path.join(os.getenv('RANGER_HOME'), 'jceks','ranger_db.jceks')
				statuscode,value = call_keystore(library_path,key,'',jceks_file_path,'get')
				if statuscode == 1:
					value = ''
			value = value.strip()
			globalDict[key] = value

def logFile(msg):
	if globalDict["dryMode"]==True:
		logFileName=globalDict["dryModeOutputFile"]
		if logFileName !="":
			if os.path.isfile(logFileName):
				if os.access(logFileName, os.W_OK):
					with open(logFileName, "a") as f:
						f.write(msg+"\n")
						f.close()
				else:
					log("[E] Unable to open file "+logFileName+" in write mode, Check file permissions.", "error")
					sys.exit()
			else:
				log("[E] "+ logFileName+" is Invalid input file name! Provide valid file path to write DBA scripts:", "error")
				sys.exit()
		else:
			log("[E] Invalid input! Provide file path to write DBA scripts:", "error")
			sys.exit()

class BaseDB(object):

	def check_connection(self, db_name, db_user, db_password):
		log("[I] ---------- Verifying DB connection ----------", "info")

	def auditdb_operation(self, xa_db_host, audit_db_host, db_name, audit_db_name, xa_db_root_user, audit_db_root_user, db_user, audit_db_user, xa_db_root_password, audit_db_root_password, db_password, audit_db_password, DBA_MODE, dryMode):
		log("[I] ---------- set audit user permissions ----------", "info")


class MysqlConf(BaseDB):
	def __init__(self, host,SQL_CONNECTOR_JAR,JAVA_BIN):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN

	def get_jisql_cmd(self, user, password ,db_name):
		path = os.getcwd()
		if os_name == "LINUX":
			jisql_cmd = "%s -cp %s:jisql/lib/* org.apache.util.sql.Jisql -driver mysqlconj -cstring jdbc:mysql://%s/%s -u %s -p %s -noheader -trim -c \;" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, self.host, db_name, user, password)
		elif os_name == "WINDOWS":
			self.JAVA_BIN = self.JAVA_BIN.strip("'")
			jisql_cmd = "%s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver mysqlconj -cstring jdbc:mysql://%s/%s -u %s -p %s -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, self.host, db_name, user, password)
		return jisql_cmd
		
	def verify_user(self, root_user, db_root_password, host, db_user, get_cmd,dryMode):
		if dryMode == False:
			log("[I] Verifying user " + db_user+ " for Host "+ host, "info")
		if os_name == "LINUX":
			query = get_cmd + " -query \"select user from mysql.user where user='%s' and host='%s';\"" %(db_user,host)
		elif os_name == "WINDOWS":	
			query = get_cmd + " -query \"select user from mysql.user where user='%s' and host='%s';\" -c ;" %(db_user,host)
		output = check_output(query)
		if output.strip(db_user + " |"):
			return True
		else:
			return False

	def check_connection(self, db_name, db_user, db_password):
		#log("[I] Checking connection..", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if os_name == "LINUX":
			query = get_cmd + " -query \"SELECT version();\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT version();\" -c ;"
		output = check_output(query)
		if output.strip('Production  |'):
			#log("[I] Checking connection passed.", "info")
			return True
		else:
			log("[E] Can't establish db connection.. Exiting.." ,"error")
			sys.exit(1)

	def verify_db(self, root_user, db_root_password, db_name,dryMode):
		if dryMode == False:
			log("[I] Verifying database " + db_name , "info")
		get_cmd = self.get_jisql_cmd(root_user, db_root_password, 'mysql')
		if os_name == "LINUX":
			query = get_cmd + " -query \"show databases like '%s';\"" %(db_name)
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"show databases like '%s';\" -c ;" %(db_name)
		output = check_output(query)
		if output.strip(db_name + " |"):
			return True
		else:
			return False

	def revoke_permissions(self, root_user, db_name, db_user, db_password, db_root_password, is_revoke,dryMode):
		hosts_arr =["%", "localhost"]
		if is_revoke:
			for host in hosts_arr:
				if dryMode == False:
					log("[I] Revoking *.* privileges of user '"+db_user+"'@'"+host+"'" , "info")
					get_cmd = self.get_jisql_cmd(root_user, db_root_password, 'mysql')
					if os_name == "LINUX":	
						query = get_cmd + " -query \"REVOKE ALL PRIVILEGES,GRANT OPTION FROM '%s'@'%s';\"" %(db_user, host)
						ret = subprocess.call(shlex.split(query))
					elif os_name == "WINDOWS":
						query = get_cmd + " -query \"REVOKE ALL PRIVILEGES,GRANT OPTION FROM '%s'@'%s';\" -c ;" %(db_user, host)
						ret = subprocess.call(query)
					if ret == 0:
						if os_name == "LINUX":	
							query = get_cmd + " -query \"FLUSH PRIVILEGES;\""
							ret = subprocess.call(shlex.split(query))
						elif os_name == "WINDOWS":
							query = get_cmd + " -query \"FLUSH PRIVILEGES;\" -c ;"
							ret = subprocess.call(query)
						if ret != 0:
							sys.exit(1)
					else:
						sys.exit(1)
				else:
					logFile("REVOKE ALL PRIVILEGES,GRANT OPTION FROM '%s'@'%s';" %(db_user, host))
					logFile("FLUSH PRIVILEGES;")

	def grant_xa_db_user(self, root_user, db_name, db_user, db_password, db_root_password, is_revoke,dryMode):
		hosts_arr =["%", "localhost"]
		for host in hosts_arr:
			if dryMode == False:
				log("[I] Granting all privileges to user '"+db_user+"'@'"+host+"' on db '"+db_name+"'" , "info")
				get_cmd = self.get_jisql_cmd(root_user, db_root_password, 'mysql')
				if os_name == "LINUX":
					query = get_cmd + " -query \"grant all privileges on %s.* to '%s'@'%s' with grant option;\"" %(db_name,db_user, host)
					ret = subprocess.call(shlex.split(query))
				elif os_name == "WINDOWS":
					query = get_cmd + " -query \"grant all privileges on %s.* to '%s'@'%s' with grant option;\" -c ;" %(db_name,db_user, host)
					ret = subprocess.call(query)
				if ret == 0:
					log("[I] FLUSH PRIVILEGES.." , "info")
					if os_name == "LINUX":
						query = get_cmd + " -query \"FLUSH PRIVILEGES;\""
						ret = subprocess.call(shlex.split(query))
					elif os_name == "WINDOWS":
						query = get_cmd + " -query \"FLUSH PRIVILEGES;\" -c ;"
						ret = subprocess.call(query)
					if ret == 0:
						log("[I] Privileges granted to '" + db_user + "'@'"+host+"' on '"+db_name+"'", "info")
					else:
						log("[E] Granting privileges to '" +db_user+ "'@'"+host+"' failed on '"+db_name+"'", "error")
						sys.exit(1)
				else:
					log("[E] Granting privileges to '" +db_user+ "'@'"+host+"' failed on '"+db_name+"'", "error")
					sys.exit(1)
			else:
				logFile("grant all privileges on %s.* to '%s'@'%s' with grant option;" %(db_name,db_user, host))
				logFile("FLUSH PRIVILEGES;")

	def grant_audit_db_user(self, db_user, audit_db_name, audit_db_user, audit_db_password, db_password,TABLE_NAME,dryMode):
		hosts_arr =["%", "localhost"]
		for host in hosts_arr:
			if dryMode == False:
				log("[I] Granting insert privileges to '"+ audit_db_user + "'@'"+host+"' on table '"+ audit_db_name+"."+TABLE_NAME+"'" , "info")
				get_cmd = self.get_jisql_cmd(db_user, db_password, audit_db_name)
				if os_name == "LINUX":
					query = get_cmd + " -query \"GRANT INSERT ON %s.%s to '%s'@'%s';\"" %(audit_db_name,TABLE_NAME,audit_db_user,host)
					ret = subprocess.call(shlex.split(query))
				if os_name == "WINDOWS":
					query = get_cmd + " -query \"GRANT INSERT ON %s.%s to '%s'@'%s';\"" %(audit_db_name,TABLE_NAME,audit_db_user,host)
					ret = subprocess.call(query)
				if ret == 0:
					log("[I] Insert Privileges granted to '" + audit_db_user+ "'@'"+host+"' on table '"+ audit_db_name+"."+TABLE_NAME+"'", "info")
				else:
					log("[E] Granting insert privileges to '" +audit_db_user+ "'@'"+host+"' failed on table '"+ audit_db_name+"."+TABLE_NAME+"'", "error")
					sys.exit(1)
			else:
				logFile("GRANT INSERT ON %s.%s to '%s'@'%s';" %(audit_db_name,TABLE_NAME,audit_db_user,host))
				logFile("FLUSH PRIVILEGES;")

	def auditdb_operation(self, xa_db_host, audit_db_host, db_name, audit_db_name, xa_db_root_user, audit_db_root_user, db_user, audit_db_user, xa_db_root_password, audit_db_root_password, db_password, audit_db_password, DBA_MODE,dryMode):
		is_revoke=True
		if db_user != audit_db_user:
			if dryMode == False:
				log("[I] ---------- Revoking permissions from Ranger Audit db user ----------","info")
			self.revoke_permissions(audit_db_root_user, audit_db_name, audit_db_user, audit_db_password, audit_db_root_password, is_revoke,dryMode)
			if dryMode == False:
				log("[I] ---------- Granting permissions to Ranger Admin db user on Audit DB ----------","info")
			self.grant_xa_db_user(audit_db_root_user, audit_db_name, db_user, db_password, audit_db_root_password, is_revoke,dryMode)
		self.grant_audit_db_user(db_user, audit_db_name, audit_db_user, audit_db_password, db_password,'xa_access_audit',dryMode)


def main(argv):

	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)
	DBA_MODE = 'TRUE'

	quiteMode = False
	dryMode=False
	is_revoke=True

	if len(argv) > 1:
		for i in range(len(argv)):
			if str(argv[i]) == "-q":
				quiteMode = True
				populate_global_dict()
			if str(argv[i]) == "-d":
				index=i+1
				try:
					dba_sql_file=str(argv[index])
					if dba_sql_file == "":
						log("[E] Invalid input! Provide file path to write Grant/Revoke sql scripts:","error")
						sys.exit(1)
				except IndexError:
					log("[E] Invalid input! Provide file path to write Grant/Revoke sql scripts:","error")
					sys.exit(1)

				if not dba_sql_file == "":
					if not os.path.exists(dba_sql_file):
						log("[I] Creating File:"+dba_sql_file,"info")
						open(dba_sql_file, 'w').close()
					else:
						log("[I] File "+dba_sql_file+ " is available.","info")

					if os.path.isfile(dba_sql_file):
						dryMode=True
						globalDict["dryMode"]=True
						globalDict["dryModeOutputFile"]=dba_sql_file
					else:
						log("[E] Invalid file Name! Unable to find file:"+dba_sql_file,"error")
						sys.exit(1)

	log("[I] Running Grant/Revoke sql script. QuiteMode:" + str(quiteMode),"info")
	if (quiteMode):
		JAVA_BIN=globalDict['JAVA_BIN']
	else:
		if os.environ['JAVA_HOME'] == "":
			log("[E] ---------- JAVA_HOME environment property not defined, aborting installation. ----------", "error")
			sys.exit(1)
		JAVA_BIN=os.path.join(os.environ['JAVA_HOME'],'bin','java')
		if os_name == "WINDOWS" :
			JAVA_BIN = JAVA_BIN+'.exe'
		if os.path.isfile(JAVA_BIN):
			pass
		else :
			while os.path.isfile(JAVA_BIN) == False:
				log("Enter java executable path: :","info")
				JAVA_BIN=input()
	log("[I] Using Java:" + str(JAVA_BIN),"info")

	if (quiteMode):
		XA_DB_FLAVOR=globalDict['DB_FLAVOR']
		AUDIT_DB_FLAVOR=globalDict['DB_FLAVOR']
	else:
		XA_DB_FLAVOR=''
		while XA_DB_FLAVOR == "":
			log("Enter db flavour{MYSQL} :","info")
			XA_DB_FLAVOR=input()
			AUDIT_DB_FLAVOR = XA_DB_FLAVOR
			XA_DB_FLAVOR = XA_DB_FLAVOR.upper()
			AUDIT_DB_FLAVOR = AUDIT_DB_FLAVOR.upper()

	log("[I] DB FLAVOR:" + str(XA_DB_FLAVOR),"info")

	if (quiteMode):
		CONNECTOR_JAR=globalDict['SQL_CONNECTOR_JAR']
	else:
		if XA_DB_FLAVOR == "MYSQL":
			log("Enter JDBC connector file for :"+XA_DB_FLAVOR,"info")
			CONNECTOR_JAR=input()
			while os.path.isfile(CONNECTOR_JAR) == False:
				log("JDBC connector file "+CONNECTOR_JAR+" does not exist, Please enter connector path :","error")
				CONNECTOR_JAR=input()
		else:
			log("[E] ---------- NO SUCH SUPPORTED DB FLAVOUR.. ----------", "error")
			sys.exit(1)

	if (quiteMode):
		xa_db_host = globalDict['db_host']
		audit_db_host = globalDict['db_host']
	else:
		xa_db_host=''
		while xa_db_host == "":
			log("Enter DB Host :","info")
			xa_db_host=input()
			audit_db_host=xa_db_host
	log("[I] DB Host:" + str(xa_db_host),"info")

	if (quiteMode):
		xa_db_root_user = globalDict['db_root_user']
		xa_db_root_password = globalDict['db_root_password']
	else:
		xa_db_root_user=''
		while xa_db_root_user == "":
			log("Enter db root user:","info")
			xa_db_root_user=input()
			log("Enter db root password:","info")
			xa_db_root_password = getpass.getpass("Enter db root password:")

	if (quiteMode):
		db_name = globalDict['db_name']
	else:
		db_name = ''
		while db_name == "":
			log("Enter DB Name :","info")
			db_name=input()

	if (quiteMode):
		db_user = globalDict['db_user']
	else:
		db_user=''
		while db_user == "":
			log("Enter db user name:","info")
			db_user=input()

	if (quiteMode):
		db_password = globalDict['db_password']
	else:
		db_password=''
		while db_password == "":
			log("Enter db user password:","info")
			db_password = getpass.getpass("Enter db user password:")

	if (quiteMode):
		audit_db_name = globalDict['audit_db_name']
	else:
		audit_db_name=''
		while audit_db_name == "":
			log("Enter audit db name:","info")
			audit_db_name = input()

	if (quiteMode):
		audit_db_user = globalDict['audit_db_user']
	else:
		audit_db_user=''
		while audit_db_user == "":
			log("Enter audit user name:","info")
			audit_db_user = input()

	if (quiteMode):
		audit_db_password = globalDict['audit_db_password']
	else:
		audit_db_password=''
		while audit_db_password == "":
			log("Enter audit db user password:","info")
			audit_db_password = getpass.getpass("Enter audit db user password:")

	audit_db_root_user = xa_db_root_user
	audit_db_root_password = xa_db_root_password

	mysql_dbversion_catalog = os.path.join('db','mysql','create_dbversion_catalog.sql')
	mysql_core_file = os.path.join('db','mysql','xa_core_db.sql')
	mysql_audit_file = os.path.join('db','mysql','xa_audit_db.sql')
	mysql_patches = os.path.join('db','mysql','patches')

	x_db_version = 'x_db_version_h'
	xa_access_audit = 'xa_access_audit'
	x_user = 'x_portal_user'

	if XA_DB_FLAVOR == "MYSQL":
		MYSQL_CONNECTOR_JAR=CONNECTOR_JAR
		xa_sqlObj = MysqlConf(xa_db_host, MYSQL_CONNECTOR_JAR, JAVA_BIN)
		xa_db_version_file = os.path.join(os.getcwd(),mysql_dbversion_catalog)
		xa_db_core_file = os.path.join(os.getcwd(),mysql_core_file)
		xa_patch_file = os.path.join(os.getcwd(),mysql_patches)
	else:
		log("[E] ---------- NO SUCH SUPPORTED DB FLAVOUR.. ----------", "error")
		sys.exit(1)

	if AUDIT_DB_FLAVOR == "MYSQL":
		MYSQL_CONNECTOR_JAR=CONNECTOR_JAR
		audit_sqlObj = MysqlConf(audit_db_host,MYSQL_CONNECTOR_JAR,JAVA_BIN)
		audit_db_file = os.path.join(os.getcwd(),mysql_audit_file)
	else:
		log("[E] ---------- NO SUCH SUPPORTED DB FLAVOUR.. ----------", "error")
		sys.exit(1)
	# Methods Begin
	if DBA_MODE == "TRUE" :
		if (dryMode==True):
			log("[I] Dry run mode:"+str(dryMode),"info")
			log("[I] Logging Grant/Revoke sql script in file:"+str(globalDict["dryModeOutputFile"]),"info")
			now = datetime.now()
			logFile("=========="+now.strftime('%Y-%m-%d %H:%M:%S')+"==========\n")
			xa_sqlObj.revoke_permissions(xa_db_root_user, db_name, db_user, db_password, xa_db_root_password, is_revoke,dryMode)
			xa_sqlObj.grant_xa_db_user(xa_db_root_user, db_name, db_user, db_password, xa_db_root_password, is_revoke,dryMode)
			audit_sqlObj.auditdb_operation(xa_db_host, audit_db_host, db_name, audit_db_name, xa_db_root_user, audit_db_root_user, db_user, audit_db_user, xa_db_root_password, audit_db_root_password, db_password, audit_db_password, DBA_MODE,dryMode)
			logFile("========================================\n")
		if (dryMode==False):
			log("[I] ---------- Revoking permissions from Ranger Admin db user ----------","info")
			xa_sqlObj.revoke_permissions(xa_db_root_user, db_name, db_user, db_password, xa_db_root_password, is_revoke,dryMode)
			log("[I] ---------- Granting permissions to Ranger Admin db user ----------","info")
			xa_sqlObj.grant_xa_db_user(xa_db_root_user, db_name, db_user, db_password, xa_db_root_password, is_revoke,dryMode)
			log("[I] ---------- Starting Ranger Audit db user operations ---------- ","info")
			audit_sqlObj.auditdb_operation(xa_db_host, audit_db_host, db_name, audit_db_name, xa_db_root_user, audit_db_root_user, db_user, audit_db_user, xa_db_root_password, audit_db_root_password, db_password, audit_db_password, DBA_MODE,dryMode)
			log("[I] ---------- Ranger Policy Manager DB and User Creation Process Completed..  ---------- ","info")
main(sys.argv)
