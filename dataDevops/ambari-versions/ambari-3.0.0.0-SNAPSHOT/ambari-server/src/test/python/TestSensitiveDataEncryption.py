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
import sys

from ambari_commons.exceptions import FatalException
from mock.mock import patch, MagicMock, call

with patch.object(os, "geteuid", new=MagicMock(return_value=0)):
  from resource_management.core import sudo
  reload(sudo)

import operator
import platform
import StringIO
from unittest import TestCase
os.environ["ROOT"] = ""

from only_for_platform import get_platform, os_distro_value, PLATFORM_WINDOWS
from ambari_commons import os_utils

if get_platform() != PLATFORM_WINDOWS:
  pass

import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

# We have to use this import HACK because the filename contains a dash
_search_file = os_utils.search_file


def search_file_proxy(filename, searchpatch, pathsep=os.pathsep):
  global _search_file

  if "ambari.properties" in filename:
    return "/tmp/ambari.properties"

  return _search_file(filename, searchpatch, pathsep)


os_utils.search_file = search_file_proxy
with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        with patch("platform.linux_distribution", return_value = os_distro_value):
          with patch("os.symlink"):
            with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
              _ambari_server_ = __import__('ambari-server')
              with patch("__builtin__.open"):
                from ambari_server.properties import Properties
                from ambari_server.serverConfiguration import configDefaults, JDBC_RCA_PASSWORD_FILE_PROPERTY, JDBC_PASSWORD_PROPERTY, \
  JDBC_RCA_PASSWORD_ALIAS, SSL_TRUSTSTORE_PASSWORD_PROPERTY, SECURITY_IS_ENCRYPTION_ENABLED, \
  SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED, SSL_TRUSTSTORE_PASSWORD_ALIAS, SECURITY_KEY_ENV_VAR_NAME
                from ambari_server.setupSecurity import get_alias_string, setup_sensitive_data_encryption, sensitive_data_encryption
                from ambari_server.serverClassPath import ServerClassPath


