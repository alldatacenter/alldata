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

import optparse
import os
import re
import shutil
import sys
from ambari_commons import subprocess32
import getpass
import logging

from ambari_commons.exceptions import FatalException
from ambari_commons.firewall import Firewall
from ambari_commons.inet_utils import force_download_file, download_progress
from ambari_commons.logging_utils import get_silent, print_info_msg, print_warning_msg, print_error_msg, get_verbose
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_utils import copy_files, run_os_command, is_root
from ambari_commons.str_utils import compress_backslashes
from ambari_server.dbConfiguration import DBMSConfigFactory, TAR_GZ_ARCHIVE_TYPE, default_connectors_map, check_jdbc_drivers
from ambari_server.serverConfiguration import configDefaults, JDKRelease, get_stack_location,\
  get_ambari_properties, get_is_secure, get_is_persisted, get_java_exe_path, get_JAVA_HOME, get_missing_properties, \
  get_resources_location, get_value_from_properties, read_ambari_user, update_properties, validate_jdk, write_property, write_gpl_license_accepted,\
  JAVA_HOME, JAVA_HOME_PROPERTY, JCE_NAME_PROPERTY, JDBC_RCA_URL_PROPERTY, JDBC_URL_PROPERTY, \
  JDK_NAME_PROPERTY, JDK_RELEASES, NR_USER_PROPERTY, OS_FAMILY, OS_FAMILY_PROPERTY, OS_TYPE, OS_TYPE_PROPERTY, OS_VERSION, \
  VIEWS_DIR_PROPERTY, JDBC_DATABASE_PROPERTY, JDK_DOWNLOAD_SUPPORTED_PROPERTY, JCE_DOWNLOAD_SUPPORTED_PROPERTY, SETUP_DONE_PROPERTIES, \
  STACK_JAVA_HOME_PROPERTY, STACK_JDK_NAME_PROPERTY, STACK_JCE_NAME_PROPERTY, STACK_JAVA_VERSION, GPL_LICENSE_ACCEPTED_PROPERTY
from ambari_server.serverUtils import is_server_runing
from ambari_server.setupSecurity import adjust_directory_permissions
from ambari_server.userInput import get_YN_input, get_validated_string_input
from ambari_server.utils import locate_file, update_latest_in_repoinfos_for_stacks, get_json_url_from_repo_file
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.ambariPath import AmbariPath

from ambari_commons.constants import AMBARI_SUDO_BINARY

logger = logging.getLogger(__name__)

# selinux commands
GET_SE_LINUX_ST_CMD = locate_file('sestatus', '/usr/sbin')
SE_SETENFORCE_CMD = "setenforce 0"
SE_STATUS_DISABLED = "disabled"
SE_STATUS_ENABLED = "enabled"
SE_MODE_ENFORCING = "enforcing"
SE_MODE_PERMISSIVE = "permissive"

PERSISTENCE_TYPE_PROPERTY = "server.persistence.type"


# Non-root user setup commands
NR_USER_COMMENT = "Ambari user"

VIEW_EXTRACT_CMD = "{0} -cp {1} " + \
                   "org.apache.ambari.server.view.ViewRegistry extract {2} " + \
                   "> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"

# use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"

JDK_PROMPT = "[{0}] {1}\n"
JDK_VALID_CHOICES = "^[{0}{1:d}]$"

JDK_VERSION_CHECK_CMD = """{0} -version 2>&1 | grep -i version | sed 's/.*version ".*\.\(.*\)\..*"/\\1/; 1q' 2>&1"""

def get_supported_jdbc_drivers():
  factory = DBMSConfigFactory()
  return factory.get_supported_jdbc_drivers()

JDBC_DB_OPTION_VALUES = get_supported_jdbc_drivers()


#
# Setup security prerequisites
#

def verify_setup_allowed(options):
  if get_silent():
    properties = get_ambari_properties()
    if properties == -1:
      print_error_msg("Error getting ambari properties")
      return -1

    isSecure = get_is_secure(properties)
    if isSecure:
      (isPersisted, masterKeyFile) = get_is_persisted(properties)
      if not isPersisted:
        print "ERROR: Cannot run silent 'setup' with password encryption enabled " \
              "and Master Key not persisted."
        print "Ambari Server 'setup' exiting."
        return 1

    factory = DBMSConfigFactory()
    default_dbms = factory.get_default_dbms_name()
    if default_dbms:
      valid = True
      if options.dbms is not None \
        and options.database_host is not None \
        and options.database_port is not None \
        and options.database_name is not None \
        and options.database_username is not None \
        and options.database_password is not None:

        if default_dbms == "sqlanywhere" and options.sqla_server_name is None:
          valid = False

      else:
        valid = False

      if not valid:
        print "ERROR: Cannot run silent setup without database connection properties provided."
        print "Ambari Server 'setup' exiting."
        return 2

  return 0


#
# Security enhancements (Linux only)
#

#
# Checks SELinux
#

def check_selinux():
  try:
    retcode, out, err = run_os_command(GET_SE_LINUX_ST_CMD)
    se_status = re.search('(disabled|enabled)', out).group(0)
    print "SELinux status is '" + se_status + "'"
    if se_status == SE_STATUS_DISABLED:
      return 0
    else:
      try:
        se_mode = re.search('(enforcing|permissive)', out).group(0)
      except AttributeError:
        err = "Error determining SELinux mode. Exiting."
        raise FatalException(1, err)
      print "SELinux mode is '" + se_mode + "'"
      if se_mode == SE_MODE_ENFORCING:
        print "Temporarily disabling SELinux"
        run_os_command(SE_SETENFORCE_CMD)
      print_warning_msg(
        "SELinux is set to 'permissive' mode and temporarily disabled.")
      ok = get_YN_input("OK to continue [y/n] (y)? ", True)
      if not ok:
        raise FatalException(1, None)
      return 0
  except OSError:
    print_warning_msg("Could not run {0}: OK".format(GET_SE_LINUX_ST_CMD))
  return 0


# No security enhancements in Windows
@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def disable_security_enhancements():
  retcode = 0
  err = ''
  return (retcode, err)

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def disable_security_enhancements():
  print 'Checking SELinux...'
  err = ''
  retcode = check_selinux()
  if not retcode == 0:
    err = 'Failed to disable SELinux. Exiting.'
  return (retcode, err)


