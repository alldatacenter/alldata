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
package org.apache.ranger.services.nifi.registry.client;

/**
 * Config property names from the NiFi Registry service definition.
 */
public interface NiFiRegistryConfigs {

    String NIFI_REG_URL = "nifi.registry.url";
    String NIFI_REG_AUTHENTICATION_TYPE = "nifi.registry.authentication";

    String NIFI_REG_SSL_KEYSTORE = "nifi.registry.ssl.keystore";
    String NIFI_REG_SSL_KEYSTORE_TYPE = "nifi.registry.ssl.keystoreType";
    String NIFI_REG_SSL_KEYSTORE_PASSWORD = "nifi.registry.ssl.keystorePassword";

    String NIFI_REG_SSL_TRUSTSTORE = "nifi.registry.ssl.truststore";
    String NIFI_REG_SSL_TRUSTSTORE_TYPE = "nifi.registry.ssl.truststoreType";
    String NIFI_REG_SSL_TRUSTSTORE_PASSWORD = "nifi.registry.ssl.truststorePassword";

    String NIFI_REG_SSL_USER_DEFAULT_CONTEXT = "nifi.registry.ssl.use.default.context";

}
