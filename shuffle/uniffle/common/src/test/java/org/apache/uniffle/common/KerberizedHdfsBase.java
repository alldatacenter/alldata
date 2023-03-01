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

package org.apache.uniffle.common;

import org.junit.jupiter.api.AfterAll;

import org.apache.uniffle.common.security.HadoopSecurityContext;
import org.apache.uniffle.common.security.NoOpSecurityContext;
import org.apache.uniffle.common.security.SecurityConfig;
import org.apache.uniffle.common.security.SecurityContextFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KerberizedHdfsBase {
  protected static KerberizedHdfs kerberizedHdfs;
  protected static Class<?> testRunner = KerberizedHdfsBase.class;

  public static void init() throws Exception {
    kerberizedHdfs = new KerberizedHdfs();
    kerberizedHdfs.setTestRunner(testRunner);
    kerberizedHdfs.setup();
  }

  @AfterAll
  public static void clear() throws Exception {
    kerberizedHdfs.tearDown();
    kerberizedHdfs = null;
  }

  public static void initHadoopSecurityContext() throws Exception {
    // init the security context
    SecurityConfig securityConfig = SecurityConfig
        .newBuilder()
        .keytabFilePath(kerberizedHdfs.getHdfsKeytab())
        .principal(kerberizedHdfs.getHdfsPrincipal())
        .reloginIntervalSec(1000)
        .build();
    SecurityContextFactory.get().init(securityConfig);

    assertEquals(HadoopSecurityContext.class, SecurityContextFactory.get().getSecurityContext().getClass());
  }

  public static void removeHadoopSecurityContext() throws Exception {
    SecurityContextFactory.get().init(null);
    assertEquals(NoOpSecurityContext.class, SecurityContextFactory.get().getSecurityContext().getClass());
  }
}

