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

from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, call, patch
from resource_management import Hook
import itertools
import getpass
import os

@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeInstall(RMFTestCase):
  TMP_PATH = '/tmp/hbase-hbase'

  @patch("os.path.isfile")
  @patch.object(getpass, "getuser", new = MagicMock(return_value='some_user'))
  @patch("tempfile.mkdtemp", new = MagicMock(return_value='/tmp/jdk_tmp_dir'))
  @patch("os.path.exists")
  def test_hook_default(self, os_path_exists_mock, os_path_isfile_mock):

    def side_effect(path):
      if path == "/etc/hadoop/conf":
        return True
      return False

    os_path_exists_mock.side_effect = side_effect
    os_path_isfile_mock.side_effect = [False, True, True, True, True]

    self.executeScript("before-ANY/scripts/hook.py",
                       classname="BeforeAnyHook",
                       command="hook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       config_file="default.json",
                       call_mocks=itertools.cycle([(0, "1000")])
    )
    self.assertResourceCalled('Group', 'hadoop',)
    self.assertResourceCalled('Group', 'nobody',)
    self.assertResourceCalled('Group', 'users',)

    self.assertResourceCalled('User', 'hive',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'oozie',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop',u'users'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'nobody',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop',u'nobody'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'ambari-qa',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop',u'users'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'flume',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'hdfs',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'storm',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'mapred',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'hbase',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'tez',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop',u'users'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'zookeeper',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'falcon',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop',u'users'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'sqoop',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'yarn',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'hcat',
                              gid = 'hadoop',
                              uid = None,
                              groups = [u'hadoop'],
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
                              content = StaticFile('changeToSecureUid.sh'),
                              mode = 0555,
                              )
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh ambari-qa /tmp/hadoop-ambari-qa,/tmp/hsperfdata_ambari-qa,/home/ambari-qa,/tmp/ambari-qa,/tmp/sqoop-ambari-qa 0',
                              not_if = '(test $(id -u ambari-qa) -gt 1000) || (false)',
                              )
    self.assertResourceCalled('Directory', '/tmp/hbase-hbase',
                              owner = 'hbase',
                              create_parents = True,
                              mode = 0775,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
                              content = StaticFile('changeToSecureUid.sh'),
                              mode = 0555,
                              )
    self.assertResourceCalled('File', '/tmp/changeUid.sh',
                              content = StaticFile('changeToSecureUid.sh'),
                              mode = 0555,
                              )
    self.assertResourceCalled('Execute', '/tmp/changeUid.sh hbase /home/hbase,/tmp/hbase,/usr/bin/hbase,/var/log/hbase,/tmp/hbase-hbase 1000',
                              not_if = '(test $(id -u hbase) -gt 1000) || (false)',
                              )
    self.assertResourceCalled('User', 'test_user1',
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('User', 'test_user2',
                              fetch_nonlocal_groups = True,
                              )
    self.assertResourceCalled('Group', 'hdfs',)
    self.assertResourceCalled('Group', 'test_group',)
    self.assertResourceCalled('User', 'hdfs',
                              fetch_nonlocal_groups = True,
                              groups = [u'hadoop', u'hdfs', u'test_group'],
                              )
    self.assertResourceCalled('Directory', '/etc/hadoop',
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-env']['content']),
                              owner = 'hdfs',
                              group = 'hadoop'
    )
    self.assertResourceCalled('Directory', '/tmp/hadoop_java_io_tmpdir',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 01777,
                              )
    self.assertResourceCalled('Directory', '/tmp/AMBARI-artifacts/',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/tmp/jdk-7u67-linux-x64.tar.gz',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/jdk-7u67-linux-x64.tar.gz'),
                              not_if = 'test -f /tmp/jdk-7u67-linux-x64.tar.gz',
                              )
    self.assertResourceCalled('File', '/tmp/jdk-7u67-linux-x64.tar.gz',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/jdk64',)
    self.assertResourceCalled('Execute', ('chmod', 'a+x', u'/usr/jdk64'),
                              sudo = True,
                              )
    self.assertResourceCalled('Execute', 'cd /tmp/jdk_tmp_dir && tar -xf /tmp/jdk-7u67-linux-x64.tar.gz && ambari-sudo.sh cp -rp /tmp/jdk_tmp_dir/* /usr/jdk64',)
    self.assertResourceCalled('Directory', '/tmp/jdk_tmp_dir',
                              action = ['delete'],
                              )
    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/bin/java',
                              mode = 0755,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('chmod', '-R', '755', u'/usr/jdk64/jdk1.7.0_45'),
                              sudo = True,
                              )
    self.assertNoMoreResources()
