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
import random
import re
import shutil
import socket
import string
import datetime
import tempfile
import urllib2
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_silent, print_warning_msg, print_error_msg
from ambari_commons.os_utils import is_root, run_os_command, copy_file, set_file_permissions, remove_file
from ambari_server.serverConfiguration import get_ambari_properties, find_properties_file, read_ambari_user, \
    SSL_TRUSTSTORE_PASSWORD_PROPERTY, get_is_secure, decrypt_password_for_alias, SSL_TRUSTSTORE_PASSWORD_ALIAS, \
    SSL_TRUSTSTORE_PATH_PROPERTY, get_value_from_properties, SSL_TRUSTSTORE_TYPE_PROPERTY, find_jdk, configDefaults, \
    SSL_API, SSL_API_PORT, DEFAULT_SSL_API_PORT, \
    get_encrypted_password, GET_FQDN_SERVICE_URL
from ambari_server.setupSecurity import adjust_directory_permissions
from ambari_server.userInput import get_YN_input, get_validated_string_input, read_password, get_prompt_default, \
    get_validated_filepath_input

#keytool commands
KEYTOOL_IMPORT_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + "keytool -import -alias '{1}' -storetype '{2}' -file '{3}' -storepass '{4}' -noprompt"
KEYTOOL_DELETE_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + "keytool -delete -alias '{1}' -storepass '{2}' -noprompt"
KEYTOOL_KEYSTORE = " -keystore '{0}'"

SSL_PASSWORD_FILE = "pass.txt"
SSL_PASSIN_FILE = "passin.txt"

# openssl command
VALIDATE_KEYSTORE_CMD = "openssl pkcs12 -info -in '{0}' -password file:'{1}' -passout file:'{2}'"
EXPRT_KSTR_CMD = "openssl pkcs12 -export -in '{0}' -inkey '{1}' -certfile '{0}' -out '{4}' -password file:'{2}' -passin file:'{3}'"
CHANGE_KEY_PWD_CND = 'openssl rsa -in {0} -des3 -out {0}.secured -passout pass:{1}'
GET_CRT_INFO_CMD = 'openssl x509 -dates -subject -in {0}'

#keytool commands
KEYTOOL_IMPORT_CERT_CMD = "{0} -import -alias '{1}' -storetype '{2}' -file '{3}' -storepass '{4}' -noprompt"
KEYTOOL_DELETE_CERT_CMD = "{0} -delete -alias '{1}' -storepass '{2}' -noprompt"
KEYTOOL_KEYSTORE = " -keystore '{0}'"

SSL_KEY_DIR = 'security.server.keys_dir'
SSL_SERVER_CERT_NAME = 'client.api.ssl.cert_name'
SSL_SERVER_KEY_NAME = 'client.api.ssl.key_name'
SSL_CERT_FILE_NAME = "https.crt"
SSL_KEY_FILE_NAME = "https.key"
SSL_KEYSTORE_FILE_NAME = "https.keystore.p12"
SSL_KEY_PASSWORD_FILE_NAME = "https.pass.txt"
SSL_KEY_PASSWORD_LENGTH = 50
SSL_DATE_FORMAT = '%b  %d %H:%M:%S %Y GMT'

#SSL certificate metainfo
COMMON_NAME_ATTR = 'CN'
NOT_BEFORE_ATTR = 'notBefore'
NOT_AFTER_ATTR = 'notAfter'

SRVR_TWO_WAY_SSL_PORT_PROPERTY = "security.server.two_way_ssl.port"
SRVR_TWO_WAY_SSL_PORT = "8441"

SRVR_ONE_WAY_SSL_PORT_PROPERTY = "security.server.one_way_ssl.port"
SRVR_ONE_WAY_SSL_PORT = "8440"

GANGLIA_HTTPS = 'ganglia.https'


