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

package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;

import org.junit.Test;

/**
 * RequestBody unit tests.
 */
public class RequestBodyTest {

  @Test
  public void testSetGetQueryString() {
    RequestBody body = new RequestBody();
    assertNull(body.getQueryString());
    body.setQueryString("foo=bar");
    assertEquals("foo=bar", body.getQueryString());
  }

  @Test
  public void testSetGetPartialResponseFields() {
    RequestBody body = new RequestBody();
    assertNull(body.getPartialResponseFields());
    body.setPartialResponseFields("foo,bar");
    assertEquals("foo,bar", body.getPartialResponseFields());
  }

  @Test
  public void testAddGetPropertySets() {
    RequestBody body = new RequestBody();
    assertEquals(0, body.getNamedPropertySets().size());
    NamedPropertySet ps = new NamedPropertySet("foo", new HashMap<>());
    body.addPropertySet(ps);
    assertEquals(1, body.getNamedPropertySets().size());
    assertSame(ps, body.getNamedPropertySets().iterator().next());
  }

  @Test
  public void testSetGetBody() {
    RequestBody body = new RequestBody();
    assertNull(body.getBody());
    body.setBody("{\"foo\" : \"value\" }");
    assertEquals("{\"foo\" : \"value\" }", body.getBody());
  }
}
