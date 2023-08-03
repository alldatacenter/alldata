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
import errno
import logging
import zipfile
try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO
try:
    from ConfigParser import ConfigParser
except ImportError:
    from configparser import ConfigParser
import subprocess
import fileinput
import zipfile
import re
import shutil
from datetime import date
import getpass
import glob
import pprint
from subprocess import  Popen,PIPE

conf_dict={}


"""
################################################
            Helper routines
################################################
"""

def log(msg,type):
    if type == 'info':
        logging.info(" %s",msg)
    if type == 'error':
        logging.error(" %s",msg)
    if type == 'debug':
        logging.debug(" %s",msg)
    if type == 'warning':
        logging.warning(" %s",msg)
    if type == 'exception':
        logging.exception(" %s",msg)

def password_validation(password, userType):
	if password:
		if re.search("[\\\`'\"]",password):
			log("[E] "+userType+" user password contains one of the unsupported special characters like \" ' \ `","error")
			sys.exit(1)
		else:
			log("[I] "+userType+" user password validated","info")
	else:
		log("[E] Blank password is not allowed,please enter valid password.","error")
		sys.exit(1)

def resolve_sym_link(path):
    path = os.path.realpath(path)
    base_dir = os.path.dirname(os.path.dirname(path))
    return path, base_dir

#prog = ["mysql", "-u", "ve", "--execute", 'insert into foo values ("snargle", 2)']

def getstatusoutput(cmd):
    """Return (status, output) of executing cmd in a shell."""
    """This new implementation should work on all platforms."""
    """pipe = subprocess.Popen(cmd, stdout=subprocess.PIPE, stdin=subprocess.PIPE, shell=True, universal_newlines=True)
    "output, err = pipe.communicate()
    "sts = pipe.returncode
    """
    ret = subprocess.call(cmd, shell=True)
    print("------------------")
    print(" cmd: " + str(cmd))
    #print " output: " + output
    print(" ret: " + str(ret))
    print("------------------")
    return ret, ret
    #if sts is None:
    #    log("sts is None!!!! Manually setting to -1. PLEASE CHECK!!!!!!!!!!!!!!","info")
    #    sts = -1
    #return sts, output


def copy_files(source_dir,dest_dir):
    for dir_path, dir_names, file_names in os.walk(source_dir):
        for file_name in file_names:
            target_dir = dir_path.replace(source_dir, dest_dir, 1)
            if not os.path.exists(target_dir):
                os.mkdir(target_dir)
            src_file = os.path.join(dir_path, file_name)
            dest_file = os.path.join(target_dir, file_name)
            log("copying src: " + src_file + " dest: " + dest_file, "debug")
            shutil.copyfile(src_file, dest_file)



def ModConfig(File, Variable, Setting):
    """
    Modify Config file variable with new setting
    """
    VarFound = False
    AlreadySet = False
    V=str(Variable)
    S=str(Setting)
    # use quotes if setting has spaces #
    if ' ' in S:
        S = '"%s"' % S

    for line in fileinput.input(File, inplace = 1):
        # process lines that look like config settings #
        if not line.lstrip(' ').startswith('#') and '=' in line:
            _infile_var = str(line.split('=')[0].rstrip(' '))
            _infile_set = str(line.split('=')[1].lstrip(' ').rstrip())
            # only change the first matching occurrence #
            if VarFound == False and _infile_var.rstrip(' ') == V:
                VarFound = True
                # don't change it if it is already set #
                if _infile_set.lstrip(' ') == S:
                    AlreadySet = True
                else:
                    line = "%s = %s\n" % (V, S)

        sys.stdout.write(line)


    # Append the variable if it wasn't found #
    if not VarFound:
        log( "Variable '%s' not found.  Adding it to %s" % (V, File), "debug")
        with open(File, "a") as f:
            f.write("%s = %s\n" % (V, S))
    elif AlreadySet == True:
        log( "Variable '%s' unchanged" % (V) , "debug")
    else:
        log( "Variable '%s' modified to '%s'" % (V, S) , "debug")

    return


def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise

def get_java_env():
    JAVA_HOME = os.getenv('JAVA_HOME')
    if JAVA_HOME:
        return os.path.join(JAVA_HOME, 'bin', 'java')
    else:
        log('java and jar commands are not available. Please configure JAVA_HOME','exception')
        os.sys.exit(1)

def get_class_path(paths):
    separator = ';' if sys.platform == 'win32' else ':';
    return separator.join(paths)

def get_jdk_options():
    global conf_dict
    return [os.getenv('RANGER_PROPERTIES', ''),"-Dlogdir="+os.getenv("RANGER_LOG_DIR"),' -Dcatalina.base=' + conf_dict['EWS_ROOT'] ]


"""
################################################
            Ranger Policy Manager routines
################################################
"""

def get_ranger_classpath():
    global conf_dict
    EWS_ROOT = conf_dict['EWS_ROOT']
    WEBAPP_ROOT = conf_dict['WEBAPP_ROOT']

    cp = [ os.path.join(WEBAPP_ROOT, "WEB-INF", "classes", "conf"), os.path.join(EWS_ROOT,"lib","*"), EWS_ROOT, os.path.join(os.getenv('JAVA_HOME'), 'lib', '*')]
    class_path = get_class_path(cp)
    return class_path

def populate_config_dict_from_env():
    global config_dict
    conf_dict['RANGER_ADMIN_DB_HOST'] = os.getenv("RANGER_ADMIN_DB_HOST")
    conf_dict['RANGER_AUDIT_DB_HOST'] = os.getenv("RANGER_AUDIT_DB_HOST")
    #conf_dict['MYSQL_BIN'] = 'mysql.exe'       #os.getenv("MYSQL_BIN")
    conf_dict['RANGER_DB_FLAVOR'] = os.getenv("RANGER_DB_FLAVOR")
    conf_dict['RANGER_AUDIT_DB_FLAVOR'] = os.getenv("RANGER_DB_FLAVOR")
    conf_dict['RANGER_ADMIN_DB_USERNAME'] = os.getenv("RANGER_ADMIN_DB_USERNAME")
    conf_dict['RANGER_ADMIN_DB_PASSWORD'] = os.getenv("RANGER_ADMIN_DB_PASSWORD")
    conf_dict['RANGER_ADMIN_DB_NAME'] = os.getenv("RANGER_ADMIN_DB_DBNAME")
    conf_dict['RANGER_AUDIT_DB_USERNAME'] = os.getenv("RANGER_AUDIT_DB_USERNAME")
    conf_dict['RANGER_AUDIT_DB_PASSWORD'] = os.getenv("RANGER_AUDIT_DB_PASSWORD")
    conf_dict['RANGER_AUDIT_DB_NAME'] = os.getenv("RANGER_AUDIT_DB_DBNAME")
    conf_dict['db_root_user'] = os.getenv("RANGER_DB_ROOT_USER")
    conf_dict['db_root_password'] = os.getenv("RANGER_ADMIN_DB_ROOT_PASSWORD")
    conf_dict['RANGER_ADMIN_DB_ROOT_PASSWORD'] = os.getenv("RANGER_ADMIN_DB_ROOT_PASSWORD")
    conf_dict['RANGER_AUDIT_DB_ROOT_PASSWORD'] = os.getenv("RANGER_AUDIT_DB_ROOT_PASSWORD")
    conf_dict['RANGER_ADMIN_HOME'] = os.getenv("RANGER_ADMIN_HOME")
    conf_dict['RANGER_AUTHENTICATION_METHOD'] = os.getenv("RANGER_AUTHENTICATION_METHOD")
    # LDAP Settings
    conf_dict['RANGER_LDAP_URL'] = os.getenv("RANGER_LDAP_URL")
    conf_dict['RANGER_LDAP_USERDNPATTERN'] = os.getenv("RANGER_LDAP_USERDNPATTERN")
    conf_dict['RANGER_LDAP_GROUPSEARCHBASE'] = os.getenv("RANGER_LDAP_GROUPSEARCHBASE")
    conf_dict['RANGER_LDAP_GROUPSEARCHFILTER'] = os.getenv("RANGER_LDAP_GROUPSEARCHFILTER")
    conf_dict['RANGER_ldap_GROUPROLEATTRIBUTE'] = os.getenv("RANGER_ldap_GROUPROLEATTRIBUTE")

    # AD Settings
    conf_dict['RANGER_LDAP_AD_DOMAIN'] = os.getenv("RANGER_LDAP_AD_DOMAIN")
    conf_dict['RANGER_LDAP_AD_URL'] = os.getenv("RANGER_LDAP_AD_URL")

