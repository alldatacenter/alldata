package com.netease.arctic.table.blocker;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestBasicTableBlockerManager extends TableTestBase {

  private static final List<BlockableOperation> OPERATIONS = new ArrayList<>();

  static {
    OPERATIONS.add(BlockableOperation.OPTIMIZE);
    OPERATIONS.add(BlockableOperation.BATCH_WRITE);
  }

  public TestBasicTableBlockerManager() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(true, true));
  }

  @Test
  public void testBlockAndRelease() throws OperationConflictException {
    TableBlockerManager tableBlockerManager = getCatalog().getTableBlockerManager(TableTestHelper.TEST_TABLE_ID);
    Assert.assertTrue(tableBlockerManager instanceof BasicTableBlockerManager);
    BasicTableBlockerManager blockerManager = (BasicTableBlockerManager) tableBlockerManager;

    Blocker block = blockerManager.block(OPERATIONS);

    blockerManager.release(block);
  }
}