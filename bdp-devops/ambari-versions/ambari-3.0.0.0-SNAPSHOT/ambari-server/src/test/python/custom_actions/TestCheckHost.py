# !/usr/bin/env python

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

from stacks.utils.RMFTestCase import *
import json
import os
import socket
from ambari_commons import subprocess32
from ambari_commons import inet_utils, OSCheck
from resource_management import Script, ConfigDictionary
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from mock.mock import patch
from mock.mock import MagicMock
from unittest import TestCase

from check_host import CheckHost

from only_for_platform import get_platform, not_for_platform, only_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler


@patch.object(HostCheckReportFileHandler, "writeHostChecksCustomActionsFile", new=MagicMock())
@patch.object(Logger, 'logger', new=MagicMock())
class TestCheckHost(TestCase):
  current_dir = os.path.dirname(os.path.realpath(__file__))
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("os.path.isfile")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testJavaHomeAvailableCheck(self, structured_out_mock, get_tmp_dir_mock, mock_config, os_isfile_mock):
    # test, java home exists
    os_isfile_mock.return_value = True
    get_tmp_dir_mock.return_value = "/tmp"
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "java_home_check",
                                                   "java_home" : "test_java_home"}}

    checkHost = CheckHost()
    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'java_home_check': {'message': 'Java home exists!',
                                                                                'exit_code': 0}})
    # test, java home doesn't exist
    os_isfile_mock.reset_mock()
    os_isfile_mock.return_value = False

    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'java_home_check': {"message": "Java home doesn't exist!",
                                                                                "exit_code" : 1}})

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("tempfile.mkdtemp", new = MagicMock(return_value='/tmp/jdk_tmp_dir'))
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("check_host.download_file")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("check_host.format")
  @patch("os.path.isfile")
  @patch("resource_management.core.shell.call")
  def testDBConnectionCheck(self, shell_call_mock, isfile_mock, format_mock, structured_out_mock, download_file_mock, get_tmp_dir_mock, mock_config):
    # test, download DBConnectionVerification.jar failed
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "mysql",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"},
                                "ambariLevelParams": {"custom_mysql_jdbc_name" : "mysql-connector-java.jar"},
                                "agentLevelParams":{"agentCacheDir": "/nonexistent_tmp"}
                                }
    get_tmp_dir_mock.return_value = "/tmp"
    download_file_mock.side_effect = Exception("test exception")
    isfile_mock.return_value = True
    checkHost = CheckHost()

    try:
      checkHost.actionexecute(None)
      self.fail("DB Check should be failed")
    except Fail:
      pass

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'Error downloading ' \
                     'DBConnectionVerification.jar from Ambari Server resources. Check network access to Ambari ' \
                     'Server.\ntest exception', 'exit_code': 1}})

    # test, download jdbc driver failed
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "oracle",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"},
                                "agentLevelParams": { "agentCacheDir": "/nonexistent_tmp"},
                                "ambariLevelParams": { "custom_oracle_jdbc_name" : "oracle-jdbc-driver.jar"}}
    format_mock.reset_mock()
    download_file_mock.reset_mock()
    p = MagicMock()
    download_file_mock.side_effect = [p, Exception("test exception")]

    try:
      checkHost.actionexecute(None)
      self.fail("DB Check should be failed")
    except Fail:
      pass

    self.assertEquals(format_mock.call_args[0][0], 'Error: Ambari Server cannot download the database JDBC driver '
                  'and is unable to test the database connection. You must run ambari-server setup '
                  '--jdbc-db={db_name} --jdbc-driver=/path/to/your/{db_name}/driver.jar on the Ambari '
                  'Server host to make the JDBC driver available for download and to enable testing '
                  'the database connection.\n')
    self.assertEquals(structured_out_mock.call_args[0][0]['db_connection_check']['exit_code'], 1)

    # test, no connection to remote db
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "postgres",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"},
                                "agentLevelParams": { "agentCacheDir": "/nonexistent_tmp"},
                                "ambariLevelParams": { "custom_postgres_jdbc_name" : "oracle-jdbc-driver.jar"}}
    format_mock.reset_mock()
    download_file_mock.reset_mock()
    download_file_mock.side_effect = [p, p]
    shell_call_mock.return_value = (1, "test message")

    try:
      checkHost.actionexecute(None)
      self.fail("DB Check should be failed")
    except Fail:
      pass

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'test message',
                                                                                    'exit_code': 1}})
    self.assertEquals(format_mock.call_args[0][0],'{java_exec} -cp {check_db_connection_path}{class_path_delimiter}'
            '{jdbc_jar_path} -Djava.library.path={java_library_path} org.apache.ambari.server.DBConnectionVerification'
            ' "{db_connection_url}" "{user_name}" {user_passwd!p} {jdbc_driver_class}')

    # test, db connection success
    download_file_mock.reset_mock()
    download_file_mock.side_effect = [p, p]
    shell_call_mock.return_value = (0, "test message")

    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check':
                                        {'message': 'DB connection check completed successfully!', 'exit_code': 0}})

    #test jdk_name and java home are not available
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "db_name" : "postgres"},
                                "agentLevelParams": { "agentCacheDir": "/nonexistent_tmp"},
                                "ambariLevelParams": { "custom_postgres_jdbc_name" : "test-postgres-jdbc.jar"}}

    isfile_mock.return_value = False

    try:
      checkHost.actionexecute(None)
      self.fail("DB Check should be failed")
    except Fail:
      pass

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'Custom java is not ' \
            'available on host. Please install it. Java home should be the same as on server. \n', 'exit_code': 1}})
    pass


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("socket.gethostbyname")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testHostResolution(self, structured_out_mock, get_tmp_dir_mock, mock_config, mock_socket):
    mock_socket.return_value = "192.168.1.1"    
    jsonFilePath = os.path.join(TestCheckHost.current_dir+"/../../resources/custom_actions", "check_host_ip_addresses.json")
    
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)
 
    mock_config.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "/tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)
    
    # ensure the correct function was called
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [], 
       'message': 'All hosts resolved to an IP address.', 
       'failed_count': 0, 
       'success_count': 5, 
       'exit_code': 0,
       'hosts_with_failures': []}})
    
    # try it now with errors
    mock_socket.side_effect = socket.error
    checkHost.actionexecute(None)
    
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [
                    {'cause': (), 'host': u'c6401.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6402.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6403.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'foobar', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'!!!', 'type': 'FORWARD_LOOKUP'}], 
       'message': 'There were 5 host(s) that could not resolve to an IP address.', 
       'failed_count': 5, 'success_count': 0, 'exit_code': 0, 'hosts_with_failures': [u'c6401.ambari.apache.org',
                                                                                      u'c6402.ambari.apache.org',
                                                                                      u'c6403.ambari.apache.org',
                                                                                      u'foobar', u'!!!']}})
    pass

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testInvalidCheck(self, structured_out_mock, get_tmp_dir_mock, mock_config):
    jsonFilePath = os.path.join(TestCheckHost.current_dir+"/../../resources/custom_actions", "invalid_check.json")
    
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)
 
    mock_config.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)
    
    # ensure the correct function was called
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({})
    pass


  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("platform.system")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch('resource_management.libraries.script.Script.put_structured_out')
  @patch('ambari_agent.HostInfo.HostInfoLinux.javaProcs')
  @patch('ambari_agent.HostInfo.HostInfoLinux.checkLiveServices')
  @patch('ambari_agent.HostInfo.HostInfoLinux.getUMask')
  @patch('ambari_agent.HostInfo.HostInfoLinux.getTransparentHugePage')
  @patch('ambari_agent.HostInfo.HostInfoLinux.checkFirewall')
  @patch('ambari_agent.HostInfo.HostInfoLinux.checkReverseLookup')
  @patch('time.time')
  def testLastAgentEnv(self, time_mock, checkReverseLookup_mock, checkFirewall_mock, getTransparentHugePage_mock,
                       getUMask_mock, checkLiveServices_mock, javaProcs_mock, put_structured_out_mock,
                       get_tmp_dir_mock, get_config_mock, systemmock):
    jsonFilePath = os.path.join(TestCheckHost.current_dir+"/../../resources/custom_actions", "check_last_agent_env.json")
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)

    get_config_mock.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "/tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)

    # ensure the correct function was called
    self.assertTrue(time_mock.called)
    self.assertTrue(checkReverseLookup_mock.called)
    self.assertTrue(checkFirewall_mock.called)
    self.assertTrue(getTransparentHugePage_mock.called)
    self.assertTrue(getUMask_mock.called)
    self.assertTrue(checkLiveServices_mock.called)
    self.assertTrue(javaProcs_mock.called)
    self.assertTrue(put_structured_out_mock.called)
    # ensure the correct keys are in the result map
    last_agent_env_check_result = put_structured_out_mock.call_args[0][0]
    self.assertTrue('last_agent_env_check' in last_agent_env_check_result)
    self.assertTrue('hostHealth' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('firewallRunning' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('firewallName' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('reverseLookup' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('alternatives' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('umask' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('stackFoldersAndFiles' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('existingUsers' in last_agent_env_check_result['last_agent_env_check'])

    # try it now with errors
    javaProcs_mock.side_effect = Exception("test exception")
    checkHost.actionexecute(None)

    #ensure the correct response is returned
    put_structured_out_mock.assert_called_with({'last_agent_env_check': {'message': 'test exception', 'exit_code': 1}})
    pass

  @only_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("platform.system")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch('resource_management.libraries.script.Script.put_structured_out')
  @patch('ambari_agent.HostInfo.HostInfoWindows.javaProcs')
  @patch('ambari_agent.HostInfo.HostInfoWindows.checkLiveServices')
  @patch('ambari_agent.HostInfo.HostInfoWindows.getUMask')
  @patch('ambari_agent.HostInfo.HostInfoWindows.checkFirewall')
  @patch('ambari_agent.HostInfo.HostInfoWindows.checkReverseLookup')
  @patch('time.time')
  def testLastAgentEnv(self, time_mock, checkReverseLookup_mock, checkFirewall_mock,
                       getUMask_mock, checkLiveServices_mock, javaProcs_mock, put_structured_out_mock,
                       get_tmp_dir_mock, get_config_mock, systemmock):
    jsonFilePath = os.path.join(TestCheckHost.current_dir, "..", "..", "resources", "custom_actions", "check_last_agent_env.json")
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)

    get_config_mock.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "/tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)

    # ensure the correct function was called
    self.assertTrue(time_mock.called)
    self.assertTrue(checkReverseLookup_mock.called)
    self.assertTrue(checkFirewall_mock.called)
    self.assertTrue(getUMask_mock.called)
    self.assertTrue(checkLiveServices_mock.called)
    self.assertTrue(javaProcs_mock.called)
    self.assertTrue(put_structured_out_mock.called)
    # ensure the correct keys are in the result map
    last_agent_env_check_result = put_structured_out_mock.call_args[0][0]
    self.assertTrue('last_agent_env_check' in last_agent_env_check_result)
    self.assertTrue('hostHealth' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('firewallRunning' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('firewallName' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('alternatives' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('umask' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('stackFoldersAndFiles' in last_agent_env_check_result['last_agent_env_check'])
    self.assertTrue('existingUsers' in last_agent_env_check_result['last_agent_env_check'])

    # try it now with errors
    javaProcs_mock.side_effect = Exception("test exception")
    checkHost.actionexecute(None)

    #ensure the correct response is returned
    put_structured_out_mock.assert_called_with({'last_agent_env_check': {'message': 'test exception', 'exit_code': 1}})
    pass


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = ('debian','7','Final')))
  @patch.object(HostCheckReportFileHandler, "resolve_ambari_config")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch.object(Script, 'get_tmp_dir')
  @patch.object(Script, 'get_config')
  @patch("os.path.isfile")
  @patch('__builtin__.open')
  def testTransparentHugePage(self, open_mock, os_path_isfile_mock, mock_config,
                              get_tmp_dir_mock, structured_out_mock,
                              resolve_config_mock):
    context_manager_mock = MagicMock()
    open_mock.return_value = context_manager_mock
    file_mock = MagicMock()
    file_mock.read.return_value = "[never] always"
    enter_mock = MagicMock()
    enter_mock.return_value = file_mock
    enter_mock = MagicMock()
    enter_mock.return_value = file_mock
    exit_mock  = MagicMock()
    setattr( context_manager_mock, '__enter__', enter_mock )
    setattr( context_manager_mock, '__exit__', exit_mock )
    os_path_isfile_mock.return_value = True
    get_tmp_dir_mock.return_value = "/tmp"
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "transparentHugePage"}}

    checkHost = CheckHost()
    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'transparentHugePage' : {'message': 'never', 'exit_code': 0}})

    # case 2, file not exists
    os_path_isfile_mock.return_value = False
    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'transparentHugePage' : {'message': '', 'exit_code': 0}})


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("tempfile.mkdtemp", new = MagicMock(return_value='/tmp/jdk_tmp_dir'))
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("check_host.download_file")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("os.path.isfile")
  @patch("resource_management.core.shell.call")
  def testUnsupportedDatabaseType(self, isfile_mock, format_mock, structured_out_mock, download_file_mock, get_tmp_dir_mock, mock_config):
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "unsupported_db",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"},
                                "hostLevelParams": { "agentCacheDir": "/nonexistent_tmp",
                                                     "custom_mysql_jdbc_name" : "mysql-connector-java.jar"}
                                }
    get_tmp_dir_mock.return_value = "/tmp"
    download_file_mock.side_effect = Exception("test exception")
    isfile_mock.return_value = True
    checkHost = CheckHost()

    try:
      checkHost.actionexecute(None)
      self.fail("DB Check should be failed")
    except Fail:
      pass

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': '\'unsupported_db\' database type not supported.', 'exit_code': 1}})


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def testBuildUrl(self, ):
    checkHost = CheckHost()

    self.assertEquals('base_url/path', checkHost.build_url('base_url', 'path'))
    self.assertEquals('base_url/path', checkHost.build_url('base_url/', 'path'))
    self.assertEquals('base_url/path', checkHost.build_url('base_url/', '/path'))
    self.assertEquals('base_url/path', checkHost.build_url('base_url', '/path'))
