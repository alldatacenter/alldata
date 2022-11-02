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

import os
import socket
import string
from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg
from ambari_commons.os_utils import search_file, run_os_command
from ambari_commons.os_windows import WinServiceController
from ambari_commons.str_utils import cbool, compress_backslashes, ensure_double_backslashes
from ambari_server.dbConfiguration import AMBARI_DATABASE_NAME, DEFAULT_USERNAME, DEFAULT_PASSWORD, \
  DBMSConfig, DbPropKeys, DbAuthenticationKeys
from ambari_server.serverConfiguration import JDBC_DRIVER_PROPERTY, JDBC_DRIVER_PATH_PROPERTY, JDBC_URL_PROPERTY, \
  JDBC_DATABASE_PROPERTY, JDBC_DATABASE_NAME_PROPERTY, \
  JDBC_HOSTNAME_PROPERTY, JDBC_PORT_PROPERTY, JDBC_USE_INTEGRATED_AUTH_PROPERTY, JDBC_USER_NAME_PROPERTY, JDBC_PASSWORD_PROPERTY, \
  JDBC_PASSWORD_FILENAME, \
  JDBC_RCA_DRIVER_PROPERTY, JDBC_RCA_URL_PROPERTY, JDBC_RCA_HOSTNAME_PROPERTY, JDBC_RCA_PORT_PROPERTY, \
  JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY, JDBC_RCA_USER_NAME_PROPERTY, JDBC_RCA_PASSWORD_FILE_PROPERTY, JDBC_RCA_PASSWORD_ALIAS, \
  PERSISTENCE_TYPE_PROPERTY, \
  get_value_from_properties, configDefaults, encrypt_password, store_password_file
from ambari_server.userInput import get_validated_string_input


# SQL Server settings
DATABASE_DBMS_MSSQL = "mssql"
DATABASE_DRIVER_NAME_MSSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
DATABASE_SERVER_MSSQL_DEFAULT = "localhost\\SQLEXPRESS"

class MSSQLAuthenticationKeys(DbAuthenticationKeys):
  def __init__(self, i_integrated_auth_key, i_user_name_key, i_password_key, i_password_alias, i_password_filename):
    self.integrated_auth_key = i_integrated_auth_key
    DbAuthenticationKeys.__init__(self, i_user_name_key, i_password_key, i_password_alias, i_password_filename)

