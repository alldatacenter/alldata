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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.config.Configuration;
import com.netease.arctic.ams.server.mapper.TableBlockerMapper;
import com.netease.arctic.ams.server.model.TableBlocker;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.blocker.RenewableBlocker;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TableBlockerService extends IJDBCService {
  private static final Logger LOG = LoggerFactory.getLogger(TableBlockerService.class);
  private final long blockerTimeout;

  private final ConcurrentHashMap<TableIdentifier, ReentrantLock> tableLockMap = new ConcurrentHashMap<>();

  public TableBlockerService(Configuration conf) {
    this.blockerTimeout = conf.getLong(ArcticMetaStoreConf.BLOCKER_TIMEOUT);
    LOG.info("table blocker service init with blockerTimeout {} ms", blockerTimeout);
  }

  /**
   * Get all valid blockers.
   *
   * @param tableIdentifier - table
   * @return all valid blockers
   */
  public List<TableBlocker> getBlockers(TableIdentifier tableIdentifier) {
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      return mapper.selectBlockers(tableIdentifier, System.currentTimeMillis());
    } catch (Exception e) {
      LOG.error("failed to get blockers for {}", tableIdentifier, e);
      throw new IllegalStateException("failed to get blockers for " + tableIdentifier, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Block some operations for table.
   *
   * @param tableIdentifier - table
   * @param operations      - operations to be blocked
   * @param properties      - 
   * @return TableBlocker if success
   * @throws OperationConflictException when operations have been blocked
   */
  public TableBlocker block(TableIdentifier tableIdentifier, List<BlockableOperation> operations,
                           @Nonnull Map<String, String> properties)
      throws OperationConflictException {
    Preconditions.checkNotNull(operations, "operations should not be null");
    Preconditions.checkArgument(!operations.isEmpty(), "operations should not be empty");
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);

      long now = System.currentTimeMillis();
      List<TableBlocker> tableBlockers = mapper.selectBlockers(tableIdentifier, now);
      if (conflict(operations, tableBlockers)) {
        throw new OperationConflictException(operations + " is conflict with " + tableBlockers);
      }
      TableBlocker tableBlocker = buildTableBlocker(tableIdentifier, operations, properties, now);
      mapper.insertBlocker(tableBlocker);
      return tableBlocker;
    } catch (OperationConflictException operationConflictException) {
      throw operationConflictException;
    } catch (Exception e) {
      LOG.error("failed to block {} for {}", operations, tableIdentifier, e);
      throw new IllegalStateException("failed to block for " + tableIdentifier, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Release blocker, succeed when blocker not exist.
   *
   * @param tableIdentifier - table
   * @param blockerId       - blockerId
   */
  public void release(TableIdentifier tableIdentifier, String blockerId) {
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      mapper.deleteBlocker(Long.parseLong(blockerId));
    } catch (Exception e) {
      LOG.error("failed to release blocker {} for {}", blockerId, tableIdentifier, e);
      throw new IllegalStateException("failed to release blocker " + blockerId + " for " + tableIdentifier, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Renew blocker.
   *
   * @param tableIdentifier - table
   * @param blockerId       - blockerId
   * @throws IllegalStateException if blocker not exist
   */
  public long renew(TableIdentifier tableIdentifier, String blockerId) throws NoSuchObjectException {
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      long now = System.currentTimeMillis();
      TableBlocker tableBlocker = mapper.selectBlocker(Long.parseLong(blockerId), now);
      if (tableBlocker == null) {
        throw new NoSuchObjectException(
            tableIdentifier + " illegal blockerId " + blockerId + ", it may be released or expired");
      }
      long expirationTime = now + blockerTimeout;
      mapper.updateBlockerExpirationTime(Long.parseLong(blockerId), expirationTime);
      return expirationTime;
    } catch (NoSuchObjectException e1) {
      throw e1;
    } catch (Exception e) {
      LOG.error("failed to renew blocker {} for {}", blockerId, tableIdentifier, e);
      throw new IllegalStateException("failed to renew blockers for " + tableIdentifier, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Check if operation are blocked now.
   *
   * @param tableIdentifier - table
   * @param operation       - operation to check
   * @return true if blocked
   */
  public boolean isBlocked(TableIdentifier tableIdentifier, BlockableOperation operation) {
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);

      long now = System.currentTimeMillis();
      List<TableBlocker> tableBlockers = mapper.selectBlockers(tableIdentifier, now);
      return conflict(operation, tableBlockers);
    } catch (Exception e) {
      LOG.error("failed to check is blocked for {} {}", tableIdentifier, operation, e);
      throw new IllegalStateException("failed to check blocked for " + tableIdentifier + " " + operation, e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Expire blockers.
   *
   * @param tableIdentifier - table
   */
  public int expireBlockers(TableIdentifier tableIdentifier) {
    Lock lock = getLock(tableIdentifier);
    lock.lock();
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      int deleted = mapper.deleteExpiredBlockers(tableIdentifier, System.currentTimeMillis());
      if (deleted > 0) {
        LOG.info("success to expire table blocker {} {}", deleted, tableIdentifier);
      }
      return deleted;
    } catch (Exception e) {
      LOG.error("failed to expire table blocker {}, ignore", tableIdentifier, e);
      return 0;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Clear blockers of table.
   *
   * @param tableIdentifier - table
   */
  public int clearBlockers(TableIdentifier tableIdentifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      return mapper.deleteBlockers(tableIdentifier);
    } catch (Exception e) {
      LOG.error("failed to clear table blocker {}, ignore", tableIdentifier, e);
      return 0;
    }
  }

  @VisibleForTesting
  void insertTableBlocker(TableBlocker blocker) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableBlockerMapper mapper = getMapper(sqlSession, TableBlockerMapper.class);
      mapper.insertBlocker(blocker);
    }
  }

  private boolean conflict(List<BlockableOperation> blockableOperations, List<TableBlocker> blockers) {
    return blockableOperations.stream()
        .anyMatch(operation -> conflict(operation, blockers));
  }

  private boolean conflict(BlockableOperation blockableOperation, List<TableBlocker> blockers) {
    return blockers.stream()
        .anyMatch(blocker -> blocker.getOperations().contains(blockableOperation.name()));
  }

  private TableBlocker buildTableBlocker(TableIdentifier tableIdentifier, List<BlockableOperation> operations,
                                         Map<String, String> properties, long now) {
    TableBlocker tableBlocker = new TableBlocker();
    tableBlocker.setTableIdentifier(tableIdentifier);
    tableBlocker.setCreateTime(now);
    tableBlocker.setExpirationTime(now + blockerTimeout);
    tableBlocker.setOperations(operations.stream().map(BlockableOperation::name).collect(Collectors.toList()));
    HashMap<String, String> propertiesOfTableBlocker = new HashMap<>(properties);
    propertiesOfTableBlocker.put(RenewableBlocker.BLOCKER_TIMEOUT, blockerTimeout + "");
    tableBlocker.setProperties(propertiesOfTableBlocker);
    return tableBlocker;
  }

  private Lock getLock(TableIdentifier tableIdentifier) {
    return tableLockMap.computeIfAbsent(tableIdentifier, s -> new ReentrantLock());
  }
}
