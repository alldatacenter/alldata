# !/usr/bin/env python

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

import platform
import datetime
import os
import errno
import tempfile
import sys
from unittest import TestCase
from mock.mock import patch
from mock.mock import MagicMock

from only_for_platform import os_distro_value, os_distro_value_linux

from ambari_commons import os_utils

from ambari_commons import OSCheck, OSConst
import os_check_type

import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

_search_file = os_utils.search_file
os_utils.search_file = MagicMock(return_value="/tmp/ambari.properties")
utils = __import__('ambari_server.utils').utils
# We have to use this import HACK because the filename contains a dash
with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
  with patch("os.access", return_value = MagicMock(return_value=True)):
    with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
      with patch("platform.linux_distribution", return_value = os_distro_value_linux):
        with patch.object(OSCheck, "os_distribution", return_value = os_distro_value):
          with patch.object(utils, "get_postgre_hba_dir"):
            os.environ["ROOT"] = ""
            ambari_server = __import__('ambari-server')
      
            from ambari_server.serverConfiguration import update_ambari_properties, configDefaults

@patch.object(platform, "linux_distribution", new = MagicMock(return_value=('Redhat', '6.4', 'Final')))
class TestOSCheck(TestCase):
  @patch.object(OSCheck, "os_distribution")
  @patch("ambari_commons.os_check._is_oracle_linux")
  def test_get_os_type(self, mock_is_oracle_linux, mock_linux_distribution):

    # 1 - Any system
    mock_is_oracle_linux.return_value = False
    mock_linux_distribution.return_value = ('my_os', '2015.09', '')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'my_os')

    # 2 - Negative case
    mock_linux_distribution.return_value = ('', 'aaaa', 'bbbbb')
    try:
      result = OSCheck.get_os_type()
      self.fail("Should throw exception in OSCheck.get_os_type()")
    except Exception as e:
      # Expected
      self.assertEquals("Cannot detect os type. Exiting...", str(e))
      pass

    # 3 - path exist: '/etc/oracle-release'
    mock_is_oracle_linux.return_value = True
    mock_linux_distribution.return_value = ('some_os', '1234', '')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'oraclelinux')

    # 4 - Common system
    mock_is_oracle_linux.return_value = False
    mock_linux_distribution.return_value = ('CenToS', '4.56', '')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'centos')

    # 5 - Red Hat Enterprise Linux
    mock_is_oracle_linux.return_value = False
    # Red Hat Enterprise Linux Server release 6.5 (Santiago)
    mock_linux_distribution.return_value = ('Red Hat Enterprise Linux Server', '6.5', 'Santiago')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'redhat')

    # Red Hat Enterprise Linux Workstation release 6.4 (Santiago)
    mock_linux_distribution.return_value = ('Red Hat Enterprise Linux Workstation', '6.4', 'Santiago')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'redhat')

    # Red Hat Enterprise Linux AS release 4 (Nahant Update 3)
    mock_linux_distribution.return_value = ('Red Hat Enterprise Linux AS', '4', 'Nahant Update 3')
    result = OSCheck.get_os_type()
    self.assertEquals(result, 'redhat')

  @patch.object(OSCheck, "os_distribution")
  @patch("os.path.exists")
  def test_get_os_family(self, mock_exists, mock_linux_distribution):

    # 1 - Any system
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ('MY_os', '5.6.7', '')
    result = OSCheck.get_os_family()
    self.assertEquals(result, 'my_os')

    # 2 - Redhat
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ('Centos Linux', '2.4', '')
    result = OSCheck.get_os_family()
    self.assertEquals(result, 'redhat')

    # 3 - Ubuntu
    mock_exists.return_value = False
    mock_linux_distribution.return_value = ('Ubuntu', '14.04', '')
    result = OSCheck.get_os_family()
    self.assertEquals(result, 'ubuntu')

    # 4 - Suse
    mock_exists.return_value = False
    mock_linux_distribution.return_value = (
    'suse linux enterprise server', '11.3', '')
    result = OSCheck.get_os_family()
    self.assertEquals(result, 'suse')

    mock_exists.return_value = False
    mock_linux_distribution.return_value = ('SLED', '1.2.3.4.5', '')
    result = OSCheck.get_os_family()
    self.assertEquals(result, 'suse')

    # 5 - Negative case
    mock_linux_distribution.return_value = ('', '111', '2222')
    try:
      result = OSCheck.get_os_family()
      self.fail("Should throw exception in OSCheck.get_os_family()")
    except Exception as e:
      # Expected
      self.assertEquals("Cannot detect os type. Exiting...", str(e))
      pass

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_version(self, mock_linux_distribution):

    # 1 - Any system
    mock_linux_distribution.return_value = ('some_os', '123.45', '')
    result = OSCheck.get_os_version()
    self.assertEquals(result, '123.45')

    # 2 - Negative case
    mock_linux_distribution.return_value = ('ssss', '', 'ddddd')
    try:
      result = OSCheck.get_os_version()
      self.fail("Should throw exception in OSCheck.get_os_version()")
    except Exception as e:
      # Expected
      self.assertEquals("Cannot detect os version. Exiting...", str(e))
      pass

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_major_version(self, mock_linux_distribution):

    # 1
    mock_linux_distribution.return_value = ('abcd_os', '123.45.67', '')
    result = OSCheck.get_os_major_version()
    self.assertEquals(result, '123')

    # 2
    mock_linux_distribution.return_value = ('Suse', '11', '')
    result = OSCheck.get_os_major_version()
    self.assertEquals(result, '11')
    
  @patch.object(OSCheck, "os_distribution")
  def test_aliases(self, mock_linux_distribution):
    OSConst.OS_TYPE_ALIASES['qwerty_os123'] = 'aliased_os5'
    OSConst.OS_FAMILY_COLLECTION.append({          
          'name': 'aliased_os_family',
          'os_list': ["aliased_os"]
    })
    
    mock_linux_distribution.return_value = ('qwerty_os', '123.45.67', '')
    
    self.assertEquals(OSCheck.get_os_type(), 'aliased_os')
    self.assertEquals(OSCheck.get_os_major_version(), '5')
    self.assertEquals(OSCheck.get_os_version(), '5.45.67')
    self.assertEquals(OSCheck.get_os_family(), 'aliased_os_family')

  @patch.object(OSCheck, "os_distribution")
  def test_get_os_release_name(self, mock_linux_distribution):

    # 1 - Any system
    mock_linux_distribution.return_value = ('', '', 'MY_NEW_RELEASE')
    result = OSCheck.get_os_release_name()
    self.assertEquals(result, 'my_new_release')

    # 2 - Negative case
    mock_linux_distribution.return_value = ('aaaa', 'bbbb', '')
    try:
      result = OSCheck.get_os_release_name()
      self.fail("Should throw exception in OSCheck.get_os_release_name()")
    except Exception as e:
      # Expected
      self.assertEquals("Cannot detect os release name. Exiting...", str(e))
      pass

  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def _test_update_ambari_properties_os(self, get_conf_dir_mock):
    from ambari_server import serverConfiguration   # need to modify constants inside the module

    properties = ["server.jdbc.user.name=ambari-server\n",
                  "server.jdbc.database_name=ambari\n",
                  "ambari-server.user=root\n",
                  "server.jdbc.user.name=ambari-server\n",
                  "jdk.name=jdk-6u31-linux-x64.bin\n",
                  "jce.name=jce_policy-6.zip\n",
                  "server.os_type=old_sys_os6\n",
                  "java.home=/usr/jdk64/jdk1.6.0_31\n"]

    serverConfiguration.OS_FAMILY = "family_of_trolls"
    serverConfiguration.OS_VERSION = "666"

    get_conf_dir_mock.return_value = '/etc/ambari-server/conf'

    (tf1, fn1) = tempfile.mkstemp()
    (tf2, fn2) = tempfile.mkstemp()
    configDefaults.AMBARI_PROPERTIES_BACKUP_FILE = fn1
    serverConfiguration.AMBARI_PROPERTIES_FILE = fn2

    f = open(configDefaults.AMBARI_PROPERTIES_BACKUP_FILE, 'w')
    try:
      for line in properties:
        f.write(line)
    finally:
      f.close()

    #Call tested method
    update_ambari_properties()

    f = open(serverConfiguration.AMBARI_PROPERTIES_FILE, 'r')
    try:
      ambari_properties_content = f.readlines()
    finally:
      f.close()

    count = 0
    for line in ambari_properties_content:
      if (not line.startswith('#')):
        count += 1
        if (line == "server.os_type=old_sys_os6\n"):
          self.fail("line=" + line)
        else:
          pass

    self.assertEquals(count, 9)
    # Command should not fail if *.rpmsave file is missing
    result = update_ambari_properties()
    self.assertEquals(result, 0)
    pass

  @patch.object(OSCheck, "os_distribution")
  def test_os_type_check(self, mock_linux_distribution):

    # 1 - server and agent os compatible
    mock_linux_distribution.return_value = ('aaa', '11', 'bb')
    base_args = ["os_check_type.py", "aaa11"]
    sys.argv = list(base_args)

    try:
      os_check_type.main()
    except SystemExit as e:
      # exit_code=0
      self.assertEquals("0", str(e))

    # 2 - server and agent os is not compatible
    mock_linux_distribution.return_value = ('ddd', '33', 'bb')
    base_args = ["os_check_type.py", "zzz_x77"]
    sys.argv = list(base_args)

    try:
      os_check_type.main()
      self.fail("Must fail because os's not compatible.")
    except Exception as e:
      self.assertEquals(
        "Local OS is not compatible with cluster primary OS family. Please perform manual bootstrap on this host.",
        str(e))
      pass

  @patch.object(OSCheck, "get_os_family")
  def is_ubuntu_family(self, get_os_family_mock):

    get_os_family_mock.return_value = "ubuntu"
    self.assertEqual(OSCheck.is_ubuntu_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_ubuntu_family(), False)

  @patch.object(OSCheck, "get_os_family")
  def test_is_suse_family(self, get_os_family_mock):

    get_os_family_mock.return_value = "suse"
    self.assertEqual(OSCheck.is_suse_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_suse_family(), False)

  @patch.object(OSCheck, "get_os_family")
  def test_is_redhat_family(self, get_os_family_mock):

    get_os_family_mock.return_value = "redhat"
    self.assertEqual(OSCheck.is_redhat_family(), True)

    get_os_family_mock.return_value = "troll_os"
    self.assertEqual(OSCheck.is_redhat_family(), False)
