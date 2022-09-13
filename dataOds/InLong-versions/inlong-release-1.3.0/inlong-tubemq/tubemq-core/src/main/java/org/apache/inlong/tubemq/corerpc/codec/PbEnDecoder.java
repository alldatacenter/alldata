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

package org.apache.inlong.tubemq.corerpc.codec;

import com.google.protobuf.AbstractMessageLite;
import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corerpc.RpcConstants;

/**
 * PB corresponding method, service type codec util tools
 */
public class PbEnDecoder {
    // The set of methods supported by RPC, only the methods in the map are accepted
    private static final Map<String, Integer> rpcMethodMap =
            new HashMap<>();
    // The set of services supported by RPC, only the services in the map are processed.
    private static final Map<String, Integer> rpcServiceMap =
            new HashMap<>();

    static {
        // The MAP corresponding to the writing of these strings and constants when the system starts up
        rpcMethodMap.put("producerRegisterP2M", RpcConstants.RPC_MSG_MASTER_PRODUCER_REGISTER);
        rpcMethodMap.put("producerHeartbeatP2M", RpcConstants.RPC_MSG_MASTER_PRODUCER_HEARTBEAT);
        rpcMethodMap.put("producerCloseClientP2M", RpcConstants.RPC_MSG_MASTER_PRODUCER_CLOSECLIENT);
        rpcMethodMap.put("consumerRegisterC2M", RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER);
        rpcMethodMap.put("consumerHeartbeatC2M", RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT);
        rpcMethodMap.put("consumerCloseClientC2M", RpcConstants.RPC_MSG_MASTER_CONSUMER_CLOSECLIENT);
        rpcMethodMap.put("brokerRegisterB2M", RpcConstants.RPC_MSG_MASTER_BROKER_REGISTER);
        rpcMethodMap.put("brokerHeartbeatB2M", RpcConstants.RPC_MSG_MASTER_BROKER_HEARTBEAT);
        rpcMethodMap.put("brokerCloseClientB2M", RpcConstants.RPC_MSG_MASTER_BROKER_CLOSECLIENT);
        rpcMethodMap.put("consumerRegisterC2B", RpcConstants.RPC_MSG_BROKER_CONSUMER_REGISTER);
        rpcMethodMap.put("consumerHeartbeatC2B", RpcConstants.RPC_MSG_BROKER_CONSUMER_HEARTBEAT);
        rpcMethodMap.put("getMessagesC2B", RpcConstants.RPC_MSG_BROKER_CONSUMER_GETMESSAGE);
        rpcMethodMap.put("consumerCommitC2B", RpcConstants.RPC_MSG_BROKER_CONSUMER_COMMIT);
        rpcMethodMap.put("sendMessageP2B", RpcConstants.RPC_MSG_BROKER_PRODUCER_SENDMESSAGE);
        rpcMethodMap.put("consumerRegisterC2MV2",
                RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER_V2);
        rpcMethodMap.put("consumerHeartbeatC2MV2",
                RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT_V2);
        rpcMethodMap.put("consumerGetPartMetaInfoC2M",
                RpcConstants.RPC_MSG_MASTER_CONSUMER_GET_PART_META);

        rpcServiceMap.put("org.apache.inlong.tubemq.corerpc.service.MasterService",
                RpcConstants.RPC_SERVICE_TYPE_MASTER_SERVICE);
        rpcServiceMap.put("org.apache.inlong.tubemq.corerpc.service.BrokerReadService",
                RpcConstants.RPC_SERVICE_TYPE_BROKER_READ_SERVICE);
        rpcServiceMap.put("org.apache.inlong.tubemq.corerpc.service.BrokerWriteService",
                RpcConstants.RPC_SERVICE_TYPE_BROKER_WRITE_SERVICE);
        rpcServiceMap.put("org.apache.inlong.tubemq.corerpc.service.BrokerWriteService$AsyncService",
                RpcConstants.RPC_SERVICE_TYPE_BROKER_WRITE_SERVICE);

    }

