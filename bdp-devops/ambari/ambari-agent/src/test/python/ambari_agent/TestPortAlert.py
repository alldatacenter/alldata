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
from alerts.port_alert import PortAlert
from mock.mock import Mock, MagicMock, patch
from AmbariConfig import AmbariConfig

class TestPortAlert(TestCase):

  def setUp(self):
    self.config = AmbariConfig()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_defaultPort(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1',
      'default_port': 80
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'OK'
    expected_text = 'TCP OK - 0.2010 response on port 80'
    time.side_effect = [123, 324, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_warning(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'WARNING'
    expected_text = 'TCP OK - 3.1170 response on port 8080'
    time.side_effect = [123, 3240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    
    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_connectionTimeout(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: Socket Timeout to 192.168.0.1:8080'
    time.side_effect = [123, 5240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()

    def collector_side_effect(clus, data):
      print data
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_noUrl(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'default_port': 80
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: Socket Timeout to host1:80'
    time.side_effect = [123, 5240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_exception(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: exception message to 192.168.0.1:8080'
    time.side_effect = [123, 345, 567]
    socket.side_effect = Exception('exception message')
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_warningTimeoutChanged(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80,
      'reporting': {
        'warning': {
          'value': 4
        }
      }
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'OK'
    expected_text = 'TCP OK - 3.1170 response on port 8080'
    time.side_effect = [123, 3240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    
    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_criticalTimeoutChanged(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80,
      'reporting': {
        'critical': {
          'value': 3
        }
      }
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: Socket Timeout to 192.168.0.1:8080'
    time.side_effect = [123, 3240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    
    
    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_criticalTimeoutTooBig(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:8080',
      'default_port': 80,
      'reporting': {
        'critical': {
          'value': 33
        }
      }
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: Socket Timeout to 192.168.0.1:8080'
    time.side_effect = [120, 123, 5240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    
    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_zookeeper(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'zookeeper_server_process',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:2181',
      'default_port': 80,
      'parameters': [
        {
          'name': 'socket.command',
          'display_name': 'Socket Command',
          'value': 'ruok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        },
        {
          'name': 'socket.command.response',
          'display_name': 'Expected Response',
          'value': 'imok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        }
      ]
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'OK'
    expected_text = 'TCP OK - 0.2010 response on port 2181'
    time.side_effect = [123, 324, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    s = socket()
    s.recv.return_value = "imok"

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_zookeeper_warning(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'zookeeper_server_process',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true',
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:2181',
      'default_port': 2181,
      'parameters': [
        {
          'name': 'socket.command',
          'display_name': 'Socket Command',
          'value': 'ruok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        },
        {
          'name': 'socket.command.response',
          'display_name': 'Expected Response',
          'value': 'imok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        }
      ]
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'WARNING'
    expected_text = 'TCP OK - 3.1170 response on port 2181'
    time.side_effect = [123, 3240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    s = socket()
    s.recv.return_value = "imok"

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()

  @patch("socket.socket")
  @patch("time.time")
  def test_collect_zookeeper_connectionTimeout(self, time, socket):
    alert_meta = {
      'definitionId': 1,
      'name': 'zookeeper_server_process',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'uri': 'http://192.168.0.1:2181',
      'default_port': 80,
      'parameters': [
        {
          'name': 'socket.command',
          'display_name': 'Socket Command',
          'value': 'ruok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        },
        {
          'name': 'socket.command.response',
          'display_name': 'Expected Response',
          'value': 'imok',
          'description': 'test',
          'type': 'STRING',
          'visibility': 'HIDDEN'
        }
      ]
    }
    cluster = 'c1'
    host = 'host1'
    cluster_id = '0'
    expected_state = 'CRITICAL'
    expected_text = 'Connection failed: Socket Timeout to 192.168.0.1:2181'
    time.side_effect = [123, 5240, 567]
    alert = PortAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    alert.configuration_builder = MagicMock()
    
    s = socket()
    s.recv.return_value = "imok"

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['state'], expected_state)
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    alert.collector = MagicMock()
    alert.collector.put = Mock(side_effect=collector_side_effect)

    alert.collect()
