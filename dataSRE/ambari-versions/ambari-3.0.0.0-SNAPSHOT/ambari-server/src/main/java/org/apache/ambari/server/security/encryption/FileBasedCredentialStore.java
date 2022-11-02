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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyStore;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileBasedCredentialStore is a CredentialStore implementation that creates and manages
 * a JCEKS (Java Cryptography Extension KeyStore) file on disk.  The key store and its contents are
 * encrypted using the key from the supplied {@link MasterKeyService}.
 * <p/>
 * Most of the work for this implementation is handled by the {@link AbstractCredentialStore}.
 * This class handles the details of the storage location and associated input and output streams.
 */
public class FileBasedCredentialStore extends AbstractCredentialStore {
  private static final Logger LOG = LoggerFactory.getLogger(FileBasedCredentialStore.class);

  /**
   * The directory to use for storing the key store file
   */
  private File keyStoreFile;

  /**
   * Constructs a new FileBasedCredentialStore using the specified key store directory
   *
   * @param keyStoreLocation a File pointing to the directory in which to store the key store file; or the file itself
   */
  public FileBasedCredentialStore(File keyStoreLocation) {
    if (keyStoreLocation == null) {
      // If the keyStoreLocation is not set, create the file (using the default filename)  in the
      // current working directory.
      LOG.warn("Writing key store to the current working directory of the running process");
      keyStoreLocation = new File(Configuration.MASTER_KEYSTORE_FILENAME_DEFAULT);
    } else if (keyStoreLocation.isDirectory()) {
      // If the keyStoreLocation is a directory, create the file (using the default filename) in
      // that directory.
      keyStoreLocation = new File(keyStoreLocation, Configuration.MASTER_KEYSTORE_FILENAME_DEFAULT);
    }

    if (keyStoreLocation.exists()) {
      if (!keyStoreLocation.canWrite()) {
        LOG.warn("The destination file is not writable. Failures may occur when writing the key store to disk: {}", keyStoreLocation.getAbsolutePath());
      }
    } else {
      File directory = keyStoreLocation.getParentFile();
      if ((directory != null) && !directory.canWrite()) {
        LOG.warn("The destination directory is not writable. Failures may occur when writing the key store to disk: {}", keyStoreLocation.getAbsolutePath());
      }
    }

    this.keyStoreFile = keyStoreLocation;
  }

  /**
   * Gets the path to this FileBasedCredentialStore's KeyStore file
   *
   * @return a file indicating the path to this FileBasedCredentialStore's KeyStore file
   */
  public File getKeyStorePath() {
    return keyStoreFile;
  }


  @Override
  protected void persistCredentialStore(KeyStore keyStore) throws AmbariException {
    putKeyStore(keyStore, this.keyStoreFile);
  }


  @Override
  protected KeyStore loadCredentialStore() throws AmbariException {
    return getKeyStore(this.keyStoreFile, DEFAULT_STORE_TYPE);
  }

  /**
   * Reads the key store data from the specified file. If the file does not exist, a new KeyStore
   * will be created.
   *
   * @param keyStoreFile a File pointing to the key store file
   * @param keyStoreType the type of key store data to read (or create)
   * @return the loaded KeyStore
   * @throws AmbariException if the Master Key Service is not set
   * @see AbstractCredentialStore#loadCredentialStore()
   */
  private KeyStore getKeyStore(final File keyStoreFile, String keyStoreType) throws AmbariException {
    KeyStore keyStore;
    FileInputStream inputStream;

    if (keyStoreFile.exists()) {
      if (keyStoreFile.length() > 0) {
        LOG.debug("Reading key store from {}", keyStoreFile.getAbsolutePath());
        try {
          inputStream = new FileInputStream(keyStoreFile);
        } catch (FileNotFoundException e) {
          throw new AmbariException(String.format("Failed to open the key store file: %s", e.getLocalizedMessage()), e);
        }
      } else {
        LOG.debug("The key store file found in {} is empty. Returning new (non-persisted) KeyStore", keyStoreFile.getAbsolutePath());
        inputStream = null;
      }
    } else {
      LOG.debug("Key store file not found in {}. Returning new (non-persisted) KeyStore", keyStoreFile.getAbsolutePath());
      inputStream = null;
    }

    try {
      keyStore = loadKeyStore(inputStream, keyStoreType);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return keyStore;
  }

  /**
   * Writes the specified KeyStore to a file.
   *
   * @param keyStore     the KeyStore to write to a file
   * @param keyStoreFile the File in which to store the KeyStore data
   * @throws AmbariException if an error occurs while writing the KeyStore data
   */
  private void putKeyStore(KeyStore keyStore, File keyStoreFile) throws AmbariException {
    LOG.debug("Writing key store to {}", keyStoreFile.getAbsolutePath());

    FileOutputStream outputStream = null;

    try {
      outputStream = new FileOutputStream(this.keyStoreFile);
      writeKeyStore(keyStore, outputStream);
    } catch (FileNotFoundException e) {
      throw new AmbariException(String.format("Failed to open the key store file: %s", e.getLocalizedMessage()), e);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }
}
