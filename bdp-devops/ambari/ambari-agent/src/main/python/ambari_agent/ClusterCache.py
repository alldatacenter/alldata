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
"""

import logging
import ambari_simplejson as json
import os
import threading
from collections import defaultdict

from ambari_agent.Utils import Utils

logger = logging.getLogger(__name__)

class ClusterCache(dict):
  """
  Maintains an in-memory cache and disk cache (for debugging purposes) for
  every cluster. This is useful for having quick access to any of the properties.
  """
  COMMON_DATA_CLUSTER = '-1'

  file_locks = defaultdict(threading.RLock)

  def __init__(self, cluster_cache_dir):
    """
    Initializes the cache.
    :param cluster_cache_dir:
    :return:
    """

    self.cluster_cache_dir = cluster_cache_dir

    self.__current_cache_json_file = os.path.join(self.cluster_cache_dir, self.get_cache_name()+'.json')
    self.__current_cache_hash_file = os.path.join(self.cluster_cache_dir, '.'+self.get_cache_name()+'.hash')

    self._cache_lock = threading.RLock()
    self.__file_lock = ClusterCache.file_locks[self.__current_cache_json_file]

    self.hash = None
    cache_dict = {}

    try:
      with self.__file_lock:
        if os.path.isfile(self.__current_cache_json_file):
          with open(self.__current_cache_json_file, 'r') as fp:
            cache_dict = json.load(fp)

        if os.path.isfile(self.__current_cache_hash_file):
          with open(self.__current_cache_hash_file, 'r') as fp:
            self.hash = fp.read()
    except (IOError,ValueError):
      logger.exception("Cannot load data from {0} and {1}".format(self.__current_cache_json_file, self.__current_cache_hash_file))
      self.hash = None
      cache_dict = {}

    try:
      self.rewrite_cache(cache_dict, self.hash)
    except:
      # Example: hostname change and restart causes old topology loading to fail with exception
      logger.exception("Loading saved cache for {0} failed".format(self.__class__.__name__))
      self.rewrite_cache({}, None)

  def get_cluster_indepedent_data(self):
    return self[ClusterCache.COMMON_DATA_CLUSTER]

  def get_cluster_ids(self):
    cluster_ids = self.keys()[:]
    if ClusterCache.COMMON_DATA_CLUSTER in cluster_ids:
      cluster_ids.remove(ClusterCache.COMMON_DATA_CLUSTER)
    return cluster_ids

  def rewrite_cache(self, cache, cache_hash):
    cache_ids_to_delete = []
    for existing_cluster_id in self:
      if not existing_cluster_id in cache:
        cache_ids_to_delete.append(existing_cluster_id)

    for cluster_id, cluster_cache in cache.iteritems():
      self.rewrite_cluster_cache(cluster_id, cluster_cache)

    with self._cache_lock:
      for cache_id_to_delete in cache_ids_to_delete:
        del self[cache_id_to_delete]

    self.on_cache_update()
    self.persist_cache(cache_hash)

  def cache_update(self, update_dict, cache_hash):
    """
    Update the current dictionary by other one
    """
    merged_dict = Utils.update_nested(self._get_mutable_copy(), update_dict)
    self.rewrite_cache(merged_dict, cache_hash)

  def cache_delete(self, delete_dict, cache_hash):
    raise NotImplemented()

  def rewrite_cluster_cache(self, cluster_id, cache):
    """
    Thread-safe method for writing out the specified cluster cache
    and rewriting the in-memory representation.
    :param cluster_id:
    :param cache:
    :return:
    """
    logger.info("Rewriting cache {0} for cluster {1}".format(self.__class__.__name__, cluster_id))

    # The cache should contain exactly the data received from server.
    # Modifications on agent-side will lead to unnecessary cache sync every agent registration. Which is a big concern on perf clusters!
    # Also immutability can lead to multithreading issues.
    immutable_cache = Utils.make_immutable(cache)
    with self._cache_lock:
      self[cluster_id] = immutable_cache

  def persist_cache(self, cache_hash):
    # ensure that our cache directory exists
    if not os.path.exists(self.cluster_cache_dir):
      os.makedirs(self.cluster_cache_dir)

    with self.__file_lock:
      with open(self.__current_cache_json_file, 'w') as f:
        json.dump(self, f, indent=2)

      if self.hash is not None:
        with open(self.__current_cache_hash_file, 'w') as fp:
          fp.write(cache_hash)

    # if all of above are successful finally set the hash
    self.hash = cache_hash

  def _get_mutable_copy(self):
    with self._cache_lock:
      return Utils.get_mutable_copy(self)

  def __getitem__(self, key):
    try:
      return super(ClusterCache, self).__getitem__(key)
    except KeyError:
      raise KeyError("{0} for cluster_id={1} is missing. Check if server sent it.".format(self.get_cache_name().title(), key))

  def on_cache_update(self):
    """
    Call back function called then cache is updated
    """
    pass

  def get_cache_name(self):
    raise NotImplemented()

  def __deepcopy__(self, memo):
    return self.__class__(self.cluster_cache_dir)

  def __copy__(self):
    return self.__class__(self.cluster_cache_dir)