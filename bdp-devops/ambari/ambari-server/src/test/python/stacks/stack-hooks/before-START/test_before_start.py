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

from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, call, patch
from resource_management import Hook
from resource_management.core.exceptions import Fail
import json

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("os.path.exists", new = MagicMock(return_value=True))
@patch.object(Hook, "run_custom_hook", new = MagicMock())
class TestHookBeforeStart(RMFTestCase):
  def test_hook_default(self):
    self.executeScript("before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', ('setenforce', '0'),
                              only_if = 'test -f /selinux/enforce',
                              not_if = "(! which getenforce ) || (which getenforce && getenforce | grep -q Disabled)",
                              sudo=True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
        owner = 'hdfs',
        cd_access = 'a',
    )                
    self.assertResourceCalled('Directory', '/tmp/hadoop-hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File',
                              '/etc/hadoop/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hdfs',
                              content=InlineTemplate('log4jproperties\nline2log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar',
        content = StaticFile('fast-hdfs-resource.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-metrics2.properties']['content']),
                              group='hadoop',
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_mappings.data',
                              owner = 'hdfs',
                              content = Template('topology_mappings.data.j2'),
                              mode = 0644,
                              group = 'hadoop',
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_script.py',
                              content = StaticFile('topology_script.py'),
                              mode = 0755,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertNoMoreResources()

  def test_hook_secured(self):
    self.executeScript("before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       command="hook",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute', ('setenforce', '0'),
                              only_if = 'test -f /selinux/enforce',
                              not_if = "(! which getenforce ) || (which getenforce && getenforce | grep -q Disabled)",
                              sudo=True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
        owner = 'hdfs',
        cd_access = 'a',
    )            
    self.assertResourceCalled('Directory', '/tmp/hadoop-hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File',
                              '/etc/hadoop/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hdfs',
                              content=InlineTemplate('log4jproperties\nline2log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar',
        content = StaticFile('fast-hdfs-resource.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-metrics2.properties']['content']),
                              group='hadoop',
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_mappings.data',
                              owner = 'hdfs',
                              content = Template('topology_mappings.data.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_script.py',
                              content = StaticFile('topology_script.py'),
                              mode = 0755,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertNoMoreResources()

  def test_hook_default_hdfs(self):
    config_file = self._getStackTestsFolder() + "/configs/default.json"
    with open(config_file, "r") as f:
      default_json = json.load(f)

    default_json['serviceName']= 'HDFS'
    self.executeScript("before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       command="hook",
                       config_dict=default_json
    )
    self.assertResourceCalled('Execute', ('setenforce', '0'),
                              only_if = 'test -f /selinux/enforce',
                              not_if = "(! which getenforce ) || (which getenforce && getenforce | grep -q Disabled)",
                              sudo=True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
        owner = 'hdfs',
        cd_access = 'a',
    )                
    self.assertResourceCalled('Directory', '/tmp/hadoop-hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              cd_access = 'a',
                              )             
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File',
                              '/etc/hadoop/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hdfs',
                              content=InlineTemplate('log4jproperties\nline2log4jproperties\nline2')
    )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar',
        content = StaticFile('fast-hdfs-resource.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-metrics2.properties']['content']),
                              group='hadoop',
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_mappings.data',
                              owner = 'hdfs',
                              content = Template('topology_mappings.data.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_script.py',
                              content = StaticFile('topology_script.py'),
                              mode = 0755,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertNoMoreResources()

  def test_hook_refresh_topology_custom_directories(self):
    config_file = "{0}/test/python/stacks/configs/default.json".format(self.get_src_folder())
    with open(config_file, "r") as f:
      default_json = json.load(f)

    default_json['serviceName'] = 'HDFS'
    default_json['configurations']['core-site']['net.topology.script.file.name'] = '/home/myhadoop/hadoop/conf.hadoop/topology_script.py'

    self.executeScript("before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       target=RMFTestCase.TARGET_STACK_HOOKS,
                       command="hook",
                       config_dict=default_json
    )
    self.assertResourceCalled('Execute', ('setenforce', '0'),
                              only_if = 'test -f /selinux/enforce',
                              not_if = "(! which getenforce ) || (which getenforce && getenforce | grep -q Disabled)",
                              sudo=True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
        owner = 'hdfs',
        cd_access = 'a',
    )                                 
    self.assertResourceCalled('Directory', '/tmp/hadoop-hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              cd_access = 'a',
                              )                       
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File',
                              '/etc/hadoop/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hdfs',
                              content=InlineTemplate('log4jproperties\nline2log4jproperties\nline2')
    )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar',
        content = StaticFile('fast-hdfs-resource.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = InlineTemplate(self.getConfig()['configurations']['hadoop-metrics2.properties']['content']),
                              group='hadoop',
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_mappings.data',
                              owner = 'hdfs',
                              content = Template('topology_mappings.data.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/topology_script.py',
                              content = StaticFile('topology_script.py'),
                              mode = 0755,
                              only_if = 'test -d /etc/hadoop/conf',
                              )
    self.assertNoMoreResources()

  def test_that_jce_is_required_in_secured_cluster(self):
    try:
      self.executeScript("before-START/scripts/hook.py",
                         classname="BeforeStartHook",
                         target=RMFTestCase.TARGET_STACK_HOOKS,
                         command="hook",
                         config_file="secured_no_jce_name.json"
      )
      self.fail("Should throw an exception")
    except Fail:
      pass  # Expected
