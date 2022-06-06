#!/usr/bin/env ambari-python-wrap
"""
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
"""
import os
from ambari_commons import inet_utils
import imp
import traceback

def error(message): return {"level": "ERROR", "message": message}

class Uri:
  @classmethod
  def default_fs(self, configs):
    return self.from_config(configs, 'core-site', 'fs.defaultFS')

  @classmethod
  def http_namenode(self, configs):
    return self.from_config(configs, 'hdfs-site', 'dfs.namenode.http-address')

  @classmethod
  def https_namenode(self, configs):
    return self.from_config(configs, 'hdfs-site', 'dfs.namenode.https-address')

  @classmethod
  def onefs(self, configs):
    return self.from_config(configs, 'onefs', 'onefs_host')

  @staticmethod
  def from_config(configs, config_type, property_name):
    return Uri(configs['configurations'][config_type]['properties'][property_name])

  def __init__(self, address):
    self.address = address

  def has_host(self, uri):
    return uri.hostname() == self.hostname()

  def hostname(self):
    return inet_utils.get_host_from_url(self.address)

  def fix_host(self, uri):
    if not uri.hostname() or not self.hostname():
      return self.address
    return self.address.replace(self.hostname(), uri.hostname())

  def __str__(self):
    return self.address

class CoreSite:
  def __init__(self, configs):
    self.configs = configs

  def validate(self):
    invalid_configs = []
    onefs_host = Uri.onefs(self.configs)
    if not Uri.default_fs(self.configs).has_host(onefs_host):
      invalid_configs.append({
        'config-name': 'fs.defaultFS',
        'item': error('Hostname should match OneFS host: {0}'.format(onefs_host))
      })
    return invalid_configs

class HdfsSite:
  def __init__(self, configs):
    self.configs = configs

  def validate(self):
    invalid_configs = []
    onefs_host = Uri.onefs(self.configs)
    if not Uri.http_namenode(self.configs).has_host(onefs_host):
      invalid_configs.append({
        'config-name': 'dfs.namenode.http-address',
        'item': error('Hostname should match OneFS host: {0}'.format(onefs_host))
      })
    if not Uri.https_namenode(self.configs).has_host(onefs_host):
      invalid_configs.append({
        'config-name': 'dfs.namenode.https-address',
        'item': error('Hostname should match OneFS host: {0}'.format(onefs_host))
      })
    return invalid_configs

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, '../../../../../stacks/')
PARENT_FILE = os.path.join(STACKS_DIR, 'service_advisor.py')

try:
  with open(PARENT_FILE, 'rb') as fp:
    service_advisor = imp.load_module('service_advisor', fp, PARENT_FILE, ('.py', 'rb', imp.PY_SOURCE))
except Exception as e:
  traceback.print_exc()
  print "Failed to load parent"
else:
  class ONEFSServiceAdvisor(service_advisor.ServiceAdvisor):
    def getServiceConfigurationRecommendations(self, configs, clusterData, services, hosts):
      try:
        self.recommendHadoopProxyUsers(configs, services, hosts)
        putCoreSiteProperty = self.putProperty(configs, "core-site", services)
        putHdfsSiteProperty = self.putProperty(configs, "hdfs-site", services)
        onefs_host = Uri.onefs(services)
        putCoreSiteProperty("fs.defaultFS", Uri.default_fs(services).fix_host(onefs_host))
        putHdfsSiteProperty("dfs.namenode.http-address", Uri.http_namenode(services).fix_host(onefs_host))
        putHdfsSiteProperty("dfs.namenode.https-address", Uri.https_namenode(services).fix_host(onefs_host))
        # self.updateYarnConfig(configs, services) TODO doesn't work possibly due to a UI bug (Couldn't retrieve 'capacity-scheduler' from services)
      except KeyError as e:
        self.logger.info('Cannot get OneFS properties from config. KeyError: %s' % e)

    def updateYarnConfig(self, configs, services):
      if not 'YARN' in self.installedServices(services): return
      capacity_scheduler_dict, received_as_key_value_pair = self.getCapacitySchedulerProperties(services)
      if capacity_scheduler_dict:
        putCapSchedProperty = self.putProperty(configs, 'capacity-scheduler', services)
        if received_as_key_value_pair:
          capacity_scheduler_dict['yarn.scheduler.capacity.node-locality-delay'] = '0'
          putCapSchedProperty('capacity-scheduler', self.concatenated(capacity_scheduler_dict))
        else:
          putCapSchedProperty('yarn.scheduler.capacity.node-locality-delay', '0')

    def concatenated(self, capacity_scheduler_dict):
      return ''.join('%s=%s\n' % (k,v) for k,v in capacity_scheduler_dict.items())

    def installedServices(self, services):
      return [service['StackServices']['service_name'] for service in services['services']]

    def getServiceConfigurationsValidationItems(self, configs, recommendedDefaults, services, hosts):
      validation_errors = []
      try:
        validation_errors.extend(self.toConfigurationValidationProblems(CoreSite(services).validate(), 'core-site'))
        validation_errors.extend(self.toConfigurationValidationProblems(HdfsSite(services).validate(), 'hdfs-site'))
      except KeyError as e:
        self.logger.info('Cannot get OneFS properties from config. KeyError: %s' % e)
      return validation_errors