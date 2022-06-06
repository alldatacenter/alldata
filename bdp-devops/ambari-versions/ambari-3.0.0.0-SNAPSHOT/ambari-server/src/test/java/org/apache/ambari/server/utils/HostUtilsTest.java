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

package org.apache.ambari.server.utils;

import org.junit.Test;

import junit.framework.Assert;

public class HostUtilsTest {
  @Test
  public void testIsValidHostname() throws Exception {

    // Valid host names
    Assert.assertTrue(HostUtils.isValidHostname("localhost"));
    Assert.assertTrue(HostUtils.isValidHostname("localhost.localdomain"));
    Assert.assertTrue(HostUtils.isValidHostname("host1.example.com"));
    Assert.assertTrue(HostUtils.isValidHostname("Host1.eXample.coM"));
    Assert.assertTrue(HostUtils.isValidHostname("host-name.example.com"));
    Assert.assertTrue(HostUtils.isValidHostname("123.456.789"));
    Assert.assertTrue(HostUtils.isValidHostname("host-123-name.ex4mpl3.c0m"));

    // Invalid host names
    Assert.assertFalse(HostUtils.isValidHostname("host_name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host;name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host?name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host@name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host=name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host+name.example.com"));
    Assert.assertFalse(HostUtils.isValidHostname("host)name).example.com"));
  }

}