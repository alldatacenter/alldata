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
package org.apache.drill.exec.rpc.control;

import org.apache.drill.exec.proto.BitControl.BitControlHandshake;
import org.apache.drill.exec.proto.BitControl.BitStatus;
import org.apache.drill.exec.proto.BitControl.CustomMessage;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.UserBitShared.SaslMessage;
import org.apache.drill.exec.rpc.RpcException;

import com.google.protobuf.MessageLite;

public class DefaultInstanceHandler {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultInstanceHandler.class);


  public static MessageLite getResponseDefaultInstance(int rpcType) throws RpcException {
    switch (rpcType) {
    case RpcType.ACK_VALUE:
      return Ack.getDefaultInstance();
    case RpcType.HANDSHAKE_VALUE:
      return BitControlHandshake.getDefaultInstance();
    case RpcType.RESP_FRAGMENT_HANDLE_VALUE:
      return FragmentHandle.getDefaultInstance();
    case RpcType.RESP_FRAGMENT_STATUS_VALUE:
      return FragmentStatus.getDefaultInstance();
    case RpcType.RESP_BIT_STATUS_VALUE:
      return BitStatus.getDefaultInstance();
    case RpcType.RESP_QUERY_STATUS_VALUE:
      return QueryProfile.getDefaultInstance();
    case RpcType.RESP_CUSTOM_VALUE:
      return CustomMessage.getDefaultInstance();
    case RpcType.SASL_MESSAGE_VALUE:
      return SaslMessage.getDefaultInstance();
    default:
      throw new UnsupportedOperationException();
    }
  }
}
