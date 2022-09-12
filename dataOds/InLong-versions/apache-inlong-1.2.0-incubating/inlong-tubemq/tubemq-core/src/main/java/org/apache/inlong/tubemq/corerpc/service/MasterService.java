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

import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;

public interface MasterService {

    ClientMaster.RegisterResponseM2P producerRegisterP2M(ClientMaster.RegisterRequestP2M request,
                                                         String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.HeartResponseM2P producerHeartbeatP2M(ClientMaster.HeartRequestP2M request,
                                                       String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.CloseResponseM2P producerCloseClientP2M(ClientMaster.CloseRequestP2M request,
                                                         String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.RegisterResponseM2C consumerRegisterC2M(ClientMaster.RegisterRequestC2M request,
                                                         String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.HeartResponseM2C consumerHeartbeatC2M(ClientMaster.HeartRequestC2M request,
                                                       String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.CloseResponseM2C consumerCloseClientC2M(ClientMaster.CloseRequestC2M request,
                                                         String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.RegisterResponseM2B brokerRegisterB2M(ClientMaster.RegisterRequestB2M request,
                                                       String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.HeartResponseM2B brokerHeartbeatB2M(ClientMaster.HeartRequestB2M request,
                                                     String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.CloseResponseM2B brokerCloseClientB2M(ClientMaster.CloseRequestB2M request,
                                                       String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.RegisterResponseM2CV2 consumerRegisterC2MV2(
            ClientMaster.RegisterRequestC2MV2 request,
            String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.HeartResponseM2CV2 consumerHeartbeatC2MV2(
            ClientMaster.HeartRequestC2MV2 request,
            String rmtAddress, boolean overtls) throws Throwable;

    ClientMaster.GetPartMetaResponseM2C consumerGetPartMetaInfoC2M(
            ClientMaster.GetPartMetaRequestC2M request,
            String rmtAddress, boolean overtls) throws Throwable;

}
