package com.netease.arctic.utils;

import com.netease.arctic.catalog.IcebergCatalogWrapper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;

public class ArcticTableUtil {
  /**
   * check arctic table is iceberg table format
   * @param arcticTable target arctic table
   * @return Whether iceberg table format
   */
  public static boolean isIcebergTableFormat(ArcticTable arcticTable) {
    return arcticTable instanceof IcebergCatalogWrapper.BasicIcebergTable;
  }

  /**
   * Return the base store of the arctic table.
   */
  public static UnkeyedTable baseStore(ArcticTable arcticTable) {
    if (arcticTable.isKeyedTable()) {
      return arcticTable.asKeyedTable().baseTable();
    } else {
      return arcticTable.asUnkeyedTable();
    }
  }

  /**
   * Return the table root location of the arctic table.
   */
  public static String tableRootLocation(ArcticTable arcticTable) {
    String tableRootLocation;
    if (!ArcticTableUtil.isIcebergTableFormat(arcticTable) && arcticTable.isUnkeyedTable()) {
      tableRootLocation = TableFileUtil.getFileDir(arcticTable.location());
    } else {
      tableRootLocation = arcticTable.location();
    }
    return tableRootLocation;
  }
}
