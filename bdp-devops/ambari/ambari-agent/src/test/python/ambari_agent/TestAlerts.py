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

import os
import socket
import sys
import urllib2
import tempfile
import random
from alerts.ams_alert import AmsAlert

from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.RecoveryManager import RecoveryManager
from ambari_agent.alerts.collector import AlertCollector
from ambari_agent.alerts.base_alert import BaseAlert
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.script_alert import ScriptAlert
from ambari_agent.alerts.web_alert import WebAlert
from ambari_agent.alerts.recovery_alert import RecoveryAlert
from ambari_agent.apscheduler.scheduler import Scheduler
from ambari_agent.ClusterConfigurationCache import ClusterConfigurationCache
from ambari_commons.urllib_handlers import RefreshHeaderProcessor

from collections import namedtuple
from mock.mock import MagicMock, patch
from unittest import TestCase

from AmbariConfig import AmbariConfig
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ConfigurationBuilder import ConfigurationBuilder

class TestAlerts(TestCase):

  def setUp(self):
    # save original open() method for later use
    self.original_open = open
    self.original_osfdopen = os.fdopen
    self.config = AmbariConfig()

  def tearDown(self):
    sys.stdout == sys.__stdout__


  @patch.object(Scheduler, "add_interval_job")
  @patch.object(Scheduler, "start")
  def test_start(self, aps_add_interval_job_mock, aps_start_mock):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_extensions_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()


    initializer_module = InitializerModule()
    
    initializer_module.config.cluster_cache_dir = test_file_path
    initializer_module.config.stacks_dir = test_stack_path
    initializer_module.config.common_services_dir = test_common_services_path
    initializer_module.config.extensions_dir = test_extensions_path
    initializer_module.config.host_scripts_dir = test_host_scripts_path
    
    initializer_module.init()
    
    ash = AlertSchedulerHandler(initializer_module)
    
    #ash = AlertSchedulerHandler(test_file_path, test_stack_path,
    #  test_common_services_path, test_extensions_path, test_host_scripts_path, cluster_configuration,
    #  self.config, None)

    ash.start()

    self.assertTrue(aps_add_interval_job_mock.called)
    self.assertTrue(aps_start_mock.called)

  @patch.object(RecoveryManager, "is_action_info_stale")
  @patch.object(RecoveryManager, "get_actions_copy")
  def test_recovery_alert(self, rm_get_actions_mock, is_stale_mock):
    definition_json = self._get_recovery_alert_definition()
    is_stale_mock.return_value = False
    rm_get_actions_mock.return_value = {
        "METRICS_COLLECTOR": {
          "count": 0,
          "lastAttempt": 1447860184,
          "warnedLastReset": False,
          "lastReset": 1447860184,
          "warnedThresholdReached": False,
          "lifetimeCount": 1,
          "warnedLastAttempt": False
        }
      }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, {})

    rm = RecoveryManager(MagicMock(), True)
    alert = RecoveryAlert(definition_json, definition_json['source'], self.config, rm)
    alert.set_helpers(collector, cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    self.assertEquals(1, alert.interval())

    #  OK - "count": 0
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])

    #  WARN - "count": 1
    rm_get_actions_mock.return_value = {
      "METRICS_COLLECTOR": {
        "count": 1,
        "lastAttempt": 1447860184,
        "warnedLastReset": False,
        "lastReset": 1447860184,
        "warnedThresholdReached": False,
        "lifetimeCount": 1,
        "warnedLastAttempt": False
      }
    }
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('WARNING', alerts[0]['state'])

    #  CRIT - "count": 5
    rm_get_actions_mock.return_value = {
      "METRICS_COLLECTOR": {
        "count": 5,
        "lastAttempt": 1447860184,
        "warnedLastReset": False,
        "lastReset": 1447860184,
        "warnedThresholdReached": False,
        "lifetimeCount": 1,
        "warnedLastAttempt": False
      }
    }
    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('CRITICAL', alerts[0]['state'])

    # OK again, after recovery manager window expired
    is_stale_mock.return_value = True

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])

    #  CRIT, after recovery manager window expired,
    # but max_lifetime_count reached, warnedThresholdReached == True
    rm_get_actions_mock.return_value = {
      "METRICS_COLLECTOR": {
        "count": 5,
        "lastAttempt": 1447860184,
        "warnedLastReset": False,
        "lastReset": 1447860184,
        "warnedThresholdReached": True,
        "lifetimeCount": 12,
        "warnedLastAttempt": False
      }
    }

    is_stale_mock.return_value = True

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('CRITICAL', alerts[0]['state'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(socket.socket,"connect")
  def test_port_alert_complex_uri(self, socket_connect_mock, get_configuration_mock):
    definition_json = self._get_port_alert_definition()

    configuration = {'hdfs-site' :
      { 'my-key': 'c6401.ambari.apache.org:2181,c6402.ambari.apache.org:2181,c6403.ambari.apache.org:2181'}
    }
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6402.ambari.apache.org")

    # use a URI that has commas to verify that we properly parse it
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertTrue('(Unit Tests)' in alerts[0]['text'])
    self.assertTrue('response time on port 2181' in alerts[0]['text'])


  def test_port_alert_no_sub(self):
    definition_json = { "name": "namenode_process", 
      "definitionId": 1,
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "http://c6401.ambari.apache.org",
        "default_port": 50070,
        "reporting": {
          "ok": {
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}"
          }
        }
      }
    }

    cluster_configuration = self.__get_cluster_configuration()

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(AlertCollector(), cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    self.assertEquals('http://c6401.ambari.apache.org', alert.uri)

    alert.collect()


  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_script_alert(self, get_configuration_mock):
    definition_json = self._get_script_alert_definition()

    # normally set by AlertSchedulerHandler
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['extensions_directory'] = os.path.join('ambari_agent', 'extensions')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    collector = AlertCollector()
    
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = ScriptAlert(definition_json, definition_json['source'], MagicMock())
    cluster_configuration = self.__get_cluster_configuration()
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    self.assertEquals(definition_json['source']['path'], alert.path)
    self.assertEquals(definition_json['source']['stacks_directory'], alert.stacks_dir)
    self.assertEquals(definition_json['source']['extensions_directory'], alert.extensions_dir)
    self.assertEquals(definition_json['source']['common_services_directory'], alert.common_services_dir)
    self.assertEquals(definition_json['source']['host_scripts_directory'], alert.host_scripts_dir)

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar, baz is rendered-baz', alerts[0]['text'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_script_alert_with_parameters(self, get_configuration_mock):
    definition_json = self._get_script_alert_definition_with_parameters()

    # normally set by AlertSchedulerHandler
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['extensions_directory'] = os.path.join('ambari_agent', 'extensions')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = ScriptAlert(definition_json, definition_json['source'], MagicMock())
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    self.assertEquals(definition_json['source']['path'], alert.path)
    self.assertEquals(definition_json['source']['stacks_directory'], alert.stacks_dir)
    self.assertEquals(definition_json['source']['common_services_directory'], alert.common_services_dir)
    self.assertEquals(definition_json['source']['extensions_directory'], alert.extensions_dir)
    self.assertEquals(definition_json['source']['host_scripts_directory'], alert.host_scripts_dir)

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('Script parameter detected: foo bar baz', alerts[0]['text'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(MetricAlert, "_load_jmx")
  def test_metric_alert(self, ma_load_jmx_mock, get_configuration_mock):
    definition_json = self._get_metric_alert_definition()
    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address': 'c6401.ambari.apache.org:80'}
    }
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    # trip an OK
    ma_load_jmx_mock.return_value = ([1, 25], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) OK: 1 25 125', alerts[0]['text'])

    # trip a warning
    ma_load_jmx_mock.return_value = ([1, 75], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('(Unit Tests) Warning: 1 75 175', alerts[0]['text'])

    # trip a critical now
    ma_load_jmx_mock.return_value = ([1, 150], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) Critical: 1 150 250', alerts[0]['text'])

    del definition_json['source']['jmx']['value']
    collector = AlertCollector()

    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    # now try without any jmx value to compare to
    ma_load_jmx_mock.return_value = ([1, 25], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) OK: 1 25 None', alerts[0]['text'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(AmsAlert, "_load_metric")
  def test_ams_alert(self, ma_load_metric_mock, get_configuration_mock):
    definition_json = self._get_ams_alert_definition()
    configuration = {'ams-site':
      {'timeline.metrics.service.webapp.address': '0.0.0.0:6188'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}


    alert = AmsAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    # trip an OK
    ma_load_metric_mock.return_value = ([{1:100,2:100,3:200,4:200}], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) OK: the mean used heap size is 150 MB.', alerts[0]['text'])

    # trip a warning
    ma_load_metric_mock.return_value = ([{1:800,2:800,3:900,4:900}], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('(Unit Tests) Warning: the mean used heap size is 850 MB.', alerts[0]['text'])

    # trip a critical now
    ma_load_metric_mock.return_value = ([{1:1000,2:1000,3:2000,4:2000}], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) Critical: the mean used heap size is 1500 MB.', alerts[0]['text'])

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(MetricAlert, "_load_jmx")
  def test_alert_uri_structure(self, ma_load_jmx_mock, get_configuration_mock):
    definition_json = self._get_metric_alert_definition()

    ma_load_jmx_mock.return_value = ([0,0], None)

    # run the alert without specifying any keys; an exception should be thrown
    # indicating that there was no URI and the result is UNKNOWN
    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])

    # set properties that make no sense wihtout the main URI properties
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('UNKNOWN', collector.alerts()[0]['state'])

    # set an actual property key (http)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.datanode.http.address' : 'c6401.ambari.apache.org:80' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('OK', collector.alerts()[0]['state'])

    # set an actual property key (https)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.datanode.https.address' : 'c6401.ambari.apache.org:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('OK', collector.alerts()[0]['state'])

    # set both (http and https)
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.datanode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.datanode.https.address' : 'c6401.ambari.apache.org:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)

    collector = AlertCollector()
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    self.assertEquals('OK', collector.alerts()[0]['state'])

  def create_initializer_module(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    #initializer_module.metadata_cache.rewrite_cluster_cache('-1',{'clusterLevelParams':{}})
    #initializer_module.metadata_cache.rewrite_cluster_cache('0',{'clusterLevelParams':{}})
    #initializer_module.configurations_cache.rewrite_cluster_cache('0', {})
    #initializer_module.configuration_builder.topology_cache = MagicMock()

    return initializer_module
  
  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(WebAlert, "_make_web_request")
  def test_web_alert(self, wa_make_web_request_mock, get_configuration_mock):
    definition_json = self._get_web_alert_definition()

    WebResponse = namedtuple('WebResponse', 'status_code time_millis error_msg')
    wa_make_web_request_mock.return_value = WebResponse(200,1.234,None)

    # run the alert and check HTTP 200
    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address' : 'c6401.ambari.apache.org:80' }
    }

    collector = AlertCollector()
    
    initializer_module = self.create_initializer_module()
    
    #cluster_configuration = self.__get_cluster_configuration()
    cluster_configuration = initializer_module.configurations_cache
    #self.__update_cluster_configuration(cluster_configuration, configuration)
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = WebAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('(Unit Tests) ok: 200', alerts[0]['text'])
    self.assertEquals('OK', alerts[0]['state'])

    # run the alert and check HTTP 500
    wa_make_web_request_mock.return_value = WebResponse(500,1.234,"Internal Server Error")
    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('(Unit Tests) warning: 500 (Internal Server Error)', alerts[0]['text'])

    # run the alert and check critical
    wa_make_web_request_mock.return_value = WebResponse(0,0,'error message')

    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    # http assertion indicating that we properly determined non-SSL
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) critical: http://c6401.ambari.apache.org:80. error message', alerts[0]['text'])

    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.datanode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.datanode.https.address' : 'c6401.ambari.apache.org:443/test/path' }
    }

    #self.__update_cluster_configuration(cluster_configuration, configuration)
    get_configuration_mock.return_value = {'configurations':configuration}
    
    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    # SSL assertion
    self.assertEquals('CRITICAL', alerts[0]['state'])
    self.assertEquals('(Unit Tests) critical: https://c6401.ambari.apache.org:443/test/path. error message', alerts[0]['text'])

    # test custom codes
    code = random.choice((600, 700, 800))
    wa_make_web_request_mock.return_value = WebResponse(code, 1.234 , "Custom Code")
    collector = AlertCollector()
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) ok: {code}'.format(code=code), alerts[0]['text'])

  def test_reschedule(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_extensions_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()

    initializer_module = InitializerModule()
    
    initializer_module.config.cluster_cache_dir = test_file_path
    initializer_module.config.stacks_dir = test_stack_path
    initializer_module.config.common_services_dir = test_common_services_path
    initializer_module.config.extensions_dir = test_extensions_path
    initializer_module.config.host_scripts_dir = test_host_scripts_path
    
    initializer_module.init()
    
    ash = AlertSchedulerHandler(initializer_module)
    
    #ash = AlertSchedulerHandler(test_file_path, test_stack_path,
    #  test_common_services_path, test_extensions_path, test_host_scripts_path, cluster_configuration,
    #  self.config, None)

    ash.start()

    self.assertEquals(1, ash.get_job_count())
    ash.reschedule()
    self.assertEquals(1, ash.get_job_count())

  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_alert_collector_purge(self, get_configuration_mock):
    definition_json = self._get_port_alert_definition()

    configuration = {'hdfs-site' :
      { 'my-key': 'value1' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    res = alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertTrue(alerts[0] is not None)
    self.assertEquals('CRITICAL', alerts[0]['state'])

    collector.remove_by_uuid('c1f73191-4481-4435-8dae-fd380e4c0be1')
    self.assertEquals(0,len(collector.alerts()))


  def test_disabled_definitions(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_extensions_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()

    initializer_module = InitializerModule()
    
    initializer_module.config.cluster_cache_dir = test_file_path
    initializer_module.config.stacks_dir = test_stack_path
    initializer_module.config.common_services_dir = test_common_services_path
    initializer_module.config.extensions_dir = test_extensions_path
    initializer_module.config.host_scripts_dir = test_host_scripts_path
    
    initializer_module.init()
    
    ash = AlertSchedulerHandler(initializer_module)
    """
      test_file_path, test_stack_path,
      test_common_services_path, test_extensions_path, test_host_scripts_path, cluster_configuration,
      self.config, None)
    """
    ash.start()
    
    """
       cachedir, stacks_dir, common_services_dir, extensions_dir,
      host_scripts_dir, cluster_configuration, config, recovery_manager,
      in_minutes=True):
    """

    self.assertEquals(1, ash.get_job_count())

    definition_json = self._get_port_alert_definition()

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    ash.schedule_definition(alert)

    self.assertEquals(2, ash.get_job_count())

    definition_json['enabled'] = False
    alert = PortAlert(definition_json, definition_json['source'], self.config)
    ash.schedule_definition(alert)

    # verify disabled alert not scheduled
    self.assertEquals(2, ash.get_job_count())

    definition_json['enabled'] = True
    pa = PortAlert(definition_json, definition_json['source'], self.config)
    ash.schedule_definition(pa)

    # verify enabled alert was scheduled
    self.assertEquals(3, ash.get_job_count())

  def test_immediate_alert(self):
    test_file_path = os.path.join('ambari_agent', 'dummy_files')
    test_stack_path = os.path.join('ambari_agent', 'dummy_files')
    test_common_services_path = os.path.join('ambari_agent', 'dummy_files')
    test_extensions_path = os.path.join('ambari_agent', 'dummy_files')
    test_host_scripts_path = os.path.join('ambari_agent', 'dummy_files')

    cluster_configuration = self.__get_cluster_configuration()
    
    initializer_module = InitializerModule()
    
    initializer_module.config.cluster_cache_dir = test_file_path
    initializer_module.config.stacks_dir = test_stack_path
    initializer_module.config.common_services_dir = test_common_services_path
    initializer_module.config.extensions_dir = test_extensions_path
    initializer_module.config.host_scripts_dir = test_host_scripts_path
    
    initializer_module.init()
    
    ash = AlertSchedulerHandler(initializer_module)
    
    #ash = AlertSchedulerHandler(test_file_path, test_stack_path,
    #  test_common_services_path, test_extensions_path, test_host_scripts_path, cluster_configuration,
    #  self.config, None)

    ash.start()

    self.assertEquals(1, ash.get_job_count())
    self.assertEquals(0, len(ash._collector.alerts()))

    execution_commands = [ {
      "clusterName": "c1",
      "hostName": "c6401.ambari.apache.org",
      "alertDefinition": self._get_port_alert_definition()
    } ]

    # execute the alert immediately and verify that the collector has the result
    ash.execute_alert(execution_commands)
    self.assertEquals(1, len(ash._collector.alerts()))


  def test_skipped_alert(self):
    definition_json = self._get_script_alert_definition()

    # normally set by AlertSchedulerHandler
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['extensions_directory'] = os.path.join('ambari_agent', 'extensions')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'skip': 'true' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = ScriptAlert(definition_json, definition_json['source'], self.config)

    # instruct the test alert script to be skipped
    alert.set_helpers(collector, cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    alert.collect()

    self.assertEquals(definition_json['source']['path'], alert.path)
    self.assertEquals(definition_json['source']['stacks_directory'], alert.stacks_dir)
    self.assertEquals(definition_json['source']['extensions_directory'], alert.extensions_dir)
    self.assertEquals(definition_json['source']['common_services_directory'], alert.common_services_dir)
    self.assertEquals(definition_json['source']['host_scripts_directory'], alert.host_scripts_dir)

    # ensure that the skipped alert was still placed into the collector; it's up to
    # the server to decide how to handle skipped alerts
    self.assertEquals(1,len(collector.alerts()))


  def test_default_reporting_text(self):
    definition_json = self._get_script_alert_definition()

    alert = ScriptAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), '{0}')

    definition_json['source']['type'] = 'PORT'
    alert = PortAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), 'TCP OK - {0:.4f} response on port {1}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), 'TCP OK - {0:.4f} response on port {1}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), 'Connection failed: {0} to {1}:{2}')

    definition_json['source']['type'] = 'WEB'
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), 'HTTP {0} response in {2:.4f} seconds')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), 'HTTP {0} response in {2:.4f} seconds')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), 'Connection failed to {1}')

    definition_json['source']['type'] = 'METRIC'
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), '{0}')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), '{0}')

    rm = RecoveryManager(MagicMock())
    definition_json['source']['type'] = 'RECOVERY'
    alert = RecoveryAlert(definition_json, definition_json['source'], self.config, rm)
    self.assertEquals(alert._get_reporting_text(alert.RESULT_OK), 'No recovery operations executed for {2}{0}.')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_WARNING), '{1} recovery operations executed for {2}{0}.')
    self.assertEquals(alert._get_reporting_text(alert.RESULT_CRITICAL), '{1} recovery operations executed for {2}{0}.')


  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_configuration_updates(self, get_configuration_mock):
    definition_json = self._get_script_alert_definition()

    # normally set by AlertSchedulerHandler
    definition_json['source']['stacks_directory'] = os.path.join('ambari_agent', 'dummy_files')
    definition_json['source']['common_services_directory'] = os.path.join('ambari_agent', 'common-services')
    definition_json['source']['extensions_directory'] = os.path.join('ambari_agent', 'extensions')
    definition_json['source']['host_scripts_directory'] = os.path.join('ambari_agent', 'host_scripts')

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    # populate the configuration cache with the initial configs
    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}
    
    # run the alert and verify the output
    alert = ScriptAlert(definition_json, definition_json['source'], MagicMock())
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar, baz is rendered-baz', alerts[0]['text'])

    # now update only the configs and run the same alert again and check
    # for different output
    configuration = {'foo-site' :
      { 'bar': 'rendered-bar2', 'baz' : 'rendered-baz2' }
    }
    get_configuration_mock.return_value = {'configurations':configuration}

    # populate the configuration cache with the initial configs
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('WARNING', alerts[0]['state'])
    self.assertEquals('bar is rendered-bar2, baz is rendered-baz2', alerts[0]['text'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_uri_structure_parsing(self, get_configuration_mock):
    uri_structure = {
      "http": "{{hdfs-site/dfs.namenode.http.address}}",
      "https": "{{hdfs-site/dfs.namenode.https.address}}",
      "https_property": "{{hdfs-site/dfs.http.policy}}",
      "https_property_value": "HTTPS_ONLY",
      "high_availability": {
        "nameservice": "{{hdfs-site/dfs.internal.nameservices}}",
        "alias_key" : "{{hdfs-site/dfs.ha.namenodes.{{ha-nameservice}}}}",
        "http_pattern" : "{{hdfs-site/dfs.namenode.http-address.{{ha-nameservice}}.{{alias}}}}",
        "https_pattern" : "{{hdfs-site/dfs.namenode.https-address.{{ha-nameservice}}.{{alias}}}}"
      }
    }

    configuration = {'hdfs-site' :
      { 'dfs.namenode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.namenode.https.address' : 'c6401.ambari.apache.org:443' }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = MockAlert()
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:80', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.namenode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.namenode.https.address' : 'c6401.ambari.apache.org:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    get_configuration_mock.return_value = {'configurations':configuration}
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:80', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    # switch to SSL
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.namenode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.namenode.https.address' : 'c6401.ambari.apache.org:443' }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertTrue(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))
    get_configuration_mock.return_value = {'configurations':configuration}

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:443', uri.uri )
    self.assertEqual( True, uri.is_ssl_enabled )

    # test HA
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTP_ONLY',
        'dfs.namenode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.namenode.https.address' : 'c6401.ambari.apache.org:443',
        'dfs.internal.nameservices' : 'c1ha',
        'dfs.ha.namenodes.c1ha' : 'nn1, nn2',
        'dfs.namenode.http-address.c1ha.nn1' : 'c6401.ambari.apache.org:8080',
        'dfs.namenode.http-address.c1ha.nn2' : 'c6402.ambari.apache.org:8080',
      }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    get_configuration_mock.return_value = {'configurations':configuration}
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertFalse(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:8080', uri.uri )
    self.assertEqual( False, uri.is_ssl_enabled )

    # test HA SSL
    configuration = {'hdfs-site' :
      { 'dfs.http.policy' : 'HTTPS_ONLY',
        'dfs.namenode.http.address' : 'c6401.ambari.apache.org:80',
        'dfs.namenode.https.address' : 'c6401.ambari.apache.org:443',
        'dfs.internal.nameservices' : 'c1ha',
        'dfs.ha.namenodes.c1ha' : 'nn1, nn2',
        'dfs.namenode.http-address.c1ha.nn1' : 'c6401.ambari.apache.org:8080',
        'dfs.namenode.http-address.c1ha.nn2' : 'c6402.ambari.apache.org:8080',
        'dfs.namenode.https-address.c1ha.nn1' : 'c6401.ambari.apache.org:8443',
        'dfs.namenode.https-address.c1ha.nn2' : 'c6402.ambari.apache.org:8443',
      }
    }

    self.__update_cluster_configuration(cluster_configuration, configuration)
    get_configuration_mock.return_value = {'configurations':configuration}
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertTrue(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6401.ambari.apache.org:8443', uri.uri )
    self.assertEqual( True, uri.is_ssl_enabled )


  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_uri_structure_parsing_without_namespace(self, get_configuration_mock):
    """
    Tests that we can parse an HA URI that only includes an alias and
    not a namespace
    :return:
    """
    uri_structure = {
      "http": "{{yarn-site/yarn.resourcemanager.webapp.address}}",
      "https": "{{yarn-site/yarn.resourcemanager.webapp.http.address}}",
      "https_property": "{{yarn-site/yarn.http.policy}}",
      "https_property_value": "HTTPS_ONLY",
      "high_availability": {
        "alias_key" : "{{yarn-site/yarn.resourcemanager.ha.rm-ids}}",
        "http_pattern" : "{{yarn-site/yarn.resourcemanager.webapp.address.{{alias}}}}",
        "https_pattern" : "{{yarn-site/yarn.resourcemanager.webapp.https.address.{{alias}}}}"
      }
    }

    configuration = { 'yarn-site' :
      { 'yarn.http.policy' : 'HTTPS_ONLY',
        'yarn.resourcemanager.webapp.address' : 'c6401.ambari.apache.org:80',
        'yarn.resourcemanager.webapp.http.address' : 'c6401.ambari.apache.org:443',
        'yarn.resourcemanager.webapp.address.rm1' : 'c6401.ambari.apache.org:8080',
        'yarn.resourcemanager.webapp.https.address.rm1' : 'c6401.ambari.apache.org:8443',
        'yarn.resourcemanager.webapp.address.rm2' : 'c6402.ambari.apache.org:8080',
        'yarn.resourcemanager.webapp.https.address.rm2' : 'c6402.ambari.apache.org:8443',
        'yarn.resourcemanager.ha.rm-ids' : 'rm1, rm2'
      }
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = MockAlert()
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6402.ambari.apache.org")
    uri_keys = alert._lookup_uri_property_keys(uri_structure)
    self.assertTrue(alert._check_uri_ssl_property(uri_keys, {'configurations':configuration}))

    uri = alert._get_uri_from_structure(uri_keys)
    self.assertEqual( 'c6402.ambari.apache.org:8443', uri.uri )
    self.assertEqual( True, uri.is_ssl_enabled )


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch('httplib.HTTPConnection')
  @patch.object(RefreshHeaderProcessor, 'http_response')
  def test_metric_alert_uses_refresh_processor(self, http_response_mock, http_connection_mock, get_configuration_mock):
    """
    Tests that the RefreshHeaderProcessor is correctly chained and called
    :param http_response_mock:
    :param http_connection_mock:
    :return:
    """
    http_conn = http_connection_mock.return_value
    http_conn.getresponse.return_value = MagicMock(status=200)
    http_response_mock.return_value = MagicMock(code=200)

    url_opener = urllib2.build_opener(RefreshHeaderProcessor())
    response = url_opener.open("http://foo.bar.baz/jmx")

    self.assertFalse(response is None)
    self.assertTrue(http_conn.request.called)
    self.assertTrue(http_conn.getresponse.called)
    self.assertTrue(http_response_mock.called)

    # now we know that the refresh header is intercepting, reset the mocks
    # and try with a METRIC alert
    MagicMock.reset_mock(http_response_mock)
    MagicMock.reset_mock(http_connection_mock)

    definition_json = self._get_metric_alert_definition()

    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address': 'c6401.ambari.apache.org:80'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    alert.collect()

    self.assertFalse(response is None)
    self.assertTrue(http_conn.request.called)
    self.assertTrue(http_conn.getresponse.called)
    self.assertTrue(http_response_mock.called)


  def test_urllib2_refresh_header_processor(self):
    from urllib2 import Request

    # setup the original request
    original_url = "http://foo.bar.baz/jmx?qry=someQuery"
    request = Request(original_url)

    # ensure that we get back a 200 with a refresh header to redirect us
    response = MagicMock(code=200)
    info_response = MagicMock()
    info_response.keys.return_value = ["Refresh"]
    info_response.getheader.return_value = "3; url=http://foobar.baz.qux:8080"

    response.info.return_value = info_response

    # add a mock parent to the refresh processor
    parent_mock = MagicMock()
    refresh_processor = RefreshHeaderProcessor()
    refresh_processor.parent = parent_mock

    # execute
    refresh_processor.http_response(request, response)

    # ensure that the parent was called with the modified URL
    parent_mock.open.assert_called_with("http://foobar.baz.qux:8080/jmx?qry=someQuery")

    # reset mocks
    MagicMock.reset_mock(parent_mock)

    # alter the refresh header to remove the time value
    info_response.getheader.return_value = "url=http://foobar.baz.qux:8443"

    # execute
    refresh_processor.http_response(request, response)

    # ensure that the parent was called with the modified URL
    parent_mock.open.assert_called_with("http://foobar.baz.qux:8443/jmx?qry=someQuery")

    # reset mocks
    MagicMock.reset_mock(parent_mock)

    # use an invalid refresh header
    info_response.getheader.return_value = "http://foobar.baz.qux:8443"

    # execute
    refresh_processor.http_response(request, response)

    # ensure that the parent was not called
    self.assertFalse(parent_mock.open.called)

    # reset mocks
    MagicMock.reset_mock(parent_mock)

    # remove the refresh header
    info_response.keys.return_value = ["SomeOtherHeaders"]

    # execute
    refresh_processor.http_response(request, response)

    # ensure that the parent was not called
    self.assertFalse(parent_mock.open.called)

    # reset mocks
    MagicMock.reset_mock(parent_mock)

    # use and invalid http code but include a refresh header
    response.code = 401
    info_response.keys.return_value = ["Refresh"]
    info_response.getheader.return_value = "3; url=http://foobar.baz.qux:8080"

    # execute
    refresh_processor.http_response(request, response)

    # ensure that the parent was not called
    self.assertFalse(parent_mock.open.called)


  def test_uri_timeout(self):
    # the web alert will have a timeout value
    definition_json = self._get_web_alert_definition()
    alert = WebAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(5.678, alert.connection_timeout)
    self.assertEquals(5, alert.curl_connection_timeout)

    # the metric definition will not and should default to 5.0
    definition_json = self._get_metric_alert_definition()
    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    self.assertEquals(5.0, alert.connection_timeout)

  @patch.object(ConfigurationBuilder, "get_configuration")
  def test_get_configuration_values(self, get_configuration_mock):
    """
    Tests that we are able to extract parameters correctly from the cached
    configuration.
    :return:
    """
    configuration = { 'foo-site' :
      { 'foo-key1' : 'value1',
        'foo-key2' : 'value2',
        'special-character-*' : 'asterisk',
        'special-character-$' : 'dollar sign',
        'special-character-%' : 'percent',
        'special-character-#' : 'hash',
        'special-character-!' : 'bang',
        'special-character-&' : 'ampersand'
      }
    }
    
    configuration_full = {'configurations':configuration}

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}
    
    alert = MockAlert()
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    self.assertEquals("constant", alert._get_configuration_value(configuration_full, "constant"))
    self.assertEquals("value1", alert._get_configuration_value(configuration_full, "{{foo-site/foo-key1}}"))
    self.assertEquals("value2", alert._get_configuration_value(configuration_full, "{{foo-site/foo-key2}}"))
    self.assertEquals("asterisk", alert._get_configuration_value(configuration_full, "{{foo-site/special-character-*}}"))
    self.assertEquals("dollar sign", alert._get_configuration_value(configuration_full, "{{foo-site/special-character-$}}"))
    self.assertEquals("hash", alert._get_configuration_value(configuration_full, "{{foo-site/special-character-#}}"))
    self.assertEquals("bang", alert._get_configuration_value(configuration_full, "{{foo-site/special-character-!}}"))
    self.assertEquals("ampersand", alert._get_configuration_value(configuration_full, "{{foo-site/special-character-&}}"))

    # try a mix of parameter and constant
    self.assertEquals("http://value1/servlet", alert._get_configuration_value(configuration_full, "http://{{foo-site/foo-key1}}/servlet"))
    self.assertEquals("http://value1/servlet/value2", alert._get_configuration_value(configuration_full, "http://{{foo-site/foo-key1}}/servlet/{{foo-site/foo-key2}}"))

    # try to request a dictionary object instead of a property
    self.assertEquals(configuration["foo-site"], alert._get_configuration_value(configuration_full, "{{foo-site}}"))

  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(MetricAlert, "_load_jmx")
  def test_metric_alert_floating_division(self, ma_load_jmx_mock, get_configuration_mock):
    definition_json = self._get_metric_alert_definition_with_float_division()
    configuration = {'hdfs-site' :
      { 'dfs.datanode.http.address': 'c6401.ambari.apache.org:80'}
    }
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}
    
    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)

    alert = MetricAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")

    # 10 / 5
    ma_load_jmx_mock.return_value = ([10, 5], None)

    alert.collect()
    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))
    self.assertEquals('OK', alerts[0]['state'])
    self.assertEquals('(Unit Tests) OK: 10 5 2.0', alerts[0]['text'])



  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(socket.socket,"connect")
  def test_alert_definition_value_error_conversion(self, socket_connect_mock, get_configuration_mock):
    """
    Tests that an alert definition with text that doesn't match the type of positional arguments
    can recover and retry the ValueError.
    :param socket_connect_mock:
    :return:
    """
    definition_json = self._get_alert_definition_with_value_error_text()

    configuration = {'hdfs-site' :
      { 'my-key': 'c6401.ambari.apache.org:2181'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6402.ambari.apache.org")

    # use a URI that has commas to verify that we properly parse it
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    # the collect should catch the invalid text in the definition
    # ValueError: Unknown format code 'd' for object of type 'float'
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('OK', alerts[0]['state'])
    self.assertTrue('(Unit Tests) TCP OK' in alerts[0]['text'])


  @patch.object(ConfigurationBuilder, "get_configuration")
  @patch.object(socket.socket,"connect")
  def test_alert_definition_too_many_positional_arguments(self, socket_connect_mock, get_configuration_mock):
    """
    Tests that an alert definition with too many arguments produces an alert to collect after the
    exceptioin is raised.
    :param socket_connect_mock:
    :return:
    """
    definition_json = self._get_alert_definition_with_too_many_positional_arguments()

    configuration = {'hdfs-site' :
      { 'my-key': 'c6401.ambari.apache.org:2181'}
    }

    collector = AlertCollector()
    cluster_configuration = self.__get_cluster_configuration()
    self.__update_cluster_configuration(cluster_configuration, configuration)
    
    initializer_module = self.create_initializer_module()
    get_configuration_mock.return_value = {'configurations':configuration}

    alert = PortAlert(definition_json, definition_json['source'], self.config)
    alert.set_helpers(collector, cluster_configuration, MagicMock())
    alert.set_cluster("c1", "0", "c6402.ambari.apache.org")

    # use a URI that has commas to verify that we properly parse it
    alert.set_helpers(collector, cluster_configuration, initializer_module.configuration_builder)
    alert.set_cluster("c1", "0", "c6401.ambari.apache.org")
    self.assertEquals(6, alert.interval())

    # the collect should catch the invalid text in the definition
    # ValueError: Unknown format code 'd' for object of type 'float'
    alert.collect()

    alerts = collector.alerts()
    self.assertEquals(0, len(collector.alerts()))

    self.assertEquals('UNKNOWN', alerts[0]['state'])
    self.assertTrue('There is a problem with the alert definition' in alerts[0]['text'])

  def __get_cluster_configuration(self):
    """
    Gets an instance of the cluster cache where the file read and write
    operations have been mocked out
    :return:
    """
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.open_side_effect
      cluster_configuration = ClusterConfigurationCache("/tmp/test_cache")
      return cluster_configuration


  @patch("os.open")
  @patch("os.fdopen")
  def __update_cluster_configuration(self, cluster_configuration, configuration, osfdopen_mock, osopen_mock):
    """
    Updates the configuration cache, using as mock file as the disk based
    cache so that a file is not created during tests
    :return:
    """
    osfdopen_mock.side_effect = self.osfdopen_side_effect
    cluster_configuration.rewrite_cluster_cache("0", {'configurations':configuration})


  def open_side_effect(self, file, mode):
    if mode == 'w':
      file_mock = MagicMock()
      return file_mock
    else:
      return self.original_open(file, mode)

  def osfdopen_side_effect(self, fd, mode):
    if mode == 'w':
      file_mock = MagicMock()
      return file_mock
    else:
      return self.original_open(file, mode)


  def _get_script_alert_definition(self):
    return {
      "definitionId": 1,
      "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "SCRIPT",
        "path": "test_script.py",
      }
    }

  def _get_script_alert_definition_with_parameters(self):
    return {
      "definitionId": 1,
      "name": "namenode_process",
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "SCRIPT",
        "path": "test_script.py",
        "parameters": [
          {
          "name": "script.parameter.foo",
          "value": "foo bar baz"
          }
        ]
      }
    }

  def _get_port_alert_definition(self):
    return { "name": "namenode_process",
      "definitionId": 1,
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "{{hdfs-site/my-key}}",
        "default_port": 50070,
        "reporting": {
          "ok": {
            "text": "(Unit Tests) TCP OK - {0:.4f} response time on port {1}"
          },
          "warning": {
            "text": "(Unit Tests) TCP WARN - {0:.4f} response time on port {1}",
            "value": 1.5
          },
          "critical": {
            "text": "(Unit Tests) Could not load process info: {0}",
            "value": 5.0
          }
        }
      }
    }


  def _get_recovery_alert_definition(self):
    return {
      "definitionId": 1,
      "componentName": "METRICS_COLLECTOR",
      "name": "ams_metrics_collector_autostart",
      "label": "Metrics Collector Recovery",
      "description": "This alert is triggered if the Metrics Collector has been auto-started for number of times equal to threshold.",
      "interval": 1,
      "scope": "HOST",
      "enabled": True,
      "source": {
        "type": "RECOVERY",
        "reporting": {
          "ok": {
            "text": "Metrics Collector has not been auto-started and is running normally{0}."
          },
          "warning": {
            "text": "Metrics Collector has been auto-started {1} times{0}.",
            "count": 1
          },
          "critical": {
            "text": "Metrics Collector has been auto-started {1} times{0}.",
            "count": 5
          }
        }
      }
    }

  def _get_metric_alert_definition(self):
    return {
      "definitionId": 1,
      "name": "DataNode CPU Check",
      "service": "HDFS",
      "component": "DATANODE",
      "label": "DataNode Process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "METRIC",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}",
          "https": "{{hdfs-site/dfs.datanode.https.address}}",
          "https_property": "{{hdfs-site/dfs.http.policy}}",
          "https_property_value": "HTTPS_ONLY"
        },
        "jmx": {
          "property_list": [
            "someJmxObject/value",
            "someOtherJmxObject/value"
          ],
          "value": "({0} * 100) + {1}"
        },
        "reporting": {
          "ok": {
            "text": "(Unit Tests) OK: {0} {1} {2}",
          },
          "warning": {
            "text": "(Unit Tests) Warning: {0} {1} {2}",
            "value": 150
          },
          "critical": {
            "text": "(Unit Tests) Critical: {0} {1} {2}",
            "value": 200
          }
        }
      }
    }

  def _get_ams_alert_definition(self):
    return {
      "definitionId": 1,
      "ignore_host": False,
      "name": "namenode_mean_heapsize_used",
      "componentName": "NAMENODE",
      "interval": 1,
      "clusterId": 2,
      "uuid": "8a857295-ad11-4985-896e-d866dc27b963",
      "label": "NameNode Mean Used Heap Size (Hourly)",
      "definitionId": 28,
      "source": {
        "ams": {
          "compute": "mean",
          "interval": 30,
          "app_id": "NAMENODE",
          "value": "{0}",
          "metric_list": [
            "jvm.JvmMetrics.MemHeapUsedM"
          ],
          "minimum_value": -1
        },
        "reporting": {
          "units": "#",
          "warning": {
            "text": "(Unit Tests) Warning: the mean used heap size is {0} MB.",
            "value": 768
          },
          "ok": {
            "text": "(Unit Tests) OK: the mean used heap size is {0} MB."
          },
          "critical": {
            "text": "(Unit Tests) Critical: the mean used heap size is {0} MB.",
            "value": 1024
          }
        },
        "type": "AMS",
        "uri": {
          "http": "{{ams-site/timeline.metrics.service.webapp.address}}",
          "https_property_value": "HTTPS_ONLY",
          "https_property": "{{ams-site/timeline.metrics.service.http.policy}}",
          "https": "{{ams-site/timeline.metrics.service.webapp.address}}",
          "connection_timeout": 5.0
        }
      },
    }

  def _get_metric_alert_definition_with_float_division(self):
    return {
      "definitionId": 1,
      "name": "DataNode CPU Check",
      "service": "HDFS",
      "component": "DATANODE",
      "label": "DataNode Process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "METRIC",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}",
          "https": "{{hdfs-site/dfs.datanode.https.address}}",
          "https_property": "{{hdfs-site/dfs.http.policy}}",
          "https_property_value": "HTTPS_ONLY"
        },
        "jmx": {
          "property_list": [
            "someJmxObject/value",
            "someOtherJmxObject/value"
          ],
          "value": "{0} / {1}"
        },
        "reporting": {
          "ok": {
            "text": "(Unit Tests) OK: {0} {1} {2}",
          },
          "warning": {
            "text": "(Unit Tests) Warning: {0} {1} {2}",
            "value": 150
          },
          "critical": {
            "text": "(Unit Tests) Critical: {0} {1} {2}",
            "value": 200
          }
        }
      }
    }

  def _get_web_alert_definition(self):
    return {
      "definitionId": 1,
      "name": "webalert_test",
      "service": "HDFS",
      "component": "DATANODE",
      "label": "WebAlert Test",
      "interval": 1,
      "scope": "HOST",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "WEB",
        "uri": {
          "http": "{{hdfs-site/dfs.datanode.http.address}}",
          "https": "{{hdfs-site/dfs.datanode.https.address}}",
          "https_property": "{{hdfs-site/dfs.http.policy}}",
          "https_property_value": "HTTPS_ONLY",
          "connection_timeout": 5.678,
          "acceptable_codes": [600, 700, 800]
        },
        "reporting": {
          "ok": {
            "text": "(Unit Tests) ok: {0}",
          },
          "warning": {
            "text": "(Unit Tests) warning: {0} ({3})",
          },
          "critical": {
            "text": "(Unit Tests) critical: {1}. {3}",
          }
        }
      }
    }

  def _get_alert_definition_with_value_error_text(self):
    return { "name": "namenode_process",
      "definitionId": 1,
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "{{hdfs-site/my-key}}",
        "default_port": 50070,
        "reporting": {
          "ok": {
            "text": "(Unit Tests) TCP OK {0:.4d}"
          },
          "warning": {
            "text": "(Unit Tests) TCP Warning {0:.4d}",
            "value": 1.5
          },
          "critical": {
            "text": "(Unit Tests) TCP Critical {0:.4d}",
            "value": 5.0
          }
        }
      }
    }

  def _get_alert_definition_with_too_many_positional_arguments(self):
    return { "name": "namenode_process",
      "definitionId": 1,
      "service": "HDFS",
      "component": "NAMENODE",
      "label": "NameNode process",
      "interval": 6,
      "scope": "host",
      "enabled": True,
      "uuid": "c1f73191-4481-4435-8dae-fd380e4c0be1",
      "source": {
        "type": "PORT",
        "uri": "{{hdfs-site/my-key}}",
        "default_port": 50070,
        "reporting": {
          "ok": {
            "text": "Bad Syntax Going To Mess You Up {0:.4d} {1} {2} {3} {4}"
          },
          "warning": {
            "text": "Bad Syntax Going To Mess You Up {0:.4d} {1} {2} {3} {4}",
            "value": 1.5
          },
          "critical": {
            "text": "Bad Syntax Going To Mess You Up {0:.4d} {1} {2} {3} {4}",
            "value": 5.0
          }
        }
      }
    }

class MockAlert(BaseAlert):
  """
  Mock class for testing
  """
  def __init__(self):
    super(MockAlert, self).__init__(None, None, AmbariConfig())

  def get_name(self):
    return "mock_alert"