def get_and_persist_truststore_path(properties, options):
  truststore_path = properties.get_property(SSL_TRUSTSTORE_PATH_PROPERTY)
  if not truststore_path:
    SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)

    while not truststore_path:
      truststore_path = get_validated_string_input(
          "Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
          SSL_TRUSTSTORE_PATH_DEFAULT, ".*", False, False, answer = options.trust_store_path)

    if truststore_path:
      properties.process_pair(SSL_TRUSTSTORE_PATH_PROPERTY, truststore_path)

  return truststore_path

def get_and_persist_truststore_type(properties, options):
  truststore_type = properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)
  if not truststore_type:
    SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
    truststore_type = get_validated_string_input(
        "TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
        SSL_TRUSTSTORE_TYPE_DEFAULT, "^(jks|jceks|pkcs12)?$", "Wrong type", False, answer = options.trust_store_type)

    if truststore_type:
        properties.process_pair(SSL_TRUSTSTORE_TYPE_PROPERTY, truststore_type)

  return truststore_type

def get_and_persist_truststore_password(properties, options):
  truststore_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  isSecure = get_is_secure(properties)
  if truststore_password:
    if isSecure:
      truststore_password = decrypt_password_for_alias(properties, SSL_TRUSTSTORE_PASSWORD_ALIAS, options)
  else:
    truststore_password = read_password("", ".*", "Password for TrustStore:",
                                        "Invalid characters in password", options.trust_store_password)
    if truststore_password:
      encrypted_password = get_encrypted_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, truststore_password, properties, options)
      properties.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, encrypted_password)

  return truststore_password

def get_keytool_path(jdk_path):
    return os.path.join(jdk_path, configDefaults.keytool_bin_subpath)

def get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password):
  cmd = KEYTOOL_IMPORT_CERT_CMD.format(get_keytool_path(jdk_path), alias, truststore_type, import_cert_path, truststore_password)
  if truststore_path:
    cmd += KEYTOOL_KEYSTORE.format(truststore_path)
  return cmd

def get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password):
    cmd = KEYTOOL_DELETE_CERT_CMD.format(get_keytool_path(jdk_path), alias, truststore_password)
    if truststore_path:
        cmd += KEYTOOL_KEYSTORE.format(truststore_path)
    return cmd


def import_cert_and_key(security_server_keys_dir, options):
  import_cert_path = get_validated_filepath_input( \
      "Enter path to Certificate: ", "Certificate not found", answer = options.import_cert_path)
  import_key_path  = get_validated_filepath_input( \
      "Enter path to Private Key: ", "Private Key not found", answer = options.import_key_path)
  pem_password = get_validated_string_input("Please enter password for Private Key: ", "",
                                            None, None, True, answer = options.pem_password)

  certInfoDict = get_cert_info(import_cert_path)

  if not certInfoDict:
    print_warning_msg('Unable to get Certificate information')
  else:
    #Validate common name of certificate
    if not is_valid_cert_host(certInfoDict):
      print_warning_msg('Unable to validate Certificate hostname')

    #Validate issue and expirations dates of certificate
    if not is_valid_cert_exp(certInfoDict):
      print_warning_msg('Unable to validate Certificate issue and expiration dates')

  #jetty requires private key files with non-empty key passwords
  retcode = 0
  err = ''
  if not pem_password:
    print 'Generating random password for HTTPS keystore...done.'
    pem_password = generate_random_string()
    retcode, out, err = run_os_command(CHANGE_KEY_PWD_CND.format(
        import_key_path, pem_password))
    import_key_path += '.secured'

  if retcode == 0:
    keystoreFilePath = os.path.join(security_server_keys_dir, \
                                    SSL_KEYSTORE_FILE_NAME)
    keystoreFilePathTmp = os.path.join(tempfile.gettempdir(), \
                                       SSL_KEYSTORE_FILE_NAME)
    passFilePath = os.path.join(security_server_keys_dir, \
                                SSL_KEY_PASSWORD_FILE_NAME)
    passFilePathTmp = os.path.join(tempfile.gettempdir(), \
                                   SSL_KEY_PASSWORD_FILE_NAME)
    passinFilePath = os.path.join(tempfile.gettempdir(), \
                                  SSL_PASSIN_FILE)
    passwordFilePath = os.path.join(tempfile.gettempdir(), \
                                    SSL_PASSWORD_FILE)

    with open(passFilePathTmp, 'w+') as passFile:
      passFile.write(pem_password)
      passFile.close
      pass

    set_file_permissions(passFilePath, "660", read_ambari_user(), False)

    copy_file(passFilePathTmp, passinFilePath)
    copy_file(passFilePathTmp, passwordFilePath)

    retcode, out, err = run_os_command(EXPRT_KSTR_CMD.format(import_cert_path, \
                                                             import_key_path, passwordFilePath, passinFilePath, keystoreFilePathTmp))
  if retcode == 0:
    print 'Importing and saving Certificate...done.'
    import_file_to_keystore(keystoreFilePathTmp, keystoreFilePath)
    import_file_to_keystore(passFilePathTmp, passFilePath)

    import_file_to_keystore(import_cert_path, os.path.join( \
        security_server_keys_dir, SSL_CERT_FILE_NAME))
    import_file_to_keystore(import_key_path, os.path.join( \
        security_server_keys_dir, SSL_KEY_FILE_NAME))

    #Validate keystore
    retcode, out, err = run_os_command(VALIDATE_KEYSTORE_CMD.format(keystoreFilePath, \
                                                                    passwordFilePath, passinFilePath))

    remove_file(passinFilePath)
    remove_file(passwordFilePath)

    if not retcode == 0:
      print 'Error during keystore validation occured!:'
      print err
      return False

    return True
  else:
    print_error_msg('Could not import Certificate and Private Key.')
    print 'SSL error on exporting keystore: ' + err.rstrip() + \
        '.\nPlease ensure that provided Private Key password is correct and ' + \
        're-import Certificate.'

    return False


