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

package org.apache.ambari.server.serveraction.kerberos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DeconstructedPrincipalTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNullPrincipal() throws Exception {
    DeconstructedPrincipal.valueOf(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyPrincipal() throws Exception {
    DeconstructedPrincipal.valueOf("", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPrincipal() throws Exception {
    DeconstructedPrincipal.valueOf("/invalid", null);
  }

  @Test
  public void testPrimary() throws Exception {
    DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf("primary", "REALM");

    assertNotNull(deconstructedPrincipal);
    assertEquals("primary", deconstructedPrincipal.getPrimary());
    assertNull(deconstructedPrincipal.getInstance());
    assertEquals("REALM", deconstructedPrincipal.getRealm());
    assertEquals("primary", deconstructedPrincipal.getPrincipalName());
    assertEquals("primary@REALM", deconstructedPrincipal.getNormalizedPrincipal());
  }

  @Test
  public void testPrimaryRealm() throws Exception {
    DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf("primary@MYREALM", "REALM");

    assertNotNull(deconstructedPrincipal);
    assertEquals("primary", deconstructedPrincipal.getPrimary());
    assertNull(deconstructedPrincipal.getInstance());
    assertEquals("MYREALM", deconstructedPrincipal.getRealm());
    assertEquals("primary", deconstructedPrincipal.getPrincipalName());
    assertEquals("primary@MYREALM", deconstructedPrincipal.getNormalizedPrincipal());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInstance() throws Exception {
    DeconstructedPrincipal.valueOf("/instance", "REALM");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInstanceRealm() throws Exception {
    DeconstructedPrincipal.valueOf("/instance@MYREALM", "REALM");
  }

  @Test
  public void testPrimaryInstance() throws Exception {
    DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf("primary/instance", "REALM");

    assertNotNull(deconstructedPrincipal);
    assertEquals("primary", deconstructedPrincipal.getPrimary());
    assertEquals("instance", deconstructedPrincipal.getInstance());
    assertEquals("instance", deconstructedPrincipal.getInstance());
    assertEquals("REALM", deconstructedPrincipal.getRealm());
    assertEquals("primary/instance", deconstructedPrincipal.getPrincipalName());
    assertEquals("primary/instance@REALM", deconstructedPrincipal.getNormalizedPrincipal());
  }

  @Test
  public void testPrimaryInstanceRealm() throws Exception {
    DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf("primary/instance@MYREALM", "REALM");

    assertNotNull(deconstructedPrincipal);
    assertEquals("primary", deconstructedPrincipal.getPrimary());
    assertEquals("instance", deconstructedPrincipal.getInstance());
    assertEquals("MYREALM", deconstructedPrincipal.getRealm());
    assertEquals("primary/instance", deconstructedPrincipal.getPrincipalName());
    assertEquals("primary/instance@MYREALM", deconstructedPrincipal.getNormalizedPrincipal());
  }

  @Test
  public void testOddCharacters() throws Exception {
    DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf("p_ri.ma-ry/i.n_s-tance@M_Y-REALM.COM", "REALM");

    assertNotNull(deconstructedPrincipal);
    assertEquals("p_ri.ma-ry", deconstructedPrincipal.getPrimary());
    assertEquals("i.n_s-tance", deconstructedPrincipal.getInstance());
    assertEquals("M_Y-REALM.COM", deconstructedPrincipal.getRealm());
    assertEquals("p_ri.ma-ry/i.n_s-tance", deconstructedPrincipal.getPrincipalName());
    assertEquals("p_ri.ma-ry/i.n_s-tance@M_Y-REALM.COM", deconstructedPrincipal.getNormalizedPrincipal());
  }

}