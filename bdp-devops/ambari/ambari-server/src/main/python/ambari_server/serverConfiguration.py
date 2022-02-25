#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import datetime
import glob
import os
import re
import shutil
import stat
import string
import sys
import tempfile
import getpass
import ambari_server.serverClassPath

from ambari_commons.exceptions import FatalException
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.os_utils import run_os_command, search_file, set_file_permissions, parse_log4j_file
from ambari_commons.logging_utils import get_debug_mode, print_info_msg, print_warning_msg, print_error_msg, \
  set_debug_mode
from ambari_server.properties import Properties
from ambari_server.userInput import get_validated_string_input
from ambari_server.utils import compare_versions, locate_file, on_powerpc
from ambari_server.ambariPath import AmbariPath
from ambari_server.userInput import get_YN_input


OS_VERSION = OSCheck().get_os_major_version()
OS_TYPE = OSCheck.get_os_type()
OS_FAMILY = OSCheck.get_os_family()

PID_NAME = "ambari-server.pid"

# Non-root user setup commands
NR_USER_PROPERTY = "ambari-server.user"

BLIND_PASSWORD = "*****"

# Common messages
PRESS_ENTER_MSG = "Press <enter> to continue."

OS_FAMILY_PROPERTY = "server.os_family"
OS_TYPE_PROPERTY = "server.os_type"

BOOTSTRAP_DIR_PROPERTY = "bootstrap.dir"
RECOMMENDATIONS_DIR_PROPERTY = 'recommendations.dir'

AMBARI_CONF_VAR = "AMBARI_CONF_DIR"
AMBARI_PROPERTIES_FILE = "ambari.properties"
AMBARI_ENV_FILE = "ambari-env.sh"
AMBARI_KRB_JAAS_LOGIN_FILE = "krb5JAASLogin.conf"
GET_FQDN_SERVICE_URL = "server.fqdn.service.url"

SERVER_OUT_FILE_KEY = "ambari.output.file.path"
VERBOSE_OUTPUT_KEY = "ambari.output.verbose"

DEBUG_MODE_KEY = "ambari.server.debug"
SUSPEND_START_MODE_KEY = "ambari.server.debug.suspend.start"

# Environment variables
AMBARI_SERVER_LIB = "AMBARI_SERVER_LIB"
JAVA_HOME = "JAVA_HOME"

AMBARI_VERSION_VAR = "AMBARI_VERSION_VAR"

# JDK
JAVA_HOME_PROPERTY = "java.home"
JDK_NAME_PROPERTY = "jdk.name"
JCE_NAME_PROPERTY = "jce.name"
JDK_DOWNLOAD_SUPPORTED_PROPERTY = "jdk.download.supported"
JCE_DOWNLOAD_SUPPORTED_PROPERTY = "jce.download.supported"

# Stack JDK
STACK_JAVA_HOME_PROPERTY = "stack.java.home"
STACK_JDK_NAME_PROPERTY = "stack.jdk.name"
STACK_JCE_NAME_PROPERTY = "stack.jce.name"
STACK_JAVA_VERSION = "stack.java.version"


#TODO property used incorrectly in local case, it was meant to be dbms name, not postgres database name,
# has workaround for now, as we don't need dbms name if persistence_type=local
JDBC_DATABASE_PROPERTY = "server.jdbc.database"                 # E.g., embedded|oracle|mysql|mssql|postgres
JDBC_DATABASE_NAME_PROPERTY = "server.jdbc.database_name"       # E.g., ambari. Not used on Windows.
JDBC_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_PORT_PROPERTY = "server.jdbc.port"
JDBC_POSTGRES_SCHEMA_PROPERTY = "server.jdbc.postgres.schema"   # Only for postgres, defaults to same value as DB name
JDBC_SQLA_SERVER_NAME = "server.jdbc.sqla.server_name"

JDBC_USER_NAME_PROPERTY = "server.jdbc.user.name"
JDBC_PASSWORD_PROPERTY = "server.jdbc.user.passwd"
JDBC_PASSWORD_FILENAME = "password.dat"
JDBC_RCA_PASSWORD_FILENAME = "rca_password.dat"

CLIENT_API_PORT_PROPERTY = "client.api.port"
CLIENT_API_PORT = "8080"
CLIENT_SECURITY = "client.security"

SERVER_VERSION_FILE_PATH = "server.version.file"

PERSISTENCE_TYPE_PROPERTY = "server.persistence.type"
JDBC_DRIVER_PROPERTY = "server.jdbc.driver"
JDBC_URL_PROPERTY = "server.jdbc.url"

# connection pool (age and time are in seconds)
JDBC_CONNECTION_POOL_TYPE = "server.jdbc.connection-pool"
JDBC_CONNECTION_POOL_ACQUISITION_SIZE = "server.jdbc.connection-pool.acquisition-size"
JDBC_CONNECTION_POOL_MAX_AGE = "server.jdbc.connection-pool.max-age"
JDBC_CONNECTION_POOL_MAX_IDLE_TIME = "server.jdbc.connection-pool.max-idle-time"
JDBC_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS = "server.jdbc.connection-pool.max-idle-time-excess"
JDBC_CONNECTION_POOL_IDLE_TEST_INTERVAL = "server.jdbc.connection-pool.idle-test-interval"

JDBC_RCA_DATABASE_PROPERTY = "server.jdbc.database"
JDBC_RCA_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_RCA_PORT_PROPERTY = "server.jdbc.port"
JDBC_RCA_SCHEMA_PROPERTY = "server.jdbc.schema"

JDBC_RCA_DRIVER_PROPERTY = "server.jdbc.rca.driver"
JDBC_RCA_URL_PROPERTY = "server.jdbc.rca.url"
JDBC_RCA_USER_NAME_PROPERTY = "server.jdbc.rca.user.name"
JDBC_RCA_PASSWORD_FILE_PROPERTY = "server.jdbc.rca.user.passwd"

DEFAULT_DBMS_PROPERTY = "server.setup.default.dbms"

JDBC_RCA_PASSWORD_ALIAS = "ambari.db.password"

### # Windows-specific # ###

JDBC_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.use.integrated.auth"

JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.rca.use.integrated.auth"

### # End Windows-specific # ###
# The user which will bootstrap embedded postgres database setup by creating the default schema and ambari user.
LOCAL_DATABASE_ADMIN_PROPERTY = "local.database.user"

# resources repo configuration
RESOURCES_DIR_PROPERTY = "resources.dir"

# stack repo upgrade
STACK_LOCATION_KEY = 'metadata.path'

# LDAP security
LDAP_MGR_PASSWORD_ALIAS = "ambari.ldap.manager.password"
LDAP_MGR_PASSWORD_PROPERTY = "ambari.ldap.connectivity.bind_password"

# SSL truststore
SSL_TRUSTSTORE_PASSWORD_ALIAS = "ambari.ssl.trustStore.password"
SSL_TRUSTSTORE_PATH_PROPERTY = "ssl.trustStore.path"
SSL_TRUSTSTORE_PASSWORD_PROPERTY = "ssl.trustStore.password"
SSL_TRUSTSTORE_TYPE_PROPERTY = "ssl.trustStore.type"

# SSL common
SSL_API = 'api.ssl'
SSL_API_PORT = 'client.api.ssl.port'
DEFAULT_SSL_API_PORT = 8443

# Kerberos
CHECK_AMBARI_KRB_JAAS_CONFIGURATION_PROPERTY = "kerberos.check.jaas.configuration"

# JDK
JDK_RELEASES="java.releases"

if on_powerpc():
  JDK_RELEASES += ".ppc64le"