def populate_config_dict_from_file():
    global config_dict
    RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")
    read_config_file = open(os.path.join(RANGER_ADMIN_HOME,'bin','install_config.properties'))
    library_path = os.path.join(RANGER_ADMIN_HOME,"cred","lib","*")
    for each_line in read_config_file.read().split('\n') :
        if len(each_line) == 0 : continue
        # print 'each_line = ' + each_line
        key , value = each_line.strip().split("=",1)
        key = key.strip()
        if 'PASSWORD' in key:
            jceks_file_path = os.path.join(os.getenv('RANGER_HOME'), 'jceks','ranger_db.jceks')
            statuscode,value = call_keystore(library_path,key,'',jceks_file_path,'get')
            if statuscode == 1:
                value = ''
        value = value.strip()
        conf_dict[key] = value
    #if os.getenv("MYSQL_BIN") is not None:
    #    conf_dict['MYSQL_BIN'] = os.getenv("MYSQL_BIN")
    #else:
    #    os.sys.exit('Please set MYSQL_BIN variable in environment settings.')


def init_variables(switch):
    global conf_dict

    if switch == 'service' :
        populate_config_dict_from_env()
    else:
        populate_config_dict_from_file()
    #INSTALL_DIR = os.path.join(conf_dict['RANGER_ADMIN_HOME'] , "app")
    INSTALL_DIR = conf_dict['RANGER_ADMIN_HOME']
    EWS_ROOT    = os.path.join(INSTALL_DIR , "ews")
    WEBAPP_ROOT = os.path.join(INSTALL_DIR , "ews" , "webapp")

    #if switch == "service":
    #    war_file_path = os.path.join(conf_dict['RANGER_ADMIN_HOME'] , "war", "security-admin-web-*.war")
    #    war_file_list = glob.glob(war_file_path)
    #    conf_dict['war_file']   = war_file_list[0]

    conf_dict['EWS_ROOT']   = EWS_ROOT
    conf_dict['WEBAPP_ROOT']= WEBAPP_ROOT
    conf_dict['INSTALL_DIR']= INSTALL_DIR
    conf_dict['JAVA_BIN']= os.path.join(os.getenv("JAVA_HOME"),'bin','java.exe')
    conf_dict['DB_FLAVOR'] = os.getenv("RANGER_DB_FLAVOR")
    conf_dict['RANGER_DB_FLAVOR'] = os.getenv("RANGER_DB_FLAVOR")
    conf_dict['RANGER_AUDIT_DB_FLAVOR'] = os.getenv("RANGER_DB_FLAVOR")
    dir = os.path.join(os.getenv("RANGER_HOME"),"connector-jar")
    ews_dir = os.path.join(os.getenv("RANGER_ADMIN_HOME"),"ews","webapp","WEB-INF","lib")
    log(ews_dir,"info")
    if not os.path.exists(dir):
        os.makedirs(dir)
    layout_dir = os.path.dirname(os.getenv("HDP_LAYOUT"))
    files = os.listdir(layout_dir)
    if files:
        for filename in files:
            #log (filename,"info")
            if os.getenv("RANGER_DB_FLAVOR") == "MYSQL" or os.getenv("RANGER_AUDIT_DB_FLAVOR") == "MYSQL":
                f = re.match("^mysql-connector-java.*?.jar",filename)
            elif os.getenv("RANGER_DB_FLAVOR") == "ORACLE" or os.getenv("RANGER_AUDIT_DB_FLAVOR") == "ORACLE":    
                f = re.match("^ojdbc.*?.jar",filename)
            elif os.getenv("RANGER_DB_FLAVOR") == "POSTGRES" or os.getenv("RANGER_AUDIT_DB_FLAVOR") == "POSTGRES":    
                f = re.match("^postgresql-connector-jdbc.*?.jar",filename)    
            elif os.getenv("RANGER_DB_FLAVOR") == "MSSQL" or os.getenv("RANGER_AUDIT_DB_FLAVOR") == "MSSQL":
                f = re.match("^sqljdbc.*?.jar",filename)    
            if f:
                src = os.path.join(layout_dir,filename)
                shutil.copy2(src, dir)
                shutil.copy2(src, ews_dir)
                conf_dict['SQL_CONNECTOR_JAR'] = os.path.join(dir,filename)


    conf_dict['db_host']=os.getenv("RANGER_ADMIN_DB_HOST") + ":" + os.getenv("RANGER_ADMIN_DB_PORT")
    conf_dict['db_name']=os.getenv("RANGER_ADMIN_DB_DBNAME")
    conf_dict['db_user']=os.getenv("RANGER_ADMIN_DB_USERNAME")
    conf_dict['db_password']=os.getenv("RANGER_ADMIN_DB_PASSWORD")
    conf_dict['audit_db_name']=os.getenv("RANGER_AUDIT_DB_DBNAME")
    conf_dict['audit_db_user']=os.getenv("RANGER_AUDIT_DB_USERNAME")
    conf_dict['audit_db_password']=os.getenv("RANGER_AUDIT_DB_PASSWORD")
    conf_dict['RANGER_ADMIN_DB_PORT']=os.getenv("RANGER_ADMIN_DB_PORT")
    conf_dict['RANGER_AUDIT_DB_PORT']=os.getenv("RANGER_AUDIT_DB_PORT")
    db_dir = os.path.join(conf_dict['RANGER_ADMIN_HOME'] , "db")
    conf_dict['mysql_core_file']=os.path.join(db_dir,'mysql','xa_core_db.sql')
    conf_dict['mysql_audit_file']=os.path.join(db_dir,'mysql','xa_audit_db.sql')
    conf_dict['oracle_core_file']=os.path.join(db_dir,'oracle','xa_core_db_oracle.sql')
    conf_dict['oracle_audit_file']=os.path.join(db_dir,'oracle','xa_audit_db_oracle.sql')
    conf_dict['postgres_core_file']=os.path.join(db_dir,'postgres','xa_core_db_postgres.sql')
    conf_dict['postgres_audit_file']=os.path.join(db_dir,'postgres','xa_audit_db_postgres.sql')
    conf_dict['sqlserver_core_file']=os.path.join(db_dir,'sqlserver','xa_core_db_sqlserver.sql')
    conf_dict['sqlserver_audit_file']=os.path.join(db_dir,'sqlserver','xa_audit_db_sqlserver.sql')
    conf_dict['sqlanywhere_core_file']= os.path.join(db_dir,'sqlanywhere','xa_core_db_sqlanywhere.sql')
    conf_dict['sqlanywhere_audit_file']= os.path.join(db_dir, 'sqlanywhere','xa_audit_db_sqlanywhere.sql')
    #conf_dict['db_core_file']           = os.path.join(db_dir, "xa_core_db.sql")
    #conf_dict['db_create_user_file']    = os.path.join(db_dir, "create_dev_user.sql")
    #conf_dict['db_audit_file']          = os.path.join(db_dir, "xa_audit_db.sql")
    #conf_dict['db_asset_file']          = os.path.join(db_dir, "reset_asset.sql")

    #log("config is : " , "debug")
    #for x in conf_dict:
    #    log(x + " : " + conf_dict[x], "debug")

