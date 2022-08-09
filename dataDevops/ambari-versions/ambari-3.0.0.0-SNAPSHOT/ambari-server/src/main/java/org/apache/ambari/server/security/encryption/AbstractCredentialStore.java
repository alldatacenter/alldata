/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.spec.SecretKeySpec;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.CredentialFactory;

/**
 * AbstractCredentialStore is an abstract implementation of CredentialStore that loads and
 * stores @{link KeyStore} data. Implementations of this class, provide the input and output streams
 * used to read and write the data.
 */
public abstract class AbstractCredentialStore implements CredentialStore {
  protected static final String DEFAULT_STORE_TYPE = "JCEKS";

  /**
   * A lock object to lock access to the credential store, allowing only one thread to access
   * the credential store at a time.
   */
  private final Lock lock = new ReentrantLock();

  /**
   * The MasterKeyService containing the key used to encrypt the KeyStore data
   */
  private MasterKeyService masterKeyService;

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

    lock.lock();
    try {
      KeyStore ks = loadCredentialStore();
      addCredential(ks, alias, credential);
      persistCredentialStore(ks);
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
    if (alias == null) {
      return null;
    } else {
      lock.lock();
      try {
        return getCredential(loadCredentialStore(), alias);
      } finally {
        lock.unlock();
      }
    }
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
    if ((alias != null) && !alias.isEmpty()) {
      lock.lock();
      try {
        KeyStore ks = loadCredentialStore();
        if (ks != null) {
          try {
            ks.deleteEntry(alias);
            persistCredentialStore(ks);
          } catch (KeyStoreException e) {
            throw new AmbariException("Failed to delete the KeyStore entry - the key store may not have been initialized", e);
          }
        }
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
    Set<String> credentials = null;

    lock.lock();
    try {
      KeyStore ks = loadCredentialStore();
      if (ks != null) {
        try {
          Enumeration<String> aliases = ks.aliases();

          if (aliases != null) {
            credentials = new HashSet<>();
            while (aliases.hasMoreElements()) {
              credentials.add(aliases.nextElement());
            }
          }
        } catch (KeyStoreException e) {
          throw new AmbariException("Failed to read KeyStore - the key store may not have been initialized", e);
        }
      }
    } finally {
      lock.unlock();
    }

    return credentials;
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
    boolean exists = false;

    if ((alias != null) && !alias.isEmpty()) {
      lock.lock();
      try {
        KeyStore ks = loadCredentialStore();
        if (ks != null) {
          try {
            exists = ks.containsAlias(alias);
          } catch (KeyStoreException e) {
            throw new AmbariException("Failed to search the KeyStore for the requested entry - the key store may not have been initialized", e);
          }
        }
      } finally {
        lock.unlock();
      }
    }

    return exists;
  }

  @Override
  public void setMasterKeyService(MasterKeyService masterKeyService) {
    this.masterKeyService = masterKeyService;
  }

  /**
   * Adds a new credential to the supplied KeyStore
   * <p/>
   * The supplied key will be converted into UTF-8 bytes before being stored.
   *
   * @param keyStore   the KeyStore
   * @param alias      a string declaring the alias (or name) of the credential
   * @param credential the credential to store
   * @throws AmbariException if an error occurs while storing the new credential
   */
  protected void addCredential(KeyStore keyStore, String alias, Credential credential) throws AmbariException {
    if (keyStore != null) {
      try {
        Key key;
        char[] value = (credential == null) ? null : credential.toValue();

        if ((value == null) || (value.length == 0)) {
          key = null;
        } else {
          key = new SecretKeySpec(toBytes(value), "AES");
        }

        keyStore.setKeyEntry(alias, key, masterKeyService.getMasterSecret(), null);
      } catch (KeyStoreException e) {
        throw new AmbariException("The key store has not been initialized", e);
      }
    }
  }

  /**
   * Retrieves the specified credential from a KeyStore
   *
   * @param keyStore the KeyStore
   * @param alias    a string declaring the alias (or name) of the credential
   * @return an array of chars containing the credential
   * @throws AmbariException if an error occurs while retrieving the new credential
   */
  protected Credential getCredential(KeyStore keyStore, String alias) throws AmbariException {
    char[] value = null;

    if (keyStore != null) {
      try {
        Key key = keyStore.getKey(alias, masterKeyService.getMasterSecret());
        if (key != null) {
          value = toChars(key.getEncoded());
        }
      } catch (UnrecoverableKeyException e) {
        throw new AmbariException("The key cannot be recovered (e.g., the given password is wrong)", e);
      } catch (KeyStoreException e) {
        throw new AmbariException("The key store has not been initialized", e);
      } catch (NoSuchAlgorithmException e) {
        throw new AmbariException(" if the algorithm for recovering the key cannot be found", e);
      }
    }

    return CredentialFactory.createCredential(value);
  }

  /**
   * Calls the implementation-specific facility to persist the KeyStore
   *
   * @param keyStore the KeyStore to persist
   * @throws AmbariException if an error occurs while persisting the key store data
   */
  protected abstract void persistCredentialStore(KeyStore keyStore) throws AmbariException;

  /**
   * Calls the implementation-specific facility to load the KeyStore
   *
   * @throws AmbariException if an error occurs while loading the key store data
   */
  protected abstract KeyStore loadCredentialStore() throws AmbariException;


  /**
   * Gets the lock object used to protect access to the credential store
   *
   * @return a Lock object
   */
  protected Lock getLock() {
    return lock;
  }

  /**
   * Loads a KeyStore from an InputStream
   * <p/>
   * Implementations are expected to call this to load the relevant KeyStore data from the
   * InputStream of some storage facility.
   *
   * @param inputStream  the InputStream to read the data from
   * @param keyStoreType the type of key store data expected
   * @return a new KeyStore instance with the loaded data
   * @throws AmbariException if an error occurs while loading the key store data from the InputStream
   */
  protected KeyStore loadKeyStore(InputStream inputStream, String keyStoreType) throws AmbariException {
    if (masterKeyService == null) {
      throw new AmbariException("Master Key Service is not set for this Credential store.");
    }

    KeyStore keyStore;
    try {
      keyStore = KeyStore.getInstance(keyStoreType);
    } catch (KeyStoreException e) {
      throw new AmbariException(String.format("No provider supports a key store implementation for the specified type: %s", keyStoreType), e);
    }

    try {
      keyStore.load(inputStream, masterKeyService.getMasterSecret());
    } catch (CertificateException e) {
      throw new AmbariException(String.format("One or more credentials from the key store could not be loaded: %s", e.getLocalizedMessage()), e);
    } catch (NoSuchAlgorithmException e) {
      throw new AmbariException(String.format("The algorithm used to check the integrity of the key store cannot be found: %s", e.getLocalizedMessage()), e);
    } catch (IOException e) {
      if (e.getCause() instanceof UnrecoverableKeyException) {
        throw new AmbariException(String.format("The password used to decrypt the key store is incorrect: %s", e.getLocalizedMessage()), e);
      } else {
        throw new AmbariException(String.format("Failed to read the key store: %s", e.getLocalizedMessage()), e);
      }
    }

    return keyStore;
  }

  /**
   * Writes a KeyStore to an OutputStream
   * <p/>
   * Implementations are expected to call this to write the relevant KeyStore data to the
   * OutputStream of some storage facility.
   *
   * @param keyStore     the KeyStore to write
   * @param outputStream the OutputStream to write the data into
   * @throws AmbariException if an error occurs while writing the key store data
   */
  protected void writeKeyStore(KeyStore keyStore, OutputStream outputStream) throws AmbariException {
    if (masterKeyService == null) {
      throw new AmbariException("Master Key Service is not set for this Credential store.");
    }

    try {
      keyStore.store(outputStream, masterKeyService.getMasterSecret());
    } catch (CertificateException e) {
      throw new AmbariException(String.format("A credential within in the key store data could not be stored: %s", e.getLocalizedMessage()), e);
    } catch (NoSuchAlgorithmException e) {
      throw new AmbariException(String.format("The appropriate data integrity algorithm could not be found: %s", e.getLocalizedMessage()), e);
    } catch (KeyStoreException e) {
      throw new AmbariException(String.format("The key store has not been initialized: %s", e.getLocalizedMessage()), e);
    } catch (IOException e) {
      throw new AmbariException(String.format("Failed to write the key store: %s", e.getLocalizedMessage()), e);
    }
  }

  /**
   * Converts an array of characters to an array of bytes by encoding each character into UTF-8 bytes.
   * <p/>
   * An attempt is made to clear out sensitive data by filling any buffers with 0's
   *
   * @param chars the array of chars to convert
   * @return an array of bytes, or null if the original array was null
   */
  protected byte[] toBytes(char[] chars) {
    if (chars == null) {
      return null;
    } else {
      CharBuffer charBuffer = CharBuffer.wrap(chars);
      ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);

      byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());

      // Clear sensitive data
      Arrays.fill(charBuffer.array(), '\u0000');
      Arrays.fill(byteBuffer.array(), (byte) 0);

      return bytes;
    }
  }

  /**
   * Converts an array of bytes to an array of character by decoding the bytes using the UTF-8
   * character set.
   * <p/>
   * An attempt is made to clear out sensitive data by filling any buffers with 0's
   *
   * @param bytes the array of bytes to convert
   * @return an array of chars, or null if the original array was null
   */
  protected char[] toChars(byte[] bytes) {
    if (bytes == null) {
      return null;
    } else {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      CharBuffer charBuffer = Charset.forName("UTF-8").decode(byteBuffer);

      char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());

      // Clear sensitive data
      Arrays.fill(charBuffer.array(), '\u0000');
      Arrays.fill(byteBuffer.array(), (byte) 0);

      return chars;
    }
  }
}
