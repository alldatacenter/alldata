/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services;

import java.util.ArrayList;
import java.util.List;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ISecurityKey;

public class DefaultCredentialsProviderChain implements IObsCredentialsProvider {

    private static final ILogger ILOG = LoggerBuilder.getLogger(DefaultCredentialsProviderChain.class);

    private IObsCredentialsProvider lastProvider = null;

    private boolean reused = true;

    private final List<IObsCredentialsProvider> credentialsProviders = new ArrayList<>(2);

    public DefaultCredentialsProviderChain(IObsCredentialsProvider... credentialsProviders) {
        if (credentialsProviders == null || credentialsProviders.length == 0) {
            throw new IllegalArgumentException("No credential providers specified");
        }

        for (IObsCredentialsProvider provider : credentialsProviders) {
            this.credentialsProviders.add(provider);
        }
    }

    public DefaultCredentialsProviderChain(boolean reused, IObsCredentialsProvider... credentialsProviders) {
        this(credentialsProviders);
        this.reused = reused;
    }

    @Override
    public void setSecurityKey(ISecurityKey securityKey) {
        throw new UnsupportedOperationException("OBSCredentialsProviderChain class does not support this method");
    }

    @Override
    public ISecurityKey getSecurityKey() {
        if (reused && lastProvider != null) {
            return this.lastProvider.getSecurityKey();
        }

        for (IObsCredentialsProvider provider : credentialsProviders) {
            try {
                ISecurityKey credentials = provider.getSecurityKey();

                if (credentials.getAccessKey() != null && credentials.getSecretKey() != null) {
                    ILOG.debug("Loading credentials from " + provider.toString());

                    this.lastProvider = provider;
                    return lastProvider.getSecurityKey();
                }
            } catch (Exception e) {
                if (ILOG.isWarnEnabled()) {
                    ILOG.warn("Loading credentials from " + provider.toString(), e);
                }
            }
        }

        if (ILOG.isErrorEnabled()) {
            ILOG.error("No credential providers specified");
        }

        this.lastProvider = null;
        throw new ObsException("No credential providers specified");
    }

    public void refresh() {
        this.lastProvider = null;
    }
}