VIEWS_DIR_PROPERTY = "views.dir"

ACTIVE_INSTANCE_PROPERTY = "active.instance"

# web server startup timeout
WEB_SERVER_STARTUP_TIMEOUT = "server.startup.web.timeout"

#Common setup or upgrade message
SETUP_OR_UPGRADE_MSG = "- If this is a new setup, then run the \"ambari-server setup\" command to create the user\n" \
                       "- If this is an upgrade of an existing setup, run the \"ambari-server upgrade\" command.\n" \
                       "Refer to the Ambari documentation for more information on setup and upgrade."

GPL_LICENSE_PROMPT_TEXT = """GPL License for LZO: https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
Enable Ambari Server to download and install GPL Licensed LZO packages [y/n] (n)? """

DEFAULT_DB_NAME = "ambari"

SECURITY_KEYS_DIR = "security.server.keys_dir"
EXTENSION_PATH_PROPERTY = 'extensions.path'
COMMON_SERVICES_PATH_PROPERTY = 'common.services.path'
MPACKS_STAGING_PATH_PROPERTY = 'mpacks.staging.path'
WEBAPP_DIR_PROPERTY = 'webapp.dir'
SHARED_RESOURCES_DIR = 'shared.resources.dir'
BOOTSTRAP_SCRIPT = 'bootstrap.script'
CUSTOM_ACTION_DEFINITIONS = 'custom.action.definitions'
BOOTSTRAP_SETUP_AGENT_SCRIPT = 'bootstrap.setup_agent.script'
STACKADVISOR_SCRIPT = 'stackadvisor.script'
PID_DIR_PROPERTY = 'pid.dir'
SERVER_TMP_DIR_PROPERTY = "server.tmp.dir"
GPL_LICENSE_ACCEPTED_PROPERTY = 'gpl.license.accepted'

REQUIRED_PROPERTIES = [OS_FAMILY_PROPERTY, OS_TYPE_PROPERTY, COMMON_SERVICES_PATH_PROPERTY, SERVER_VERSION_FILE_PATH,
                       WEBAPP_DIR_PROPERTY, STACK_LOCATION_KEY, SECURITY_KEYS_DIR, JDBC_DATABASE_NAME_PROPERTY,
                       NR_USER_PROPERTY, JAVA_HOME_PROPERTY, JDBC_PASSWORD_PROPERTY, SHARED_RESOURCES_DIR,
                       JDBC_USER_NAME_PROPERTY, BOOTSTRAP_SCRIPT, RESOURCES_DIR_PROPERTY, CUSTOM_ACTION_DEFINITIONS,
                       BOOTSTRAP_SETUP_AGENT_SCRIPT, STACKADVISOR_SCRIPT, BOOTSTRAP_DIR_PROPERTY, PID_DIR_PROPERTY,
                       MPACKS_STAGING_PATH_PROPERTY]

# if these properties are available 'ambari-server setup -s' is not triggered again.
SETUP_DONE_PROPERTIES = [OS_FAMILY_PROPERTY, OS_TYPE_PROPERTY, JDK_NAME_PROPERTY, JDBC_DATABASE_PROPERTY,
                         NR_USER_PROPERTY, PERSISTENCE_TYPE_PROPERTY
]

def get_default_views_dir():
  return AmbariPath.get("/var/lib/ambari-server/resources/views")

def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    default_conf_dir = AmbariPath.get("/etc/ambari-server/conf")
    print_info_msg("{0} is not set, using default {1}".format(AMBARI_CONF_VAR, default_conf_dir))
    return default_conf_dir

def find_properties_file():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    err = 'File %s not found in search path $%s: %s' % (AMBARI_PROPERTIES_FILE,
          AMBARI_CONF_VAR, get_conf_dir())
    print_error_msg (err)
    raise FatalException(1, err)
  else:
    print_info_msg('Loading properties from {0}'.format(conf_file))
  return conf_file

# Load ambari properties and return dict with values
def get_ambari_properties():
  conf_file = find_properties_file()

  # Try to read ambari properties file. It is OK to fail.
  # Return -1 on failure.
  properties = None
  try:
    properties = Properties()
    with open(conf_file) as hfR:
      properties.load(hfR)
  except (Exception), e:
    print_error_msg ('Could not read "%s": %s' % (conf_file, str(e)))
    return -1

  # Try to replace $ROOT with the value from the OS environment.
  # OK to fail. Just print the exception and return the properties
  # dictionary read above
  try:
    root_env = 'ROOT'
    if root_env in os.environ:
      root = os.environ["ROOT"].rstrip("/")
    else:
      root = ''

    for k,v in properties.iteritems():
      properties.__dict__[k] = v.replace("$ROOT", root)
      properties._props[k] = v.replace("$ROOT", root)
  except (Exception), e:
    print_error_msg ('Could not replace %s in "%s": %s' %(conf_file, root_env, str(e)))
  return properties

class ServerDatabaseType(object):
  internal = 0
  remote = 1


class ServerDatabaseEntry(object):
  def __init__(self, name, title, db_type, aliases=None):
    """
    :type name str
    :type title str
    :type db_type int
    :type aliases list
    """
    self.__name = name
    self.__title = title
    self.__type = db_type
    if aliases is None:
      aliases = []

    self.__aliases = aliases

  @property
  def name(self):
    return self.__name

  @property
  def title(self):
    return self.__title

  @property
  def dbtype(self):
    return self.__type

  def __str__(self):
    return self.name

  def __eq__(self, other):
    if other is None:
      return False

    if isinstance(other, ServerDatabaseEntry):
      return self.name == other.name and self.dbtype == other.dbtype
    elif isinstance(other, str):
      return self.name == other or other in self.__aliases

    raise RuntimeError("Not compatible type")


class ServerDatabases(object):
  postgres = ServerDatabaseEntry("postgres", "Postgres", ServerDatabaseType.remote)
  oracle = ServerDatabaseEntry("oracle", "Oracle", ServerDatabaseType.remote)
  mysql = ServerDatabaseEntry("mysql", "MySQL", ServerDatabaseType.remote)
  mssql = ServerDatabaseEntry("mssql", "MSSQL", ServerDatabaseType.remote)
  derby = ServerDatabaseEntry("derby", "Derby", ServerDatabaseType.remote)
  sqlanywhere = ServerDatabaseEntry("sqlanywhere", "SQL Anywhere", ServerDatabaseType.remote)
  postgres_internal = ServerDatabaseEntry("postgres", "Embedded Postgres", ServerDatabaseType.internal, aliases=['embedded'])

  @staticmethod
  def databases():
    props = ServerDatabases.__dict__
    r_props = []
    for p in props:
      if isinstance(props[p], ServerDatabaseEntry):
        r_props.append(props[p].name)

    return set(r_props)

  @staticmethod
  def match(name):
    """
    :type name str
    :rtype ServerDatabaseEntry
    """
    props = ServerDatabases.__dict__

    for p in props:
      if isinstance(props[p], ServerDatabaseEntry):
        if name == props[p]:
          return props[p]

    return None

