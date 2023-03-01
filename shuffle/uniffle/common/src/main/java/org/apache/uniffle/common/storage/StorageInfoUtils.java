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

package org.apache.uniffle.common.storage;

import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.uniffle.proto.RssProtos;


public class StorageInfoUtils {
  public static Map<String, RssProtos.StorageInfo> toProto(
      Map<String, StorageInfo> info) {
    Map<String, RssProtos.StorageInfo> result = Maps.newHashMapWithExpectedSize(info.size());
    info.forEach((k, v) -> result.put(k, v.toProto()));
    return result;
  }

  public static Map<String, StorageInfo> fromProto(Map<String, RssProtos.StorageInfo> info) {
    Map<String, StorageInfo> result = Maps.newHashMapWithExpectedSize(info.size());
    for (Map.Entry<String, RssProtos.StorageInfo> entry : info.entrySet()) {
      String key = entry.getKey();
      RssProtos.StorageInfo val = entry.getValue();
      StorageInfo storageInfo = new StorageInfo(
          val.getMountPoint(),
          StorageMedia.fromProto(val.getStorageMedia()),
          val.getCapacity(),
          val.getUsedBytes(),
          StorageStatus.fromProto(val.getStatus()));
      result.put(key, storageInfo);
    }
    return result;
  }
}
