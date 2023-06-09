/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.rpc.data;

import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.BitData.BitClientHandshake;
import org.apache.drill.exec.proto.BitData.BitServerHandshake;
import org.apache.drill.exec.proto.BitData.FragmentRecordBatch;
import org.apache.drill.exec.proto.BitData.RpcType;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.SaslMessage;
import org.apache.drill.exec.rpc.RpcException;

import com.google.protobuf.MessageLite;

public class DataDefaultInstanceHandler {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataDefaultInstanceHandler.class);

  public static MessageLite getResponseDefaultInstanceClient(int rpcType) throws RpcException {
    switch (rpcType) {
    case RpcType.ACK_VALUE:
      return Ack.getDefaultInstance();
    case RpcType.HANDSHAKE_VALUE:
      return BitServerHandshake.getDefaultInstance();
    case RpcType.SASL_MESSAGE_VALUE:
      return SaslMessage.getDefaultInstance();
    case RpcType.DATA_ACK_WITH_CREDIT_VALUE:
        return BitData.AckWithCredit.getDefaultInstance();
    default:
      throw new UnsupportedOperationException();
    }
  }

  public static MessageLite getResponseDefaultInstanceServer(int rpcType) throws RpcException {
    switch (rpcType) {
    case RpcType.ACK_VALUE:
      return Ack.getDefaultInstance();
    case RpcType.HANDSHAKE_VALUE:
      return BitClientHandshake.getDefaultInstance();
    case RpcType.REQ_RECORD_BATCH_VALUE:
      return FragmentRecordBatch.getDefaultInstance();
    case RpcType.SASL_MESSAGE_VALUE:
      return SaslMessage.getDefaultInstance();

    default:
      throw new UnsupportedOperationException();
    }
  }
}