#
# Microsoft SQL Server configuration and setup
#
class MSSQLConfig(DBMSConfig):
  def __init__(self, options, properties, storage_type):
    super(MSSQLConfig, self).__init__(options, properties, storage_type)

    """
    #Just load the defaults. The derived classes will be able to modify them later
    """
    self.dbms = DATABASE_DBMS_MSSQL
    self.driver_class_name = DATABASE_DRIVER_NAME_MSSQL

    self.JDBC_DRIVER_INSTALL_MSG = 'Before starting Ambari Server, you must install the SQL Server JDBC driver.'

    # The values from options supersede the values from properties
    self.database_host = DBMSConfig._init_member_with_prop_default(options, "database_host", properties, self.dbPropKeys.server_key, "")
    try:
      if not self.database_host:
        self.database_host = options.default_database_host
      else:
        self.database_host = compress_backslashes(self.database_host)
    except:
      self.database_host = DATABASE_SERVER_MSSQL_DEFAULT
      pass
    self.database_port = DBMSConfig._init_member_with_prop_default(options, "database_port",
                                                                   properties, self.dbPropKeys.port_key, "1433")
    self.database_name = DBMSConfig._init_member_with_prop_default(options, "database_name",
                                                                   properties, self.dbPropKeys.db_name_key, configDefaults.DEFAULT_DB_NAME)

    self.use_windows_authentication = cbool(DBMSConfig._init_member_with_prop_default(options, "database_windows_auth",
        properties, self.dbAuthKeys.integrated_auth_key, False))
    self.database_username = DBMSConfig._init_member_with_prop_default(options, "database_username",
                                                                       properties, self.dbAuthKeys.user_name_key, DEFAULT_USERNAME)
    self.database_password = DBMSConfig._init_member_with_default(options, "database_password", "")
    if not self.database_password:
      self.database_password = DBMSConfig._read_password_from_properties(properties, options)

    self.database_url = self._build_sql_server_connection_string()

    self.persistence_property = None

    self.env_var_db_name = ""
    self.env_var_db_log_name = ""

    self.init_script_file = ""
    self.drop_tables_script_file = ""

  #
  # No local DB configuration supported
  #
  def _is_local_database(self):
    return False

  def _configure_database_password(showDefault=True):
    #No password needed, using SQL Server integrated authentication
    pass

  def _prompt_db_properties(self):
    if self.must_set_database_options:
      #prompt for SQL Server host and instance name
      hostname_prompt = "SQL Server host and instance for the {0} database: ({1}) ".format(self.db_title, self.database_host)
      self.database_host = get_validated_string_input(hostname_prompt, self.database_host, None, None, False, True)

      #prompt for SQL Server authentication method
      if not self.use_windows_authentication or \
              self.database_username is None or self.database_username == "":
        auth_option_default = '1'
      else:
        auth_option_default = '2'

      user_prompt = \
        "[1] - Use SQL Server integrated authentication\n[2] - Use username+password authentication\n" \
        "Enter choice ({0}): ".format(auth_option_default)
      auth_option = get_validated_string_input(user_prompt,
                                               auth_option_default,
                                               "^[12]$",
                                               "Invalid number.",
                                               False
      )
      if str(auth_option) == '1':
        self.use_windows_authentication = True
        self.database_password = None
      else:
        self.use_windows_authentication = False

        user_prompt = "SQL Server user name for the {0} database: ({1}) ".format(self.db_title, self.database_username)
        username = get_validated_string_input(user_prompt, self.database_username, None, "User name", False,
                                              False)
        self.database_username = username

        user_prompt = "SQL Server password for the {0} database: ".format(self.db_title)
        password = get_validated_string_input(user_prompt, "", None, "Password", True, False)
        self.database_password = password

    self.database_url = self._build_sql_server_connection_string()

    return True

  def _setup_remote_server(self, properties, options):
    if self.ensure_jdbc_driver_installed(properties):
      properties.removeOldProp(self.dbPropKeys.port_key)
      properties.removeOldProp(self.dbAuthKeys.integrated_auth_key)
      properties.removeOldProp(self.dbAuthKeys.user_name_key)
      properties.removeOldProp(self.dbAuthKeys.password_key)

      properties.process_pair(self.persistence_property, 'remote')

      properties.process_pair(self.dbPropKeys.dbms_key, self.dbms)
      properties.process_pair(self.dbPropKeys.driver_key, self.driver_class_name)
      properties.process_pair(self.dbPropKeys.server_key, ensure_double_backslashes(self.database_host))
      if self.database_port is not None and self.database_port != "":
        properties.process_pair(self.dbPropKeys.port_key, self.database_port)
      properties.process_pair(self.dbPropKeys.db_name_key, self.database_name)

      self._store_db_auth_config(properties, self.dbAuthKeys, options)

      properties.process_pair(self.dbPropKeys.db_url_key, self.database_url)
    pass

  def _setup_remote_database(self):
    print 'Populating the {0} database structure...'.format(self.db_title)

    self._populate_database_structure()

  def _reset_remote_database(self):
    print 'Resetting the {0} database structure...'.format(self.db_title)

    self._populate_database_structure()

  def _is_jdbc_driver_installed(self, properties):
    """
    #Attempt to find the sqljdbc4.jar and sqljdbc_auth.dll by scanning the PATH.
    :param None
    :rtype : bool
    """
    paths = "." + os.pathsep + os.environ["PATH"]

    # Find the jar by attempting to load it as a resource dll
    driver_path = search_file("sqljdbc4.jar", paths)
    if not driver_path:
      return 0

    auth_dll_path = search_file("sqljdbc_auth.dll", paths)
    if not auth_dll_path:
      return 0

    try:
      driver_path = properties[JDBC_DRIVER_PATH_PROPERTY]
      if driver_path is None or driver_path is "":
        return 0
    except Exception:
      # No such attribute set
      return 0

    return 1

  def _install_jdbc_driver(self, properties, files_list):
    driver_path = get_value_from_properties(properties, JDBC_DRIVER_PATH_PROPERTY, None)
    if driver_path is None or driver_path == "":
      driver_path = self._get_jdbc_driver_path()

      properties.process_pair(JDBC_DRIVER_PATH_PROPERTY, driver_path)
      return True
    return False

  def ensure_dbms_is_running(self, options, properties, scmStatus=None):
    """
    :param scmStatus : SvcStatusCallback
    :rtype : None
    """

    db_host_components = self.database_host.split("\\")
    if len(db_host_components) == 1:
      db_machine = self.database_host
      sql_svc_name = "MSSQLServer"
    else:
      db_machine = db_host_components[0]
      sql_svc_name = "MSSQL$" + db_host_components[1]

    if db_machine == "localhost" or db_machine.lower() == os.getenv("COMPUTERNAME").lower() or \
            db_machine.lower() == socket.getfqdn().lower():
      #TODO: Configure the SQL Server service name in ambari.properties
      ret = WinServiceController.EnsureServiceIsStarted(sql_svc_name)
      if 0 != ret:
        raise FatalException(-1, "Error starting SQL Server: " + string(ret))

      if scmStatus is not None:
        scmStatus.reportStartPending()

      ret = WinServiceController.EnsureServiceIsStarted("SQLBrowser")  #The SQL Server JDBC driver needs this one
      if 0 != ret:
        raise FatalException(-1, "Error starting SQL Server Browser: " + string(ret))
    pass


  def _get_jdbc_driver_path(self):
    paths = "." + os.pathsep + os.environ["PATH"]

    # Find the jar in the PATH
    driver_path = search_file("sqljdbc4.jar", paths)
    return driver_path

  def _build_sql_server_connection_string(self):
    databaseUrl = "jdbc:sqlserver://{0}".format(ensure_double_backslashes(self.database_host))
    if self.database_port is not None and self.database_port != "":
      databaseUrl += ":{0}".format(self.database_port)
    databaseUrl += ";databaseName={0}".format(self.database_name)
    if(self.use_windows_authentication):
      databaseUrl += ";integratedSecurity=true"
    #No need to append the username and password, the Ambari server adds them by itself when connecting to the database
    return databaseUrl

  def _store_db_auth_config(self, properties, keys, options):
    if (self.use_windows_authentication):
      properties.process_pair(keys.integrated_auth_key, "True")
      properties.removeProp(keys.password_key)
    else:
      properties.process_pair(keys.integrated_auth_key, "False")

      properties.process_pair(keys.user_name_key, self.database_username)

      if self.isSecure:
        encrypted_password = encrypt_password(keys.password_alias, self.database_password, options)
        if self.database_password != encrypted_password:
          properties.process_pair(keys.password_key, encrypted_password)
      else:
        passwordFile = store_password_file(self.database_password, keys.password_filename)
        properties.process_pair(keys.password_key, passwordFile)

  def _populate_database_structure(self):
    # Setup DB
    os.environ[self.env_var_db_name] = self.database_name
    os.environ[self.env_var_db_log_name] = self.database_name + '_log'

    # Don't create the database, assume it already exists. Just clear out the known tables structure
    MSSQLConfig._execute_db_script(self.database_host, self.drop_tables_script_file)

    # Init DB
    MSSQLConfig._execute_db_script(self.database_host, self.init_script_file)
    pass

  @staticmethod
  def _execute_db_script(databaseHost, databaseScript, minReportedSeverityLevel=10):
    dbCmd = 'sqlcmd -S {0} -b -V {1} -i {2}'.format(databaseHost, minReportedSeverityLevel, databaseScript)
    retCode, outData, errData = run_os_command(['cmd', '/C', dbCmd])
    if not retCode == 0:
      err = 'Running database create script failed. Error output: {0} Output: {1} Exiting.'.format(errData, outData)
      raise FatalException(retCode, err)
    print_info_msg("sqlcmd output:")
    print_info_msg(outData)
    pass

