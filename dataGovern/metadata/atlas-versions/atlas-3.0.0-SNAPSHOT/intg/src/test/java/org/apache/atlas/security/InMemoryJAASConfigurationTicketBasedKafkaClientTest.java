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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;


@Test
public class InMemoryJAASConfigurationTicketBasedKafkaClientTest {

    private static final String ATLAS_JAAS_PROP_FILE = "atlas-jaas.properties";

    @BeforeClass
    public void setUp() throws Exception {
            InMemoryJAASConfiguration.init(ATLAS_JAAS_PROP_FILE);
            InMemoryJAASConfiguration.setConfigSectionRedirect("KafkaClient", "ticketBased-KafkaClient");
    }

    @Test
    public void testGetAppConfigurationEntryStringForticketBasedKafkaClient() {

        AppConfigurationEntry[] entries =
                Configuration.getConfiguration().getAppConfigurationEntry("KafkaClient");
        Assert.assertNotNull(entries);
        Assert.assertEquals((String) entries[0].getOptions().get("useTicketCache"), "true");
    }


}

