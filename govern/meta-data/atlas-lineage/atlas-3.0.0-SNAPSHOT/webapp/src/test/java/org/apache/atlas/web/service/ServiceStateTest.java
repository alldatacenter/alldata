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

package org.apache.atlas.web.service;

import org.apache.atlas.ha.HAConfiguration;
import org.apache.commons.configuration.Configuration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ServiceStateTest {

    @Mock
    private Configuration configuration;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testShouldBeActiveIfHAIsDisabled() {
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(false);

        ServiceState serviceState = new ServiceState(configuration);
        assertEquals(ServiceState.ServiceStateValue.ACTIVE, serviceState.getState());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testShouldDisallowTransitionIfHAIsDisabled() {
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY, false)).thenReturn(false);

        ServiceState serviceState = new ServiceState(configuration);
        serviceState.becomingPassive();
        fail("Should not allow transition");
    }

    @Test
    public void testShouldChangeStateIfHAIsEnabled() {
        when(configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);
        when(configuration.getBoolean(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)).thenReturn(true);

        ServiceState serviceState = new ServiceState(configuration);
        serviceState.becomingPassive();
        assertEquals(ServiceState.ServiceStateValue.BECOMING_PASSIVE, serviceState.getState());
    }
}
