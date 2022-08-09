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
import tempfile
from unittest import TestCase
import os
import logging
from mock.mock import patch, MagicMock, call
from ambari_agent.LiveStatus import LiveStatus
from ambari_commons import OSCheck
from only_for_platform import os_distro_value

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.ActualConfigHandler import ActualConfigHandler


class TestActualConfigHandler(TestCase):

  def setUp(self):
    LiveStatus.SERVICES = [
      "HDFS", "MAPREDUCE", "GANGLIA", "HBASE",
      "ZOOKEEPER", "OOZIE",
      "KERBEROS", "TEMPLETON", "HIVE",
      "YARN", "MAPREDUCE2", "FLUME", "TEZ",
      "FALCON", "STORM"
    ]
    LiveStatus.CLIENT_COMPONENTS = [
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_CLIENT"},
      {"serviceName" : "HDFS",
       "componentName" : "HDFS_CLIENT"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "MAPREDUCE_CLIENT"},
      {"serviceName" : "ZOOKEEPER",
       "componentName" : "ZOOKEEPER_CLIENT"},
      {"serviceName" : "OOZIE",
       "componentName" : "OOZIE_CLIENT"},
      {"serviceName" : "HCATALOG",
       "componentName" : "HCAT"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_CLIENT"},
      {"serviceName" : "YARN",
       "componentName" : "YARN_CLIENT"},
      {"serviceName" : "MAPREDUCE2",
       "componentName" : "MAPREDUCE2_CLIENT"},
      {"serviceName" : "PIG",
       "componentName" : "PIG"},
      {"serviceName" : "SQOOP",
       "componentName" : "SQOOP"},
      {"serviceName" : "TEZ",
       "componentName" : "TEZ_CLIENT"},
      {"serviceName" : "FALCON",
       "componentName" : "FALCON_CLIENT"}
    ]
    LiveStatus.COMPONENTS = [
      {"serviceName" : "HDFS",
       "componentName" : "DATANODE"},
      {"serviceName" : "HDFS",
       "componentName" : "NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "SECONDARY_NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "JOURNALNODE"},
      {"serviceName" : "HDFS",
       "componentName" : "ZKFC"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "JOBTRACKER"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "TASKTRACKER"},
      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_SERVER"},
      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_MONITOR"},
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_MASTER"},
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_REGIONSERVER"},
      {"serviceName" : "FLUME",
       "componentName" : "FLUME_SERVER"},
      {"serviceName" : "ZOOKEEPER",
       "componentName" : "ZOOKEEPER_SERVER"},
      {"serviceName" : "OOZIE",
       "componentName" : "OOZIE_SERVER"},
      {"serviceName" : "HCATALOG",
       "componentName" : "HCATALOG_SERVER"},
      {"serviceName" : "KERBEROS",
       "componentName" : "KERBEROS_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_METASTORE"},
      {"serviceName" : "HIVE",
       "componentName" : "MYSQL_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "WEBHCAT_SERVER"},
      {"serviceName" : "YARN",
       "componentName" : "RESOURCEMANAGER"},
      {"serviceName" : "YARN",
       "componentName" : "NODEMANAGER"},
      {"serviceName" : "YARN",
       "componentName" : "APP_TIMELINE_SERVER"},
      {"serviceName" : "MAPREDUCE2",
       "componentName" : "HISTORYSERVER"},
      {"serviceName" : "FALCON",
       "componentName" : "FALCON_SERVER"},
      {"serviceName" : "STORM",
       "componentName" : "NIMBUS"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_REST_API"},
      {"serviceName" : "STORM",
       "componentName" : "SUPERVISOR"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_UI_SERVER"},
      {"serviceName" : "STORM",
       "componentName" : "DRPC_SERVER"}
    ]

  logger = logging.getLogger()

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_read_write(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags = { "global": "version1", "core-site": "version2" }
    handler = ActualConfigHandler(config, tags)
    handler.write_actual(tags)
    output = handler.read_actual()
    self.assertEquals(tags, output)
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_read_empty(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    handler = ActualConfigHandler(config, {})

    conf_file = open(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME), 'w')
    conf_file.write("")
    conf_file.close()
    
    output = handler.read_actual()
    self.assertEquals(None, output)
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_read_write_component(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    handler = ActualConfigHandler(config, {})
    handler.write_actual(tags1)
    handler.write_actual_component('FOO', tags1)

    output1 = handler.read_actual_component('FOO')
    output2 = handler.read_actual_component('GOO')

    self.assertEquals(tags1, output1)
    self.assertEquals(None, output2)
    
    tags2 = { "global": "version1", "core-site": "version2" }
    handler.write_actual(tags2)

    output3 = handler.read_actual()
    output4 = handler.read_actual_component('FOO')
    self.assertEquals(tags2, output3)
    self.assertEquals(tags1, output4)
    os.remove(os.path.join(tmpdir, "FOO_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  def test_write_actual_component_and_client_components(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    tags2 = { "global": "version33", "core-site": "version33" }
    clientsToUpdateConfigs1 = ["*"]
    handler = ActualConfigHandler(config, {})
    handler.write_actual_component('HDFS_CLIENT', tags1)
    handler.write_actual_component('HBASE_CLIENT', tags1)
    self.assertEquals(tags1, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    handler.write_actual_component('DATANODE', tags2)
    self.assertEquals(tags2, handler.read_actual_component('DATANODE'))
    self.assertEquals(tags1, handler.read_actual_component('HDFS_CLIENT'))
    handler.write_client_components('HDFS', tags2, clientsToUpdateConfigs1)
    self.assertEquals(tags2, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))

    os.remove(os.path.join(tmpdir, "DATANODE_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, "HBASE_CLIENT_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, "HDFS_CLIENT_" + ActualConfigHandler.CONFIG_NAME))

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_file")
  def test_write_client_components(self, write_file_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags0 = {"global": "version0", "core-site": "version0"}
    tags1 = {"global": "version1", "core-site": "version2"}
    tags2 = {"global": "version33", "core-site": "version33"}
    clientsToUpdateConfigs1 = ["HDFS_CLIENT","HBASE_CLIENT"]
    configTags = {'HDFS_CLIENT': tags0, 'HBASE_CLIENT': tags1}
    handler = ActualConfigHandler(config, configTags)
    self.assertEquals(tags0, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    handler.write_client_components('HDFS', tags2, clientsToUpdateConfigs1)
    self.assertEquals(tags2, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    self.assertTrue(write_file_mock.called)
    self.assertEqual(1, write_file_mock.call_count)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_file")
  def test_write_empty_client_components(self, write_file_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags0 = {"global": "version0", "core-site": "version0"}
    tags1 = {"global": "version1", "core-site": "version2"}
    tags2 = {"global": "version33", "core-site": "version33"}
    clientsToUpdateConfigs1 = []
    configTags = {'HDFS_CLIENT': tags0, 'HBASE_CLIENT': tags1}
    handler = ActualConfigHandler(config, configTags)
    self.assertEquals(tags0, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    handler.write_client_components('HDFS', tags2, clientsToUpdateConfigs1)
    self.assertEquals(tags0, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    self.assertFalse(write_file_mock.called)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler, "write_file")
  @patch.object(ActualConfigHandler, "read_file")
  def test_read_actual_component_inmemory(self, read_file_mock, write_file_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    read_file_mock.return_value = tags1

    handler = ActualConfigHandler(config, {})

    handler.write_actual_component('NAMENODE', tags1)
    self.assertTrue(write_file_mock.called)
    self.assertEquals(tags1, handler.read_actual_component('NAMENODE'))
    self.assertFalse(read_file_mock.called)
    self.assertEquals(tags1, handler.read_actual_component('DATANODE'))
    self.assertTrue(read_file_mock.called)
    self.assertEquals(1, read_file_mock.call_count)
    self.assertEquals(tags1, handler.read_actual_component('DATANODE'))
    self.assertEquals(1, read_file_mock.call_count)
