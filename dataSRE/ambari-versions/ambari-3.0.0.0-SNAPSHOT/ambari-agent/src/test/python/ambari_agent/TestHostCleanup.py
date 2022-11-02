#!/usr/bin/env python

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
import unittest
from mock.mock import patch, Mock, MagicMock, call, create_autospec
from ambari_agent import HostCleanup
import StringIO
import sys
import tempfile
import os.path
import optparse
import logging
from ambari_commons import OSCheck

PACKAGE_SECTION = "packages"
PACKAGE_KEY = "pkg_list"
USER_SECTION = "users"
USER_KEY = "usr_list"
REPO_SECTION = "repositories"
REPOS_KEY = "pkg_list"
DIR_SECTION = "directories"
DIR_KEY = "dir_list"
PROCESS_SECTION = "processes"
PROCESS_KEY = "proc_list"
PROCESS_OWNER_KEY = "proc_owner_list"
ALT_SECTION = "alternatives"
ALT_KEYS = ["symlink_list", "target_list"]
ALT_ERASE_CMD = "alternatives --remove {0} {1}"
USER_HOMEDIR_SECTION = "usr_homedir"

hostcheck_result_fileContent = """
[processes]
proc_list = 323,434
proc_owner_list = abc,efg

[users]
usr_list = rrdcached,ambari-qa,hive,oozie,hbase,hcat,mysql,mapred,hdfs,zookeeper,sqoop

[repositories]
repo_list = HDP-1.3.0,HDP-epel

[directories]
dir_list = /etc/hadoop,/etc/hbase,/etc/hcatalog,/tmp/hive

[alternatives]
symlink_list = hcatalog-conf,hadoop-default,hadoop-log,oozie-conf
target_list = /etc/hcatalog/conf.dist,/usr/share/man/man1/hadoop.1.gz,/etc/oozie/conf.dist,/usr/lib/hadoop

[packages]
pkg_list = sqoop.noarch,hadoop-libhdfs.x86_64,rrdtool.x86_64,ganglia-gmond.x86_64

[metadata]
created = 2013-07-02 20:39:22.162757"""

from only_for_platform import only_for_platform, not_for_platform, PLATFORM_WINDOWS, PLATFORM_LINUX

