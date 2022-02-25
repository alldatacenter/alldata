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

package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.ivory.Cluster;
import org.apache.ambari.server.controller.ivory.Feed;
import org.apache.ambari.server.controller.ivory.Instance;
import org.apache.ambari.server.controller.ivory.IvoryService;

/**
 * An IvoryService implementation used for testing the DR related resource providers.
 */
public class TestIvoryService implements IvoryService{

  private int instanceCounter = 0;

  private final Map<String, Feed> feeds = new HashMap<>();
  private final Map<String, Cluster> clusters = new HashMap<>();
  private final Map<String, Map<String, Instance>> instanceMap = new HashMap<>();

  private final Map<String, String> suspendedFeedStatusMap = new HashMap<>();
  private final Map<String, String> suspendedInstanceStatusMap = new HashMap<>();


  public TestIvoryService(Map<String, Feed> feeds,
                          Map<String, Cluster> clusters,
                          HashMap<String, Map<String, Instance>> instanceMap) {
    if (feeds != null) {
      this.feeds.putAll(feeds);
    }
    if (clusters != null) {
      this.clusters.putAll(clusters);
    }
    if (instanceMap != null) {
      this.instanceMap.putAll(instanceMap);
    }
  }

  @Override
  public void submitFeed(Feed feed) {
    feeds.put(feed.getName(), feed);
  }

  @Override
  public Feed getFeed(String feedName) {
    return feeds.get(feedName);
  }

  @Override
  public List<String> getFeedNames() {
    return new LinkedList<>(feeds.keySet());
  }

  @Override
  public void updateFeed(Feed feed) {
    feeds.put(feed.getName(), feed);
  }

  @Override
  public void suspendFeed(String feedName) {
    suspendedFeedStatusMap.put(feedName, setFeedStatus(feedName, "SUSPENDED"));
  }

  @Override
  public void resumeFeed(String feedName) {
    String suspendedStatus = suspendedFeedStatusMap.get(feedName);
    if (suspendedStatus != null) {
      setFeedStatus(feedName, suspendedStatus);
      suspendedFeedStatusMap.remove(feedName);
    }
  }

  @Override
  public void scheduleFeed(String feedName) {
    setFeedStatus(feedName, "SCHEDULED");
    addDummyInstance(feedName);
  }

  @Override
  public void deleteFeed(String feedName) {
    feeds.remove(feedName);
  }

  @Override
  public void submitCluster(Cluster cluster) {
    clusters.put(cluster.getName(), cluster);
  }

  @Override
  public Cluster getCluster(String clusterName) {
    return clusters.get(clusterName);
  }

  @Override
  public List<String> getClusterNames() {
    return new LinkedList<>(clusters.keySet());
  }

  @Override
  public void updateCluster(Cluster cluster) {
    clusters.put(cluster.getName(), cluster);
  }

  @Override
  public void deleteCluster(String clusterName) {
    clusters.remove(clusterName);
  }

  @Override
  public List<Instance> getInstances(String feedName) {
    Map<String, Instance> instances = instanceMap.get(feedName);
    if (instances != null) {
      return new LinkedList<>(instances.values());
    }
    return Collections.emptyList();
  }

  @Override
  public void suspendInstance(String feedName, String instanceId) {
    String instanceKey = feedName + "/" + instanceId;

    suspendedInstanceStatusMap.put(instanceKey, setInstanceStatus(feedName, instanceId, "SUSPENDED"));
  }

  @Override
  public void resumeInstance(String feedName, String instanceId) {
    String instanceKey = feedName + "/" + instanceId;

    String suspendedStatus = suspendedInstanceStatusMap.get(instanceKey);
    if (suspendedStatus != null) {
      setInstanceStatus(feedName, instanceId, suspendedStatus);
      suspendedInstanceStatusMap.remove(instanceKey);
    }
  }

  @Override
  public void killInstance(String feedName, String instanceId) {
    Map<String, Instance> instances = instanceMap.get(feedName);
    if (instances != null) {
      instances.remove(instanceId);
    }
  }


  // ----- helper methods ----------------------------------------------------

  private String setFeedStatus(String feedName, String status) {
    String currentStatus = null;
    Feed feed = feeds.get(feedName);

    if (feed != null) {
      currentStatus = feed.getStatus();
      if (!currentStatus.equals(status)) {
        feed = new Feed(feed.getName(),
            feed.getDescription(),
            status,
            feed.getSchedule(),
            feed.getSourceClusterName(),
            feed.getSourceClusterStart(),
            feed.getSourceClusterEnd(),
            feed.getSourceClusterLimit(),
            feed.getSourceClusterAction(),
            feed.getTargetClusterName(),
            feed.getTargetClusterStart(),
            feed.getTargetClusterEnd(),
            feed.getTargetClusterLimit(),
            feed.getTargetClusterAction(),
            feed.getProperties());

        feeds.put(feed.getName(), feed);
      }
    }
    return currentStatus;
  }

  private String setInstanceStatus(String feedName, String instanceId, String status) {
    String currentStatus = null;
    Map<String, Instance> instances = instanceMap.get(feedName);

    if (instances != null) {
      Instance instance = instances.get(instanceId);
      if (instance != null) {
        currentStatus = instance.getStatus();
        if (!currentStatus.equals(status)) {
          instance = new Instance(instance.getFeedName(),
                                  instance.getId(),
                                  status,
                                  instance.getStartTime(),
                                  instance.getEndTime(),
                                  instance.getDetails(),
                                  instance.getLog());
          instances.put(instance.getId(), instance);
        }
      }
    }
    return currentStatus;
  }

  private void addDummyInstance(String feedName) {
    Map<String, Instance> instances = instanceMap.get(feedName);
    if (instances == null) {
      instances = new HashMap<>();
      instanceMap.put(feedName, instances);
    }

    String id = "Instance" + instanceCounter++;
    Instance instance = new Instance(feedName, id, "RUNNING",
        "2011-01-01T00:00Z", "2011-01-01T00:10Z", "details", "stdout" );
    instances.put(id, instance);
  }


}
