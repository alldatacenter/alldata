/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.schema.registry.client.connection.util;

import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL;
import static org.apache.ranger.plugin.client.HadoopConfigHolder.RANGER_AUTH_TYPE;
import static org.apache.ranger.plugin.client.HadoopConfigHolder.RANGER_LOOKUP_KEYTAB;
import static org.apache.ranger.plugin.client.HadoopConfigHolder.RANGER_LOOKUP_PRINCIPAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SecurityUtilsTest {

    @Test
    public void isHttpsConnection() {
        Map<String, String> conf = new HashMap();
        conf.put(SCHEMA_REGISTRY_URL.name(), "https://dummy:8081");
        assertTrue(SecurityUtils.isHttpsConnection(conf));

        conf = new HashMap();
        conf.put(SCHEMA_REGISTRY_URL.name(), "http://dummy:8081");
        assertFalse(SecurityUtils.isHttpsConnection(conf));
    }

    @Test
    public void createSSLContext() throws Exception {
        String keyStorePath = "keystore.jks";
        String keyStorePassword = "password";
        String keyStoreType = "jks";

        String trustStorePath = "trustsrore.jks";
        String trustStorePassword = "password";
        String trustStoreType = "jks";

        Map<String, String> conf = new HashMap();
        SSLContext sslContext = SecurityUtils.createSSLContext(conf, "TLS");
        assertTrue(sslContext != null);

        conf.put("keyStorePath", keyStorePath);
        conf.put("keyStorePassword", keyStorePassword);
        conf.put("keyStoreType", keyStoreType);

        conf.put("trustStorePath", trustStorePath);
        conf.put("trustStorePassword", trustStorePassword);
        conf.put("trustStoreType", trustStoreType);
        sslContext = SecurityUtils.createSSLContext(conf, "TLS");

        assertTrue(sslContext != null);

    }

    @Test
    public void getJaasConfigForClientPrincipal() {
        Map<String, String> conf = new HashMap();
        assertNull(SecurityUtils.getJaasConfigForClientPrincipal(conf));
        conf.put(RANGER_LOOKUP_KEYTAB, "/tmp/rangerlookup.keytab");
        assertNull(SecurityUtils.getJaasConfigForClientPrincipal(conf));
        conf.put(RANGER_LOOKUP_PRINCIPAL, "rangerlookup");

        String expected = "com.sun.security.auth.module.Krb5LoginModule" +
                " required useTicketCache=false principal=\"rangerlookup\" " +
                "useKeyTab=true keyTab=\"/tmp/rangerlookup.keytab\";";
        String actual = SecurityUtils.getJaasConfigForClientPrincipal(conf);
        assertEquals(actual, expected);
    }


    @Test
    public void isKerberosEnabled() {
        Map<String, String> conf = new HashMap();

        conf.put(RANGER_AUTH_TYPE, "kerberos");
        conf.put("schema-registry.authentication", "kerberos");
        assertTrue(SecurityUtils.isKerberosEnabled(conf));

        conf = new HashMap();
        assertFalse(SecurityUtils.isKerberosEnabled(conf));

        conf = new HashMap();
        conf.put(RANGER_AUTH_TYPE, "kerberos");
        assertFalse(SecurityUtils.isKerberosEnabled(conf));

        conf = new HashMap();
        conf.put("schema-registry.authentication", "kerberos");
        assertFalse(SecurityUtils.isKerberosEnabled(conf));

        conf = new HashMap();
        conf.put(RANGER_AUTH_TYPE, "kerberos");
        conf.put("schema-registry.authentication", "Something wrong");
        assertFalse(SecurityUtils.isKerberosEnabled(conf));

        conf = new HashMap();
        conf.put("schema-registry.authentication", "kerberos");
        conf.put(RANGER_AUTH_TYPE, "Something wrong");
        assertFalse(SecurityUtils.isKerberosEnabled(conf));

    }
}