#
# User account creation
#

class AmbariUserChecks(object):
  def __init__(self):
    self.NR_USER_CHANGE_PROMPT = ""
    self.NR_USER_CUSTOMIZE_PROMPT = ""
    self.NR_DEFAULT_USER = ""
    self.NR_USER_COMMENT = "Ambari user"

    self.register_service = False
    self.user = None
    self.password = None

  def do_checks(self):
    try:
      user = read_ambari_user()
      if not user:
        user = self.NR_DEFAULT_USER

      if self.user is not None:   #Command-line parameter is the default
        update_user_setting = True
        prompt_msg = self.NR_USER_CUSTOMIZE_PROMPT.format('y')
      else:
        update_user_setting = False
        if user != self.NR_DEFAULT_USER:
          prompt_msg = self.NR_USER_CHANGE_PROMPT.format(user, 'n')
        else:
          prompt_msg = self.NR_USER_CUSTOMIZE_PROMPT.format('n')
        self.user = user if user else self.NR_DEFAULT_USER

      self.register_service = get_YN_input(prompt_msg, update_user_setting)
      if self.register_service:
        retcode = self._create_custom_user()
        if retcode != 0:
          return retcode

      adjust_directory_permissions(self.user)
    except OSError as e:
      print_error_msg("Failed: %s" % str(e))
      return 4
    except Exception as e:
      print_error_msg("Unexpected error %s" % str(e))
      return 1
    return 0

  def _create_custom_user(self):
    pass

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class AmbariUserChecksWindows(AmbariUserChecks):
  def __init__(self, options):
    super(AmbariUserChecksWindows, self).__init__()

    self.NR_USER_CHANGE_PROMPT = "Ambari-server service is configured to run under user '{0}'. Change this setting [y/n] ({1})? "
    self.NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server service [y/n] ({0})? "
    self.NR_DEFAULT_USER = "NT AUTHORITY\\SYSTEM"
    self.NR_SYSTEM_USERS = ["NT AUTHORITY\\SYSTEM", "NT AUTHORITY\\NetworkService", "NT AUTHORITY\\LocalService"]

    self.user = options.svc_user
    self.password = options.svc_password

  def _create_custom_user(self):
    user = get_validated_string_input(
      "Enter user account for ambari-server service ({0}):".format(self.user),
      self.user, None,
      "Invalid username.",
      False
    )
    if user in self.NR_SYSTEM_USERS:
      self.user = user
      return 0

    if get_silent():
      password = self.password
    else:
      password = get_validated_string_input("Enter password for user {0}:".format(user), "", None, "Password", True, False)

    from ambari_commons.os_windows import UserHelper

    uh = UserHelper(user)

    if uh.find_user():
      print_info_msg("User {0} already exists, make sure that you typed correct password for user, "
                     "skipping user creation".format(user))
    else:
      status, message = uh.create_user(password)
      if status == UserHelper.USER_EXISTS:
        print_info_msg("User {0} already exists, make sure that you typed correct password for user, "
                       "skipping user creation".format(user))

      elif status == UserHelper.ACTION_FAILED:  # fail
        print_warning_msg("Can't create user {0}. Failed with message {1}".format(user, message))
        return UserHelper.ACTION_FAILED

    self.password = password

    # setting SeServiceLogonRight and SeBatchLogonRight to user
    #This is unconditional
    status, message = uh.add_user_privilege('SeServiceLogonRight')
    if status == UserHelper.ACTION_FAILED:
      print_warning_msg("Can't add SeServiceLogonRight to user {0}. Failed with message {1}".format(user, message))
      return UserHelper.ACTION_FAILED

    status, message = uh.add_user_privilege('SeBatchLogonRight')
    if status == UserHelper.ACTION_FAILED:
      print_warning_msg("Can't add SeBatchLogonRight to user {0}. Failed with message {1}".format(user, message))
      return UserHelper.ACTION_FAILED

    print_info_msg("User configuration is done.")
    print_warning_msg("When using non SYSTEM user make sure that your user has read\write access to log directories and "
                      "all server directories. In case of integrated authentication for SQL Server make sure that your "
                      "user is properly configured to access the ambari database.")

    if user.find('\\') == -1:
      user = '.\\' + user

    self.user = user
    return 0

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class AmbariUserChecksLinux(AmbariUserChecks):
  def __init__(self, options):
    super(AmbariUserChecksLinux, self).__init__()

    self.NR_USER_CHANGE_PROMPT = "Ambari-server daemon is configured to run under user '{0}'. Change this setting [y/n] ({1})? "
    self.NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server daemon [y/n] ({0})? "
    self.NR_DEFAULT_USER = getpass.getuser()

    self.NR_USERADD_CMD = 'useradd -M --comment "{1}" ' \
                          '--shell %s ' % locate_file('nologin', '/sbin') + '-d ' + AmbariPath.get('/var/lib/ambari-server/keys/') + ' {0}'

  def _create_custom_user(self):
    user = get_validated_string_input(
      "Enter user account for ambari-server daemon ({0}):".format(self.user),
      self.user,
      "^[a-z_][a-z0-9_-]{1,31}$",
      "Invalid username.",
      False
    )

    print_info_msg("Trying to create user {0}".format(user))
    command = self.NR_USERADD_CMD.format(user, self.NR_USER_COMMENT)
    retcode, out, err = run_os_command(command)
    if retcode == 9:  # 9 = username already in use
      print_info_msg("User {0} already exists, "
                     "skipping user creation".format(user))

    elif retcode != 0:  # fail
      print_warning_msg("Can't create user {0}. Command {1} "
                        "finished with {2}: \n{3}".format(user, command, retcode, err))
      return retcode

    print_info_msg("User configuration is done.")

    self.user = user

    return 0

def check_ambari_user(options):
  uc = AmbariUserChecks(options)
  retcode = uc.do_checks()
  return retcode, uc.register_service, uc.user, uc.password


