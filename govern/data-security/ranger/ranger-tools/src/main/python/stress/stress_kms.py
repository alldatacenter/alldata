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
#


from apache_ranger.client.ranger_kms_client import RangerKMSClient
from apache_ranger.client.ranger_client     import HadoopSimpleAuth
from apache_ranger.model.ranger_kms         import RangerKey
from threading                              import Thread
from datetime                               import datetime
import logging

#
# This script requires Python package apache_ranger to be present.
# The package can be installed using following command:
#    pip3 install --upgrade apache_ranger
#

##
## This script calls KMS APIs from multiple-threads
##
## parameters:
##  kms_url:             URL to Apache Ranger admin server
##  kms_auth:            authentication to connect to Apache Ranger admin server
##  key_count            number of keys to create
##  rollover_key_count:  number of times each key to be rolled over
##  encrypted_key_count: number of encrypted keys to generate per key
##  thread_count:        number of threads to call Apache Ranger APIs from
##
kms_url             = 'http://localhost:9292'
kms_auth            = HadoopSimpleAuth('keyadmin')
key_count           = 320
rollover_key_count  = 10
encrypted_key_count = 6
thread_count        = 16
delete_on_exit      = True
key_prefix          = 'test_'


##
## Setup logging
##  following configuration writes logs to console and file stress_kms.log
##
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)-6s [%(threadName)s] %(message)s",
                    handlers=(logging.StreamHandler(), logging.FileHandler('stress_kms.log')))

LOG = logging.getLogger(__name__)


def run_client(idxClient, t):
  kms_client  = RangerKMSClient(kms_url, kms_auth)
  num_of_keys = int(key_count / thread_count)

  for i in range(0, num_of_keys):
    key_name = key_prefix + str(idxClient) + '_' + str(i)

    try:
      key = kms_client.create_key(RangerKey({'name':key_name}))
      LOG.info('create_key(%s): %s', key_name, key)

      for j in range(1, rollover_key_count):
        rollover_key = kms_client.rollover_key(key_name, key.material)
        LOG.info('rollover_key(%s): %s', key_name, rollover_key)

        expected_version = key_name + "@" + str(j)

        if rollover_key.versionName != expected_version:
          LOG.error('rollover_key(%s): incorrect versionName. Expected "%s", got "%s"', key_name, expected_version, rollover_key.versionName)

      kms_client.invalidate_cache_for_key(key_name)
      LOG.info('invalidate_cache_for_key(%s)', key_name)

      key_metadata = kms_client.get_key_metadata(key_name)
      LOG.info('get_key_metadata(%s): %s', key_name, key_metadata)

      current_key = kms_client.get_current_key(key_name)
      LOG.info('get_current_key(%s): %s', key_name, current_key)

      encrypted_keys = kms_client.generate_encrypted_key(key_name, encrypted_key_count)
      LOG.info('generate_encrypted_key(%s, %d):', key_name, encrypted_key_count)
      for i in range(len(encrypted_keys)):
        encrypted_key   = encrypted_keys[i]
        decrypted_key   = kms_client.decrypt_encrypted_key(key_name, encrypted_key.versionName, encrypted_key.iv, encrypted_key.encryptedKeyVersion.material)
        reencrypted_key = kms_client.reencrypt_encrypted_key(key_name, encrypted_key.versionName, encrypted_key.iv, encrypted_key.encryptedKeyVersion.material)
        LOG.info('  encrypted_keys[%d]:  %s', i, encrypted_key)
        LOG.info('  decrypted_key[%d]:   %s', i, decrypted_key)
        LOG.info('  reencrypted_key[%d]: %s', i, reencrypted_key)

      reencrypted_keys = kms_client.batch_reencrypt_encrypted_keys(key_name, encrypted_keys)
      LOG.info('batch_reencrypt_encrypted_keys(%s, %d):', key_name, len(encrypted_keys))
      for i in range(len(reencrypted_keys)):
        LOG.info('  batch_reencrypt_encrypted_key[%d]: %s', i, reencrypted_keys[i])

      key_versions = kms_client.get_key_versions(key_name)
      LOG.info('get_key_versions(%s): %d', key_name, len(key_versions))
      for i in range(len(key_versions)):
        LOG.info('  key_versions[%d]: %s', i, key_versions[i])

      for i in range(len(key_versions)):
        key_version = kms_client.get_key_version(key_versions[i].versionName)
        LOG.info('get_key_version(%d): %s', i, key_version)

      key = kms_client.get_key(key_name)
      LOG.info('get_key(%s): %s', key_name, key)
    except Exception as e:
      LOG.error('failed while working on key %s: %s', key_name, e.__traceback__, exc_info=True)

  if delete_on_exit:
    LOG.info("REMOVING KEYS")

    for i in range(0, num_of_keys):
      key_name = key_prefix + str(idxClient) + '_' + str(i)

      try:
        kms_client.delete_key(key_name)
        LOG.info('delete_key(%s)', key_name)
      except Exception as e:
        LOG.error('failed in deleting key %s: %s', key_name, e.__traceback__, exc_info=True)

    LOG.info("DONE REMOVING KEYS")


LOG.info("*******************************")
LOG.info("****** TEST RUN: START ******")
LOG.info("*******************************")
LOG.info("")
LOG.info("PARAM: kms_url             = %s", kms_url)
LOG.info("PARAM: kms_auth            = %s", kms_auth)
LOG.info("PARAM: key_count           = %s", key_count)
LOG.info("PARAM: rollover_key_count  = %s", rollover_key_count)
LOG.info("PARAM: encrypted_key_count = %s", encrypted_key_count)
LOG.info("PARAM: thread_count        = %s", thread_count)
LOG.info("PARAM: delete_on_exit      = %s", delete_on_exit)
LOG.info("PARAM: key_prefix          = %s", key_prefix)
LOG.info("")

start_time = datetime.now()

clients = []
for idxClient in range(0, thread_count):
  clients.append(Thread(target=run_client, args=(idxClient, 0)))

LOG.info("STARTING %s THREADS", len(clients))

for client in clients:
  client.start()

LOG.info("WAITING FOR THREADS TO FINISH")

for client in clients:
  client.join()

end_time = datetime.now()

LOG.info("Time taken: %s", end_time - start_time)

LOG.info("********************************")
LOG.info("****** TEST RUN: FINISHED ******")
LOG.info("********************************")
