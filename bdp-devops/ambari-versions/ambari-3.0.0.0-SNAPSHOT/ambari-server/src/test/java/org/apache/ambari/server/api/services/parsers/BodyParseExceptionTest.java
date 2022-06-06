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

package org.apache.ambari.server.api.services.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * BodyParseException unit tests.
 */
public class BodyParseExceptionTest {

  @Test
  public void testCreateFromString() {
    String msg = "some msg";
    BodyParseException e = new BodyParseException(msg);

    assertEquals(msg, e.getMessage());
  }

  @Test
  public void testCreateFromException() {
    Exception e = new Exception("test error msg");
    BodyParseException bpe = new BodyParseException(e);

    assertEquals("Invalid Request: Malformed Request Body.  An exception occurred parsing the request body: " +
        e.getMessage(), bpe.getMessage());
  }
}
