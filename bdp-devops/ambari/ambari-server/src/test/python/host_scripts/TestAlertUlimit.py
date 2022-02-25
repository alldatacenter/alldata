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

import alert_ulimit
from mock.mock import patch, MagicMock
from unittest import TestCase


class TestAlertUlimit(TestCase):

  @patch('resource.getrlimit')
  def test_ulimits(self, ulimit_mock):

    # OK
    ulimit_mock.return_value = 1024, 1024
    res = alert_ulimit.execute()
    self.assertEquals(res, ('OK', ['Ulimit for open files (-n) is 1024']))

    # WARNING
    ulimit_mock.return_value = 200000, 200000
    res = alert_ulimit.execute()
    self.assertEquals(res, ('WARNING', ['Ulimit for open files (-n) is 200000 which is higher or equal than warning value of 200000']))

    # OK
    ulimit_mock.return_value = 1000000, 1000000
    res = alert_ulimit.execute()
    self.assertEquals(res, ('CRITICAL', ['Ulimit for open files (-n) is 1000000 which is higher or equal than critical value of 800000']))