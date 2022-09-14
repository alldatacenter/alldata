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

package org.apache.inlong.tubemq.corerpc;

import java.util.concurrent.TimeUnit;
import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corerpc.client.Callback;
import org.apache.inlong.tubemq.corerpc.client.Client;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.codec.PbEnDecoder;
import org.apache.inlong.tubemq.corerpc.exception.NetworkException;
import org.apache.inlong.tubemq.corerpc.exception.OverflowException;
import org.apache.inlong.tubemq.corerpc.protocol.RpcProtocol;
import org.apache.inlong.tubemq.corerpc.utils.MixUtils;

public class RpcServiceInvoker extends AbstractServiceInvoker {
    private NodeAddrInfo targetAddress;

    public RpcServiceInvoker(ClientFactory clientFactory, Class serviceClass,
                             RpcConfig conf, NodeAddrInfo targetAddress) {
        super(clientFactory, serviceClass, conf);
        this.targetAddress = targetAddress;
    }

    public Client getClientOnce() throws Exception {
        return clientFactory.getClient(targetAddress, this.conf);
    }

    @Override
    public Object callMethod(String targetInterface, String method,
                             Object arg, Callback callback) throws Throwable {
        Client client =
                clientFactory.getClient(targetAddress, this.conf);
        if (client == null) {
            throw new NetworkException("Client is null, Channel is not connected!");
        }
        if (!client.isReady()) {
            throw new NetworkException("Channel is not connected!");
        } else {
            if (!client.isWritable()) {
                throw new OverflowException("Channel is not writable, please try later!");
            }
        }
        int requestTimeout =
                this.conf.getInt(RpcConstants.REQUEST_TIMEOUT, 10000);
        RequestWrapper requestWrapper =
                new RequestWrapper(PbEnDecoder.getServiceIdByServiceName(targetInterface),
                        RpcProtocol.RPC_PROTOCOL_VERSION,
                        RpcConstants.RPC_FLAG_MSG_TYPE_REQUEST,
                        requestTimeout);
        requestWrapper.setMethodId(PbEnDecoder.getMethIdByName(method));
        requestWrapper.setRequestData(arg);
        ResponseWrapper responseWrapper =
                client.call(requestWrapper, callback,
                        requestTimeout, TimeUnit.MILLISECONDS);
        if (responseWrapper != null) {
            if (responseWrapper.isSuccess()) {
                return responseWrapper.getResponseData();
            } else {
                throw MixUtils.unwrapException(new StringBuilder(512)
                        .append(responseWrapper.getErrMsg()).append("#")
                        .append(responseWrapper.getStackTrace()).toString());
            }
        }
        return null;
    }
}
