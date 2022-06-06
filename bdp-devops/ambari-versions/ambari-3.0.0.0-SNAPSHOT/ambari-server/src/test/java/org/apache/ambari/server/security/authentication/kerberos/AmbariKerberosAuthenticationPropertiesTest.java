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

package org.apache.ambari.server.security.authentication.kerberos;

import org.junit.Assert;
import org.junit.Test;

public class AmbariKerberosAuthenticationPropertiesTest {
  @Test
  public void testKerberosAuthenticationEnabled() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    properties.setKerberosAuthenticationEnabled(true);
    Assert.assertEquals(true, properties.isKerberosAuthenticationEnabled());

    properties.setKerberosAuthenticationEnabled(false);
    Assert.assertEquals(false, properties.isKerberosAuthenticationEnabled());
  }

  @Test
  public void testSpnegoPrincipalName() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    properties.setSpnegoPrincipalName("HTTP/_HOST@EXAMPLE.COM");
    Assert.assertEquals("HTTP/_HOST@EXAMPLE.COM", properties.getSpnegoPrincipalName());

    properties.setSpnegoPrincipalName("something else");
    Assert.assertEquals("something else", properties.getSpnegoPrincipalName());
  }

  @Test
  public void testSpnegoKeytabFilePath() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    properties.setSpnegoKeytabFilePath("/etc/security/keytabs/spnego.service.keytab");
    Assert.assertEquals("/etc/security/keytabs/spnego.service.keytab", properties.getSpnegoKeytabFilePath());

    properties.setSpnegoKeytabFilePath("something else");
    Assert.assertEquals("something else", properties.getSpnegoKeytabFilePath());
  }

  @Test
  public void testAuthToLocalRules() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    properties.setAuthToLocalRules("RULE:[1:$1@$0](user1@EXAMPLE.COM)s/.*/user2/\\nDEFAULT");
    Assert.assertEquals("RULE:[1:$1@$0](user1@EXAMPLE.COM)s/.*/user2/\\nDEFAULT", properties.getAuthToLocalRules());

    properties.setAuthToLocalRules("something else");
    Assert.assertEquals("something else", properties.getAuthToLocalRules());
  }
}