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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.credutils.kerberos;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJaasConf extends Configuration {
    private final String userPrincipalName;
    private final boolean enableDebugLogs;

    public AbstractJaasConf(final String userPrincipalName, final boolean enableDebugLogs) {
        this.userPrincipalName = userPrincipalName;
        this.enableDebugLogs = enableDebugLogs;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
        final Map<String, String> options = new HashMap<>();
        options.put("principal", userPrincipalName);
        options.put("isInitiator", Boolean.TRUE.toString());
        options.put("storeKey", Boolean.TRUE.toString());
        options.put("debug", Boolean.toString(enableDebugLogs));
        addOptions(options);
        return new AppConfigurationEntry[] { new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.unmodifiableMap(options)) };
    }

    abstract void addOptions(Map<String, String> options);
}