#
# Windows service setup
#

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def service_setup(register_service, svc_user, svc_password):
  from ambari_windows_service import svcsetup

  svc_user_setup = svc_user if svc_user.upper() != "NT AUTHORITY\\SYSTEM" else None

  result = svcsetup(register_service, svc_user_setup, svc_password)
  if result == 0:
    write_property(NR_USER_PROPERTY, svc_user)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def service_setup(register_service, svc_user, svc_password):
  #Nothing else to do in Linux
  write_property(NR_USER_PROPERTY, svc_user)


#
# Firewall
#

def check_firewall():
  firewall_obj = Firewall().getFirewallObject()
  firewall_on = firewall_obj.check_firewall()
  if firewall_obj.stderrdata and len(firewall_obj.stderrdata) > 0:
    print firewall_obj.stderrdata
  if firewall_on:
    print_warning_msg("%s is running. Confirm the necessary Ambari ports are accessible. " %
                      firewall_obj.FIREWALL_SERVICE_NAME +
                      "Refer to the Ambari documentation for more details on ports.")
    ok = get_YN_input("OK to continue [y/n] (y)? ", True)
    if not ok:
      raise FatalException(1, None)


#
#  ## JDK ###
#

class JDKSetup(object):
  def __init__(self):
    self.JDK_DEFAULT_CONFIGS = []

    self.JDK_PROMPT = "[{0}] {1}\n"
    self.JDK_CUSTOM_CHOICE_PROMPT = "[{0}] Custom JDK\n==============================================================================\nEnter choice ({1}): "
    self.JDK_VALID_CHOICES = "^[{0}{1:d}]$"
    self.JDK_MIN_FILESIZE = 5000
    self.JAVA_BIN = ""

    self.jdk_index = 0

  #
  # Downloads and installs the JDK and the JCE policy archive
  #
  def download_and_install_jdk(self, args, properties, ambariOnly = False):
    conf_file = properties.fileName

    jcePolicyWarn = "JCE Policy files are required for configuring Kerberos security. If you plan to use Kerberos," \
                    "please make sure JCE Unlimited Strength Jurisdiction Policy Files are valid on all hosts."

    if args.java_home:
      #java_home was specified among the command-line arguments. Use it as custom JDK location.
      if not validate_jdk(args.java_home):
        err = "Path to java home " + args.java_home + " or java binary file does not exists"
        raise FatalException(1, err)

      print_warning_msg("JAVA_HOME " + args.java_home + " must be valid on ALL hosts")
      print_warning_msg(jcePolicyWarn)
      IS_CUSTOM_JDK = True

      properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
      properties.removeOldProp(JDK_NAME_PROPERTY)
      properties.removeOldProp(JCE_NAME_PROPERTY)

      if not ambariOnly:
        properties.process_pair(STACK_JAVA_HOME_PROPERTY, args.java_home)
        properties.removeOldProp(STACK_JDK_NAME_PROPERTY)
        properties.removeOldProp(STACK_JCE_NAME_PROPERTY)

      self._ensure_java_home_env_var_is_set(args.java_home)
      self.jdk_index = self.custom_jdk_number

      if args.stack_java_home: # reset stack specific jdk properties if stack_java_home exists
        print 'Setting JAVA_HOME for stack services...'
        print_warning_msg("JAVA_HOME " + args.stack_java_home + " (Stack) must be valid on ALL hosts")
        print_warning_msg(jcePolicyWarn)
        properties.process_pair(STACK_JAVA_HOME_PROPERTY, args.stack_java_home)
        properties.removeOldProp(STACK_JDK_NAME_PROPERTY)
        properties.removeOldProp(STACK_JCE_NAME_PROPERTY)

      return

    java_home_var = get_JAVA_HOME()
    if OS_FAMILY == OSConst.WINSRV_FAMILY:
      progress_func = None
    else:
      progress_func = download_progress

    if java_home_var:
      message = "Do you want to change Oracle JDK [y/n] (n)? "
      if ambariOnly:
        message = "Do you want to change Oracle JDK for Ambari Server [y/n] (n)? "
      change_jdk = get_YN_input(message, False)
      if not change_jdk:
        self._ensure_java_home_env_var_is_set(java_home_var)
        self.jdk_index = self.custom_jdk_number
        return

    #Continue with the normal setup, taking the first listed JDK version as the default option
    jdk_num = str(self.jdk_index + 1)
    (self.jdks, jdk_choice_prompt, jdk_valid_choices, self.custom_jdk_number) = self._populate_jdk_configs(properties, jdk_num, ambariOnly)

    jdk_num = get_validated_string_input(
      jdk_choice_prompt,
      jdk_num,
      jdk_valid_choices,
      "Invalid number.",
      False
    )

    self.jdk_index = int(jdk_num) - 1

    if self.jdk_index == self.custom_jdk_number:
      print_warning_msg("JDK must be installed on all hosts and JAVA_HOME must be valid on all hosts.")
      print_warning_msg(jcePolicyWarn)

      if get_silent():
        print_error_msg("Path to JAVA_HOME should be specified via -j option.")
        sys.exit(1)

      args.java_home = get_validated_string_input("Path to JAVA_HOME: ", None, None, None, False, False)
      if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, "bin", self.JAVA_BIN)):
        err = "Java home path or java binary file is unavailable. Please put correct path to java home."
        raise FatalException(1, err)
      print "Validating JDK on Ambari Server...done."

      properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
      properties.removeOldProp(JDK_NAME_PROPERTY)
      properties.removeOldProp(JCE_NAME_PROPERTY)

      if not ambariOnly:
        properties.process_pair(STACK_JAVA_HOME_PROPERTY, args.java_home)
        properties.removeOldProp(STACK_JDK_NAME_PROPERTY)
        properties.removeOldProp(STACK_JCE_NAME_PROPERTY)

      # Make sure any previously existing JDK and JCE name properties are removed. These will
      # confuse things in a Custom JDK scenario
      properties.removeProp(JDK_NAME_PROPERTY)
      properties.removeProp(JCE_NAME_PROPERTY)
      if not ambariOnly:
        properties.removeOldProp(STACK_JDK_NAME_PROPERTY)
        properties.removeOldProp(STACK_JCE_NAME_PROPERTY)

      self._ensure_java_home_env_var_is_set(args.java_home)
      return

    jdk_cfg = self.jdks[self.jdk_index]

    resources_dir = get_resources_location(properties)

    dest_file = os.path.abspath(os.path.join(resources_dir, jdk_cfg.dest_file))
    if os.path.exists(dest_file):
      print "JDK already exists, using " + dest_file
    elif properties[JDK_DOWNLOAD_SUPPORTED_PROPERTY].upper() == "FALSE":
      print "ERROR: Oracle JDK is not found in {1}. JDK download is not supported in this distribution. Please download Oracle JDK " \
            "archive ({0}) manually from Oracle site, place it into {1} and re-run this script.".format(jdk_cfg.dest_file, dest_file)
      print "NOTE: If you have already downloaded the file, please verify if the name is exactly same as {0}.".format(jdk_cfg.dest_file)
      print 'Exiting...'
      sys.exit(1)
    else:
      ok = get_YN_input("To download the Oracle JDK and the Java Cryptography Extension (JCE) "
                        "Policy Files you must accept the "
                        "license terms found at "
                        "http://www.oracle.com/technetwork/java/javase/"
                        "terms/license/index.html and not accepting will "
                        "cancel the Ambari Server setup and you must install the JDK and JCE "
                        "files manually.\nDo you accept the "
                        "Oracle Binary Code License Agreement [y/n] (y)? ", True)
      if not ok:
        print 'Exiting...'
        sys.exit(1)

      jdk_url = jdk_cfg.url

      print 'Downloading JDK from ' + jdk_url + ' to ' + dest_file
      self._download_jdk(jdk_url, dest_file, progress_func)

    try:
      (retcode, out, java_home_dir) = self._install_jdk(dest_file, jdk_cfg)
    except Exception, e:
      print "Installation of JDK has failed: %s\n" % str(e)
      file_exists = os.path.isfile(dest_file)
      if file_exists:
        ok = get_YN_input("JDK found at " + dest_file + ". "
                          "Would you like to re-download the JDK [y/n] (y)? ", not get_silent())
        if not ok:
          err = "Unable to install JDK. Please remove JDK file found at " + \
                dest_file + " and re-run Ambari Server setup"
          raise FatalException(1, err)
        else:
          jdk_url = jdk_cfg.url

          print 'Re-downloading JDK from ' + jdk_url + ' to ' + dest_file
          self._download_jdk(jdk_url, dest_file, progress_func)
          print 'Successfully re-downloaded JDK distribution to ' + dest_file

          try:
            (retcode, out) = self._install_jdk(dest_file, jdk_cfg)
          except Exception, e:
            print "Installation of JDK was failed: %s\n" % str(e)
            err = "Unable to install JDK. Please remove JDK, file found at " + \
                  dest_file + " and re-run Ambari Server setup"
            raise FatalException(1, err)

      else:
        err = "Unable to install JDK. File " + dest_file + " does not exist, " \
                                                           "please re-run Ambari Server setup"
        raise FatalException(1, err)

    properties.process_pair(JDK_NAME_PROPERTY, jdk_cfg.dest_file)
    properties.process_pair(JAVA_HOME_PROPERTY, java_home_dir)
    if not ambariOnly:
      properties.process_pair(STACK_JDK_NAME_PROPERTY, jdk_cfg.dest_file)
      properties.process_pair(STACK_JAVA_HOME_PROPERTY, java_home_dir)

    self._ensure_java_home_env_var_is_set(java_home_dir)

  def download_and_unpack_jce_policy(self, properties, ambariOnly = False):
    err_msg_stdout = "JCE Policy files are required for secure HDP setup. Please ensure " \
              " all hosts have the JCE unlimited strength policy 6, files."

    resources_dir = get_resources_location(properties)

    jdk_cfg = self.jdks[self.jdk_index]

    try:
      JDKSetup._download_jce_policy(jdk_cfg.jcpol_url, jdk_cfg.dest_jcpol_file, resources_dir, properties, ambariOnly)
    except FatalException, e:
      print err_msg_stdout
      print_error_msg("Failed to download JCE policy files:")
      if e.reason is not None:
        print_error_msg("\nREASON: {0}".format(e.reason))
        # TODO: We don't fail installation if _download_jce_policy fails. Is it OK?

    print 'Installing JCE policy...'
    try:
      jdk_path = properties.get_property(JAVA_HOME_PROPERTY)
      JDKSetup.unpack_jce_policy(jdk_path, resources_dir, jdk_cfg.dest_jcpol_file)
      self.adjust_jce_permissions(jdk_path)
    except FatalException, e:
      print err_msg_stdout
      print_error_msg("Failed to install JCE policy files:")
      if e.reason is not None:
        print_error_msg("\nREASON: {0}".format(e.reason))
        # TODO: We don't fail installation if _download_jce_policy fails. Is it OK?

  @staticmethod
  def unpack_jce_policy(jdk_path, resources_dir, jce_packed_file):
    jdk_security_path = os.path.abspath(os.path.join(jdk_path, configDefaults.JDK_SECURITY_DIR))

    jce_zip_path = os.path.abspath(os.path.join(resources_dir, jce_packed_file))
    expand_jce_zip_file(jce_zip_path, jdk_security_path)

  def _populate_jdk_configs(self, properties, jdk_num, ambariOnly = False):
    def remove_jdk_condition(name):
      """
      Removes jdk1.7 from the default choices.
      This method can be removed if JDK 7 support (for stack services) will be dropped.
      """
      if name != "jdk1.7":
        return True
      else:
       print "JDK 7 detected. Removed from choices."
       return False
    if properties.has_key(JDK_RELEASES):
      jdk_names = properties[JDK_RELEASES].split(',')
      jdk_names = filter(None, jdk_names)
      if ambariOnly:
        jdk_names = filter(lambda x : remove_jdk_condition(x), jdk_names)
      jdks = []
      for jdk_name in jdk_names:
        jdkR = JDKRelease.from_properties(properties, jdk_name)
        jdks.append(jdkR)
    else:
      jdks = self.JDK_DEFAULT_CONFIGS

    n_config = 1
    jdk_choice_prompt = ''
    jdk_choices = ''
    for jdk in jdks:
      jdk_choice_prompt += self.JDK_PROMPT.format(n_config, jdk.desc)
      jdk_choices += str(n_config)
      n_config += 1

    jdk_choice_prompt += self.JDK_CUSTOM_CHOICE_PROMPT.format(n_config, jdk_num)
    jdk_valid_choices = self.JDK_VALID_CHOICES.format(jdk_choices, n_config)

    return jdks, jdk_choice_prompt, jdk_valid_choices, n_config - 1

  def _download_jdk(self, jdk_url, dest_file, progress_func = None):
    jdk_download_fail_msg = " Failed to download JDK: {0}. Please check that the " \
                            "JDK is available at {1}. Also you may specify JDK file " \
                            "location in local filesystem using --jdk-location command " \
                            "line argument.".format("{0}", jdk_url)
    try:
      force_download_file(jdk_url, dest_file, progress_func = progress_func)

      print 'Successfully downloaded JDK distribution to ' + dest_file
    except FatalException:
      raise
    except Exception, e:
      err = jdk_download_fail_msg.format(str(e))
      raise FatalException(1, err)

  @staticmethod
  def _download_jce_policy(jcpol_url, dest_jcpol_file, resources_dir, properties, ambariOnly = False):
    dest_file = os.path.abspath(os.path.join(resources_dir, dest_jcpol_file))

    if not os.path.exists(dest_file):
      if properties[JCE_DOWNLOAD_SUPPORTED_PROPERTY].upper() == "FALSE":
        print "ERROR: JCE Policy archive is not found in {1}. JCE Policy archive download is not supported in this distribution. " \
          "Please download JCE Policy archive ({0}) from Oracle site, place it into {1} and re-run this script.".format(dest_jcpol_file, dest_file)
        print 'Exiting...'
        sys.exit(1)
      print 'Downloading JCE Policy archive from ' + jcpol_url + ' to ' + dest_file
      try:
        force_download_file(jcpol_url, dest_file)

        print 'Successfully downloaded JCE Policy archive to ' + dest_file
      except FatalException:
        raise
      except Exception, e:
        err = 'Failed to download JCE Policy archive: ' + str(e)
        raise FatalException(1, err)
    else:
      print "JCE Policy archive already exists, using " + dest_file

    properties.process_pair(JCE_NAME_PROPERTY, dest_jcpol_file)
    if not ambariOnly:
      properties.process_pair(STACK_JCE_NAME_PROPERTY, dest_jcpol_file)

  # Base implementation, overriden in the subclasses
  def _install_jdk(self, java_inst_file, java_home_dir):
    pass

  def adjust_jce_permissions(self, jdk_path):
    pass

  # Base implementation, overriden in the subclasses
  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    pass

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class JDKSetupWindows(JDKSetup):
  def __init__(self):
    super(JDKSetupWindows, self).__init__()
    self.JDK_DEFAULT_CONFIGS = [
      JDKRelease("jdk7.67", "Oracle JDK 1.7.67",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-7u67-windows-x64.exe", "jdk-7u67-windows-x64.exe",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/UnlimitedJCEPolicyJDK7.zip", "UnlimitedJCEPolicyJDK7.zip",
                 "C:\\jdk1.7.0_67",
                 "Creating (jdk.*)/jre")
    ]

    self.jdks = self.JDK_DEFAULT_CONFIGS
    self.custom_jdk_number = len(self.jdks)

    self.JAVA_BIN = "java.exe"

  def _install_jdk(self, java_inst_file, jdk_cfg):
    jdk_inst_dir = jdk_cfg.inst_dir
    print "Installing JDK to {0}".format(jdk_inst_dir)

    if not os.path.exists(jdk_inst_dir):
      os.makedirs(jdk_inst_dir)

    if java_inst_file.endswith(".exe"):
      (dirname, filename) = os.path.split(java_inst_file)
      installLogFilePath = os.path.join(configDefaults.OUT_DIR, filename + "-install.log")
      #jre7u67.exe /s INSTALLDIR=<dir> STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-server\\jre7u67.exe-install.log
      installCmd = [
        java_inst_file,
        "/s",
        "INSTALLDIR=" + jdk_inst_dir,
        "STATIC=1",
        "WEB_JAVA=0",
        "/L",
        installLogFilePath
      ]
      retcode, out, err = run_os_command(installCmd)
      #TODO: support .msi file installations
      #msiexec.exe jre.msi /s INSTALLDIR=<dir> STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-server\\jre7u67-install.log ?
    else:
      err = "JDK installation failed.Unknown file mask."
      raise FatalException(1, err)

    if retcode == 1603:
      # JDK already installed
      print "JDK already installed in {0}".format(jdk_inst_dir)
      retcode = 0
    else:
      if retcode != 0:
        err = "Installation of JDK returned exit code %s" % retcode
        raise FatalException(retcode, err)

      print "Successfully installed JDK to {0}".format(jdk_inst_dir)

    # Don't forget to adjust the JAVA_HOME env var

    return (retcode, out, jdk_inst_dir)

  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    if not os.environ.has_key(JAVA_HOME) or os.environ[JAVA_HOME] != java_home_dir:
      java_home_dir_unesc = compress_backslashes(java_home_dir)
      retcode, out, err = run_os_command("SETX {0} {1} /M".format(JAVA_HOME, java_home_dir_unesc))
      if retcode != 0:
        print_warning_msg("SETX output: " + out)
        print_warning_msg("SETX error output: " + err)
        err = "Setting JAVA_HOME failed. Exit code={0}".format(retcode)
        raise FatalException(1, err)

      os.environ[JAVA_HOME] = java_home_dir

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class JDKSetupLinux(JDKSetup):
  def __init__(self):
    super(JDKSetupLinux, self).__init__()
    self.JDK_DEFAULT_CONFIGS = [
      JDKRelease("jdk1.8", "Oracle JDK 1.8 + Java Cryptography Extension (JCE) Policy Files 8",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-8u112-linux-x64.tar.gz", "jdk-8u112-linux-x64.tar.gz",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jce_policy-8.zip", "jce_policy-8.zip",
                 AmbariPath.get("/usr/jdk64/jdk1.8.0_112"),
                 "(jdk.*)/jre")
    ]

    self.jdks = self.JDK_DEFAULT_CONFIGS
    self.custom_jdk_number = len(self.jdks)

    self.JAVA_BIN = "java"

    self.CREATE_JDK_DIR_CMD = "/bin/mkdir -p {0}"
    self.CHMOD_JDK_DIR_CMD = "chmod a+x {0}"
    self.SET_JCE_PERMISSIONS = "chown {0} {1}/{2}/*"
    self.SET_JCE_JAR_MODE = "chmod 664 {0}/{1}/{2}"
    self.SET_JCE_FILE_MODE = "chmod 644 {0}/{1}/{2}"
    # use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
    self.UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"

  def _install_jdk(self, java_inst_file, jdk_cfg):
    jdk_inst_dir = jdk_cfg.inst_dir
    print "Installing JDK to {0}".format(jdk_inst_dir)

    retcode, out, err = run_os_command(self.CREATE_JDK_DIR_CMD.format(jdk_inst_dir))
    retcode, out, err = run_os_command(self.CHMOD_JDK_DIR_CMD.format(jdk_inst_dir))
    savedPath = os.getcwd()
    os.chdir(jdk_inst_dir)

    try:
      if java_inst_file.endswith(".gz"):
        retcode, out, err = run_os_command(self.UNTAR_JDK_ARCHIVE.format(java_inst_file))
      else:
        err = "JDK installation failed.Unknown file extension."
        raise FatalException(1, err)
    finally:
      os.chdir(savedPath)

    if retcode != 0:
      err = "Installation of JDK returned exit code %s" % retcode
      raise FatalException(retcode, err)

    jdk_version = re.search(jdk_cfg.reg_exp, out).group(1)
    java_home_dir = os.path.join(jdk_inst_dir, jdk_version)

    print "Successfully installed JDK to {0}".format(jdk_inst_dir)
    return (retcode, out, java_home_dir)

  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    #No way to do this in Linux. Best we can is to set the process environment variable.
    os.environ[JAVA_HOME] = java_home_dir

  def adjust_jce_permissions(self, jdk_path):
    ambari_user = read_ambari_user()
    cmds = []
    if ambari_user:
      cmds.append(self.SET_JCE_PERMISSIONS.format(ambari_user, jdk_path, configDefaults.JDK_SECURITY_DIR))
    cmds.append(self.SET_JCE_FILE_MODE.format(jdk_path, configDefaults.JDK_SECURITY_DIR, "*"))
    cmds.append(self.SET_JCE_JAR_MODE.format(jdk_path, configDefaults.JDK_SECURITY_DIR, "*.jar"))

    cmd = " && ".join(cmds)

    process = subprocess32.Popen(cmd,
                           stdout=subprocess32.PIPE,
                           stdin=subprocess32.PIPE,
                           stderr=subprocess32.PIPE,
                           shell=True
                           )
    (stdoutdata, stderrdata) = process.communicate()

    if process.returncode != 0:
      print_warning_msg("Failed to change jce permissions. {0}\n{1}".format(stderrdata, stdoutdata))

