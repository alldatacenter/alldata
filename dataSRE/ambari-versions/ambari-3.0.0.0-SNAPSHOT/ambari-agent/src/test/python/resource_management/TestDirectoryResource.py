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
from ambari_agent import main
main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
from unittest import TestCase
from mock.mock import patch, MagicMock
from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

import os
from resource_management.core.system import System
from resource_management.core import Environment, Fail
from resource_management.core.resources import Directory

if get_platform() != PLATFORM_WINDOWS:
  import pwd
  import grp


@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestDirectoryResource(TestCase):

  @not_for_platform(PLATFORM_WINDOWS)
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.makedirs")
  @patch("resource_management.core.sudo.path_isdir")
  @patch("resource_management.core.sudo.stat")
  @patch("resource_management.core.sudo.chmod")
  @patch("resource_management.core.sudo.chown")
  @patch("pwd.getpwnam")
  @patch("grp.getgrnam")
  def test_create_directory_recursive(self, getgrnam_mock, getpwnam_mock,
                                      os_chown_mock, os_chmod_mock, os_stat_mock,
                                      isdir_mock, os_makedirs_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    isdir_mock.return_value = True
    getpwnam_mock.return_value = MagicMock()
    getpwnam_mock.return_value.pw_uid = 66
    getgrnam_mock.return_value = MagicMock()
    getgrnam_mock.return_value.gr_gid = 77
    
    os_stat_mock.return_value = type("", (), dict(st_mode=0755, st_uid=0, st_gid=0))()
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='create',
           mode=0777,
           owner="hdfs",
           group="hadoop",
           create_parents = True
      )
      
    os_makedirs_mock.assert_called_with('/a/b/c/d', 0777)
    os_chmod_mock.assert_called_with('/a/b/c/d', 0777)
    os_chown_mock.assert_any_call('/a/b/c/d', getpwnam_mock.return_value, getgrnam_mock.return_value)
  
  @patch("resource_management.core.sudo.path_exists")
  @patch.object(os.path, "dirname")
  @patch("resource_management.core.sudo.path_isdir")
  @patch("resource_management.core.sudo.makedir")
  @patch("resource_management.core.sudo.stat")
  @patch("resource_management.core.sudo.chmod")
  @patch("resource_management.core.sudo.chown")
  @patch("pwd.getpwnam")
  @patch("grp.getgrnam")
  def test_create_directory_not_recursive(self, getgrnam_mock, getpwnam_mock,
                                      os_chown_mock, os_chmod_mock, os_stat_mock,
                                      mkdir_mock, isdir_mock, os_dirname_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    os_dirname_mock.return_value = "/a/b/c"
    isdir_mock.return_value = True
    getpwnam_mock.return_value = MagicMock()
    getpwnam_mock.return_value.pw_uid = 66
    getgrnam_mock.return_value = MagicMock()
    getpwnam_mock.return_value.gr_gid = 77
    os_stat_mock.return_value = type("", (), dict(st_mode=0755, st_uid=0, st_gid=0))()
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='create',
           mode=0777,
           owner="hdfs",
           group="hadoop"
      )
      
    mkdir_mock.assert_called_with('/a/b/c/d', 0777)
    os_chmod_mock.assert_called_with('/a/b/c/d', 0777)
    os_chown_mock.assert_any_call('/a/b/c/d', getpwnam_mock.return_value, getgrnam_mock.return_value)
    
  @patch("resource_management.core.sudo.path_exists")
  @patch.object(os.path, "dirname")
  @patch("resource_management.core.sudo.path_isdir")
  def test_create_directory_failed_no_parent(self, isdir_mock, os_dirname_mock, 
                                      os_path_exists_mock):
    os_path_exists_mock.return_value = False
    os_dirname_mock.return_value = "/a/b/c"
    isdir_mock.return_value = False
    
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='create',
             mode=0777,
             owner="hdfs",
             group="hadoop"
        )
      self.fail("Must fail because parent directory /a/b/c doesn't exist")
    except Fail as e:
      self.assertEqual('Applying Directory[\'/a/b/c/d\'] failed, parent directory /a/b/c doesn\'t exist',
                       str(e))

  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_create_directory_path_is_file_or_line(self, isdir_mock, os_path_exists_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = False
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='create',
             mode=0777,
             owner="hdfs",
             group="hadoop"
        )
      self.fail("Must fail because file /a/b/c/d already exists")
    except Fail as e:
      self.assertEqual('Applying Directory[\'/a/b/c/d\'] failed, file /a/b/c/d already exists',
                       str(e))
  
  @patch("resource_management.core.sudo.rmtree")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_delete_directory(self, isdir_mock, os_path_exists_mock, rmtree_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = True
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='delete'
      )
      
    rmtree_mock.assert_called_with('/a/b/c/d')
    
  @patch("resource_management.core.sudo.path_exists")
  def test_delete_noexisting_directory(self, os_path_exists_mock):
    os_path_exists_mock.return_value = False
    
    with Environment('/') as env:
      Directory('/a/b/c/d',
           action='delete'
      )
  
  @patch("resource_management.core.sudo.rmtree")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_delete_directory_with_path_to_file(self, isdir_mock, os_path_exists_mock, rmtree_mock):
    os_path_exists_mock.return_value = True
    isdir_mock.return_value = False
    
    try:
      with Environment('/') as env:
        Directory('/a/b/c/d',
             action='delete'
        )
      self.fail("Must fail because /a/b/c/d is not a directory")
    except Fail as e:
      self.assertEqual('Applying Directory[\'/a/b/c/d\'] failed, /a/b/c/d is not a directory',
                       str(e))