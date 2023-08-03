/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.KerberizedHdfsBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SecurityContextFactoryTest extends KerberizedHdfsBase {

  @BeforeAll
  public static void beforeAll() throws Exception {
    testRunner = SecurityContextFactoryTest.class;
    KerberizedHdfsBase.init();
  }

  @AfterEach
  public void afterEach() throws Exception {
    SecurityContextFactory.get().init(null);
  }

  /**
   * It should return the {@link NoOpSecurityContext} when not initializing securityContext
   */
  @Test
  public void testDefaultSecurityContext() throws Exception {
    SecurityContext securityContext = SecurityContextFactory.get().getSecurityContext();
    assertEquals(NoOpSecurityContext.class, securityContext.getClass());

    // init with null config, should return NoOpSecurityContext.
    SecurityContextFactory.get().init(null);
    securityContext = SecurityContextFactory.get().getSecurityContext();
    assertEquals(NoOpSecurityContext.class, securityContext.getClass());
  }

  @Test
  public void testCreateHadoopSecurityContext() throws Exception {
    // case1: lack some config, should throw exception
    final SecurityConfig securityConfig = SecurityConfig
        .newBuilder()
        .keytabFilePath("")
        .build();
    try {
      SecurityContextFactory.get().init(securityConfig);
      fail();
    } catch (Exception e) {
      // ignore
    }

    // case2: create the correct hadoop security context
    final SecurityConfig correctConfig = SecurityConfig
        .newBuilder()
        .keytabFilePath(kerberizedHdfs.getHdfsKeytab())
        .principal(kerberizedHdfs.getHdfsPrincipal())
        .reloginIntervalSec(60)
        .build();
    SecurityContextFactory.get().init(correctConfig);
    SecurityContext securityContext = SecurityContextFactory.get().getSecurityContext();
    assertEquals(HadoopSecurityContext.class, securityContext.getClass());
    securityContext.close();
  }
}
