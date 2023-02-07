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

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * {@link Sasl#createSaslClient} is known to be slow. This class caches available client factories.
 */
public class FastSaslClientFactory implements SaslClientFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FastSaslClientFactory.class);

  // lazy initialization; all relevant providers should have registered with Security so that
  // Sasl#getSaslClientFactories returns the latest possible list of SaslClient factories
  private static final class Holder {
    static final FastSaslClientFactory INSTANCE = new FastSaslClientFactory();

    // prevent instantiation
    private Holder() {
    }
  }

  public static FastSaslClientFactory getInstance() {
    return Holder.INSTANCE;
  }

  // package private
  @VisibleForTesting
  static void reload() {
    getInstance().refresh();
  }

  // non-final for testing purposes
  private ImmutableMap<String, List<SaslClientFactory>> clientFactories;

  // prevent instantiation
  private FastSaslClientFactory() {
    refresh();
  }

  // used in initialization, and for testing
  private void refresh() {
    final Enumeration<SaslClientFactory> factories = Sasl.getSaslClientFactories();
    final Map<String, List<SaslClientFactory>> map = Maps.newHashMap();

    while (factories.hasMoreElements()) {
      final SaslClientFactory factory = factories.nextElement();
      // Passing null so factory is populated with all possibilities.  Properties passed when
      // instantiating a client are what really matter. See createSaslClient.
      for (final String mechanismName : factory.getMechanismNames(null)) {
        if (!map.containsKey(mechanismName)) {
          map.put(mechanismName, new ArrayList<SaslClientFactory>());
        }
        map.get(mechanismName).add(factory);
      }
    }

    clientFactories = ImmutableMap.copyOf(map);
    if (logger.isDebugEnabled()) {
      logger.debug("Registered sasl client factories: {}", clientFactories.keySet());
    }
  }

  @Override
  public SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol, String serverName,
                                     Map<String, ?> props, CallbackHandler cbh) throws SaslException {
    for (final String mechanism : mechanisms) {
      final List<SaslClientFactory> factories = clientFactories.get(mechanism);
      if (factories != null) {
        for (final SaslClientFactory factory : factories) {
          final SaslClient saslClient = factory.createSaslClient(new String[]{mechanism}, authorizationId, protocol,
              serverName, props, cbh);
          if (saslClient != null) {
            return saslClient;
          }
        }
      }
    }
    return null;
  }

  @Override
  public String[] getMechanismNames(final Map<String, ?> props) {
    return clientFactories.keySet().toArray(new String[0]);
  }
}
