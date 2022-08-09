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

with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
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
                from ambari_server.setupSso import setup_sso, AMBARI_SSO_AUTH_ENABLED, \
                  SSO_PROVIDER_URL, SSO_CERTIFICATE, JWT_COOKIE_NAME, JWT_AUDIENCES, \
                  SSO_ENABLED_SERVICES, SSO_MANAGE_SERVICES

class TestSetupSso(unittest.TestCase):
  @patch("ambari_server.setupSso.is_server_runing")
  def test_sso_setup_should_fail_if_server_is_not_running(self, is_server_runing_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (False, 0)
    options = self._create_empty_options_mock()

    try:
      setup_sso(options)
      self.fail("Should fail with non-fatal exception")
    except FatalException as e:
      self.assertTrue("Ambari Server is not running" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  def test_silent_mode_is_not_allowed(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = True
    options = self._create_empty_options_mock()

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except NonFatalException as e:
      self.assertTrue("setup-sso is not enabled in silent mode." in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  def test_invalid_sso_enabled_cli_option_should_result_in_error(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'not_true_or_false'

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("--sso-enabled should be to either 'true' or 'false'" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  def test_missing_sso_provider_url_cli_option_when_enabling_sso_should_result_in_error(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = ''

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Missing option: --sso-provider-url" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  def test_missing_sso_public_cert_file_cli_option_when_enabling_sso_should_result_in_error(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_public_cert_file = ''

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Missing option: --sso-public-cert-file" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  def test_invalid_sso_provider_url_cli_option_when_enabling_sso_should_result_in_error(self, is_server_runing_mock, get_silent_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_provider_url = '!invalidHost:invalidPort'

    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    options.sso_provider_url = 'The SSO provider URL is https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso'
    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    options.sso_provider_url = 'https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso is the SSO provider URL'
    try:
      setup_sso(options)
      self.fail("Should fail with fatal exception")
    except FatalException as e:
      self.assertTrue("Invalid --sso-provider-url" in e.reason)
      pass

    sys.stdout = sys.__stdout__
    pass


  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.get_json_via_rest_api")
  @patch('__builtin__.open')
  def test_all_cli_options_are_collected_when_enabling_sso(self, open_mock,
                                                           get_json_via_rest_api_mock,
                                                           is_server_runing_mock,
                                                           get_silent_mock,
                                                           get_ambari_properties_mock,
                                                           perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    certificate_data = '-----BEGIN CERTIFICATE-----\n' \
                       'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n' \
                       '................................................................\n' \
                       'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n' \
                       '-----END CERTIFICATE-----'
    mock_file = MagicFile(certificate_data)
    open_mock.side_effect = [mock_file]

    get_json_via_rest_api_mock.return_value = (200, {})
    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties()
    get_ambari_properties_mock.return_value = properties

    sso_enabled = 'true'
    sso_enabled_services = 'Ambari, SERVICE1, SERVICE2'
    sso_provider_url = 'https://c7402.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'
    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_enabled_ambari = 'true'
    options.sso_manage_services = 'true'
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list
    options.sso_enabled_services = sso_enabled_services

    setup_sso(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties']
    self.assertEqual(ssoProperties[AMBARI_SSO_AUTH_ENABLED], sso_enabled)
    self.assertEqual(ssoProperties[SSO_PROVIDER_URL], sso_provider_url)
    self.assertEqual(ssoProperties[SSO_CERTIFICATE], certificate_data)
    self.assertEqual(ssoProperties[JWT_COOKIE_NAME], sso_jwt_cookie_name)
    self.assertEqual(ssoProperties[JWT_AUDIENCES], sso_jwt_audience_list)

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.get_json_via_rest_api")
  @patch('__builtin__.open')
  def test_only_sso_enabled_cli_option_is_collected_when_disabling_sso(self, open_mock,
                                                                       get_json_via_rest_api_mock,
                                                                       is_server_runing_mock,
                                                                       get_silent_mock,
                                                                       get_ambari_properties_mock,
                                                                       perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    certificate_data = '-----BEGIN CERTIFICATE-----\n' \
                       'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n' \
                       '................................................................\n' \
                       'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n' \
                       '-----END CERTIFICATE-----'
    mock_file = MagicFile(certificate_data)
    open_mock.side_effect = [mock_file]

    get_json_via_rest_api_mock.return_value = (200, {})

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False

    properties = Properties()
    get_ambari_properties_mock.return_value = properties

    sso_enabled = 'false'
    sso_provider_url = 'http://testHost:8080'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'
    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list

    setup_sso(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestMethod = args[4]
    self.assertTrue(isinstance(requestMethod, str))
    self.assertEqual(requestMethod, "DELETE")

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_YN_input")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.get_json_via_rest_api")
  @patch('__builtin__.open')
  def test_sso_is_enabled_for_all_services_via_user_input(self, open_mock,
                                                          get_json_via_rest_api_mock,
                                                          is_server_runing_mock,
                                                          get_silent_mock,
                                                          get_ambari_properties_mock,
                                                          get_YN_input_mock,
                                                          perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    certificate_data = '-----BEGIN CERTIFICATE-----\n' \
                       'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n' \
                       '................................................................\n' \
                       'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n' \
                       '-----END CERTIFICATE-----'
    mock_file = MagicFile(certificate_data)
    open_mock.side_effect = [mock_file]

    get_json_via_rest_api_mock.return_value = (200, {})

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()

    def yn_input_side_effect(*args, **kwargs):
      if 'Manage SSO configurations' in args[0]:
        return True
      elif 'Manage SSO configurations' in args[0]:
        return True
      elif 'all services' in args[0]:
        return True
      else:
        raise Exception("ShouldNotBeInvoked") # only the 'Use SSO for all services' question should be asked for now

    get_YN_input_mock.side_effect = yn_input_side_effect

    sso_enabled = 'true'
    sso_provider_url = 'http://testHost:8080'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'

    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_enabled_ambari = 'true'
    options.sso_manage_services = 'true'
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list

    setup_sso(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties']
    self.assertEqual(ssoProperties[AMBARI_SSO_AUTH_ENABLED], sso_enabled)
    self.assertEqual(ssoProperties[SSO_PROVIDER_URL], sso_provider_url)
    self.assertEqual(ssoProperties[SSO_CERTIFICATE], certificate_data)
    self.assertEqual(ssoProperties[JWT_COOKIE_NAME], sso_jwt_cookie_name)
    self.assertEqual(ssoProperties[JWT_AUDIENCES], sso_jwt_audience_list)
    self.assertEqual(ssoProperties[SSO_MANAGE_SERVICES], "true")
    self.assertEqual(ssoProperties[SSO_ENABLED_SERVICES], "*")

    sys.stdout = sys.__stdout__
    pass

  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("urllib2.urlopen")
  @patch("ambari_server.setupSso.get_cluster_name")
  @patch("ambari_server.setupSso.get_YN_input")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch('__builtin__.open')
  def test_setup_sso_should_not_fail_when_sso_config_cannot_be_loaded_due_to_404_error(self, open_mock,
                                                                                       is_server_runing_mock,
                                                                                       get_silent_mock,
                                                                                       get_ambari_properties_mock,
                                                                                       get_YN_input_mock,
                                                                                       get_cluster_name_mock,
                                                                                       urlopen_mock,
                                                                                       perform_changes_via_rest_api_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    certificate_data = '-----BEGIN CERTIFICATE-----\n' \
                       'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n' \
                       '................................................................\n' \
                       'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n' \
                       '-----END CERTIFICATE-----'
    mock_file = MagicFile(certificate_data)
    open_mock.side_effect = [mock_file]

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()
    get_cluster_name_mock.return_value = 'cluster1'
    get_YN_input_mock.__return_value = True

    urlopen_mock.side_effect = HTTPError(MagicMock(status=404), 404, 'not found', None, None)

    sso_enabled = 'true'
    sso_provider_url = 'http://testHost:8080'
    sso_public_cert_file = '/test/file/path'
    sso_jwt_cookie_name = 'test_cookie'
    sso_jwt_audience_list = 'test, audience, list'

    options = self._create_empty_options_mock()
    options.sso_enabled = sso_enabled
    options.sso_enabled_ambari = sso_enabled
    options.sso_manage_services = 'true'
    options.sso_provider_url = sso_provider_url
    options.sso_public_cert_file = sso_public_cert_file
    options.sso_jwt_cookie_name = sso_jwt_cookie_name
    options.sso_jwt_audience_list = sso_jwt_audience_list

    setup_sso(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties']
    self.assertEqual(ssoProperties[AMBARI_SSO_AUTH_ENABLED], sso_enabled)
    self.assertEqual(ssoProperties[SSO_PROVIDER_URL], sso_provider_url)
    self.assertEqual(ssoProperties[SSO_CERTIFICATE], certificate_data)
    self.assertEqual(ssoProperties[JWT_COOKIE_NAME], sso_jwt_cookie_name)
    self.assertEqual(ssoProperties[JWT_AUDIENCES], sso_jwt_audience_list)
    self.assertEqual(ssoProperties[SSO_MANAGE_SERVICES], "true")
    self.assertEqual(ssoProperties[SSO_ENABLED_SERVICES], "*")


  @patch("urllib2.urlopen")
  @patch("ambari_server.setupSso.perform_changes_via_rest_api")
  @patch("ambari_server.setupSso.get_cluster_name")
  @patch("ambari_server.setupSso.get_YN_input")
  @patch("ambari_server.setupSso.get_ambari_properties")
  @patch("ambari_server.setupSso.get_silent")
  @patch("ambari_server.setupSso.is_server_runing")
  @patch("ambari_server.setupSso.get_json_via_rest_api")
  @patch('__builtin__.open')
  def test_sso_enabled_services_are_collected_via_user_input(self, open_mock,
                                                             get_json_via_rest_api_mock,
                                                             is_server_runing_mock,
                                                             get_silent_mock,
                                                             get_ambari_properties_mock,
                                                             get_YN_input_mock,
                                                             get_cluster_name_mock,
                                                             perform_changes_via_rest_api_mock,
                                                             urlopen_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    certificate_data = '-----BEGIN CERTIFICATE-----\n' \
                       'MIIE3DCCA8SgAwIBAgIJAKfbOMmFyOlNMA0GCSqGSIb3DQEBBQUAMIGkMQswCQYD\n' \
                       '................................................................\n' \
                       'dXRpbmcxFzAVBgNVBAMTDmNsb3VkYnJlYWstcmdsMSUwIwYJKoZIhvcNAQkBFhZy\n' \
                       '-----END CERTIFICATE-----'
    mock_file = MagicFile(certificate_data)
    open_mock.side_effect = [mock_file]

    eligible_services = \
      """
          {
            "href": "http://c7401:8080/api/v1/clusters/cluster1/services?ServiceInfo/sso_integration_supported=true",
            "items": [
              {
                  "href": "http://c7401:8080/api/v1/clusters/cluster1/services/HDFS",
                  "ServiceInfo": {
                      "cluster_name": "cluster1",
                      "service_name": "HDFS",
                      "sso_integration_supported": true,
                      "sso_integration_requires_kerberos": false,
                      "kerberos_enabled": false
                  }
              },
              {
                  "href": "http://c7401:8080/api/v1/clusters/cluster1/services/ZOOKEPER",
                  "ServiceInfo": {
                      "cluster_name": "cluster1",
                      "service_name": "ZOOKEPER",
                      "sso_integration_supported": true,
                      "sso_integration_requires_kerberos": false,
                      "kerberos_enabled": false
                  }
              }
            ]
          }
      """
    eligible_services_json = {
      "href": "http://c7401:8080/api/v1/clusters/cluster1/services?ServiceInfo/sso_integration_supported=true",
      "items": [
        {
          "href": "http://c7401:8080/api/v1/clusters/cluster1/services/HDFS",
          "ServiceInfo": {
            "cluster_name": "cluster1",
            "service_name": "HDFS",
            "sso_integration_supported": True,
            "sso_integration_requires_kerberos": False,
            "kerberos_enabled": False
          }
        },
        {
          "href": "http://c7401:8080/api/v1/clusters/cluster1/services/ZOOKEPER",
          "ServiceInfo": {
            "cluster_name": "cluster1",
            "service_name": "ZOOKEPER",
            "sso_integration_supported": True,
            "sso_integration_requires_kerberos": False,
            "kerberos_enabled": False
          }
        }
      ]
    }

    get_json_via_rest_api_mock.return_value = (200, {})
    get_json_via_rest_api_mock.return_value = (200, eligible_services_json)

    is_server_runing_mock.return_value = (True, 0)
    get_silent_mock.return_value = False
    get_ambari_properties_mock.return_value = Properties()
    get_cluster_name_mock.return_value = 'cluster1'

    def yn_input_side_effect(*args, **kwargs):
      if 'all services' in args[0]:
        return False
      else:
        return True

    get_YN_input_mock.side_effect = yn_input_side_effect

    response = MagicMock()
    response.getcode.return_value = 200
    response.read.return_value = eligible_services
    urlopen_mock.return_value = response

    options = self._create_empty_options_mock()
    options.sso_enabled = 'true'
    options.sso_enabled_ambari = 'true'
    options.sso_manage_services = 'true'
    options.sso_provider_url = 'http://testHost:8080'
    options.sso_public_cert_file = '/test/file/path'
    options.sso_jwt_cookie_name = 'test_cookie'
    options.sso_jwt_audience_list = 'test, audience, list'

    setup_sso(options)

    self.assertTrue(perform_changes_via_rest_api_mock.called)
    requestCall = perform_changes_via_rest_api_mock.call_args_list[0]
    args, kwargs = requestCall
    requestData = args[5]
    self.assertTrue(isinstance(requestData, dict))
    ssoProperties = requestData['Configuration']['properties']
    self.assertEqual(ssoProperties[SSO_MANAGE_SERVICES], "true")
    self.assertEqual(ssoProperties[SSO_ENABLED_SERVICES], "HDFS,ZOOKEPER")

    sys.stdout = sys.__stdout__
    pass

  def _create_empty_options_mock(self):
    options = MagicMock()
    options.sso_enabled = None
    options.sso_enabled_ambari = None
    options.sso_manage_services = None
    options.sso_enabled_services = None
    options.sso_provider_url = None
    options.sso_public_cert_file = None
    options.sso_jwt_cookie_name = None
    options.sso_jwt_audience_list = None
    return options
    