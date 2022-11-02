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
from alerts.base_alert import BaseAlert
from AmbariConfig import AmbariConfig

class TestBaseAlert(TestCase):

  def setUp(self):
    self.config = AmbariConfig()

  def test_interval_noData(self):
    alert_meta = {}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    interval = alert.interval()
    self.assertEquals(interval, 1)

  def test_interval_zero(self):
    alert_meta = {'interval': 0}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    interval = alert.interval()
    self.assertEquals(interval, 1)

  def test_interval(self):
    alert_meta = {'interval': 5}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    interval = alert.interval()
    self.assertEquals(interval, 5)

  def test_isEnabled(self):
    alert_meta = {'enabled': 'true'}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    enabled = alert.is_enabled()
    self.assertEquals(enabled, 'true')

  def test_getName(self):
    alert_meta = {'name': 'ambari'}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    name = alert.get_name()
    self.assertEquals(name, 'ambari')

  def test_getUuid(self):
    alert_meta = {'uuid': '123'}
    alert_source_meta = {}

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    uuid = alert.get_uuid()
    self.assertEquals(uuid, '123')

  def test_setCluster(self):
    alert_meta = {}
    alert_source_meta = {}
    cluster = 'cluster'
    host = 'host'
    cluster_id = '0'

    alert = BaseAlert(alert_meta, alert_source_meta, self.config)
    alert.set_cluster(cluster, cluster_id, host)
    self.assertEquals(alert.cluster_name, cluster)
    self.assertEquals(alert.host_name, host)
