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
os.environ["ROOT"] = ""

import os
import shutil
import tempfile
from ambari_commons.exceptions import FatalException
from mock.mock import patch, MagicMock
from unittest import TestCase
from ambari_server.properties import Properties
import platform

from ambari_commons import os_utils
os_utils.search_file = MagicMock(return_value="/tmp/ambari.properties")
import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        from ambari_server.dbConfiguration import get_jdbc_driver_path, get_native_libs_path
        from ambari_server.serverConfiguration import get_conf_dir
        from ambari_server.serverClassPath import ServerClassPath, AMBARI_SERVER_LIB, SERVER_CLASSPATH_KEY, JDBC_DRIVER_PATH_PROPERTY

@patch.object(platform, "linux_distribution", new = MagicMock(return_value=('Redhat', '6.4', 'Final')))
@patch("os.path.isdir", new = MagicMock(return_value=True))
@patch("os.access", new = MagicMock(return_value=True))
@patch("ambari_server.serverConfiguration.search_file", new=MagicMock(return_value="/tmp/ambari.properties"))
class TestConfigs(TestCase):

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def test_server_class_path_default(self, get_conf_dir_mock):
    properties = Properties()
    get_conf_dir_mock.return_value = "/etc/ambari-server/conf"

    expected_classpath = "'/etc/ambari-server/conf:/usr/lib/ambari-server/*'"
    serverClassPath = ServerClassPath(properties, None)
    self.assertEquals(expected_classpath, serverClassPath.get_full_ambari_classpath_escaped_for_shell())

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  @patch("ambari_server.dbConfiguration.get_jdbc_driver_path")
  @patch("ambari_server.dbConfiguration.get_native_libs_path")
  def test_server_class_path_custom_jar(self, get_native_libs_path_mock, get_jdbc_driver_path_mock,
                                     get_conf_dir_mock):
    properties = Properties()
    get_jdbc_driver_path_mock.return_value = "/path/to/jdbc.jar"
    get_native_libs_path_mock.return_value = None
    get_conf_dir_mock.return_value = "/etc/ambari-server/conf"
    os.environ[AMBARI_SERVER_LIB] = "/custom/ambari/jar/location"


    expected_classpath ="'/etc/ambari-server/conf:/custom/ambari/jar/location/*:/path/to/jdbc.jar'"
    serverClassPath = ServerClassPath(properties, MagicMock())
    actual_classpath = serverClassPath.get_full_ambari_classpath_escaped_for_shell()
    del os.environ[AMBARI_SERVER_LIB]
    self.assertEquals(expected_classpath, actual_classpath)

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  @patch("ambari_server.dbConfiguration.get_jdbc_driver_path")
  @patch("ambari_server.dbConfiguration.get_native_libs_path")
  def test_server_class_path_custom_env_classpath(self, get_native_libs_path_mock, get_jdbc_driver_path_mock,
                                        get_conf_dir_mock):
    properties = Properties()
    get_jdbc_driver_path_mock.return_value = "/path/to/jdbc.jar"
    get_native_libs_path_mock.return_value = None
    get_conf_dir_mock.return_value = "/etc/ambari-server/conf"
    os.environ[SERVER_CLASSPATH_KEY] = "/custom/server/env/classpath"

    expected_classpath = "'/etc/ambari-server/conf:/custom/server/env/classpath:/usr/lib/ambari-server/*:/path/to/jdbc.jar'"
    serverClassPath = ServerClassPath(properties, MagicMock())
    actual_classpath = serverClassPath.get_full_ambari_classpath_escaped_for_shell()
    del os.environ[SERVER_CLASSPATH_KEY]
    self.assertEquals(expected_classpath, actual_classpath)

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  @patch("ambari_server.dbConfiguration.get_jdbc_driver_path")
  @patch("ambari_server.dbConfiguration.get_native_libs_path")
  def test_server_class_path_custom_jdbc_path(self, get_native_libs_path_mock, get_jdbc_driver_path_mock,
                                                  get_conf_dir_mock):
    properties = Properties()
    properties.process_pair(JDBC_DRIVER_PATH_PROPERTY, "/ambari/properties/path/to/custom/jdbc.jar")
    get_jdbc_driver_path_mock.return_value = "/path/to/jdbc.jar"
    get_native_libs_path_mock.return_value = None
    get_conf_dir_mock.return_value = "/etc/ambari-server/conf"

    expected_classpath = "'/etc/ambari-server/conf:/usr/lib/ambari-server/*:/ambari/properties/path/to/custom/jdbc.jar:/path/to/jdbc.jar'"
    serverClassPath = ServerClassPath(properties, MagicMock())
    actual_classpath = serverClassPath.get_full_ambari_classpath_escaped_for_shell()
    self.assertEquals(expected_classpath, actual_classpath)

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def test_server_class_path_find_all_jars(self, get_conf_dir_mock):
    temp_dir = tempfile.mkdtemp()
    sub_dir = tempfile.mkdtemp(dir=temp_dir)
    serverClassPath = ServerClassPath(None, None)
    jar0 = tempfile.NamedTemporaryFile(suffix='.jar')
    jar1 = tempfile.NamedTemporaryFile(suffix='.jar', dir=temp_dir)
    jar2 = tempfile.NamedTemporaryFile(suffix='.jar', dir=temp_dir)
    jar3 = tempfile.NamedTemporaryFile(suffix='.jar', dir=sub_dir)
    # test /dir/*:file.jar
    classpath = str(temp_dir) + os.path.sep + "*" + os.path.pathsep + jar0.name
    jars = serverClassPath._find_all_jars(classpath)
    self.assertEqual(len(jars), 3)
    self.assertTrue(jar0.name in jars)
    self.assertTrue(jar1.name in jars)
    self.assertTrue(jar2.name in jars)
    self.assertFalse(jar3.name in jars)

    # test no classpath specified
    try:
      serverClassPath._find_all_jars(None)
      self.fail()
    except FatalException as fe:
      pass

    shutil.rmtree(temp_dir)

  @patch.object(ServerClassPath, "_find_all_jars")
  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def test_server_class_path_validate_classpath(self, get_conf_dir_mock,
                                                find_jars_mock):
    serverClassPath = ServerClassPath(None, None)

    # No jars
    find_jars_mock.return_value = []
    try:
      serverClassPath._validate_classpath(None)
    except:
      self.fail()

    # Correct jars list
    find_jars_mock.return_value = ["ambari-metrics-common-2.1.1.236.jar",
                                   "ambari-server-2.1.1.236.jar",
                                   "jetty-client-8.1.17.v20150415.jar",
                                   "spring-core-3.0.7.RELEASE.jar"]
    try:
      serverClassPath._validate_classpath(None)
    except:
      self.fail()

    # Incorrect jars list, multiple versions for ambari-server.jar
    find_jars_mock.return_value = ["ambari-metrics-common-2.1.1.236.jar",
                                   "ambari-server-2.1.1.236.jar",
                                   "ambari-server-2.1.1.hotfixed.jar",
                                   "jetty-client-8.1.17.v20150415.jar",
                                   "spring-core-3.0.7.RELEASE.jar"]
    try:
      serverClassPath._validate_classpath(None)
      self.fail()
    except:
      pass

    # Incorrect jars list, multiple versions for not ambari-server.jar
    find_jars_mock.return_value = ["ambari-metrics-common-2.1.1.236.jar",
                                   "ambari-server-2.1.1.236.jar",
                                   "jetty-client-8.1.17.v20150415.jar",
                                   "jetty-client-9.jar",
                                   "spring-core-3.0.7.RELEASE.jar"]
    try:
      serverClassPath._validate_classpath(None)
    except:
      self.fail()