def download_and_install_jdk(options):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)

  jdkSetup = JDKSetup()
  jdkSetup.download_and_install_jdk(options, properties)

  if jdkSetup.jdk_index != jdkSetup.custom_jdk_number:
    jdkSetup.download_and_unpack_jce_policy(properties)

  update_properties(properties)

  ambari_java_version_valid = check_ambari_java_version_is_valid(get_JAVA_HOME(), jdkSetup.JAVA_BIN, 8, properties)
  if not ambari_java_version_valid:
    jdkSetup = JDKSetup() # recreate object
    jdkSetup.download_and_install_jdk(options, properties, True)
    if jdkSetup.jdk_index != jdkSetup.custom_jdk_number:
      jdkSetup.download_and_unpack_jce_policy(properties, True)
    update_properties(properties)

  return 0


#
# Configures the OS settings in ambari properties.
#
def configure_os_settings():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  try:
    conf_os_type = properties[OS_TYPE_PROPERTY]
    if conf_os_type != '':
      print_info_msg("os_type already set in the properties file")
      return 0
  except (KeyError):
    print_error_msg("os_type is not set in the properties file. Setting it now.")

  # to check server/agent compatibility
  master_os_family = OS_FAMILY + OS_VERSION
  # to check supported os_types
  master_os_type = OS_TYPE + OS_VERSION

  write_property(OS_FAMILY_PROPERTY, master_os_family)
  write_property(OS_TYPE_PROPERTY, master_os_type)
  return 0


