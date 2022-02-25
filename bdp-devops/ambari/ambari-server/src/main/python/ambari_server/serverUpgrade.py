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

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
import sys
import shutil
import base64
import urllib2
import re
import glob
import optparse
import logging
import ambari_simplejson as json

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg, print_error_msg, get_verbose
from ambari_commons.os_utils import is_root, run_os_command
from ambari_server.dbConfiguration import DBMSConfigFactory, CUSTOM_JDBC_DB_NAMES, TAR_GZ_ARCHIVE_TYPE,  check_jdbc_drivers, \
  get_jdbc_driver_path, ensure_jdbc_driver_is_installed, LINUX_DBMS_KEYS_LIST, default_connectors_map
from ambari_server.properties import Properties
from ambari_server.serverConfiguration import configDefaults, get_resources_location, update_properties, \
  check_database_name_property, get_ambari_properties, get_ambari_version, \
  get_java_exe_path, get_stack_location, parse_properties_file, read_ambari_user, update_ambari_properties, \
  update_database_name_property, get_admin_views_dir, get_views_dir, get_views_jars, \
  AMBARI_PROPERTIES_FILE, CLIENT_SECURITY, RESOURCES_DIR_PROPERTY, GPL_LICENSE_ACCEPTED_PROPERTY, \
  SETUP_OR_UPGRADE_MSG, update_krb_jaas_login_properties, AMBARI_KRB_JAAS_LOGIN_FILE, get_db_type, update_ambari_env, \
  AMBARI_ENV_FILE, JDBC_DATABASE_PROPERTY, get_default_views_dir, write_gpl_license_accepted, set_property
from ambari_server.setupSecurity import adjust_directory_permissions, \
  generate_env, ensure_can_start_under_current_user
from ambari_server.utils import compare_versions, get_json_url_from_repo_file, update_latest_in_repoinfos_for_stacks
from ambari_server.serverUtils import is_server_runing, get_ambari_server_api_base, get_ssl_context
from ambari_server.userInput import get_validated_string_input, get_prompt_default, read_password, get_YN_input
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.setupMpacks import replay_mpack_logs
from ambari_commons.logging_utils import get_debug_mode, set_debug_mode_from_options, get_silent

logger = logging.getLogger(__name__)

# constants
STACK_NAME_VER_SEP = "-"

SCHEMA_UPGRADE_HELPER_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
                            " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SCHEMA_UPGRADE_HELPER_CMD_DEBUG = "{0} " \
                         "-server -XX:NewRatio=2 " \
                         "-XX:+UseConcMarkSweepGC " + \
                         " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005," \
                         "server=y,suspend={2} " \
                         "-cp {1} " + \
                         "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
                         " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SCHEMA_UPGRADE_DEBUG = False

SUSPEND_START_MODE = False

INSTALLED_LZO_WITHOUT_GPL_TEXT = "By saying no, Ambari will not automatically install LZO on any new host in the cluster.  " \
                                "It is up to you to ensure LZO is installed and configured appropriately.  " \
                                "Without LZO being installed and configured, data compressed with LZO will not be readable.  " \
                                "Are you sure you want to proceed? [y/n] (n)? "

LZO_ENABLED_GPL_TEXT = "GPL License for LZO: https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html\n" \
                       "Your cluster is configured to use LZO which is GPL software. " \
                       "You must agree to enable Ambari to continue downloading and installing LZO  [y/n] (n)? "

def load_stack_values(version, filename):
  import xml.etree.ElementTree as ET
  values = {}
  root = ET.parse(filename).getroot()
  for ostag in root:
    ostype = ostag.attrib['type']
    for repotag in ostag:
      reponametag = repotag.find('reponame')
      repoidtag = repotag.find('repoid')
      baseurltag = repotag.find('baseurl')
      if reponametag is not None and repoidtag is not None and baseurltag is not None:
        key = "repo:/" + reponametag.text
        key += "/" + version
        key += "/" + ostype
        key += "/" + repoidtag.text
        key += ":baseurl"
        values[key] = baseurltag.text

  return values

#
# Repo upgrade
#

def change_objects_owner(args):
  print_info_msg('Fixing database objects owner', True)

  properties = Properties()   #Dummy, args contains the dbms name and parameters already

  factory = DBMSConfigFactory()
  dbms = factory.create(args, properties)

  dbms.change_db_files_owner()

#
# Schema upgrade
#