class ServerConfigDefaults(object):
  def __init__(self):
    properties = get_ambari_properties()
    if properties == -1:
      print_error_msg("Error getting ambari properties")
  
    self.JAVA_SHARE_PATH = "/usr/share/java"
    self.SHARE_PATH = "/usr/share"
    self.OUT_DIR = parse_log4j_file(get_conf_dir() + "/log4j.properties")['ambari.log.dir'].replace("//", "/")
    self.SERVER_OUT_FILE = os.path.join(self.OUT_DIR, "ambari-server.out")
    self.SERVER_LOG_FILE = os.path.join(self.OUT_DIR, "ambari-server.log")
    self.DB_CHECK_LOG = os.path.join(self.OUT_DIR, "ambari-server-check-database.log")
    self.ROOT_FS_PATH = os.sep

    self.JDK_INSTALL_DIR = ""
    self.JDK_SEARCH_PATTERN = ""
    self.JAVA_EXE_SUBPATH = ""
    self.JDK_SECURITY_DIR = os.path.join("jre", "lib", "security")
    self.SERVER_RESOURCES_DIR = ""

    # Configuration defaults
    self.DEFAULT_CONF_DIR = ""
    if properties == -1:
      self.PID_DIR = AmbariPath.get(os.sep + os.path.join("var", "run", "ambari-server"))
      self.BOOTSTRAP_DIR = AmbariPath.get(os.sep + os.path.join("var", "run", "ambari-server", "bootstrap"))
      self.RECOMMENDATIONS_DIR = AmbariPath.get(os.sep + os.path.join("var", "run", "ambari-server", "stack-recommendations"))
    else:
      self.PID_DIR = properties.get_property(PID_DIR_PROPERTY)
      self.BOOTSTRAP_DIR = properties.get_property(BOOTSTRAP_DIR_PROPERTY)
      self.RECOMMENDATIONS_DIR = properties.get_property(RECOMMENDATIONS_DIR_PROPERTY)
    
    # this directories should be pre-created by user and be writable.
    self.check_if_directories_writable([self.OUT_DIR, self.PID_DIR])
    
    self.DEFAULT_LIBS_DIR = ""
    self.DEFAULT_VLIBS_DIR = ""

    self.AMBARI_PROPERTIES_BACKUP_FILE = ""
    self.AMBARI_ENV_BACKUP_FILE = ""
    self.AMBARI_KRB_JAAS_LOGIN_BACKUP_FILE = ""

    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    self.NR_ADJUST_OWNERSHIP_LIST = []
    self.NR_CHANGE_OWNERSHIP_LIST = []
    self.NR_USERADD_CMD = ""

    self.MASTER_KEY_FILE_PERMISSIONS = "640"
    self.CREDENTIALS_STORE_FILE_PERMISSIONS = "640"
    self.TRUST_STORE_LOCATION_PERMISSIONS = "640"

    self.DEFAULT_DB_NAME = "ambari"

    self.STACK_LOCATION_DEFAULT = ""
    self.EXTENSION_LOCATION_DEFAULT = ""
    self.COMMON_SERVICES_LOCATION_DEFAULT = ""
    self.MPACKS_STAGING_LOCATION_DEFAULT = ""
    self.SERVER_TMP_DIR_DEFAULT = ""
    self.DASHBOARD_DIRNAME = "dashboards"

    self.DEFAULT_VIEWS_DIR = ""

    #keytool commands
    self.keytool_bin_subpath = ""

    #Standard messages
    self.MESSAGE_SERVER_RUNNING_AS_ROOT = ""
    self.MESSAGE_WARN_SETUP_NOT_ROOT = ""
    self.MESSAGE_ERROR_RESET_NOT_ROOT = ""
    self.MESSAGE_ERROR_UPGRADE_NOT_ROOT = ""
    self.MESSAGE_CHECK_FIREWALL = ""
    
  def check_if_directories_writable(self, directories):
    for directory in directories:
      if not os.path.isdir(directory):
        try:
          os.makedirs(directory, 0755)
        except Exception as ex:
          # permission denied here is expected when ambari runs as non-root
          print_error_msg("Could not create {0}. Reason: {1}".format(directory, str(ex)))
      
      if not os.path.isdir(directory) or not os.access(directory, os.W_OK):
        raise FatalException(-1, "Unable to access {0} directory. Confirm the directory is created and is writable by Ambari Server user account '{1}'".format(directory, getpass.getuser()))

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ServerConfigDefaultsWindows(ServerConfigDefaults):
  def __init__(self):
    super(ServerConfigDefaultsWindows, self).__init__()
    self.JDK_INSTALL_DIR = "C:\\"
    self.JDK_SEARCH_PATTERN = "j[2se|dk|re]*"
    self.JAVA_EXE_SUBPATH = "bin\\java.exe"

    # Configuration defaults
    self.DEFAULT_CONF_DIR = "conf"
    self.DEFAULT_LIBS_DIR = "lib"

    self.AMBARI_PROPERTIES_BACKUP_FILE = "ambari.properties.backup"
    self.AMBARI_KRB_JAAS_LOGIN_BACKUP_FILE = ""  # ToDo: should be adjusted later
    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    # The permissions are icacls
    self.NR_ADJUST_OWNERSHIP_LIST = [
      (self.OUT_DIR, "M", "{0}", True),  #0110-0100-0100 rw-r-r
      (self.OUT_DIR, "F", "{0}", False), #0111-0101-0101 rwx-rx-rx
      (self.PID_DIR, "M", "{0}", True),
      (self.PID_DIR, "F", "{0}", False),
      ("bootstrap", "F", "{0}", False),
      ("ambari-env.cmd", "F", "{0}", False),
      ("keystore", "M", "{0}", True),
      ("keystore", "F", "{0}", False),
      ("keystore\\db", "700", "{0}", False),
      ("keystore\\db\\newcerts", "700", "{0}", False),
      ("resources\\stacks", "755", "{0}", True),
      ("resources\\custom_actions", "755", "{0}", True),
      ("conf", "644", "{0}", True),
      ("conf", "755", "{0}", False),
      ("conf\\password.dat", "640", "{0}", False),
      # Also, /etc/ambari-server/conf/password.dat
      # is generated later at store_password_file
    ]
    self.NR_USERADD_CMD = "cmd /C net user {0} {1} /ADD"

    self.SERVER_RESOURCES_DIR = "resources"
    self.STACK_LOCATION_DEFAULT = "resources\\stacks"
    self.EXTENSION_LOCATION_DEFAULT = "resources\\extensions"
    self.COMMON_SERVICES_LOCATION_DEFAULT = "resources\\common-services"
    self.MPACKS_STAGING_LOCATION_DEFAULT = "resources\\mpacks"
    self.SERVER_TMP_DIR_DEFAULT = "data\\tmp"

    self.DEFAULT_VIEWS_DIR = "resources\\views"

    #keytool commands
    self.keytool_bin_subpath = "bin\\keytool.exe"

    #Standard messages
    self.MESSAGE_SERVER_RUNNING_AS_ROOT = "Ambari Server running with 'root' privileges."
    self.MESSAGE_WARN_SETUP_NOT_ROOT = "Ambari-server setup is run with root-level privileges, passwordless sudo access for some commands commands may be required"
    self.MESSAGE_ERROR_RESET_NOT_ROOT = "Ambari-server reset must be run with administrator-level privileges"
    self.MESSAGE_ERROR_UPGRADE_NOT_ROOT = "Ambari-server upgrade must be run with administrator-level privileges"
    self.MESSAGE_CHECK_FIREWALL = "Checking firewall status..."

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ServerConfigDefaultsLinux(ServerConfigDefaults):
  def __init__(self):
    super(ServerConfigDefaultsLinux, self).__init__()
    # JDK
    self.JDK_INSTALL_DIR = AmbariPath.get("/usr/jdk64")
    self.JDK_SEARCH_PATTERN = "jdk*"
    self.JAVA_EXE_SUBPATH = "bin/java"

    # Configuration defaults
    self.DEFAULT_CONF_DIR = AmbariPath.get("/etc/ambari-server/conf")
    self.DEFAULT_LIBS_DIR = AmbariPath.get("/usr/lib/ambari-server")
    self.DEFAULT_VLIBS_DIR = AmbariPath.get("/var/lib/ambari-server")

    self.AMBARI_PROPERTIES_BACKUP_FILE = "ambari.properties.rpmsave"
    self.AMBARI_ENV_BACKUP_FILE = "ambari-env.sh.rpmsave"
    self.AMBARI_KRB_JAAS_LOGIN_BACKUP_FILE = "krb5JAASLogin.conf.rpmsave"
    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    self.NR_ADJUST_OWNERSHIP_LIST = [
      (self.OUT_DIR + "/*", "644", "{0}", True),
      (self.OUT_DIR, "755", "{0}", False),
      (self.PID_DIR + "/*", "644", "{0}", True),
      (self.PID_DIR, "755", "{0}", False),
      (self.BOOTSTRAP_DIR, "755", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/ambari-env.sh"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/ambari-sudo.sh"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/keys/*"), "600", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/keys/"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/keys/db/"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/keys/db/newcerts/"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/keys/.ssh"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/common-services/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/stacks/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/extensions/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/dashboards/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/mpacks/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/custom_actions/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/host_scripts/"), "755", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/views/*"), "644", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/resources/views/"), "755", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/views/work/"), "755", "{0}", True),
      (AmbariPath.get("/etc/ambari-server/conf/*"), "644", "{0}", True),
      (AmbariPath.get("/etc/ambari-server/conf/"), "755", "{0}", False),
      (AmbariPath.get("/etc/ambari-server/conf/password.dat"), "640", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/keys/pass.txt"), "600", "{0}", False),
      (AmbariPath.get("/etc/ambari-server/conf/ldap-password.dat"), "640", "{0}", False),
      (self.RECOMMENDATIONS_DIR, "744", "{0}", True),
      (self.RECOMMENDATIONS_DIR, "755", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/data/"), "644", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/data/"), "755", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/data/tmp/*"), "644", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/data/tmp/"), "755", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/data/cache/"), "600", "{0}", True),
      (AmbariPath.get("/var/lib/ambari-server/data/cache/"), "700", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/common-services/STORM/0.9.1/package/files/wordCount.jar"), "644", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/stacks/HDP/2.1.GlusterFS/services/STORM/package/files/wordCount.jar"), "644", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/stack-hooks/before-START/files/fast-hdfs-resource.jar"), "644", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/stacks/HDP/2.1/services/SMARTSENSE/package/files/view/smartsense-ambari-view-1.4.0.0.60.jar"), "644", "{0}", False),
      (AmbariPath.get("/var/lib/ambari-server/resources/stacks/HDP/3.0/hooks/before-START/files/fast-hdfs-resource.jar"), "644", "{0}", False),
      # Also, /etc/ambari-server/conf/password.dat
      # is generated later at store_password_file
    ]
    self.NR_CHANGE_OWNERSHIP_LIST = [
      (AmbariPath.get("/var/lib/ambari-server"), "{0}", True),
      (AmbariPath.get("/usr/lib/ambari-server"), "{0}", True),
      (self.OUT_DIR, "{0}", True),
      (self.PID_DIR, "{0}", True),
      (AmbariPath.get("/etc/ambari-server"), "{0}", True),
    ]
    self.NR_USERADD_CMD = 'useradd -M --comment "{1}" ' \
                 '--shell %s ' % locate_file('nologin', '/sbin') + ' -d ' + AmbariPath.get('/var/lib/ambari-server/keys/') + ' {0}'

    self.SERVER_RESOURCES_DIR = AmbariPath.get("/var/lib/ambari-server/resources")
    self.STACK_LOCATION_DEFAULT = AmbariPath.get("/var/lib/ambari-server/resources/stacks")
    self.EXTENSION_LOCATION_DEFAULT = AmbariPath.get("/var/lib/ambari-server/resources/extensions")
    self.COMMON_SERVICES_LOCATION_DEFAULT = AmbariPath.get("/var/lib/ambari-server/resources/common-services")
    self.MPACKS_STAGING_LOCATION_DEFAULT = AmbariPath.get("/var/lib/ambari-server/resources/mpacks")
    self.SERVER_TMP_DIR_DEFAULT = AmbariPath.get("/var/lib/ambari-server/data/tmp")

    self.DEFAULT_VIEWS_DIR = AmbariPath.get("/var/lib/ambari-server/resources/views")

    #keytool commands
    self.keytool_bin_subpath = "bin/keytool"

    #Standard messages
    self.MESSAGE_SERVER_RUNNING_AS_ROOT = "Ambari Server running with administrator privileges."
    self.MESSAGE_WARN_SETUP_NOT_ROOT = "Ambari-server setup is run with root-level privileges, passwordless sudo access for some commands commands may be required"
    self.MESSAGE_ERROR_RESET_NOT_ROOT = "Ambari-server reset should be run with root-level privileges"
    self.MESSAGE_ERROR_UPGRADE_NOT_ROOT = "Ambari-server upgrade must be run with root-level privileges"
    self.MESSAGE_CHECK_FIREWALL = "Checking firewall status..."

configDefaults = ServerConfigDefaults()

# Security
SECURITY_MASTER_KEY_LOCATION = "security.master.key.location"
SECURITY_KEY_IS_PERSISTED = "security.master.key.ispersisted"
SECURITY_KEY_ENV_VAR_NAME = "AMBARI_SECURITY_MASTER_KEY"
SECURITY_MASTER_KEY_FILENAME = "master"
SECURITY_IS_ENCRYPTION_ENABLED = "security.passwords.encryption.enabled"
SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED = "security.server.encrypt_sensitive_data"
SECURITY_KERBEROS_JASS_FILENAME = "krb5JAASLogin.conf"

SECURITY_PROVIDER_GET_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.security.encryption" + \
                            ".CredentialProvider GET {2} {3} {4} " + \
                            "> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_PUT_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.security.encryption" + \
                            ".CredentialProvider PUT {2} {3} {4} " + \
                            "> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_KEY_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.security.encryption" + \
                            ".MasterKeyServiceImpl {2} {3} {4} " + \
                            "> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SECURITY_SENSITIVE_DATA_ENCRYPTON_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.security.encryption.SensitiveDataEncryption" + \
                            " {2} " + \
                            "> " + configDefaults.SERVER_OUT_FILE + " 2>&1"



def read_ambari_user():
  '''
  Reads ambari user from properties file
  '''
  properties = get_ambari_properties()
  if properties != -1:
    user = properties[NR_USER_PROPERTY]
    if user:
      return user
  return None

def get_is_active_instance():
  # active.instance, if missing, will be considered to be true;
  # if present, it should be explicitly set to "true" to set this as the active instance;
  # any other value will be taken as a "false"
  properties = get_ambari_properties()
  # Get the value of active.instance.
  active_instance_value = None
  if properties != -1:
    if ACTIVE_INSTANCE_PROPERTY in properties.propertyNames():
      active_instance_value = properties[ACTIVE_INSTANCE_PROPERTY]

  if active_instance_value is None:  # property is missing
    is_active_instance = True
  elif (active_instance_value == 'true'): # property is explicitly set to true
    is_active_instance = True
  else:  # any other value
    is_active_instance = False

  return is_active_instance

def get_value_from_properties(properties, key, default=""):
  try:
    value = properties.get_property(key)
    if not value:
      value = default
  except:
    return default
  return value

def get_views_dir(properties):
  views_dir = properties.get_property(VIEWS_DIR_PROPERTY)
  if views_dir is None or views_dir == "":
    views_dirs = glob.glob(AmbariPath.get("/var/lib/ambari-server/resources/views/work"))
  else:
    views_dirs = glob.glob(views_dir + "/work")
  return views_dirs

def get_admin_views_dir(properties):
  views_dir = properties.get_property(VIEWS_DIR_PROPERTY)
  if views_dir is None or views_dir == "":
    views_dirs = glob.glob(AmbariPath.get("/var/lib/ambari-server/resources/views/work/ADMIN_VIEW*"))
  else:
    views_dirs = glob.glob(views_dir + "/work/ADMIN_VIEW*")
  return views_dirs

def get_views_jars(properties):
  views_dir = properties.get_property(VIEWS_DIR_PROPERTY)
  if views_dir is None or views_dir == "":
    views_jars = glob.glob(AmbariPath.get("/var/lib/ambari-server/resources/views/*.jar"))
  else:
    views_jars = glob.glob(views_dir + "/*.jar")
  return views_jars

def get_is_secure(properties):
  isSecure = properties.get_property(SECURITY_IS_ENCRYPTION_ENABLED)
  isSecure = True if isSecure and isSecure.lower() == 'true' else False
  return isSecure

def get_is_persisted(properties):
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  isPersisted = True if masterKeyFile else False

  return (isPersisted, masterKeyFile)

def get_credential_store_location(properties):
  store_loc = properties[SECURITY_KEYS_DIR]
  if store_loc is None or store_loc == "":
    store_loc = AmbariPath.get("/var/lib/ambari-server/keys/credentials.jceks")
  else:
    store_loc += os.sep + "credentials.jceks"
  return store_loc

def get_master_key_location(properties):
  keyLocation = properties[SECURITY_MASTER_KEY_LOCATION]
  if keyLocation is None or keyLocation == "":
    keyLocation = properties[SECURITY_KEYS_DIR]
  return keyLocation

def get_ambari_server_ui_port(properties):
  ambari_server_ui_port = CLIENT_API_PORT
  client_api_port = properties.get_property(CLIENT_API_PORT_PROPERTY)
  if client_api_port:
    ambari_server_ui_port = client_api_port
  api_ssl = properties.get_property(SSL_API)
  if api_ssl and str(api_ssl).lower() == "true":
    ambari_server_ui_port = DEFAULT_SSL_API_PORT
    ssl_api_port = properties.get_property(SSL_API_PORT)
    if ssl_api_port:
      ambari_server_ui_port = ssl_api_port
  return ambari_server_ui_port

# Copy file to /tmp and save with file.# (largest # is latest file)
def backup_file_in_temp(filePath):
  if filePath is not None:
    tmpDir = tempfile.gettempdir()
    back_up_file_count = len(glob.glob1(tmpDir, AMBARI_PROPERTIES_FILE + "*"))
    try:
      shutil.copyfile(filePath, tmpDir + os.sep +
                      AMBARI_PROPERTIES_FILE + "." + str(back_up_file_count + 1))
    except (Exception), e:
      print_error_msg('Could not backup file in temp "%s": %s' % (
        back_up_file_count, str(e)))
  return 0

def get_ambari_version(properties):
  """
  :param properties: Ambari properties
  :return: Return a string of the ambari version. When comparing versions, please use "compare_versions" function.
  """
  version = None
  try:
    server_version_file_path = properties[SERVER_VERSION_FILE_PATH]
    if server_version_file_path and os.path.exists(server_version_file_path):
      with open(server_version_file_path, 'r') as file:
        version = file.read().strip()
  except:
    print_error_msg("Error getting ambari version")
  return version

def get_db_type(properties):
  """
  :rtype ServerDatabaseEntry
  """
  db_type = None
  persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]

  if properties[JDBC_DATABASE_PROPERTY]:
    db_type = ServerDatabases.match(properties[JDBC_DATABASE_PROPERTY])
    if db_type == ServerDatabases.postgres and persistence_type == "local":
      db_type = ServerDatabases.postgres_internal

  if properties[JDBC_URL_PROPERTY] and db_type is None:
    jdbc_url = properties[JDBC_URL_PROPERTY].lower()
    if str(ServerDatabases.postgres) in jdbc_url:
      db_type = ServerDatabases.postgres
    elif str(ServerDatabases.oracle) in jdbc_url:
      db_type = ServerDatabases.oracle
    elif str(ServerDatabases.mysql) in jdbc_url:
      db_type = ServerDatabases.mysql
    elif str(ServerDatabases.mssql) in jdbc_url:
      db_type = ServerDatabases.mssql
    elif str(ServerDatabases.derby) in jdbc_url:
      db_type = ServerDatabases.derby
    elif str(ServerDatabases.sqlanywhere) in jdbc_url:
      db_type = ServerDatabases.sqlanywhere

  if persistence_type == "local" and db_type is None:
    db_type = ServerDatabases.postgres_internal

  return db_type

def check_database_name_property(upgrade=False):
  """
  :param upgrade: If Ambari is being upgraded.
  :return:
  """
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  version = get_ambari_version(properties)
  if upgrade and (properties[JDBC_DATABASE_PROPERTY] not in ServerDatabases.databases()
                    or properties.has_key(JDBC_RCA_SCHEMA_PROPERTY)):
    # This code exists for historic reasons in which property names changed from Ambari 1.6.1 to 1.7.0
    persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
    if persistence_type == "remote":
      db_name = properties[JDBC_RCA_SCHEMA_PROPERTY]  # this was a property in Ambari 1.6.1, but not after 1.7.0
      if db_name:
        write_property(JDBC_DATABASE_NAME_PROPERTY, db_name)

      # If DB type is missing, attempt to reconstruct it from the JDBC URL
      db_type = properties[JDBC_DATABASE_PROPERTY]
      if db_type is None or db_type.strip().lower() not in ServerDatabases.databases():
        db_type = get_db_type(properties).name
        if db_type:
          write_property(JDBC_DATABASE_PROPERTY, db_type)

      properties = get_ambari_properties()
    elif persistence_type == "local":
      # Ambari 1.6.1, had "server.jdbc.database" as the DB name, and the
      # DB type was assumed to be "postgres" if was embedded ("local")
      db_name = properties[JDBC_DATABASE_PROPERTY]
      if db_name:
        write_property(JDBC_DATABASE_NAME_PROPERTY, db_name)
        write_property(JDBC_DATABASE_PROPERTY, "postgres")
        properties = get_ambari_properties()

  dbname = properties[JDBC_DATABASE_NAME_PROPERTY]
  if dbname is None or dbname == "":
    err = "DB Name property not set in config file.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(-1, err)

def update_database_name_property(upgrade=False):
  try:
    check_database_name_property(upgrade)
  except FatalException:
    properties = get_ambari_properties()
    if properties == -1:
      err = "Error getting ambari properties"
      raise FatalException(-1, err)
    print_warning_msg("{0} property isn't set in {1} . Setting it to default value - {2}".format(JDBC_DATABASE_NAME_PROPERTY, AMBARI_PROPERTIES_FILE, configDefaults.DEFAULT_DB_NAME))
    properties.process_pair(JDBC_DATABASE_NAME_PROPERTY, configDefaults.DEFAULT_DB_NAME)
    conf_file = find_properties_file()
    try:
      with open(conf_file, "w") as hfW:
        properties.store(hfW)
    except Exception, e:
      err = 'Could not write ambari config file "%s": %s' % (conf_file, e)
      raise FatalException(-1, err)


def encrypt_password(alias, password, options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, None)
  return get_encrypted_password(alias, password, properties, options)

def get_encrypted_password(alias, password, properties, options):
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure:
    masterKey = None
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties, options)

    retCode = save_passwd_for_alias(alias, password, masterKey)
    if retCode != 0:
      print_error_msg ('Failed to save secure password!')
      return password
    else:
      return get_alias_string(alias)

  return password


def is_alias_string(passwdStr):
  regex = re.compile("\$\{alias=[\w\.]+\}")
  # Match implies string at beginning of word
  r = regex.match(passwdStr)
  if r is not None:
    return True
  else:
    return False

def get_alias_string(alias):
  return "${alias=" + alias + "}"

def get_alias_from_alias_string(aliasStr):
  return aliasStr[8:-1]

def read_passwd_for_alias(alias, masterKey="", options=None):
  if alias:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to {0}".format(configDefaults.JDK_INSTALL_DIR))
      return 1

    tempFileName = "ambari.passwd"
    passwd = ""
    tempDir = tempfile.gettempdir()
    #create temporary file for writing
    tempFilePath = tempDir + os.sep + tempFileName
    with open(tempFilePath, 'w+'):
      os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)

    if options is not None and hasattr(options, 'master_key') and options.master_key:
      masterKey = options.master_key
    if not masterKey:
      masterKey = "None"

    serverClassPath = ambari_server.serverClassPath.ServerClassPath(get_ambari_properties(), None)
    command = SECURITY_PROVIDER_GET_CMD.format(get_java_exe_path(),
                                               serverClassPath.get_full_ambari_classpath_escaped_for_shell(), alias, tempFilePath, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider get passwd: {0}".format(str(retcode)))
    if retcode != 0:
      print_error_msg ('ERROR: Unable to read password from store. alias = {0}'.format(alias))
    else:
      with open(tempFilePath, 'r') as hfRTemp:
        passwd = hfRTemp.read()
      # Remove temporary file
    os.remove(tempFilePath)
    return passwd
  else:
    print_error_msg("Alias is unreadable.")

def decrypt_password_for_alias(properties, alias, options=None):
  isSecure = get_is_secure(properties)
  if isSecure:
    masterKey = None
    (isPersisted, masterKeyFile) = get_is_persisted(properties)
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties, options)
    return read_passwd_for_alias(alias, masterKey, options)
  else:
    return alias

def save_passwd_for_alias(alias, passwd, masterKey=""):
  if alias and passwd:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to {0}".format(configDefaults.JDK_INSTALL_DIR))
      return 1

    if masterKey is None or masterKey == "":
      masterKey = "None"

    serverClassPath = ambari_server.serverClassPath.ServerClassPath(get_ambari_properties(), None)
    command = SECURITY_PROVIDER_PUT_CMD.format(get_java_exe_path(),
                                               serverClassPath.get_full_ambari_classpath_escaped_for_shell(), alias, passwd, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save passwd: {0}".format(str(retcode)))
    return retcode
  else:
    print_error_msg("Alias or password is unreadable.")


def get_pass_file_path(conf_file, filename):
  return os.path.join(os.path.dirname(conf_file), filename)

def store_password_file(password, filename):
  conf_file = find_properties_file()
  passFilePath = get_pass_file_path(conf_file, filename)

  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
  print_info_msg("Adjusting filesystem permissions")
  ambari_user = read_ambari_user()
  if ambari_user: # at the first install ambari_user can be None. Which is fine since later on password.dat is chowned with the correct ownership.
    set_file_permissions(passFilePath, "660", ambari_user, False)

  #Windows paths need double backslashes, otherwise the Ambari server deserializer will think the single \ are escape markers
  return passFilePath.replace('\\', '\\\\')

def remove_password_file(filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
                              filename)

  if os.path.exists(passFilePath):
    try:
      os.remove(passFilePath)
    except Exception, e:
      print_warning_msg('Unable to remove password file: {0}'.format(str(e)))
      return 1
  pass
  return 0


def get_web_server_startup_timeout(properties):
  """
  Gets the time, in seconds, that the startup script should wait for the web server to bind to
  the configured port. If this value is too low, then the startup script will return an
  error code even though Ambari is actually starting up.
  :param properties:
  :return: The timeout value, in seconds. The default is 90.
  """
  # get the timeout property and strip it if it exists
  timeout = properties[WEB_SERVER_STARTUP_TIMEOUT]
  timeout = None if timeout is None else timeout.strip()

  if timeout is None or timeout == "":
    timeout = 90
  else:
    timeout = int(timeout)
  return timeout


def get_original_master_key(properties, options = None):
  input = True
  masterKey = None
  env_master_key = os.environ.get(SECURITY_KEY_ENV_VAR_NAME)
  while(input):
    # Find an alias that exists
    alias = None
    property = properties.get_property(JDBC_PASSWORD_PROPERTY)
    if property and is_alias_string(property):
      alias = JDBC_RCA_PASSWORD_ALIAS

    if not alias:
      property = properties.get_property(LDAP_MGR_PASSWORD_PROPERTY)
      if property and is_alias_string(property):
        alias = LDAP_MGR_PASSWORD_ALIAS

    if not alias:
      property = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
      if property and is_alias_string(property):
        alias = SSL_TRUSTSTORE_PASSWORD_ALIAS

    # Decrypt alias with master to validate it, if no master return
    password = None
    if alias and env_master_key and env_master_key is not "" and env_master_key != "None":
      password = read_passwd_for_alias(alias, env_master_key, options)
    if not password:
      try:
        if options is not None and hasattr(options, 'master_key') and options.master_key:
          masterKey = options.master_key
        if not masterKey:
          masterKey = get_validated_string_input('Enter current Master Key: ',
                                                 "", ".*", "", True, True)
          if options is not None:
            options.master_key = masterKey
      except KeyboardInterrupt:
        print_warning_msg('Exiting...')
        sys.exit(1)
      if alias and masterKey:
        password = read_passwd_for_alias(alias, masterKey, options)
        if not password:
          masterKey = None
          if options is not None:
            options.master_key = None
          print_error_msg ("ERROR: Master key does not match")

          continue

    input = False

  return masterKey


# Load database connection properties from conf file
def parse_properties_file(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  args.server_version_file_path = properties[SERVER_VERSION_FILE_PATH]
  args.persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
  args.jdbc_url = properties[JDBC_URL_PROPERTY]

  args.dbms = properties[JDBC_DATABASE_PROPERTY]
  if not args.persistence_type:
    args.persistence_type = "local"

  if args.persistence_type == 'remote':
    args.database_host = properties[JDBC_HOSTNAME_PROPERTY]
    args.database_port = properties[JDBC_PORT_PROPERTY]

  args.database_name = properties[JDBC_DATABASE_NAME_PROPERTY]
  args.database_username = properties[JDBC_USER_NAME_PROPERTY]
  args.postgres_schema = properties[JDBC_POSTGRES_SCHEMA_PROPERTY] \
    if JDBC_POSTGRES_SCHEMA_PROPERTY in properties.propertyNames() else None
  args.database_password_file = properties[JDBC_PASSWORD_PROPERTY]
  if args.database_password_file:
    if not is_alias_string(args.database_password_file):
      with open(properties[JDBC_PASSWORD_PROPERTY]) as hfDbPwd:
        args.database_password = hfDbPwd.read()
    else:
      args.database_password = args.database_password_file
  return 0

def is_jaas_keytab_exists(conf_file):
  with open(conf_file, "r") as f:
    lines = f.read()

  match = re.search("keyTab=(.*)$", lines, re.MULTILINE)
  return os.path.exists(match.group(1).strip("\"").strip())

def update_krb_jaas_login_properties():
  """
  Update configuration files
  :return: int -2 - skipped, -1 - error, 0 - successful
  """
  prev_conf_file = search_file(configDefaults.AMBARI_KRB_JAAS_LOGIN_BACKUP_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_KRB_JAAS_LOGIN_FILE, get_conf_dir())

  # check if source and target files exists, if not - skip copy action
  if prev_conf_file is None or conf_file is None:
    return -2

  # if rpmsave file contains invalid keytab, we can skip restoring
  if not is_jaas_keytab_exists(prev_conf_file):
    return -2

  try:
    # restore original file, destination arg for rename func shouldn't exists
    os.remove(conf_file)
    os.rename(prev_conf_file, conf_file)
    print_warning_msg("Original file %s kept" % AMBARI_KRB_JAAS_LOGIN_FILE)
  except OSError as e:
    print_error_msg ("Couldn't move %s file: %s" % (prev_conf_file, str(e)))
    return -1

  return 0

def update_ambari_env():
  prev_env_file = search_file(configDefaults.AMBARI_ENV_BACKUP_FILE, configDefaults.DEFAULT_VLIBS_DIR)
  env_file = search_file(AMBARI_ENV_FILE, configDefaults.DEFAULT_VLIBS_DIR)

  # Previous env file does not exist
  if (not prev_env_file) or (prev_env_file is None):
    print ("INFO: Can not find %s file from previous version, skipping restore of environment settings. "
           "%s may not include any user customization.") % (configDefaults.AMBARI_ENV_BACKUP_FILE, AMBARI_ENV_FILE)
    return 0

  try:
    if env_file is not None:
      os.remove(env_file)
      os.rename(prev_env_file, env_file)
      print ("INFO: Original file %s kept") % (AMBARI_ENV_FILE)
  except OSError as e:
    print_error_msg ( "Couldn't move %s file: %s" % (prev_env_file, str(e)))
    return -1

  return 0

def set_property(key, value, rewrite=True):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)

  if not rewrite and key in properties.keys():
    return

  properties.process_pair(key, value)
  update_properties(properties)

 
# default should be false / not accepted 
def write_gpl_license_accepted(default_prompt_value = False, text = GPL_LICENSE_PROMPT_TEXT):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)


  if GPL_LICENSE_ACCEPTED_PROPERTY in properties.keys() and properties.get_property(GPL_LICENSE_ACCEPTED_PROPERTY).lower() == "true":
    return True

  result = get_YN_input(text, default_prompt_value)
  set_property(GPL_LICENSE_ACCEPTED_PROPERTY, str(result).lower())

  return result

def update_ambari_properties():
  prev_conf_file = search_file(configDefaults.AMBARI_PROPERTIES_BACKUP_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())

  # Previous config file does not exist
  if (not prev_conf_file) or (prev_conf_file is None):
    print_warning_msg("Can not find %s file from previous version, skipping import of settings" % configDefaults.AMBARI_PROPERTIES_BACKUP_FILE)
    return 0

  # ambari.properties file does not exists
  if conf_file is None:
    print_error_msg("Can't find %s file" % AMBARI_PROPERTIES_FILE)
    return -1

  with open(prev_conf_file) as hfOld:
    try:
      old_properties = Properties()
      old_properties.load(hfOld)
    except Exception, e:
      print_error_msg ('Could not read "%s": %s' % (prev_conf_file, str(e)))
      return -1

  try:
    new_properties = Properties()
    with open(conf_file) as hfNew:
      new_properties.load(hfNew)

    for prop_key, prop_value in old_properties.getPropertyDict().items():
      prop_value = prop_value.replace("/usr/lib/python2.6/site-packages", "/usr/lib/ambari-server/lib")
      if "agent.fqdn.service.url" == prop_key:
        # what is agent.fqdn property in ambari.props?
        new_properties.process_pair(GET_FQDN_SERVICE_URL, prop_value)
      elif "server.os_type" == prop_key:
        new_properties.process_pair(OS_TYPE_PROPERTY, OS_FAMILY + OS_VERSION)
      elif JDK_RELEASES == prop_key:
        # don't replace new jdk releases with old releases, because they can be updated
        pass
      else:
        new_properties.process_pair(prop_key, prop_value)

    # Adding custom user name property if it is absent
    # In previous versions without custom user support server was started as
    # "root" anyway so it's a reasonable default
    if NR_USER_PROPERTY not in new_properties.keys():
      new_properties.process_pair(NR_USER_PROPERTY, "root")

    # update the os. In case os detection routine changed
    new_properties.process_pair(OS_FAMILY_PROPERTY, OS_FAMILY + OS_VERSION)

    with open(conf_file, 'w') as hfW:
      new_properties.store(hfW)

  except Exception, e:
    print_error_msg ('Could not write "%s": %s' % (conf_file, str(e)))
    return -1

  timestamp = datetime.datetime.now()
  fmt = '%Y%m%d%H%M%S'
  new_conf_file = prev_conf_file + '.' + timestamp.strftime(fmt)
  try:
    os.rename(prev_conf_file, new_conf_file)
  except Exception, e:
    print_error_msg ('Could not rename "%s" to "%s": %s' % (prev_conf_file, new_conf_file, str(e)))
    #Not critical, move on

  return 0

# update properties in a section-less properties file
# Cannot use ConfigParser due to bugs in version 2.6
def update_properties(propertyMap):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  backup_file_in_temp(conf_file)
  if propertyMap is not None and conf_file is not None:
    properties = Properties()
    try:
      with open(conf_file, 'r') as file:
        properties.load(file)
    except (Exception), e:
      print_error_msg('Could not read "%s": %s' % (conf_file, e))
      return -1

    for key in propertyMap.keys():
      properties.removeOldProp(key)
      properties.process_pair(key, str(propertyMap[key]))

    for key in properties.keys():
      if not propertyMap.has_key(key):
        properties.removeOldProp(key)

    with open(conf_file, 'w') as file:
      properties.store_ordered(file)

  return 0

def update_properties_2(properties, propertyMap):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  backup_file_in_temp(conf_file)
  if conf_file is not None:
    if propertyMap is not None:
      for key in propertyMap.keys():
        properties.removeOldProp(key)
        properties.process_pair(key, str(propertyMap[key]))
      pass

    with open(conf_file, 'w') as file:
      properties.store_ordered(file)
    pass
  pass

def write_property(key, value):
  conf_file = find_properties_file()
  properties = Properties()
  try:
    with open(conf_file, "r") as hfR:
      properties.load(hfR)
  except Exception, e:
    print_error_msg('Could not read ambari config file "%s": %s' % (conf_file, e))
    return -1
  properties.process_pair(key, value)
  try:
    with open(conf_file, 'w') as hfW:
      properties.store(hfW)
  except Exception, e:
    print_error_msg('Could not write ambari config file "%s": %s' % (conf_file, e))
    return -1
  return 0

#
# Checks if options determine local DB configuration
#
def is_local_database(args):
  try:
    return args.persistence_type == 'local'
  except AttributeError:
    return False


def update_debug_mode():
  debug_mode = get_debug_mode()
  # The command-line settings supersede the ones in ambari.properties
  if not debug_mode & 1:
    properties = get_ambari_properties()
    if properties == -1:
      print_error_msg("Error getting ambari properties")
      return -1

    if get_value_from_properties(properties, DEBUG_MODE_KEY, False):
      debug_mode = debug_mode | 1
    if get_value_from_properties(properties, SUSPEND_START_MODE_KEY, False):
      debug_mode = debug_mode | 2

    set_debug_mode(debug_mode)

#
### JDK ###
#

#
# Describes the JDK configuration data, necessary for download and installation
#
class JDKRelease:
  name = ""
  desc = ""
  url = ""
  dest_file = ""
  jcpol_url = "http://public-repo-1.hortonworks.com/ARTIFACTS/UnlimitedJCEPolicyJDK7.zip"
  dest_jcpol_file = ""
  inst_dir = ""

  def __init__(self, i_name, i_desc, i_url, i_dest_file, i_jcpol_url, i_dest_jcpol_file, i_inst_dir, i_reg_exp):
    if i_name is None or i_name is "":
      raise FatalException(-1, "Invalid JDK name: " + (i_desc or ""))
    self.name = i_name
    if i_desc is None or i_desc is "":
      self.desc = self.name
    else:
      self.desc = i_desc
    if i_url is None or i_url is "":
      raise FatalException(-1, "Invalid URL for JDK " + i_name)
    self.url = i_url
    if i_dest_file is None or i_dest_file is "":
      self.dest_file = i_name + ".exe"
    else:
      self.dest_file = i_dest_file
    if not (i_jcpol_url is None or i_jcpol_url is ""):
      self.jcpol_url = i_jcpol_url
    if i_dest_jcpol_file is None or i_dest_jcpol_file is "":
      self.dest_jcpol_file = "jcpol-" + i_name + ".zip"
    else:
      self.dest_jcpol_file = i_dest_jcpol_file
    if i_inst_dir is None or i_inst_dir is "":
      self.inst_dir = os.path.join(configDefaults.JDK_INSTALL_DIR, i_desc)
    else:
      self.inst_dir = i_inst_dir
    if i_reg_exp is None or i_reg_exp is "":
      raise FatalException(-1, "Invalid output parsing regular expression for JDK " + i_name)
    self.reg_exp = i_reg_exp

  @classmethod
  def from_properties(cls, properties, section_name):
    (desc, url, dest_file, jcpol_url, jcpol_file, inst_dir, reg_exp) = JDKRelease.__load_properties(properties, section_name)
    cls = JDKRelease(section_name, desc, url, dest_file, jcpol_url, jcpol_file, inst_dir, reg_exp)
    return cls

  @staticmethod
  def __load_properties(properties, section_name):
    if section_name is None or section_name is "":
      raise FatalException(-1, "Invalid properties section: " + ("(empty)" if section_name is None else ""))
    if(properties.has_key(section_name + ".desc")):   #Not critical
      desc = properties[section_name + ".desc"]
    else:
      desc = section_name
    if not properties.has_key(section_name + ".url"):
      raise FatalException(-1, "Invalid JDK URL in the properties section: " + section_name)
    url = properties[section_name + ".url"]      #Required
    if not properties.has_key(section_name + ".re"):
      raise FatalException(-1, "Invalid JDK output parsing regular expression in the properties section: " + section_name)
    reg_exp = properties[section_name + ".re"]      #Required
    if(properties.has_key(section_name + ".dest-file")):   #Not critical
      dest_file = properties[section_name + ".dest-file"]
    else:
      dest_file = section_name + ".exe"
    if(properties.has_key(section_name + ".jcpol-url")):   #Not critical
      jcpol_url = properties[section_name + ".jcpol-url"]
    else:
      jcpol_url = None
    if(properties.has_key(section_name + ".jcpol-file")):   #Not critical
      jcpol_file = properties[section_name + ".jcpol-file"]
    else:
      jcpol_file = None
    if(properties.has_key(section_name + ".home")):   #Not critical
      inst_dir = properties[section_name + ".home"]
    else:
      inst_dir = "C:\\" + section_name
    return (desc, url, dest_file, jcpol_url, jcpol_file, inst_dir, reg_exp)
  pass

def get_JAVA_HOME():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return None

  java_home = properties[JAVA_HOME_PROPERTY]

  if (not 0 == len(java_home)) and (os.path.exists(java_home)):
    return java_home

  return None

#
# Checks jdk path for correctness
#
def validate_jdk(jdk_path):
  if jdk_path:
    if os.path.exists(jdk_path):
      java_exe_path = os.path.join(jdk_path, configDefaults.JAVA_EXE_SUBPATH)
      if os.path.exists(java_exe_path) and os.path.isfile(java_exe_path):
        return True
  return False

#
# Finds the available JDKs.
#
def find_jdk():
  jdkPath = get_JAVA_HOME()
  if jdkPath:
    if validate_jdk(jdkPath):
      return jdkPath
  print("INFO: Looking for available JDKs at {0}".format(configDefaults.JDK_INSTALL_DIR))
  jdks = glob.glob(os.path.join(configDefaults.JDK_INSTALL_DIR, configDefaults.JDK_SEARCH_PATTERN))
  #[fbarca] Use the newest JDK
  jdks.sort(None, None, True)
  print_info_msg("Found: {0}".format(str(jdks)))
  if len(jdks) == 0:
    return
  for jdkPath in jdks:
    print "INFO: Trying to use JDK {0}".format(jdkPath)
    if validate_jdk(jdkPath):
      print "INFO: Selected JDK {0}".format(jdkPath)
      return jdkPath
    else:
      print_error_msg ("JDK {0} is invalid".format(jdkPath))
  return

def get_java_exe_path():
  jdkPath = find_jdk()
  if jdkPath:
    java_exe = os.path.join(jdkPath, configDefaults.JAVA_EXE_SUBPATH)
    return java_exe
  return


#
# Server resource files location
#
def get_resources_location(properties):
  err = 'Invalid directory'
  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
    if not resources_dir:
      resources_dir = configDefaults.SERVER_RESOURCES_DIR
  except (KeyError), e:
    err = 'Property ' + str(e) + ' is not defined at ' + properties.fileName
    resources_dir = configDefaults.SERVER_RESOURCES_DIR

  if not os.path.exists(os.path.abspath(resources_dir)):
    msg = 'Resources dir ' + resources_dir + ' is incorrectly configured: ' + err
    raise FatalException(1, msg)

  return resources_dir

#
# Stack location
#
def get_stack_location(properties):
  stack_location = properties[STACK_LOCATION_KEY]
  if not stack_location:
    stack_location = configDefaults.STACK_LOCATION_DEFAULT
  return stack_location

#
# Extension location
#
def get_extension_location(properties):
  extension_location = properties[EXTENSION_PATH_PROPERTY]
  if not extension_location:
    extension_location = configDefaults.EXTENSION_LOCATION_DEFAULT
  return extension_location

#
# Common services location
#
def get_common_services_location(properties):
  common_services_location = properties[COMMON_SERVICES_PATH_PROPERTY]
  if not common_services_location:
    common_services_location = configDefaults.COMMON_SERVICES_LOCATION_DEFAULT
  return common_services_location

#
# Management packs staging location
#
def get_mpacks_staging_location(properties):
  mpacks_staging_location = properties[MPACKS_STAGING_PATH_PROPERTY]
  if not mpacks_staging_location:
    mpacks_staging_location = configDefaults.MPACKS_STAGING_LOCATION_DEFAULT
  return mpacks_staging_location


#
# Dashboard location
#
def get_dashboard_location(properties):
  resources_dir = get_resources_location(properties)
  dashboard_location = os.path.join(resources_dir, configDefaults.DASHBOARD_DIRNAME)
  return dashboard_location

#
# Server temp location
#
def get_server_temp_location(properties):
  server_tmp_dir = properties[SERVER_TMP_DIR_PROPERTY]
  if not server_tmp_dir:
    server_tmp_dir = configDefaults.SERVER_TMP_DIR_DEFAULT
  return server_tmp_dir

def get_missing_properties(properties, property_set=REQUIRED_PROPERTIES):
  missing_propertiers = []
  for property in property_set:
    value = properties[property]
    if not value:
      missing_propertiers.append(property)

  return missing_propertiers
