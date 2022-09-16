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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corerpc.client.Callback;
import org.apache.inlong.tubemq.corerpc.client.Client;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.codec.PbEnDecoder;
import org.apache.inlong.tubemq.corerpc.exception.StandbyException;
import org.apache.inlong.tubemq.corerpc.protocol.RpcProtocol;
import org.apache.inlong.tubemq.corerpc.utils.MixUtils;

public class RpcServiceFailoverInvoker extends AbstractServiceInvoker {

    private final AtomicInteger retryCounter = new AtomicInteger(0);
    private MasterInfo masterInfo;
    private Client currentClient;
    private int masterNodeCnt;

    public RpcServiceFailoverInvoker(ClientFactory clientFactory, Class serviceClass,
                                     RpcConfig conf, MasterInfo masterInfo) {
        super(clientFactory, serviceClass, conf);
        this.masterInfo = masterInfo;
        this.masterNodeCnt = masterInfo.getNodeHostPortList().size();
        getNextClient(false);
    }

    @Override
    public Object callMethod(String targetInterface, String method,
                             Object arg, Callback callback) throws Throwable {
        if (currentClient == null
                || !currentClient.isReady()) {
            getNextClient(false);
        }
        int currentCounter = retryCounter.get();
        RequestWrapper requestWrapper =
                new RequestWrapper(PbEnDecoder.getServiceIdByServiceName(targetInterface),
                        RpcProtocol.RPC_PROTOCOL_VERSION,
                        RpcConstants.RPC_FLAG_MSG_TYPE_REQUEST,
                        requestTimeout);
        requestWrapper.setMethodId(PbEnDecoder.getMethIdByName(method));
        requestWrapper.setRequestData(arg);
        Throwable t = null;
        for (int i = 0; i < masterNodeCnt; i++) {
            if (currentClient != null) {
                try {
                    ResponseWrapper responseWrapper =
                            currentClient.call(requestWrapper, callback,
                                    requestTimeout, TimeUnit.MILLISECONDS);
                    if (responseWrapper != null) {
                        if (responseWrapper.isSuccess()) {
                            return responseWrapper.getResponseData();
                        } else {
                            Throwable remote =
                                    MixUtils.unwrapException(new StringBuilder(512)
                                            .append(responseWrapper.getErrMsg()).append("#")
                                            .append(responseWrapper.getStackTrace()).toString());
                            if ((IOException.class.isAssignableFrom(remote.getClass()))
                                    || (StandbyException.class.isAssignableFrom(remote.getClass()))) {
                                if (currentCounter == retryCounter.get()) {
                                    getNextClient(true);
                                    currentCounter++;
                                }
                                t = remote;
                            } else {
                                throw remote;
                            }
                        }
                    } else {
                        break;
                    }
                } catch (Throwable e) {
                    // If the call throws an exception and the master address is not polled, we need to try again.
                    if (currentCounter == retryCounter.get()) {
                        getNextClient(true);
                        currentCounter++;
                    }
                    t = e;
                }
            } else {
                int index = (currentCounter & Integer.MAX_VALUE) % masterNodeCnt;
                t = new IOException(new StringBuilder(512).append("Connect server ")
                        .append(masterInfo.getNodeHostPortList().get(index)).append(" failure!").toString());
                if (currentCounter == retryCounter.get()) {
                    getNextClient(false);
                    currentCounter++;
                }
            }
        }
        if (t != null) {
            throw t;
        }
        return null;
    }

    private synchronized Client getNextClient(boolean forceChange) {
        // forceChange : force to create a new connection to the master
        if (currentClient != null) {
            if (forceChange || !currentClient.isReady()) {
                currentClient.close();
                currentClient = null;
            }
        }
        if (currentClient == null) {
            Client client = null;
            int retryTimes = masterNodeCnt;
            List<String> addressList = masterInfo.getNodeHostPortList();
            while (client == null || !client.isReady()) {
                String nodeKey =
                        addressList.get((retryCounter.getAndIncrement() & Integer.MAX_VALUE) % masterNodeCnt);
                NodeAddrInfo nodeAddrInfo = masterInfo.getAddrMap4Failover().get(nodeKey);
                try {
                    client = clientFactory.getClient(nodeAddrInfo, conf);
                } catch (Throwable e) {
                    //
                }
                if (retryTimes-- == 0) {
                    break;
                }
            }
            if (client != null) {
                currentClient = client;
            }
            return client;
        } else {
            return currentClient;
        }
    }
}