def run_schema_upgrade(args):
  db_title = get_db_type(get_ambari_properties()).title
  silent = get_silent()
  default_answer = 'y' if silent else 'n'
  default_value = silent
  confirm = get_YN_input("Ambari Server configured for %s. Confirm "
                         "you have made a backup of the Ambari Server database [y/n] (%s)? " % (db_title, default_answer), default_value)

  if not confirm:
    print_error_msg("Database backup is not confirmed")
    return 1

  jdk_path = get_java_exe_path()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1

  ensure_jdbc_driver_is_installed(args, get_ambari_properties())

  print_info_msg('Upgrading database schema', True)

  serverClassPath = ServerClassPath(get_ambari_properties(), args)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell(validate_classpath=True)

  set_debug_mode_from_options(args)
  debug_mode = get_debug_mode()
  debug_start = (debug_mode & 1) or SCHEMA_UPGRADE_DEBUG
  suspend_start = (debug_mode & 2) or SUSPEND_START_MODE
  suspend_mode = 'y' if suspend_start else 'n'
  command = SCHEMA_UPGRADE_HELPER_CMD_DEBUG.format(jdk_path, class_path, suspend_mode) if debug_start else SCHEMA_UPGRADE_HELPER_CMD.format(jdk_path, class_path)

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)
  environ = generate_env(args, ambari_user, current_user)

  (retcode, stdout, stderr) = run_os_command(command, env=environ)
  upgrade_response = json.loads(stdout)

  check_gpl_license_approved(upgrade_response)

  print_info_msg("Return code from schema upgrade command, retcode = {0}".format(str(retcode)), True)
  if stdout:
    print_info_msg("Console output from schema upgrade command:", True)
    print_info_msg(stdout, True)
    print
  if retcode > 0:
    print_error_msg("Error executing schema upgrade, please check the server logs.")
    if stderr:
      print_error_msg("Error output from schema upgrade command:")
      print_error_msg(stderr)
      print
  else:
    print_info_msg('Schema upgrade completed', True)
  return retcode

def check_gpl_license_approved(upgrade_response):
  if 'lzo_enabled' not in upgrade_response or upgrade_response['lzo_enabled'].lower() != "true":
    set_property(GPL_LICENSE_ACCEPTED_PROPERTY, "false", rewrite=False)
    return

  while not write_gpl_license_accepted(text = LZO_ENABLED_GPL_TEXT) and not get_YN_input(INSTALLED_LZO_WITHOUT_GPL_TEXT, False):
    pass

#
# Upgrades the Ambari Server.
#
def move_user_custom_actions():
  print_info_msg('Moving *.py files from custom_actions to custom_actions/scripts')
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    conf_file = properties.fileName
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    print_error_msg(err)
    raise FatalException(1, err)

  custom_actions_dir_path = os.path.join(resources_dir, 'custom_actions')
  custom_actions_scripts_dir_path = os.path.join(custom_actions_dir_path, 'scripts')
  print_info_msg('Moving *.py files from %s to %s' % (custom_actions_dir_path, custom_actions_scripts_dir_path))

  try:
    for custom_action_file_name in os.listdir(custom_actions_dir_path):
      custom_action_file_path = os.path.join(custom_actions_dir_path, custom_action_file_name)
      if os.path.isfile(custom_action_file_path) and custom_action_file_path.endswith('.py'):
        print_info_msg('Moving %s to %s' % (custom_action_file_path, custom_actions_scripts_dir_path))
        shutil.move(custom_action_file_path, custom_actions_scripts_dir_path)
  except (OSError, shutil.Error) as e:
    err = 'Upgrade failed. Can not move *.py files from %s to %s. ' % (custom_actions_dir_path, custom_actions_scripts_dir_path) + str(e)
    print_error_msg(err)
    raise FatalException(1, err)

