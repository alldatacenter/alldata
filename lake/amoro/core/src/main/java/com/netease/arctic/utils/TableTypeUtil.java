package com.netease.arctic.utils;

import com.netease.arctic.catalog.IcebergCatalogWrapper;
import com.netease.arctic.table.ArcticTable;

/**
 * Used for check arctic table type
 */
public class TableTypeUtil {
  /**
   * check arctic table is iceberg table format
   * @param arcticTable target arctic table
   * @return Whether iceberg table format
   */
  public static boolean isIcebergTableFormat(ArcticTable arcticTable) {
    return arcticTable instanceof IcebergCatalogWrapper.BasicIcebergTable;
  }
}
