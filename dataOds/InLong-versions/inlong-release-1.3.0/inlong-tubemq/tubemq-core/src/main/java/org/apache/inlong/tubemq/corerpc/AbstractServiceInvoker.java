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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.apache.inlong.tubemq.corerpc.client.Callback;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.utils.MixUtils;

public abstract class AbstractServiceInvoker implements InvocationHandler {

    protected ClientFactory clientFactory;
    protected Class serviceClass;
    protected RpcConfig conf;
    protected int requestTimeout;

    protected AbstractServiceInvoker(ClientFactory clientFactory,
                                     Class serviceClass,
                                     RpcConfig conf) {
        this.clientFactory = clientFactory;
        this.serviceClass = serviceClass;
        this.conf = conf;
        this.requestTimeout = conf.getInt(RpcConstants.REQUEST_TIMEOUT, 10000);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ((args == null) || (args.length == 0)) {
            throw new RuntimeException("Call method without args!");
        }
        String finalInterfaceName = method.getDeclaringClass().getName();
        String finalMethodName = method.getName();
        Object finalArg = args[0];
        Callback callback = null;
        if (args[args.length - 1] instanceof Callback) {
            finalInterfaceName =
                    ((Class) method.getDeclaringClass().getGenericInterfaces()[0]).getName();
            callback = new RpcResponseCallback((Callback<?>) args[args.length - 1]);
        }
        return callMethod(finalInterfaceName, finalMethodName, finalArg, callback);
    }

    public abstract Object callMethod(String targetInterface, String method,
                                      Object arg, Callback callback) throws Throwable;

    public void destroy() {
        // client.close();
    }

    private static class RpcResponseCallback implements Callback {

        private Callback chainedCallback;

        public RpcResponseCallback(Callback chainedCallback) {
            this.chainedCallback = chainedCallback;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleResult(Object result) {
            if (result instanceof ResponseWrapper) {
                ResponseWrapper wrapper = (ResponseWrapper) result;
                if (wrapper.isSuccess()) {
                    chainedCallback.handleResult(wrapper.getResponseData());
                } else {
                    String errMsg = new StringBuilder(512)
                            .append(wrapper.getErrMsg() == null ? "" : wrapper.getErrMsg())
                            .append("#").append(wrapper.getStackTrace()).toString();
                    handleError(MixUtils.unwrapException(errMsg));
                }
            }
        }

        @Override
        public void handleError(Throwable error) {
            chainedCallback.handleError(error);
        }
    }
}
