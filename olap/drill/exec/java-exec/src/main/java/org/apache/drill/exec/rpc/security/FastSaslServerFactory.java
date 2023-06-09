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
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * {@link Sasl#createSaslServer} is known to be slow. This class caches available server factories.
 * This is a modified version of Apache Hadoop's implementation.
 */
public final class FastSaslServerFactory implements SaslServerFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FastSaslServerFactory.class);

  // lazy initialization; all relevant providers should have registered with Security so that
  // Sasl#getSaslServerFactories returns the latest possible list of SaslServer factories
  private static final class Holder {
    static final FastSaslServerFactory INSTANCE = new FastSaslServerFactory();

    // prevent instantiation
    private Holder() {
    }
  }

  public static FastSaslServerFactory getInstance() {
    return Holder.INSTANCE;
  }

  // package private
  @VisibleForTesting
  static void reload() {
    getInstance().refresh();
  }

  // non-final for testing purposes
  private ImmutableMap<String, List<SaslServerFactory>> serverFactories;

  // prevent instantiation
  private FastSaslServerFactory() {
    refresh();
  }

  // used in initialization, and for testing
  private void refresh() {
    final Enumeration<SaslServerFactory> factories = Sasl.getSaslServerFactories();
    final Map<String, List<SaslServerFactory>> map = Maps.newHashMap();

    while (factories.hasMoreElements()) {
      final SaslServerFactory factory = factories.nextElement();
      // Passing null so factory is populated with all possibilities.  Properties passed when
      // instantiating a server are what really matter. See createSaslServer.
      for (final String mechanismName : factory.getMechanismNames(null)) {
        if (!map.containsKey(mechanismName)) {
          map.put(mechanismName, new ArrayList<SaslServerFactory>());
        }
        map.get(mechanismName).add(factory);
      }
    }

    serverFactories = ImmutableMap.copyOf(map);
    if (logger.isDebugEnabled()) {
      logger.debug("Registered sasl server factories: {}", serverFactories.keySet());
    }
  }

  @Override
  public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props,
                                     CallbackHandler cbh) throws SaslException {
    final List<SaslServerFactory> factories = serverFactories.get(mechanism);
    if (factories != null) {
      for (final SaslServerFactory factory : factories) {
        final SaslServer saslServer = factory.createSaslServer(mechanism, protocol, serverName, props, cbh);
        if (saslServer != null) {
          return saslServer;
        }
      }
    }
    return null;
  }

  @Override
  public String[] getMechanismNames(final Map<String, ?> props) {
    return serverFactories.keySet().toArray(new String[0]);
  }
}
