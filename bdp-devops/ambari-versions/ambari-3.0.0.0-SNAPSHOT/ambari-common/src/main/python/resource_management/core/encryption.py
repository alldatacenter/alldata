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
import os
import ambari_pyaes
from ambari_pbkdf2.pbkdf2 import PBKDF2

def ensure_decrypted(value, encryption_key=None):
  if is_encrypted(value):
    return decrypt(encrypted_value(value), agent_encryption_key() if encryption_key is None else encryption_key)
  else:
    return value

def decrypt(encrypted_value, encryption_key):
  salt, iv, data = [each.decode('hex') for each in encrypted_value.decode('hex').split('::')]
  key = PBKDF2(encryption_key, salt, iterations=65536).read(16)
  aes = ambari_pyaes.AESModeOfOperationCBC(key, iv=iv)
  return ambari_pyaes.util.strip_PKCS7_padding(aes.decrypt(data))

def is_encrypted(value):
  return isinstance(value, basestring) and value.startswith('${enc=aes256_hex, value=') # XXX: ideally it shouldn't be hardcoded but currently only one enc type is supported

def encrypted_value(value):
  return value.split('value=')[1][:-1]

def agent_encryption_key():
  if 'AGENT_ENCRYPTION_KEY' not in os.environ:
    raise RuntimeError('Missing encryption key: AGENT_ENCRYPTION_KEY is not defined at environment.')
  return os.environ['AGENT_ENCRYPTION_KEY']