def import_file_to_keystore(source, destination):
  shutil.copy(source, destination)
  set_file_permissions(destination, "660", read_ambari_user(), False)


def generate_random_string(length=SSL_KEY_PASSWORD_LENGTH):
  chars = string.digits + string.ascii_letters
  return ''.join(random.choice(chars) for x in range(length))


def get_cert_info(path):
  retcode, out, err = run_os_command(GET_CRT_INFO_CMD.format(path))

  if retcode != 0:
    print 'Error getting Certificate info'
    print err
    return None

  if out:
    certInfolist = out.split(os.linesep)
  else:
    print 'Empty Certificate info'
    return None

  notBefore = None
  notAfter = None
  subject = None

  for item in range(len(certInfolist)):
    if certInfolist[item].startswith('notAfter='):
      notAfter = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('notBefore='):
      notBefore = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('subject='):
      subject = certInfolist[item].split('=', 1)[1]

    #Convert subj to dict
  pattern = re.compile(r"[A-Z]{1,2}=[\w.-]{1,}")
  if subject:
    subjList = pattern.findall(subject)
    keys = [item.split('=')[0] for item in subjList]
    values = [item.split('=')[1] for item in subjList]
    subjDict = dict(zip(keys, values))

    result = subjDict
    result['notBefore'] = notBefore
    result['notAfter'] = notAfter
    result['subject'] = subject

    return result
  else:
    return {}


def is_valid_cert_exp(certInfoDict):
  if certInfoDict.has_key(NOT_BEFORE_ATTR):
    notBefore = certInfoDict[NOT_BEFORE_ATTR]
  else:
    print_warning_msg('There is no Not Before value in Certificate')
    return False

  if certInfoDict.has_key(NOT_AFTER_ATTR):
    notAfter = certInfoDict['notAfter']
  else:
    print_warning_msg('There is no Not After value in Certificate')
    return False

  notBeforeDate = datetime.datetime.strptime(notBefore, SSL_DATE_FORMAT)
  notAfterDate = datetime.datetime.strptime(notAfter, SSL_DATE_FORMAT)

  currentDate = datetime.datetime.now()

  if currentDate > notAfterDate:
    print_warning_msg('Certificate expired on: ' + str(notAfterDate))
    return False

  if currentDate < notBeforeDate:
    print_warning_msg('Certificate will be active from: ' + str(notBeforeDate))
    return False

  return True