#TODO fix the base_dir part
def setup_install_files():
    global conf_dict

    EWS_ROOT = conf_dict['EWS_ROOT']
    INSTALL_DIR = conf_dict['INSTALL_DIR']
    WEBAPP_ROOT = conf_dict['WEBAPP_ROOT']

    EWS_LIB_DIR = os.path.join(EWS_ROOT,"lib")
    #EWS_LOG_DIR = os.path.join(EWS_ROOT,"logs")
    RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")

    log("Setting up installation files and directory", "debug")

    if not os.path.isdir(INSTALL_DIR):
        log("creating Install dir : " + INSTALL_DIR, "debug")
        os.makedirs(INSTALL_DIR)

    if not os.path.isdir(EWS_ROOT):
        log("creating EWS dir : " + EWS_ROOT, "debug")
        os.makedirs(EWS_ROOT)

    if not os.path.isdir(WEBAPP_ROOT):
        log("creating WEBAPP dir : " + WEBAPP_ROOT, "debug")
        os.makedirs(WEBAPP_ROOT)

    if not os.path.isdir(os.path.join(WEBAPP_ROOT,"WEB-INF","classes","conf")):
        log("creating conf dir : " + WEBAPP_ROOT, "debug")
        os.makedirs(os.path.join(WEBAPP_ROOT,"WEB-INF","classes","conf"))

    if not os.path.isdir(EWS_LIB_DIR):
        log("creating EWS_LIB_DIR dir : " + EWS_LIB_DIR, "debug")
        os.makedirs(EWS_LIB_DIR)

    #if not os.path.isdir(EWS_LOG_DIR):
    #    log("creating EWS_LOG_DIR dir : " + EWS_LOG_DIR, "debug")
    #    os.makedirs(EWS_LOG_DIR)

    #log("copying libraries ", "debug")
    #copy_files(os.path.join(RANGER_ADMIN_HOME,"ews","lib"), EWS_LIB_DIR)

    #log("copying xapolicymgr.properties file", "debug")
    #shutil.copyfile(os.path.join(RANGER_ADMIN_HOME,"ews","xapolicymgr.properties"), os.path.join(EWS_ROOT,"xapolicymgr.properties"))

    log("copying conf.dist/ to conf/", "debug")
    copy_files(os.path.join(WEBAPP_ROOT,"WEB-INF","classes","conf.dist"), os.path.join(WEBAPP_ROOT,"WEB-INF","classes","conf"))
    log(" Setting up installation files and directory DONE", "info");
pass


def write_config_to_file():
    global conf_dict
    RANGER_ADMIN_HOME = os.getenv("RANGER_ADMIN_HOME")
    library_path = os.path.join(RANGER_ADMIN_HOME, 'cred', 'lib','*')
    jceks_file_path = os.path.join(os.getenv("RANGER_HOME"), "jceks")
    if not os.path.isdir(jceks_file_path):
        mkdir_p(jceks_file_path)
    jceks_file_path = os.path.join(jceks_file_path,'ranger_db.jceks')

    file_path = os.path.dirname(os.path.realpath(__file__))
    write_conf_to_file = os.path.join(file_path, "install_config.properties")
    open(write_conf_to_file,'wb')
    for key,value in conf_dict.items():
        if 'PASSWORD' in key :
            #call_keystore(library_path,key,value,jceks_file_path,'create')
            value=''
        ModConfig(write_conf_to_file , key,value)


def init_logfiles():
    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level=logging.DEBUG)


def sanity_check_configure_files():
    global conf_dict

    log("Checking MYSQL executable and db files!!", 'debug')
    db_core_file = conf_dict['db_core_file']
    db_create_user_file = conf_dict['db_create_user_file']
    db_audit_file = conf_dict['db_audit_file']
    db_asset_file = conf_dict['db_asset_file']

    #if os.path.isfile(MYSQL_BIN):
    #    log("MYSQL Client bin : " + MYSQL_BIN + " file found", 'info')
    #else:
    #    os.sys.exit('MYSQL_BIN: ' + MYSQL_BIN + ' file does not exist')

    if os.path.isfile(db_core_file):
        log("DB core file " + db_core_file + " file found", 'info')
    else:
        log('db_core_file: ' + db_core_file + ' file does not exist','exception')
        os.sys.exit(1)

    if os.path.isfile(db_create_user_file):
        log("DB create user file " + db_create_user_file + " file found", 'info')
    else:
        log('db_create_user_file: ' + db_create_user_file + ' file does not exist','exception')
        os.sys.exit(1)

    if os.path.isfile(db_audit_file):
        log("DB audit file " + db_audit_file + " file found", 'info')
    else:
        log('db_audit_file: ' + db_audit_file + ' file does not exist','exception')
        os.sys.exit(1)

    if os.path.isfile(db_asset_file):
        log("DB asset file " + db_asset_file + " file found", 'info')
    else:
        log('db_asset_file: ' + db_asset_file + ' file does not exist','exception')
        os.sys.exit(1)

def get_mysql_cmd(user, password, host):
    global conf_dict
    MYSQL_BIN = conf_dict["MYSQL_BIN"]

    cmdArr = [ MYSQL_BIN ,'-B' ,'--user=%s' %user, '--password=%s' %password, '--host=%s' %host , '--skip-column-names']
    return cmdArr

