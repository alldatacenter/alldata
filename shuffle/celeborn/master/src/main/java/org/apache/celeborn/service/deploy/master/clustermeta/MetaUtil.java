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

package org.apache.celeborn.service.deploy.master.clustermeta;

import java.util.HashMap;
import java.util.Map;

import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.identity.UserIdentifier$;
import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.quota.ResourceConsumption;
import org.apache.celeborn.common.util.Utils;

public class MetaUtil {
  private MetaUtil() {}

  public static WorkerInfo addrToInfo(ResourceProtos.WorkerAddress address) {
    return new WorkerInfo(
        address.getHost(),
        address.getRpcPort(),
        address.getPushPort(),
        address.getFetchPort(),
        address.getReplicatePort());
  }

  public static ResourceProtos.WorkerAddress infoToAddr(WorkerInfo info) {
    return ResourceProtos.WorkerAddress.newBuilder()
        .setHost(info.host())
        .setRpcPort(info.rpcPort())
        .setPushPort(info.pushPort())
        .setFetchPort(info.fetchPort())
        .setReplicatePort(info.replicatePort())
        .build();
  }

  public static Map<String, DiskInfo> fromPbDiskInfos(
      Map<String, ResourceProtos.DiskInfo> diskInfos) {
    Map<String, DiskInfo> map = new HashMap<>();

    diskInfos.forEach(
        (k, v) -> {
          DiskInfo diskInfo =
              new DiskInfo(
                  v.getMountPoint(),
                  v.getUsableSpace(),
                  v.getAvgFlushTime(),
                  v.getAvgFetchTime(),
                  v.getUsedSlots());
          diskInfo.setStatus(Utils.toDiskStatus(v.getStatus()));
          map.put(k, diskInfo);
        });
    return map;
  }

  public static Map<String, ResourceProtos.DiskInfo> toPbDiskInfos(
      Map<String, DiskInfo> diskInfos) {
    Map<String, ResourceProtos.DiskInfo> map = new HashMap<>();
    diskInfos.forEach(
        (k, v) ->
            map.put(
                k,
                ResourceProtos.DiskInfo.newBuilder()
                    .setMountPoint(v.mountPoint())
                    .setUsableSpace(v.actualUsableSpace())
                    .setAvgFlushTime(v.avgFlushTime())
                    .setAvgFetchTime(v.avgFetchTime())
                    .setUsedSlots(v.activeSlots())
                    .setStatus(v.status().getValue())
                    .build()));
    return map;
  }

  public static Map<UserIdentifier, ResourceConsumption> fromPbUserResourceConsumption(
      Map<String, ResourceProtos.ResourceConsumption> pbUserResourceConsumption) {
    Map<UserIdentifier, ResourceConsumption> map = new HashMap<>();
    pbUserResourceConsumption.forEach(
        (k, v) -> {
          ResourceConsumption resourceConsumption =
              new ResourceConsumption(
                  v.getDiskBytesWritten(),
                  v.getDiskFileCount(),
                  v.getHdfsBytesWritten(),
                  v.getHdfsFileCount());
          map.put(UserIdentifier$.MODULE$.apply(k), resourceConsumption);
        });
    return map;
  }

  public static Map<String, ResourceProtos.ResourceConsumption> toPbUserResourceConsumption(
      Map<UserIdentifier, ResourceConsumption> userResourceConsumption) {
    Map<String, ResourceProtos.ResourceConsumption> map = new HashMap<>();
    userResourceConsumption.forEach(
        (k, v) ->
            map.put(
                k.toString(),
                ResourceProtos.ResourceConsumption.newBuilder()
                    .setDiskBytesWritten(v.diskBytesWritten())
                    .setDiskFileCount(v.diskFileCount())
                    .setHdfsBytesWritten(v.hdfsBytesWritten())
                    .setHdfsFileCount(v.hdfsFileCount())
                    .build()));
    return map;
  }
}
