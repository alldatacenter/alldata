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

from ambari_agent.alerts.collector import AlertCollector

from mock.mock import patch
from unittest import TestCase

class TestAlertCollector(TestCase):

  def test_put_noCluster(self):
    cluster = 'TestCluster'
    alert = {
      'name': 'AlertName',
      'uuid': '12'
    }
    collector = AlertCollector()
    collector._AlertCollector__buckets = {
      'TestCluster2': {}
    }
    collector.put(cluster, alert)

    self.assertEquals(collector._AlertCollector__buckets, {'TestCluster': {'AlertName': alert}, 'TestCluster2': {}})

  def test_put_clusterExists(self):
    cluster = 'TestCluster'
    alert = {
      'name': 'AlertName',
      'uuid': '12'
    }
    collector = AlertCollector()
    collector._AlertCollector__buckets = {
      'TestCluster': {}
    }
    collector.put(cluster, alert)

    self.assertEquals(collector._AlertCollector__buckets, {'TestCluster': {'AlertName': alert}})

  def test_put_alertExists(self):
    cluster = 'TestCluster'
    alert = {
      'name': 'AlertName',
      'uuid': '12'
    }
    collector = AlertCollector()
    collector._AlertCollector__buckets = {
      'TestCluster': {
        'AlertName': {
          'smth': 'some_value'
        }
      }
    }
    collector.put(cluster, alert)

    self.assertEquals(collector._AlertCollector__buckets, {'TestCluster': {'AlertName': alert}})

  def test_remove(self):
    alert1 = {
      'name': 'AlertName1',
      'uuid': 11
    }
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster': {
        'AlertName1': alert1,
        'AlertName2': alert2
      }
    }
    controller.remove('TestCluster', 'AlertName1')

    self.assertEquals(controller._AlertCollector__buckets, {'TestCluster': {'AlertName2': alert2}})

  def test_remove_noCluster(self):
    alert1 = {
      'name': 'AlertName1',
      'uuid': 11
    }
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster2': {
        'AlertName1': alert1,
        'AlertName2': alert2
      }
    }
    controller.remove('TestCluster', 'AlertName1')

    self.assertEquals(controller._AlertCollector__buckets, {'TestCluster2': {'AlertName1': alert1, 'AlertName2': alert2}})

  def test_remove_noAlert(self):
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster2': {
        'AlertName2': alert2
      }
    }
    controller.remove('TestCluster', 'AlertName1')

    self.assertEquals(controller._AlertCollector__buckets, {'TestCluster2': {'AlertName2': alert2}})

  def test_remove_by_uuid(self):
    alert1 = {
      'name': 'AlertName1',
      'uuid': '11'
    }
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster2': {
        'AlertName1': alert1,
        'AlertName2': alert2
      }
    }
    controller.remove_by_uuid('11')

    self.assertEquals(controller._AlertCollector__buckets, {'TestCluster2': {'AlertName2': alert2}})

  def test_remove_by_uuid_absent(self):
    alert1 = {
      'name': 'AlertName1',
      'uuid': '11'
    }
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster': {
        'AlertName1': alert1,
        'AlertName2': alert2
      }
    }
    controller.remove_by_uuid('13')

    self.assertEquals(controller._AlertCollector__buckets, {'TestCluster': {'AlertName1': alert1, 'AlertName2': alert2}})

  def test_alerts(self):
    alert1 = {
      'name': 'AlertName1',
      'uuid': '11'
    }
    alert2 = {
      'name': 'AlertName2',
      'uuid': '12'
    }
    alert3 = {
      'name': 'AlertName3',
      'uuid': '13'
    }
    alert4 = {
      'name': 'AlertName4',
      'uuid': '14'
    }
    controller = AlertCollector()
    controller._AlertCollector__buckets = {
      'TestCluster1': {
        'AlertName1': alert1,
        'AlertName2': alert2
      },
      'TestCluster2': {
        'AlertName3': alert3,
        'AlertName4': alert4
      }
    }
    list = controller.alerts()

    self.assertEquals(controller._AlertCollector__buckets, {})
    self.assertEquals(list.sort(), [alert1, alert2, alert3, alert4].sort())