def upgrade(args):
  print_info_msg("Upgrade Ambari Server", True)
  if not is_root():
    err = configDefaults.MESSAGE_ERROR_UPGRADE_NOT_ROOT
    raise FatalException(4, err)
  print_info_msg('Updating Ambari Server properties in {0} ...'.format(AMBARI_PROPERTIES_FILE), True)
  retcode = update_ambari_properties()
  if not retcode == 0:
    err = AMBARI_PROPERTIES_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  print_info_msg('Updating Ambari Server properties in {0} ...'.format(AMBARI_ENV_FILE), True)
  retcode = update_ambari_env()
  if not retcode == 0:
    err = AMBARI_ENV_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  retcode = update_krb_jaas_login_properties()
  if retcode == -2:
    pass  # no changes done, let's be silent
  elif retcode == 0:
    print_info_msg("File {0} updated.".format(AMBARI_KRB_JAAS_LOGIN_FILE), True)
  elif not retcode == 0:
    err = AMBARI_KRB_JAAS_LOGIN_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  restore_custom_services()
  replay_mpack_logs()
  try:
    update_database_name_property(upgrade=True)
  except FatalException:
    return -1

  # Ignore the server version & database options passed via command-line arguments
  parse_properties_file(args)

  #TODO check database version
  change_objects_owner(args)

  retcode = run_schema_upgrade(args)
  if not retcode == 0:
    print_error_msg("Ambari server upgrade failed. Please look at {0}, for more details.".format(configDefaults.SERVER_LOG_FILE))
    raise FatalException(11, 'Schema upgrade failed.')

  user = read_ambari_user()
  if user is None:
    warn = "Can not determine custom ambari user.\n" + SETUP_OR_UPGRADE_MSG
    print_warning_msg(warn)
  else:
    adjust_directory_permissions(user)

  # create jdbc symlinks if jdbc drivers are available in resources
  check_jdbc_drivers(args)

  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  # Move *.py files from custom_actions to custom_actions/scripts
  # This code exists for historic reasons in which custom action python scripts location changed from Ambari 1.7.0 to 2.0.0
  ambari_version = get_ambari_version(properties)
  if ambari_version is None:
    args.warnings.append("*.py files were not moved from custom_actions to custom_actions/scripts.")
  elif compare_versions(ambari_version, "2.0.0") == 0:
    move_user_custom_actions()

  # Move files installed by package to default views directory to a custom one
  for views_dir in get_views_dir(properties):
    root_views_dir = views_dir + "/../"

    if os.path.samefile(root_views_dir, get_default_views_dir()):
      continue

    for file in glob.glob(get_default_views_dir()+'/*'):
      shutil.move(file, root_views_dir)

  # Remove ADMIN_VIEW directory for upgrading Admin View on Ambari upgrade from 1.7.0 to 2.0.0
  admin_views_dirs = get_admin_views_dir(properties)
  for admin_views_dir in admin_views_dirs:
    shutil.rmtree(admin_views_dir)

  # Modify timestamp of views jars to current time
  views_jars = get_views_jars(properties)
  for views_jar in views_jars:
    os.utime(views_jar, None)

  # check if ambari is configured to use LDAP authentication
  if properties.get_property(CLIENT_SECURITY) == "ldap":
    args.warnings.append("LDAP authentication is detected. You must run the \"ambari-server setup-ldap\" command to adjust existing LDAP configuration.")

  # adding custom jdbc name and previous custom jdbc properties
  # we need that to support new dynamic jdbc names for upgraded ambari
  add_jdbc_properties(properties)

  json_url = get_json_url_from_repo_file()
  if json_url:
    print "Ambari repo file contains latest json url {0}, updating stacks repoinfos with it...".format(json_url)
    properties = get_ambari_properties()
    stack_root = get_stack_location(properties)
    update_latest_in_repoinfos_for_stacks(stack_root, json_url)
  else:
    print "Ambari repo file doesn't contain latest json url, skipping repoinfos modification"


def add_jdbc_properties(properties):
  for db_name in CUSTOM_JDBC_DB_NAMES:
    if db_name == "sqlanywhere":
      symlink_name = db_name + "-jdbc-driver" + TAR_GZ_ARCHIVE_TYPE
    else:
      symlink_name = db_name + "-jdbc-driver.jar"

    resources_dir = get_resources_location(properties)
    custom_db_jdbc_property_name = "custom." + db_name + ".jdbc.name"

    if os.path.lexists(os.path.join(resources_dir, symlink_name)):
      properties.process_pair(custom_db_jdbc_property_name, symlink_name)
      properties.process_pair("previous." + custom_db_jdbc_property_name, default_connectors_map[db_name])
      update_properties(properties)


#
# Set current cluster version (run Finalize during manual RU)
#
def set_current(options):
  logger.info("Set current cluster version.")
  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  finalize_options = SetCurrentVersionOptions(options)

  if finalize_options.no_finalize_options_set():
    err = 'Must specify --cluster-name and --version-display-name. Please invoke ambari-server.py --help to print the options.'
    raise FatalException(1, err)

  admin_login = get_validated_string_input(prompt="Enter Ambari Admin login: ", default=None,
                                           pattern=None, description=None,
                                           is_pass=False, allowEmpty=False)
  admin_password = get_validated_string_input(prompt="Enter Ambari Admin password: ", default=None,
                                              pattern=None, description=None,
                                              is_pass=True, allowEmpty=False)

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  base_url = get_ambari_server_api_base(properties)
  url = base_url + "clusters/{0}/stack_versions".format(finalize_options.cluster_name)
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')

  data = {
    "ClusterStackVersions": {
      "repository_version": finalize_options.desired_repo_version,
      "state": "CURRENT",
      "force": finalize_options.force_repo_version
    }
  }

  if get_verbose():
    sys.stdout.write('\nCalling API ' + url + ' : ' + str(data) + '\n')

  request.add_data(json.dumps(data))
  request.get_method = lambda: 'PUT'

  try:
    response = urllib2.urlopen(request, context=get_ssl_context(properties))
  except urllib2.HTTPError, e:
    code = e.getcode()
    content = e.read()
    err = 'Error during setting current version. Http status code - {0}. \n {1}'.format(
      code, content)
    raise FatalException(1, err)
  except Exception as e:
    err = 'Setting current version failed. Error details: %s' % e
    raise FatalException(1, err)

  sys.stdout.write('\nCurrent version successfully updated to ' + finalize_options.desired_repo_version)

  sys.stdout.write('\n')
  sys.stdout.flush()

