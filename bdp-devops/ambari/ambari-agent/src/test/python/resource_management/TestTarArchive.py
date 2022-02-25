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

from mock.mock import patch, MagicMock
from unittest import TestCase

from ambari_commons.os_check import OSCheck
from only_for_platform import os_distro_value
from resource_management.core.environment import Environment
from resource_management.libraries.functions import tar_archive

@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestTarArchive(TestCase):

  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_archive_dir(self, execute_mock):
    archive = '/home/etc.tar.gz'
    directory = '/etc'

    with Environment():
      tar_archive.archive_dir(archive, directory)

    self.assertEqual(execute_mock.call_count, 1)
    self.assertEqual(execute_mock.call_args[0][0].command, ('tar', '-zcf', archive, '-C', directory, '.'))


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_archive_directory_dereference(self, execute_mock):
    archive = '/home/etc.tar.gz'
    directory = '/etc'

    with Environment():
      tar_archive.archive_directory_dereference(archive, directory)

    self.assertEqual(execute_mock.call_count, 1)
    self.assertEqual(execute_mock.call_args[0][0].command, ('tar', '-zchf', archive, '-C', directory, '.'))


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_archive_dir_via_temp_file(self, execute_mock):
    archive = '/home/etc.tar.gz'
    directory = '/etc'

    with Environment():
      tar_archive.archive_dir_via_temp_file(archive, directory)

    self.assertEqual(execute_mock.call_count, 2)
    self.assertEqual(execute_mock.call_args_list[0][0][0].command[:2], ('tar', '-zchf'))
    self.assertEqual(execute_mock.call_args_list[0][0][0].command[3:], ('-C', directory, '.'))
    temp_file = execute_mock.call_args_list[0][0][0].command[2]
    self.assertEqual(execute_mock.call_args_list[1][0][0].command, ('mv', temp_file, archive))
