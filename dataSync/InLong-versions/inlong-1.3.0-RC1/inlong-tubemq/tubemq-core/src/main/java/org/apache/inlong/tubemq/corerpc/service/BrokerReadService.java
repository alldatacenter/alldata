/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.service;

import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;

public interface BrokerReadService {

    ClientBroker.RegisterResponseB2C consumerRegisterC2B(ClientBroker.RegisterRequestC2B request,
                                                         String rmtAddress, boolean overtls) throws Throwable;

    ClientBroker.HeartBeatResponseB2C consumerHeartbeatC2B(ClientBroker.HeartBeatRequestC2B request,
                                                           String rmtAddress, boolean overtls) throws Throwable;

    ClientBroker.GetMessageResponseB2C getMessagesC2B(ClientBroker.GetMessageRequestC2B request,
                                                      String rmtAddress, boolean overtls) throws Throwable;

    ClientBroker.CommitOffsetResponseB2C consumerCommitC2B(ClientBroker.CommitOffsetRequestC2B request,
                                                           String rmtAddress, boolean overtls) throws Throwable;

}
