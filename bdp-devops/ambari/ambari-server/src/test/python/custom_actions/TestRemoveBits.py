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
import json
import os
from ambari_commons import subprocess32
import select
import install_packages

from mock.mock import patch
from mock.mock import MagicMock
from stacks.utils.RMFTestCase import *
from mock.mock import patch, MagicMock
from resource_management.core.base import Resource
from resource_management.core.exceptions import Fail

from only_for_platform import get_platform, not_for_platform, only_for_platform, os_distro_value, PLATFORM_WINDOWS

class TestRemoveBits(RMFTestCase):

  def test_remove_hdp_21(self):
    self.executeScript("scripts/remove_bits.py",
                       classname="RemoveBits",
                       command="remove_hdp_21",
                       config_file="install_packages_config.json",
                       target=RMFTestCase.TARGET_CUSTOM_ACTIONS,
                       os_type=('Redhat', '6.4', 'Final'),
                       )
    self.assertResourceCalled('Package', 'phoenix',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hbase',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'flume',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'storm',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'tezfalcon',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'sqoop',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'pig',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'oozie-client',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'oozie',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'webhcat-tar-pig',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'webhcat-tar-hive',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hcatalog',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hive-webhcat',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hive-jdbc',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hive-hcatalog',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hive',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-mapreduce',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-client',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-yarn',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-libhdfs',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-hdfs',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop-lzo',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'hadoop',
                              action = ['remove'],
                              )
    self.assertResourceCalled('Package', 'zookeeper',
                              action = ['remove'],
                              )
    self.assertNoMoreResources()