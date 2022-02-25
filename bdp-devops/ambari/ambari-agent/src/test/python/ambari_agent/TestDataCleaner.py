#!/usr/bin/env python
# -*- coding: utf-8 -*-

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

import unittest
from mock.mock import patch, MagicMock, call, Mock
from ambari_agent import DataCleaner
from ambari_agent import AmbariConfig
import os
from ambari_commons import OSCheck
from only_for_platform import os_distro_value

class TestDataCleaner(unittest.TestCase):

  def setUp(self):
    self.test_dir = [('test_path', [],
                      ['errors-12.txt', 'output-12.txt', 'site-12.pp', 'site-13.pp', 'site-15.pp',
                       'structured-out-13.json', 'command-13.json', 'version'])]
    self.config = MagicMock()
    self.config.get.side_effect = [2592000, (3600 + 1), 10000, "test_path"]
    DataCleaner.logger = MagicMock()

  def test_init_success(self):
    config = MagicMock()
    config.get.side_effect = [2592000, (3600 + 1), 10000, "test_path"]
    DataCleaner.logger.reset_mock()
    cleaner = DataCleaner.DataCleaner(config)
    self.assertFalse(DataCleaner.logger.warn.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_config(self):
    """
    Verify that if the config does not have a property, default values are used.
    """
    DataCleaner.logger.reset_mock()
    config = AmbariConfig.AmbariConfig()
    config.remove_option('agent', 'data_cleanup_max_age')
    config.remove_option('agent', 'data_cleanup_interval')
    config.remove_option('agent', 'data_cleanup_max_size_MB')
    cleaner = DataCleaner.DataCleaner(config)

    self.assertEqual(cleaner.file_max_age, 86400)
    self.assertEqual(cleaner.cleanup_interval, 3600)
    self.assertEqual(cleaner.cleanup_max_size_MB, 10000)

  def test_init_warn(self):
    config = MagicMock()
    config.get.side_effect = [1, (3600 - 1), (10000 + 1), "test_path"]
    DataCleaner.logger.reset_mock()
    cleaner = DataCleaner.DataCleaner(config)
    self.assertTrue(DataCleaner.logger.warn.called)
    self.assertTrue(cleaner.file_max_age == 86400)
    self.assertTrue(cleaner.cleanup_interval == 3600)
    self.assertTrue(cleaner.cleanup_max_size_MB == 10000)

  @patch('os.walk')
  @patch('time.time')
  @patch('os.path.getmtime')
  @patch('os.remove')
  @patch('os.path.getsize')
  def test_cleanup_success(self, sizeMock, remMock, mtimeMock, timeMock, walkMock):
    self.config.reset_mock()
    DataCleaner.logger.reset_mock()

    walkMock.return_value = iter(self.test_dir)
    timeMock.return_value = 2592000 + 2
    mtimeMock.side_effect = [1, 1, 1, 2, 1, 1, 1, 1]
    sizeMock.return_value = 100

    cleaner = DataCleaner.DataCleaner(self.config)
    cleaner.cleanup()

    self.assertTrue(len(remMock.call_args_list) == 6)
    remMock.assert_any_call(os.path.join('test_path', 'errors-12.txt'))
    remMock.assert_any_call(os.path.join('test_path', 'output-12.txt'))
    remMock.assert_any_call(os.path.join('test_path', 'site-12.pp'))
    remMock.assert_any_call(os.path.join('test_path', 'site-15.pp'))
    remMock.assert_any_call(os.path.join('test_path', 'structured-out-13.json'))
    remMock.assert_any_call(os.path.join('test_path', 'command-13.json'))
    pass

  @patch('os.walk')
  @patch('time.time')
  @patch('os.path.getmtime')
  @patch('os.remove')
  @patch('os.path.getsize')
  def test_cleanup_remove_error(self, sizeMock, remMock, mtimeMock, timeMock, walkMock):
    self.config.reset_mock()
    DataCleaner.logger.reset_mock()

    walkMock.return_value = iter(self.test_dir)
    timeMock.return_value = 2592000 + 2
    mtimeMock.side_effect = [1, 1, 1, 2, 1, 1, 1, 1]
    sizeMock.return_value = 100

    def side_effect(arg):
      if arg == os.path.join('test_path', 'site-15.pp'):
        raise Exception("Can't remove file")

    remMock.side_effect = side_effect
    cleaner = DataCleaner.DataCleaner(self.config)
    cleaner.cleanup()

    self.assertTrue(len(remMock.call_args_list) == 6)
    self.assertTrue(DataCleaner.logger.error.call_count == 1)
    pass

if __name__ == "__main__":
  suite = unittest.TestLoader().loadTestsFromTestCase(TestDataCleaner)
  unittest.TextTestRunner(verbosity=2).run(suite)
