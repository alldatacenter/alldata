/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.security;

public class ProviderCredentialThreadContext {

    private static ThreadLocal<ProviderCredentials> context = new ThreadLocal<ProviderCredentials>();

    private ProviderCredentialThreadContext() {

    }

    private static class ProviderCredentialThreadContextHolder {
        private static ProviderCredentialThreadContext instance = new ProviderCredentialThreadContext();
    }

    public static ProviderCredentialThreadContext getInstance() {
        return ProviderCredentialThreadContextHolder.instance;
    }

    public void setProviderCredentials(ProviderCredentials providerCredentials) {
        context.set(providerCredentials);
    }

    public ProviderCredentials getProviderCredentials() {
        return context.get();
    }

    public void clearProviderCredentials() {
        context.remove();
    }

}
