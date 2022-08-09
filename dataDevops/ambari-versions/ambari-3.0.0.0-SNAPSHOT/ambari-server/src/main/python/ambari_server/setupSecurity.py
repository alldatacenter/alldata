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
import \
  ambari_simplejson as json  # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import base64
import fileinput
import getpass
import logging
import os
import re
import shutil
import stat
import sys
import tempfile
import time
import urllib2
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import print_warning_msg, print_error_msg, print_info_msg, get_verbose
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_utils import is_root, set_file_permissions, \
  run_os_command, search_file, is_valid_filepath, change_owner, get_ambari_repo_file_full_name, get_file_owner
from ambari_server.dbConfiguration import ensure_jdbc_driver_is_installed
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.serverConfiguration import configDefaults, parse_properties_file, \
  encrypt_password, find_jdk, find_properties_file, get_alias_string, get_ambari_properties, get_conf_dir, \
  get_credential_store_location, get_is_persisted, get_is_secure, get_master_key_location, get_db_type, write_property, \
  get_original_master_key, get_value_from_properties, get_java_exe_path, is_alias_string, read_ambari_user, \
  read_passwd_for_alias, remove_password_file, save_passwd_for_alias, store_password_file, update_properties_2, \
  BLIND_PASSWORD, BOOTSTRAP_DIR_PROPERTY, JDBC_PASSWORD_FILENAME, JDBC_PASSWORD_PROPERTY, \
  JDBC_RCA_PASSWORD_ALIAS, JDBC_RCA_PASSWORD_FILE_PROPERTY, JDBC_USE_INTEGRATED_AUTH_PROPERTY, \
  LDAP_MGR_PASSWORD_ALIAS, LDAP_MGR_PASSWORD_PROPERTY, CLIENT_SECURITY, \
  SECURITY_IS_ENCRYPTION_ENABLED, SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED, SECURITY_KEY_ENV_VAR_NAME, SECURITY_KERBEROS_JASS_FILENAME, \
  SECURITY_PROVIDER_KEY_CMD, SECURITY_SENSITIVE_DATA_ENCRYPTON_CMD, SECURITY_MASTER_KEY_FILENAME, SSL_TRUSTSTORE_PASSWORD_ALIAS, \
  SSL_TRUSTSTORE_PASSWORD_PROPERTY, SSL_TRUSTSTORE_PATH_PROPERTY, SSL_TRUSTSTORE_TYPE_PROPERTY, \
  JDK_NAME_PROPERTY, JCE_NAME_PROPERTY, JAVA_HOME_PROPERTY, \
  get_resources_location, SECURITY_MASTER_KEY_LOCATION, SETUP_OR_UPGRADE_MSG, \
  CHECK_AMBARI_KRB_JAAS_CONFIGURATION_PROPERTY
from ambari_server.serverUtils import is_server_runing, get_ambari_server_api_base, \
  get_ambari_admin_username_password_pair, perform_changes_via_rest_api, get_ssl_context, get_cluster_name, \
  get_eligible_services, get_boolean_from_dictionary, get_value_from_dictionary
from ambari_server.setupActions import SETUP_ACTION, LDAP_SETUP_ACTION
from ambari_server.userInput import get_validated_string_input, get_prompt_default, read_password, get_YN_input, \
  quit_if_has_answer
from contextlib import closing
from urllib2 import HTTPError

logger = logging.getLogger(__name__)

LDAP_AD="AD"
LDAP_IPA="IPA"
LDAP_GENERIC="Generic"

LDAP_TYPES = [LDAP_AD, LDAP_IPA, LDAP_GENERIC]

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"
REGEX_PORT = "^([0-9]{1,5}$)"
REGEX_HOSTNAME_PORT = "^(.*:[0-9]{1,5}$)"
REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_SKIP_CONVERT = "^(skip|convert)?$"
REGEX_REFERRAL = "^(follow|ignore)?$"
REGEX_LDAP_TYPE = "^({})?$".format("|".join(LDAP_TYPES))
REGEX_ANYTHING = ".*"
LDAP_TO_PAM_MIGRATION_HELPER_CMD = "{0} -cp {1} " + \
                                   "org.apache.ambari.server.security.authentication.LdapToPamMigrationHelper" + \
                                   " >> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

AUTO_GROUP_CREATION = "auto.group.creation"

SERVER_API_LDAP_URL = 'ldap_sync_events'
SETUP_LDAP_CONFIG_URL = 'services/AMBARI/components/AMBARI_SERVER/configurations/ldap-configuration'

PAM_CONFIG_FILE = 'pam.configuration'

LDAP_MGR_USERNAME_PROPERTY = "ambari.ldap.connectivity.bind_dn"
LDAP_MGR_PASSWORD_FILENAME = "ldap-password.dat"
LDAP_ANONYMOUS_BIND="ambari.ldap.connectivity.anonymous_bind"
LDAP_USE_SSL="ambari.ldap.connectivity.use_ssl"
LDAP_DISABLE_ENDPOINT_IDENTIFICATION = "ambari.ldap.advanced.disable_endpoint_identification"
NO_AUTH_METHOD_CONFIGURED = "no auth method"

AMBARI_LDAP_AUTH_ENABLED = "ambari.ldap.authentication.enabled"
LDAP_MANAGE_SERVICES = "ambari.ldap.manage_services"
LDAP_ENABLED_SERVICES = "ambari.ldap.enabled_services"
WILDCARD_FOR_ALL_SERVICES = "*"
FETCH_SERVICES_FOR_LDAP_ENTRYPOINT = "clusters/%s/services?ServiceInfo/ldap_integration_supported=true&fields=ServiceInfo/*"

def read_master_key(isReset=False, options = None):
  passwordPattern = ".*"
  passwordPrompt = "Please provide master key for locking the credential store: "
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"
  passwordDefault = ""
  if isReset:
    passwordPrompt = "Enter new Master Key: "

  input = True
  while(input):
    masterKey = get_validated_string_input(passwordPrompt, passwordDefault, passwordPattern, passwordDescr,
                                           True, True, answer = options.master_key)

    if not masterKey:
      print "Master Key cannot be empty!"
      continue

    masterKey2 = get_validated_string_input("Re-enter master key: ", passwordDefault, passwordPattern, passwordDescr,
                                            True, True, answer = options.master_key)

    if masterKey != masterKey2:
      print "Master key did not match!"
      continue

    input = False

  return masterKey

def save_master_key(options, master_key, key_location, persist=True):
  if master_key:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
      return 1
    serverClassPath = ServerClassPath(get_ambari_properties(), options)
    command = SECURITY_PROVIDER_KEY_CMD.format(get_java_exe_path(),
      serverClassPath.get_full_ambari_classpath_escaped_for_shell(), master_key, key_location, persist)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save KEY: " +
                   str(retcode))
  else:
    print_error_msg("Master key cannot be None.")


