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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.atlas.security.SecurityProperties.CERT_STORES_CREDENTIAL_PROVIDER_PATH;

public class SecurityUtil {

    public static final Logger LOG = LoggerFactory.getLogger(SecurityUtil.class);

    /**
     * Retrieves a password from a configured credential provider or prompts for the password and stores it in the
     * configured credential provider.
     * @param config application configuration
     * @param key the key/alias for the password.
     * @return the password.
     * @throws IOException
     */
    public static String getPassword(org.apache.commons.configuration.Configuration config, String key) throws IOException {
        return getPassword(config, key, CERT_STORES_CREDENTIAL_PROVIDER_PATH);
    }


    /**
     * Retrieves a password from a configured credential provider or prompts for the password and stores it in the
     * configured credential provider.
     *
     * @param config           application configuration
     * @param key              the key/alias for the password.
     * @param pathPropertyName property of path
     * @return the password.
     * @throws IOException
     */
    public static String getPassword(org.apache.commons.configuration.Configuration config, String key, String pathPropertyName) throws IOException {

        String password;

        String provider = config.getString(pathPropertyName);
        if (provider != null) {
            LOG.info("Attempting to retrieve password for key {} from {} configured credential provider path {}", key, pathPropertyName, provider);
            Configuration c = new Configuration();
            c.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, provider);
            CredentialProvider credentialProvider = CredentialProviderFactory.getProviders(c).get(0);
            CredentialProvider.CredentialEntry entry = credentialProvider.getCredentialEntry(key);
            if (entry == null) {
                throw new IOException(String.format("No credential entry found for %s. "
                        + "Please create an entry in the configured credential provider", key));
            } else {
                password = String.valueOf(entry.getCredential());
            }

        } else {
            throw new IOException("No credential provider path " + pathPropertyName + " configured for storage of certificate store passwords");
        }

        return password;
    }


}
