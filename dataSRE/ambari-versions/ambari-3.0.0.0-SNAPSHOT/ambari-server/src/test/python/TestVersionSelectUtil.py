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

from unittest import TestCase
import os
from mock.mock import patch, MagicMock

from resource_management.core.logger import Logger

class TestVersionSelectUtil(TestCase):
  """
  Class that tests the method of the version_select_util.py file.
  """
  def setUp(self):
    import imp

    Logger.logger = MagicMock()

    self.test_directory = os.path.dirname(os.path.abspath(__file__))
    test_file_path = os.path.join(self.test_directory, '../../../../ambari-common/src/main/python/resource_management/libraries/functions/version_select_util.py')
    with open(test_file_path, 'rb') as fp:
        self.module = imp.load_module('module', fp, test_file_path, ('.py', 'rb', imp.PY_SOURCE))

  @patch('__builtin__.open')
  @patch("resource_management.core.shell.call")
  @patch('os.path.exists')
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool")
  def test_get_component_version_from_symlink(self, get_stack_tool_mock, os_path_exists_mock, call_mock, open_mock):
    stack_expected_version = "2.2.1.0-2175"

    # Mock classes for reading from a file
    class MagicFile(object):
      allowed_names = set(["hadoop-hdfs-namenode",
                           "hadoop-hdfs-datanode",
                           "zookeeper-server",
                           "zookeeper-client"
                           ])
      def read(self, value):
        return (value + " - " + stack_expected_version) if value in self.allowed_names else ("ERROR: Invalid package - " + value)

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self
    pass

    class MagicFile1(MagicFile):
      def read(self):
        return super(MagicFile1, self).read("hadoop-nonexistent-component-name")
    class MagicFile2(MagicFile):
      def read(self):
        return super(MagicFile2, self).read("hadoop-hdfs-namenode")
    class MagicFile3(MagicFile):
      def read(self):
        return super(MagicFile3, self).read("hadoop-hdfs-datanode")

    get_stack_tool_mock.side_effect = [("hdp-select", "/usr/bin/hdp-select", "hdp-select"),
                                       ("hdp-select", "/usr/bin/hdp-select", "hdp-select"),
                                       ("hdp-select", "/usr/bin/hdp-select", "hdp-select"),
                                       ("hdp-select", "/usr/bin/hdp-select", "hdp-select")]
    os_path_exists_mock.side_effect = [False, True, True, True]
    open_mock.side_effect = [MagicFile1(), MagicFile2(), MagicFile3()]
    call_mock.side_effect = [(0, "value will come from MagicFile"), ] * 3

    # Missing stack name
    version = self.module.get_component_version_from_symlink(None, "hadoop-hdfs-datanode")
    self.assertEquals(version, None)
    # Missing component name
    version = self.module.get_component_version_from_symlink("HDP", None)
    self.assertEquals(version, None)

    # Invalid stack name
    version = self.module.get_component_version_from_symlink("StackDoesNotExist", "hadoop-hdfs-datanode")
    self.assertEquals(version, None)
    # Invalid component name
    version = self.module.get_component_version_from_symlink("HDP", "hadoop-nonexistent-component-name")
    self.assertEquals(version, None)

    # Pass
    version = self.module.get_component_version_from_symlink("HDP", "hadoop-hdfs-namenode")
    self.assertEquals(version, stack_expected_version)
    version = self.module.get_component_version_from_symlink("HDP", "hadoop-hdfs-datanode")
    self.assertEquals(version, stack_expected_version)

  @patch('__builtin__.open')
  @patch("resource_management.core.shell.call")
  @patch('os.path.exists')
  @patch("resource_management.libraries.functions.stack_tools.get_stack_tool")
  def test_get_component_version_no_build_ids(self, get_stack_tool_mock, os_path_exists_mock, call_mock, open_mock):
    stack_expected_version = "2.2.1.0"

    # Mock classes for reading from a file
    class MagicFile(object):
      allowed_names = set(["hive-server2",
                           "zookeeper-server"])
      def read(self, value):
        return (value + " - " + stack_expected_version) if value in self.allowed_names else ("ERROR: Invalid package - " + value)

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self
    pass

    class MagicFile1(MagicFile):
      def read(self):
        return super(MagicFile1, self).read("hive-server2")
    class MagicFile2(MagicFile):
      def read(self):
        return super(MagicFile2, self).read("zookeeper-server")

    get_stack_tool_mock.side_effect = [("hdp-select", "/usr/bin/hdp-select", "hdp-select"),
                                       ("hdp-select", "/usr/bin/hdp-select", "hdp-select")]
    os_path_exists_mock.side_effect = [True, True]
    open_mock.side_effect = [MagicFile1(), MagicFile2()]
    call_mock.side_effect = [(0, "value will come from MagicFile"), ] * 2

    # Pass
    version = self.module.get_component_version_from_symlink("HDP", "hive-server2")
    self.assertEquals(version, stack_expected_version)
    version = self.module.get_component_version_from_symlink("HDP", "zookeeper-server")
    self.assertEquals(version, stack_expected_version)
