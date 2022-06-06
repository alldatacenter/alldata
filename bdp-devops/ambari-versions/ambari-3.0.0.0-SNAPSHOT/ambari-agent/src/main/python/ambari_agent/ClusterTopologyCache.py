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


from ambari_agent import hostname
from ambari_agent.ClusterCache import ClusterCache
from ambari_agent.Utils import ImmutableDictionary, synchronized

from collections import defaultdict
import threading
import logging

logger = logging.getLogger(__name__)
topology_update_lock = threading.RLock()

class ClusterTopologyCache(ClusterCache):
  """
  Maintains an in-memory cache and disk cache of the topology for
  every cluster. This is useful for having quick access to any of the
  topology properties.
  """

  def __init__(self, cluster_cache_dir, config):
    """
    Initializes the topology cache.
    :param cluster_cache_dir:
    :return:
    """
    self.hosts_to_id = ImmutableDictionary({})
    self.components_by_key = ImmutableDictionary({})
    self.hostname = hostname.hostname(config)
    self.current_host_ids_to_cluster = {}
    self.cluster_local_components = {}
    self.cluster_host_info = None
    self.component_version_map = {}
    super(ClusterTopologyCache, self).__init__(cluster_cache_dir)

  def get_cache_name(self):
    return 'topology'

  @synchronized(topology_update_lock)
  def on_cache_update(self):
    self.cluster_host_info = None

    hosts_to_id = defaultdict(lambda:{})
    components_by_key = defaultdict(lambda:{})

    for cluster_id, cluster_topology in self.iteritems():
      self.current_host_ids_to_cluster[cluster_id] = None
      if 'hosts' in cluster_topology:
        for host_dict in cluster_topology.hosts:
          hosts_to_id[cluster_id][host_dict.hostId] = host_dict

          if host_dict.hostName == self.hostname:
            self.current_host_ids_to_cluster[cluster_id] = host_dict.hostId

      if 'components' in cluster_topology:
        for component_dict in cluster_topology.components:
          key = "{0}/{1}".format(component_dict.serviceName, component_dict.componentName)
          components_by_key[cluster_id][key] = component_dict

    for cluster_id, cluster_topology in self.iteritems():
      self.cluster_local_components[cluster_id] = []
      self.component_version_map[cluster_id] = defaultdict(lambda:defaultdict(lambda: {}))

      if not self.current_host_ids_to_cluster[cluster_id]:
        continue

      current_host_id = self.current_host_ids_to_cluster[cluster_id]

      if 'components' in self[cluster_id]:
        for component_dict in self[cluster_id].components:
          if 'version' in component_dict.commandParams:
            self.component_version_map[cluster_id][component_dict.serviceName][component_dict.componentName] = component_dict.commandParams.version

          if 'hostIds' in component_dict and current_host_id in component_dict.hostIds:
            if current_host_id in component_dict.hostIds:
              self.cluster_local_components[cluster_id].append(component_dict.componentName)


    self.hosts_to_id = ImmutableDictionary(hosts_to_id)
    self.components_by_key = ImmutableDictionary(components_by_key)

  @synchronized(topology_update_lock)
  def get_cluster_host_info(self, cluster_id):
    """
    Get dictionary used in commands as clusterHostInfo
    """
    if self.cluster_host_info is not None:
      return self.cluster_host_info

    cluster_host_info = defaultdict(lambda: [])
    for component_dict in self[cluster_id].components:
      component_name = component_dict.componentName
      hostnames = [self.hosts_to_id[cluster_id][host_id].hostName for host_id in component_dict.hostIds]
      cluster_host_info[component_name.lower()+"_hosts"] += hostnames

    cluster_host_info['all_hosts'] = []
    cluster_host_info['all_racks'] = []
    cluster_host_info['all_ipv4_ips'] = []
    
    for hosts_dict in self[cluster_id].hosts:
      host_name = hosts_dict.hostName
      rack_name = hosts_dict.rackName
      ip = hosts_dict.ipv4
      
      cluster_host_info['all_hosts'].append(host_name)
      cluster_host_info['all_racks'].append(rack_name)
      cluster_host_info['all_ipv4_ips'].append(ip)

    self.cluster_host_info = cluster_host_info
    return cluster_host_info

  @synchronized(topology_update_lock)
  def get_component_info_by_key(self, cluster_id, service_name, component_name):
    """
    Find component by service_name and component_name in list of component dictionaries.
    """
    key = "{0}/{1}".format(service_name, component_name)

    try:
      return self.components_by_key[cluster_id][key]
    except KeyError:
      return None

  @synchronized(topology_update_lock)
  def get_cluster_local_components(self, cluster_id):
    return self.cluster_local_components[cluster_id]

  @synchronized(topology_update_lock)
  def get_cluster_component_version_map(self, cluster_id):
    return self.component_version_map[cluster_id]

  @synchronized(topology_update_lock)
  def get_host_info_by_id(self, cluster_id, host_id):
    """
    Find host by id in list of host dictionaries.
    """
    try:
      return self.hosts_to_id[cluster_id][host_id]
    except KeyError:
      return None

  @synchronized(topology_update_lock)
  def get_current_host_info(self, cluster_id):
    current_host_id = self.current_host_ids_to_cluster[cluster_id]
    return self.get_host_info_by_id(cluster_id, current_host_id)

  @synchronized(topology_update_lock)
  def get_current_host_id(self, cluster_id):
    try:
      return self.current_host_ids_to_cluster[cluster_id]
    except KeyError:
      return None

  @staticmethod
  def _find_host_by_id_in_dict(host_dicts, host_id):
    for host_dict in host_dicts:
      if host_dict['hostId'] == host_id:
        return host_dict
    return None

  @staticmethod
  def _find_component_in_dict(component_dicts, service_name, component_name):
    for component_dict in component_dicts:
      if component_dict['serviceName'] == service_name and component_dict['componentName'] == component_name:
        return component_dict
    return None

  def cache_update(self, cache_update, cache_hash):
    """
    Handle event of update of topology.

    Possible scenarios are:
    - add new host
    - update existing host information by hostId (e.g. rack name)
    - add new component
    - update component information by service_name and component_name
    """
    mutable_dict = self._get_mutable_copy()

    for cluster_id, cluster_updates_dict in cache_update.iteritems():
      # adding a new cluster via UPDATE
      if not cluster_id in mutable_dict:
        mutable_dict[cluster_id] = cluster_updates_dict
        continue

      if 'hosts' in cluster_updates_dict:
        if not 'hosts' in mutable_dict[cluster_id]:
          mutable_dict[cluster_id]['hosts'] = []

        hosts_mutable_list = mutable_dict[cluster_id]['hosts']
        for host_updates_dict in cluster_updates_dict['hosts']:
          host_mutable_dict = ClusterTopologyCache._find_host_by_id_in_dict(hosts_mutable_list, host_updates_dict['hostId'])
          if host_mutable_dict is not None:
            host_mutable_dict.update(host_updates_dict)
          else:
            hosts_mutable_list.append(host_updates_dict)

      if 'components' in cluster_updates_dict:
        if not 'components' in mutable_dict[cluster_id]:
          mutable_dict[cluster_id]['components'] = []

        components_mutable_list = mutable_dict[cluster_id]['components']
        for component_updates_dict in cluster_updates_dict['components']:
          component_mutable_dict = ClusterTopologyCache._find_component_in_dict(components_mutable_list, component_updates_dict['serviceName'], component_updates_dict['componentName'])
          if component_mutable_dict is not None:
            if 'hostIds' in component_updates_dict:
              if not 'hostIds' in component_mutable_dict:
                component_mutable_dict['hostIds'] = []
              component_updates_dict['hostIds'] += component_mutable_dict['hostIds']
              component_updates_dict['hostIds'] = list(set(component_updates_dict['hostIds']))
            component_mutable_dict.update(component_updates_dict)
          else:
            components_mutable_list.append(component_updates_dict)

    self.rewrite_cache(mutable_dict, cache_hash)

  def cache_delete(self, cache_update, cache_hash):
    """
    Handle event of delete on topology.

    Possible scenarios are:
    - delete host
    - delete component
    - delete component host
    - delete cluster
    """
    mutable_dict = self._get_mutable_copy()
    clusters_ids_to_delete = []

    for cluster_id, cluster_updates_dict in cache_update.iteritems():
      if not cluster_id in mutable_dict:
        logger.error("Cannot do topology delete for cluster cluster_id={0}, because do not have information about the cluster".format(cluster_id))
        continue

      if 'hosts' in cluster_updates_dict:
        hosts_mutable_list = mutable_dict[cluster_id]['hosts']
        for host_updates_dict in cluster_updates_dict['hosts']:
          host_to_delete = ClusterTopologyCache._find_host_by_id_in_dict(hosts_mutable_list, host_updates_dict['hostId'])
          if host_to_delete is not None:
            mutable_dict[cluster_id]['hosts'] = [host_dict for host_dict in hosts_mutable_list if host_dict != host_to_delete]
          else:
            logger.error("Cannot do topology delete for cluster_id={0}, host_id={1}, because cannot find the host in cache".format(cluster_id, host_updates_dict['hostId']))

      if 'components' in cluster_updates_dict:
        components_mutable_list = mutable_dict[cluster_id]['components']
        for component_updates_dict in cluster_updates_dict['components']:
          component_mutable_dict = ClusterTopologyCache._find_component_in_dict(components_mutable_list, component_updates_dict['serviceName'], component_updates_dict['componentName'])
          if 'hostIds' in component_mutable_dict:
            exclude_host_ids = component_updates_dict['hostIds']
            component_mutable_dict['hostIds'] = [host_id for host_id in component_mutable_dict['hostIds'] if host_id not in exclude_host_ids]
          if not 'hostIds' in component_mutable_dict or component_mutable_dict['hostIds'] == []:
            if component_mutable_dict is not None:
              mutable_dict[cluster_id]['components'] = [component_dict for component_dict in components_mutable_list if component_dict != component_mutable_dict]
            else:
              logger.error("Cannot do component delete for cluster_id={0}, serviceName={1}, componentName={2}, because cannot find the host in cache".format(cluster_id, component_updates_dict['serviceName'], component_updates_dict['componentName']))

      if cluster_updates_dict == {}:
        clusters_ids_to_delete.append(cluster_id)

    for cluster_id in clusters_ids_to_delete:
      del mutable_dict[cluster_id]

    self.rewrite_cache(mutable_dict, cache_hash)


