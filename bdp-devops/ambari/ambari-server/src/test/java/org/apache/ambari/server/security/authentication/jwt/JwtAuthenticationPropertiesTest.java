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

package org.apache.ambari.server.security.authentication.jwt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.junit.Test;

public class JwtAuthenticationPropertiesTest {

  @Test
  public void testSetNullAudiences() {
    assertNull(new JwtAuthenticationProperties(Collections.emptyMap()).getAudiences());
  }

  @Test
  public void testSetEmptyAudiences() {
    final Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(AmbariServerConfigurationKey.SSO_JWT_AUDIENCES.key(), "");
    assertNull(new JwtAuthenticationProperties(configurationMap).getAudiences());
  }

  @Test
  public void testSetValidAudiences() {
    final String[] expectedAudiences = {"first", "second", "third"};
    final Map<String, String> configurationMap = new HashMap<>();
    configurationMap.put(AmbariServerConfigurationKey.SSO_JWT_AUDIENCES.key(), "first,second,third");
    final JwtAuthenticationProperties jwtAuthenticationProperties = new JwtAuthenticationProperties(configurationMap);
    assertNotNull(jwtAuthenticationProperties.getAudiences());
    assertArrayEquals(expectedAudiences, jwtAuthenticationProperties.getAudiences().toArray(new String[]{}));
  }
}
