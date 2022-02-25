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

class ClusterAlertDefinitionsCache(ClusterCache):
  """
  Maintains an in-memory cache and disk cache of the host level params send from server for
  every cluster. This is useful for having quick access to any of the
  topology properties.

  Host level params. Is parameters used by execution and status commands which can be generated
  differently for every host.
  """

  def __init__(self, cluster_cache_dir):
    """
    Initializes the host level params cache.
    :param cluster_cache_dir:
    :return:
    """
    super(ClusterAlertDefinitionsCache, self).__init__(cluster_cache_dir)

  def get_alert_definition_index_by_id(self, alert_dict, cluster_id, alert_id):
    definitions = alert_dict[cluster_id]['alertDefinitions']
    for i in xrange(len(definitions)):
      if definitions[i]['definitionId'] == alert_id:
        return i

    return None

  def cache_update(self, cache_update, cache_hash):
    mutable_dict = self._get_mutable_copy()

    for cluster_id in cache_update:
      # adding a new cluster via UPDATE
      if not cluster_id in mutable_dict:
        mutable_dict[cluster_id] = cache_update[cluster_id]
        continue

      for alert_definition in cache_update[cluster_id]['alertDefinitions']:
        id_to_update = alert_definition['definitionId']
        index_of_alert = self.get_alert_definition_index_by_id(mutable_dict, cluster_id, id_to_update)
        if index_of_alert == None:
          mutable_dict[cluster_id]['alertDefinitions'].append(alert_definition)
        else:
          mutable_dict[cluster_id]['alertDefinitions'][index_of_alert] = alert_definition
          
      # for other non-definitions properties
      for property, value in cache_update[cluster_id].iteritems():
        if property == 'alertDefinitions':
          continue
        
        mutable_dict[cluster_id][property] = value

    self.rewrite_cache(mutable_dict, cache_hash)

  def cache_delete(self, cache_update, cache_hash):
    mutable_dict = self._get_mutable_copy()
    clusters_ids_to_delete = []

    for cluster_id in cache_update:
      if not cluster_id in mutable_dict:
        logger.error("Cannot do alert_definitions delete for cluster cluster_id={0}, because do not have information about the cluster".format(cluster_id))
        continue

      # deleting whole cluster
      if cache_update[cluster_id] == {}:
        clusters_ids_to_delete.append(cluster_id)
        continue

      for alert_definition in cache_update[cluster_id]['alertDefinitions']:

        id_to_update = alert_definition['definitionId']
        index_of_alert = self.get_alert_definition_index_by_id(mutable_dict, cluster_id, id_to_update)

        if index_of_alert == None:
          raise Exception("Cannot delete an alert with id={0}".format(id_to_update))

        del mutable_dict[cluster_id]['alertDefinitions'][index_of_alert]

    for cluster_id in clusters_ids_to_delete:
      del mutable_dict[cluster_id]

    self.rewrite_cache(mutable_dict, cache_hash)


  def get_cache_name(self):
    return 'alert_definitions'