def create_mysql_user(db_name, db_user, db_password, db_host, db_root_password):
	global conf_dict
	cmdArr = []

	MYSQL_BIN = conf_dict["MYSQL_BIN"]
	hosts_arr =["%", "localhost"]
	#check_mysql_password()
	### From properties file
	log("\nCreating MySQL user "+db_user+" (using root priviledges)\n", 'debug')
	for host in hosts_arr:
		cmdArr = get_mysql_cmd('root', db_root_password, db_host)
		#subprocess.call(["mysql", "-u", username, "-p%s" % password, "-e", "SELECT @@hostname"]

		cmdArr.extend(["-e", "select count(*) from mysql.user where user='%s' and host='%s'" %(db_user, host)])
		output = subprocess.check_output(cmdArr)
		if output.strip("\n\r") is "1":
			log( "\nMYSQL User already exists!\n", "debug")
		else:
			cmdArr = get_mysql_cmd('root', db_root_password, db_host)
			if db_password == "":
				#cmdStr = '"' + MYSQL_BIN + '"' + ' -B -u root --password='+db_root_password+' -h '+db_host+' -e \'create user "'+db_user+'"@"'+db_host+'";\''
				cmdArr.extend(["-e", "create user %s@%s" %(db_user, host)])
			else:
				cmdArr.extend(["-e", "create user '%s'@'%s' identified by '%s' " %(db_user, host, db_password)])
				#cmdStr = '"' + MYSQL_BIN + '"' + ' -B -u root --password='+db_root_password+' -h '+db_host+' -e \'create user "'+db_user+'"@"'+db_host+'" identified by "'+db_password+'";\''
				ret = subprocess.check_call(cmdArr)
			if ret == 0:
				#mysqlquery="GRANT ALL ON "+db_name+".* TO \'"+db_user+"'@'"+db_host+"' ;\
				#grant all privileges on "+db_name+".* to '"+db_user+"'@'"+db_host+"' with grant option;\
				#FLUSH PRIVILEGES;"
				cmdArr = get_mysql_cmd('root', db_root_password, db_host)
				cmdArr.extend(["-e", "GRANT ALL ON *.* TO '%s'@'%s'; grant all privileges on *.* to '%s'@'%s' with grant option; FLUSH PRIVILEGES" %(db_user,host,db_user,host)])
				ret = subprocess.check_call(cmdArr)
				if ret == 0:
					log("\nCreating MySQL user '" + db_user + "' (using root priviledges for % hosts ) DONE\n", "info")
				else:
					log("\nCreating MySQL user '" + db_user + "' (using root priviledges) FAILED\n", "info")
					sys.exit(1)

def check_mysql_password ():
    global conf_dict
    db_root_password = conf_dict["RANGER_ADMIN_DB_ROOT_PASSWORD"]
    MYSQL_HOST = conf_dict['MYSQL_HOST']
    MYSQL_BIN = conf_dict['MYSQL_BIN']

    log("Checking MYSQL root password : **** ","debug")

    cmdStr = "\""+MYSQL_BIN+"\""+" -u root --password="+db_root_password+" -h "+MYSQL_HOST+" -s -e \"select version();\""
    status, output = getstatusoutput(cmdStr)
    print("Status: " + str(status))
    print("output: " + str(output))

    if status == 0:
        log("Checking MYSQL root password DONE", "info")
    else:
        log("COMMAND: mysql -u root --password=..... -h " + MYSQL_HOST + " : FAILED with error message:\n*********************************\n" + output + "\n*********************************\n", "exception")
        sys.exit(1)


#def check_mysql_user_password():
#    global conf_dict
#    db_user = conf_dict["RANGER_ADMIN_DB_USERNAME"]
#    db_password = conf_dict["RANGER_ADMIN_DB_PASSWORD"]
#    db_root_password = conf_dict["RANGER_ADMIN_DB_ROOT_PASSWORD"]
#    MYSQL_HOST = conf_dict['MYSQL_HOST']
#
#    db = MySQLdb.connect(host=MYSQL_HOST, user=db_user, passwd=db_password)
#    if db:
#        log("Checking MYSQL "+ db_user +" password DONE", "info")
#    else:
#        log("COMMAND: mysql -u " + db_user + " --password=..... -h " + MYSQL_HOST + " : FAILED with error message:\n*********************************\n" + {msg} + "\n*********************************\n", "exception")
#
#def check_mysql_audit_user_password():
#    global conf_dict
#    audit_db = conf_dict["RANGER_AUDIT_DB_NAME"]
#    audit_db_user = conf_dict["RANGER_AUDIT_DB_USERNAME"]
#    audit_db_password = conf_dict["RANGER_AUDIT_DB_PASSWORD"]
#    MYSQL_HOST = conf_dict['MYSQL_HOST']
#
#    try:
#        db = MySQLdb.connect(host=MYSQL_HOST, user=audit_db_user, passwd=audit_db_password, db=audit_db)
#    except MySQLdb.Error, e:
#     exceptnMsg =  "Error %d: %s" % (e.args[0], e.args[1])
#     log("COMMAND: mysql -u " + audit_db_user + " --password=..... -h " + MYSQL_HOST + " : FAILED with error message:\n*********************************\n" + exceptnMsg + "\n*********************************\n", "exception")
#     sys.exit (1)
#    if db:
#        log("Checking Ranger Audit Table owner password DONE", "info")

#def exec_sql_file(cursor, sql_file):
#    log( "[INFO] Executing SQL script file: " + sql_file, "debug")
#    statement = ""
#    for line in open(sql_file):
#        if re.match(r'--', line):  # ignore sql comment lines
#            continue
#        if not re.search(r'[^-;]+;', line):  # keep appending lines that don't end in ';'
#            statement = statement + line
#        else:  # when you get a line ending in ';' then exec statement and reset for next statement
#            statement = statement + line
#            #print "\n\n[DEBUG] Executing SQL statement:\n%s" % (statement)
#            try:
#                cursor.execute(statement)
#                print cursor
#            except MySQLdb.Error, e:
#                log( "[WARN] MySQLError during execute statement \n\tArgs: " + str(e.args), "debug")
#            statement = ""

def upgrade_db():
    global config_dict

    db_user = conf_dict["RANGER_ADMIN_DB_USERNAME"]
    db_password = conf_dict["RANGER_ADMIN_DB_PASSWORD"]
    db_root_password = conf_dict["RANGER_ADMIN_DB_ROOT_PASSWORD"]
    db_name = conf_dict["RANGER_ADMIN_DB_NAME"]
    MYSQL_BIN = conf_dict['MYSQL_BIN']
    MYSQL_HOST = conf_dict['RANGER_ADMIN_DB_HOST']

    log("\nCreating Baseline DB upgrade ... \n", "debug")
    DBVERSION_CATALOG_CREATION = os.path.join(conf_dict['RANGER_DB_DIR'], 'create_dbversion_catalog.sql')
    PATCHES_PATH = os.path.join(conf_dict['RANGER_DB_DIR'], 'patches')
    if os.path.isfile(DBVERSION_CATALOG_CREATION):
        #import sql file
        proc = subprocess.Popen([MYSQL_BIN, "--user=%s" % db_user, "--host=%s" %MYSQL_HOST, "--password=%s" % db_password, db_name],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE)
        out, err = proc.communicate(file(DBVERSION_CATALOG_CREATION).read())
        log("\nBaseline DB upgraded successfully\n", "info")

    #first get all patches and then apply each patch
    files = os.listdir(PATCHES_PATH)
    # files: coming from os.listdir() sorted alphabetically, thus not numerically
    sorted_files = sorted(files, key=lambda x: str(x.split('.')[0]))
    for filename in sorted_files:
        currentPatch = PATCHES_PATH + "/"+filename
        if os.path.isfile(currentPatch):
            #apply_patches(cursor,currentPatch)
            proc = subprocess.Popen([MYSQL_BIN, "--user=%s" % db_user, "--host=%s" %MYSQL_HOST, "--password=%s" % db_password, db_name],
                        stdin=subprocess.PIPE,
                        stdout=subprocess.PIPE)
            out, err = proc.communicate(file(currentPatch).read())
            log( "\nPatch applied: " +  currentPatch +"\n", "debug")


