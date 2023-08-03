package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.server.utils.IcebergTableUtil;
import org.apache.iceberg.DataFile;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestIcebergTableFileScanHelper extends TestUnkeyedTableFileScanHelper {
  public TestIcebergTableFileScanHelper(CatalogTestHelper catalogTestHelper,
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

  @Override
  protected TableFileScanHelper buildFileScanHelper() {
    long baseSnapshotId = IcebergTableUtil.getSnapshotId(getArcticTable(), true);
    return new IcebergTableFileScanHelper(getArcticTable(), baseSnapshotId);
  }

  @Override
  protected void assertDataFileClass(IcebergDataFile file) {
    Assert.assertTrue(file.internalFile() + " is not DataFile", file.internalFile() instanceof DataFile);
  }
}
