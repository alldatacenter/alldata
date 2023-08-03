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

package com.netease.arctic.server.table.executor;

import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.TableBlockerMapper;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableServiceTestBase;
import com.netease.arctic.server.table.blocker.TableBlocker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class TestBlockerExpiringExecutor extends TableServiceTestBase {
  private final ServerTableIdentifier tableIdentifier =
      ServerTableIdentifier.of(0L, "test_catalog", "test_db", "test_table_blocker");
  private final Persistency persistency = new Persistency();
  private TableRuntime tableRuntime;
  private TableManager tableManager;

  @Before
  public void mock() {
    tableRuntime = Mockito.mock(TableRuntime.class);
    tableManager = Mockito.mock(TableManager.class);
    Mockito.when(tableRuntime.getTableIdentifier()).thenReturn(tableIdentifier);
  }

  @Test
  public void testExpireBlocker() {
    BlockerExpiringExecutor blockerExpiringExecutor = new BlockerExpiringExecutor(tableManager);
    TableBlocker tableBlocker = new TableBlocker();
    tableBlocker.setTableIdentifier(tableIdentifier);
    tableBlocker.setExpirationTime(System.currentTimeMillis() - 10);
    tableBlocker.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker.setOperations(Collections.singletonList(BlockableOperation.OPTIMIZE.name()));
    persistency.insertTableBlocker(tableBlocker);

    TableBlocker tableBlocker2 = new TableBlocker();
    tableBlocker2.setTableIdentifier(tableIdentifier);
    tableBlocker2.setExpirationTime(System.currentTimeMillis() + 100000);
    tableBlocker2.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker2.setOperations(Collections.singletonList(BlockableOperation.BATCH_WRITE.name()));
    persistency.insertTableBlocker(tableBlocker2);

    Assert.assertEquals(2, persistency.selectTableBlockers(tableIdentifier).size());
    Assert.assertNotNull(persistency.selectTableBlocker(tableBlocker.getBlockerId()));
    Assert.assertNotNull(persistency.selectTableBlocker(tableBlocker2.getBlockerId()));

    blockerExpiringExecutor.execute(tableRuntime);
    Assert.assertEquals(1, persistency.selectTableBlockers(tableIdentifier).size());
    Assert.assertNull(persistency.selectTableBlocker(tableBlocker.getBlockerId()));
    Assert.assertNotNull(persistency.selectTableBlocker(tableBlocker2.getBlockerId()));

    persistency.deleteBlockers(tableIdentifier);
    Assert.assertEquals(0, persistency.selectTableBlockers(tableIdentifier).size());
  }

  private static class Persistency extends PersistentBase {
    public void insertTableBlocker(TableBlocker tableBlocker) {
      doAs(TableBlockerMapper.class,
          mapper -> mapper.insertBlocker(tableBlocker));
    }

    public List<TableBlocker> selectTableBlockers(ServerTableIdentifier tableIdentifier) {
      return getAs(TableBlockerMapper.class,
          mapper -> mapper.selectBlockers(tableIdentifier, 1));
    }

    public void deleteBlockers(ServerTableIdentifier tableIdentifier) {
      doAs(TableBlockerMapper.class,
          mapper -> mapper.deleteBlockers(tableIdentifier));
    }

    public TableBlocker selectTableBlocker(long blockerId) {
      return getAs(TableBlockerMapper.class,
          mapper -> mapper.selectBlocker(blockerId, 1));
    }
  }

}