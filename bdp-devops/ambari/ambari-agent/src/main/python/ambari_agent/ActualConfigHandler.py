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

import ambari_simplejson as json
import logging
import os

logger = logging.getLogger()

class ActualConfigHandler:
  CONFIG_NAME = 'config.json'

  def __init__(self, config, configTags):
    self.config = config
    self.configTags = configTags

  def findRunDir(self):
    runDir = '/var/run/ambari-agent'
    if self.config.has_option('agent', 'prefix'):
      runDir = self.config.get('agent', 'prefix')
    if not os.path.exists(runDir):
      runDir = '/tmp'
    return runDir

  def write_actual(self, tags):
    self.write_file(self.CONFIG_NAME, tags)

  def write_actual_component(self, component, tags):
    self.configTags[component] = tags
    filename = component + "_" + self.CONFIG_NAME
    self.write_file(filename, tags)

  def write_client_components(self, serviceName, tags, components):
    from LiveStatus import LiveStatus
    for comp in LiveStatus.CLIENT_COMPONENTS:
      if comp['serviceName'] == serviceName:
        componentName = comp['componentName']
        if componentName in self.configTags and \
            tags != self.configTags[componentName] and \
            (components == ["*"] or componentName in components):
          self.write_actual_component(componentName, tags)
    pass

  def write_file(self, filename, tags):
    runDir = self.findRunDir()
    conf_file = open(os.path.join(runDir, filename), 'w')
    json.dump(tags, conf_file)
    conf_file.close()

  def read_file(self, filename):
    runDir = self.findRunDir()
    fullname = os.path.join(runDir, filename)
    if os.path.isfile(fullname):
      res = None
      conf_file = open(os.path.join(runDir, filename), 'r')
      try:
        res = json.load(conf_file)
        if (0 == len(res)):
          res = None
      except Exception, e:
        logger.error("Error parsing " + filename + ": " + repr(e))
        res = None
        pass
      conf_file.close()

      return res
    return None

  def read_actual(self):
    return self.read_file(self.CONFIG_NAME)

  def read_actual_component(self, componentName):
    if componentName not in self.configTags.keys():
      self.configTags[componentName] = \
        self.read_file(componentName + "_" + self.CONFIG_NAME)
    return self.configTags[componentName]
  
  def update_component_tag(self, componentName, tag, version):
    self.read_actual_component(componentName)
    self.configTags[componentName][tag] = version
    
    filename = componentName + "_" + self.CONFIG_NAME
    self.write_file(filename, self.configTags[componentName])
    
