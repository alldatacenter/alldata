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
apache-ranger 0.0.5
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
For more examples, checkout `sample-client` python  project in [ranger-examples](https://github.com/apache/ranger/blob/master/ranger-examples/sample-client/src/main/python/sample_client.py) module.
