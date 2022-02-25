/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.ivory;

import java.util.List;

/**
 * Ivory service.
 */
public interface IvoryService {

  // ----- Feed operations ---------------------------------------------------

  /**
   * Submit a feed.
   *
   * @param feed  the feed
   */
  void submitFeed(Feed feed);

  /**
   * Get a feed for the given name.
   *
   * @param feedName  the feed name
   *
   * @return a feed that matches the given name; null if none is found
   */
  Feed getFeed(String feedName);

  /**
   * Get all the known feed names.
   *
   * @return a list of feed names; may not be null
   */
  List<String> getFeedNames();

  /**
   * Update a feed based on the given {@link Feed} object.
   *
   * @param feed the feed object
   */
  void updateFeed(Feed feed);

  /**
   * Suspend the feed with the given feed name.
   *
   * @param feedName  the feed name
   */
  void suspendFeed(String feedName);

  /**
   * Resume the feed with the given feed name.
   *
   * @param feedName  the feed name
   */
  void resumeFeed(String feedName);

  /**
   * Schedule the feed with the given feed name.
   *
   * @param feedName  the feed name
   */
  void scheduleFeed(String feedName);

  /**
   * Delete the feed with the given feed name.
   *
   * @param feedName the feed name
   */
  void deleteFeed(String feedName);


  // ----- Cluster operations ------------------------------------------------

  /**
   * Submit a cluster.
   *
   * @param cluster  the cluster
   */
  void submitCluster(Cluster cluster);

  /**
   * Get a cluster for the given name.
   *
   * @param clusterName  the cluster name
   *
   * @return a cluster that matches the given name; null if none is found
   */
  Cluster getCluster(String clusterName);

  /**
   * Get all the known cluster names.
   *
   * @return a list of cluster names; may not be null
   */
  List<String> getClusterNames();

  /**
   * Update a cluster based on the given {@link Cluster} object.
   *
   * @param cluster  the cluster
   */
  void updateCluster(Cluster cluster);

  /**
   * Delete the cluster with the given name.
   *
   * @param clusterName  the cluster name
   */
  void deleteCluster(String clusterName);


  // ----- Instance operations -----------------------------------------------

  /**
   * Get all the instances for a given feed.
   *
   * @param feedName  the feed name
   *
   * @return the list of instances for the given feed
   */
  List<Instance> getInstances(String feedName); //read

  /**
   * Suspend the instance for the given feed name and id.
   *
   * @param feedName    the feed name
   * @param instanceId  the id
   */
  void suspendInstance(String feedName, String instanceId);

  /**
   * Resume the instance for the given feed name and id.
   *
   * @param feedName    the feed name
   * @param instanceId  the id
   */
  void resumeInstance(String feedName, String instanceId);

  /**
   * Kill the instance for the given feed name and id.
   *
   * @param feedName    the feed name
   * @param instanceId  the id
   */
  void killInstance(String feedName, String instanceId);
}
