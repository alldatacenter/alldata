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

Ambari Agent

"""
import os, sys

from mock.mock import patch
from mock.mock import MagicMock
from unittest import TestCase

from resource_management import *
from resource_management import Script

from ambari_commons.os_check import OSCheck
from clear_repocache import ClearRepoCache

class TestClearRepoCache(TestCase):


  @patch.object(OSCheck, "is_suse_family")
  @patch.object(OSCheck, "is_ubuntu_family")
  @patch.object(OSCheck, "is_redhat_family")
  @patch.object(Script, 'get_config')
  @patch("resource_management.libraries.providers.repository.File")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch.object(System, "os_family", new='redhat')
  def testClearRepoCache(self, structured_out_mock, file_mock, mock_config, is_redhat_mock, is_ubuntu_mock, is_suse_mock):
    is_suse_mock.return_value = False
    is_ubuntu_mock.return_value = False
    is_redhat_mock.return_value = True
    clearRepoCache = ClearRepoCache()

    with Environment("/", test_mode=True) as env:
      clearRepoCache.actionexecute(None)

    self.assertTrue(structured_out_mock.called)
