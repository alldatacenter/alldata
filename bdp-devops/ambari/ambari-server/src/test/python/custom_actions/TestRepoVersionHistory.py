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
import tempfile
from stacks.utils.RMFTestCase import *
from resource_management.libraries.functions import repo_version_history
import logging

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestRepoVersionHistory(RMFTestCase):



  def test_read_and_write_repo_version_history(self):
    f, filename = tempfile.mkstemp()


    try:
      # Check read of empty file
      repo_version_history.REPO_VERSION_HISTORY_FILE = filename
      repo_version_history.Logger = logging.getLogger()
      result = repo_version_history.read_actual_version_from_history_file('2.3.2.0')
      self.assertEquals(result, None)

      # Check read of single value
      repo_version_history.write_actual_version_to_history_file('2.3.2.0', '2.3.2.0-210')
      result = repo_version_history.read_actual_version_from_history_file('2.3.2.0')
      self.assertEquals(result, '2.3.2.0-210')

      # Check read after update
      repo_version_history.write_actual_version_to_history_file('2.3.2.0', '2.3.2.0-2716')
      result = repo_version_history.read_actual_version_from_history_file('2.3.2.0')
      self.assertEquals(result, '2.3.2.0-2716')

      # Check read after update
      repo_version_history.write_actual_version_to_history_file('2.3.2.0', '2.3.2.0-2758')
      result = repo_version_history.read_actual_version_from_history_file('2.3.2.0')
      self.assertEquals(result, '2.3.2.0-2758')

      # Check read after writing down version for another stack
      repo_version_history.write_actual_version_to_history_file('2.3.1.0', '2.3.1.0-27')
      result = repo_version_history.read_actual_version_from_history_file('2.3.1.0')
      self.assertEquals(result, '2.3.1.0-27')
      result = repo_version_history.read_actual_version_from_history_file('2.3.2.0')
      self.assertEquals(result, '2.3.2.0-2758')

      # Check read of another stack
      result = repo_version_history.read_actual_version_from_history_file('2.3.0.0')
      self.assertEquals(result, None)

    finally:
      os.unlink(filename)