@patch.object(platform, "linux_distribution", new = MagicMock(return_value=('Redhat', '6.4', 'Final')))
@patch("ambari_server.dbConfiguration_linux.get_postgre_hba_dir", new = MagicMock(return_value = "/var/lib/pgsql/data"))
@patch("ambari_server.dbConfiguration_linux.get_postgre_running_status", new = MagicMock(return_value = "running"))
class TestSensitiveDataEncryption(TestCase):
  def setUp(self):
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    sys.stdout = sys.__stdout__

  @patch("os.path.isdir", new = MagicMock(return_value=True))
  @patch("os.access", new = MagicMock(return_value=True))
  @patch.object(ServerClassPath, "get_full_ambari_classpath_escaped_for_shell", new = MagicMock(return_value = 'test' + os.pathsep + 'path12'))
  @patch("ambari_server.setupSecurity.find_jdk")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.run_os_command")
  def test_sensitive_data_encryption(self, run_os_command_mock, get_ambari_properties_method, find_jdk_mock):
    find_jdk_mock.return_value = "/"
    environ = os.environ.copy()

    run_os_command_mock.return_value = 0,"",""
    properties = Properties()
    get_ambari_properties_method.return_value = properties
    options = self._create_empty_options_mock()
    sensitive_data_encryption(options, "encription")
    run_os_command_mock.assert_called_with('None -cp test:path12 org.apache.ambari.server.security.encryption.SensitiveDataEncryption encription > /var/log/ambari-server/ambari-server.out 2>&1', environ)
    pass

  @patch("ambari_server.setupSecurity.print_error_msg")
  @patch("ambari_server.setupSecurity.find_jdk")
  def test_sensitive_data_encryption_nojdk(self, find_jdk_mock, print_mock):
    find_jdk_mock.return_value = None

    options = self._create_empty_options_mock()
    code = sensitive_data_encryption(options, "encription")
    self.assertEquals(code, 1)
    print_mock.assert_called_with("No JDK found, please run the \"setup\" "
                                  "command to install a JDK automatically or install any "
                                  "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    pass

  @patch("os.path.isdir", new = MagicMock(return_value=True))
  @patch("os.access", new = MagicMock(return_value=True))
  @patch.object(ServerClassPath, "get_full_ambari_classpath_escaped_for_shell", new = MagicMock(return_value = 'test' + os.pathsep + 'path12'))
  @patch("ambari_server.setupSecurity.find_jdk")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.run_os_command")
  def test_sensitive_data_decryption_not_persisted(self, run_os_command_mock, get_ambari_properties_method, find_jdk_mock):
    find_jdk_mock.return_value = "/"
    environ = os.environ.copy()
    master = "master"
    environ[SECURITY_KEY_ENV_VAR_NAME] = master

    run_os_command_mock.return_value = 0,"",""
    properties = Properties()
    get_ambari_properties_method.return_value = properties
    options = self._create_empty_options_mock()
    sensitive_data_encryption(options, "decryption", master)
    run_os_command_mock.assert_called_with('None -cp test:path12 org.apache.ambari.server.security.encryption.SensitiveDataEncryption decryption > /var/log/ambari-server/ambari-server.out 2>&1', environ)
    pass

  @patch("ambari_server.setupSecurity.get_is_persisted")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_passwd_for_alias")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_validated_string_input")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  @patch("ambari_server.setupSecurity.get_original_master_key")
  def test_reset_master_key_not_persisted(self, get_original_master_key_mock, sensitive_data_encryption_metod, is_root_method,
                                          get_ambari_properties_method,
                                          search_file_message, get_YN_input_method,
                                          get_validated_string_input_method, save_master_key_method,
                                          update_properties_method, read_passwd_for_alias_method,
                                          save_passwd_for_alias_method,
                                          read_ambari_user_method,
                                          exists_mock, get_is_secure_method,
                                          get_is_persisted_method):

    is_root_method.return_value = True
    search_file_message.return_value = False
    read_ambari_user_method.return_value = None

    p = Properties()
    FAKE_PWD_STRING = '${alias=fakealias}'
    p.process_pair(JDBC_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    master_key = "aaa"
    get_YN_input_method.side_effect = [False, True, False]
    get_validated_string_input_method.return_value = master_key
    get_original_master_key_mock.return_value = master_key
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False
    get_is_secure_method.return_value = True
    get_is_persisted_method.return_value = (False, "")

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)
    calls = [call(options, "decryption", master_key), call(options, "encryption", master_key)]
    sensitive_data_encryption_metod.assert_has_calls(calls)

    self.assertFalse(save_master_key_method.called)
    self.assertTrue(get_original_master_key_mock.called)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(get_validated_string_input_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(2, read_passwd_for_alias_method.call_count)
    self.assertTrue(2, save_passwd_for_alias_method.call_count)
    self.assertFalse(save_master_key_method.called)

    result_expected = {JDBC_PASSWORD_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       SECURITY_IS_ENCRYPTION_ENABLED: 'true',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  @patch("ambari_server.setupSecurity.get_is_persisted")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_passwd_for_alias")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  @patch("ambari_server.setupSecurity.get_original_master_key")
  def test_encrypt_part_not_persisted(self, get_original_master_key_mock, sensitive_data_encryption_metod, is_root_method,
                                          get_ambari_properties_method,
                                          search_file_message, get_YN_input_method,
                                          save_master_key_method,
                                          update_properties_method, read_passwd_for_alias_method,
                                          save_passwd_for_alias_method,
                                          read_ambari_user_method,
                                          exists_mock, get_is_secure_method,
                                          get_is_persisted_method):

    is_root_method.return_value = True
    search_file_message.return_value = False
    read_ambari_user_method.return_value = None

    p = Properties()
    FAKE_PWD_STRING = '${alias=fakealias}'
    p.process_pair(JDBC_PASSWORD_PROPERTY, get_alias_string(JDBC_RCA_PASSWORD_ALIAS))
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    master_key = "aaa"
    get_YN_input_method.side_effect = [False, False, False]
    get_original_master_key_mock.return_value = master_key
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False
    get_is_secure_method.return_value = True
    get_is_persisted_method.return_value = (False, "filePath")

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)
    calls = [call(options, "encryption", master_key)]
    sensitive_data_encryption_metod.assert_has_calls(calls)

    self.assertFalse(save_master_key_method.called)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(get_original_master_key_mock.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(2, read_passwd_for_alias_method.call_count)
    self.assertTrue(2, save_passwd_for_alias_method.call_count)
    self.assertFalse(save_master_key_method.called)

    result_expected = {JDBC_PASSWORD_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       SECURITY_IS_ENCRYPTION_ENABLED: 'true',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  @patch("ambari_server.setupSecurity.get_is_persisted")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_passwd_for_alias")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.get_original_master_key")
  def test_decrypt_missed_masterkey_not_persisted(self, get_original_master_key_mock, is_root_method,
                                      get_ambari_properties_method,
                                      search_file_message, get_YN_input_method,
                                      save_master_key_method,
                                      read_passwd_for_alias_method,
                                      save_passwd_for_alias_method,
                                      read_ambari_user_method,
                                      exists_mock, get_is_secure_method,
                                      get_is_persisted_method):

    is_root_method.return_value = True
    search_file_message.return_value = False
    read_ambari_user_method.return_value = None

    p = Properties()
    FAKE_PWD_STRING = '${alias=fakealias}'
    p.process_pair(JDBC_PASSWORD_PROPERTY, get_alias_string(JDBC_RCA_PASSWORD_ALIAS))
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    get_YN_input_method.side_effect = [True, False]
    get_original_master_key_mock.return_value = None
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False
    get_is_secure_method.return_value = True
    get_is_persisted_method.return_value = (False, "filePath")

    options = self._create_empty_options_mock()
    self.assertTrue(setup_sensitive_data_encryption(options) == 1)

    self.assertFalse(save_master_key_method.called)
    self.assertTrue(get_YN_input_method.called)
    pass

  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  def test_setup_sensitive_data_encryption_no_ambari_prop_not_root(self,  is_root_method, get_ambari_properties_method):

    is_root_method.return_value = False
    get_ambari_properties_method.return_value = -1
    options = self._create_empty_options_mock()

    try:
      setup_sensitive_data_encryption(options)
      self.fail("Should throw exception")
    except FatalException as fe:
      self.assertTrue('Failed to read properties file.' == fe.reason)
      pass
    pass

  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("ambari_server.setupSecurity.get_is_persisted")
  @patch("ambari_server.setupSecurity.remove_password_file")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_master_key")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  @patch("ambari_server.setupSecurity.adjust_directory_permissions")
  def test_setup_sensitive_data_encryption_not_persist(self, adjust_directory_permissions_mock, sensitive_data_encryption_metod, is_root_method,
                                                       get_ambari_properties_method, get_YN_input_method, save_master_key_method,
                                                       update_properties_method,
                                                       read_ambari_user_method, read_master_key_method,
                                                       save_passwd_for_alias_method, remove_password_file_method,
                                                       get_is_persisted_method, get_is_secure_method, exists_mock):

    is_root_method.return_value = True

    p = Properties()
    FAKE_PWD_STRING = "fakepasswd"
    p.process_pair(JDBC_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    master_key = "aaa"
    read_master_key_method.return_value = master_key
    get_YN_input_method.return_value = False
    read_ambari_user_method.return_value = "asd"
    save_passwd_for_alias_method.return_value = 0
    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = False
    exists_mock.return_value = False

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)

    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(read_ambari_user_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertFalse(save_master_key_method.called)
    self.assertTrue(save_passwd_for_alias_method.called)
    self.assertEquals(2, save_passwd_for_alias_method.call_count)
    self.assertTrue(remove_password_file_method.called)
    self.assertTrue(adjust_directory_permissions_mock.called)
    sensitive_data_encryption_metod.assert_called_with(options, "encryption", master_key)

    result_expected = {JDBC_PASSWORD_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       SECURITY_IS_ENCRYPTION_ENABLED: 'true',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("ambari_server.setupSecurity.get_is_persisted")
  @patch("ambari_server.setupSecurity.read_master_key")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.serverConfiguration.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  def test_setup_sensitive_data_encryption_persist(self, sensitive_data_encryption_metod, is_root_method,
                                                   get_ambari_properties_method, search_file_message,
                                                   get_YN_input_method, save_master_key_method,
                                                   update_properties_method,
                                                   read_ambari_user_method, read_master_key_method,
                                                   get_is_persisted_method, get_is_secure_method, exists_mock,
                                                   save_passwd_for_alias_method):
    is_root_method.return_value = True

    p = Properties()
    FAKE_PWD_STRING = "fakepasswd"
    p.process_pair(JDBC_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    search_file_message.return_value = "propertiesfile"

    master_key = "aaa"
    read_master_key_method.return_value = master_key
    get_YN_input_method.return_value = True
    read_ambari_user_method.return_value = None
    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = False
    exists_mock.return_value = False
    save_passwd_for_alias_method.return_value = 0

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)

    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(read_ambari_user_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(save_master_key_method.called)
    sensitive_data_encryption_metod.assert_called_with(options, "encryption")

    result_expected = {JDBC_PASSWORD_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       SECURITY_IS_ENCRYPTION_ENABLED: 'true',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  @patch("ambari_server.setupSecurity.read_master_key")
  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_passwd_for_alias")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.save_master_key")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("ambari_server.setupSecurity.get_is_persisted")
  def test_reset_master_key_persisted(self, get_is_persisted_method, get_is_secure_method, sensitive_data_encryption_metod, is_root_method,
                                      get_ambari_properties_method, search_file_message,
                                      get_YN_input_method,
                                      save_master_key_method, update_properties_method,
                                      read_passwd_for_alias_method, save_passwd_for_alias_method,
                                      read_ambari_user_method, exists_mock,
                                      read_master_key_method):

    # Testing call under root
    is_root_method.return_value = True

    search_file_message.return_value = "filepath"
    read_ambari_user_method.return_value = None

    p = Properties()
    FAKE_PWD_STRING = '${alias=fakealias}'
    p.process_pair(JDBC_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    master_key = "aaa"

    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = True
    get_YN_input_method.side_effect = [False, True, True]
    read_master_key_method.return_value = master_key
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)
    calls = [call(options, "decryption"), call(options, "encryption")]
    sensitive_data_encryption_metod.assert_has_calls(calls)

    self.assertTrue(save_master_key_method.called)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(2, read_passwd_for_alias_method.call_count)
    self.assertTrue(2, save_passwd_for_alias_method.call_count)

    result_expected = {JDBC_PASSWORD_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         get_alias_string(JDBC_RCA_PASSWORD_ALIAS),
                       SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       SECURITY_IS_ENCRYPTION_ENABLED: 'true',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.read_ambari_user")
  @patch("ambari_server.setupSecurity.save_passwd_for_alias")
  @patch("ambari_server.setupSecurity.read_passwd_for_alias")
  @patch("ambari_server.setupSecurity.update_properties_2")
  @patch("ambari_server.setupSecurity.get_YN_input")
  @patch("ambari_server.setupSecurity.search_file")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.is_root")
  @patch("ambari_server.setupSecurity.sensitive_data_encryption")
  @patch("ambari_server.setupSecurity.get_is_secure")
  @patch("ambari_server.setupSecurity.get_is_persisted")
  def test_decrypt_sensitive_data_persister(self, get_is_persisted_method, get_is_secure_method, sensitive_data_encryption_metod, is_root_method,
                                  get_ambari_properties_method, search_file_message,
                                  get_YN_input_method,
                                  update_properties_method,
                                  read_passwd_for_alias_method, save_passwd_for_alias_method,
                                  read_ambari_user_method, exists_mock):

    # Testing call under root
    is_root_method.return_value = True

    search_file_message.return_value = "filepath"
    read_ambari_user_method.return_value = None

    p = Properties()
    FAKE_PWD_STRING = '${alias=fakealias}'
    p.process_pair(JDBC_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, FAKE_PWD_STRING)
    p.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, FAKE_PWD_STRING)
    get_ambari_properties_method.return_value = p

    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = True
    get_YN_input_method.side_effect = [True, False]
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False

    options = self._create_empty_options_mock()
    setup_sensitive_data_encryption(options)
    calls = [call(options, "decryption")]
    sensitive_data_encryption_metod.assert_has_calls(calls)

    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(2, read_passwd_for_alias_method.call_count)
    self.assertTrue(2, save_passwd_for_alias_method.call_count)

    result_expected = {JDBC_PASSWORD_PROPERTY: "fakepassword",
                       JDBC_RCA_PASSWORD_FILE_PROPERTY: "fakepassword",
                       SSL_TRUSTSTORE_PASSWORD_PROPERTY: "fakepassword",
                       SECURITY_IS_ENCRYPTION_ENABLED: 'false',
                       SECURITY_SENSITIVE_DATA_ENCRYPTON_ENABLED: 'false'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    pass

  def _create_empty_options_mock(self):
    options = MagicMock()
    options.ldap_enabled = None
    options.ldap_enabled_ambari = None
    options.ldap_manage_services = None
    options.ldap_enabled_services = None
    options.ldap_url = None
    options.ldap_primary_host = None
    options.ldap_primary_port = None
    options.ldap_secondary_url = None
    options.ldap_secondary_host = None
    options.ldap_secondary_port = None
    options.ldap_ssl = None
    options.ldap_user_class = None
    options.ldap_user_attr = None
    options.ldap_user_group_member_attr = None
    options.ldap_group_class = None
    options.ldap_group_attr = None
    options.ldap_member_attr = None
    options.ldap_dn = None
    options.ldap_base_dn = None
    options.ldap_manager_dn = None
    options.ldap_manager_password = None
    options.ldap_save_settings = None
    options.ldap_referral = None
    options.ldap_bind_anonym = None
    options.ldap_force_setup = None
    options.ambari_admin_username = None
    options.ambari_admin_password = None
    options.ldap_sync_admin_name = None
    options.ldap_sync_username_collisions_behavior = None
    options.ldap_sync_disable_endpoint_identification = None
    options.ldap_force_lowercase_usernames = None
    options.ldap_pagination_enabled = None
    options.ldap_sync_admin_password = None
    options.custom_trust_store = None
    options.trust_store_type = None
    options.trust_store_path = None
    options.trust_store_password = None
    options.security_option = None
    options.api_ssl = None
    options.api_ssl_port = None
    options.import_cert_path = None
    options.import_cert_alias = None
    options.pem_password = None
    options.import_key_path = None
    options.master_key = None
    options.master_key_persist = None
    options.jaas_principal = None
    options.jaas_keytab = None
    return options


