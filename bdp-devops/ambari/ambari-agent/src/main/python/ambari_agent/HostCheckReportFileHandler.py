#!/usr/bin/env python

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

import datetime
import os.path
import logging
import re
import traceback
from AmbariConfig import AmbariConfig
import ConfigParser

HADOOP_ROOT_DIR = "/usr/hdp"
HADOOP_PERM_REMOVE_LIST = ["current"]
HADOOP_ITEMDIR_REGEXP = re.compile("(\d\.){3}\d-\d{4}")
logger = logging.getLogger(__name__)

class HostCheckReportFileHandler:

  HOST_CHECK_FILE = "hostcheck.result"
  HOST_CHECK_CUSTOM_ACTIONS_FILE = "hostcheck_custom_actions.result"

  def __init__(self, config=None):
    self.hostCheckFilePath = None
    
    if config is None:
      config = self.resolve_ambari_config()
      
    hostCheckFileDir = config.get('agent', 'prefix')
    self.hostCheckFilePath = os.path.join(hostCheckFileDir, self.HOST_CHECK_FILE)
    self.hostCheckCustomActionsFilePath = os.path.join(hostCheckFileDir, self.HOST_CHECK_CUSTOM_ACTIONS_FILE)
    
  def resolve_ambari_config(self):
    try:
      config = AmbariConfig()
      if os.path.exists(AmbariConfig.getConfigFile()):
        config.read(AmbariConfig.getConfigFile())
      else:
        raise Exception("No config found, use default")

    except Exception, err:
      logger.warn(err)
    return config
    
  def writeHostChecksCustomActionsFile(self, structuredOutput):
    if self.hostCheckCustomActionsFilePath is None:
      return
    
    try:
      logger.info("Host check custom action report at " + self.hostCheckCustomActionsFilePath)
      config = ConfigParser.RawConfigParser()
      config.add_section('metadata')
      config.set('metadata', 'created', str(datetime.datetime.now()))
      
      if 'installed_packages' in structuredOutput.keys():
        items = []
        for itemDetail in structuredOutput['installed_packages']:
          items.append(itemDetail['name'])
        config.add_section('packages')
        config.set('packages', 'pkg_list', ','.join(map(str, items)))

      if 'existing_repos' in structuredOutput.keys():
        config.add_section('repositories')
        config.set('repositories', 'repo_list', ','.join(structuredOutput['existing_repos']))
        
      self.removeFile(self.hostCheckCustomActionsFilePath)
      self.touchFile(self.hostCheckCustomActionsFilePath)
      with open(self.hostCheckCustomActionsFilePath, 'wb') as configfile:
        config.write(configfile)
    except Exception, err:
      logger.error("Can't write host check file at %s :%s " % (self.hostCheckCustomActionsFilePath, err.message))
      traceback.print_exc()

  def _stack_list_directory(self):
    """
    Return filtered list of <stack-root> directory allowed to be removed
    :rtype list
    """

    if not os.path.exists(HADOOP_ROOT_DIR):
      return []

    folder_content = os.listdir(HADOOP_ROOT_DIR)
    remove_list = []

    remlist_items_count = 0

    for item in folder_content:
      full_path = "%s%s%s" % (HADOOP_ROOT_DIR, os.path.sep, item)
      if item in HADOOP_PERM_REMOVE_LIST:
        remove_list.append(full_path)
        remlist_items_count += 1

      if HADOOP_ITEMDIR_REGEXP.match(item) is not None:
        remove_list.append(full_path)
        remlist_items_count += 1

    # if remove list have same length as target folder, assume that they are identical
    if remlist_items_count == len(folder_content):
      remove_list.append(HADOOP_ROOT_DIR)

    return remove_list

  def writeHostCheckFile(self, hostInfo):
    if self.hostCheckFilePath is None:
      return

    try:
      logger.debug("Host check report at " + self.hostCheckFilePath)
      config = ConfigParser.RawConfigParser()
      config.add_section('metadata')
      config.set('metadata', 'created', str(datetime.datetime.now()))

      if 'existingUsers' in hostInfo.keys():
        items = []
        items2 = []
        for itemDetail in hostInfo['existingUsers']:
          items.append(itemDetail['name'])
          items2.append(itemDetail['homeDir'])
        config.add_section('users')
        config.set('users', 'usr_list', ','.join(items))
        config.set('users', 'usr_homedir_list', ','.join(items2))

      if 'alternatives' in hostInfo.keys():
        items = []
        items2 = []
        for itemDetail in hostInfo['alternatives']:
          items.append(itemDetail['name'])
          items2.append(itemDetail['target'])
        config.add_section('alternatives')
        config.set('alternatives', 'symlink_list', ','.join(items))
        config.set('alternatives', 'target_list', ','.join(items2))

      if 'stackFoldersAndFiles' in hostInfo.keys():
        items = []
        for itemDetail in hostInfo['stackFoldersAndFiles']:
          items.append(itemDetail['name'])
        items += self._stack_list_directory()
        config.add_section('directories')
        config.set('directories', 'dir_list', ','.join(items))

      if 'hostHealth' in hostInfo.keys():
        if 'activeJavaProcs' in hostInfo['hostHealth'].keys():
          items = []
          for itemDetail in hostInfo['hostHealth']['activeJavaProcs']:
            items.append(itemDetail['pid'])
          config.add_section('processes')
          config.set('processes', 'proc_list', ','.join(map(str, items)))

      self.removeFile(self.hostCheckFilePath)
      self.touchFile(self.hostCheckFilePath)
      with open(self.hostCheckFilePath, 'wb') as configfile:
        config.write(configfile)
    except Exception, err:
      logger.error("Can't write host check file at %s :%s " % (self.hostCheckFilePath, err.message))
      traceback.print_exc()

  def removeFile(self, path):
    if os.path.isfile(path):
      logger.debug("Removing old host check file at %s" % path)
      os.remove(path)

  def touchFile(self, path):
    if not os.path.isfile(path):
      logger.debug("Creating host check file at %s" % path)
      open(path, 'w').close()


