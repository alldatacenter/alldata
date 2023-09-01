/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.config.Reconfigurable;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.coordinator.access.AccessCheckResult;
import org.apache.uniffle.coordinator.access.AccessInfo;
import org.apache.uniffle.coordinator.access.checker.AccessChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

public class AccessManager implements Reconfigurable {

  private static final Logger LOG = LoggerFactory.getLogger(AccessManager.class);

  private final CoordinatorConf coordinatorConf;
  private final ClusterManager clusterManager;
  private final QuotaManager quotaManager;
  private final Configuration hadoopConf;
  private List<AccessChecker> accessCheckers = Lists.newArrayList();

  public AccessManager(
      CoordinatorConf conf,
      ClusterManager clusterManager,
      QuotaManager quotaManager,
      Configuration hadoopConf) throws Exception {
    this.coordinatorConf = conf;
    this.clusterManager = clusterManager;
    this.hadoopConf = hadoopConf;
    this.quotaManager = quotaManager;
    init();
  }

  private void init() {
    List<String> checkers = coordinatorConf.get(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS);
    if (CollectionUtils.isEmpty(checkers)) {
      LOG.warn("Access checkers is empty, will not init any checkers.");
      return;
    }

    accessCheckers = RssUtils.loadExtensions(AccessChecker.class, checkers, this);
  }

  public AccessCheckResult handleAccessRequest(AccessInfo accessInfo) {
    String uuid = "";
    CoordinatorMetrics.counterTotalAccessRequest.inc();
    for (AccessChecker checker : accessCheckers) {
      AccessCheckResult accessCheckResult = checker.check(accessInfo);
      if (!accessCheckResult.isSuccess()) {
        return accessCheckResult;
      }
      String resultUuid = accessCheckResult.getUuid();
      if (!"".equals(resultUuid)) {
        uuid = resultUuid;
      }
    }

    return new AccessCheckResult(true, Constants.COMMON_SUCCESS_MESSAGE, uuid);
  }

  public CoordinatorConf getCoordinatorConf() {
    return coordinatorConf;
  }

  public ClusterManager getClusterManager() {
    return clusterManager;
  }

  public List<AccessChecker> getAccessCheckers() {
    return accessCheckers;
  }

  public Configuration getHadoopConf() {
    return hadoopConf;
  }

  public QuotaManager getQuotaManager() {
    return quotaManager;
  }

  public void close() throws IOException {
    for (AccessChecker checker : accessCheckers) {
      checker.close();
    }
  }

  public boolean isPropertyReconfigurable(String property) {
    for (AccessChecker checker : accessCheckers) {
      if (checker instanceof Reconfigurable
          && ((Reconfigurable) checker).isPropertyReconfigurable(property)) {
        return true;
      }
    }
    return false;
  }

  public void reconfigure(RssConf conf) {
    for (AccessChecker checker : accessCheckers) {
      if (checker instanceof Reconfigurable) {
        ((Reconfigurable) checker).reconfigure(conf);
      }
    }
  }
}
