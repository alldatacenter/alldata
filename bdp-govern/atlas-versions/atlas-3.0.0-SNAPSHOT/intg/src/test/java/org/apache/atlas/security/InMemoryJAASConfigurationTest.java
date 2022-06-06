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

import org.apache.hadoop.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;


//Unstable test. Disabling
@Test(enabled=false)
public class InMemoryJAASConfigurationTest {

    private static final String ATLAS_JAAS_PROP_FILE = "atlas-jaas.properties";

    @BeforeClass
    protected void setUp() throws Exception {
            InMemoryJAASConfiguration.init(ATLAS_JAAS_PROP_FILE);
    }

    @Test(enabled=false)
    public void testGetAppConfigurationEntryStringForKafkaClient() {
        AppConfigurationEntry[] entries =
                Configuration.getConfiguration().getAppConfigurationEntry("KafkaClient");
        Assert.assertNotNull(entries);
        Assert.assertEquals(1, entries.length);
        String principal = (String) entries[0].getOptions().get("principal");
        Assert.assertNotNull(principal);
        String[] components = principal.split("[/@]");
        Assert.assertEquals(3, components.length);
        Assert.assertEquals(false, StringUtils.equalsIgnoreCase(components[1], "_HOST"));

    }

    @Test(enabled=false)
    public void testGetAppConfigurationEntryStringForMyClient() {
        AppConfigurationEntry[] entries =
                Configuration.getConfiguration().getAppConfigurationEntry("myClient");
        Assert.assertNotNull(entries);
        Assert.assertEquals(2, entries.length);
        String principal = (String) entries[0].getOptions().get("principal");
        Assert.assertNotNull(principal);
        String[] components = principal.split("[/@]");
        Assert.assertEquals(3, components.length);
        Assert.assertEquals(true, StringUtils.equalsIgnoreCase(components[1], "abcd"));

        principal = (String) entries[1].getOptions().get("principal");
        Assert.assertNotNull(principal);
        components = principal.split("[/@]");
        Assert.assertEquals(2, components.length);
    }

    @Test(enabled=false)
    public void testGetAppConfigurationEntryStringForUnknownClient() {
        AppConfigurationEntry[] entries =
                Configuration.getConfiguration().getAppConfigurationEntry("UnknownClient");
        Assert.assertNull(entries);
    }

}

