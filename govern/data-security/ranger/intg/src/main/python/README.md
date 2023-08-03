<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Ranger - Python client

Python library for Apache Ranger.

## Installation

Use the package manager [pip](https://pip.pypa.io/en/stable/) to install python client for Apache Ranger.

```bash
> pip install apache-ranger
```

Verify if apache-ranger client is installed:
```bash
> pip list

Package      Version
------------ ---------
apache-ranger 0.0.10
```

## Usage

```python test_ranger.py```
```python
# test_ranger.py

from apache_ranger.model.ranger_service import *
from apache_ranger.client.ranger_client import *
from apache_ranger.model.ranger_policy  import *


## Step 1: create a client to connect to Apache Ranger admin
ranger_url  = 'http://localhost:6080'
ranger_auth = ('admin', 'rangerR0cks!')

# For Kerberos authentication
#
# from requests_kerberos import HTTPKerberosAuth
#
# ranger_auth = HTTPKerberosAuth()

ranger = RangerClient(ranger_url, ranger_auth)

# to disable SSL certificate validation (not recommended for production use!)
#
# ranger.session.verify = False


## Step 2: Let's create a service
service         = RangerService()
service.name    = 'test_hive'
service.type    = 'hive'
service.configs = {'username':'hive', 'password':'hive', 'jdbc.driverClassName': 'org.apache.hive.jdbc.HiveDriver', 'jdbc.url': 'jdbc:hive2://ranger-hadoop:10000', 'hadoop.security.authorization': 'true'}

print('Creating service: name=' + service.name)

created_service = ranger.create_service(service)

print('    created service: name=' + created_service.name + ', id=' + str(created_service.id))


## Step 3: Let's create a policy
policy           = RangerPolicy()
policy.service   = service.name
policy.name      = 'test policy'
policy.resources = { 'database': RangerPolicyResource({ 'values': ['test_db'] }),
                     'table':    RangerPolicyResource({ 'values': ['test_tbl'] }),
                     'column':   RangerPolicyResource({ 'values': ['*'] }) }

allowItem1          = RangerPolicyItem()
allowItem1.users    = [ 'admin' ]
allowItem1.accesses = [ RangerPolicyItemAccess({ 'type': 'create' }),
                        RangerPolicyItemAccess({ 'type': 'alter' }) ]

denyItem1          = RangerPolicyItem()
denyItem1.users    = [ 'admin' ]
denyItem1.accesses = [ RangerPolicyItemAccess({ 'type': 'drop' }) ]

policy.policyItems     = [ allowItem1 ]
policy.denyPolicyItems = [ denyItem1 ]

print('Creating policy: name=' + policy.name)

created_policy = ranger.create_policy(policy)

print('    created policy: name=' + created_policy.name + ', id=' + str(created_policy.id))


## Step 4: Delete policy and service created above
print('Deleting policy: id=' + str(created_policy.id))

ranger.delete_policy_by_id(created_policy.id)

print('    deleted policy: id=' + str(created_policy.id))

print('Deleting service: id=' + str(created_service.id))

ranger.delete_service_by_id(created_service.id)

print('    deleted service: id=' + str(created_service.id))

```

```python test_ranger_kms.py```
```python
# test_ranger_kms.py
from apache_ranger.client.ranger_kms_client import RangerKMSClient
from apache_ranger.client.ranger_client     import HadoopSimpleAuth
from apache_ranger.model.ranger_kms         import RangerKey
import time


##
## Step 1: create a client to connect to Apache Ranger KMS
##
kms_url  = 'http://localhost:9292'
kms_auth = HadoopSimpleAuth('keyadmin')

# For Kerberos authentication
#
# from requests_kerberos import HTTPKerberosAuth
#
# kms_auth = HTTPKerberosAuth()
#
# For HTTP Basic authentication
#
# kms_auth = ('keyadmin', 'rangerR0cks!')

kms_client = RangerKMSClient(kms_url, kms_auth)



##
## Step 2: Let's call KMS APIs
##

kms_status = kms_client.kms_status()
print('kms_status():', kms_status)
print()

key_name = 'test_' + str(int(time.time() * 1000))

key = kms_client.create_key(RangerKey({'name':key_name}))
print('create_key(' + key_name + '):', key)
print()

rollover_key = kms_client.rollover_key(key_name, key.material)
print('rollover_key(' + key_name + '):', rollover_key)
print()

kms_client.invalidate_cache_for_key(key_name)
print('invalidate_cache_for_key(' + key_name + ')')
print()

key_metadata = kms_client.get_key_metadata(key_name)
print('get_key_metadata(' + key_name + '):', key_metadata)
print()

current_key = kms_client.get_current_key(key_name)
print('get_current_key(' + key_name + '):', current_key)
print()

encrypted_keys = kms_client.generate_encrypted_key(key_name, 6)
print('generate_encrypted_key(' + key_name + ', ' + str(6) + '):')
for i in range(len(encrypted_keys)):
  encrypted_key   = encrypted_keys[i]
  decrypted_key   = kms_client.decrypt_encrypted_key(key_name, encrypted_key.versionName, encrypted_key.iv, encrypted_key.encryptedKeyVersion.material)
  reencrypted_key = kms_client.reencrypt_encrypted_key(key_name, encrypted_key.versionName, encrypted_key.iv, encrypted_key.encryptedKeyVersion.material)
  print('  encrypted_keys[' + str(i) + ']: ', encrypted_key)
  print('  decrypted_key[' + str(i) + ']:  ', decrypted_key)
  print('  reencrypted_key[' + str(i) + ']:', reencrypted_key)
print()

reencrypted_keys = kms_client.batch_reencrypt_encrypted_keys(key_name, encrypted_keys)
print('batch_reencrypt_encrypted_keys(' + key_name + ', ' + str(len(encrypted_keys)) + '):')
for i in range(len(reencrypted_keys)):
  print('  batch_reencrypt_encrypted_key[' + str(i) + ']:', reencrypted_keys[i])
print()

key_versions = kms_client.get_key_versions(key_name)
print('get_key_versions(' + key_name + '):', len(key_versions))
for i in range(len(key_versions)):
  print('  key_versions[' + str(i) + ']:', key_versions[i])
print()

for i in range(len(key_versions)):
  key_version = kms_client.get_key_version(key_versions[i].versionName)
  print('get_key_version(' + str(i) + '):', key_version)
print()

key_names = kms_client.get_key_names()
print('get_key_names():', len(key_names))
for i in range(len(key_names)):
  print('  key_name[' + str(i) + ']:', key_names[i])
print()

keys_metadata = kms_client.get_keys_metadata(key_names)
print('get_keys_metadata(' + str(key_names) + '):', len(keys_metadata))
for i in range(len(keys_metadata)):
  print('  key_metadata[' + str(i) + ']:', keys_metadata[i])
print()

key = kms_client.get_key(key_name)
print('get_key(' + key_name + '):', key)
print()

kms_client.delete_key(key_name)
print('delete_key(' + key_name + ')')
```

For more examples, checkout `sample-client` python  project in [ranger-examples](https://github.com/apache/ranger/blob/master/ranger-examples/sample-client/src/main/python/sample_client.py) module.
