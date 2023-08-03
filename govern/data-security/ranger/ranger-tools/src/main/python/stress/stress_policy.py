#!/usr/bin/env python3

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from apache_ranger.model.ranger_service import *
from apache_ranger.client.ranger_client import *
from apache_ranger.model.ranger_policy  import *
from threading                          import Thread, Lock
from datetime                           import datetime
from random                             import randrange
import logging


##
## This script creates and updates Hive policies from multiple threads.
## Optionally deletes all created policies at the end.
##
## parameters:
##  ranger_url:      URL to Apache Ranger admin server
##  ranger_auth:     authentication to connect to Apache Ranger admin server
##  ranger_qparams:  query parameters to be sent in each request to Ranger (optional)
##  ranger_headers:  HTTP headers to be sent in each request to Ranger (optional)
##  service_name:    service to create policies in
##  permissions:     list of permissions supported in Hive
##  thread_count:    number of threads to call Apache Ranger APIs from
##  policy_count:    number of policies to create
##  min_col_count:   minumum number of columns in a policy
##  max_col_count:   maximum number of columns in a policy
##  min_item_count:  minimum number of policyItems in a policy
##  max_item_count:  maximum number of policyItems in a policy
##  min_user_count:  minumum number of users in a policyItem
##  max_user_count:  maximum number of users in a policyItem
##  min_group_count: minumum number of groups in a policyItem
##  max_group_count: maximum number of groups in a policyItem
##  min_perm_count:  minumum number of permissions in a policyItem
##  max_perm_count:  maximum number of permissions in a policyItem
##
ranger_url      = 'http://localhost:6080'
ranger_auth     = ('admin', 'rangerR0cks!')
ranger_qparams  = None
ranger_headers  = None
service_name    = 'dev_hive'
permissions     = [ 'create', 'alter', 'drop', 'select', 'update', 'index', 'lock', 'all', 'read', 'write', 'repladmin', 'serviceadmin', 'tempudfadmin', 'refresh' ]
thread_count    = 25
policy_count    = 5000
min_col_count   = 1
max_col_count   = 6
min_item_count  = 1
max_item_count  = 5
min_user_count  = 5
max_user_count  = 20
min_group_count = 5
max_group_count = 30
min_perm_count  = 2
max_perm_count  = len(permissions)

tbls_per_db     = 20
prefix_db       = 'db_'
prefix_tbl      = 'tbl_'
prefix_col      = 'col_'
prefix_user     = 'user_'
prefix_group    = 'group_'
delete_on_exit  = True

##
## global variables used by the script
##
lock_next_policy     = Lock()
lock_next_resource   = Lock()
lock_progress        = Lock()
lock_counts          = Lock()
next_policy_idx      = 0
next_db_idx          = 0
next_tbl_idx         = None
created_policies     = []
updated_policy_count = 0
deleted_policy_count = 0
policy_item_count    = 0
policy_user_count    = 0
policy_group_count   = 0
policy_perm_count    = 0



##
## Setup logging
##  following configuration writes logs to console and file stress_policy.log
##
logging.basicConfig(level=logging.INFO,
		    format="%(asctime)s [%(threadName)s] %(message)s",
                    handlers=(logging.StreamHandler(), logging.FileHandler('stress_policy.log')))

LOG = logging.getLogger(__name__)

##
## create policies using the given client
##
def create_policies(ranger, thread_idx):
  global created_policies

  while True:
    policy = get_next_policy()

    if policy is None:
      LOG.debug("***END OF LOOP***")
      break

    for attempt in range(0, 3):
      LOG.debug('Creating policy - attempt #%d: %s', attempt, policy)

      created_policy = ranger.create_policy(policy)

      LOG.debug('Created policy: %s', created_policy)

      if created_policy:
        break

    lock_progress.acquire()

    if created_policy:
      LOG.debug('Created policy: id=%d', created_policy.id)
      created_policies.append(created_policy)

    created_policy_count = len(created_policies)

    if (created_policy_count % 50) == 0:
      LOG.info("  CREATE POLICY PROGRESS: %d policies", created_policy_count)

    lock_progress.release()


##
## update policies using the given client
##
def update_policies(ranger, thread_idx):
  global created_policies, updated_policy_count

  count     = int(policy_count / thread_count)
  start_idx = thread_idx * count

  for i in range(0, count):
    policy         = created_policies[start_idx + i]
    updated_policy = get_updated_policy(policy)

    for attempt in range(0, 3):
      LOG.debug('Updating policy id=%d - attempt #%d', policy.id, attempt)

      result = ranger.update_policy_by_id(policy.id, updated_policy)

      if result:
        break

    lock_progress.acquire()

    if result:
      updated_policy_count += 1

    if (updated_policy_count % 50) == 0:
      LOG.info("  UPDATE POLICY PROGRESS: %d policies", updated_policy_count)

    lock_progress.release()


##
## delete policies using the given client
##
def delete_policies(ranger, thread_idx):
  global created_policies, deleted_policy_count

  count     = int(policy_count / thread_count)
  start_idx = thread_idx * count

  for i in range(0, count):
    policy = created_policies[start_idx + i]

    LOG.debug('Deleting policy id=%d', policy.id)

    ranger.delete_policy_by_id(policy.id)

    lock_progress.acquire()

    deleted_policy_count += 1

    if (deleted_policy_count % 50) == 0:
      LOG.info("  DELETE POLICY PROGRESS: %d policies", deleted_policy_count)

    lock_progress.release()

