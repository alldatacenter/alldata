#!/usr/bin/env python
"""
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

Ambari Agent

"""

import os
import re
import socket
import getpass
import tempfile

from resource_management.libraries.functions.default import default
from ambari_commons import os_utils
from ambari_commons.repo_manager import ManagerFactory
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.inet_utils import download_file
from resource_management import Script, Execute, format
from ambari_agent.HostInfo import HostInfo
from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler
from resource_management.core.resources import Directory, File
from resource_management.core.exceptions import Fail
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core import shell
from resource_management.core.logger import Logger


# WARNING. If you are adding a new host check that is used by cleanup, add it to BEFORE_CLEANUP_HOST_CHECKS
# It is used by HostCleanup.py
CHECK_JAVA_HOME = "java_home_check"
CHECK_DB_CONNECTION = "db_connection_check"
CHECK_HOST_RESOLUTION = "host_resolution_check"
CHECK_LAST_AGENT_ENV = "last_agent_env_check"
CHECK_INSTALLED_PACKAGES = "installed_packages"
CHECK_EXISTING_REPOS = "existing_repos"
CHECK_TRANSPARENT_HUGE_PAGE = "transparentHugePage"

BEFORE_CLEANUP_HOST_CHECKS = ','.join([CHECK_LAST_AGENT_ENV, CHECK_INSTALLED_PACKAGES, CHECK_EXISTING_REPOS, CHECK_TRANSPARENT_HUGE_PAGE])

# If exit_code for some of these checks is not 0 task will be failed
FALLIBLE_CHECKS = [CHECK_DB_CONNECTION]

DB_MYSQL = "mysql"
DB_ORACLE = "oracle"
DB_POSTGRESQL = "postgres"
DB_MSSQL = "mssql"
DB_SQLA = "sqlanywhere"

JDBC_DRIVER_CLASS_MYSQL = "com.mysql.jdbc.Driver"
JDBC_DRIVER_CLASS_ORACLE = "oracle.jdbc.driver.OracleDriver"
JDBC_DRIVER_CLASS_POSTGRESQL = "org.postgresql.Driver"
JDBC_DRIVER_CLASS_MSSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
JDBC_DRIVER_CLASS_SQLA = "sap.jdbc4.sqlanywhere.IDriver"

JDBC_AUTH_SYMLINK_MSSQL = "sqljdbc_auth.dll"

JDBC_DRIVER_SQLA_JAR = "sajdbc4.jar"
JARS_PATH_IN_ARCHIVE_SQLA = "/sqla-client-jdbc/java"
LIBS_PATH_IN_ARCHIVE_SQLA = "/sqla-client-jdbc/native/lib64"
JDBC_DRIVER_SQLA_JAR_PATH_IN_ARCHIVE = "/sqla-client-jdbc/java/" + JDBC_DRIVER_SQLA_JAR

THP_FILE_REDHAT = "/sys/kernel/mm/redhat_transparent_hugepage/enabled"
THP_FILE_UBUNTU = "/sys/kernel/mm/transparent_hugepage/enabled"

