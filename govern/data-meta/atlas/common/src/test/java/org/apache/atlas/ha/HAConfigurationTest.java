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

package org.apache.atlas.ha;

import org.apache.atlas.AtlasConstants;
import org.apache.atlas.security.SecurityProperties;
import org.apache.commons.configuration.Configuration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HAConfigurationTest {

    private static final String[] TEST_ATLAS_SERVER_IDS_HA = new String[] { "id1", "id2" };

    @Mock
    private Configuration configuration;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        System.setProperty(AtlasConstants.SYSTEM_PROPERTY_APP_PORT, AtlasConstants.DEFAULT_APP_PORT_STR);
    }

    @Test
    public void testIsHAEnabledByLegacyConfiguration() {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(Boolean.TRUE);

        boolean isHAEnabled = HAConfiguration.isHAEnabled(configuration);
        assertTrue(isHAEnabled);

        // restore
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(false);
        isHAEnabled = HAConfiguration.isHAEnabled(configuration);
        assertFalse(isHAEnabled);
    }

    @Test
    public void testIsHAEnabledByIds() {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(false);
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(TEST_ATLAS_SERVER_IDS_HA);
        boolean isHAEnabled = HAConfiguration.isHAEnabled(configuration);
        assertTrue(isHAEnabled);

        // restore
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(new String[] { "id1"});
        isHAEnabled = HAConfiguration.isHAEnabled(configuration);
        assertFalse(isHAEnabled);
    }

    @Test
    public void testShouldReturnHTTPSBoundAddress() {
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21443");
        when(configuration.getBoolean(SecurityProperties.TLS_ENABLED)).thenReturn(true);

        String address = HAConfiguration.getBoundAddressForId(configuration, "id1");

        assertEquals(address, "https://127.0.0.1:21443");
    }

    @Test
    public void testShouldReturnListOfAddressesInConfig() {
        when(configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS)).thenReturn(TEST_ATLAS_SERVER_IDS_HA);
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id1")).thenReturn("127.0.0.1:21000");
        when(configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +"id2")).thenReturn("127.0.0.1:31000");

        List<String> serverInstances = HAConfiguration.getServerInstances(configuration);
        assertEquals(serverInstances.size(), 2);
        assertTrue(serverInstances.contains("http://127.0.0.1:21000"));
        assertTrue(serverInstances.contains("http://127.0.0.1:31000"));
    }

    @Test
    public void testShouldGetZookeeperAcl() {
        when(configuration.getString(HAConfiguration.HA_ZOOKEEPER_ACL)).thenReturn("sasl:myclient@EXAMPLE.COM");

        HAConfiguration.ZookeeperProperties zookeeperProperties =
                HAConfiguration.getZookeeperProperties(configuration);
        assertTrue(zookeeperProperties.hasAcl());
    }

    @Test
    public void testShouldGetZookeeperAuth() {
        when(configuration.getString(HAConfiguration.HA_ZOOKEEPER_AUTH)).thenReturn("sasl:myclient@EXAMPLE.COM");

        HAConfiguration.ZookeeperProperties zookeeperProperties =
                HAConfiguration.getZookeeperProperties(configuration);
        assertTrue(zookeeperProperties.hasAuth());
    }
}