def is_valid_cert_host(certInfoDict):
  if certInfoDict.has_key(COMMON_NAME_ATTR):
    commonName = certInfoDict[COMMON_NAME_ATTR]
  else:
    print_warning_msg('There is no Common Name in Certificate')
    return False

  fqdn = get_fqdn()

  if not fqdn:
    print_warning_msg('Failed to get server FQDN')
    return False

  if commonName != fqdn:
    print_warning_msg('Common Name in Certificate: ' + commonName + ' does not match the server FQDN: ' + fqdn)
    return False

  return True


def get_fqdn(timeout=2):
  properties = get_ambari_properties()
  if properties == -1:
    print "Error reading ambari properties"
    return None

  get_fqdn_service_url = properties[GET_FQDN_SERVICE_URL]
  try:
    handle = urllib2.urlopen(get_fqdn_service_url, '', timeout)
    str = handle.read()
    handle.close()
    return str
  except Exception:
    return socket.getfqdn().lower()


def is_valid_https_port(port):
  properties = get_ambari_properties()
  if properties == -1:
    print "Error getting ambari properties"
    return False

  one_way_port = properties[SRVR_ONE_WAY_SSL_PORT_PROPERTY]
  if not one_way_port:
    one_way_port = SRVR_ONE_WAY_SSL_PORT

  two_way_port = properties[SRVR_TWO_WAY_SSL_PORT_PROPERTY]
  if not two_way_port:
    two_way_port = SRVR_TWO_WAY_SSL_PORT

  if port.strip() == one_way_port.strip():
    print "Port for https can't match the port for one way authentication port(" + one_way_port + ")"
    return False

  if port.strip() == two_way_port.strip():
    print "Port for https can't match the port for two way authentication port(" + two_way_port + ")"
    return False

  return True


def import_cert_and_key_action(security_server_keys_dir, properties, options):
  if import_cert_and_key(security_server_keys_dir, options):
    properties.process_pair(SSL_SERVER_CERT_NAME, SSL_CERT_FILE_NAME)
    properties.process_pair(SSL_SERVER_KEY_NAME, SSL_KEY_FILE_NAME)
    properties.process_pair(SSL_API, "true")
    return True
  else:
    return False

def run_component_https_cmd(cmd):
  retcode, out, err = run_os_command(cmd)

  if not retcode == 0:
    err = 'Error occured during truststore setup ! :' + out + " : " + err
    raise FatalException(1, err)