#
# Search for folders with custom services and restore them from backup
#
def restore_custom_services():
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    conf_file = properties.fileName
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    print_error_msg(err)
    raise FatalException(1, err)

  stack_services_search_path = os.path.join("stacks","*","*","services","*")
  stack_old_dir_name = "stacks_*.old"
  stack_backup_services_search_path = os.path.join("*","*","services","*")
  stack_old_dir_mask = r'/stacks.*old/'
  stack_base_service_dir = '/stacks/'

  find_and_copy_custom_services(resources_dir, stack_services_search_path, stack_old_dir_name,
                                stack_backup_services_search_path, stack_old_dir_mask, stack_base_service_dir)

  common_services_search_path = os.path.join("common-services","*")
  common_old_dir_name = "common-services_*.old"
  common_backup_services_search_path = "*"
  common_old_dir_mask = r'/common-services.*old'
  common_base_service_dir = '/common-services/'

  find_and_copy_custom_services(resources_dir, common_services_search_path, common_old_dir_name,
                                common_backup_services_search_path, common_old_dir_mask, common_base_service_dir)


def find_and_copy_custom_services(resources_dir, services_search_path, old_dir_name, backup_services_search_path,
                                    old_dir_mask, base_service_dir):
  services = glob.glob(os.path.join(resources_dir, services_search_path))
  managed_services = []
  is_common_services_base_dir = "common-services" in base_service_dir
  for service in services:
    if os.path.isdir(service) and not os.path.basename(service) in managed_services:
      managed_services.append(os.path.basename(service))
  # add deprecated managed services
  managed_services.extend(["NAGIOS","GANGLIA","MAPREDUCE","WEBHCAT","AMBARI_INFRA"])

  stack_backup_dirs = glob.glob(os.path.join(resources_dir, old_dir_name))
  if stack_backup_dirs:
    last_backup_dir = max(stack_backup_dirs, key=os.path.getctime)
    backup_services = glob.glob(os.path.join(last_backup_dir, backup_services_search_path))

    regex = re.compile(old_dir_mask)
    for backup_service in backup_services:
      backup_base_service_dir = os.path.dirname(backup_service)
      current_base_service_dir = regex.sub(base_service_dir, backup_base_service_dir)
      # if services dir does not exists, we do not manage this stack
      if not os.path.exists(current_base_service_dir):
        continue

      # process dirs only
      if is_common_services_base_dir:
        version_dirs_in_backup_service_dir = glob.glob(os.path.join(backup_service,"*"))
        if os.path.isdir(backup_service) and not os.path.islink(backup_service):
          service_name = os.path.basename(backup_service)
          current_service_dir_path = os.path.join(current_base_service_dir, service_name)
          if not service_name in managed_services:
            if not os.path.exists(current_service_dir_path):
              os.makedirs(current_service_dir_path)
            for version_dir_path in version_dirs_in_backup_service_dir:
              if not os.path.islink(version_dir_path):
                version_dir =  os.path.basename(version_dir_path)
                shutil.copytree(version_dir_path, os.path.join(current_service_dir_path, version_dir))
      else:
        if os.path.isdir(backup_service) and not os.path.islink(backup_service):
          service_name = os.path.basename(backup_service)
          if not service_name in managed_services:
            shutil.copytree(backup_service, os.path.join(current_base_service_dir,service_name))



class SetCurrentVersionOptions:
  def __init__(self, options):
    try:
      self.cluster_name = options.cluster_name
    except AttributeError:
      self.cluster_name = None

    try:
      self.desired_repo_version = options.desired_repo_version
    except AttributeError:
      self.desired_repo_version = None

    try:
      self.force_repo_version = options.force_repo_version
    except AttributeError:
      self.force_repo_version = False

    if not self.force_repo_version:
      self.force_repo_version = False

  def no_finalize_options_set(self):
    return self.cluster_name is None or self.desired_repo_version is None
