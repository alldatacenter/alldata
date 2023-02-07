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
package org.apache.drill.exec.rpc.security;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.rpc.security.kerberos.KerberosFactory;
import org.apache.drill.exec.rpc.security.plain.PlainFactory;

import javax.security.sasl.SaslException;
import java.util.Map;
import java.util.Set;

public class ClientAuthenticatorProvider implements AuthenticatorProvider {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(ClientAuthenticatorProvider.class);

  private static final String customFactories = System.getProperty("drill.customAuthFactories");

  private static final class Holder {
    static final ClientAuthenticatorProvider INSTANCE = new ClientAuthenticatorProvider();

    // prevent instantiation
    private Holder() {
    }
  }

  public static ClientAuthenticatorProvider getInstance() {
    return Holder.INSTANCE;
  }

  // Mapping: simple name -> authenticator factory
  private final Map<String, AuthenticatorFactory> authFactories = CaseInsensitiveMap.newHashMapWithExpectedSize(5);

  private ClientAuthenticatorProvider() {
    // factories provided by Drill
    final KerberosFactory kerberosFactory = new KerberosFactory();
    authFactories.put(kerberosFactory.getSimpleName(), kerberosFactory);
    final PlainFactory plainFactory = new PlainFactory();
    authFactories.put(plainFactory.getSimpleName(), plainFactory);

    // then, custom factories
    if (customFactories != null) {
      final String[] factories = customFactories.split(",");
      for (final String factory : factories) {
        try {
          final Class<?> clazz = Class.forName(factory);
          if (AuthenticatorFactory.class.isAssignableFrom(clazz)) {
            final AuthenticatorFactory instance = (AuthenticatorFactory) clazz.newInstance();
            authFactories.put(instance.getSimpleName(), instance);
          }
        } catch (final ReflectiveOperationException e) {
          logger.error("Failed to create auth factory {}", factory, e);
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Configured mechanisms: {}", authFactories.keySet());
    }
  }

  @Override
  public AuthenticatorFactory getAuthenticatorFactory(final String name) throws SaslException {
    final AuthenticatorFactory mechanism = authFactories.get(name);
    if (mechanism == null) {
      throw new SaslException(String.format("Unknown mechanism: '%s' Configured mechanisms: %s",
          name, authFactories.keySet()));
    }
    return mechanism;
  }

  @Override
  public Set<String> getAllFactoryNames() {
    return authFactories.keySet();
  }

  @Override
  public boolean containsFactory(final String name) {
    return authFactories.containsKey(name);
  }

  @Override
  public void close() throws Exception {
    AutoCloseables.close(authFactories.values());
    authFactories.clear();
  }
}