#
# JDBC
#

def _check_jdbc_options(options):
  return (options.jdbc_driver is not None and options.jdbc_db is not None)

def setup_jdbc(args):
  if not os.path.isfile(args.jdbc_driver):
    err = "File {0} does not exist!".format(args.jdbc_driver)
    raise FatalException(1, err)

  if args.jdbc_db not in JDBC_DB_OPTION_VALUES:
    err = "Unsupported database name {0}. Please see help for more information.".format(args.jdbc_db)
    raise FatalException(1, err)

  _cache_jdbc_driver(args)

# No JDBC driver caching in Windows at this point. Will cache it along with the integrated authentication dll into a
#  zip archive at a later moment.
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _cache_jdbc_driver(args):
  pass

#TODO JDBC driver caching almost duplicates the LinuxDBMSConfig._install_jdbc_driver() functionality
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def _cache_jdbc_driver(args):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)

  resources_dir = get_resources_location(properties)
  path, jdbc_name = os.path.split(args.jdbc_driver)
  custom_db_jdbc_property_name = "custom." + args.jdbc_db + ".jdbc.name"
  custom_db_jdbc_property_value = properties[custom_db_jdbc_property_name]

  symlink_name = os.path.join(resources_dir, default_connectors_map[args.jdbc_db])

  if custom_db_jdbc_property_value:
    properties.process_pair("previous." + custom_db_jdbc_property_name, custom_db_jdbc_property_value)
  else:
    if os.path.lexists(symlink_name):
      properties.process_pair("previous." + custom_db_jdbc_property_name, default_connectors_map[args.jdbc_db])

  properties.process_pair(custom_db_jdbc_property_name, jdbc_name)

  dest = os.path.join(resources_dir, jdbc_name)
  if os.path.isfile(dest):
    os.remove(dest)

  try:
    shutil.copy(args.jdbc_driver, dest)
    print "Copying {0} to {1}".format(args.jdbc_driver, dest)
  except Exception, e:
    err = "Cannot copy file {0} to {1} due to: {2} . Please check file " \
          "permissions and free disk space.".format(args.jdbc_driver, dest, str(e))
    raise FatalException(1, err)

  try:
    if (dest != symlink_name):
      if os.path.isfile(symlink_name):
        os.remove(symlink_name)
      os.symlink(dest, symlink_name)
      print "Creating symlink {0} to {1}".format(dest, symlink_name)
  except Exception, e:
    err = "Cannot create symlink {0} to {1} due to: {2} . Please check file " \
          "permissions and free disk space.".format(dest, symlink_name, str(e))
    raise FatalException(1, err)

  update_properties(properties)
  print "If you are updating existing jdbc driver jar for " + args.jdbc_db + " with " + jdbc_name + ". Please remove the " \
        "old driver jar, from all hosts. Restarting services that need the driver, will " \
        "automatically copy the new jar to the hosts."
  print "JDBC driver was successfully initialized."

