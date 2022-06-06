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
import tempfile

from unittest import TestCase
from mock.mock import patch

from only_for_platform import get_platform, not_for_platform, PLATFORM_WINDOWS

if get_platform() != PLATFORM_WINDOWS:
  from validate_configs import ValidateConfigs


@not_for_platform(PLATFORM_WINDOWS)
class TestValidateConfigs(TestCase):

  @patch("os.geteuid")
  def test_check_users(self, geteuid_mock):
    # if excecuting not by root
    geteuid_mock.return_value = 1

    vc = ValidateConfigs()
    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_user" : "root",
      "component_configurations/NAMENODE/hadoop-env/user_group" : "root"
    }
    self.assertEquals(vc.check_users(params), True)
    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_user" : "root",
      "component_configurations/NAMENODE/hadoop-env/user_group" : "wrong_group"
    }
    self.assertEquals(vc.check_users(params), False)
    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_user" : "wrong_user",
      "component_configurations/NAMENODE/hadoop-env/user_group" : "root"
    }
    self.assertEquals(vc.check_users(params), False)
    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_user" : "wrong_user",
      "component_configurations/NAMENODE/hadoop-env/user_group" : "wrong_group"
    }
    self.assertEquals(vc.check_users(params), False)

  def test_check_user_in_group(self):
    vc = ValidateConfigs()

    self.assertTrue(vc.check_user_in_group('root', 'root'))
    self.assertFalse(vc.check_user_in_group('root', 'wrong_group'))
    self.assertFalse(vc.check_user_in_group('wrong_user', 'root'))
    self.assertFalse(vc.check_user_in_group('wrong_user', 'wrong_group'))

  def test_check_directories(self):
    temp_dir = tempfile.mkdtemp()
    vc = ValidateConfigs()

    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_log_dir_prefix" : "/"
    }

    self.assertFalse(vc.check_directories(params))

    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_log_dir_prefix" : temp_dir
    }

    self.assertTrue(vc.check_directories(params))

    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_log_dir_prefix" : temp_dir + '/some_new_dir'
    }

    self.assertTrue(vc.check_directories(params))

  def test_flatten_dict(self):
    init_dict = {
      "a" : "a",
      "b" : {
        "b": "b"
      },
      "c": {
        "c": {
          "c": "c"
        }
      }
    }

    result_list = ['prefix/a/a', 'prefix/c/c/c/c', 'prefix/b/b/b']

    vc = ValidateConfigs()

    self.assertEquals(vc.flatten_dict(init_dict, prefix="prefix"), result_list)

  def test_get_value(self):

    params = {
      "component_configurations/NAMENODE/hadoop-env/hdfs_user" : "root",
      "component_configurations/NAMENODE/hadoop-env/user_group" : "${hdfs_user}"
    }

    vc = ValidateConfigs()

    self.assertEquals(vc.get_value("component_configurations/NAMENODE/hadoop-env/hdfs_user", params), 'root')
    self.assertEquals(vc.get_value("component_configurations/NAMENODE/hadoop-env/user_group", params), 'root')