#
# Microsoft SQL Server Ambari database configuration and setup
#
class MSSQLAmbariDBConfig(MSSQLConfig):
  def __init__(self, options, properties, storage_type):
    self.dbPropKeys = DbPropKeys(
      JDBC_DATABASE_PROPERTY,
      JDBC_DRIVER_PROPERTY,
      JDBC_HOSTNAME_PROPERTY,
      JDBC_PORT_PROPERTY,
      JDBC_DATABASE_NAME_PROPERTY,
      JDBC_URL_PROPERTY)
    self.dbAuthKeys = MSSQLAuthenticationKeys(
      JDBC_USE_INTEGRATED_AUTH_PROPERTY,
      JDBC_USER_NAME_PROPERTY,
      JDBC_PASSWORD_PROPERTY,
      JDBC_RCA_PASSWORD_ALIAS,
      JDBC_PASSWORD_FILENAME
    )

    super(MSSQLAmbariDBConfig, self).__init__(options, properties, storage_type)

    if self.database_name is None or self.database_name is "":
      self.database_name = AMBARI_DATABASE_NAME

    self.persistence_property = PERSISTENCE_TYPE_PROPERTY

    self.env_var_db_name ='AMBARIDBNAME'
    self.env_var_db_log_name = 'AMBARIDBLOGNAME'

    # The values from options supersede the values from properties
    self.init_script_file = compress_backslashes(DBMSConfig._init_member_with_default(options, "init_db_script_file",
        "resources" + os.path.sep + "Ambari-DDL-SQLServer-CREATE.sql"))
    self.drop_tables_script_file = compress_backslashes(DBMSConfig._init_member_with_default(options, "cleanup_db_script_file",
        "resources" + os.path.sep + "Ambari-DDL-SQLServer-DROP.sql"))

  def _setup_remote_server(self, properties, options):
    super(MSSQLAmbariDBConfig, self)._setup_remote_server(properties, options)

    properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, self.driver_class_name)
    properties.process_pair(JDBC_RCA_HOSTNAME_PROPERTY, ensure_double_backslashes(self.database_host))
    if self.database_port is not None and self.database_port != "":
      properties.process_pair(JDBC_RCA_PORT_PROPERTY, self.database_port)

    authKeys = MSSQLAuthenticationKeys(
      JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY,
      JDBC_RCA_USER_NAME_PROPERTY,
      JDBC_RCA_PASSWORD_FILE_PROPERTY,
      JDBC_RCA_PASSWORD_ALIAS,
      JDBC_PASSWORD_FILENAME
    )
    self._store_db_auth_config(properties, authKeys)

    properties.process_pair(JDBC_RCA_URL_PROPERTY, self.database_url)
    pass


def createMSSQLConfig(options, properties, storage_type, dbId):
  return MSSQLAmbariDBConfig(options, properties, storage_type)
