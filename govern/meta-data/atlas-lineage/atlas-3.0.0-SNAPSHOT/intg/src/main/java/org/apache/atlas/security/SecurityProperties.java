/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.security;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public final class SecurityProperties {

    private SecurityProperties() {
    }

    public static final String TLS_ENABLED = "atlas.enableTLS";
    public static final String KEYSTORE_FILE_KEY = "keystore.file";
    public static final String DEFAULT_KEYSTORE_FILE_LOCATION = "target/atlas.keystore";
    public static final String KEYSTORE_PASSWORD_KEY = "keystore.password";
    public static final String KEYSTORE_TYPE = "keystore.type";
    public static final String TRUSTSTORE_FILE_KEY = "truststore.file";
    public static final String DEFATULT_TRUSTORE_FILE_LOCATION = "target/atlas.keystore";
    public static final String TRUSTSTORE_PASSWORD_KEY = "truststore.password";
    public static final String TRUSTSTORE_TYPE = "truststore.type";
    public static final String SERVER_CERT_PASSWORD_KEY = "password";
    public static final String CLIENT_AUTH_KEY = "client.auth.enabled";
    public static final String CERT_STORES_CREDENTIAL_PROVIDER_PATH = "cert.stores.credential.provider.path";
    public static final String HADOOP_SECURITY_CREDENTIAL_PROVIDER_PATH = "hadoop.security.credential.provider.path";
    public static final String SSL_CLIENT_PROPERTIES = "ssl-client.xml";
    public static final String BIND_ADDRESS = "atlas.server.bind.address";
    public static final String ATLAS_SSL_EXCLUDE_CIPHER_SUITES = "atlas.ssl.exclude.cipher.suites";
    public static final List<String> DEFAULT_CIPHER_SUITES = Arrays.asList(
            ".*NULL.*", ".*RC4.*", ".*MD5.*", ".*DES.*", ".*DSS.*");
    public static final String ATLAS_SSL_EXCLUDE_PROTOCOLS = "atlas.ssl.exclude.protocols";
    public static final String[] DEFAULT_EXCLUDE_PROTOCOLS = new String[] { "TLSv1", "TLSv1.1" };

}
