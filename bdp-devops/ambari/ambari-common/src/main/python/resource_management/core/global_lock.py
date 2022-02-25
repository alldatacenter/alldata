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
import threading
from resource_management.core.exceptions import Fail

# concurrent kinit's can cause the following error:
# Internal credentials cache error while storing credentials while getting initial credentials
LOCK_TYPE_KERBEROS = "KERBEROS_LOCK"

# dictionary of all global lock instances
__GLOBAL_LOCKS = {
  LOCK_TYPE_KERBEROS : threading.RLock()
}

def get_lock(lock_type):
  """
  Gets the global lock associated with the specified type. This does not actually acquire
  the lock, it simply returns the RLock instance. It is up to the caller to invoke RLock.acquire()
  and RLock.release() correctly.
  :param lock_type:
  :return: a global threading.RLock() instance
  :rtype: threading.RLock()
  """
  if lock_type not in __GLOBAL_LOCKS:
    raise Fail("There is no global lock associated with {0}".format(str(lock_type)))

  return __GLOBAL_LOCKS[lock_type]
