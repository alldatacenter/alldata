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
import logging
import unittest
import socket
import platform
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import create_autospec
import ambari_commons
from ambari_commons import OSCheck
import os
from only_for_platform import not_for_platform, os_distro_value, PLATFORM_WINDOWS
from ambari_commons.firewall import Firewall
from ambari_commons.os_check import OSCheck, OSConst
from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler
from ambari_agent.HostInfo import HostInfo, HostInfoLinux
from ambari_agent.Hardware import Hardware
from ambari_agent.AmbariConfig import AmbariConfig
from resource_management.core import shell
from resource_management.core.system import System


@not_for_platform(PLATFORM_WINDOWS)
@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestHostInfo:#(TestCase):

  @patch('os.path.exists')
  def test_checkFolders(self, path_mock):
    path_mock.return_value = True
    hostInfo = HostInfo()
    results = []
    existingUsers = [{'name':'a1', 'homeDir':os.path.join('home', 'a1')}, {'name':'b1', 'homeDir':os.path.join('home', 'b1')}]
    hostInfo.checkFolders([os.path.join("etc", "conf"), os.path.join("var", "lib"), "home"], ["a1", "b1"], ["c","d"], existingUsers, results)
    print results
    self.assertEqual(6, len(results))
    names = [i['name'] for i in results]
    for item in [os.path.join('etc','conf','a1'), os.path.join('var','lib','a1'), os.path.join('etc','conf','b1'), os.path.join('var','lib','b1')]:

      self.assertTrue(item in names)

  def test_checkUsers(self):
    hostInfo = HostInfoLinux()
    results = []
    hostInfo.checkUsers(["root", "zxlmnap12341234"], results)
    self.assertEqual(1, len(results))
    newlist = sorted(results, key=lambda k: k['name'])
    self.assertTrue(newlist[0]['name'], "root")
    self.assertTrue(newlist[0]['homeDir'], "/root")
    self.assertTrue(newlist[0]['status'], "Available")

  @patch.object(OSCheck, "get_os_type")
  @patch('os.umask')
  @patch.object(HostCheckReportFileHandler, 'writeHostCheckFile')
  @patch("resource_management.core.providers.get_provider")
  @patch.object(HostInfoLinux, 'checkUsers')
  @patch.object(HostInfoLinux, 'checkLiveServices')
  @patch.object(HostInfoLinux, 'javaProcs')
  @patch.object(HostInfoLinux, 'checkFolders')
  @patch.object(HostInfoLinux, 'etcAlternativesConf')
  @patch.object(HostInfoLinux, 'hadoopVarRunCount')
  @patch.object(HostInfoLinux, 'hadoopVarLogCount')
  @patch.object(HostInfoLinux, 'checkFirewall')
  @patch.object(HostInfoLinux, 'checkUnlimitedJce')
  def test_hostinfo_register_suse(self, jce_mock, cit_mock, hvlc_mock, hvrc_mock, eac_mock, cf_mock, jp_mock,
                             cls_mock, cu_mock, get_packages_mock, whcf_mock, os_umask_mock, get_os_type_mock):

    m = MagicMock()

    m.get_package_details.return_value = ["pkg1", "pkg2"]
    m.get_installed_pkgs_by_names.return_value = ["pkg2"]
    m.get_installed_pkgs_by_repo.return_value = ["pkg1"]

    get_packages_mock.return_value = m
    cit_mock.return_value = True
    hvlc_mock.return_value = 1
    hvrc_mock.return_value = 1
    get_os_type_mock.return_value = "suse"

    hostInfo = HostInfoLinux()
    dict = {}
    hostInfo.register(dict, False, False)
    self.assertTrue(cit_mock.called)
    self.assertTrue(os_umask_mock.called)
    self.assertTrue(whcf_mock.called)
    self.assertTrue(jce_mock.called)

    self.assertTrue('agentTimeStampAtReporting' in dict['hostHealth'])

  @patch.object(OSCheck, "get_os_type")
  @patch('os.umask')
  @patch.object(HostCheckReportFileHandler, 'writeHostCheckFile')
  @patch("resource_management.core.providers.get_provider")
  @patch.object(HostInfoLinux, 'checkUsers')
  @patch.object(HostInfoLinux, 'checkLiveServices')
  @patch.object(HostInfoLinux, 'javaProcs')
  @patch.object(HostInfoLinux, 'checkFolders')
  @patch.object(HostInfoLinux, 'etcAlternativesConf')
  @patch.object(HostInfoLinux, 'hadoopVarRunCount')
  @patch.object(HostInfoLinux, 'hadoopVarLogCount')
  @patch.object(HostInfoLinux, 'checkFirewall')
  @patch.object(HostInfoLinux, 'getTransparentHugePage')
  def test_hostinfo_register(self, get_transparentHuge_page_mock, cit_mock, hvlc_mock, hvrc_mock, eac_mock, cf_mock, jp_mock,
                             cls_mock, cu_mock, get_packages_mock, whcf_mock, os_umask_mock, get_os_type_mock):
    m = MagicMock()

    m.get_package_details.return_value = ["pkg1", "pkg2"]
    m.get_installed_pkgs_by_names.return_value = ["pkg2"]
    m.get_installed_pkgs_by_repo.return_value = ["pkg1"]

    get_packages_mock.return_value = m

    cit_mock.return_value = True
    hvlc_mock.return_value = 1
    hvrc_mock.return_value = 1
    get_os_type_mock.return_value = "redhat"

    hostInfo = HostInfoLinux()
    dict = {}
    hostInfo.register(dict, True, True)
    self.verifyReturnedValues(dict)

    hostInfo.register(dict, True, False)
    self.verifyReturnedValues(dict)

    hostInfo.register(dict, False, True)
    self.verifyReturnedValues(dict)
    self.assertTrue(os_umask_mock.call_count == 2)

    cit_mock.reset_mock()
    hostInfo = HostInfoLinux()
    dict = {}
    hostInfo.register(dict, False, False)
    self.assertTrue(cit_mock.called)
    self.assertEqual(1, cit_mock.call_count)

  def verifyReturnedValues(self, dict):
    hostInfo = HostInfoLinux()
    self.assertEqual(dict['alternatives'], [])
    self.assertEqual(dict['stackFoldersAndFiles'], [])
    self.assertEqual(dict['existingUsers'], [])
    self.assertTrue(dict['firewallRunning'])
    self.assertEqual(dict['firewallName'], "iptables")

  @patch("os.path.exists")
  @patch("os.path.islink")
  @patch("os.path.isdir")
  @patch("os.path.isfile")
  def test_dirType(self, os_path_isfile_mock, os_path_isdir_mock, os_path_islink_mock, os_path_exists_mock):
    host = HostInfoLinux()

    os_path_exists_mock.return_value = False
    result = host.dirType("/home")
    self.assertEquals(result, 'not_exist')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'sym_link')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'directory')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = False
    os_path_isfile_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'file')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = False
    os_path_isfile_mock.return_value = False
    result = host.dirType("/home")
    self.assertEquals(result, 'unknown')

  @patch("os.path.exists")
  @patch("glob.glob")
  def test_hadoopVarRunCount(self, glob_glob_mock, os_path_exists_mock):
    hostInfo = HostInfoLinux()

    os_path_exists_mock.return_value = True
    glob_glob_mock.return_value = ['pid1','pid2','pid3']
    result = hostInfo.hadoopVarRunCount()
    self.assertEquals(result, 3)

    os_path_exists_mock.return_value = False
    result = hostInfo.hadoopVarRunCount()
    self.assertEquals(result, 0)

  @patch("os.path.exists")
  @patch("glob.glob")
  def test_hadoopVarLogCount(self, glob_glob_mock, os_path_exists_mock):
    hostInfo = HostInfoLinux()

    os_path_exists_mock.return_value = True
    glob_glob_mock.return_value = ['log1','log2']
    result = hostInfo.hadoopVarLogCount()
    self.assertEquals(result, 2)

    os_path_exists_mock.return_value = False
    result = hostInfo.hadoopVarLogCount()
    self.assertEquals(result, 0)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = ('redhat','11','Final')))
  @patch("os.listdir", create=True, autospec=True)
  @patch("__builtin__.open", create=True, autospec=True)
  @patch("pwd.getpwuid", create=True, autospec=True)
  def test_javaProcs(self, pwd_getpwuid_mock, buitin_open_mock, os_listdir_mock):
    hostInfo = HostInfoLinux()
    openRead = MagicMock()
    openRead.read.return_value = '/java/;/hadoop/'
    buitin_open_mock.side_effect = [openRead, ['Uid: 22']]
    pwuid = MagicMock()
    pwd_getpwuid_mock.return_value = pwuid
    pwuid.pw_name = 'user'
    os_listdir_mock.return_value = ['1']
    list = []
    hostInfo.javaProcs(list)

    self.assertEquals(list[0]['command'], '/java/;/hadoop/')
    self.assertEquals(list[0]['pid'], 1)
    self.assertTrue(list[0]['hadoop'])
    self.assertEquals(list[0]['user'], 'user')

  @patch.object(OSCheck, "get_os_type")
  @patch("resource_management.core.shell.call")
  def test_checkLiveServices(self, shell_call, get_os_type_method):
    get_os_type_method.return_value = 'redhat'
    hostInfo = HostInfoLinux()

    shell_call.return_value = (0, '', 'err')
    result = []
    hostInfo.checkLiveServices([('service1',)], result)

    self.assertEquals(result[0]['desc'], '')
    self.assertEquals(result[0]['status'], 'Healthy')
    self.assertEquals(result[0]['name'], 'service1')

    shell_call.return_value = (1, 'out', 'err')
    result = []
    hostInfo.checkLiveServices([('service1',)], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], 'out')

    shell_call.return_value = (1, '', 'err')
    result = []
    hostInfo.checkLiveServices([('service1',)], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], 'err')

    shell_call.return_value = (1, '', 'err')
    result = []
    hostInfo.checkLiveServices([('service1',)], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertTrue(len(result[0]['desc']) > 0)

    shell_call.return_value = (0, '', 'err')
    result = []
    hostInfo.checkLiveServices([('service1', 'service2',)], result)

    self.assertEquals(result[0]['status'], 'Healthy')
    self.assertEquals(result[0]['name'], 'service1 or service2')
    self.assertEquals(result[0]['desc'], '')

    shell_call.return_value = (1, 'out', 'err')
    result = []
    hostInfo.checkLiveServices([('service1', 'service2',)], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1 or service2')
    self.assertEquals(result[0]['desc'], 'out{0}out'.format(os.linesep))

    msg = 'thrown by shell call'
    shell_call.side_effect = Exception(msg)
    result = []
    hostInfo.checkLiveServices([('service1',)], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], msg)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = ('redhat','11','Final')))
  @patch("os.path.exists")
  @patch("os.listdir", create=True, autospec=True)
  @patch("os.path.islink")
  @patch("os.path.realpath")
  def test_etcAlternativesConf(self, os_path_realpath_mock, os_path_islink_mock, os_listdir_mock, os_path_exists_mock):
    hostInfo = HostInfoLinux()
    os_path_exists_mock.return_value = False
    result = hostInfo.etcAlternativesConf('',[])

    self.assertEquals(result, [])

    os_path_exists_mock.return_value = True
    os_listdir_mock.return_value = ['config1']
    os_path_islink_mock.return_value = True
    os_path_realpath_mock.return_value = 'real_path_to_conf'
    result = []
    hostInfo.etcAlternativesConf('project', result)

    self.assertEquals(result[0]['name'], 'config1')
    self.assertEquals(result[0]['target'], 'real_path_to_conf')

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("resource_management.core.shell.call")
  def test_FirewallRunning(self, run_os_command_mock, get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 0, "Table: filter", ""
    self.assertTrue(Firewall().getFirewallObject().check_firewall())


  @patch.object(socket, "getfqdn")
  @patch.object(socket, "gethostbyname")
  @patch.object(socket, "gethostname")
  def test_checkReverseLookup(self, gethostname_mock, gethostbyname_mock, getfqdn_mock):
    gethostname_mock.return_value = "test"
    gethostbyname_mock.side_effect = ["123.123.123.123", "123.123.123.123"]
    getfqdn_mock.return_value = "test.example.com"

    hostInfo = HostInfoLinux()

    self.assertTrue(hostInfo.checkReverseLookup())
    gethostbyname_mock.assert_any_call("test.example.com")
    gethostbyname_mock.assert_any_call("test")
    self.assertEqual(2, gethostbyname_mock.call_count)

    gethostbyname_mock.side_effect = ["123.123.123.123", "231.231.231.231"]

    self.assertFalse(hostInfo.checkReverseLookup())

    gethostbyname_mock.side_effect = ["123.123.123.123", "123.123.123.123"]
    getfqdn_mock.side_effect = socket.error()

    self.assertFalse(hostInfo.checkReverseLookup())

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("resource_management.core.shell.call")
  def test_FirewallStopped(self, run_os_command_mock, get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 3, "", ""
    self.assertFalse(Firewall().getFirewallObject().check_firewall())

  @patch.object(OSCheck, "get_os_family")
  @patch("os.path.isfile")
  @patch('__builtin__.open')
  def test_transparent_huge_page(self, open_mock, os_path_isfile_mock, get_os_family_mock):
    context_manager_mock = MagicMock()
    open_mock.return_value = context_manager_mock
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    file_mock = MagicMock()
    file_mock.read.return_value = "[never] always"
    enter_mock = MagicMock()
    enter_mock.return_value = file_mock
    exit_mock  = MagicMock()
    setattr( context_manager_mock, '__enter__', enter_mock )
    setattr( context_manager_mock, '__exit__', exit_mock )

    hostInfo = HostInfoLinux()

    os_path_isfile_mock.return_value = True
    self.assertEqual("never", hostInfo.getTransparentHugePage())

    os_path_isfile_mock.return_value = False
    self.assertEqual("", hostInfo.getTransparentHugePage())

  @patch.object(OSCheck, "get_os_family")
  @patch("os.path.isfile")
  @patch('__builtin__.open')
  def test_transparent_huge_page_debian(self, open_mock, os_path_isfile_mock, get_os_family_mock):
    context_manager_mock = MagicMock()
    open_mock.return_value = context_manager_mock
    get_os_family_mock.return_value = OSConst.UBUNTU_FAMILY
    file_mock = MagicMock()
    file_mock.read.return_value = "[never] always"
    enter_mock = MagicMock()
    enter_mock.return_value = file_mock
    exit_mock  = MagicMock()
    setattr( context_manager_mock, '__enter__', enter_mock )
    setattr( context_manager_mock, '__exit__', exit_mock )

    hostInfo = HostInfoLinux()

    os_path_isfile_mock.return_value = True
    self.assertEqual("never", hostInfo.getTransparentHugePage())

    os_path_isfile_mock.return_value = False
    self.assertEqual("", hostInfo.getTransparentHugePage())


if __name__ == "__main__":
  unittest.main()
