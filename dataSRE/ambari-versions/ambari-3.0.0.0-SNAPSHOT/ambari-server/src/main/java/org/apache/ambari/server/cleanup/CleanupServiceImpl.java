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
package org.apache.ambari.server.cleanup;

import java.util.Set;

import javax.inject.Inject;

import org.apache.ambari.server.orm.dao.Cleanable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Service in charge to perform the cleanup/purge on the entities that support this functionality.
 */
@Singleton
public class CleanupServiceImpl implements CleanupService<TimeBasedCleanupPolicy> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupServiceImpl.class);

  class Result implements CleanupResult {
    private final long affectedRows;
    private final int errorCount;

    public Result(long affectedRows, int errorCount) {
      this.affectedRows = affectedRows;
      this.errorCount = errorCount;
    }

    @Override
    public long getAffectedRows() {
      return affectedRows;
    }

    @Override
    public int getErrorCount() {
      return errorCount;
    }
  }

  // this Set is automatically populated by the guice framework (based on the cleanup interface)
  private Set<Cleanable> cleanables;

  /**
   * Constructor for testing purposes.
   *
   * @param cleanables
   */
  @Inject
  protected CleanupServiceImpl(Set<Cleanable> cleanables) {
    this.cleanables = cleanables;
  }

  /**
   * Triggers the cleanup process on the registered DAOs.
   *
   * @param cleanupPolicy the policy based on which the cleanup is done
   * @return the number of affected rows
   */
  @Override
  public CleanupResult cleanup(TimeBasedCleanupPolicy cleanupPolicy) {
    long affectedRows = 0;
    int errorCount = 0;
    for (Cleanable cleanable : cleanables) {
      LOGGER.info("Running the purge process for DAO: [{}] with cleanup policy: [{}]", cleanable, cleanupPolicy);
      try {
        affectedRows += cleanable.cleanup(cleanupPolicy);
      }
      catch (Exception ex) {
        LOGGER.error("Running the purge process for DAO: [{}] failed with: {}", cleanable, ex);
        errorCount++;
      }
    }

    return new Result(affectedRows, errorCount);
  }

}

