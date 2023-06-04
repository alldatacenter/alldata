/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.client.entry.security;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.client.entry.Entry;

import org.junit.Assert;
import org.junit.Test;

public class SecurityContextFactoryTest {

  @Test
  public void testInitKerberosCommandArgs() {
    String[] args = new String[] {
      "run",
      "--enable-kerberos",
      "--keytab-path", "test.keytab",
      "--principal", "test_principal",
      "--krb5-conf-path", "test_krb5.conf"
    };
    BaseCommandArgs commandArgs = Entry.loadCommandArguments(args);
    Assert.assertTrue(commandArgs.isEnableKerberos());
    Assert.assertEquals("test.keytab", commandArgs.getKeytabPath());
    Assert.assertEquals("test_principal", commandArgs.getPrincipal());
    Assert.assertEquals("test_krb5.conf", commandArgs.getKrb5ConfPath());
  }
}