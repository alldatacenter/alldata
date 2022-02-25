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
package org.apache.ambari.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Utility class to read passwords from files or the Credential Store
 */
public class PasswordUtils {

  private static final Logger LOG = LoggerFactory.getLogger(PasswordUtils.class);
  private static final Lock LOCK = new ReentrantLock();
  private static final PasswordUtils INSTANCE = new PasswordUtils();

  /**
   * The constructor we need for creating a singleton instance
   */
  private PasswordUtils() {
  }

  @Inject
  private static Configuration configuration;

  private volatile CredentialProvider credentialProvider = null;

  public static PasswordUtils getInstance() {
    return INSTANCE;
  }

  /**
   * Reading the password belong to the given password property
   *
   * @param passwordProperty
   *          this is either the Credential Store alias or the password file name
   *          you want to read the password for/from
   * @param defaultPassword
   *          the default password this function returns in case the given
   *          <code>passwordProperty</code> is <blank> or the password file cannot
   *          be read for any reason
   * @return in case <code>passwordProperty</code> belongs to a Credential Store
   *         alias this function returns the password of the given Credential
   *         Store alias or <code>null</code> (if the given alias is
   *         <code>blank</code> or there is no password found in CS); otherwise
   *         either the password found in the given password file is returned or
   *         <code>defaultPassword</code> if the given path is <code>blank</code>
   *         or cannot be read for any reason
   * @throws RuntimeException
   *           if any error occurred while reading the password file
   */
  public String readPassword(String passwordProperty, String defaultPassword) {
    if (StringUtils.isNotBlank(passwordProperty)) {
      if (CredentialProvider.isAliasString(passwordProperty)) {
        return readPasswordFromStore(passwordProperty);
      } else {
        return readPasswordFromFile(passwordProperty, defaultPassword);
      }
    }
    return defaultPassword;
  }

  /**
   * Reading password from the given password file
   *
   * @param filePath
   *          the path of the file to read the password from
   * @param defaultPassword
   *          the default password this function returns in case the given
   *          <code>filePath</code> is <code>blank</code> or the password file
   *          cannot be read for any reason
   * @return the password found in the given password file or
   *         <code>defaultPassword</code> if the given path is <code>blank</code>
   *         or cannot be read for any reason
   * @throws RuntimeException
   *           when any error occurred while reading the password file
   */
  public String readPasswordFromFile(String filePath, String defaultPassword) {
    if (StringUtils.isBlank(filePath) || !fileExistsAndCanBeRead(filePath)) {
      LOG.debug("DB password file not specified or does not exist/can not be read - using default");
      return defaultPassword;
    } else {
      LOG.debug("Reading password from file {}", filePath);
      String password = null;
      try {
        password = FileUtils.readFileToString(new File(filePath), Charset.defaultCharset());
        return StringUtils.chomp(password);
      } catch (IOException e) {
        throw new RuntimeException("Unable to read password from file [" + filePath + "]", e);
      }
    }
  }

  private boolean fileExistsAndCanBeRead(String filePath) {
    final File passwordFile = new File(filePath);
    return passwordFile.exists() && passwordFile.canRead() && passwordFile.isFile();
  }

  private String readPasswordFromStore(String aliasStr) {
    return readPasswordFromStore(aliasStr, configuration);
  }

  /**
   * Reading the password from Credential Store for the given alias
   *
   * @param aliasStr
   *          the Credential Store alias you want to read the password for
   * @param configuration with configured master key:
   * masterKeyLocation
   *          the master key location file
   * isMasterKeyPersisted
   *          a flag indicating whether the master key is persisted
   * masterKeyStoreLocation
   *          the master key-store location file
   * @return the password of the given alias if it is not <code>blank</code> and
   *         there is password stored for this alias in Credential Store;
   *         <code>null</code> otherwise
   */
  public String readPasswordFromStore(String aliasStr, Configuration configuration) {
    String password = null;
    loadCredentialProvider(configuration);
    if (credentialProvider != null) {
      char[] result = null;
      try {
        result = credentialProvider.getPasswordForAlias(aliasStr);
      } catch (AmbariException e) {
        LOG.error("Error reading from credential store.", e);
      }
      if (result != null) {
        password = new String(result);
      } else {
        if (CredentialProvider.isAliasString(aliasStr)) {
          LOG.error("Cannot read password for alias = " + aliasStr);
        } else {
          LOG.warn("Raw password provided, not an alias. It cannot be read from credential store.");
        }
      }
    }
    return password;
  }

  private void loadCredentialProvider(Configuration configuration) {
    if (credentialProvider == null) {
      try {
        LOCK.lock();
        credentialProvider = new CredentialProvider(null, configuration);
      } catch (Exception e) {
        LOG.info("Credential provider creation failed", e);
        credentialProvider = null;
      } finally {
        LOCK.unlock();
      }
    }
  }

}
