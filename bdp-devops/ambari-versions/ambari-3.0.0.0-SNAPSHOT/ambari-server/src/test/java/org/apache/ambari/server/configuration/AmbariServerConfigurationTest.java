/*
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

package org.apache.ambari.server.configuration;

import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.LDAP_ENABLED;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AmbariServerConfigurationTest {

  @Test
  public void testGetValue() {
    final Map<String, String> configurationMap = Collections.singletonMap(TPROXY_AUTHENTICATION_ENABLED.key(), "true");

    final AmbariServerConfiguration ambariServerConfiguration = new AmbariServerConfiguration(configurationMap) {
      @Override
      protected AmbariServerConfigurationCategory getCategory() {
        return null;
      }
    };

    // Get the value specified in the configuration map
    Assert.assertEquals("true", ambariServerConfiguration.getValue(TPROXY_AUTHENTICATION_ENABLED, configurationMap));

    // Get the default value
    Assert.assertEquals("false", LDAP_ENABLED.getDefaultValue());
    Assert.assertEquals(LDAP_ENABLED.getDefaultValue(), ambariServerConfiguration.getValue(LDAP_ENABLED, configurationMap));

    // Handle a nulls
    Assert.assertEquals("defaultValue", ambariServerConfiguration.getValue(null, configurationMap, "defaultValue"));
    Assert.assertEquals("defaultValue", ambariServerConfiguration.getValue("property.name", null, "defaultValue"));
    Assert.assertNull(ambariServerConfiguration.getValue("property.name", Collections.emptyMap(), null));

    // check toMap()
    Assert.assertEquals(configurationMap, ambariServerConfiguration.toMap());
  }
}