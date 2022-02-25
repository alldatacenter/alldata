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
import os
import logging
from unittest import TestCase
from mock.mock import Mock, MagicMock, patch

from resource_management.libraries.functions import mounted_dirs_helper
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management import Directory
from resource_management.libraries.script.script import Script


class StubParams(object):
  """
  Dummy class to fake params where params.x performs a get on params.dict["x"]
  """
  def __init__(self):
    self.dict = {}

  def __getattr__(self, name):
    return self.dict[name]

  def __repr__(self):
    name = self.__class__.__name__
    mocks = set(dir(self))
    mocks = [x for x in mocks if not str(x).startswith("__")]   # Exclude private methods
    return "<StubParams: {0}; mocks: {1}>".format(name, str(mocks))


def fake_create_dir(directory):
  """
  Fake function used as function pointer.
  """
  print "Fake function to create directory {0}".format(directory)


@patch.object(Script, "get_config", new=MagicMock(return_value={'configurations':{'cluster-env': {'ignore_bad_mounts': False, 'manage_dirs_on_root': True, 'one_dir_per_partition': False}}}))
class TestDatanodeHelper(TestCase):
  """
  Test the functionality of the dfs_datanode_helper.py
  """
  logger = logging.getLogger('TestDatanodeHelper')

  grid0 = "/grid/0/data"
  grid1 = "/grid/1/data"
  grid2 = "/grid/2/data"

  params = StubParams()
  params.data_dir_mount_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
  params.dfs_data_dir = "{0},{1},{2}".format(grid0, grid1, grid2)
  params.hdfs_user = "hdfs_test"
  params.user_group = "hadoop_test"


  @patch("resource_management.libraries.functions.mounted_dirs_helper.Directory")
  @patch.object(Logger, "warning")
  @patch.object(Logger, "info")
  @patch.object(Logger, "error")
  def test_normalized(self, log_error, log_info, warning_info, dir_mock):
    """
    Test that the data dirs are normalized by removing leading and trailing whitespace, and case sensitive.
    """
    params = StubParams()
    params.data_dir_mount_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
    params.dfs_data_dir = "/grid/0/data  ,  /grid/1/data  ,/GRID/2/Data/"

    # Function under test
    mounted_dirs_helper.handle_mounted_dirs(fake_create_dir, params.dfs_data_dir, params.data_dir_mount_file, update_cache=False)

    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]
    for (name, args, kwargs) in log_error.mock_calls:
      print args[0]

    log_info.assert_any_call("Forcefully ensuring existence and permissions of the directory: /grid/0/data")
    log_info.assert_any_call("Forcefully ensuring existence and permissions of the directory: /grid/1/data")
    log_info.assert_any_call("Forcefully ensuring existence and permissions of the directory: /GRID/2/Data/")

    self.assertEquals(0, log_error.call_count)

  @patch("resource_management.libraries.functions.mounted_dirs_helper.Directory")
  @patch.object(Logger, "info")
  @patch.object(Logger, "warning")
  @patch.object(Logger, "error")
  @patch.object(mounted_dirs_helper, "get_dir_to_mount_from_file")
  @patch.object(mounted_dirs_helper, "get_mount_point_for_dir")
  @patch.object(os.path, "isdir")
  @patch.object(os.path, "exists")
  def test_grid_becomes_unmounted(self, mock_os_exists, mock_os_isdir, mock_get_mount_point,
                                  mock_get_data_dir_to_mount_from_file, log_error, log_warning, log_info, dir_mock):
    """
    Test when grid2 becomes unmounted
    """
    mock_os_exists.return_value = True    # Indicate that history file exists

    # Initially, all grids were mounted
    mock_get_data_dir_to_mount_from_file.return_value = {self.grid0: "/dev0", self.grid1: "/dev1", self.grid2: "/dev2"}

    # Grid2 then becomes unmounted
    mock_get_mount_point.side_effect = ["/dev0", "/dev1", "/"] * 2
    mock_os_isdir.side_effect = [False, False, False] + [True, True, True]
    # Function under test
    mounted_dirs_helper.handle_mounted_dirs(fake_create_dir, self.params.dfs_data_dir, self.params.data_dir_mount_file, update_cache=False)
    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]

    error_logs = []
    for (name, args, kwargs) in log_error.mock_calls:
      error_logs.append(args[0])    # this is a one-tuple
    error_msg = "".join(error_logs)

    self.assertEquals(1, log_error.call_count)
    self.assertTrue("Directory /grid/2/data became unmounted from /dev2 . Current mount point: / ."
                    " Please ensure that mounts are healthy. If the mount change was intentional, you can update the contents of "
                    "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist." in error_msg)


  @patch("resource_management.libraries.functions.mounted_dirs_helper.Directory")
  @patch.object(Logger, "info")
  @patch.object(Logger, "warning")
  @patch.object(Logger, "error")
  @patch.object(mounted_dirs_helper, "get_dir_to_mount_from_file")
  @patch.object(mounted_dirs_helper, "get_mount_point_for_dir")
  @patch.object(os.path, "isdir")
  @patch.object(os.path, "exists")
  def test_grid_becomes_remounted(self, mock_os_exists, mock_os_isdir, mock_get_mount_point,
                                  mock_get_data_dir_to_mount_from_file, log_error, log_warning, log_info, dir_mock):
    """
    Test when grid2 becomes remounted
    """
    mock_os_exists.return_value = True    # Indicate that history file exists

    # Initially, all grids were mounted
    mock_get_data_dir_to_mount_from_file.return_value = {self.grid0: "/dev0", self.grid1: "/dev1", self.grid2: "/"}

    # Grid2 then becomes remounted
    mock_get_mount_point.side_effect = ["/dev0", "/dev1", "/dev2"] * 2
    mock_os_isdir.side_effect = [False, False, False] + [True, True, True]

    # Function under test
    mounted_dirs_helper.handle_mounted_dirs(fake_create_dir, self.params.data_dir_mount_file, self.params.data_dir_mount_file, update_cache=False)

    for (name, args, kwargs) in log_info.mock_calls:
      print args[0]

    for (name, args, kwargs) in log_error.mock_calls:
      print args[0]

    self.assertEquals(0, log_error.call_count)

  def test_get_mounts_with_multiple_data_dirs(self):
    self.assertEquals([], mounted_dirs_helper.get_mounts_with_multiple_data_dirs(["/", "/hodoop", "/tmp"], "/hadoop/data,/tmp"))
    self.assertEquals([("/", ["/hadoop/data", "/tmp"])], mounted_dirs_helper.get_mounts_with_multiple_data_dirs(["/"], "/hadoop/data,/tmp"))

  def test_may_manage_folder(self):
    # root, no history file, manage_dirs_on_root = True
    # folder should be managed
    dirs_unmounted=set()
    self.assertEquals(True, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir=None, is_non_root_dir=False, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = True, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set())

    # root, no history file, manage_dirs_on_root = False
    # folder should not be managed
    dirs_unmounted=set()
    self.assertEquals(False, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir=None, is_non_root_dir=False, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = False, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set(['/grid/0/data']))

    # non root, no history file, manage_dirs_on_root = False
    # folder should be managed
    dirs_unmounted=set()
    self.assertEquals(True, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir=None, is_non_root_dir=True, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = False, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set())

    # unmounted to root, manage_dirs_on_root = True
    # folder should not be managed
    dirs_unmounted=set()
    self.assertEquals(False, mounted_dirs_helper._may_manage_folder('/grid/0/data', '/grid/0', True, dirs_unmounted, [], False, '/'))
    self.assertEquals(dirs_unmounted, set(['/grid/0/data']))

    # unmounted to root, manage_dirs_on_root = False
    # folder should not be managed
    dirs_unmounted=set()
    self.assertEquals(False, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir='/grid/0/data', is_non_root_dir=False, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = False, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set(['/grid/0/data']))

    # same mount = root, manage_dirs_on_root = False
    # folder should not be managed
    dirs_unmounted=set()
    self.assertEquals(False, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir='/', is_non_root_dir=False, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = False, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set())

    # same mount = root, manage_dirs_on_root = True
    # folder should be managed
    dirs_unmounted=set()
    self.assertEquals(True, mounted_dirs_helper._may_manage_folder(dir_='/grid/0/data', last_mount_point_for_dir='/', is_non_root_dir=False, dirs_unmounted=dirs_unmounted, error_messages = [], manage_dirs_on_root = True, curr_mount_point = '/'))
    self.assertEquals(dirs_unmounted, set())

    # mount changed to non root, manage_dirs_on_root = False
    # folder should not be managed
    dirs_unmounted=set()
    self.assertEquals(False, mounted_dirs_helper._may_manage_folder('/grid/0/data', '/', True, dirs_unmounted, [], False, '/grid/0'))
    self.assertEquals(dirs_unmounted, set(['/grid/0/data']))
