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

import java.util.Map;

public class KeytabJaasConf extends AbstractJaasConf {
    private final String keytabFilePath;

    public KeytabJaasConf(final String userPrincipalName, final String keytabFilePath, final boolean enableDebugLogs) {
        super(userPrincipalName, enableDebugLogs);
        this.keytabFilePath = keytabFilePath;
    }

    public void addOptions(final Map<String, String> options) {
        options.put("useKeyTab", Boolean.TRUE.toString());
        options.put("keyTab", keytabFilePath);
        options.put("doNotPrompt", Boolean.TRUE.toString());
    }

}