    public static byte[] pbEncode(Object object) throws Exception {
        AbstractMessageLite rspDataMessage = (AbstractMessageLite) object;
        return rspDataMessage.toByteArray();
    }

    /**
     * Decode pb content
     *
     * @param isRequest     whether a request message
     * @param methodId      the method id
     * @param bytes         the message content
     * @return              the message's object
     * @throws Exception    the exception while decoding messsage
     */
    public static Object pbDecode(boolean isRequest, int methodId, byte[] bytes) throws Exception {
        // #lizard forgives
        // According to the method ID carried in the pb message, the corresponding class is directly used for mapping.
        if (isRequest) {
            switch (methodId) {
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_REGISTER: {
                    return ClientMaster.RegisterRequestP2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_HEARTBEAT: {
                    return ClientMaster.HeartRequestP2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_CLOSECLIENT: {
                    return ClientMaster.CloseRequestP2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER: {
                    return ClientMaster.RegisterRequestC2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT: {
                    return ClientMaster.HeartRequestC2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_CLOSECLIENT: {
                    return ClientMaster.CloseRequestC2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_REGISTER: {
                    return ClientMaster.RegisterRequestB2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_HEARTBEAT: {
                    return ClientMaster.HeartRequestB2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER_V2: {
                    return ClientMaster.RegisterRequestC2MV2.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT_V2: {
                    return ClientMaster.HeartRequestC2MV2.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_GET_PART_META: {
                    return ClientMaster.GetPartMetaRequestC2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_CLOSECLIENT: {
                    return ClientMaster.CloseRequestB2M.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_SENDMESSAGE: {
                    return ClientBroker.SendMessageRequestP2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_REGISTER: {
                    return ClientBroker.RegisterRequestC2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_HEARTBEAT: {
                    return ClientBroker.HeartBeatRequestC2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_GETMESSAGE: {
                    return ClientBroker.GetMessageRequestC2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_COMMIT: {
                    return ClientBroker.CommitOffsetRequestC2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_CLOSE:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_CLOSE:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_REGISTER:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_HEARTBEAT:
                default: {
                    throw new Exception(new StringBuilder(256)
                            .append("Unsupported method ID :")
                            .append(methodId).toString());
                }
            }
        } else {
            switch (methodId) {
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_REGISTER: {
                    return ClientMaster.RegisterResponseM2P.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_HEARTBEAT: {
                    return ClientMaster.HeartResponseM2P.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_PRODUCER_CLOSECLIENT: {
                    return ClientMaster.CloseResponseM2P.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER: {
                    return ClientMaster.RegisterResponseM2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT: {
                    return ClientMaster.HeartResponseM2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_CLOSECLIENT: {
                    return ClientMaster.CloseResponseM2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_REGISTER: {
                    return ClientMaster.RegisterResponseM2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_HEARTBEAT: {
                    return ClientMaster.HeartResponseM2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_BROKER_CLOSECLIENT: {
                    return ClientMaster.CloseResponseM2B.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER_V2: {
                    return ClientMaster.RegisterResponseM2CV2.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT_V2: {
                    return ClientMaster.HeartResponseM2CV2.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_MASTER_CONSUMER_GET_PART_META: {
                    return ClientMaster.GetPartMetaResponseM2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_SENDMESSAGE: {
                    return ClientBroker.SendMessageResponseB2P.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_REGISTER: {
                    return ClientBroker.RegisterResponseB2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_HEARTBEAT: {
                    return ClientBroker.HeartBeatResponseB2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_GETMESSAGE: {
                    return ClientBroker.GetMessageResponseB2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_COMMIT: {
                    return ClientBroker.CommitOffsetResponseB2C.parseFrom(bytes);
                }
                case RpcConstants.RPC_MSG_BROKER_CONSUMER_CLOSE:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_CLOSE:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_REGISTER:
                case RpcConstants.RPC_MSG_BROKER_PRODUCER_HEARTBEAT:
                default: {
                    throw new Exception(new StringBuilder(256)
                            .append("Unsupported method ID :")
                            .append(methodId).toString());
                }
            }
        }
    }

    public static int getMethIdByName(String methodName) throws Exception {
        Integer methodId = rpcMethodMap.get(methodName);
        if (methodId == null) {
            throw new Exception(new StringBuilder(256)
                    .append("Unsupported method name ").append(methodName).toString());
        } else {
            return methodId;
        }
    }

    public static int getServiceIdByServiceName(String serviceName) throws Exception {
        Integer serviceId = rpcServiceMap.get(serviceName);
        if (serviceId == null) {
            throw new Exception(new StringBuilder(256)
                    .append("Unsupported service name ").append(serviceName).toString());
        } else {
            return serviceId;
        }
    }

    /**
     * Valid service type and method parameters
     *
     * @param serviceId     the service id
     * @param methodId      the method id
     * @param sBuilder      the string buffer
     * @return              whether is valid content.
     * @throws Exception    the exception while processing.
     */
    public static boolean isValidServiceTypeAndMethod(int serviceId,
                                                      int methodId,
                                                      final StringBuilder sBuilder) throws Exception {
        // #lizard forgives
        //First confirm the valid data according to the service ID according to the service ID.
        switch (serviceId) {
            case RpcConstants.RPC_SERVICE_TYPE_MASTER_SERVICE: {
                switch (methodId) {
                    case RpcConstants.RPC_MSG_MASTER_PRODUCER_REGISTER:
                    case RpcConstants.RPC_MSG_MASTER_PRODUCER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_MASTER_PRODUCER_CLOSECLIENT:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_CLOSECLIENT:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_REGISTER_V2:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_HEARTBEAT_V2:
                    case RpcConstants.RPC_MSG_MASTER_CONSUMER_GET_PART_META:
                    case RpcConstants.RPC_MSG_MASTER_BROKER_REGISTER:
                    case RpcConstants.RPC_MSG_MASTER_BROKER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_MASTER_BROKER_CLOSECLIENT: {
                        return true;
                    }
                    default: {
                        if (sBuilder != null) {
                            sBuilder.append("Unsupported method ").append(methodId)
                                    .append("in service type ").append(serviceId)
                                    .append("!");
                        }
                        return false;
                    }
                }
            }
            case RpcConstants.RPC_SERVICE_TYPE_BROKER_READ_SERVICE: {
                switch (methodId) {
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_REGISTER:
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_SENDMESSAGE:
                    case RpcConstants.RPC_MSG_BROKER_CONSUMER_REGISTER:
                    case RpcConstants.RPC_MSG_BROKER_CONSUMER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_BROKER_CONSUMER_GETMESSAGE:
                    case RpcConstants.RPC_MSG_BROKER_CONSUMER_COMMIT:
                    case RpcConstants.RPC_MSG_BROKER_CONSUMER_CLOSE: {
                        return true;
                    }
                    default: {
                        if (sBuilder != null) {
                            sBuilder.append("Unsupported method ").append(methodId)
                                    .append("in service type ").append(serviceId)
                                    .append("!");
                        }
                        return false;
                    }
                }
            }
            case RpcConstants.RPC_SERVICE_TYPE_BROKER_WRITE_SERVICE: {
                switch (methodId) {
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_REGISTER:
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_HEARTBEAT:
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_SENDMESSAGE:
                    case RpcConstants.RPC_MSG_BROKER_PRODUCER_CLOSE: {
                        return true;
                    }
                    default: {
                        if (sBuilder != null) {
                            sBuilder.append("Unsupported method ").append(methodId)
                                    .append("in service type ").append(serviceId)
                                    .append("!");
                        }
                        return false;
                    }
                }
            }
            default: {
                if (sBuilder != null) {
                    sBuilder.append("Service Type is invalid!");
                }
                return false;
            }
        }
    }
}
