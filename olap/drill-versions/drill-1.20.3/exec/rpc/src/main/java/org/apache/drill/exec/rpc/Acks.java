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

import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;

public class Acks {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Acks.class);

  public static final Ack OK = Ack.newBuilder().setOk(true).build();
  public static final Ack FAIL = Ack.newBuilder().setOk(false).build();
  //-------To dynamic credit value: -1 means the receiver failed to solve, 0 means no explicit credit and the sender keeps its sender credit.
  // a value which is great than 0 means having an explicit credit value
  public static final int FAIL_CREDIT = -1;

  public static final int NO_SUGGESTED_CREDIT = 0;
}
