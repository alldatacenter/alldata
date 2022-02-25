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

package org.apache.ambari.server.security.encryption;

import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * InMemoryCredentialStore is a CredentialStore implementation that creates and manages
 * a JCEKS (Java Cryptography Extension KeyStore) in memory.  The key store and its contents are
 * encrypted using the key from the supplied {@link MasterKeyService}.
 * <p/>
 * This class handles the details of the in-memory storage buffer and associated input and output
 * streams. Each credential is stored in its own KeyStore that may be be purged upon some
 * retention timeout - if specified.
 */
public class InMemoryCredentialStore extends AbstractCredentialStore {
  private static final Logger LOG = LoggerFactory.getLogger(InMemoryCredentialStore.class);

  /**
   * A cache containing the KeyStore data
   */
  private final Cache<String, KeyStore> cache;

  /**
   * Constructs a new InMemoryCredentialStore where credentials have no retention timeout
   */
  public InMemoryCredentialStore() {
    this(0, TimeUnit.MINUTES, false);
  }

  /**
   * Constructs a new InMemoryCredentialStore with a specified credential timeout
   *
   * @param retentionDuration the time in some units to keep stored credentials, from the time they are added
   * @param units             the units for the retention duration (minutes, seconds, etc...)
   * @param activelyPurge     true to actively purge credentials after the retention time has expired;
   *                          otherwise false, to passively purge credentials after the retention time has expired
   */
  public InMemoryCredentialStore(final long retentionDuration, final TimeUnit units, boolean activelyPurge) {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

    // If the retentionDuration is less the 1, then no retention policy is to be enforced
    if (retentionDuration > 0) {
      // If actively purging expired credentials, set up a timer to periodically clean the cache
      if (activelyPurge) {
        ThreadFactory threadFactory = new ThreadFactory() {
          @Override
          public Thread newThread(Runnable runnable) {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            if (t != null) {
              t.setName(String.format("%s active cleanup timer", InMemoryCredentialStore.class.getSimpleName()));
              t.setDaemon(true);
            }
            return t;
          }
        };
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Cleaning up cache due to retention timeout of {} milliseconds",
                  units.toMillis(retentionDuration));
            }
            cache.cleanUp();
          }
        };

        Executors.newSingleThreadScheduledExecutor(threadFactory).schedule(runnable, 1, TimeUnit.MINUTES);
      }

      builder.expireAfterWrite(retentionDuration, units);
    }

    cache = builder.build();
  }

  /**
   * Adds a new credential to this CredentialStore
   * <p/>
   * The supplied key will be converted into UTF-8 bytes before being stored.
   * <p/>
   * This implementation is thread-safe, allowing one thread at a time to access the credential store.
   *
   * @param alias      a string declaring the alias (or name) of the credential
   * @param credential the credential to store
   * @throws AmbariException if an error occurs while storing the new credential
   */
  @Override
  public void addCredential(String alias, Credential credential) throws AmbariException {
    if ((alias == null) || alias.isEmpty()) {
      throw new IllegalArgumentException("Alias cannot be null or empty.");
    }

    Lock lock = getLock();
    lock.lock();
    try {
      KeyStore keyStore = loadKeyStore(null, DEFAULT_STORE_TYPE);
      addCredential(keyStore, alias, credential);
      cache.put(alias, keyStore);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Retrieves the specified credential from this CredentialStore
   * <p/>
   * This implementation is thread-safe, allowing one thread at a time to access the credential store.
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @return a Credential or null of not found
   * @throws AmbariException if an error occurs while retrieving the new credential
   */
  @Override
  public Credential getCredential(String alias) throws AmbariException {
    Credential credential = null;

    if ((alias != null) && !alias.isEmpty()) {
      Lock lock = getLock();
      lock.lock();
      try {
        KeyStore keyStore = cache.getIfPresent(alias);
        if (keyStore != null) {
          credential = getCredential(keyStore, alias);
        }
      } finally {
        lock.unlock();
      }
    }

    return credential;
  }

  /**
   * Removes the specified credential from this CredentialStore
   * <p/>
   * This implementation is thread-safe, allowing one thread at a time to access the credential store.
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @throws AmbariException if an error occurs while removing the new credential
   */
  @Override
  public void removeCredential(String alias) throws AmbariException {
    if (alias != null) {
      Lock lock = getLock();
      lock.lock();
      try {
        cache.invalidate(alias);
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Returns a list of the alias names for the credentials stored in the CredentialStore
   * <p/>
   * This implementation is thread-safe, allowing one thread at a time to access the credential store.
   *
   * @return a Set of Strings representing alias names for the credentials stored in the CredentialStore
   * @throws AmbariException if an error occurs while searching forthe credential
   */
  @Override
  public Set<String> listCredentials() throws AmbariException {
    Lock lock = getLock();
    lock.lock();
    try {
      return new HashSet<>(cache.asMap().keySet());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Tests this CredentialStore for the existence of a credential with the specified alias
   * <p/>
   * This implementation is thread-safe, allowing one thread at a time to access the credential store.
   *
   * @param alias a string declaring the alias (or name) of the credential
   * @return true if the alias exists; otherwise false
   * @throws AmbariException if an error occurs while searching forthe credential
   */
  @Override
  public boolean containsCredential(String alias) throws AmbariException {
    Lock lock = getLock();
    lock.lock();
    try {
      return (alias != null) && !alias.isEmpty() && (cache.getIfPresent(alias) != null);
    } finally {
      lock.unlock();
    }
  }


  @Override
  protected void persistCredentialStore(KeyStore keyStore) throws AmbariException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected KeyStore loadCredentialStore() throws AmbariException {
    throw new UnsupportedOperationException();
  }
}