def verify_db (db_user, db_password, db_name, db_host):
    global conf_dict
    MYSQL_BIN = conf_dict['MYSQL_BIN']

    log("\nVerifying Database: " + db_name+"\n","debug")

    cmdArr = get_mysql_cmd(db_user, db_password, db_host)
    cmdArr.extend(["-e", "show databases like '%s'" %(db_name)])
    output = subprocess.check_output(cmdArr)
    if output.strip('\r\n') == db_name:
        return True
    else:
        return False

def import_db ():

    global conf_dict

    db_user = conf_dict["RANGER_ADMIN_DB_USERNAME"]
    db_password = conf_dict["RANGER_ADMIN_DB_PASSWORD"]
    db_root_password = conf_dict["RANGER_ADMIN_DB_ROOT_PASSWORD"]
    db_name = conf_dict['RANGER_ADMIN_DB_NAME']
    MYSQL_HOST = conf_dict['RANGER_ADMIN_DB_HOST']

    db_core_file =  conf_dict['db_core_file']
    db_asset_file = conf_dict['db_asset_file']
    MYSQL_BIN = conf_dict['MYSQL_BIN']
    log ("\nImporting to Database: " + db_name,"debug");

    if verify_db(db_user, db_password, db_name, MYSQL_HOST):
        log("\nDatabase "+db_name + " already exists. Ignoring import_db\n","info")
    else:
        log("\nDatabase does not exist. Creating databse : \n" + db_name,"info")

        cmdArr = get_mysql_cmd('root', db_root_password, MYSQL_HOST)
        cmdArr.extend(["-e", "create database %s" %(db_name)])
        ret = subprocess.check_call(cmdArr)
        if ret != 0:
            log("\nDatabase creation failed!!\n","exception")
            sys.exit(1)

        ##execute each line from sql file to import DB
        if os.path.isfile(db_core_file):
            log("Importing database : " + db_name + " from file: " + db_core_file,"info")
            proc = subprocess.Popen([MYSQL_BIN, "--user=%s" % db_user, "--host=%s" %MYSQL_HOST, "--password=%s" % db_password, db_name],
                        stdin=subprocess.PIPE,
                        stdout=subprocess.PIPE)
            out, err = proc.communicate(file(db_core_file).read())
            if (proc.returncode == 0):
                log("\nAdmin db file Imported successfully\n","info")
            else:
                log("\nAdmin db file Import failed!\n","info")
                sys.exit(1)
        else:
            log("\nImport sql file not found\n","exception")
            sys.exit(1)

        if os.path.isfile(db_asset_file):
            proc = subprocess.Popen([MYSQL_BIN, "--user=%s" % db_user, "--host=%s" %MYSQL_HOST, "--password=%s" % db_password, db_name],
                        stdin=subprocess.PIPE,
                        stdout=subprocess.PIPE)
            out, err = proc.communicate(file(db_asset_file).read())
            if (proc.returncode == 0):
                log("\nAsset file Imported successfully\n","info")
            else:
                log("\nAsset file Import filed!\n","info")
                sys.exit(1)
        else:
            log("\nImport asset sql file not found\n","exception")
            sys.exit(1)

#def extract_war():
#    global conf_dict
#    war_file = conf_dict['war_file']
#    WEBAPP_ROOT = conf_dict['WEBAPP_ROOT']
#
#    if os.path.isfile(war_file):
#        log("Extract War file " + war_file + " to " + WEBAPP_ROOT,"info")
#    else:
#        log(war_file + " file not found!","exception")
#
#    if os.path.isdir(WEBAPP_ROOT):
#        with zipfile.ZipFile(war_file, "r") as z:
#            z.extractall(WEBAPP_ROOT)
#        log("Extract War file " + war_file + " to " + WEBAPP_ROOT + " DONE! ","info")
#        if os.path.isfile ( os.path.join(WEBAPP_ROOT, "WEB-INF", "logback.xml.prod")) :
#            shutil.copyfile(os.path.join(WEBAPP_ROOT, "WEB-INF", "logback.xml.prod"), os.path.join(WEBAPP_ROOT, "WEB-INF", "logback.xml"))

# def copy_mysql_connector():
#     log("Copying MYSQL Connector to "+app_home+"/WEB-INF/lib ","info")
#     shutil.copyfile(MYSQL_CONNECTOR_JAR, app_home+"/WEB-INF/lib/"+MYSQL_CONNECTOR_JAR)
#     if os.path.isfile(app_home+"/WEB-INF/lib/"+MYSQL_CONNECTOR_JAR):
#         log("Copying MYSQL Connector to app_home/WEB-INF/lib DONE","info");
#     else:
#          log("Copying MYSQL Connector to "+app_home+"/WEB-INF/lib failed","exception")


#Update Properties to File
#1 -> propertyName 2 -> newPropertyValue 3 -> fileName
def updatePropertyToFile(propertyName, newPropertyValue, fileName):
    replaceStr = propertyName +"="+ newPropertyValue
    log("replaceStr: " + replaceStr, "debug")
    successMsg = "property : " + propertyName + " not found!"
    for line in fileinput.input(fileName, inplace = 1): # Does a list of files, and writes redirects STDOUT to the file in question
      if line.replace(propertyName, replaceStr):
        successMsg = "File " + fileName + " Updated successfully : "+ propertyName
    log(successMsg, "info")
pass

def update_xapolicymgr_properties():
    global conf_dict
    WEBAPP_ROOT = conf_dict['WEBAPP_ROOT']
    xapolicymgr_properties = os.path.join(WEBAPP_ROOT, "WEB-INF", "classes", "conf", "ranger_webserver.properties")
    log("xapolicymgr_properties: " + xapolicymgr_properties, "debug")
    ModConfig(xapolicymgr_properties,"xa.webapp.dir", WEBAPP_ROOT.replace('\\','/' ))

def updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file):
    ret = subprocess.call(['python', '%s\\update_property.py' %os.getenv("RANGER_ADMIN_HOME"), propertyName ,newPropertyValue ,to_file])
    if ret == 0:
        log("Updated property for :"+to_file,"info")
    else:
        log("Update property failed for :"+to_file,"info")
        sys.exit(1)
	
