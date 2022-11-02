# !/usr/bin/env python

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

from resource_management.core import global_lock
from resource_management.core.exceptions import Fail

from unittest import TestCase

utils = __import__('ambari_server.utils').utils

class TestGlobalLock(TestCase):
  def test_get_invalid_lock(self):
    """
    Tests that an invalid lock throws an exception
    :return:
    """
    try:
      global_lock.get_lock("INVALID")
      self.fail("Expected an exception when trying to retrieve an invalid lock")
    except Fail:
      pass

  def test_get_kerberos_lock(self):
    """
    Tests that the kerberos lock can be retrieved.
    :return:
    """
    kerberos_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    self.assertFalse(kerberos_lock is None)

    kerberos_lock_2 = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    self.assertEqual(kerberos_lock, kerberos_lock_2)

    kerberos_lock.acquire()
    kerberos_lock.release()

    kerberos_lock_2.acquire()
    kerberos_lock_2.release()