class CheckHost(Script):
  # Package prefixes that are used to find repos (then repos are used to find other packages)
  PACKAGES = [
    "^hadoop.*$", "^zookeeper.*$", "^webhcat.*$", "^oozie.*$", "^ambari.*$", "^.+-manager-server-db.*$",
    "^.+-manager-daemons.*$", "^mahout[_\-]\d.*$", "^spark.*$", "^falcon.*$", "^hbase.*$", "^kafka.*$", "^knox.*$",
    "^sqoop.*$", "^storm.*$", "^flume.*$","^hcatalog.*$", "^phoenix.*$", "^ranger.*$", "^accumulo.*$", "^hive_.*$",
    "^pig[_\-.].*$" # there's a default 'pigz' package which we should avoid
  ]
  

  # ignore packages from repos whose names start with these strings
  IGNORE_PACKAGES_FROM_REPOS = [
    "installed"
  ]
  

  # ignore required packages
  IGNORE_PACKAGES = [
    "epel-release", "ambari-server", "ambari-agent", "nagios",
    # ganglia related:
    "ganglia", "libganglia", "libconfuse", "perl", "rrdtool", "python-rrdtool", "gmetad", "librrd", "rrdcached"
  ]
  
  # Additional packages to look for (search packages that start with these)
  ADDITIONAL_PACKAGES = [
    "ambari-log4j"
  ]
  
  # ignore repos from the list of repos to be cleaned
  IGNORE_REPOS = [
    "HDP-UTILS", "AMBARI", "BASE", "EXTRAS"
  ]

  def __init__(self):
    self.reportFileHandler = HostCheckReportFileHandler()
    self.pkg_provider = ManagerFactory.get()
  
  def actionexecute(self, env):
    Logger.info("Host checks started.")
    config = Script.get_config()
    tmp_dir = Script.get_tmp_dir()
    report_file_handler_dict = {}

    #print "CONFIG: " + str(config)

    check_execute_list = config['commandParams']['check_execute_list']
    if check_execute_list == '*BEFORE_CLEANUP_HOST_CHECKS*':
      check_execute_list = BEFORE_CLEANUP_HOST_CHECKS
    structured_output = {}

    Logger.info("Check execute list: " + str(check_execute_list))

    # check each of the commands; if an unknown exception wasn't handled
    # by the functions, then produce a generic exit_code : 1
    if CHECK_JAVA_HOME in check_execute_list:
      try :
        java_home_check_structured_output = self.execute_java_home_available_check(config)
        structured_output[CHECK_JAVA_HOME] = java_home_check_structured_output
      except Exception, exception:
        Logger.exception("There was an unexpected error while checking for the Java home location: " + str(exception))
        structured_output[CHECK_JAVA_HOME] = {"exit_code" : 1, "message": str(exception)}

    if CHECK_DB_CONNECTION in check_execute_list:
      try :
        db_connection_check_structured_output = self.execute_db_connection_check(config, tmp_dir)
        structured_output[CHECK_DB_CONNECTION] = db_connection_check_structured_output
      except Exception, exception:
        Logger.exception("There was an unknown error while checking database connectivity: " + str(exception))
        structured_output[CHECK_DB_CONNECTION] = {"exit_code" : 1, "message": str(exception)}

    if CHECK_HOST_RESOLUTION in check_execute_list:
      try :
        host_resolution_structured_output = self.execute_host_resolution_check(config)
        structured_output[CHECK_HOST_RESOLUTION] = host_resolution_structured_output
      except Exception, exception :
        Logger.exception("There was an unknown error while checking IP address lookups: " + str(exception))
        structured_output[CHECK_HOST_RESOLUTION] = {"exit_code" : 1, "message": str(exception)}
    if CHECK_LAST_AGENT_ENV in check_execute_list:
      try :
        last_agent_env_structured_output = self.execute_last_agent_env_check()
        structured_output[CHECK_LAST_AGENT_ENV] = last_agent_env_structured_output
      except Exception, exception :
        Logger.exception("There was an unknown error while checking last host environment details: " + str(exception))
        structured_output[CHECK_LAST_AGENT_ENV] = {"exit_code" : 1, "message": str(exception)}
        
    # CHECK_INSTALLED_PACKAGES and CHECK_EXISTING_REPOS required to run together for
    # reasons of not doing the same common work twice for them as it takes some time, especially on Ubuntu.
    if CHECK_INSTALLED_PACKAGES in check_execute_list and CHECK_EXISTING_REPOS in check_execute_list:
      try :
        installed_packages, repos = self.execute_existing_repos_and_installed_packages_check(config)
        structured_output[CHECK_INSTALLED_PACKAGES] = installed_packages
        structured_output[CHECK_EXISTING_REPOS] = repos
      except Exception, exception :
        Logger.exception("There was an unknown error while checking installed packages and existing repositories: " + str(exception))
        structured_output[CHECK_INSTALLED_PACKAGES] = {"exit_code" : 1, "message": str(exception)}
        structured_output[CHECK_EXISTING_REPOS] = {"exit_code" : 1, "message": str(exception)}

    # Here we are checking transparent huge page if CHECK_TRANSPARENT_HUGE_PAGE is in check_execute_list
    if CHECK_TRANSPARENT_HUGE_PAGE in check_execute_list:
      try :
        transparent_huge_page_structured_output = self.execute_transparent_huge_page_check(config)
        structured_output[CHECK_TRANSPARENT_HUGE_PAGE] = transparent_huge_page_structured_output
      except Exception, exception :
        Logger.exception("There was an unknown error while getting transparent huge page data: " + str(exception))
        structured_output[CHECK_TRANSPARENT_HUGE_PAGE] = {"exit_code" : 1, "message": str(exception)}

    # this is necessary for HostCleanup to know later what were the results.
    self.reportFileHandler.writeHostChecksCustomActionsFile(structured_output)
    
    self.put_structured_out(structured_output)

    error_message = ""
    for check_name in FALLIBLE_CHECKS:
      if check_name in structured_output and "exit_code" in structured_output[check_name] \
          and structured_output[check_name]["exit_code"] != 0:
        error_message += "Check {0} was unsuccessful. Exit code: {1}.".format(check_name, \
                                                                             structured_output[check_name]["exit_code"])
        if "message" in structured_output[check_name]:
          error_message += " Message: {0}".format(structured_output[check_name]["message"])
        error_message += "\n"

    Logger.info("Host checks completed.")
    Logger.debug("Structured output: " + str(structured_output))

    if error_message:
      Logger.error(error_message)
      raise Fail(error_message)


  def execute_transparent_huge_page_check(self, config):
    Logger.info("Transparent huge page check started.")

    thp_regex = "\[(.+)\]"
    file_name = None
    if OSCheck.is_ubuntu_family():
      file_name = THP_FILE_UBUNTU
    elif OSCheck.is_redhat_family():
      file_name = THP_FILE_REDHAT
    if file_name and os.path.isfile(file_name):
      with open(file_name) as f:
        file_content = f.read()
        transparent_huge_page_check_structured_output = {"exit_code" : 0, "message": str(re.search(thp_regex,
                                                                                                    file_content).groups()[0])}
    else:
      transparent_huge_page_check_structured_output = {"exit_code" : 0, "message": ""}

    Logger.info("Transparent huge page check completed.")
    return transparent_huge_page_check_structured_output

  def execute_existing_repos_and_installed_packages_check(self, config):
      Logger.info("Installed packages and existing repos checks started.")
      installedPackages = self.pkg_provider.installed_packages()
      availablePackages = self.pkg_provider.available_packages()

      repos = self.pkg_provider.get_installed_repos(self.PACKAGES, installedPackages + availablePackages,
                                                    self.IGNORE_PACKAGES_FROM_REPOS)

      packagesInstalled = self.pkg_provider.get_installed_pkgs_by_repo(repos, self.IGNORE_PACKAGES, installedPackages)
      additionalPkgsInstalled = self.pkg_provider.get_installed_pkgs_by_names(self.ADDITIONAL_PACKAGES, installedPackages)
      allPackages = list(set(packagesInstalled + additionalPkgsInstalled))
      
      installedPackages = self.pkg_provider.get_package_details(installedPackages, allPackages)
      repos = self.pkg_provider.get_repos_to_remove(repos, self.IGNORE_REPOS)

      Logger.info("Installed packages and existing repos checks completed.")
      return installedPackages, repos


  def execute_java_home_available_check(self, config):
    Logger.info("Java home check started.")
    java_home = config['commandParams']['java_home']

    Logger.info("Java home to check: " + java_home)
    java_bin = "java"
    if OSCheck.is_windows_family():
      java_bin = "java.exe"
  
    if not os.path.isfile(os.path.join(java_home, "bin", java_bin)):
      Logger.warning("Java home doesn't exist!")
      java_home_check_structured_output = {"exit_code" : 1, "message": "Java home doesn't exist!"}
    else:
      Logger.info("Java home exists!")
      java_home_check_structured_output = {"exit_code" : 0, "message": "Java home exists!"}

    Logger.info("Java home check completed.")
    return java_home_check_structured_output


  def execute_db_connection_check(self, config, tmp_dir):
    Logger.info("DB connection check started.")
  
    # initialize needed data
  
    ambari_server_hostname = config['commandParams']['ambari_server_host']
    check_db_connection_jar_name = "DBConnectionVerification.jar"
    jdk_location = config['commandParams']['jdk_location']
    java_home = config['commandParams']['java_home']
    db_name = config['commandParams']['db_name']
    no_jdbc_error_message = None

    if db_name == DB_MYSQL:
      jdbc_driver_mysql_name = default("/ambariLevelParams/custom_mysql_jdbc_name", None)
      if not jdbc_driver_mysql_name:
        no_jdbc_error_message = "The MySQL JDBC driver has not been set. Please ensure that you have executed 'ambari-server setup --jdbc-db=mysql --jdbc-driver=/path/to/jdbc_driver'."
      else:
        jdbc_url = CheckHost.build_url(jdk_location, jdbc_driver_mysql_name)
        jdbc_driver_class = JDBC_DRIVER_CLASS_MYSQL
        jdbc_name = jdbc_driver_mysql_name
    elif db_name == DB_ORACLE:
      jdbc_driver_oracle_name = default("/ambariLevelParams/custom_oracle_jdbc_name", None)
      if not jdbc_driver_oracle_name:
        no_jdbc_error_message = "The Oracle JDBC driver has not been set. Please ensure that you have executed 'ambari-server setup --jdbc-db=oracle --jdbc-driver=/path/to/jdbc_driver'."
      else:
        jdbc_url = CheckHost.build_url(jdk_location, jdbc_driver_oracle_name)
        jdbc_driver_class = JDBC_DRIVER_CLASS_ORACLE
        jdbc_name = jdbc_driver_oracle_name
    elif db_name == DB_POSTGRESQL:
      jdbc_driver_postgres_name = default("/ambariLevelParams/custom_postgres_jdbc_name", None)
      if not jdbc_driver_postgres_name:
        no_jdbc_error_message = "The Postgres JDBC driver has not been set. Please ensure that you have executed 'ambari-server setup --jdbc-db=postgres --jdbc-driver=/path/to/jdbc_driver'."
      else:
        jdbc_url = CheckHost.build_url(jdk_location, jdbc_driver_postgres_name)
        jdbc_driver_class = JDBC_DRIVER_CLASS_POSTGRESQL
        jdbc_name = jdbc_driver_postgres_name
    elif db_name == DB_MSSQL:
      jdbc_driver_mssql_name = default("/ambariLevelParams/custom_mssql_jdbc_name", None)
      if not jdbc_driver_mssql_name:
        no_jdbc_error_message = "The MSSQL JDBC driver has not been set. Please ensure that you have executed 'ambari-server setup --jdbc-db=mssql --jdbc-driver=/path/to/jdbc_driver'."
      else:
        jdbc_url = CheckHost.build_url(jdk_location, jdbc_driver_mssql_name)
        jdbc_driver_class = JDBC_DRIVER_CLASS_MSSQL
        jdbc_name = jdbc_driver_mssql_name
    elif db_name == DB_SQLA:
      jdbc_driver_sqla_name = default("/ambariLevelParams/custom_sqlanywhere_jdbc_name", None)
      if not jdbc_driver_sqla_name:
        no_jdbc_error_message = "The SQLAnywhere JDBC driver has not been set. Please ensure that you have executed 'ambari-server setup --jdbc-db=sqlanywhere --jdbc-driver=/path/to/jdbc_driver'."
      else:
        jdbc_url = CheckHost.build_url(jdk_location, jdbc_driver_sqla_name)
        jdbc_driver_class = JDBC_DRIVER_CLASS_SQLA
        jdbc_name = jdbc_driver_sqla_name
    else: no_jdbc_error_message = format("'{db_name}' database type not supported.")

    if no_jdbc_error_message:
      Logger.warning(no_jdbc_error_message)
      db_connection_check_structured_output = {"exit_code" : 1, "message": no_jdbc_error_message}
      return db_connection_check_structured_output

    db_connection_url = config['commandParams']['db_connection_url']
    user_name = config['commandParams']['user_name']
    user_passwd = config['commandParams']['user_passwd']
    agent_cache_dir = os.path.abspath(config["agentLevelParams"]["agentCacheDir"])
    check_db_connection_url = CheckHost.build_url(jdk_location, check_db_connection_jar_name)
    jdbc_path = os.path.join(agent_cache_dir, jdbc_name)
    class_path_delimiter = ":"
    if db_name == DB_SQLA:
      jdbc_jar_path = agent_cache_dir + JDBC_DRIVER_SQLA_JAR_PATH_IN_ARCHIVE
      java_library_path = agent_cache_dir + JARS_PATH_IN_ARCHIVE_SQLA + class_path_delimiter + agent_cache_dir + \
                          LIBS_PATH_IN_ARCHIVE_SQLA
    else:
      jdbc_jar_path = jdbc_path
      java_library_path = agent_cache_dir

    check_db_connection_path = os.path.join(agent_cache_dir, check_db_connection_jar_name)

    java_bin = "java"
    if OSCheck.is_windows_family():
      java_bin = "java.exe"
      class_path_delimiter = ";"

    java_exec = os.path.join(java_home, "bin",java_bin)

    if ('jdk_name' not in config['commandParams'] or config['commandParams']['jdk_name'] == None \
        or config['commandParams']['jdk_name'] == '') and not os.path.isfile(java_exec):
      message = "Custom java is not available on host. Please install it. Java home should be the same as on server. " \
                "\n"
      Logger.warning(message)
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output

    environment = { "no_proxy": format("{ambari_server_hostname}") }
    # download and install java if it doesn't exists
    if not os.path.isfile(java_exec):
      jdk_name = config['commandParams']['jdk_name']
      jdk_url = CheckHost.build_url(jdk_location, jdk_name)
      jdk_download_target = os.path.join(agent_cache_dir, jdk_name)
      java_dir = os.path.dirname(java_home)
      try:
        download_file(jdk_url, jdk_download_target)
      except Exception, e:
        message = "Error downloading JDK from Ambari Server resources. Check network access to " \
                  "Ambari Server.\n" + str(e)
        Logger.exception(message)
        db_connection_check_structured_output = {"exit_code" : 1, "message": message}
        return db_connection_check_structured_output

      if jdk_name.endswith(".exe"):
        install_cmd = "{0} /s INSTALLDIR={1} STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-agent".format(
        os_utils.quote_path(jdk_download_target), os_utils.quote_path(java_home),
        )
        install_path = [java_dir]
        try:
          Execute(install_cmd, path = install_path)
        except Exception, e:
          message = "Error installing java.\n" + str(e)
          Logger.exception(message)
          db_connection_check_structured_output = {"exit_code" : 1, "message": message}
          return db_connection_check_structured_output
      else:
        tmp_java_dir = tempfile.mkdtemp(prefix="jdk_tmp_", dir=tmp_dir)
        sudo = AMBARI_SUDO_BINARY
        if jdk_name.endswith(".bin"):
          chmod_cmd = ("chmod", "+x", jdk_download_target)
          install_cmd = format("cd {tmp_java_dir} && echo A | {jdk_download_target} -noregister && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")
        elif jdk_name.endswith(".gz"):
          chmod_cmd = ("chmod","a+x", java_dir)
          install_cmd = format("cd {tmp_java_dir} && tar -xf {jdk_download_target} && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")
        try:
          Directory(java_dir)
          Execute(chmod_cmd, not_if = format("test -e {java_exec}"), sudo = True)
          Execute(install_cmd, not_if = format("test -e {java_exec}"))
          File(format("{java_home}/bin/java"), mode=0755, cd_access="a")
          Directory(java_home, owner=getpass.getuser(), recursive_ownership=True)
          Execute(('chmod', '-R', '755', java_home), sudo = True)
        except Exception, e:
          message = "Error installing java.\n" + str(e)
          Logger.exception(message)
          db_connection_check_structured_output = {"exit_code" : 1, "message": message}
          return db_connection_check_structured_output
        finally:
          Directory(tmp_java_dir, action="delete")

    # download DBConnectionVerification.jar from ambari-server resources
    try:
      download_file(check_db_connection_url, check_db_connection_path)

    except Exception, e:
      message = "Error downloading DBConnectionVerification.jar from Ambari Server resources. Check network access to " \
                "Ambari Server.\n" + str(e)
      Logger.exception(message)
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output
  
    # download jdbc driver from ambari-server resources
    try:
      download_file(jdbc_url, jdbc_path)
      if db_name == DB_MSSQL and OSCheck.is_windows_family():
        jdbc_auth_path = os.path.join(agent_cache_dir, JDBC_AUTH_SYMLINK_MSSQL)
        jdbc_auth_url = CheckHost.build_url(jdk_location, JDBC_AUTH_SYMLINK_MSSQL)
        download_file(jdbc_auth_url, jdbc_auth_path)
      elif db_name == DB_SQLA:
        # unpack tar.gz jdbc which was donaloaded
        untar_sqla_type2_driver = ('tar', '-xvf', jdbc_path, '-C', agent_cache_dir)
        Execute(untar_sqla_type2_driver, sudo = True)
    except Exception, e:
      message = format("Error: Ambari Server cannot download the database JDBC driver and is unable to test the " \
                "database connection. You must run ambari-server setup --jdbc-db={db_name} " \
                "--jdbc-driver=/path/to/your/{db_name}/driver.jar on the Ambari Server host to make the JDBC " \
                "driver available for download and to enable testing the database connection.\n") + str(e)
      Logger.exception(message)
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output

    # For Oracle connection as SYS should be as SYSDBA
    if db_name == DB_ORACLE and user_name.upper() == "SYS":
      user_name = "SYS AS SYSDBA"

    # try to connect to db
    db_connection_check_command = format("{java_exec} -cp {check_db_connection_path}{class_path_delimiter}" \
           "{jdbc_jar_path} -Djava.library.path={java_library_path} org.apache.ambari.server.DBConnectionVerification \"{db_connection_url}\" " \
           "\"{user_name}\" {user_passwd!p} {jdbc_driver_class}")

    if db_name == DB_SQLA:
      db_connection_check_command = "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:{0}{1} {2}".format(agent_cache_dir,
                                                                LIBS_PATH_IN_ARCHIVE_SQLA, db_connection_check_command)

    code, out = shell.call(db_connection_check_command)

    if code == 0:
      db_connection_check_structured_output = {"exit_code" : 0, "message": "DB connection check completed successfully!" }
    else:
      db_connection_check_structured_output = {"exit_code" : 1, "message":  out }

    Logger.info("DB connection check completed.")
    return db_connection_check_structured_output

  # check whether each host in the command can be resolved to an IP address
  def execute_host_resolution_check(self, config):
    Logger.info("IP address forward resolution check started.")
    
    FORWARD_LOOKUP_REASON = "FORWARD_LOOKUP"
    
    failedCount = 0
    failures = []
    hosts_with_failures = []
   
    if config['commandParams']['hosts'] is not None :
      hosts = config['commandParams']['hosts'].split(",")
      successCount = len(hosts)
    else :
      successCount = 0
      hosts = ""

    socket.setdefaulttimeout(3)
    for host in hosts:
      try:
        host = host.strip()
        socket.gethostbyname(host)
      except socket.error,exception:
        successCount -= 1
        failedCount += 1

        hosts_with_failures.append(host)

        failure = { "host": host, "type": FORWARD_LOOKUP_REASON,
          "cause": exception.args }

        failures.append(failure)

    if failedCount > 0 :
      message = "There were " + str(failedCount) + " host(s) that could not resolve to an IP address."
    else :
      message = "All hosts resolved to an IP address."

    Logger.info(message)

    host_resolution_check_structured_output = {
      "exit_code" : 0,
      "message" : message,
      "failed_count" : failedCount,
      "success_count" : successCount,
      "failures" : failures,
      "hosts_with_failures" : hosts_with_failures
      }

    Logger.info("IP address forward resolution check completed.")
    return host_resolution_check_structured_output

  # computes and returns the host information of the agent
  def execute_last_agent_env_check(self):
    Logger.info("Last Agent Env check started.")
    hostInfo = HostInfo()
    last_agent_env_check_structured_output = { }
    hostInfo.register(last_agent_env_check_structured_output, runExpensiveChecks=False, checkJavaProcs=True)
    Logger.info("Last Agent Env check completed successfully.")

    return last_agent_env_check_structured_output

  @staticmethod
  def build_url(base_url, path):
    """
    Builds a URL by appending a relative path to a base URL, ensuring additional /'s are not added
    between the two parts.

    :param base_url: a base URL
    :param path: a relative path
    :return: the resulting URL
    """

    url = base_url[0:-1] if base_url and base_url.endswith('/') else base_url
    url = url + '/'
    url = url + (path[1:] if path and path.startswith('/') else path)
    return url


if __name__ == "__main__":
  CheckHost().execute()
