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
from alerts.metric_alert import MetricAlert
from mock.mock import Mock, MagicMock, patch
from AmbariConfig import AmbariConfig

class TestMetricAlert(TestCase):

  def setUp(self):
    self.config = AmbariConfig()

  @patch("urllib2.urlopen")
  def test_collect(self, urllib):
    alert_meta = {
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'jmx': {
        'property_list': [
          'x/y'
        ]
      },
      'uri': {
        'http': '192.168.0.10:8080',
        'https_property': '{{hdfs-site/dfs.http.policy}}',
        'https_property_value': 'HTTPS_ONLY'
      },
      "reporting": {
        "ok": {
          "text": "OK: {0}"
        },
        "warning": {
          "text": "Warn: {0}",
          "value": 2
        },
        "critical": {
          "text": "Crit: {0}",
          "value": 5
        }
      }
    }
    cluster = 'c1'
    host = 'host1'
    expected_text = 'OK: 1'

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['label'], alert_meta['label'])
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['service'], alert_meta['serviceName'])
      self.assertEquals(data['component'], alert_meta['componentName'])
      self.assertEquals(data['uuid'], alert_meta['uuid'])
      self.assertEquals(data['enabled'], alert_meta['enabled'])
      self.assertEquals(data['cluster'], cluster)
      self.assertEquals(clus, cluster)

    response = Mock()
    urllib.return_value = response
    response.read = Mock(return_value='{"beans": [{"y": 1}]}')
    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = MetricAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(mock_collector, {'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, host)

    alert.collect()

  @patch("urllib2.urlopen")
  def test_collect(self, urllib):
    alert_meta = {
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'jmx': {
        'property_list': [
          'x/y'
        ]
      },
      'uri': {
        'http': '192.168.0.10:8080',
        'https_property': '{{hdfs-site/dfs.http.policy}}',
        'https_property_value': 'HTTPS_ONLY'
      },
      "reporting": {
        "ok": {
          "text": "OK: {0}"
        },
        "warning": {
          "text": "Warn: {0}",
          "value": 2
        },
        "critical": {
          "text": "Crit: {0}",
          "value": 5
        }
      }
    }
    cluster = 'c1'
    host = 'host1'
    expected_text = 'Warn: 4'

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['label'], alert_meta['label'])
      self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['service'], alert_meta['serviceName'])
      self.assertEquals(data['component'], alert_meta['componentName'])
      self.assertEquals(data['uuid'], alert_meta['uuid'])
      self.assertEquals(data['enabled'], alert_meta['enabled'])
      self.assertEquals(data['cluster'], cluster)
      self.assertEquals(clus, cluster)

    response = Mock()
    urllib.return_value = response
    response.read = Mock(return_value='{"beans": [{"y": 4}]}')
    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = MetricAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(mock_collector, {'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, host)

    alert.collect()

  @patch("urllib2.urlopen")
  def test_collect(self, urllib):
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
      'jmx': {
        'property_list': [
          'x/y'
        ]
      },
      'uri': {
        'http': '192.168.0.10:8080',
        'https_property': '{{hdfs-site/dfs.http.policy}}',
        'https_property_value': 'HTTPS_ONLY'
      },
      "reporting": {
        "ok": {
          "text": "OK: {0}"
        },
        "warning": {
          "text": "Warn: {0}",
          "value": 2
        },
        "critical": {
          "text": "Crit: {0}",
          "value": 5
        }
      }
    }
    cluster = 'c1'
    cluster_id = '0'
    host = 'host1'
    expected_text = 'Crit: 12'

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['clusterId'], cluster_id)
      self.assertEquals(clus, cluster)

    response = Mock()
    urllib.return_value = response
    response.read = Mock(return_value='{"beans": [{"y": 12}]}')
    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = MetricAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(mock_collector, MagicMock(), MagicMock())#{'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, cluster_id, host)

    alert.collect()