def get_next_resource():
  global lock_next_resource
  global next_db_idx, next_tbl_idx

  lock_next_resource.acquire()

  db_idx  = next_db_idx
  tbl_idx = next_tbl_idx

  if next_tbl_idx is None:
    next_tbl_idx = 0
  else:
    next_tbl_idx += 1

    if next_tbl_idx >= tbls_per_db:
      next_tbl_idx = None
      next_db_idx  += 1

  lock_next_resource.release()

  resource = { 'database': { 'values': [ prefix_db + str(db_idx) ] } }

  if tbl_idx is not None:
    resource['table'] = { 'values': [ prefix_tbl + str(tbl_idx) ] }

    num_of_cols = randrange(min_col_count, max_col_count)

    cols = []
    for i in range(0, num_of_cols):
      cols.append(prefix_col + str(i))

    resource['column'] = { 'values': cols }

  return resource

def get_policy_items():
  global lock_counts
  global policy_item_count, policy_user_count, policy_group_count, policy_perm_count

  policy_items = []

  num_of_items = randrange(min_item_count, max_item_count)
  user_count   = 0
  group_count  = 0
  perm_count   = 0

  for idxItem in range(0, num_of_items):
    num_of_users  = randrange(min_user_count, max_user_count)
    num_of_groups = randrange(min_group_count, max_group_count)
    num_of_perms  = randrange(min_perm_count, max_perm_count)

    policyItem = RangerPolicyItem({ 'users': [], 'groups': [], 'accesses': []})

    for i in range(0, num_of_users):
      policyItem.users.append(prefix_user + str(i))

    for i in range(0, num_of_groups):
      policyItem.groups.append(prefix_group + str(i))

    for i in range(0, num_of_perms):
      policyItem.accesses.append(RangerPolicyItemAccess({ 'type': get_permission(i) }))

    policy_items.append(policyItem)

    user_count  += num_of_users
    group_count += num_of_groups
    perm_count  += num_of_perms

  lock_counts.acquire()

  policy_item_count  += num_of_items
  policy_user_count  += user_count
  policy_group_count += group_count
  policy_perm_count  += perm_count

  lock_counts.release()

  return policy_items


def get_next_policy():
  global lock_next_policy
  global next_policy_idx

  lock_next_policy.acquire()

  policy_idx = next_policy_idx

  next_policy_idx += 1

  lock_next_policy.release()

  if policy_idx >= policy_count:
    return None

  policy = RangerPolicy({ 'service': service_name, 'name': 'test policy - ' + str(policy_idx)})

  policy.resources   = get_next_resource()
  policy.policyItems = get_policy_items()

  return policy

#
# retain policy resources
# update policy items: users, groups, permissions
#
def get_updated_policy(policy):
  updated_policy = RangerPolicy({ 'id': policy.id, 'guid': policy.guid, 'service': policy.service, 'name': policy.name, 'resources': policy.resources })

  updated_policy.policyItems = get_policy_items()

  return updated_policy

def get_permission(idx):
  return permissions[idx % len(permissions)]

def reset_counts():
  global policy_item_count, policy_user_count, policy_group_count, policy_perm_count

  policy_item_count  = 0
  policy_user_count  = 0
  policy_group_count = 0
  policy_perm_count  = 0


LOG.info("*******************************")
LOG.info("****** TEST RUN: START ******")
LOG.info("*******************************")

run_start_time = datetime.now()

##
## create one RangerClient for each thread
##
clients = []
for i in range(0, thread_count):
  clients.append(RangerClient(ranger_url, ranger_auth, ranger_qparams, ranger_headers))

##
##  create threads to create policies
##
threads = []
for i in range(0, thread_count):
  threads.append(Thread(target=create_policies, args=(clients[i], i)))

LOG.info("STARTING %s CREATE POLICY THREADS", len(threads))

start_time = datetime.now()

for thread in threads:
  thread.start()

LOG.info("WAITING FOR CREATE POLICY THREADS TO FINISH")

for thread in threads:
  thread.join()

end_time = datetime.now()

LOG.info("Created %d policies (items=%d, users=%d, groups=%d, permissions=%d) in %s", len(created_policies), policy_item_count, policy_user_count, policy_group_count, policy_perm_count, end_time - start_time)


reset_counts()

##
##  create threads to update policies
##
threads = []
for i in range(0, thread_count):
  threads.append(Thread(target=update_policies, args=(clients[i], i)))

LOG.info("STARTING %s UPDATE POLICY THREADS", len(threads))

start_time = datetime.now()

for thread in threads:
  thread.start()

LOG.info("WAITING FOR UPDATE POLICY THREADS TO FINISH")

for thread in threads:
  thread.join()

end_time = datetime.now()

LOG.info("Updated %d policies (items=%d, users=%d, groups=%d, permissions=%d) in %s", len(created_policies), policy_item_count, policy_user_count, policy_group_count, policy_perm_count, end_time - start_time)


if delete_on_exit:
  threads = []
  for i in range(0, thread_count):
    threads.append(Thread(target=delete_policies, args=(clients[i], i)))

  LOG.info("STARTING %s DELETE POLICY THREADS", len(clients))

  start_time = datetime.now()

  for thread in threads:
    thread.start()

  LOG.info("WAITING FOR UPDATE POLICY THREADS TO FINISH")

  for thread in threads:
    thread.join()

  end_time = datetime.now()

  LOG.info("Deleted %d policies in %s", len(created_policies), end_time - start_time)

run_end_time = datetime.now()

LOG.info("Time taken: %s", run_end_time - run_start_time)

LOG.info("********************************")
LOG.info("****** TEST RUN: FINISHED ******")
LOG.info("********************************")
