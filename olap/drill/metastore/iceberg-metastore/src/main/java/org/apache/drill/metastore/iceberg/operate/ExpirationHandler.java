/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.iceberg.operate;

import org.apache.iceberg.Table;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.exceptions.CommitFailedException;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Iceberg table generates metadata for each modification operation:
 * snapshot, manifest file, table metadata file. Also when performing delete operation,
 * previously stored data files are not deleted. These files with the time
 * can occupy lots of space.
 * <p/>
 * Table metadata in Iceberg is expired automatically
 * if {@link TableProperties#METADATA_DELETE_AFTER_COMMIT_ENABLED} set to true.
 * Number of metadata files to be retained is configured using {@link TableProperties#METADATA_PREVIOUS_VERSIONS_MAX}.
 * Snapshots and data expiration should be called manually to align with metadata expiration process,
 * the same table properties are used to determine if expiration is needed and which number
 * of snapshots should be retained.
 */
public class ExpirationHandler {

  private static final Logger logger = LoggerFactory.getLogger(ExpirationHandler.class);

  private final Table table;
  private final boolean shouldExpire;
  private final int retainNumber;

  public ExpirationHandler(Table table) {
    this.table = table;

    Map<String, String> properties = table.properties();
    this.shouldExpire = PropertyUtil.propertyAsBoolean(properties,
      TableProperties.METADATA_DELETE_AFTER_COMMIT_ENABLED,
      TableProperties.METADATA_DELETE_AFTER_COMMIT_ENABLED_DEFAULT);
    this.retainNumber = PropertyUtil.propertyAsInt(properties,
      TableProperties.METADATA_PREVIOUS_VERSIONS_MAX,
      TableProperties.METADATA_PREVIOUS_VERSIONS_MAX_DEFAULT);
  }

  /**
   * Expires snapshots and related data if needed
   * based on the given table properties values.
   */
  public void expire() {
    if (shouldExpire) {
      table.expireSnapshots()
        .expireOlderThan(System.currentTimeMillis())
        .retainLast(retainNumber)
        .commit();
    }
  }

  /**
   * Expires snapshots and related data and ignores possible exceptions.
   */
  public void expireQuietly() {
    try {
      expire();
    } catch (ValidationException | CommitFailedException e) {
      logger.warn("Unable to expire snapshots: {}", e.getMessage());
      logger.debug("Error when expiring snapshots", e);
    }
  }
}
