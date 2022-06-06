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

import os
os.environ["ROOT"] = ""

import mock
from mock.mock import MagicMock, patch
from ambari_server import BackupRestore


class TestBKRestore(unittest.TestCase):

  @patch('os.walk')
  @patch('zipfile.ZipFile')
  @patch.object(BackupRestore, "zipdir")
  def test_perform_backup(self, zipdir_mock, zipfile_mock, oswalk_mock):
    #set up the mocks
    files_mock = MagicMock()
    files_mock.__iter__.return_value = ['file1', 'file2']
    path_mock =  MagicMock()
    zipname_mock = MagicMock()


    bkRestore = BackupRestore.BackupRestore(files_mock, zipname_mock, path_mock)
    bkRestore.perform_backup()

    self.assertEqual(zipfile_mock.call_count, 1)
    self.assertEqual(zipdir_mock.call_count, 1)

    zipfile_mock.side_effect = Exception('Invalid path!')
    try:
      bkRestore.perform_backup()
      self.fail("should throw exception")
    except:
      self.assertTrue(True)


  @patch('zipfile.ZipFile')
  @patch.object(BackupRestore, "unzip")
  def test_perform_restore(self, unzip_mock, zipfile_mock):
    #set up the mocks
    path_mock =  MagicMock()
    zipname_mock = MagicMock()
    files_mock = MagicMock()
    files_mock.__iter__.return_value = ['file1', 'file2']


    bkRestore = BackupRestore.BackupRestore(files_mock, zipname_mock, path_mock)
    bkRestore.perform_restore()

    self.assertEqual(unzip_mock.call_count, 1)

    unzip_mock.side_effect = Exception('Invalid path!')
    try:
      bkRestore.perform_restore()
      self.fail("should throw exception")
    except:
      self.assertTrue(True)



















