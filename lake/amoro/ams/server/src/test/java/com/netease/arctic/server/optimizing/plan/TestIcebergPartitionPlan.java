package com.netease.arctic.server.optimizing.plan;

import com.google.common.collect.Maps;
import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.optimizing.IcebergRewriteExecutorFactory;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.server.optimizing.scan.IcebergTableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import com.netease.arctic.server.utils.IcebergTableUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

@RunWith(Parameterized.class)
public class TestIcebergPartitionPlan extends TestUnkeyedPartitionPlan {
  public TestIcebergPartitionPlan(CatalogTestHelper catalogTestHelper,
                                  TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {new BasicCatalogTestHelper(TableFormat.ICEBERG),
            new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.ICEBERG),
            new BasicTableTestHelper(false, false)}};
  }


  @Test
  public void testFragmentFiles() {
    testFragmentFilesBase();
  }

  @Test
  public void testSegmentFiles() {
    testSegmentFilesBase();
  }

  @Test
  public void testWithDeleteFiles() {
    testWithDeleteFilesBase();
  }

  @Test
  public void testOnlyOneFragmentFiles() {
    testOnlyOneFragmentFileBase();
  }

  @Override
  protected AbstractPartitionPlan getPartitionPlan() {
    return new IcebergPartitionPlan(getTableRuntime(), getArcticTable(), getPartition(),
        System.currentTimeMillis());
  }

  @Override
  protected TableFileScanHelper getTableFileScanHelper() {
    long baseSnapshotId = IcebergTableUtil.getSnapshotId(getArcticTable(), true);
    return new IcebergTableFileScanHelper(getArcticTable(), baseSnapshotId);
  }

  protected Map<String, String> buildProperties() {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(OptimizingInputProperties.TASK_EXECUTOR_FACTORY_IMPL,
        IcebergRewriteExecutorFactory.class.getName());
    return properties;
  }
}
