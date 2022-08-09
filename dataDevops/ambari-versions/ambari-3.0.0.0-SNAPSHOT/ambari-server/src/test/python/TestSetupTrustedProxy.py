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
import platform
import sys
import unittest
import StringIO

from mock.mock import patch, MagicMock

from only_for_platform import os_distro_value
from ambari_commons import os_utils
from urllib2 import HTTPError

import shutil

# Mock classes for reading from a file
class MagicFile(object):
  def __init__(self, data):
    self.data = data

  def read(self):
    return self.data

  def __exit__(self, exc_type, exc_val, exc_tb):
    pass

  def __enter__(self):
    return self
pass

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

with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '7.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        with patch("platform.linux_distribution", return_value = os_distro_value):
          with patch("os.symlink"):
            with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
              _ambari_server_ = __import__('ambari-server')
              with patch("__builtin__.open"):
                from ambari_commons.exceptions import FatalException, NonFatalException
                from ambari_server.properties import Properties
                from ambari_server.setupTrustedProxy import setup_trusted_proxy, TPROXY_SUPPORT_ENABLED, PROXYUSER_HOSTS, PROXYUSER_USERS, PROXYUSER_GROUPS

class TestSetupTrustedProxy(unittest.TestCase):

  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  def test_tproxy_setup_should_fail_if_server_is_not_running(self, is_server_runing_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (False, 0)
    options = self._create_empty_options_mock()

    try:
      setup_trusted_proxy(options)
      self.fail("Should fail with non-fatal exception")
    except FatalException as e:
      self.assertTrue("Ambari Server is not running" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  def test_silent_mode_is_not_allowed(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = True
    options = self._create_empty_options_mock()

    try:
      setup_trusted_proxy(options)
      self.fail("Should fail with fatal exception")
    except NonFatalException as e:
      self.assertTrue("setup-trusted-proxy is not enabled in silent mode." in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  def test_invalid_tproxy_enabled_cli_option_should_result_in_error(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.tproxy_enabled = 'not_true_or_false'

    try:
      setup_trusted_proxy(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("--tproxy-enabled should be to either 'true' or 'false'" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupTrustedProxy.perform_changes_via_rest_api")
  @patch("ambari_server.setupTrustedProxy.get_YN_input")
  @patch("ambari_server.setupTrustedProxy.get_validated_string_input")  
  @patch("ambari_server.setupTrustedProxy.get_ambari_properties")
  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  @patch("ambari_server.setupTrustedProxy.get_json_via_rest_api")
  def test_tproxy_is_enabled_for_two_proxy_users(self, get_json_via_rest_api_mock, is_server_runing_mock, get_silent_mock,
                                                get_ambari_properties_mock, get_validated_string_input_mock, get_YN_input_mock, perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    get_json_via_rest_api_mock.return_value = (200, {})

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()

    user_name1 = 'knox'
    hosts1 = 'knox_hosts'
    users1 = 'knox_users'
    groups1 = 'knox_groups'
    
    user_name2 = 'admin'
    hosts2 = 'admin_hosts'
    users2 = 'admin_users'
    groups2 = 'admin_groups'
    get_validated_string_input_mock.side_effect = [user_name1, hosts1, users1, groups1, user_name2, hosts2, users2, groups2]

    get_YN_input_mock.side_effect = [True, False] #answer 'True' for the first time when asking for a new proxy user addition and then 'False' (indicating we do not want to add more proxy users)

    options = self._create_empty_options_mock()
    options.tproxy_enabled = 'true'

    setup_trusted_proxy(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    tproxyProperties = requestData['Configuration']['properties']
    self.assertEqual(tproxyProperties[TPROXY_SUPPORT_ENABLED], 'true')

    self.assertEqual(tproxyProperties[PROXYUSER_HOSTS.format(user_name1)], hosts1)
    self.assertEqual(tproxyProperties[PROXYUSER_USERS.format(user_name1)], users1)
    self.assertEqual(tproxyProperties[PROXYUSER_GROUPS.format(user_name1)], groups1)

    self.assertEqual(tproxyProperties[PROXYUSER_HOSTS.format(user_name2)], hosts2)
    self.assertEqual(tproxyProperties[PROXYUSER_USERS.format(user_name2)], users2)
    self.assertEqual(tproxyProperties[PROXYUSER_GROUPS.format(user_name2)], groups2)

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupTrustedProxy.perform_changes_via_rest_api")
  @patch("ambari_server.setupTrustedProxy.get_ambari_properties")
  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  @patch("ambari_server.setupTrustedProxy.get_json_via_rest_api")
  def test_disabling_tproxy_support(self, get_json_via_rest_api_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    get_json_via_rest_api_mock.return_value = (200, {})

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties()
    get_ambari_properties_mock.return_value = properties

    options = self._create_empty_options_mock()
    options.tproxy_enabled = 'false'

    setup_trusted_proxy(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestMethod = args[4]
    self.assertTrue(isinstance(requestMethod, str))
    self.assertEqual(requestMethod, "DELETE")

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  @patch("os.path.isfile")
  def test_enable_tproxy_support_using_configuration_file_path_from_command_line_should_fail_if_file_does_not_exist(self, isfile_mock, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    isfile_mock.return_value = False

    options = self._create_empty_options_mock()
    options.tproxy_enabled = 'true'
    options.tproxy_configuration_file_path = 'samplePath'

    try:
      setup_trusted_proxy(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("--tproxy-configuration-file-path is set to a non-existing file" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupTrustedProxy.perform_changes_via_rest_api")
  @patch("ambari_server.setupTrustedProxy.get_ambari_properties")
  @patch("ambari_server.setupTrustedProxy.get_silent")
  @patch("ambari_server.setupTrustedProxy.is_server_runing")
  @patch("ambari_server.setupTrustedProxy.get_json_via_rest_api")
  @patch("os.path.isfile")
  @patch('__builtin__.open')
  def test_enable_tproxy_support_using_configuration_file_path_from_command_line(self, open_mock, isfile_mock, get_json_via_rest_api_mock, is_server_runing_mock, get_silent_mock, get_ambari_properties_mock, perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    get_json_via_rest_api_mock.return_value = (200, {})

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties()
    get_ambari_properties_mock.return_value = properties

    isfile_mock.return_value = True

    tproxy_configurations = "["\
                            "  {"\
                            "    \"proxyuser\" : \"knox\"," \
                            "    \"hosts\"     : \"host1\"," \
                            "    \"users\"     : \"user1\"," \
                            "    \"groups\"    : \"group1\"" \
                            "  }," \
                            "  {"\
                            "    \"proxyuser\": \"admin\"," \
                            "    \"hosts\"    : \"host2\"," \
                            "    \"users\"    : \"user2\"," \
                            "    \"groups\"   : \"group2\"" \
                            "  }" \
                            "]"
    mock_file = MagicFile(tproxy_configurations)
    open_mock.side_effect = [mock_file]

    options = self._create_empty_options_mock()
    options.tproxy_enabled = 'true'
    options.tproxy_configuration_file_path = 'samplePath'

    setup_trusted_proxy(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    tproxyProperties = requestData['Configuration']['properties']
    self.assertEqual(tproxyProperties[TPROXY_SUPPORT_ENABLED], 'true')

    user_name1="knox"
    self.assertEqual(tproxyProperties[PROXYUSER_HOSTS.format(user_name1)], "host1")
    self.assertEqual(tproxyProperties[PROXYUSER_USERS.format(user_name1)], "user1")
    self.assertEqual(tproxyProperties[PROXYUSER_GROUPS.format(user_name1)], "group1")

    user_name2="admin"
    self.assertEqual(tproxyProperties[PROXYUSER_HOSTS.format(user_name2)], "host2")
    self.assertEqual(tproxyProperties[PROXYUSER_USERS.format(user_name2)], "user2")
    self.assertEqual(tproxyProperties[PROXYUSER_GROUPS.format(user_name2)], "group2")

    sys.stdout = sys.__stdout__
    pass


  def _create_empty_options_mock(self):
    options = MagicMock()
    options.tproxy_enabled = None
    options.tproxy_configuration_file_path = None
    return options
