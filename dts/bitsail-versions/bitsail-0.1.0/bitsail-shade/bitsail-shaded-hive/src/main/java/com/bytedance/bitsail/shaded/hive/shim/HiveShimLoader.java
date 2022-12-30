/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.shaded.hive.shim;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import org.apache.hive.common.util.HiveVersionInfo;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A loader to load HiveShim.
 */
public class HiveShimLoader {
  private static final Map<String, HiveShim> SHIMS = new ConcurrentHashMap<>(2);

  static {
    HiveShimV120 hiveShimV120 = new HiveShimV120();
    HiveShimV121 hiveShimV121 = new HiveShimV121();
    HiveShimV122 hiveShimV122 = new HiveShimV122();
    HiveShimV200 hiveShimV200 = new HiveShimV200();
    HiveShimV210 hiveShimV210 = new HiveShimV210();
    HiveShimV211 hiveShimV211 = new HiveShimV211();
    HiveShimV310 hiveShimV310 = new HiveShimV310();
    HiveShimV312 hiveShimV312 = new HiveShimV312();
    SHIMS.put(hiveShimV120.getVersion(), hiveShimV120);
    SHIMS.put(hiveShimV121.getVersion(), hiveShimV121);
    SHIMS.put(hiveShimV122.getVersion(), hiveShimV122);
    SHIMS.put(hiveShimV200.getVersion(), hiveShimV200);
    SHIMS.put(hiveShimV210.getVersion(), hiveShimV210);
    SHIMS.put(hiveShimV211.getVersion(), hiveShimV211);
    SHIMS.put(hiveShimV310.getVersion(), hiveShimV310);
    SHIMS.put(hiveShimV312.getVersion(), hiveShimV312);
  }

  private HiveShimLoader() {
  }

  public static HiveShim loadHiveShim() {
    return loadHiveShim(getHiveVersion());
  }

  public static HiveShim loadHiveShim(String version) {
    HiveShim hiveShim = SHIMS.get(version);
    if (Objects.isNull(hiveShim)) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "Unsupported Hive version " + version);
    }
    return hiveShim;
  }

  public static String getHiveVersion() {
    return HiveVersionInfo.getShortVersion();
  }
}
