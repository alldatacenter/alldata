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

package org.apache.celeborn.service.deploy.master.clustermeta.ha;

import java.io.File;
import java.io.IOException;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import org.apache.celeborn.common.haclient.MasterNotLeaderException;
import org.apache.celeborn.common.rpc.RpcCallContext;
import org.apache.celeborn.service.deploy.master.clustermeta.AbstractMetaManager;
import org.apache.celeborn.service.deploy.master.clustermeta.ResourceProtos;

public class HAHelper {

  public static boolean checkShouldProcess(
      RpcCallContext context, AbstractMetaManager masterStatusSystem) {
    if ((masterStatusSystem instanceof HAMasterMetaManager)) {
      HARaftServer ratisServer = ((HAMasterMetaManager) masterStatusSystem).getRatisServer();
      if (ratisServer.isLeader()) {
        return true;
      }
      if (context != null) {
        if (ratisServer.getCachedLeaderPeerRpcEndpoint().isPresent()) {
          context.sendFailure(
              new MasterNotLeaderException(
                  ratisServer.getRpcEndpoint(),
                  ratisServer.getCachedLeaderPeerRpcEndpoint().get()));
        } else {
          context.sendFailure(
              new MasterNotLeaderException(
                  ratisServer.getRpcEndpoint(), MasterNotLeaderException.LEADER_NOT_PRESENTED));
        }
      }
      return false;
    }
    return true;
  }

  public static ByteString convertRequestToByteString(ResourceProtos.ResourceRequest request) {
    byte[] requestBytes = request.toByteArray();
    return ByteString.copyFrom(requestBytes);
  }

  public static ResourceProtos.ResourceRequest convertByteStringToRequest(ByteString byteString)
      throws InvalidProtocolBufferException {
    byte[] bytes = byteString.toByteArray();
    return ResourceProtos.ResourceRequest.parseFrom(bytes);
  }

  public static Message convertResponseToMessage(ResourceProtos.ResourceResponse response) {
    byte[] requestBytes = response.toByteArray();
    return Message.valueOf(ByteString.copyFrom(requestBytes));
  }

  /**
   * Creates a temporary snapshot file.
   *
   * @param storage the snapshot storage
   * @return the temporary snapshot file
   * @throws IOException if error occurred while creating the snapshot file
   */
  public static File createTempSnapshotFile(SimpleStateMachineStorage storage) throws IOException {
    File tempDir = new File(storage.getSmDir().getParentFile(), "tmp");
    if (!tempDir.isDirectory() && !tempDir.mkdir()) {
      throw new IOException(
          "Cannot create temporary snapshot directory at " + tempDir.getAbsolutePath());
    }
    return File.createTempFile(
        "raft_snapshot_" + System.currentTimeMillis() + "_", ".dat", tempDir);
  }
}
