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

package com.netease.arctic.table.blocker;

import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.table.TableIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A manager for table level blocker.
 */
public interface TableBlockerManager {

  /**
   * Return the table identifier of this blocker manager.
   *
   * @return table identifier
   */
  TableIdentifier tableIdentifier();

  /**
   * Block these operations of this table.
   *
   * @param operations should not be empty.
   * @param properties should not be null.
   * @return return the blocker if success
   * @throws OperationConflictException when operations to block are conflict
   */
  Blocker block(List<BlockableOperation> operations, Map<String, String> properties) throws OperationConflictException;

  default Blocker block(List<BlockableOperation> operations) throws OperationConflictException {
    return block(operations, Collections.emptyMap());
  }

  /**
   * Release the blocker.
   *
   * @param blocker the blocker to release
   */
  void release(Blocker blocker);

  /**
   * Get all blockers of this table.
   *
   * @return all blockers
   */
  List<Blocker> getBlockers();
}