def setup_https(options):
  if not is_root():
    warn = 'ambari-server setup-https is run as ' \
          'non-root user, some sudo privileges might be required'
    print warn
  options.exit_message = None
  if not get_silent():
    properties = get_ambari_properties()
    try:
      security_server_keys_dir = properties.get_property(SSL_KEY_DIR)
      client_api_ssl_port = DEFAULT_SSL_API_PORT if properties.get_property(SSL_API_PORT) in ("") \
            else properties.get_property(SSL_API_PORT)
      api_ssl = properties.get_property(SSL_API) in ['true']
      client_api_ssl_port_old_value = properties.get_property(SSL_API_PORT)
      api_ssl_old_value = properties.get_property(SSL_API)
      cert_was_imported = False
      cert_must_import = True

      disable_https = options.api_ssl in ['false'] if options.api_ssl is not None else None
      configure_https = options.api_ssl in ['true'] if options.api_ssl is not None else None

      if api_ssl:
        disable_https = disable_https if disable_https is not None else get_YN_input("Do you want to disable HTTPS [y/n] (n)? ", False)
        if disable_https:
          properties.process_pair(SSL_API, "false")
          cert_must_import=False
        else:
          properties.process_pair(SSL_API_PORT, \
                                  get_validated_string_input( \
                                      "SSL port ["+str(client_api_ssl_port)+"] ? ", \
                                      str(client_api_ssl_port), \
                                      "^[0-9]{1,5}$", "Invalid port.", False, validatorFunction = is_valid_https_port, \
                                      answer = options.api_ssl_port))
          cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties, options)
      else:
        if get_YN_input("Do you want to configure HTTPS [y/n] (y)? ", True, configure_https):
          properties.process_pair(SSL_API_PORT, \
                                  get_validated_string_input("SSL port ["+str(client_api_ssl_port)+"] ? ", \
                                                             str(client_api_ssl_port), "^[0-9]{1,5}$", "Invalid port.",
                                                             False, validatorFunction = is_valid_https_port,
                                                             answer = options.api_ssl_port))
          cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties, options)
        else:
          return False

      if cert_must_import and not cert_was_imported:
        print 'Setup of HTTPS failed. Exiting.'
        return False

      conf_file = find_properties_file()
      f = open(conf_file, 'w')
      properties.store(f, "Changed by 'ambari-server setup-https' command")

      if api_ssl_old_value != properties.get_property(SSL_API) \
          or client_api_ssl_port_old_value != properties.get_property(SSL_API_PORT):
        print "Ambari server URL changed. To make use of the Tez View in Ambari " \
              "please update the property tez.tez-ui.history-url.base in tez-site"

      ambari_user = read_ambari_user()
      if ambari_user:
        adjust_directory_permissions(ambari_user)
      return True
    except (KeyError), e:
        err = 'Property ' + str(e) + ' is not defined'
        raise FatalException(1, err)
  else:
    warning = "setup-https is not enabled in silent mode."
    raise NonFatalException(warning)


def setup_truststore(options, import_cert=False):
  if not get_silent():
    jdk_path = find_jdk()
    if jdk_path is None:
      err = "No JDK found, please run the \"ambari-server setup\" " \
            "command to install a JDK automatically or install any " \
            "JDK manually to " + configDefaults.JDK_INSTALL_DIR
      raise FatalException(1, err)

    properties = get_ambari_properties()

    truststore_confirm = True if options.trust_store_path is not None and options.trust_store_path else False
    truststore_reconfigure = True if options.trust_store_reconfigure is not None else False

    if truststore_confirm or get_YN_input("Do you want to configure a truststore [y/n] (y)? ", True):

      #Re-configuration enabled only for option "Setup truststore"
      if not import_cert and properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)\
        and (truststore_reconfigure or get_YN_input(
            "The truststore is already configured. Do you want to re-configure "
            "the truststore [y/n] (y)? ", True)):
        properties.removeProp(SSL_TRUSTSTORE_TYPE_PROPERTY)
        properties.removeProp(SSL_TRUSTSTORE_PATH_PROPERTY)
        properties.removeProp(SSL_TRUSTSTORE_PASSWORD_PROPERTY)

      truststore_type = get_and_persist_truststore_type(properties, options)
      truststore_path = get_and_persist_truststore_path(properties, options)
      truststore_password = get_and_persist_truststore_password(properties, options)

      if import_cert:

        import_cert_confirm = True if options.import_cert_path is not None else get_YN_input("Do you want to import a certificate [y/n] (y)? ", True)
        if import_cert_confirm:
          aliasOption = options.import_cert_alias if options.import_cert_alias is not None and options.import_cert_alias else None
          alias = aliasOption if aliasOption is not None \
            else get_validated_string_input("Please enter an alias for the certificate: ", "", None, None, False, False)

          run_os_command(get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password))

          import_cert_path = get_validated_filepath_input("Enter path to certificate: ",
                                                          "Certificate not found",
                                                          answer=options.import_cert_path)

          run_component_https_cmd(get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password))

    else:
      return

    conf_file = find_properties_file()
    f = open(conf_file, 'w')
    properties.store(f, "Changed by 'ambari-server setup-security' command")
  else:
    print "setup-security is not enabled in silent mode."
