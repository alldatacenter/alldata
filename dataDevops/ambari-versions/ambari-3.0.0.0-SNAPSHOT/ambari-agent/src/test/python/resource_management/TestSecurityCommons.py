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

from mock.mock import patch, MagicMock, Mock
from unittest import TestCase
from resource_management.libraries.functions.security_commons import *
from datetime import datetime, timedelta
from tempfile import gettempdir
import os

class TestSecurityCommons(TestCase):
  @patch('os.path.isfile')
  def test_validate_security_config_properties(self, os_path_isfile_mock):

    # Testing with correct values_checks
    params = {}
    params["config_file"] = {}
    params["config_file"]["property1"] = ["firstCase"]
    params["config_file"]["property2"] = ["secondCase"]
    params["config_file"]["property3"] = ["thirdCase"]
    params["config_file"]["property4"] = ["fourthCase"]
    params["config_file"]["property5"] = ["fifthCase"]
    params["config_file"]["property6"] = ["sixthCase"]

    configuration_rules = {}
    configuration_rules["config_file"] = {}
    configuration_rules["config_file"]["value_checks"] = {}
    configuration_rules["config_file"]["value_checks"]["property1"] = ["firstCase"]
    configuration_rules["config_file"]["value_checks"]["property2"] = ["secondCase"]

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      True)  # issues is empty

    #Testing with correct empty_checks
    configuration_rules["config_file"]["empty_checks"] = ["property3", "property4"]

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      True)  # issues is empty

    # Testing with correct read_checks
    configuration_rules["config_file"]["read_checks"] = ["property5", "property6"]

    os_path_isfile_mock.return_value = True
    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      True)  # issues is empty

    # Testing with wrong values_checks
    configuration_rules["config_file"]["value_checks"]["property1"] = ["failCase"]
    configuration_rules["config_file"]["value_checks"]["property2"] = ["failCase2"]

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      False)  # Doesn't return an empty issues

    configuration_rules["config_file"]["value_checks"]["property1"] = ["firstCase"]
    configuration_rules["config_file"]["value_checks"]["property2"] = ["secondCase"]

    # Testing with a property which doesn't exist in params
    configuration_rules["config_file"]["empty_checks"].append("property7")

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      False)  # Doesn't return an empty list

    configuration_rules["config_file"]["empty_checks"].remove("property7")

    # Testing with a property which doesn't exist in params
    configuration_rules["config_file"]["read_checks"].append("property8")

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      False)  # Doesn't return an empty list

    configuration_rules["config_file"]["read_checks"].remove("property8")

    #Testing empty_checks and read_checks with an empty params[config_file][property]
    params["config_file"]["property1"] = [""]
    params["config_file"]["property2"] = [""]
    params["config_file"]["property3"] = [""]
    params["config_file"]["property4"] = [""]
    params["config_file"]["property5"] = [""]
    params["config_file"]["property6"] = [""]

    self.assertEquals(not validate_security_config_properties(params, configuration_rules),
                      False)  # Doesn't return an empty list


  def test_build_expectations(self):

    config_file = 'config_file'

    value_checks = {}
    value_checks["property1"] = ["value1"]
    value_checks["property2"] = ["value2"]

    empty_checks = ["property3", "property4"]

    read_checks = ["property5", "property6"]

    result = build_expectations(config_file, value_checks, empty_checks, read_checks)

    self.assertEquals(len(result[config_file]['value_checks']), len(value_checks))
    self.assertEquals(len(result[config_file]['empty_checks']), len(empty_checks))
    self.assertEquals(len(result[config_file]['read_checks']), len(read_checks))

    # Testing that returns empty dict if is called without values
    result = build_expectations(config_file, [], [], [])

    self.assertEquals(not result[config_file].items(), True)

  def test_get_params_from_filesystem_JAAS(self):
    conf_dir = gettempdir()
    jaas_file = "test_jaas.conf"
    jaas_file_path = conf_dir + os.sep + jaas_file

    # Create temporary test file (mocking a files for reading isn't available for the current version
    # of the library
    with open(jaas_file_path, "w+") as f:
      f.write('Client {\n'
              '  com.sun.security.auth.module.Krb5LoginModule required\n'
              '  useKeyTab=true\n'
              '  storeKey=true\n'
              '  useTicketCache=false\n'
              '  keyTab="/etc/security/keytabs/hbase.service.keytab"\n'
              '  principal="hbase/vp-ambari-ranger-med-0120-2.cs1cloud.internal@EXAMPLE.COM";\n'
              '};\n')

    config_file = {
      jaas_file : FILE_TYPE_JAAS_CONF
    }

    result = get_params_from_filesystem(conf_dir, config_file)
    expected = {
      'test_jaas': {
        'Client': {
          'keyTab': '/etc/security/keytabs/hbase.service.keytab',
          'useTicketCache': 'false',
          'storeKey': 'true',
          'com.sun.security.auth.module.Krb5LoginModule': 'required',
          'useKeyTab': 'true',
          'principal': 'hbase/vp-ambari-ranger-med-0120-2.cs1cloud.internal@EXAMPLE.COM'
        }
      }
    }

    self.assertEquals(expected, result)

    os.unlink(jaas_file_path)

    print result

  @patch('xml.etree.ElementTree.parse')
  @patch('os.path.isfile')
  def test_get_params_from_filesystem(self, file_exists_mock, et_parser_mock):
    file_exists_mock.return_value = True
    conf_dir = gettempdir()
    config_file = {
      "config.xml": FILE_TYPE_XML
    }


    prop1_name_mock = MagicMock()
    prop1_name_mock.text.return_value = 'property1'
    prop1_value_mock = MagicMock()
    prop1_value_mock.text.return_value = 'true'

    prop2_name_mock = MagicMock()
    prop2_name_mock.text.return_value = 'property2'
    prop2_value_mock = MagicMock()
    prop2_value_mock.text.return_value = 'false'

    prop3_name_mock = MagicMock()
    prop3_name_mock.text.return_value = 'property3'
    prop3_value_mock = MagicMock()
    prop3_value_mock.text.return_value = 'true'

    props = []
    props.append([prop1_name_mock, prop1_value_mock])
    props.append([prop2_name_mock, prop2_value_mock])
    props.append([prop3_name_mock, prop3_value_mock])

    element_tree_mock = MagicMock()
    et_parser_mock.return_value = element_tree_mock

    get_root_mock = MagicMock()
    element_tree_mock.getroot.return_value = get_root_mock
    get_root_mock.getchildren.return_value = props

    result = get_params_from_filesystem(conf_dir, config_file)

    # Testing that the mock is called with the correct path
    et_parser_mock.assert_called_with(conf_dir + os.sep + "config.xml")

    #Testing that the dictionary and the list from the result are not empty
    self.assertEquals(not result, False)
    self.assertEquals(not result[result.keys()[0]], False)

    #Testing that returns an empty dictionary if is called with no props
    empty_props = []

    get_root_mock.getchildren.return_value = empty_props

    result = get_params_from_filesystem(conf_dir, config_file)

    self.assertEquals(not result, False)
    self.assertEquals(not result['config'].items(), True)

    #Testing that returns an empty dictionary if is called with empty config_files
    empty_config_file = {}

    result = get_params_from_filesystem(conf_dir, empty_config_file)

    self.assertEquals(not result, True)

    #Test that params returns an exception
    et_parser_mock.reset_mock()
    et_parser_mock.side_effect = Exception("Invalid path")

    try:
      get_params_from_filesystem(conf_dir, config_file)
    except:
      self.assertTrue(True)

  @patch('os.path.exists')
  @patch('os.makedirs')
  @patch('os.path.isfile')
  @patch('ambari_simplejson.load')
  @patch('resource_management.libraries.functions.security_commons.new_cached_exec')
  @patch('__builtin__.open')
  def test_cached_executor(self, open_file_mock, new_cached_exec_mock, ambari_simplejson_load_mock,
                           os_isfile_mock, os_makedirs_mock, os_path_exists_mock):

    # Test that function works when is called with correct parameters
    temp_dir = gettempdir()
    kinit_path = "kinit"
    user = "user"
    hostname ="hostnamne"
    keytab_file ="/etc/security/keytabs/nn.service.keytab"
    principal = "nn/c6405.ambari.apache.org@EXAMPLE.COM"
    key = str(hash("%s|%s" % (principal, keytab_file)))
    expiration_time = 30
    filename = key + "_tmp.txt"
    file_path = temp_dir + os.sep + "kinit_executor_cache"

    os_path_exists_mock.return_value = True
    os_isfile_mock.return_value = True

    output = {}
    output[key] = {}
    output[key] = {"last_successful_execution": str(datetime.now())}

    ambari_simplejson_load_mock.return_value = output

    cached_kinit_executor(kinit_path, user, keytab_file, principal, hostname, temp_dir, expiration_time)
    os_path_exists_mock.assert_called_with(file_path)
    os_isfile_mock.assert_called_with(file_path + os.sep + filename)
    open_file_mock.assert_called_with(file_path + os.sep + filename, 'r')

    # Test that the new_cached_exec function is called if the time spend since the last call is greater than 30 minutes
    last_successful_executation = datetime.now()
    last_successful_executation = last_successful_executation - timedelta(minutes=31)

    output_error = {}
    output_error[key] = {}
    output_error[key] = {"last_successful_execution": str(last_successful_executation)}

    ambari_simplejson_load_mock.reset_mock()
    ambari_simplejson_load_mock.return_value = output_error

    new_cached_exec_mock.return_value = output

    cached_kinit_executor(kinit_path, user, keytab_file, principal, hostname, temp_dir, expiration_time)

    self.assertTrue(new_cached_exec_mock.called)
    new_cached_exec_mock.assert_called_with(key, file_path + os.sep + filename, kinit_path, temp_dir, user, keytab_file, principal, hostname)

    # Test that the makedirs function is called with correct path when the directory doesn't exist
    os_path_exists_mock.return_value = False

    cached_kinit_executor(kinit_path, user, keytab_file, principal, hostname, temp_dir, expiration_time)

    os_makedirs_mock.assert_called_with(file_path)

    # Test that the ambari_simplejson throws an exception
    os_path_exists_mock.return_value = True

    ambari_simplejson_load_mock.reset_mock()
    ambari_simplejson_load_mock.side_effect = Exception("Invalid file")

    try:
      cached_kinit_executor(kinit_path, user, keytab_file, principal, hostname, temp_dir, expiration_time)
    except:
      self.assertTrue(True)

    # Test that the new_cached_exec function is called if the output doesn't have data
    ambari_simplejson_load_mock.reset_mock()
    ambari_simplejson_load_mock.return_value = None

    new_cached_exec_mock.return_value = output

    cached_kinit_executor(kinit_path, user, keytab_file, principal, hostname, temp_dir, expiration_time)

    self.assertTrue(new_cached_exec_mock.called)
    new_cached_exec_mock.assert_called_with(key, file_path + os.sep + filename, kinit_path, temp_dir, user, keytab_file, principal, hostname)
