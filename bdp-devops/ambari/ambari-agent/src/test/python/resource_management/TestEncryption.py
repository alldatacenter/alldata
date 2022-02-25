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
from resource_management.core.encryption import ensure_decrypted

class TestUtils(TestCase):

  def test_attr_to_bitmask(self):
    encypted_value = '${enc=aes256_hex, value=616639333036363938646230613262383a3a32313537386561376136326362656436656135626165313664613265316336663a3a6361633666333432653532393863313364393064626133653562353663663235}'
    encyption_key = 'i%r041K%1VC!C5 K=('
    self.assertEquals('mysecret', ensure_decrypted(encypted_value, encyption_key))
