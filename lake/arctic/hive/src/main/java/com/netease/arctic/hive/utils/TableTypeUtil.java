package com.netease.arctic.hive.utils;

import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.table.ArcticTable;

public class TableTypeUtil {
  public static boolean isHive(ArcticTable arcticTable) {
    return arcticTable instanceof SupportHive;
  }
}