def update_properties():
    global conf_dict
    sys_conf_dict={}

    RANGER_DB_FLAVOR = conf_dict["RANGER_DB_FLAVOR"]
    RANGER_AUDIT_DB_FLAVOR = conf_dict["RANGER_DB_FLAVOR"]
    MYSQL_HOST = conf_dict["RANGER_ADMIN_DB_HOST"]
    WEBAPP_ROOT = conf_dict["WEBAPP_ROOT"]
    db_user = conf_dict["RANGER_ADMIN_DB_USERNAME"]
    db_password = conf_dict["RANGER_ADMIN_DB_PASSWORD"]
    db_name = conf_dict["RANGER_ADMIN_DB_NAME"]
    RANGER_ADMIN_DB_PORT = conf_dict["RANGER_ADMIN_DB_PORT"]
    RANGER_AUDIT_DB_PORT = conf_dict["RANGER_AUDIT_DB_PORT"]

    audit_db_user = conf_dict["RANGER_AUDIT_DB_USERNAME"]
    audit_db_password = conf_dict["RANGER_AUDIT_DB_PASSWORD"]
    audit_db_name = conf_dict["RANGER_AUDIT_DB_NAME"]

    to_file_ranger = os.path.join(WEBAPP_ROOT, "WEB-INF", "classes", "conf", "ranger-admin-site.xml")
    newPropertyValue=''
    to_file_default = os.path.join(WEBAPP_ROOT, "WEB-INF", "classes", "conf", "ranger-admin-default-site.xml")
    if os.path.isfile(to_file_ranger):
        log("to_file_ranger: " + to_file_ranger + " file found", "info")
    else:
        log("to_file_ranger: " + to_file_ranger + " does not exists", "warning")
    if os.path.isfile(to_file_default):
        log("to_file_default: " + to_file_default + " file found", "info")
    else:
        log("to_file_default: " + to_file_default + " does not exists", "warning")	

    log("SQL_HOST is : " + MYSQL_HOST,"debug")
    if RANGER_DB_FLAVOR == "MYSQL":
        propertyName="ranger.jpa.jdbc.url"
        newPropertyValue="jdbc:log4jdbc:mysql://%s:%s/%s" %(MYSQL_HOST ,RANGER_ADMIN_DB_PORT, db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.user"
        newPropertyValue=db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.user"
        newPropertyValue=audit_db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.url"
        newPropertyValue="jdbc:log4jdbc:mysql://%s:%s/%s" %(MYSQL_HOST, RANGER_AUDIT_DB_PORT, audit_db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.MySQLPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.audit.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.MySQLPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.jdbc.driver"
        newPropertyValue="net.sf.log4jdbc.DriverSpy"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.driver"
        newPropertyValue="net.sf.log4jdbc.DriverSpy"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

    elif RANGER_DB_FLAVOR == "ORACLE":
        propertyName="ranger.jpa.jdbc.url"
        #if MYSQL_HOST.count(":") == 2:
        if MYSQL_HOST.count(":") == 2 or MYSQL_HOST.count(":") == 0:
            #jdbc:oracle:thin:@[HOST][:PORT]:SID or #jdbc:oracle:thin:@GL
            cstring="jdbc:oracle:thin:@%s" %(MYSQL_HOST)
        else:
            #jdbc:oracle:thin:@//[HOST][:PORT]/SERVICE
            cstring="jdbc:oracle:thin:@//%s" %(MYSQL_HOST)

        newPropertyValue=cstring
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.user"
        newPropertyValue=db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.user"
        newPropertyValue=audit_db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.url"
        newPropertyValue=cstring
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.OraclePlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.audit.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.OraclePlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.jdbc.driver"
        newPropertyValue="oracle.jdbc.OracleDriver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.driver"
        newPropertyValue="oracle.jdbc.OracleDriver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

    elif RANGER_DB_FLAVOR == "POSTGRES":
        propertyName="ranger.jpa.jdbc.url"
        newPropertyValue="jdbc:postgresql://%s:%s/%s" %(MYSQL_HOST, RANGER_ADMIN_DB_PORT, db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)
		
        propertyName="ranger.jpa.jdbc.user"
        newPropertyValue=db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.user"
        newPropertyValue=audit_db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)
		
        propertyName="ranger.jpa.audit.jdbc.url"
        newPropertyValue="jdbc:postgresql://%s:%s/%s" %(MYSQL_HOST, RANGER_AUDIT_DB_PORT, audit_db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.PostgreSQLPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.audit.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.PostgreSQLPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.jdbc.driver"
        newPropertyValue="org.postgresql.Driver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.driver"
        newPropertyValue="org.postgresql.Driver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)
       

    elif RANGER_DB_FLAVOR == "MSSQL":
        propertyName="ranger.jpa.jdbc.url"
        newPropertyValue="jdbc:sqlserver://%s:%s;databaseName=%s" %(MYSQL_HOST, RANGER_ADMIN_DB_PORT, db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.user"
        newPropertyValue=db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.user"
        newPropertyValue=audit_db_user
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)
		
        propertyName="ranger.jpa.audit.jdbc.url"
        newPropertyValue="jdbc:sqlserver://%s:%s;databaseName=%s" % (MYSQL_HOST, RANGER_AUDIT_DB_PORT, audit_db_name)
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.SQLServerPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.audit.jdbc.dialect"
        newPropertyValue="org.eclipse.persistence.platform.database.SQLServerPlatform"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.jdbc.driver"
        newPropertyValue="com.microsoft.sqlserver.jdbc.SQLServerDriver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.driver"
        newPropertyValue="com.microsoft.sqlserver.jdbc.SQLServerDriver"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

    if (os.path.isfile(os.getenv("RANGER_ADMIN_CRED_KEYSTORE_FILE"))):
        propertyName="ranger.credential.provider.path"
        newPropertyValue=os.getenv("RANGER_ADMIN_CRED_KEYSTORE_FILE")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.jdbc.credential.alias"
        newPropertyValue="ranger.jpa.jdbc.password"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.credential.provider.path"
        newPropertyValue=os.getenv("RANGER_ADMIN_CRED_KEYSTORE_FILE")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.jdbc.password"
        newPropertyValue="_"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.credential.alias"
        newPropertyValue="ranger.jpa.audit.jdbc.password"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_default)

        propertyName="ranger.jpa.audit.jdbc.password"
        newPropertyValue="_"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

    else:
        propertyName="ranger.jpa.jdbc.password"
        newPropertyValue=os.getenv("RANGER_ADMIN_DB_PASSWORD")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.jpa.audit.jdbc.password"
        newPropertyValue=os.getenv("RANGER_AUDIT_DB_PASSWORD")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

    if os.getenv("RANGER_AUTHENTICATION_METHOD") == "LDAP":

        password_validation(os.getenv("RANGER_LDAP_BIND_PASSWORD"), "LDAP_BIND")

        propertyName="ranger.authentication.method"
        newPropertyValue=os.getenv("RANGER_AUTHENTICATION_METHOD")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.url"
        newPropertyValue=os.getenv("RANGER_LDAP_URL")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.user.dnpattern"
        newPropertyValue=os.getenv("RANGER_LDAP_USERDNPATTERN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.group.searchbase"
        newPropertyValue=os.getenv("RANGER_LDAP_GROUPSEARCHBASE")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.group.searchfilter"
        newPropertyValue=os.getenv("RANGER_LDAP_GROUPSEARCHFILTER")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.group.roleattribute"
        newPropertyValue=os.getenv("RANGER_LDAP_GROUPROLEATTRIBUTE")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.base.dn"
        newPropertyValue=os.getenv("RANGER_LDAP_BASE_DN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.bind.dn"
        newPropertyValue=os.getenv("RANGER_LDAP_BIND_DN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.bind.password"
        newPropertyValue="_"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.referral"
        newPropertyValue=os.getenv("RANGER_LDAP_REFERRAL")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.user.searchfilter"
        newPropertyValue=os.getenv("RANGER_LDAP_USERSEARCHFILTER")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)
         
    elif os.getenv("RANGER_AUTHENTICATION_METHOD") == "ACTIVE_DIRECTORY":

        password_validation(os.getenv("RANGER_LDAP_AD_BIND_PASSWORD"), "AD_BIND")

        propertyName="ranger.authentication.method"
        newPropertyValue=os.getenv("RANGER_AUTHENTICATION_METHOD")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.domain"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_DOMAIN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.url"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_URL")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.base.dn"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_BASE_DN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.bind.dn"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_BIND_DN")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.bind.password"
        newPropertyValue="_"
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.referral"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_REFERRAL")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

        propertyName="ranger.ldap.ad.user.searchfilter"
        newPropertyValue=os.getenv("RANGER_LDAP_AD_USERSEARCHFILTER")
        updatePropertyToFilePy(propertyName ,newPropertyValue ,to_file_ranger)

def setup_authentication(authentication_method, xmlPath):
   if authentication_method == "UNIX":
       # log("Setting up UNIX authentication for : " + xmlPath,"debug")
       # appContextPath = xmlPath + "/META-INF/security-applicationContext.xml"
       # beanSettingPath = xmlPath + "/META-INF/contextXML/unix_bean_settings.xml"
       # secSettingPath = xmlPath + "/META-INF/contextXML/unix_security_settings.xml"
       # ## Logic is to find UNIX_BEAN_SETTINGS_START,UNIX_SEC_SETTINGS_START  from appContext xml file and append
       # ## the xml properties from unix bean settings file
       # if os.path.isfile(appContextPath) and os.path.isfile(unixSettingPath):
       #     beanStrToBeAppended =  open(beanSettingPath).read()
       #     secStrToBeAppended =  open(secSettingPath).read()
       #     fileObj = open(appContextPath)
       #     for line in fileObj.read().split(';\n'):
       #         beanLineToAppend = line.match("UNIX_BEAN_SETTINGS_START")
       #         beanLineToAppend.apend(beanStrToBeAppended)
       #         secLineToAppend = line.match("UNIX_SEC_SETTINGS_START")
       #         secLineToAppend.append(secStrToBeAppended)
       #
       #     fileObj.close()
       #     sys.exit(0);
       pass
   elif authentication_method == "LDAP":
       log("Setting up authentication for : " + xmlPath,"debug")

       log("Setting up "+authentication_method+" authentication for : " + xmlPath,"debug")

       appContextPath = os.path.join(xmlPath, "WEB-INF", "classes", "conf", "security-applicationContext.xml")
       beanSettingPath = os.path.join(xmlPath, "META-INF","contextXML","ldap_bean_settings.xml")
       secSettingPath = os.path.join(xmlPath , "META-INF","contextXML","ldap_security_settings.xml")
       ## Logic is to find LDAP_BEAN_SETTINGS_START,LDAP_SEC_SETTINGS_START  from appContext xml file and append
       ## the xml properties from unix bean settings file
       if os.path.isfile(appContextPath) and os.path.isfile(beanSettingPath):
           beanStrToBeAppended =  open(beanSettingPath).read()
           secStrToBeAppended =  open(secSettingPath).read()
           fileObj = open(appContextPath)
           data = ''
           for line in fileObj.read().split('\n'):
               if ("LDAP_BEAN_SETTINGS_START") in line:
                   line = line + '\n' + beanStrToBeAppended
               if ("LDAP_SEC_SETTINGS_START") in line:
                   line = line + '\n' + secStrToBeAppended
               if data == '':
                   data = line
               else:
                   data = data + '\n' + line
           fileObj.close()

           fileObj = open(appContextPath,'w')
           fileObj.writelines(data)
           fileObj.close()

   elif authentication_method == "ACTIVE_DIRECTORY":
       log("Setting up "+authentication_method+" authentication for : " + xmlPath,"debug")
       appContextPath = os.path.join(xmlPath, "WEB-INF", "classes", "conf", "security-applicationContext.xml")
       beanSettingPath = os.path.join(xmlPath , "META-INF","contextXML","ad_bean_settings.xml")
       secSettingPath = os.path.join(xmlPath , "META-INF","contextXML","ad_security_settings.xml")

       ## Logic is to find AD_BEAN_SETTINGS_START,AD_SEC_SETTINGS_START  from appContext xml file and append
       ## the xml properties from unix bean settings file
       if os.path.isfile(appContextPath) and os.path.isfile(beanSettingPath):
           beanStrToBeAppended =  open(beanSettingPath).read()
           secStrToBeAppended =  open(secSettingPath).read()
           fileObj = open(appContextPath)
           data = ''
           for line in fileObj.read().split('\n'):
               if ("AD_BEAN_SETTINGS_START") in line :
                    line = line + '\n'+  beanStrToBeAppended
               if ("AD_SEC_SETTINGS_START") in line:
                    line = line + '\n' + secStrToBeAppended
               if data == '':
                   data = line
               else:
                   data = data + '\n' + line
           fileObj.close()

           fileObj = open(appContextPath,'w')
           fileObj.writelines(data)
           fileObj.close()

   elif authentication_method == "NONE":
      log("Authentication Method: "+authentication_method+" authentication for : " + xmlPath,"debug")
#pass
#
def do_authentication_setup():
   global conf_dict
   webappRoot = conf_dict['WEBAPP_ROOT']
   sys_conf_dict={}
#    ##Written new function to perform authentication setup for all  cases
   authentication_method = conf_dict['RANGER_AUTHENTICATION_METHOD']
   log("Starting setup based on user authentication method = " + authentication_method,"debug")
   setup_authentication(authentication_method, webappRoot)
   # ldap_file=  os.path.join(webappRoot ,"WEB-INF","resources","xa_ldap.properties")
   ldap_file=  os.path.join(webappRoot, "WEB-INF", "classes", "conf", "xa_ldap.properties")
   if os.path.isfile(ldap_file):
       log(ldap_file + " file found", "info")
   else:
       log(ldap_file + " does not exists", "warning")
   """
   config = StringIO.StringIO()
   config.write('[LDAP_AD_CONF]\n')
   config.write(open(ldap_file).read())
   config.seek(0, os.SEEK_SET)
   ##Now parse using configparser
   cObj = ConfigParser.ConfigParser()
   cObj.optionxform = str
   cObj.readfp(config)
   options = cObj.options('LDAP_AD_CONF')
   for option in options:
       value = cObj.get('LDAP_AD_CONF', option)
       sys_conf_dict[option] = value
       cObj.set("LDAP_AD_CONF",option, value)
   log("LDAP file : "+ ldap_file + " file found", "info")
   """
   if authentication_method == "LDAP":
       log("Loading LDAP attributes and properties", "debug");
       newPropertyValue=''
       ##########
       propertyName="xa_ldap_url"
       newPropertyValue=conf_dict['RANGER_LDAP_URL']
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
       ###########
       propertyName="xa_ldap_userDNpattern"
       newPropertyValue=conf_dict['RANGER_LDAP_USERDNPATTERN']
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
       ###########
       propertyName="xa_ldap_groupSearchBase"
       newPropertyValue=conf_dict['RANGER_LDAP_GROUPSEARCHBASE']
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
       ###########
       propertyName="xa_ldap_groupSearchFilter"
       newPropertyValue=conf_dict['RANGER_LDAP_GROUPSEARCHFILTER']
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
       ###########
       propertyName="xa_ldap_groupRoleAttribute"
       newPropertyValue=conf_dict['RANGER_ldap_GROUPROLEATTRIBUTE']
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
       ###########
       propertyName="authentication_method"
       newPropertyValue=authentication_method
       # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
       ModConfig(ldap_file,propertyName,newPropertyValue)
   else:
       log( "LDAP file: "+ ldap_file +" does not exists","exception")
   if authentication_method == "ACTIVE_DIRECTORY":
       log("[I] Loading ACTIVE DIRECTORY attributes and properties", "debug")
       newPropertyValue=''
       ldap_file= os.path.join(webappRoot, "WEB-INF", "classes", "conf", "xa_ldap.properties")
       if os.path.isfile(ldap_file):
           log("LDAP file : "+ ldap_file + " file found", "info")
           propertyName="xa_ldap_ad_url"
           newPropertyValue=conf_dict['RANGER_LDAP_AD_URL']
           # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
           ModConfig(ldap_file,propertyName,newPropertyValue)
           ###########
           propertyName="xa_ldap_ad_domain"
           newPropertyValue=conf_dict['RANGER_LDAP_AD_DOMAIN']
           # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
           ModConfig(ldap_file,propertyName,newPropertyValue)
           ###########
           propertyName="authentication_method"
           newPropertyValue=authentication_method
           # cObj.set('LDAP_AD_CONF',propertyName,newPropertyValue)
           ModConfig(ldap_file,propertyName,newPropertyValue)
       else:
           log(ldap_file + " does not exists", "exception")

#   with open(ldap_file, 'wb') as configfile:
#       cObj.write(configfile)
#
#    #if authentication_method == "UNIX":
#        ## I think it is not needed for Windows
#        ##do_unixauth_setup
#    log("Finished setup based on user authentication method=authentication_method", "info")
#pass

def setup_audit_user_db():
    global conf_dict

    MYSQL_BIN = conf_dict['MYSQL_BIN']
    MYSQL_HOST = conf_dict['RANGER_AUDIT_DB_HOST']

    db_root_password = conf_dict["RANGER_AUDIT_DB_ROOT_PASSWORD"]
    audit_db_user = conf_dict["RANGER_AUDIT_DB_USERNAME"]
    audit_db_password = conf_dict["RANGER_AUDIT_DB_PASSWORD"]
    audit_db_name = conf_dict['RANGER_AUDIT_DB_NAME']
    db_audit_file =  conf_dict['db_audit_file']

    #check_mysql_audit_user_password()
    log("\n--------- Creating mysql audit user --------- \n","info")
    create_mysql_user(audit_db_name, audit_db_user, audit_db_password, MYSQL_HOST, db_root_password)
    log("\n--------- Creating mysql audit user DONE----- \n","info")

    log("\n--------- Importing Audit Database ---------\n","info")
    # Verify if audit db is present
    if verify_db(audit_db_user, audit_db_password, audit_db_name, MYSQL_HOST):
        log("\nDatabase "+audit_db_name + " already exists. Ignoring import_db\n","info")
    else:
        log("\nCreating Database " + audit_db_name, "info")
        # Create audit db is not present
        cmdArr = get_mysql_cmd('root', db_root_password, MYSQL_HOST)
        cmdArr.extend(["-e", "create database %s" %(audit_db_name)])
        ret = subprocess.check_call(cmdArr)
        if ret != 0:
            log("Database creation failed!!","error")
            sys.exit(1)
        else:
            log("Creating database "+audit_db_name+" succeeded", "info")
    # Check if audit table exists
    AUDIT_TABLE="xa_access_audit"
    log("Verifying table "+AUDIT_TABLE+" in audit database "+audit_db_name, "debug")

    cmdArr = get_mysql_cmd(audit_db_user, audit_db_password, MYSQL_HOST)
    cmdArr.extend([audit_db_name, "-e", "show tables like '%s'" %(AUDIT_TABLE)])
    output = subprocess.check_output(cmdArr)
    if output.strip('\r\n') != AUDIT_TABLE:
        # Import audit table
        log("\nImporting Audit Database file: " + db_audit_file,"debug")
        if os.path.isfile(db_audit_file):
            proc = subprocess.Popen([MYSQL_BIN, "--user=%s" % audit_db_user, "--host=%s" %MYSQL_HOST, "--password=%s" % audit_db_password, audit_db_name],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE)
            out, err = proc.communicate(file(db_audit_file).read())
            if (proc.returncode == 0):
                log("\nAudit file Imported successfully\n","info")
            else:
                log("\nAudit file Import failed!\n","info")
                sys.exit(1)
        else:
            log("\nAudit file not found!\n","info")

    else:
        log("\nTable "+AUDIT_TABLE+" already exists in audit database "+audit_db_name +"\n","info")

    log("\n--------- Importing Audit Database DONE-----\n","info")


def setup_admin_db_user():
    global conf_dict

    MYSQL_HOST = conf_dict['RANGER_ADMIN_DB_HOST']

    db_user = conf_dict["RANGER_ADMIN_DB_USERNAME"]
    db_password = conf_dict["RANGER_ADMIN_DB_PASSWORD"]
    db_root_password = conf_dict["RANGER_ADMIN_DB_ROOT_PASSWORD"]
    db_name = conf_dict['RANGER_ADMIN_DB_NAME']

    log("--------- Creating mysql user --------- ","info")
    create_mysql_user(db_name, db_user, db_password, MYSQL_HOST, db_root_password)
    #log("--------- Creating mysql user DONE----- ","info")

    #log("--------- Importing Admin Database --------- ","info")
    import_db()
    #log("--------- Importing Admin Database DONE----- ","info")
    #log("--------- Applying patches --------------- ","info")
    upgrade_db()
    #log("--------- Applying patches DONE----------- ","info")


## Ranger Functions Ends here --------------------


def call_keystore(libpath,aliasKey,aliasValue , filepath,getorcreate):
    finalLibPath = libpath.replace('\\','/').replace('//','/')
    finalFilePath = 'jceks://file/'+filepath.replace('\\','/').replace('//','/')
    if getorcreate == 'create':
        commandtorun = ['java', '-cp', finalLibPath, 'org.apache.ranger.credentialapi.buildks' ,'create', aliasKey, '-value', aliasValue, '-provider',finalFilePath]
        p = Popen(commandtorun,stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
        statuscode = p.returncode
        return statuscode
    elif getorcreate == 'get':
        commandtorun = ['java', '-cp', finalLibPath, 'org.apache.ranger.credentialapi.buildks' ,'get', aliasKey, '-provider',finalFilePath]
        p = Popen(commandtorun,stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, error = p.communicate()
        statuscode = p.returncode
        return statuscode, output
    else:
        print('proper command not received for input need get or create')


# Entry point to script using --service
def run_setup(cmd):
    init_logfiles()
    log("--------- Running Ranger PolicyManager Install Script ---------","debug")
    #parse_config_file()
    init_variables("service")
    setup_install_files()
    write_config_to_file()
    #extract_war()
    update_properties()
    #do_authentication_setup()
    return

# Entry point to script using --configure
def configure():
    init_logfiles()
    log("--------- Running Ranger PolicyManager Configure Script --------- ","info")
    #parse_config_file()
    init_variables("configure")
    sanity_check_configure_files()
    #log(" --------- Importing DB --------- ","info")
    # copy_mysql_connector()
    #log(" --------- Creatin Audit DB --------- ","info")
    setup_admin_db_user()
    setup_audit_user_db()
