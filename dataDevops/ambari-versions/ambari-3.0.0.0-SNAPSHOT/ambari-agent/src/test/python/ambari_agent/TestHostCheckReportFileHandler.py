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
from mock.mock import patch
import os
import tempfile
from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler
import logging
import ConfigParser

class TestHostCheckReportFileHandler(TestCase):

  logger = logging.getLogger()

  def test_write_host_check_report_really_empty(self):
    tmpfile = tempfile.mktemp()

    config = ConfigParser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)
    mydict = {}
    handler.writeHostCheckFile(mydict)

    configValidator = ConfigParser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    if configValidator.has_section('users'):
      users = configValidator.get('users', 'usr_list')
      self.assertEquals(users, '')


  @patch("os.path.exists")
  @patch("os.listdir")
  def test_write_host_check_report_empty(self, list_mock, exists_mock):
    tmpfile = tempfile.mktemp()
    exists_mock.return_value = False
    list_mock.return_value = []

    config = ConfigParser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)
    mydict = {}
    mydict['hostHealth'] = {}
    mydict['existingUsers'] = []
    mydict['alternatives'] = []
    mydict['stackFoldersAndFiles'] = []
    mydict['hostHealth']['activeJavaProcs'] = []
    mydict['installedPackages'] = []
    mydict['existingRepos'] = []

    handler.writeHostCheckFile(mydict)

    configValidator = ConfigParser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    users = configValidator.get('users', 'usr_list')
    users = configValidator.get('users', 'usr_homedir_list')
    self.assertEquals(users, '')
    names = configValidator.get('alternatives', 'symlink_list')
    targets = configValidator.get('alternatives', 'target_list')
    self.assertEquals(names, '')
    self.assertEquals(targets, '')

    paths = configValidator.get('directories', 'dir_list')
    self.assertEquals(paths, '')

    procs = configValidator.get('processes', 'proc_list')
    self.assertEquals(procs, '')

    time = configValidator.get('metadata', 'created')
    self.assertTrue(time != None)

  @patch("os.path.exists")
  @patch("os.listdir")
  def test_write_host_check_report(self, list_mock, exists_mock):
    tmpfile = tempfile.mktemp()
    exists_mock.return_value = False
    list_mock.return_value = []

    config = ConfigParser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)

    mydict = {}
    mydict['hostHealth'] = {}
    mydict['existingUsers'] = [{'name':'user1', 'homeDir':'/var/log', 'status':'Exists'}]
    mydict['alternatives'] = [
      {'name':'/etc/alternatives/hadoop-conf', 'target':'/etc/hadoop/conf.dist'},
      {'name':'/etc/alternatives/hbase-conf', 'target':'/etc/hbase/conf.1'}
    ]
    mydict['stackFoldersAndFiles'] = [{'name':'/a/b', 'type':'directory'},{'name':'/a/b.txt', 'type':'file'}]
    mydict['hostHealth']['activeJavaProcs'] = [
      {'pid':355,'hadoop':True,'command':'some command','user':'root'},
      {'pid':455,'hadoop':True,'command':'some command','user':'hdfs'}
    ]
    handler.writeHostCheckFile(mydict)

    configValidator = ConfigParser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)
    users = configValidator.get('users', 'usr_list')
    homedirs = configValidator.get('users', 'usr_homedir_list')
    self.assertEquals(users, 'user1')
    self.assertEquals(homedirs, '/var/log')

    names = configValidator.get('alternatives', 'symlink_list')
    targets = configValidator.get('alternatives', 'target_list')
    self.chkItemsEqual(names, ['/etc/alternatives/hadoop-conf', '/etc/alternatives/hbase-conf'])
    self.chkItemsEqual(targets, ['/etc/hadoop/conf.dist','/etc/hbase/conf.1'])

    paths = configValidator.get('directories', 'dir_list')
    self.chkItemsEqual(paths, ['/a/b','/a/b.txt'])

    procs = configValidator.get('processes', 'proc_list')
    self.chkItemsEqual(procs, ['455', '355'])


    mydict['installed_packages'] = [
      {'name':'hadoop','version':'3.2.3','repoName':'HDP'},
      {'name':'hadoop-lib','version':'3.2.3','repoName':'HDP'}
    ]
    mydict['existing_repos'] = ['HDP', 'HDP-epel']
    
    handler.writeHostChecksCustomActionsFile(mydict)
    configValidator = ConfigParser.RawConfigParser()
    configPath_ca = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_CUSTOM_ACTIONS_FILE)
    configValidator.read(configPath_ca)
    
    pkgs = configValidator.get('packages', 'pkg_list')
    self.chkItemsEqual(pkgs, ['hadoop', 'hadoop-lib'])

    repos = configValidator.get('repositories', 'repo_list')
    self.chkItemsEqual(repos, ['HDP', 'HDP-epel'])

    time = configValidator.get('metadata', 'created')
    self.assertTrue(time != None)

  @patch("os.path.exists")
  @patch("os.listdir")
  def test_write_host_stack_list(self, list_mock, exists_mock):
    exists_mock.return_value = True
    list_mock.return_value = ["1.1.1.1-1234", "current", "test"]

    tmpfile = tempfile.mktemp()

    config = ConfigParser.RawConfigParser()
    config.add_section('agent')
    config.set('agent', 'prefix', os.path.dirname(tmpfile))

    handler = HostCheckReportFileHandler(config)

    mydict = {}
    mydict['hostHealth'] = {}
    mydict['stackFoldersAndFiles'] = [{'name':'/a/b', 'type':'directory'},{'name':'/a/b.txt', 'type':'file'}]

    handler.writeHostCheckFile(mydict)

    configValidator = ConfigParser.RawConfigParser()
    configPath = os.path.join(os.path.dirname(tmpfile), HostCheckReportFileHandler.HOST_CHECK_FILE)
    configValidator.read(configPath)

    paths = configValidator.get('directories', 'dir_list')
    self.chkItemsEqual(paths, ['/a/b', '/a/b.txt', '/usr/hdp/1.1.1.1-1234', '/usr/hdp/current'])

  def chkItemsEqual(self, commaDelimited, items):
    items1 = commaDelimited.split(',')
    items1.sort()
    items.sort()
    items1Str = ','.join(items1)
    items2Str = ','.join(items)
    self.assertEquals(items2Str, items1Str)

if __name__ == "__main__":
  unittest.main(verbosity=2)
