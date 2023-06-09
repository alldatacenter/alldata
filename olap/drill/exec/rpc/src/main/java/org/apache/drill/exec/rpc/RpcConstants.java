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
package org.apache.drill.exec.rpc;

public class RpcConstants {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RpcConstants.class);

  private RpcConstants(){}

  public static final boolean SOME_DEBUGGING = false;
  public static final boolean EXTRA_DEBUGGING = false;

  // RPC Handler names
  public static final String TIMEOUT_HANDLER = "timeout-handler";
  public static final String PROTOCOL_DECODER = "protocol-decoder";
  public static final String PROTOCOL_ENCODER = "protocol-encoder";
  public static final String MESSAGE_DECODER = "message-decoder";
  public static final String HANDSHAKE_HANDLER = "handshake-handler";
  public static final String MESSAGE_HANDLER = "message-handler";
  public static final String EXCEPTION_HANDLER = "exception-handler";
  public static final String IDLE_STATE_HANDLER = "idle-state-handler";
  public static final String HEARTBEAT_HANDLER = "heartbeat-handler";
  public static final String SASL_DECRYPTION_HANDLER = "sasl-decryption-handler";
  public static final String SASL_ENCRYPTION_HANDLER = "sasl-encryption-handler";
  public static final String LENGTH_DECODER_HANDLER = "length-decoder";
  public static final String CHUNK_CREATION_HANDLER = "chunk-creation-handler";
  public static final String SSL_HANDLER = "ssl-handler";



  // GSSAPI RFC 2222 allows only 3 octets to specify the length of maximum encoded buffer each side can receive.
  // Hence the recommended maximum buffer size is kept as 16Mb i.e. 0XFFFFFF bytes.
  public static final int MAX_RECOMMENDED_WRAPPED_SIZE = 0XFFFFFF;

  public static final int LENGTH_FIELD_OFFSET = 0;
  public static final int LENGTH_FIELD_LENGTH = 4;
  public static final int LENGTH_ADJUSTMENT = 0;
  public static final int INITIAL_BYTES_TO_STRIP = 0;
}