@not_for_platform(PLATFORM_WINDOWS)
class TestHostCleanup(TestCase):

  def setUp(self):
    HostCleanup.logger = MagicMock()
    self.hostcleanup = HostCleanup.HostCleanup()
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @patch("os.listdir", create=True, autospec=True)
  def test_read_host_check_file_with_content(self, os_listdir_mock):
    out = StringIO.StringIO()
    sys.stdout = out
    tmpfile = tempfile.mktemp()
    f = open(tmpfile,'w')
    f.write(hostcheck_result_fileContent)
    f.close()

    os_listdir_mock.return_value = ['111']
    tf2 = tempfile.mktemp()
    f2 = open(tf2,'w')
    f2.write('java_home|hadoop')
    f2.close()
    with patch('os.path.join') as patch_join_mock:
      patch_join_mock.return_value = f2.name
      propMap = self.hostcleanup.read_host_check_file(tmpfile)

    self.assertTrue('434' in propMap["processes"]["proc_list"])
    self.assertTrue('abc' in propMap["processes"]["proc_owner_list"])
    self.assertTrue("mysql" in propMap["users"])
    self.assertTrue("HDP-epel" in propMap["repositories"])
    self.assertTrue("/etc/hadoop" in propMap["directories"])
    self.assertTrue("hcatalog-conf" in propMap["alternatives"]["symlink_list"])
    self.assertTrue("/etc/oozie/conf.dist" in propMap["alternatives"]["target_list"])
    self.assertTrue("hadoop-libhdfs.x86_64" in propMap["packages"])
    sys.stdout = sys.__stdout__

  class HostCleanupOptions:
    def __init__(self, outputfile, inputfiles, skip, verbose, silent, java_home):
      self.outputfile = outputfile
      self.inputfiles = inputfiles
      self.skip = skip
      self.verbose = verbose
      self.silent = silent
      self.java_home = java_home

  @patch.object(HostCleanup.HostCleanup, 'do_clear_cache')
  @patch.object(HostCleanup, 'get_YN_input')
  @patch.object(HostCleanup.HostCleanup, 'do_cleanup')
  @patch.object(HostCleanup.HostCleanup, 'is_current_user_root')
  @patch.object(logging.FileHandler, 'setFormatter')
  @patch.object(HostCleanup.HostCleanup,'read_host_check_file')
  @patch.object(logging,'basicConfig')
  @patch.object(logging, 'FileHandler')
  @patch.object(optparse.OptionParser, 'parse_args')
  def test_options(self, parser_mock, file_handler_mock, logging_mock, read_host_check_file_mock,
                   set_formatter_mock, user_root_mock, do_cleanup_mock, get_yn_input_mock, clear_cache_mock):
    open('/tmp/someinputfile1', 'a').close()
    open('/tmp/someinputfile2', 'a').close()
    parser_mock.return_value = (TestHostCleanup.HostCleanupOptions('/someoutputfile', '/tmp/someinputfile1,/tmp/someinputfile2', '', False,
                                                                   False, 'java_home'), [])
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    user_root_mock.return_value = True
    get_yn_input_mock.return_value = True
    HostCleanup.main()

    # test --out
    file_handler_mock.assert_called_with('/someoutputfile')
    # test --skip
    self.assertEquals([''],HostCleanup.SKIP_LIST)
    #test --verbose
    logging_mock.assert_called_with(level=logging.INFO)
    # test --in
    read_host_check_file_mock.assert_called_with('tmp_hostcheck.result')
    self.assertTrue(get_yn_input_mock.called)

  @patch.object(HostCleanup.HostCleanup, 'get_files_in_dir')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  def test_clear_cache(self, erase_files_mock, get_files_mock):
    old_data = HostCleanup.CACHE_FILES_PATTERN

    HostCleanup.CACHE_FILES_PATTERN = {
      'somedir': ['*.txt']
    }

    files_list = ['/tmp/somedir/test.txt']
    get_files_mock.return_value = files_list

    self.hostcleanup.do_clear_cache('/tmp')


    get_files_mock.assert_called_with('/tmp/somedir', '*.txt')
    erase_files_mock.assert_called_with(files_list)

    HostCleanup.CACHE_FILES_PATTERN = old_data

  @patch.object(HostCleanup.HostCleanup, 'do_clear_cache')
  @patch.object(HostCleanup, 'get_YN_input')
  @patch.object(HostCleanup.HostCleanup, 'do_cleanup')
  @patch.object(HostCleanup.HostCleanup, 'is_current_user_root')
  @patch.object(logging.FileHandler, 'setFormatter')
  @patch.object(HostCleanup.HostCleanup,'read_host_check_file')
  @patch.object(logging,'basicConfig')
  @patch.object(logging, 'FileHandler')
  @patch.object(optparse.OptionParser, 'parse_args')
  def test_options_silent(self, parser_mock, file_handler_mock, logging_mock, read_host_check_file_mock,
                   set_formatter_mock, user_root_mock, do_cleanup_mock, get_yn_input_mock, clear_cache_mock):
    open('/tmp/someinputfile1', 'a').close()
    open('/tmp/someinputfile2', 'a').close()
    parser_mock.return_value = (TestHostCleanup.HostCleanupOptions('/someoutputfile', '/tmp/someinputfile1,/tmp/someinputfile2', '', False,
                                                                   True, 'java_home'), [])
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    user_root_mock.return_value = True
    get_yn_input_mock.return_value = True
    HostCleanup.main()

    # test --out
    file_handler_mock.assert_called_with('/someoutputfile')
    # test --skip
    self.assertEquals([''],HostCleanup.SKIP_LIST)
    #test --verbose
    logging_mock.assert_called_with(level=logging.INFO)
    # test --in
    read_host_check_file_mock.assert_called_with('tmp_hostcheck.result')
    self.assertFalse(get_yn_input_mock.called)

  @patch.object(HostCleanup.HostCleanup, 'do_clear_cache')
  @patch.object(HostCleanup.HostCleanup, 'get_additional_dirs')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_alternatives')
  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(OSCheck, "get_os_type")
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_all(self, do_erase_packages_method, do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method,
                      do_erase_alternatives_method, get_additional_dirs_method, clear_cache_mock):
    out = StringIO.StringIO()
    sys.stdout = out
    get_additional_dirs_method.return_value = ['/tmp/hadoop-yarn','/tmp/hsperfdata_007']
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:{PROCESS_KEY:['1234']},
                   ALT_SECTION:{ALT_KEYS[0]:['alt1','alt2'], ALT_KEYS[1]:[
                     'dir1']}, USER_HOMEDIR_SECTION:['decf']}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertTrue(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertTrue(do_erase_files_silent_method.called)
    self.assertTrue(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    self.assertTrue(do_erase_alternatives_method.called)
    calls = [call(['decf']), call(['abcd', 'pqrst']), call(['/tmp/hadoop-yarn','/tmp/hsperfdata_007'])]
    do_erase_dir_silent_method.assert_has_calls(calls)
    do_erase_packages_method.assert_called_once_with(['abcd', 'pqrst'])
    do_erase_files_silent_method.assert_called_once_with(['abcd', 'pqrst'])
    do_delete_users_method.assert_called_once_with(['abcd', 'pqrst'])
    do_kill_processes_method.assert_called_once_with(['1234'])
    do_erase_alternatives_method.assert_called_once_with({ALT_KEYS[0]:['alt1',
                                              'alt2'], ALT_KEYS[1]:['dir1']})

    sys.stdout = sys.__stdout__

  @patch.object(HostCleanup.HostCleanup, 'do_clear_cache')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_by_owner')
  @patch.object(HostCleanup.HostCleanup, 'get_user_ids')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_alternatives')
  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(OSCheck, "get_os_type")
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_default(self, do_erase_packages_method, do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method,
                      do_erase_alternatives_method, get_user_ids_method,
                      do_delete_by_owner_method, clear_cache_mock):

    global SKIP_LIST
    oldSkipList = HostCleanup.SKIP_LIST
    HostCleanup.SKIP_LIST = ["users"]
    out = StringIO.StringIO()
    sys.stdout = out
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:{PROCESS_KEY:['abcd', 'pqrst']},
                   ALT_SECTION:{ALT_KEYS[0]:['alt1','alt2'], ALT_KEYS[1]:[
                     'dir1']}}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertFalse(do_delete_by_owner_method.called)
    self.assertFalse(get_user_ids_method.called)
    self.assertFalse(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertTrue(do_erase_files_silent_method.called)
    self.assertTrue(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    self.assertTrue(do_erase_alternatives_method.called)
    HostCleanup.SKIP_LIST = oldSkipList
    sys.stdout = sys.__stdout__

  @patch("os.stat")
  @patch("os.path.join")
  @patch("os.listdir")
  @patch.object(HostCleanup.HostCleanup, 'do_clear_cache')
  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(OSCheck, "get_os_type")
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_with_skip(self, do_erase_packages_method,
                      do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method,
                      clear_cache_mock, listdir_mock, join_mock, stat_mock):

    out = StringIO.StringIO()
    sys.stdout = out
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:{PROCESS_KEY:['abcd', 'pqrst']}}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']
    HostCleanup.SKIP_LIST = [PACKAGE_SECTION, REPO_SECTION]

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertTrue(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertFalse(do_erase_files_silent_method.called)
    self.assertFalse(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    calls = [call(None), call(['abcd', 'pqrst'])]
    do_erase_dir_silent_method.assert_has_calls(calls)
    do_delete_users_method.assert_called_once_with(['abcd', 'pqrst'])
    do_kill_processes_method.assert_called_once_with(['abcd', 'pqrst'])

    sys.stdout = sys.__stdout__

  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch("os.stat")
  @patch("os.path.join")
  @patch("os.listdir")
  def test_do_delete_by_owner(self, listdir_mock, join_mock, stat_mock, do_erase_dir_silent_method):
    listdir_mock.return_value = ["k", "j"]
    join_mock.return_value = "path"
    response = MagicMock()
    response.st_uid = 1
    stat_mock.return_value = response
    self.hostcleanup.do_delete_by_owner([1, 2], ["a"])
    self.assertTrue(do_erase_dir_silent_method.called)
    calls = [call(["path"]), call(["path"])]
    do_erase_dir_silent_method.assert_has_calls(calls)

  @patch.object(HostCleanup.HostCleanup, 'run_os_command')
  def test_do_delete_users(self, run_os_command_mock):
    run_os_command_mock.return_value = (1, "", "")
    self.hostcleanup.do_delete_users(["a", "b"])
    self.assertTrue(run_os_command_mock.called)
    calls = [call('userdel -rf a'), call('userdel -rf b'), call('groupdel hadoop')]
    run_os_command_mock.assert_has_calls(calls)

  @patch("os.listdir", create=True, autospec=True)
  def test_read_host_check_file(self, os_listdir_mock):
    out = StringIO.StringIO()
    sys.stdout = out
    tmpfile = tempfile.mktemp()
    f = open(tmpfile,'w')
    f.write(hostcheck_result_fileContent)
    f.close()

    propertyMap = self.hostcleanup.read_host_check_file(tmpfile)

    self.assertTrue(propertyMap.has_key(PACKAGE_SECTION))
    self.assertTrue(propertyMap.has_key(REPO_SECTION))
    self.assertTrue(propertyMap.has_key(USER_SECTION))
    self.assertTrue(propertyMap.has_key(DIR_SECTION))
    self.assertTrue(propertyMap.has_key(PROCESS_SECTION))
    self.assertEquals(propertyMap[PROCESS_SECTION][PROCESS_KEY][0], "323")

    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'run_os_command')
  @patch.object(OSCheck, "get_os_type")
  def test_do_erase_packages(self, get_os_type_method, run_os_command_method):
    out = StringIO.StringIO()
    sys.stdout = out

    get_os_type_method.return_value = 'redhat'
    run_os_command_method.return_value = (0, 'success', 'success')

    retval = self.hostcleanup.do_erase_packages(['abcd', 'wxyz'])

    self.assertTrue(get_os_type_method.called)
    self.assertTrue(run_os_command_method.called)
    run_os_command_method.assert_called_with("yum erase -y {0}".format(' '
    .join(['abcd', 'wxyz'])))
    self.assertEquals(0, retval)

    get_os_type_method.reset()
    run_os_command_method.reset()

    get_os_type_method.return_value = 'suse'
    run_os_command_method.return_value = (0, 'success', 'success')

    retval = self.hostcleanup.do_erase_packages(['abcd', 'wxyz'])

    self.assertTrue(get_os_type_method.called)
    self.assertTrue(run_os_command_method.called)
    run_os_command_method.assert_called_with("zypper -n -q remove {0}"
    .format(' '.join(['abcd', 'wxyz'])))
    self.assertEquals(0, retval)

    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'get_files_in_dir')
  @patch.object(OSCheck, "get_os_type")
  def test_find_repo_files_for_repos(self, get_os_type_method,
                                    get_files_in_dir_method):
    out = StringIO.StringIO()
    sys.stdout = out

    tmpfile = tempfile.mktemp()
    fileContent = """[###]
[aass]
[$$]
444]saas[333
1122[naas]2222
name=sd des derft 3.1
"""
    with open(tmpfile,'w') as file:
      file.write(fileContent)
    get_os_type_method.return_value = 'redhat'
    get_files_in_dir_method.return_value = [ tmpfile ]

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['aass'])
    self.assertTrue(get_files_in_dir_method.called)
    self.assertTrue(get_os_type_method.called)
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['sd des derft 3.1'])
    self.assertTrue(get_files_in_dir_method.called)
    self.assertTrue(get_os_type_method.called)
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['sd des derft 3.1', 'aass'])
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['saas'])
    self.assertEquals(repoFiles, [])

    repoFiles = self.hostcleanup.find_repo_files_for_repos([''])
    self.assertEquals(repoFiles, [])

    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'run_os_command')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'get_alternatives_desc')
  def test_do_erase_alternatives(self, get_alternatives_desc_mock,
                    do_erase_dir_silent_mock, run_os_command_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    get_alternatives_desc_mock.return_value = 'somepath to alternative\n'
    run_os_command_mock.return_value = (0, None, None)

    alt_map = {ALT_KEYS[0]:['alt1'], ALT_KEYS[1]:['dir1']}

    self.hostcleanup.do_erase_alternatives(alt_map)

    self.assertTrue(get_alternatives_desc_mock.called)
    get_alternatives_desc_mock.called_once_with('alt1')
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.called_once_with(ALT_ERASE_CMD.format('alt1', 'somepath'))
    self.assertTrue(do_erase_dir_silent_mock.called)
    do_erase_dir_silent_mock.called_once_with(['dir1'])

    sys.stdout = sys.__stdout__

if __name__ == "__main__":
  unittest.main()
