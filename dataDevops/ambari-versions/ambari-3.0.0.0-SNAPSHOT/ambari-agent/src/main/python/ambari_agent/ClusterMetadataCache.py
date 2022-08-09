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

from ambari_agent.ClusterCache import ClusterCache
import logging

logger = logging.getLogger(__name__)

class ClusterMetadataCache(ClusterCache):
  """
  Maintains an in-memory cache and disk cache of the metadata send from server for
  every cluster. This is useful for having quick access to any of the
  topology properties.
  """

  def __init__(self, cluster_cache_dir, config):
    """
    Initializes the topology cache.
    :param cluster_cache_dir:
    :return:
    """
    self.config = config
    super(ClusterMetadataCache, self).__init__(cluster_cache_dir)

  def on_cache_update(self):
    try:
      self.config.update_configuration_from_metadata(self['-1']['agentConfigs'])
    except KeyError:
      pass

  def cache_delete(self, cache_update, cache_hash):
    """
    Only deleting cluster is supported here
    """
    mutable_dict = self._get_mutable_copy()
    clusters_ids_to_delete = []

    for cluster_id, cluster_updates_dict in cache_update.iteritems():
      if cluster_updates_dict != {}:
        raise Exception("Deleting cluster subvalues is not supported")

      clusters_ids_to_delete.append(cluster_id)

    for cluster_id in clusters_ids_to_delete:
      del mutable_dict[cluster_id]

    self.rewrite_cache(mutable_dict, cache_hash)

  def get_cache_name(self):
    return 'metadata'
