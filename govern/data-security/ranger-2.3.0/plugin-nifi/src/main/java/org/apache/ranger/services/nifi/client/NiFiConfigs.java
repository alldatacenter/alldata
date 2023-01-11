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
package org.apache.ranger.services.nifi.client;

/**
 * Config property names from the NiFi service definition.
 */
public interface NiFiConfigs {

    String NIFI_URL = "nifi.url";
    String NIFI_AUTHENTICATION_TYPE = "nifi.authentication";

    String NIFI_SSL_KEYSTORE = "nifi.ssl.keystore";
    String NIFI_SSL_KEYSTORE_TYPE = "nifi.ssl.keystoreType";
    String NIFI_SSL_KEYSTORE_PASSWORD = "nifi.ssl.keystorePassword";

    String NIFI_SSL_TRUSTSTORE = "nifi.ssl.truststore";
    String NIFI_SSL_TRUSTSTORE_TYPE = "nifi.ssl.truststoreType";
    String NIFI_SSL_TRUSTSTORE_PASSWORD = "nifi.ssl.truststorePassword";

    String NIFI_SSL_USER_DEFAULT_CONTEXT = "nifi.ssl.use.default.context";

}
