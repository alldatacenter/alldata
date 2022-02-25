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

from unittest import TestCase
from ambari_commons.kerberos.kerberos_common import resolve_encryption_family_list, resolve_encryption_families

class TestEncryptionTypes(TestCase):

  def test_resolves_family(self):
    expected = set([
      'aes256-cts-hmac-sha1-96',
      'aes128-cts-hmac-sha1-96',
      'aes256-cts-hmac-sha384-192',
      'aes128-cts-hmac-sha256-128',
      'rc4-hmac'])
    self.assertEquals(expected, resolve_encryption_family_list(['rc4', 'aes']))

  def test_no_resolve_if_no_family_is_given(self):
    expected = set(['aes256-cts-hmac-sha1-96', 'rc4-hmac'])
    self.assertEquals(expected, resolve_encryption_family_list(['rc4-hmac', 'aes256-cts-hmac-sha1-96']))

  def test_eliminates_duplications(self):
    expected = set([
      'aes256-cts-hmac-sha1-96',
      'aes128-cts-hmac-sha1-96',
      'aes256-cts-hmac-sha384-192',
      'aes128-cts-hmac-sha256-128'])
    self.assertEquals(expected, resolve_encryption_family_list(['aes', 'aes128-cts-hmac-sha1-96']))

  def test_translate_str(self):
    self.assertEquals('rc4-hmac', resolve_encryption_families('rc4'))