#
# Database
#

# Ask user for database connection properties
def prompt_db_properties(options):
  factory = DBMSConfigFactory()

  if not factory.force_dbms_setup():
    ok = False
    if options.must_set_database_options:
      ok = get_YN_input("Enter advanced database configuration [y/n] (n)? ", False)
  else:
    ok = True

  print 'Configuring database...'

  options.must_set_database_options = ok
  options.database_index = factory.select_dbms(options)

def _setup_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")

  factory = DBMSConfigFactory()

  dbmsAmbari = factory.create(options, properties, "Ambari")
  resultA = dbmsAmbari.configure_database(properties, options)

  # Now save the properties file
  if resultA:
    update_properties(properties)

    dbmsAmbari.setup_database()

def _createDefDbFactory(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")
  if not (properties.getPropertyDict().has_key(JDBC_URL_PROPERTY) and
            properties.getPropertyDict().has_key(JDBC_RCA_URL_PROPERTY)):
    raise FatalException(-1, "Ambari Server not set up yet. Nothing to reset.")

  empty_options = optparse.Values()
  empty_options.must_set_database_options = options.must_set_database_options
  empty_options.database_index = options.database_index
  empty_options.database_host = ""
  empty_options.database_port = ""
  empty_options.database_name = ""
  empty_options.database_windows_auth = False
  empty_options.database_username = ""
  empty_options.database_password = ""
  empty_options.init_db_script_file = ""
  empty_options.cleanup_db_script_file = ""

  factory = DBMSConfigFactory()

  return empty_options, factory, properties

def _reset_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
  if persistence_type == "remote":
      err = 'Ambari doesn\'t support resetting exernal DB automatically. ' \
            'To reset Ambari Server schema you must first drop and then create it ' \
            'using DDL scripts from "{0}"'.format(AmbariPath.get("/var/lib/ambari-server/resources/"))
      raise FatalException(1, err)
  else:
    factory = DBMSConfigFactory()

    dbmsAmbari = factory.create(options, properties)
    dbmsAmbari.reset_database()


#
# Extract the system views
#
def extract_views(options):
  java_exe_path = get_java_exe_path()
  if java_exe_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1

  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  vdir = get_value_from_properties(properties, VIEWS_DIR_PROPERTY, configDefaults.DEFAULT_VIEWS_DIR)

  files = [f for f in os.listdir(vdir) if os.path.isfile(os.path.join(vdir,f))]
  serverClassPath = ServerClassPath(get_ambari_properties(), options)
  for f in files:
    command = VIEW_EXTRACT_CMD.format(java_exe_path,
                                      serverClassPath.get_full_ambari_classpath_escaped_for_shell(), os.path.join(vdir,f))
    retcode, stdout, stderr = run_os_command(command)
    if retcode == 0:
      sys.stdout.write(f + "\n")
    elif retcode == 2:
      sys.stdout.write("Error extracting " + f + "\n")
    else:
      sys.stdout.write(".")
      sys.stdout.flush()

    print_info_msg("Return code from extraction of view archive " + f + ": " +
                   str(retcode))

  sys.stdout.write("\n")
  return 0


def expand_jce_zip_file(jce_zip_path, jdk_security_path):
  f = None
  import zipfile

  if os.path.exists(jdk_security_path) and os.path.exists(jce_zip_path):
    try:
      f = zipfile.ZipFile(jce_zip_path, "r")
      zip_members = f.namelist()
      for member in zip_members:
        if member.endswith(os.sep):
          os.makedirs(os.path.join(jdk_security_path, member))
        else:
          f.extract(member, jdk_security_path)
      unziped_jce_path = os.path.split(zip_members[len(zip_members) - 1])[0]
    finally:
      try:
        f.close()
      except Exception as e:
        err = "Fail during the extraction of {0}.".format(jce_zip_path)
        raise FatalException(1, err)
  else:
    err = "The path {0} or {1} is invalid.".format(jdk_security_path, jce_zip_path)
    raise FatalException(1, err)

  if unziped_jce_path:
    from_path = os.path.join(jdk_security_path, unziped_jce_path)
    jce_files = os.listdir(from_path)
    for i in range(len(jce_files)):
      jce_files[i] = os.path.join(from_path, jce_files[i])

    copy_files(jce_files, jdk_security_path)
    dir_to_delete = os.path.join(jdk_security_path, unziped_jce_path.split(os.sep)[0])
    shutil.rmtree(dir_to_delete)


def check_setup_already_done():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  return not bool(get_missing_properties(properties, property_set=SETUP_DONE_PROPERTIES))

#
# Setup the Ambari Server.
#
def setup(options):
  logger.info("Setup ambari-server.")
  if options.only_silent:
    if check_setup_already_done():
      print "Nothing was done. Ambari Setup already performed and cannot re-run setup in silent mode. Use \"ambari-server setup\" command without -s option to change Ambari setup."
      sys.exit(0)

  retcode = verify_setup_allowed(options)
  if not retcode == 0:
    raise FatalException(1, None)

  if not is_root():
    warn_msg = configDefaults.MESSAGE_WARN_SETUP_NOT_ROOT
    print warn_msg

  # proceed jdbc properties if they were set
  if _check_jdbc_options(options):
    setup_jdbc(options)
    return

  (retcode, err) = disable_security_enhancements()
  if not retcode == 0:
    raise FatalException(retcode, err)

  #Create ambari user, if needed
  (retcode, register_service, svc_user, svc_password) = check_ambari_user(options)
  if not retcode == 0:
    err = 'Failed to create user. Exiting.'
    raise FatalException(retcode, err)

  print configDefaults.MESSAGE_CHECK_FIREWALL
  check_firewall()

  print 'Checking JDK...'
  try:
    download_and_install_jdk(options)
  except FatalException as e:
    err = 'Downloading or installing JDK failed: {0}. Exiting.'.format(e)
    raise FatalException(e.code, err)

  print 'Checking GPL software agreement...'
  write_gpl_license_accepted(default_prompt_value=options.accept_gpl)

  print 'Completing setup...'
  retcode = configure_os_settings()
  if not retcode == 0:
    err = 'Configure of OS settings in ambari.properties failed. Exiting.'
    raise FatalException(retcode, err)

  print 'Configuring database...'
  prompt_db_properties(options)

  #DB setup should be done last after doing any setup.

  _setup_database(options)

  check_jdbc_drivers(options)

  if not options.skip_view_extraction:
    print 'Extracting system views...'
    retcode = extract_views(options)
    if not retcode == 0:
      err = 'Error while extracting system views. Exiting'
      raise FatalException(retcode, err)

  json_url = get_json_url_from_repo_file()
  if json_url:
    print "Ambari repo file contains latest json url {0}, updating stacks repoinfos with it...".format(json_url)
    properties = get_ambari_properties()
    stack_root = get_stack_location(properties)
    update_latest_in_repoinfos_for_stacks(stack_root, json_url)
  else:
    print "Ambari repo file doesn't contain latest json url, skipping repoinfos modification"

  # we've already done this, but new files were created so run it one time.
  adjust_directory_permissions(svc_user)

  service_setup(register_service, svc_user, svc_password)

#
# Setup the JCE policy for Ambari Server.
#
def setup_jce_policy(args):
  logger.info("Setup JCE policy for ambari-server.")
  if not os.path.exists(args[1]):
    err = "Can not run 'setup-jce'. Invalid path {0}.".format(args[1])
    raise FatalException(1, err)

  properties = get_ambari_properties()
  resources_dir = get_resources_location(properties)

  zip_path = os.path.split(args[1])
  zip_dir = zip_path[0]

  if not zip_dir == resources_dir:
    try:
      shutil.copy(args[1], resources_dir)
    except Exception as e:
      err = "Fail while trying to copy {0} to {1}. {2}".format(args[1], resources_dir, e)
      raise FatalException(1, err)

  jdk_path = properties.get_property(JAVA_HOME_PROPERTY)
  if not jdk_path or not os.path.exists(jdk_path):
    err = "JDK not installed, you need to run 'ambari-server setup' before attempting to install the JCE policy."
    raise FatalException(1, err)

  zip_name = zip_path[1]
  properties.process_pair(JCE_NAME_PROPERTY, zip_name)

  print 'Installing JCE policy...'
  try:
    JDKSetup.unpack_jce_policy(jdk_path, resources_dir, zip_name)
  except FatalException as e:
    err = 'Installing JCE failed: {0}. Exiting.'.format(e)
    raise FatalException(e.code, err)

  update_properties(properties)

  print 'NOTE: Restart Ambari Server to apply changes' + \
        ' ("ambari-server restart|stop|start")'

def check_ambari_java_version_is_valid(java_home, java_bin, min_version, properties):
  """
  Check that ambari uses the proper (minimum) JDK with a shell command.
  Returns true, if Ambari meets with the minimal JDK version requirement.
  """
  result = True
  print 'Check JDK version for Ambari Server...'
  try:
    command = JDK_VERSION_CHECK_CMD.format(os.path.join(java_home, 'bin', java_bin))
    process = subprocess32.Popen(command,
                               stdout=subprocess32.PIPE,
                               stdin=subprocess32.PIPE,
                               stderr=subprocess32.PIPE,
                               shell=True
                               )
    (out, err) = process.communicate()
    if process.returncode != 0:
      err = "Checking JDK version command returned with exit code %s" % process.returncode
      raise FatalException(process.returncode, err)
    else:
      actual_jdk_version = int(out)
      print 'JDK version found: {0}'.format(actual_jdk_version)
      if actual_jdk_version < min_version:
        print 'Minimum JDK version is {0} for Ambari. Setup JDK again only for Ambari Server.'.format(min_version)
        properties.process_pair(STACK_JAVA_VERSION, out)
        result = False
      else:
        print 'Minimum JDK version is {0} for Ambari. Skipping to setup different JDK for Ambari Server.'.format(min_version)

  except FatalException as e:
    err = 'Running java version check command failed: {0}. Exiting.'.format(e)
    raise FatalException(e.code, err)
  except Exception as e:
    err = 'Running java version check command failed: {0}. Exiting.'.format(e)
    raise FatalException(1, err)

  return result
#
# Resets the Ambari Server.
#
def reset(options):
  logger.info("Reset ambari-server.")
  if not is_root():
    err = configDefaults.MESSAGE_ERROR_RESET_NOT_ROOT
    raise FatalException(4, err)

  status, stateDesc = is_server_runing()
  if status:
    err = 'Ambari-server must be stopped to reset'
    raise FatalException(1, err)

  #force reset if silent option provided
  if get_silent():
    default = "yes"
  else:
    default = "no"

  choice = get_YN_input("**** WARNING **** You are about to reset and clear the "
                        "Ambari Server database. This will remove all cluster "
                        "host and configuration information from the database. "
                        "You will be required to re-configure the Ambari server "
                        "and re-run the cluster wizard. \n"
                        "Are you SURE you want to perform the reset "
                        "[yes/no] ({0})? ".format(default), get_silent())
  okToRun = choice
  if not okToRun:
    err = "Ambari Server 'reset' cancelled"
    raise FatalException(1, err)

  _reset_database(options)
  pass
