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
from mock.mock import patch, MagicMock, ANY
from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

import os
import sys
from resource_management.core import Environment, Fail
from resource_management.core.resources import File
from resource_management.core.system import System
import resource_management

if get_platform() != PLATFORM_WINDOWS:
  import resource_management.core.providers.system
  import grp
  import pwd
else:
  import resource_management.core.providers.windows.system



@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestFileResource(TestCase):
  @patch.object(os.path, "dirname")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_dir_exist(self, isdir_mock, dirname_mock):
    """
    Tests if 'create' action fails when path is existent directory
    """
    isdir_mock.side_effect = [True, False]
    try:
      with Environment('/') as env:
        File('/existent_directory',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail("Must fail when directory with name 'path' exist")
    except Fail as e:
      self.assertEqual('Applying File[\'/existent_directory\'] failed, directory with name /existent_directory exists',
                       str(e))
    self.assertFalse(dirname_mock.called)

  @patch.object(os.path, "dirname")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_parent_dir_non_exist(self, isdir_mock, dirname_mock):
    """
    Tests if 'create' action fails when parent directory of path
    doesn't exist
    """
    isdir_mock.side_effect = [False, False]
    dirname_mock.return_value = "/non_existent_directory"
    try:
      with Environment('/') as env:
        File('/non_existent_directory/file',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail('Must fail on non existent parent directory')
    except Fail as e:
      self.assertEqual(
        'Applying File[\'/non_existent_directory/file\'] failed, parent directory /non_existent_directory doesn\'t exist',
        str(e))
    self.assertTrue(dirname_mock.called)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.read_file")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_non_existent_file(self, isdir_mock, exists_mock, create_file_mock, read_file_mock, ensure_mock):
    """
    Tests if 'create' action create new non existent file and write proper data
    """
    isdir_mock.side_effect = [False, True]
    exists_mock.return_value = False
    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content'
      )


    create_file_mock.assert_called_once('/directory/file', 'file-content', encoding=None, on_file_created=ANY)
    ensure_mock.assert_called()


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.read_file")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_replace(self, isdir_mock, exists_mock, create_file_mock, read_file_mock, ensure_mock):
    """
    Tests if 'create' action rewrite existent file with new data
    """
    isdir_mock.side_effect = [False, True]
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content'
      )

    read_file_mock.assert_called_with('/directory/file', encoding=None)
    create_file_mock.assert_called_with('/directory/file', 'new-content', encoding=None, on_file_created=ANY)


  @patch("resource_management.core.sudo.unlink")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_delete_is_directory(self, isdir_mock, exist_mock, unlink_mock):
    """
    Tests if 'delete' action fails when path is directory
    """
    isdir_mock.return_value = True

    try:
      with Environment('/') as env:
        File('/directory/file',
             action='delete',
             mode=0777,
             backup=False,
             content='new-content'
        )
      
      self.fail("Should fail when deleting directory")
    except Fail:
      pass

    self.assertEqual(isdir_mock.call_count, 1)
    self.assertEqual(exist_mock.call_count, 0)
    self.assertEqual(unlink_mock.call_count, 0)

  @patch("resource_management.core.sudo.unlink")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_delete(self, isdir_mock, exist_mock, unlink_mock):
    """
    Tests if 'delete' action removes file
    """
    isdir_mock.return_value = False

    with Environment('/') as env:
      File('/directory/file',
           action='delete',
           mode=0777,
           backup=False,
           content='new-content'
      )
    

    self.assertEqual(isdir_mock.call_count, 1)
    self.assertEqual(exist_mock.call_count, 1)
    self.assertEqual(unlink_mock.call_count, 1)


  @patch("resource_management.core.sudo.path_isdir")
  def test_attribute_path(self, isdir_mock):
    """
    Tests 'path' attribute
    """
    isdir_mock.side_effect = [True, False]

    try:
      with Environment('/') as env:
        File('/existent_directory',
             action='create',
             mode=0777,
             content='file-content'
        )
      
      self.fail("Must fail when directory with name 'path' exist")
    except Fail as e:
      pass

    isdir_mock.assert_called_with('/existent_directory')

  @patch.object(resource_management.core.Environment, "backup_file")
  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.read_file")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_attribute_backup(self, isdir_mock, exists_mock, create_file_mock,  read_file_mock, ensure_mock, backup_file_mock):
    """
    Tests 'backup' attribute
    """
    isdir_mock.side_effect = [False, True, False, True]
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content'
      )
    

    self.assertEqual(backup_file_mock.call_count, 0)

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=True,
           content='new-content'
      )
    

    self.assertEqual(backup_file_mock.call_count, 1)
    backup_file_mock.assert_called_with('/directory/file')


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("__builtin__.open")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_attribute_replace(self, isdir_mock, exists_mock, open_mock, ensure_mock):
    """
    Tests 'replace' attribute
    """
    isdir_mock.side_effect = [False, True]
    old_file, new_file = MagicMock(), MagicMock()
    open_mock.side_effect = [old_file, new_file]
    old_file.read.return_value = 'old-content'
    exists_mock.return_value = True

    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           backup=False,
           content='new-content',
           replace=False
      )

    
    old_file.read.assert_called()
    self.assertEqual(new_file.__enter__().write.call_count, 0)
    ensure_mock.assert_called()
    self.assertEqual(open_mock.call_count, 0)


  @patch("pwd.getpwnam")
  @patch("grp.getgrnam")
  @patch("resource_management.core.sudo.chown")
  @patch("resource_management.core.sudo.chmod")
  @patch("resource_management.core.sudo.stat")
  def test_ensure_metadata(self, stat_mock, chmod_mock, chown_mock, getgrnam_mock,
                           getpwnam_mock):
    """
    Tests if _ensure_metadata changes owner, usergroup and permissions of file to proper values
    """
    from resource_management.core.providers.system import _ensure_metadata

    class stat():
      def __init__(self):
        self.st_mode = 0666
        self.st_uid = 1
        self.st_gid = 1

    stat_mock.return_value = stat()
    getpwnam_mock.return_value = MagicMock()
    getpwnam_mock.return_value.pw_uid = 0
    getgrnam_mock.return_value = MagicMock()
    getgrnam_mock.return_value.gr_gid = 0

    with Environment('/') as env:
      _ensure_metadata('/directory/file', user='root', group='hdfs', mode=0777)

    stat_mock.assert_called_with('/directory/file')
    self.assertEqual(chmod_mock.call_count, 1)
    self.assertEqual(chown_mock.call_count, 1)
    getgrnam_mock.assert_called_once_with('hdfs')
    getpwnam_mock.assert_called_with('root')

    chmod_mock.reset_mock()
    chown_mock.reset_mock()
    getpwnam_mock.return_value = MagicMock()
    getpwnam_mock.return_value.pw_uid = 1
    getgrnam_mock.return_value = MagicMock()
    getgrnam_mock.return_value.gr_gid = 1

    with Environment('/') as env:
      _ensure_metadata('/directory/file', user='root', group='hdfs', mode=0777)

    self.assertEqual(chmod_mock.call_count, 1)
    chown_mock.assert_called_with('/directory/file', None, None)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.providers.system.FileProvider._get_content")
  @patch("resource_management.core.sudo.read_file")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_encoding(self, isdir_mock, exists_mock, create_file_mock, read_file_mock, get_content_mock ,ensure_mock):

    isdir_mock.side_effect = [False, True]
    content_mock = MagicMock()
    old_content_mock = MagicMock()
    get_content_mock.return_value = content_mock
    read_file_mock.return_value = old_content_mock
    exists_mock.return_value = True
    with Environment('/') as env:
      File('/directory/file',
           action='create',
           mode=0777,
           content='file-content',
           encoding = "UTF-8"
      )


    read_file_mock.assert_called_with('/directory/file', encoding='UTF-8')

