/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.rpc.user.security;

import static org.apache.drill.exec.ExecConstants.USER_AUTHENTICATOR_IMPL;

import java.lang.reflect.Constructor;
import java.util.Collection;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.exception.DrillbitStartupException;

import org.apache.drill.shaded.guava.com.google.common.base.Strings;

/**
 * Factory class which provides {@link UserAuthenticator} implementation based on the BOOT options.
 */
public class UserAuthenticatorFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserAuthenticatorFactory.class);

  /**
   * Create a {@link UserAuthenticator} implementation based on BOOT settings in
   * given <i>drillConfig</i>.
   *
   * @param config DrillConfig containing BOOT options.
   * @return Initialized {@link UserAuthenticator} implementation instance.
   *         It is responsibility of the caller to close the authenticator when no longer needed.
   *
   * @throws DrillbitStartupException when no implementation found for given BOOT options.
   */
  public static UserAuthenticator createAuthenticator(final DrillConfig config, ScanResult scan)
      throws DrillbitStartupException {

    if(!config.hasPath(USER_AUTHENTICATOR_IMPL)) {
      throw new DrillbitStartupException(String.format("BOOT option '%s' is missing in config.",
          USER_AUTHENTICATOR_IMPL));
    }

    final String authImplConfigured = config.getString(USER_AUTHENTICATOR_IMPL);

    if (Strings.isNullOrEmpty(authImplConfigured)) {
      throw new DrillbitStartupException(String.format("Invalid value '%s' for BOOT option '%s'", authImplConfigured,
          USER_AUTHENTICATOR_IMPL));
    }

    final Collection<Class<? extends UserAuthenticator>> authImpls =
        scan.getImplementations(UserAuthenticator.class);
    logger.debug("Found UserAuthenticator implementations: {}", authImpls);

    for(Class<? extends UserAuthenticator> clazz : authImpls) {
      final UserAuthenticatorTemplate template = clazz.getAnnotation(UserAuthenticatorTemplate.class);
      if (template == null) {
        logger.warn("{} doesn't have {} annotation. Skipping.", clazz.getCanonicalName(),
            UserAuthenticatorTemplate.class);
        continue;
      }

      if (Strings.isNullOrEmpty(template.type())) {
        logger.warn("{} annotation doesn't have valid type field for UserAuthenticator implementation {}. Skipping..",
            UserAuthenticatorTemplate.class, clazz.getCanonicalName());
        continue;
      }

      if (template.type().equalsIgnoreCase(authImplConfigured)) {
        Constructor<?> validConstructor = null;
        for (Constructor<?> c : clazz.getConstructors()) {
          if (c.getParameterTypes().length == 0) {
            validConstructor = c;
            break;
          }
        }

        if (validConstructor == null) {
          logger.warn("Skipping UserAuthenticator implementation class '{}' since it doesn't " +
              "implement a constructor [{}()]", clazz.getCanonicalName(), clazz.getName());
          continue;
        }

        // Instantiate authenticator and initialize it
        try {
          final UserAuthenticator authenticator = clazz.newInstance();
          authenticator.setup(config);
          return authenticator;
        } catch(IllegalArgumentException | IllegalAccessException | InstantiationException e) {
          throw new DrillbitStartupException(
              String.format("Failed to create and initialize the UserAuthenticator class '%s'",
                  clazz.getCanonicalName()), e);
        }
      }
    }

    String errMsg = String.format("Failed to find the implementation of '%s' for type '%s'",
        UserAuthenticator.class.getCanonicalName(), authImplConfigured);
    logger.error(errMsg);
    throw new DrillbitStartupException(errMsg);
  }
}