def adjust_directory_permissions(ambari_user):
  properties = get_ambari_properties()

  bootstrap_dir = os.path.abspath(get_value_from_properties(properties, BOOTSTRAP_DIR_PROPERTY))
  print_info_msg("Cleaning bootstrap directory ({0}) contents...".format(bootstrap_dir))

  if os.path.exists(bootstrap_dir):
    shutil.rmtree(bootstrap_dir) #Ignore the non-existent dir error

  if not os.path.exists(bootstrap_dir):
    try:
      os.makedirs(bootstrap_dir)
    except Exception, ex:
      print_warning_msg("Failed recreating the bootstrap directory: {0}".format(str(ex)))
      pass
  else:
    print_warning_msg("Bootstrap directory lingering around after 5s. Unable to complete the cleanup.")
  pass

  # Add master key and credential store if exists
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  if masterKeyFile:
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((masterKeyFile, configDefaults.MASTER_KEY_FILE_PERMISSIONS, "{0}", False))
  credStoreFile = get_credential_store_location(properties)
  if os.path.exists(credStoreFile):
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((credStoreFile, configDefaults.CREDENTIALS_STORE_FILE_PERMISSIONS, "{0}", False))
  trust_store_location = properties[SSL_TRUSTSTORE_PATH_PROPERTY]
  if trust_store_location:
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((trust_store_location, configDefaults.TRUST_STORE_LOCATION_PERMISSIONS, "{0}", False))

  # Update JDK and JCE permissions
  resources_dir = get_resources_location(properties)
  jdk_file_name = properties.get_property(JDK_NAME_PROPERTY)
  jce_file_name = properties.get_property(JCE_NAME_PROPERTY)
  java_home = properties.get_property(JAVA_HOME_PROPERTY)
  if jdk_file_name:
    jdk_file_path = os.path.abspath(os.path.join(resources_dir, jdk_file_name))
    if(os.path.exists(jdk_file_path)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_file_path, "644", "{0}", False))
  if jce_file_name:
    jce_file_path = os.path.abspath(os.path.join(resources_dir, jce_file_name))
    if(os.path.exists(jce_file_path)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jce_file_path, "644", "{0}", False))
  if java_home:
    jdk_security_dir = os.path.abspath(os.path.join(java_home, configDefaults.JDK_SECURITY_DIR))
    if(os.path.exists(jdk_security_dir)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_security_dir + "/*", "644", "{0}", True))
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_security_dir, "755", "{0}", False))

  # Grant read permissions to all users. This is required when a non-admin user is configured to setup ambari-server.
  # However, do not change ownership of the repo file to ambari user.

  ambari_repo_file = get_ambari_repo_file_full_name()

  if ambari_repo_file:
    if (os.path.exists(ambari_repo_file)):
        ambari_repo_file_owner = get_file_owner(ambari_repo_file)
        configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((ambari_repo_file, "644", ambari_repo_file_owner, False))


  print "Adjusting ambari-server permissions and ownership..."

  for pack in configDefaults.NR_ADJUST_OWNERSHIP_LIST:
    file = pack[0]
    mod = pack[1]
    user = pack[2].format(ambari_user)
    recursive = pack[3]
    print_info_msg("Setting file permissions: {0} {1} {2} {3}".format(file, mod, user, recursive))
    set_file_permissions(file, mod, user, recursive)

  for pack in configDefaults.NR_CHANGE_OWNERSHIP_LIST:
    path = pack[0]
    user = pack[1].format(ambari_user)
    recursive = pack[2]
    print_info_msg("Changing ownership: {0} {1} {2}".format(path, user, recursive))
    change_owner(path, user, recursive)

def configure_ldap_password(ldap_manager_password_option, interactive_mode):
  password_default = ""
  password_prompt = 'Enter Bind DN Password: '
  confirm_password_prompt = 'Confirm Bind DN Password: '
  password_pattern = ".*"
  password_descr = "Invalid characters in password."
  password = read_password(password_default, password_pattern, password_prompt, password_descr, ldap_manager_password_option, confirm_password_prompt) if interactive_mode else ldap_manager_password_option

  return password

#
# Get the principal names from the given CSV file and set them on the given LDAP event specs.
#
def get_ldap_event_spec_names(file, specs, new_specs):

  try:
    if os.path.exists(file):
      new_spec = new_specs[0]
      with open(file, 'r') as names_file:
        names = names_file.read()
        new_spec['names'] = names.replace('\n', '').replace('\t', '')
        names_file.close()
        specs += new_specs
    else:
      err = 'Sync event creation failed. File ' + file + ' not found.'
      raise FatalException(1, err)
  except Exception as exception:
    err = 'Caught exception reading file ' + file + ' : ' + str(exception)
    raise FatalException(1, err)


class LdapSyncOptions:
  def __init__(self, options):
    try:
      self.ldap_sync_all = options.ldap_sync_all
    except AttributeError:
      self.ldap_sync_all = False

    try:
      self.ldap_sync_existing = options.ldap_sync_existing
    except AttributeError:
      self.ldap_sync_existing = False

    try:
      self.ldap_sync_users = options.ldap_sync_users
    except AttributeError:
      self.ldap_sync_users = None

    try:
      self.ldap_sync_groups = options.ldap_sync_groups
    except AttributeError:
      self.ldap_sync_groups = None

    try:
      self.ldap_sync_admin_name = options.ldap_sync_admin_name
    except AttributeError:
      self.ldap_sync_admin_name = None

    try:
      self.ldap_sync_admin_password = options.ldap_sync_admin_password
    except AttributeError:
      self.ldap_sync_admin_password = None

    try:
      self.ldap_sync_post_process_existing_users = options.ldap_sync_post_process_existing_users
    except AttributeError:
      self.ldap_sync_post_process_existing_users = False

  def no_ldap_sync_options_set(self):
    return not self.ldap_sync_all and not self.ldap_sync_existing and self.ldap_sync_users is None and self.ldap_sync_groups is None

def get_ldap_property_from_db(properties, admin_login, admin_password, property_name):
  ldap_properties_from_db = get_ldap_properties_from_db(properties, admin_login, admin_password)
  return ldap_properties_from_db[property_name] if ldap_properties_from_db else None

def get_ldap_properties_from_db(properties, admin_login, admin_password):
  ldap_properties = None
  url = get_ambari_server_api_base(properties) + SETUP_LDAP_CONFIG_URL
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  request.get_method = lambda: 'GET'
  request_in_progress = True

  sys.stdout.write('\nFetching LDAP configuration from DB')
  num_of_tries = 0
  while request_in_progress:
    num_of_tries += 1
    if num_of_tries == 60:
      raise FatalException(1, "Could not fetch LDAP configuration within a minute; giving up!")
    sys.stdout.write('.')
    sys.stdout.flush()

    try:
      with closing(urllib2.urlopen(request, context=get_ssl_context(properties))) as response:
        response_status_code = response.getcode()
        if response_status_code != 200:
          request_in_progress = False
          err = 'Error while fetching LDAP configuration. Http status code - ' + str(response_status_code)
          raise FatalException(1, err)
        else:
            response_body = json.loads(response.read())
            ldap_properties = response_body['Configuration']['properties']
            if not ldap_properties:
              time.sleep(1)
            else:
              request_in_progress = False
    except HTTPError as e:
      if e.code == 404:
        sys.stdout.write(' No configuration.')
        return None
      err = 'Error while fetching LDAP configuration. Error details: %s' % e
      raise FatalException(1, err)
    except Exception as e:
      err = 'Error while fetching LDAP configuration. Error details: %s' % e
      raise FatalException(1, err)

  return ldap_properties

