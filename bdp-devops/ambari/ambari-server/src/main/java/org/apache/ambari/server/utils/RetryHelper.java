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
package org.apache.ambari.server.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods to support operations retry
 * TODO injection as Guice singleon, static for now to avoid major modifications
 */
public class RetryHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RetryHelper.class);
  private static Clusters s_clusters;

  private static ThreadLocal<Set<Cluster>> affectedClusters = new ThreadLocal<Set<Cluster>>(){
    @Override
    protected Set<Cluster> initialValue() {
      return new HashSet<>();
    }
  };

  private static int operationsRetryAttempts = 0;

  public static void init(Clusters clusters, int operationsRetryAttempts) {
    s_clusters = clusters;
    RetryHelper.operationsRetryAttempts = operationsRetryAttempts;
  }

  public static void addAffectedCluster(Cluster cluster) {
    if (operationsRetryAttempts > 0 ) {
      affectedClusters.get().add(cluster);
    }
  }

  public static Set<Cluster> getAffectedClusters() {
    return Collections.unmodifiableSet(affectedClusters.get());
  }

  public static void clearAffectedClusters() {
    if (operationsRetryAttempts > 0) {
      affectedClusters.get().clear();
    }
  }

  public static int getOperationsRetryAttempts() {
    return operationsRetryAttempts;
  }

  public static boolean isDatabaseException(Throwable ex) {
    do {
      if (ex instanceof DatabaseException) {
        return true;
      }
      ex = ex.getCause();

    } while (ex != null);

    return false;
  }

  public static void invalidateAffectedClusters() {
    for (Cluster cluster : affectedClusters.get()) {
      s_clusters.invalidate(cluster);
      affectedClusters.get().remove(cluster);
    }
  }

  public static <T> T executeWithRetry(Callable<T> command) throws AmbariException {
    RetryHelper.clearAffectedClusters();
    int retryAttempts = RetryHelper.getOperationsRetryAttempts();
    do {
      try {
        return command.call();
      } catch (Exception e) {
        if (RetryHelper.isDatabaseException(e)) {

          RetryHelper.invalidateAffectedClusters();

          if (retryAttempts > 0) {
            LOG.error("Ignoring database exception to perform operation retry, attempts remaining: " + retryAttempts, e);
            retryAttempts--;
          } else {
            RetryHelper.clearAffectedClusters();
            throw new AmbariException(e.getMessage(), e);
          }
        } else {
          RetryHelper.clearAffectedClusters();
          throw new AmbariException(e.getMessage(), e);
        }
      }

    } while (true);
  }
}
