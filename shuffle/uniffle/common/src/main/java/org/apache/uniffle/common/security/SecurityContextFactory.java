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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityContextFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityContextFactory.class);

  private SecurityContext securityContext = new NoOpSecurityContext();

  static class LazyHolder {
    static final SecurityContextFactory SECURITY_CONTEXT_FACTORY = new SecurityContextFactory();
  }

  public static SecurityContextFactory get() {
    return LazyHolder.SECURITY_CONTEXT_FACTORY;
  }

  public void init(SecurityConfig securityConfig) throws Exception {
    if (securityConfig == null) {
      this.securityContext = new NoOpSecurityContext();
      return;
    }

    this.securityContext = new HadoopSecurityContext(
        securityConfig.getKrb5ConfPath(),
        securityConfig.getKeytabFilePath(),
        securityConfig.getPrincipal(),
        securityConfig.getReloginIntervalSec()
    );
    LOGGER.info("Initialized security context: {}", securityContext.getClass().getSimpleName());
  }

  public SecurityContext getSecurityContext() {
    if (securityContext == null) {
      throw new RuntimeException("No initialized security context.");
    }
    return securityContext;
  }
}
