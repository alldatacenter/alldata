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
import platform
import logging
import subprocess
import fileinput
from os.path import basename
from subprocess import Popen,PIPE
from datetime import date
import datetime
from time import gmtime, strftime
globalDict = {}

os_name = platform.system()
os_name = os_name.upper()
is_unix = os_name == "LINUX" or os_name == "DARWIN"

jisql_debug=True

RANGER_KMS_HOME = os.getenv("RANGER_KMS_HOME")
if RANGER_KMS_HOME is None:
	RANGER_KMS_HOME = os.getcwd()

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
		read_config_file = open(os.path.join(RANGER_KMS_HOME,'install.properties'))
	elif os_name == "WINDOWS":
		read_config_file = open(os.path.join(RANGER_KMS_HOME,'bin','install_config.properties'))
	library_path = os.path.join(RANGER_KMS_HOME,"cred","lib","*")

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

def jisql_log(query, db_password):
	if jisql_debug == True:
		if os_name == "WINDOWS":
			query = query.replace(' -p "'+db_password+'"' , ' -p "********"')
			log("[JISQL] "+query, "info")
		else:
			query = query.replace(" -p '"+db_password+"'" , " -p '********'")
			log("[JISQL] "+query, "info")

class BaseDB(object):

	def check_connection(self, db_name, db_user, db_password):
		log("[I] ---------- Verifying DB connection ----------", "info")

	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		log("[I] ---------- Verifying table ----------", "info")

	def import_db_file(self, db_name, db_user, db_password, file_name):
		log("[I] ---------- Importing db schema ----------", "info")


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
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password ,db_name):
		path = RANGER_KMS_HOME
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
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver mysqlconj -cstring jdbc:mysql://%s/%s%s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN,db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.host, db_name,db_ssl_param,user, password)
		return jisql_cmd

	def check_connection(self, db_name, db_user, db_password):
		log("[I] Checking connection..", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -query \"SELECT version();\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT version();\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip('Production  |'):
			log("[I] Checking connection passed.", "info")
			return True
		else:
			log("[E] Can't establish connection!! Exiting.." ,"error")
			log("[I] Please run DB setup first or contact Administrator.." ,"info")
			sys.exit(1)


	def import_db_file(self, db_name, db_user, db_password, file_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing db schema to database " + db_name + " from file: " + name,"info")
			get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
			if is_unix:
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -input %s -c ;" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(query)
			if ret == 0:
				log("[I] "+name + " DB schema imported successfully","info")
			else:
				log("[E] "+name + " DB schema import failed!","error")
				sys.exit(1)
		else:
			log("[E] DB schema file " + name+ " not found","error")
			sys.exit(1)


	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -query \"show tables like '%s';\"" %(TABLE_NAME)
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"show tables like '%s';\" -c ;" %(TABLE_NAME)
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip(TABLE_NAME + " |"):
			log("[I] Table " + TABLE_NAME +" already exists in database '" + db_name + "'","info")
			return True
		else:
			log("[I] Table " + TABLE_NAME +" does not exist in database " + db_name + "","info")
			return False


class OracleConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host 
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password):
		path = RANGER_KMS_HOME
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
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u '%s' -p '%s' -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR,path, self.db_override_jdbc_connection_string, user, password)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u '%s' -p '%s' -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR,path, cstring, user, password)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string, user, password)
			else:
				jisql_cmd = "%s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver oraclethin -cstring %s -u \"%s\" -p \"%s\" -noheader -trim" %(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, cstring, user, password)
		return jisql_cmd

	def check_connection(self, db_name, db_user, db_password):
		log("[I] Checking connection", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password)
		if is_unix:
			query = get_cmd + " -c \; -query \"select * from v$version;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"select * from v$version;\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip('Production  |'):
			log("[I] Connection success", "info")
			return True
		else:
			log("[E] Can't establish connection!", "error")
			sys.exit(1)


	def import_db_file(self, db_name, db_user, db_password, file_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing script " + db_name + " from file: " + name,"info")
			get_cmd = self.get_jisql_cmd(db_user, db_password)
			if is_unix:
				query = get_cmd + " -input %s -c \;" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -input %s -c ;" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(query)
			if ret == 0:
				log("[I] "+name + " imported successfully","info")
			else:
				log("[E] "+name + " import failed!","error")
				sys.exit(1)
		else:
			log("[E] Import " +name + " sql file not found","error")
			sys.exit(1)


	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		get_cmd = self.get_jisql_cmd(db_user ,db_password)
		if is_unix:
			query = get_cmd + " -c \; -query 'select default_tablespace from user_users;'"
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"select default_tablespace from user_users;\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query).strip()
		output = output.strip(' |')
		db_name = db_name.upper()
		if ((output == db_name) or (db_name =='' and output is not None and output != '')):
			log("[I] User name " + db_user + " and tablespace " + output + " already exists.","info")
			log("[I] Verifying table " + TABLE_NAME +" in tablespace " + output, "info")
			get_cmd = self.get_jisql_cmd(db_user, db_password)
			if is_unix:
				query = get_cmd + " -c \; -query \"select UPPER(table_name) from all_tables where UPPER(tablespace_name)=UPPER('%s') and UPPER(table_name)=UPPER('%s');\"" %(output ,TABLE_NAME)
			elif os_name == "WINDOWS":
				query = get_cmd + " -query \"select UPPER(table_name) from all_tables where UPPER(tablespace_name)=UPPER('%s') and UPPER(table_name)=UPPER('%s');\" -c ;" %(output ,TABLE_NAME)
			jisql_log(query, db_password)
			output = check_output(query)
			if output.strip(TABLE_NAME.upper() + ' |'):
				log("[I] Table " + TABLE_NAME +" already exists in tablespace " + output + "","info")
				return True
			else:
				log("[I] Table " + TABLE_NAME +" does not exist in tablespace " + output + "","info")
				return False
		else:
			log("[E] "+db_user + " user already assigned to some other tablespace , provide different DB name.","error")
			sys.exit(1)



class PostgresConf(BaseDB):
	# Constructor
	def __init__(self, host,SQL_CONNECTOR_JAR,JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,db_ssl_certificate_file,javax_net_ssl_trustStore_type,javax_net_ssl_keyStore_type,is_db_override_jdbc_connection_string,db_override_jdbc_connection_string):
		self.host = host
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
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		#TODO: User array for forming command
		path = RANGER_KMS_HOME
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
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver postgresql -cstring %s -u %s -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path, self.db_override_jdbc_connection_string,user, password)
			else:
				jisql_cmd = "%s %s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -driver postgresql -cstring jdbc:postgresql://%s/%s%s -u %s -p '%s' -noheader -trim -c \;" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR,path, self.host, db_name, db_ssl_param,user, password)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver postgresql -cstring %s -u %s -p \"%s\" -noheader -trim" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.db_override_jdbc_connection_string,user, password)
			else:
				jisql_cmd = "%s %s -cp %s;%s\jisql\\lib\\* org.apache.util.sql.Jisql -driver postgresql -cstring jdbc:postgresql://%s/%s%s -u %s -p \"%s\" -noheader -trim" %(self.JAVA_BIN, db_ssl_cert_param,self.SQL_CONNECTOR_JAR, path, self.host, db_name, db_ssl_param,user, password)
		return jisql_cmd

	def check_connection(self, db_name, db_user, db_password):
		log("[I] Checking connection", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -query \"SELECT 1;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT 1;\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip('1 |'):
			log("[I] connection success", "info")
			return True
		else:
			log("[E] Can't establish connection", "error")
			sys.exit(1)

	def import_db_file(self, db_name, db_user, db_password, file_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing db schema to database " + db_name + " from file: " + name,"info")
			get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
			if is_unix:
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -input %s -c ;" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(query)
			if ret == 0:
				log("[I] "+name + " DB schema imported successfully","info")
			else:
				log("[E] "+name + " DB schema import failed!","error")
				sys.exit(1)
		else:
			log("[E] DB schema file " + name+ " not found","error")
			sys.exit(1)


	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		log("[I] Verifying table " + TABLE_NAME +" in database " + db_name, "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -query \"select * from (select table_name from information_schema.tables where table_catalog='%s' and table_name = '%s') as temp;\"" %(db_name , TABLE_NAME)
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"select * from (select table_name from information_schema.tables where table_catalog='%s' and table_name = '%s') as temp;\" -c ;" %(db_name , TABLE_NAME)
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip(TABLE_NAME +" |"):
			log("[I] Table " + TABLE_NAME +" already exists in database " + db_name, "info")
			return True
		else:
			log("[I] Table " + TABLE_NAME +" does not exist in database " + db_name, "info")
			return False


class SqlServerConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		#TODO: User array for forming command
		path = RANGER_KMS_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver mssql -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR,path, user, password, self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver mssql -cstring jdbc:sqlserver://%s\\;databaseName=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR,path, user, password, self.host,db_name)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver mssql -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver mssql -cstring jdbc:sqlserver://%s;databaseName=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password, self.host,db_name)
		return jisql_cmd

	def check_connection(self, db_name, db_user, db_password):
		log("[I] Checking connection", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -c \; -query \"SELECT 1;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT 1;\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip('1 |'):
			log("[I] Connection success", "info")
			return True
		else:
			log("[E] Can't establish connection", "error")
			sys.exit(1)

	def import_db_file(self, db_name, db_user, db_password, file_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing db schema to database " + db_name + " from file: " + name,"info")
			get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
			if is_unix:
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(query)
			if ret == 0:
				log("[I] "+name + " DB schema imported successfully","info")
			else:
				log("[E] "+name + " DB Schema import failed!","error")
				sys.exit(1)
		else:
			log("[I] DB Schema file " + name+ " not found","error")
			sys.exit(1)

	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -c \; -query \"SELECT TABLE_NAME FROM information_schema.tables where table_name = '%s';\"" %(TABLE_NAME)
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT TABLE_NAME FROM information_schema.tables where table_name = '%s';\" -c ;" %(TABLE_NAME)
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip(TABLE_NAME + " |"):
			log("[I] Table '" + TABLE_NAME + "' already exists in  database '" + db_name + "'","info")
			return True
		else:
			log("[I] Table '" + TABLE_NAME + "' does not exist in database '" + db_name + "'","info")
			return False

class SqlAnywhereConf(BaseDB):
	# Constructor
	def __init__(self, host, SQL_CONNECTOR_JAR, JAVA_BIN, is_db_override_jdbc_connection_string, db_override_jdbc_connection_string):
		self.host = host
		self.SQL_CONNECTOR_JAR = SQL_CONNECTOR_JAR
		self.JAVA_BIN = JAVA_BIN
		self.is_db_override_jdbc_connection_string = is_db_override_jdbc_connection_string
		self.db_override_jdbc_connection_string = db_override_jdbc_connection_string

	def get_jisql_cmd(self, user, password, db_name):
		path = RANGER_KMS_HOME
		self.JAVA_BIN = self.JAVA_BIN.strip("'")
		if is_unix:
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver sapsajdbc4 -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path,user, password,self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s:%s/jisql/lib/* org.apache.util.sql.Jisql -user %s -p '%s' -driver sapsajdbc4 -cstring jdbc:sqlanywhere:database=%s;host=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path,user, password,db_name,self.host)
		elif os_name == "WINDOWS":
			if self.is_db_override_jdbc_connection_string == 'true' and self.db_override_jdbc_connection_string is not None and len(self.db_override_jdbc_connection_string) > 0:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver sapsajdbc4 -cstring %s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password,self.db_override_jdbc_connection_string)
			else:
				jisql_cmd = "%s -cp %s;%s\\jisql\\lib\\* org.apache.util.sql.Jisql -user %s -p \"%s\" -driver sapsajdbc4 -cstring jdbc:sqlanywhere:database=%s;host=%s -noheader -trim"%(self.JAVA_BIN, self.SQL_CONNECTOR_JAR, path, user, password,db_name,self.host)
		return jisql_cmd

	def check_connection(self, db_name, db_user, db_password):
		log("[I] Checking connection", "info")
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -c \; -query \"SELECT 1;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT 1;\" -c ;"
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip('1 |'):
			log("[I] Connection success", "info")
			return True
		else:
			log("[E] Can't establish connection", "error")
			sys.exit(1)

	def import_db_file(self, db_name, db_user, db_password, file_name):
		name = basename(file_name)
		if os.path.isfile(file_name):
			log("[I] Importing db schema to database " + db_name + " from file: " + name,"info")
			get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
			if is_unix:
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(shlex.split(query))
			elif os_name == "WINDOWS":
				query = get_cmd + " -input %s" %file_name
				jisql_log(query, db_password)
				ret = subprocess.call(query)
			if ret == 0:
				log("[I] "+name + " DB schema imported successfully","info")
			else:
				log("[E] "+name + " DB Schema import failed!","error")
				sys.exit(1)
		else:
			log("[I] DB Schema file " + name+ " not found","error")
			sys.exit(1)

	def check_table(self, db_name, db_user, db_password, TABLE_NAME):
		self.set_options(db_name, db_user, db_password, TABLE_NAME)
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -c \; -query \"SELECT name FROM sysobjects where name = '%s' and type='U';\"" %(TABLE_NAME)
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"SELECT name FROM sysobjects where name = '%s' and type='U';\" -c ;" %(TABLE_NAME)
		jisql_log(query, db_password)
		output = check_output(query)
		if output.strip(TABLE_NAME + " |"):
			log("[I] Table '" + TABLE_NAME + "' already exists in  database '" + db_name + "'","info")
			return True
		else:
			log("[I] Table '" + TABLE_NAME + "' does not exist in database '" + db_name + "'","info")
			return False

	def set_options(self, db_name, db_user, db_password, TABLE_NAME):
		get_cmd = self.get_jisql_cmd(db_user, db_password, db_name)
		if is_unix:
			query = get_cmd + " -c \; -query \"set option public.reserved_keywords='LIMIT';\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.reserved_keywords='LIMIT';\" -c ;"
		jisql_log(query, db_password)
		ret = subprocess.call(shlex.split(query))
		if is_unix:
			query = get_cmd + " -c \; -query \"set option public.max_statement_count=0;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.max_statement_count=0;\" -c;"
		jisql_log(query, db_password)
		ret = subprocess.call(shlex.split(query))
		if is_unix:
			query = get_cmd + " -c \; -query \"set option public.max_cursor_count=0;\""
		elif os_name == "WINDOWS":
			query = get_cmd + " -query \"set option public.max_cursor_count=0;\" -c;"
		jisql_log(query, db_password)
		ret = subprocess.call(shlex.split(query))

def main(argv):
	populate_global_dict()

	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)

	if os.environ['JAVA_HOME'] == "":
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
	XA_DB_FLAVOR = globalDict['DB_FLAVOR']
	XA_DB_FLAVOR = XA_DB_FLAVOR.upper()

	log("[I] DB FLAVOR :" + XA_DB_FLAVOR ,"info")
	xa_db_host = globalDict['db_host']

	mysql_core_file = globalDict['mysql_core_file']
	mysql_patches = os.path.join('db','mysql','patches')

	oracle_core_file = globalDict['oracle_core_file'] 
	oracle_patches = os.path.join('db','oracle','patches')

	postgres_core_file = globalDict['postgres_core_file']
	postgres_patches = os.path.join('db','postgres','patches')

	sqlserver_core_file = globalDict['sqlserver_core_file']
	sqlserver_patches = os.path.join('db','sqlserver','patches')

	sqlanywhere_core_file = globalDict['sqlanywhere_core_file']
	sqlanywhere_patches = os.path.join('db','sqlanywhere','patches')

	db_name = globalDict['db_name']
	db_user = globalDict['db_user']
	db_password = globalDict['db_password']

	x_db_version = 'x_db_version_h'
	x_user = 'ranger_masterkey'

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
		xa_db_core_file = os.path.join(RANGER_KMS_HOME , mysql_core_file)
		
	elif XA_DB_FLAVOR == "ORACLE":
		ORACLE_CONNECTOR_JAR=globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = OracleConf(xa_db_host, ORACLE_CONNECTOR_JAR, JAVA_BIN)
		xa_db_core_file = os.path.join(RANGER_KMS_HOME ,oracle_core_file)

	elif XA_DB_FLAVOR == "POSTGRES":
		POSTGRES_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = PostgresConf(xa_db_host, POSTGRES_CONNECTOR_JAR, JAVA_BIN,db_ssl_enabled,db_ssl_required,db_ssl_verifyServerCertificate,javax_net_ssl_keyStore,javax_net_ssl_keyStorePassword,javax_net_ssl_trustStore,javax_net_ssl_trustStorePassword,db_ssl_auth_type,db_ssl_certificate_file,javax_net_ssl_trustStore_type,javax_net_ssl_keyStore_type,is_override_db_connection_string,db_override_jdbc_connection_string)
		xa_db_core_file = os.path.join(RANGER_KMS_HOME , postgres_core_file)

	elif XA_DB_FLAVOR == "MSSQL":
		SQLSERVER_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = SqlServerConf(xa_db_host, SQLSERVER_CONNECTOR_JAR, JAVA_BIN,is_override_db_connection_string,db_override_jdbc_connection_string)
		xa_db_core_file = os.path.join(RANGER_KMS_HOME , sqlserver_core_file)

	elif XA_DB_FLAVOR == "SQLA":
		if not os_name == "WINDOWS" :
			if os.environ['LD_LIBRARY_PATH'] == "":
				log("[E] ---------- LD_LIBRARY_PATH environment property not defined, aborting installation. ----------", "error")
				sys.exit(1)
		SQLANYWHERE_CONNECTOR_JAR = globalDict['SQL_CONNECTOR_JAR']
		xa_sqlObj = SqlAnywhereConf(xa_db_host, SQLANYWHERE_CONNECTOR_JAR, JAVA_BIN,is_override_db_connection_string,db_override_jdbc_connection_string)
		xa_db_core_file = os.path.join(RANGER_KMS_HOME , sqlanywhere_core_file)

	else:
		log("[E] --------- NO SUCH SUPPORTED DB FLAVOUR!! ---------", "error")
		sys.exit(1)

	log("[I] --------- Verifying Ranger DB connection ---------","info")
	xa_sqlObj.check_connection(db_name, db_user, db_password)

	if len(argv)==1:

		log("[I] --------- Verifying Ranger DB tables ---------","info")
		if xa_sqlObj.check_table(db_name, db_user, db_password, x_user):
			pass
		else:
			log("[I] --------- Importing Ranger Core DB Schema ---------","info")
			xa_sqlObj.import_db_file(db_name, db_user, db_password, xa_db_core_file)


main(sys.argv)