def is_ldap_enabled(properties, admin_login, admin_password):
  ldap_enabled = get_ldap_property_from_db(properties, admin_login, admin_password, AMBARI_LDAP_AUTH_ENABLED)
  return ldap_enabled if ldap_enabled is not None else 'false'


#
# Sync users and groups with configured LDAP
#
def sync_ldap(options):
  logger.info("Sync users and groups with configured LDAP.")

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY,"") == 'pam':
    err = "PAM is configured. Can not sync LDAP."
    raise FatalException(1, err)

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  # set ldap sync options
  ldap_sync_options = LdapSyncOptions(options)

  if ldap_sync_options.no_ldap_sync_options_set():
    err = 'Must specify a sync option (all, existing, users or groups).  Please invoke ambari-server.py --help to print the options.'
    raise FatalException(1, err)

  #TODO: use serverUtils.get_ambari_admin_username_password_pair (requires changes in ambari-server.py too to modify option names)
  admin_login = ldap_sync_options.ldap_sync_admin_name\
    if ldap_sync_options.ldap_sync_admin_name is not None and ldap_sync_options.ldap_sync_admin_name \
    else get_validated_string_input(prompt="Enter Ambari Admin login: ", default=None,
                                           pattern=None, description=None,
                                           is_pass=False, allowEmpty=False)
  admin_password = ldap_sync_options.ldap_sync_admin_password \
    if ldap_sync_options.ldap_sync_admin_password is not None and ldap_sync_options.ldap_sync_admin_password \
    else get_validated_string_input(prompt="Enter Ambari Admin password: ", default=None,
                                              pattern=None, description=None,
                                              is_pass=True, allowEmpty=False)

  if is_ldap_enabled(properties, admin_login, admin_password) != 'true':
    err = "LDAP is not configured. Run 'ambari-server setup-ldap' first."
    raise FatalException(1, err)

  url = get_ambari_server_api_base(properties) + SERVER_API_LDAP_URL
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')

  if ldap_sync_options.ldap_sync_all:
    sys.stdout.write('\nSyncing all.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"all"},{"principal_type":"groups","sync_type":"all"}]}}]
  elif ldap_sync_options.ldap_sync_existing:
    sys.stdout.write('\nSyncing existing.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"existing"},{"principal_type":"groups","sync_type":"existing"}]}}]
  else:
    sys.stdout.write('\nSyncing specified users and groups.')
    bodies = [{"Event":{"specs":[]}}]
    body = bodies[0]
    events = body['Event']
    specs = events['specs']

    if ldap_sync_options.ldap_sync_users is not None:
      new_specs = [{"principal_type":"users","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(ldap_sync_options.ldap_sync_users, specs, new_specs)
    if ldap_sync_options.ldap_sync_groups is not None:
      new_specs = [{"principal_type":"groups","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(ldap_sync_options.ldap_sync_groups, specs, new_specs)

  if ldap_sync_options.ldap_sync_post_process_existing_users:
    for spec in bodies[0]["Event"]["specs"]:
      spec["post_process_existing_users"] = "true"

  if get_verbose():
    sys.stdout.write('\nCalling API ' + url + ' : ' + str(bodies) + '\n')

  request.add_data(json.dumps(bodies))
  request.get_method = lambda: 'POST'

  try:
    response = urllib2.urlopen(request, context=get_ssl_context(properties))
  except Exception as e:
    err = 'Sync event creation failed. Error details: %s' % e
    raise FatalException(1, err)

  response_status_code = response.getcode()
  if response_status_code != 201:
    err = 'Error during syncing. Http status code - ' + str(response_status_code)
    raise FatalException(1, err)
  response_body = json.loads(response.read())

  url = response_body['resources'][0]['href']
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  body = [{"LDAP":{"synced_groups":"*","synced_users":"*"}}]
  request.add_data(json.dumps(body))
  request.get_method = lambda: 'GET'
  request_in_progress = True

  while request_in_progress:
    sys.stdout.write('.')
    sys.stdout.flush()

    try:
      response = urllib2.urlopen(request, context=get_ssl_context(properties))
    except Exception as e:
      request_in_progress = False
      err = 'Sync event check failed. Error details: %s' % e
      raise FatalException(1, err)

    response_status_code = response.getcode()
    if response_status_code != 200:
      err = 'Error during syncing. Http status code - ' + str(response_status_code)
      raise FatalException(1, err)
    response_body = json.loads(response.read())
    sync_info = response_body['Event']

    if sync_info['status'] == 'ERROR':
      raise FatalException(1, str(sync_info['status_detail']))
    elif sync_info['status'] == 'COMPLETE':
      print '\n\nCompleted LDAP Sync.'
      print 'Summary:'
      for principal_type, summary in sync_info['summary'].iteritems():
        print '  {0}:'.format(principal_type)
        for action, amount in summary.iteritems():
          print '    {0} = {1!s}'.format(action, amount)
      request_in_progress = False
    else:
      time.sleep(1)

  sys.stdout.write('\n')
  sys.stdout.flush()

def sensitive_data_encryption(options, direction, masterKey=None):
  environ = os.environ.copy()
  if masterKey:
    environ[SECURITY_KEY_ENV_VAR_NAME] = masterKey
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1
  serverClassPath = ServerClassPath(get_ambari_properties(), options)
  command = SECURITY_SENSITIVE_DATA_ENCRYPTON_CMD.format(get_java_exe_path(), serverClassPath.get_full_ambari_classpath_escaped_for_shell(), direction)
  (retcode, stdout, stderr) = run_os_command(command, environ)
  pass

def setup_sensitive_data_encryption(options):
  if not is_root():
    warn = 'ambari-server encrypt-passwords is run as ' \
          'non-root user, some sudo privileges might be required'
    print warn

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  db_windows_auth_prop = properties.get_property(JDBC_USE_INTEGRATED_AUTH_PROPERTY)
  db_sql_auth = False if db_windows_auth_prop and db_windows_auth_prop.lower() == 'true' else True
  db_password = properties.get_property(JDBC_PASSWORD_PROPERTY)
  # Encrypt passwords cannot be called before setup
  if db_sql_auth and not db_password:
    print 'Please call "setup" before "encrypt-passwords". Exiting...'
    return 1

  # Check configuration for location of master key
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)

  # Read clear text DB password from file
  if db_sql_auth and not is_alias_string(db_password) and os.path.isfile(db_password):
    with open(db_password, 'r') as passwdfile:
      db_password = passwdfile.read()

  ts_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  resetKey = False
  decrypt = False
  masterKey = None

  if isSecure:
    print "Password encryption is enabled."
    decrypt = get_YN_input("Do you want to decrypt passwords managed by Ambari? [y/n] (n): ", False)
    if not decrypt:
      resetKey = get_YN_input("Do you want to reset Master Key? [y/n] (n): ", False)

  # Make sure both passwords are clear-text if master key is lost
  if resetKey or decrypt:
    if isPersisted:
      sensitive_data_encryption(options, "decryption")
    else:
      print "Master Key not persisted."
      masterKey = get_original_master_key(properties, options)
      # Unable get the right master key or skipped question <enter>
      if not masterKey:
        # todo fix unreachable code
        printManualDecryptionWarning(db_password, db_sql_auth, ts_password)
        return 1
      sensitive_data_encryption(options, "decryption", masterKey)

  # Read back any encrypted passwords
  db_password, ts_password = deryptPasswordsConfigs(db_password, db_sql_auth, masterKey, ts_password)
  save_decrypted_ambari_properties(db_password, properties, ts_password)


  if not decrypt:
    if resetKey or not isSecure:
      # Read master key and encrypt sensitive data, if non-secure or reset is true
      masterKey, isPersisted = setup_master_key(masterKeyFile, options, properties, resetKey)
    else:
      if not isPersisted:
        # For encrypting of only unencrypted passwords without resetting the key ask
        # for master key if not persisted.
        print "Master Key not persisted."
        masterKey = get_original_master_key(properties, options)
    encrypt_sensitive_data(db_password, masterKey, options, isPersisted, properties, ts_password)

  # Since files for store and master are created we need to ensure correct
  # permissions
  ambari_user = read_ambari_user()
  if ambari_user:
    adjust_directory_permissions(ambari_user)
  return 0


def save_decrypted_ambari_properties(db_password, properties, ts_password):
  propertyMap = {SECURITY_IS_ENCRYPTION_ENABLED: 'false'}
  propertyMap[SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED] = 'false'
  if db_password:
    propertyMap[JDBC_PASSWORD_PROPERTY] = db_password
    if properties.get_property(JDBC_RCA_PASSWORD_FILE_PROPERTY):
      propertyMap[JDBC_RCA_PASSWORD_FILE_PROPERTY] = db_password
  if ts_password:
    propertyMap[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = ts_password
  update_properties_2(properties, propertyMap)


def encrypt_sensitive_data(db_password, masterKey, options, persist, properties, ts_password):
  propertyMap = {SECURITY_IS_ENCRYPTION_ENABLED: 'true'}
  # Encrypt only un-encrypted passwords
  if db_password and not is_alias_string(db_password):
    retCode = save_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, db_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure database password.'
    else:
      propertyMap[JDBC_PASSWORD_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)
      remove_password_file(JDBC_PASSWORD_FILENAME)
      if properties.get_property(JDBC_RCA_PASSWORD_FILE_PROPERTY):
        propertyMap[JDBC_RCA_PASSWORD_FILE_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)

  if ts_password and not is_alias_string(ts_password):
    retCode = save_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure TrustStore password.'
    else:
      propertyMap[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS)

  propertyMap[SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED] = 'true'
  if persist:
    sensitive_data_encryption(options, "encryption")
  else:
    sensitive_data_encryption(options, "encryption", masterKey)
  update_properties_2(properties, propertyMap)


def setup_master_key(masterKeyFile, options, properties, resetKey):
  masterKey = read_master_key(resetKey, options)
  persist = get_YN_input("Do you want to persist master key. If you choose " \
                         "not to persist, you need to provide the Master " \
                         "Key while starting the ambari server as an env " \
                         "variable named " + SECURITY_KEY_ENV_VAR_NAME + \
                         " or the start will prompt for the master key."
                         " Persist [y/n] (y)? ", True, options.master_key_persist)
  if persist:
    save_master_key(options, masterKey, get_master_key_location(properties) + os.sep +
                    SECURITY_MASTER_KEY_FILENAME, persist)
  elif not persist and masterKeyFile:
    try:
      os.remove(masterKeyFile)
      print_info_msg("Deleting master key file at location: " + str(
        masterKeyFile))
    except Exception, e:
      print 'ERROR: Could not remove master key file. %s' % e
  # Blow up the credential store made with previous key, if any
  store_file = get_credential_store_location(properties)
  if os.path.exists(store_file):
    try:
      os.remove(store_file)
    except:
      print_warning_msg("Failed to remove credential store file.")
  return masterKey, persist


def deryptPasswordsConfigs(db_password, db_sql_auth, masterKey, ts_password):
  if db_sql_auth and db_password and is_alias_string(db_password):
    db_password = read_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, masterKey)
  if ts_password and is_alias_string(ts_password):
    ts_password = read_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, masterKey)
  return db_password, ts_password


def printManualDecryptionWarning(db_password, db_sql_auth, ts_password):
  print "To disable encryption, do the following:"
  print "- Edit " + find_properties_file() + \
        " and set " + SECURITY_IS_ENCRYPTION_ENABLED + " = " + "false." + \
        " and set " + SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED + " = " + "false." + \
        " and set all passwords and sensitive data in service configs to right value."
  err = "{0} is already encrypted. Please call {1} to store unencrypted" \
        " password and call 'encrypt-passwords' again."
  if db_sql_auth and db_password and is_alias_string(db_password):
    print err.format('- Database password', "'" + SETUP_ACTION + "'")
  if ts_password and is_alias_string(ts_password):
    print err.format('TrustStore password', "'" + LDAP_SETUP_ACTION + "'")


def setup_ambari_krb5_jaas(options):
  jaas_conf_file = search_file(SECURITY_KERBEROS_JASS_FILENAME, get_conf_dir())
  if os.path.exists(jaas_conf_file):
    print 'Setting up Ambari kerberos JAAS configuration to access ' + \
          'secured Hadoop daemons...'
    principal = get_validated_string_input('Enter ambari server\'s kerberos '
                                 'principal name (ambari@EXAMPLE.COM): ', 'ambari@EXAMPLE.COM', '.*', '', False,
                                 False, answer = options.jaas_principal)
    keytab = get_validated_string_input('Enter keytab path for ambari '
                                 'server\'s kerberos principal: ',
                                 '/etc/security/keytabs/ambari.keytab', '.*', False, False,
                                  validatorFunction=is_valid_filepath, answer = options.jaas_keytab)

    for line in fileinput.FileInput(jaas_conf_file, inplace=1):
      line = re.sub('keyTab=.*$', 'keyTab="' + keytab + '"', line)
      line = re.sub('principal=.*$', 'principal="' + principal + '"', line)
      print line,

    write_property(CHECK_AMBARI_KRB_JAAS_CONFIGURATION_PROPERTY, "true")
  else:
    raise NonFatalException('No jaas config file found at location: ' +
                            jaas_conf_file)


class LdapPropTemplate:
  def __init__(self, properties, i_option, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_default=None):
    self.prop_name = i_prop_name
    self.option = i_option
    stored_value = get_value_from_properties(properties, i_prop_name)
    self.default_value = LdapDefault(stored_value) if stored_value else i_prop_default
    self.prompt_pattern = i_prop_val_pattern
    self.prompt_regex = i_prompt_regex
    self.allow_empty_prompt = i_allow_empty_prompt

  def get_default_value(self, ldap_type):
    return self.default_value.get_default_value(ldap_type) if self.default_value else None

  def get_prompt_text(self, ldap_type):
    default_value = self.get_default_value(ldap_type)
    return format_prop_val_prompt(self.prompt_pattern, default_value)

  def get_input(self, ldap_type, interactive_mode):
    default_value = self.get_default_value(ldap_type)
    return get_validated_string_input(self.get_prompt_text(ldap_type),
                                      default_value, self.prompt_regex,
                                       "Invalid characters in the input!", False, self.allow_empty_prompt,
                                       answer = self.option) if interactive_mode else self.option

  def should_query_ldap_type(self):
    return not self.allow_empty_prompt and not self.option and self.default_value and self.default_value.depends_on_ldap_type()

class LdapDefault:
  def __init__(self, value):
    self.default_value = value

  def get_default_value(self, ldap_type):
    return self.default_value

  def depends_on_ldap_type(self):
    return False


class LdapDefaultMap(LdapDefault):
  def __init__(self, value_map):
    LdapDefault.__init__(self, None)
    self.default_value_map = value_map

  def get_default_value(self, ldap_type):
    return self.default_value_map[ldap_type] if self.default_value_map and ldap_type in self.default_value_map else None

  def depends_on_ldap_type(self):
    return True

def format_prop_val_prompt(prop_prompt_pattern, prop_default_value):
  default_value = get_prompt_default(prop_default_value)
  return prop_prompt_pattern.format((" " + default_value) if default_value is not None and default_value != "" else "")

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def init_ldap_properties_list_reqd(properties, options):
  # python2.x dict is not ordered
  ldap_properties = [
    LdapPropTemplate(properties, options.ldap_primary_host, "ambari.ldap.connectivity.server.host", "Primary LDAP Host{0}: ", REGEX_HOSTNAME, False, LdapDefaultMap({LDAP_IPA:'ipa.ambari.apache.org', LDAP_GENERIC:'ldap.ambari.apache.org'})),
    LdapPropTemplate(properties, options.ldap_primary_port, "ambari.ldap.connectivity.server.port", "Primary LDAP Port{0}: ", REGEX_PORT, False, LdapDefaultMap({LDAP_IPA:'636', LDAP_GENERIC:'389'})),
    LdapPropTemplate(properties, options.ldap_secondary_host, "ambari.ldap.connectivity.secondary.server.host", "Secondary LDAP Host <Optional>{0}: ", REGEX_HOSTNAME, True),
    LdapPropTemplate(properties, options.ldap_secondary_port, "ambari.ldap.connectivity.secondary.server.port", "Secondary LDAP Port <Optional>{0}: ", REGEX_PORT, True),
    LdapPropTemplate(properties, options.ldap_ssl, "ambari.ldap.connectivity.use_ssl", "Use SSL [true/false]{0}: ", REGEX_TRUE_FALSE, False, LdapDefaultMap({LDAP_AD:'false', LDAP_IPA:'true', LDAP_GENERIC:'false'})),
    LdapPropTemplate(properties, options.ldap_user_attr, "ambari.ldap.attributes.user.name_attr", "User ID attribute{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'sAMAccountName', LDAP_IPA:'uid', LDAP_GENERIC:'uid'})),
    LdapPropTemplate(properties, options.ldap_base_dn, "ambari.ldap.attributes.user.search_base", "Search Base{0}: ", REGEX_ANYTHING, False, LdapDefault("dc=ambari,dc=apache,dc=org")),
    LdapPropTemplate(properties, options.ldap_referral, "ambari.ldap.advanced.referrals", "Referral method [follow/ignore]{0}: ", REGEX_REFERRAL, True, LdapDefault("follow")),
    LdapPropTemplate(properties, options.ldap_bind_anonym, "ambari.ldap.connectivity.anonymous_bind" "Bind anonymously [true/false]{0}: ", REGEX_TRUE_FALSE, False, LdapDefault("false"))
  ]
  return ldap_properties

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def init_ldap_properties_list_reqd(properties, options):
  ldap_properties = [
    LdapPropTemplate(properties, options.ldap_primary_host, "ambari.ldap.connectivity.server.host", "Primary LDAP Host{0}: ", REGEX_HOSTNAME, False, LdapDefaultMap({LDAP_IPA:'ipa.ambari.apache.org', LDAP_GENERIC:'ldap.ambari.apache.org'})),
    LdapPropTemplate(properties, options.ldap_primary_port, "ambari.ldap.connectivity.server.port", "Primary LDAP Port{0}: ", REGEX_PORT, False, LdapDefaultMap({LDAP_IPA:'636', LDAP_GENERIC:'389'})),
    LdapPropTemplate(properties, options.ldap_secondary_host, "ambari.ldap.connectivity.secondary.server.host", "Secondary LDAP Host <Optional>{0}: ", REGEX_HOSTNAME, True),
    LdapPropTemplate(properties, options.ldap_secondary_port, "ambari.ldap.connectivity.secondary.server.port", "Secondary LDAP Port <Optional>{0}: ", REGEX_PORT, True),
    LdapPropTemplate(properties, options.ldap_ssl, "ambari.ldap.connectivity.use_ssl", "Use SSL [true/false]{0}: ", REGEX_TRUE_FALSE, False, LdapDefaultMap({LDAP_AD:'false', LDAP_IPA:'true', LDAP_GENERIC:'false'})),
    LdapPropTemplate(properties, options.ldap_user_class, "ambari.ldap.attributes.user.object_class", "User object class{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'user', LDAP_IPA:'posixAccount', LDAP_GENERIC:'posixUser'})),
    LdapPropTemplate(properties, options.ldap_user_attr, "ambari.ldap.attributes.user.name_attr", "User ID attribute{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'sAMAccountName', LDAP_IPA:'uid', LDAP_GENERIC:'uid'})),
    LdapPropTemplate(properties, options.ldap_user_group_member_attr, "ambari.ldap.attributes.user.group_member_attr", "User group member attribute{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'memberof', LDAP_IPA:'member', LDAP_GENERIC:'memberof'})),
    LdapPropTemplate(properties, options.ldap_group_class, "ambari.ldap.attributes.group.object_class", "Group object class{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'group', LDAP_IPA:'posixGroup', LDAP_GENERIC:'posixGroup'})),
    LdapPropTemplate(properties, options.ldap_group_attr, "ambari.ldap.attributes.group.name_attr", "Group name attribute{0}: ", REGEX_ANYTHING, False, LdapDefault("cn")),
    LdapPropTemplate(properties, options.ldap_member_attr, "ambari.ldap.attributes.group.member_attr", "Group member attribute{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'member', LDAP_IPA:'member', LDAP_GENERIC:'memberUid'})),
    LdapPropTemplate(properties, options.ldap_dn, "ambari.ldap.attributes.dn_attr", "Distinguished name attribute{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'distinguishedName', LDAP_IPA:'dn', LDAP_GENERIC:'dn'})),
    LdapPropTemplate(properties, options.ldap_base_dn, "ambari.ldap.attributes.user.search_base", "Search Base{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({LDAP_AD:'dc=ambari,dc=apache,dc=org', LDAP_IPA:'cn=accounts,dc=ambari,dc=apache,dc=org', LDAP_GENERIC:'dc=ambari,dc=apache,dc=org'})),
    LdapPropTemplate(properties, options.ldap_referral, "ambari.ldap.advanced.referrals", "Referral method [follow/ignore]{0}: ", REGEX_REFERRAL, True, LdapDefault("follow")),
    LdapPropTemplate(properties, options.ldap_bind_anonym, "ambari.ldap.connectivity.anonymous_bind", "Bind anonymously [true/false]{0}: ", REGEX_TRUE_FALSE, False, LdapDefault("false")),
    LdapPropTemplate(properties, options.ldap_sync_username_collisions_behavior, "ambari.ldap.advanced.collision_behavior", "Handling behavior for username collisions [convert/skip] for LDAP sync{0}: ", REGEX_SKIP_CONVERT, False, LdapDefault("skip")),
    LdapPropTemplate(properties, options.ldap_force_lowercase_usernames, "ambari.ldap.advanced.force_lowercase_usernames", "Force lower-case user names [true/false]{0}:", REGEX_TRUE_FALSE, True),
    LdapPropTemplate(properties, options.ldap_pagination_enabled, "ambari.ldap.advanced.pagination_enabled", "Results from LDAP are paginated when requested [true/false]{0}:", REGEX_TRUE_FALSE, True)
  ]
  return ldap_properties

def update_ldap_configuration(admin_login, admin_password, properties, ldap_property_value_map):
  request_data = {
    "Configuration": {
      "category": "ldap-configuration",
      "properties": {
      }
    }
  }
  request_data['Configuration']['properties'] = ldap_property_value_map
  perform_changes_via_rest_api(properties, admin_login, admin_password, SETUP_LDAP_CONFIG_URL, 'PUT', request_data)

def should_query_ldap_type(ldap_property_list_reqd):
  for ldap_prop in ldap_property_list_reqd:
    if ldap_prop.should_query_ldap_type():
      return True
  return False

def query_ldap_type(ldap_type_option):
  return get_validated_string_input("Please select the type of LDAP you want to use [{}]({}):".format("/".join(LDAP_TYPES), LDAP_GENERIC),
                                    LDAP_GENERIC,
                                    REGEX_LDAP_TYPE,
                                    "Please enter one of the followings '{}'!".format("', '".join(LDAP_TYPES)),
                                    False,
                                    False,
                                    answer = ldap_type_option)

def is_interactive(property_list):
  for prop in property_list:
    if not prop.option and not prop.allow_empty_prompt:
      return True

  return False


def setup_ldap(options):
  logger.info("Setup LDAP.")

  properties = get_ambari_properties()

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  enforce_ldap = options.ldap_force_setup if options.ldap_force_setup is not None else False
  if not enforce_ldap:
    current_client_security = get_value_from_properties(properties, CLIENT_SECURITY, NO_AUTH_METHOD_CONFIGURED)
    if current_client_security != 'ldap':
      query = "Currently '{0}' is configured, do you wish to use LDAP instead [y/n] ({1})? "
      ldap_setup_default = 'y' if current_client_security == NO_AUTH_METHOD_CONFIGURED else 'n'
      if get_YN_input(query.format(current_client_security, ldap_setup_default), ldap_setup_default == 'y'):
        pass
      else:
        err = "Currently '" + current_client_security + "' configured. Can not setup LDAP."
        raise FatalException(1, err)

  admin_login, admin_password = get_ambari_admin_username_password_pair(options)
  ldap_properties = get_ldap_properties_from_db(properties, admin_login, admin_password)
  if ldap_properties:
    properties.update(ldap_properties)
  sys.stdout.write('\n')

  isSecure = get_is_secure(properties)

  if options.ldap_url:
    options.ldap_primary_host = options.ldap_url.split(':')[0]
    options.ldap_primary_port = options.ldap_url.split(':')[1]

  if options.ldap_secondary_url:
    options.ldap_secondary_host = options.ldap_secondary_url.split(':')[0]
    options.ldap_secondary_port = options.ldap_secondary_url.split(':')[1]


  ldap_property_list_reqd = init_ldap_properties_list_reqd(properties, options)
  ldap_bind_dn_template = LdapPropTemplate(properties, options.ldap_manager_dn, LDAP_MGR_USERNAME_PROPERTY, "Bind DN{0}: ", REGEX_ANYTHING, False, LdapDefaultMap({
    LDAP_AD:'cn=ldapbind,dc=ambari,dc=apache,dc=org',
    LDAP_IPA:'uid=ldapbind,cn=users,cn=accounts,dc=ambari,dc=apache,dc=org',
    LDAP_GENERIC:'uid=ldapbind,cn=users,dc=ambari,dc=apache,dc=org'}))
  ldap_type = query_ldap_type(options.ldap_type) if options.ldap_type or should_query_ldap_type(ldap_property_list_reqd) else LDAP_GENERIC
  ldap_property_list_opt = [LDAP_MGR_USERNAME_PROPERTY,
                            LDAP_MGR_PASSWORD_PROPERTY,
                            LDAP_DISABLE_ENDPOINT_IDENTIFICATION,
                            SSL_TRUSTSTORE_TYPE_PROPERTY,
                            SSL_TRUSTSTORE_PATH_PROPERTY,
                            SSL_TRUSTSTORE_PASSWORD_PROPERTY,
                            LDAP_MANAGE_SERVICES,
                            LDAP_ENABLED_SERVICES]

  ldap_property_list_passwords=[LDAP_MGR_PASSWORD_PROPERTY, SSL_TRUSTSTORE_PASSWORD_PROPERTY]


  ssl_truststore_type_default = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
  ssl_truststore_path_default = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)
  disable_endpoint_identification_default = get_value_from_properties(properties, LDAP_DISABLE_ENDPOINT_IDENTIFICATION, "False")

  ldap_property_value_map = {}
  ldap_property_values_in_ambari_properties = {}
  interactive_mode = is_interactive(ldap_property_list_reqd)
  for ldap_prop in ldap_property_list_reqd:
    input = ldap_prop.get_input(ldap_type, interactive_mode)

    if input is not None and input != "":
      ldap_property_value_map[ldap_prop.prop_name] = input

    if ldap_prop.prop_name == LDAP_ANONYMOUS_BIND:
      anonymous = (input and input.lower() == 'true')
      mgr_password = None
      # Ask for manager credentials only if bindAnonymously is false
      if not anonymous:
        username = ldap_bind_dn_template.get_input(ldap_type, interactive_mode)
        ldap_property_value_map[LDAP_MGR_USERNAME_PROPERTY] = username
        mgr_password = configure_ldap_password(options.ldap_manager_password, interactive_mode)
        ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = mgr_password
    elif ldap_prop.prop_name == LDAP_USE_SSL:
      ldaps = (input and input.lower() == 'true')
      ts_password = None

      if ldaps:
        disable_endpoint_identification = get_validated_string_input("Disable endpoint identification during SSL handshake [true/false] ({0}): ".format(disable_endpoint_identification_default),
                                                                     disable_endpoint_identification_default,
                                                                     REGEX_TRUE_FALSE, "Invalid characters in the input!", False, allowEmpty=True,
                                                                     answer=options.ldap_sync_disable_endpoint_identification) if interactive_mode else options.ldap_sync_disable_endpoint_identification
        if disable_endpoint_identification is not None:
          ldap_property_value_map[LDAP_DISABLE_ENDPOINT_IDENTIFICATION] = disable_endpoint_identification

        truststore_default = "n"
        truststore_set = bool(ssl_truststore_path_default)
        if truststore_set:
          truststore_default = "y"
        custom_trust_store = True if options.trust_store_path is not None and options.trust_store_path else False
        if not custom_trust_store:
          custom_trust_store = get_YN_input("Do you want to provide custom TrustStore for Ambari [y/n] ({0})?".
                                          format(truststore_default),
                                          truststore_set) if interactive_mode else None
        if custom_trust_store:
          ts_type = get_validated_string_input("TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(ssl_truststore_type_default)),
            ssl_truststore_type_default, "^(jks|jceks|pkcs12)?$", "Wrong type", False, answer=options.trust_store_type) if interactive_mode else options.trust_store_type
          ts_path = None
          while True:
            ts_path = get_validated_string_input(format_prop_val_prompt("Path to TrustStore file{0}: ", ssl_truststore_path_default),
                                                 ssl_truststore_path_default, ".*", False, False, answer = options.trust_store_path) if interactive_mode else options.trust_store_path
            if os.path.exists(ts_path):
              break
            else:
              print 'File not found.'
              hasAnswer = options.trust_store_path is not None and options.trust_store_path
              quit_if_has_answer(hasAnswer)

          ts_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password", options.trust_store_password) if interactive_mode else options.trust_store_password

          ldap_property_values_in_ambari_properties[SSL_TRUSTSTORE_TYPE_PROPERTY] = ts_type
          ldap_property_values_in_ambari_properties[SSL_TRUSTSTORE_PATH_PROPERTY] = ts_path
          ldap_property_values_in_ambari_properties[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = ts_password
          pass
        elif properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY):
          print 'The TrustStore is already configured: '
          print '  ' + SSL_TRUSTSTORE_TYPE_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)
          print '  ' + SSL_TRUSTSTORE_PATH_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_PATH_PROPERTY)
          print '  ' + SSL_TRUSTSTORE_PASSWORD_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
          if get_YN_input("Do you want to remove these properties [y/n] (y)? ", True, options.trust_store_reconfigure):
            properties.removeOldProp(SSL_TRUSTSTORE_TYPE_PROPERTY)
            properties.removeOldProp(SSL_TRUSTSTORE_PATH_PROPERTY)
            properties.removeOldProp(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
        pass
      pass

  populate_ambari_requires_ldap(options, ldap_property_value_map)
  populate_service_management(options, ldap_property_value_map, properties, admin_login, admin_password)

  print '=' * 20
  print 'Review Settings'
  print '=' * 20
  for property in ldap_property_list_reqd:
    if ldap_property_value_map.has_key(property.prop_name):
      print("%s %s" % (property.get_prompt_text(ldap_type), ldap_property_value_map[property.prop_name]))

  for property in ldap_property_list_opt:
    if ldap_property_value_map.has_key(property):
      if property not in ldap_property_list_passwords:
        print("%s: %s" % (property, ldap_property_value_map[property]))
      else:
        print("%s: %s" % (property, BLIND_PASSWORD))

  for property in ldap_property_list_opt:
    if ldap_property_values_in_ambari_properties.has_key(property):
      if property not in ldap_property_list_passwords:
        print("%s: %s" % (property, ldap_property_values_in_ambari_properties[property]))
      else:
        print("%s: %s" % (property, BLIND_PASSWORD))

  save_settings = True if options.ldap_save_settings is not None else get_YN_input("Save settings [y/n] (y)? ", True)

  if save_settings:
    if isSecure:
      if ts_password:
        encrypted_passwd = encrypt_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password, options)
        if ts_password != encrypted_passwd:
          ldap_property_values_in_ambari_properties[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = encrypted_passwd

    print 'Saving LDAP properties...'

    #Saving LDAP configuration in Ambari DB using the REST API
    update_ldap_configuration(admin_login, admin_password, properties, ldap_property_value_map)

    #The only properties we want to write out in Ambari.properties are the client.security type being LDAP and the custom Truststore related properties (if any)
    ldap_property_values_in_ambari_properties[CLIENT_SECURITY] = 'ldap'
    update_properties_2(properties, ldap_property_values_in_ambari_properties)

    print 'Saving LDAP properties finished'

  return 0

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def generate_env(options, ambari_user, current_user):
  properties = get_ambari_properties()
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  environ = os.environ.copy()
  # Need to handle master key not persisted scenario
  if isSecure and not masterKeyFile:
    prompt = False
    masterKey = environ.get(SECURITY_KEY_ENV_VAR_NAME)

    if masterKey is not None and masterKey != "":
      pass
    else:
      keyLocation = environ.get(SECURITY_MASTER_KEY_LOCATION)

      if keyLocation is not None:
        try:
          # Verify master key can be read by the java process
          with open(keyLocation, 'r'):
            pass
        except IOError:
          print_warning_msg("Cannot read Master key from path specified in "
                            "environemnt.")
          prompt = True
      else:
        # Key not provided in the environment
        prompt = True

    if prompt:
      import pwd

      masterKey = get_original_master_key(properties)
      environ[SECURITY_KEY_ENV_VAR_NAME] = masterKey
      tempDir = tempfile.gettempdir()
      tempFilePath = tempDir + os.sep + "masterkey"
      save_master_key(options, masterKey, tempFilePath, True)
      if ambari_user != current_user:
        uid = pwd.getpwnam(ambari_user).pw_uid
        gid = pwd.getpwnam(ambari_user).pw_gid
        os.chown(tempFilePath, uid, gid)
      else:
        os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)

      if tempFilePath is not None:
        environ[SECURITY_MASTER_KEY_LOCATION] = tempFilePath

  return environ

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def generate_env(options, ambari_user, current_user):
  return os.environ.copy()

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def ensure_can_start_under_current_user(ambari_user):
  #Ignore the requirement to run as root. In Windows, by default the child process inherits the security context
  # and the environment from the parent process.
  return ""

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def ensure_can_start_under_current_user(ambari_user):
  current_user = getpass.getuser()
  if ambari_user is None:
    err = "Unable to detect a system user for Ambari Server.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(1, err)
  if current_user != ambari_user and not is_root():
    err = "Unable to start Ambari Server as user {0}. Please either run \"ambari-server start\" " \
          "command as root, as sudo or as user \"{1}\"".format(current_user, ambari_user)
    raise FatalException(1, err)
  return current_user

class PamPropTemplate:
  def __init__(self, properties, i_option, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_name_default=None):
    self.prop_name = i_prop_name
    self.option = i_option
    self.pam_prop_name = get_value_from_properties(properties, i_prop_name, i_prop_name_default)
    self.pam_prop_val_prompt = i_prop_val_pattern.format(get_prompt_default(self.pam_prop_name))
    self.prompt_regex = i_prompt_regex
    self.allow_empty_prompt = i_allow_empty_prompt

def init_pam_properties_list_reqd(properties, options):
  properties = [
    PamPropTemplate(properties, options.pam_config_file, PAM_CONFIG_FILE, "PAM configuration file* {0}: ", REGEX_ANYTHING, False, "/etc/pam.d/ambari"),
    PamPropTemplate(properties, options.pam_auto_create_groups, AUTO_GROUP_CREATION, "Do you want to allow automatic group creation* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false"),
  ]
  return properties

def setup_pam(options):
  if not is_root():
    err = 'Ambari-server setup-pam should be run with root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY,"") == 'ldap':
    query = "LDAP is currently configured, do you wish to use PAM instead [y/n] (n)? "
    if get_YN_input(query, False):
      pass
    else:
      err = "LDAP is configured. Can not setup PAM."
      raise FatalException(1, err)

  pam_property_list_reqd = init_pam_properties_list_reqd(properties, options)

  pam_property_value_map = {}
  pam_property_value_map[CLIENT_SECURITY] = 'pam'

  for pam_prop in pam_property_list_reqd:
    input = get_validated_string_input(pam_prop.pam_prop_val_prompt, pam_prop.pam_prop_name, pam_prop.prompt_regex,
                                       "Invalid characters in the input!", False, pam_prop.allow_empty_prompt,
                                       answer = pam_prop.option)
    if input is not None and input != "":
      pam_property_value_map[pam_prop.prop_name] = input

  # Verify that the PAM config file exists, else show warning...
  pam_config_file = pam_property_value_map[PAM_CONFIG_FILE]
  if not os.path.exists(pam_config_file):
    print_warning_msg("The PAM configuration file, {0} does not exist.  " \
                      "Please create it before restarting Ambari.".format(pam_config_file))

  update_properties_2(properties, pam_property_value_map)
  print 'Saving...done'
  return 0

#
# Migration of LDAP users & groups to PAM
#
def migrate_ldap_pam(args):
  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY,"") != 'pam':
    err = "PAM is not configured. Please configure PAM authentication first."
    raise FatalException(1, err)

  db_title = get_db_type(properties).title
  confirm = get_YN_input("Ambari Server configured for %s. Confirm "
                        "you have made a backup of the Ambari Server database [y/n] (y)? " % db_title, True)

  if not confirm:
    print_error_msg("Database backup is not confirmed")
    return 1

  jdk_path = get_java_exe_path()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1

  # At this point, the args does not have the ambari database information.
  # Augment the args with the correct ambari database information
  parse_properties_file(args)

  ensure_jdbc_driver_is_installed(args, properties)

  print 'Migrating LDAP Users & Groups to PAM'

  serverClassPath = ServerClassPath(properties, args)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell()

  command = LDAP_TO_PAM_MIGRATION_HELPER_CMD.format(jdk_path, class_path)

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)
  environ = generate_env(args, ambari_user, current_user)

  (retcode, stdout, stderr) = run_os_command(command, env=environ)
  print_info_msg("Return code from LDAP to PAM migration command, retcode = " + str(retcode))
  if stdout:
    print "Console output from LDAP to PAM migration command:"
    print stdout
    print
  if stderr:
    print "Error output from LDAP to PAM migration command:"
    print stderr
    print
  if retcode > 0:
    print_error_msg("Error executing LDAP to PAM migration, please check the server logs.")
  else:
    print_info_msg('LDAP to PAM migration completed')
  return retcode


def populate_ambari_requires_ldap(options, properties):
  if options.ldap_enabled_ambari is None:
    enabled = get_boolean_from_dictionary(properties, AMBARI_LDAP_AUTH_ENABLED, False)
    enabled = get_YN_input("Use LDAP authentication for Ambari [y/n] ({0})? ".format('y' if enabled else 'n'), enabled)
  else:
    enabled = 'true' == options.ldap_enabled_ambari

  properties[AMBARI_LDAP_AUTH_ENABLED] = 'true' if enabled else 'false'


def populate_service_management(options, properties, ambari_properties, admin_login, admin_password):
  services = ""
  if options.ldap_enabled_services is None:
    if options.ldap_manage_services is None:
      manage_services = get_boolean_from_dictionary(properties, LDAP_MANAGE_SERVICES, False)
      manage_services = get_YN_input("Manage LDAP configurations for eligible services [y/n] ({0})? ".format('y' if manage_services else 'n'), manage_services)
    else:
      manage_services = 'true' == options.ldap_manage_services
      stored_manage_services = get_boolean_from_dictionary(properties, LDAP_MANAGE_SERVICES, False)
      print("Manage LDAP configurations for eligible services [y/n] ({0})? {1}".format('y' if stored_manage_services else 'n', 'y' if manage_services else 'n'))

    if manage_services:
      enabled_services = get_value_from_dictionary(properties, LDAP_ENABLED_SERVICES, "").upper().split(',')

      all = WILDCARD_FOR_ALL_SERVICES in enabled_services
      configure_for_all_services = get_YN_input(" Manage LDAP for all services [y/n] ({0})? ".format('y' if all else 'n'), all)
      if configure_for_all_services:
        services = WILDCARD_FOR_ALL_SERVICES
      else:
        cluster_name = get_cluster_name(ambari_properties, admin_login, admin_password)

        if cluster_name:
          eligible_services = get_eligible_services(ambari_properties, admin_login, admin_password, cluster_name, FETCH_SERVICES_FOR_LDAP_ENTRYPOINT, 'LDAP')

          if eligible_services and len(eligible_services) > 0:
            service_list = []

            for service in eligible_services:
              enabled = service.upper() in enabled_services
              question = "   Manage LDAP for {0} [y/n] ({1})? ".format(service, 'y' if enabled else 'n')
              if get_YN_input(question, enabled):
                service_list.append(service)

            services = ','.join(service_list)
          else:
            print ("   There are no eligible services installed.")
  else:
    if options.ldap_manage_services:
      manage_services = 'true' == options.ldap_manage_services
    else:
      manage_services = True

    services = options.ldap_enabled_services.upper() if options.ldap_enabled_services else ""

  properties[LDAP_MANAGE_SERVICES] = 'true' if manage_services else 'false'
  properties[LDAP_ENABLED_SERVICES] = services