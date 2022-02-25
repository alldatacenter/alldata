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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.AmbariPath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ntp.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterKeyServiceImpl implements MasterKeyService {
  private static final Logger LOG = LoggerFactory.getLogger(MasterKeyServiceImpl.class);
  private static final String MASTER_PASSPHRASE = "masterpassphrase";
  private static final String MASTER_PERSISTENCE_TAG_PREFIX = "#1.0# ";
  private final EncryptionService encryptionService = new AESEncryptionService();

  private char[] master = null;

  /**
   * Constructs a new MasterKeyServiceImpl using a master key read from a file.
   *
   * @param masterKeyFile the location of the master key file
   */
  public MasterKeyServiceImpl(File masterKeyFile) {
    initFromFile(masterKeyFile);
  }

  private void initFromFile(File masterKeyFile) {
    if (masterKeyFile == null) {
      throw new IllegalArgumentException("Master Key location not provided.");
    }

    if (masterKeyFile.exists()) {
      if (isMasterKeyFile(masterKeyFile)) {
        try {
          initializeFromFile(masterKeyFile);
        } catch (Exception e) {
          LOG.error(String.format("Cannot initialize master key from %s: %s", masterKeyFile.getAbsolutePath(), e.getLocalizedMessage()), e);
        }
      } else {
        LOG.error(String.format("The file at %s is not a master ket file", masterKeyFile.getAbsolutePath()));
      }
    } else {
      LOG.error(String.format("Cannot open master key file, %s", masterKeyFile.getAbsolutePath()));
    }
  }

  /**
   * Constructs a new MasterKeyServiceImpl using the specified master key.
   *
   * @param masterKey the master key
   */
  public MasterKeyServiceImpl(String masterKey) {
    if (masterKey != null) {
      master = masterKey.toCharArray();
    } else {
      throw new IllegalArgumentException("Master key cannot be null");
    }
  }

  /**
   * default constructor
   */
  public MasterKeyServiceImpl(){}

  /**
   * Constructs a new MasterKeyServiceImpl using prefered source according config.
   * masterKey > masterKeyLocation > environment
   */
  public MasterKeyServiceImpl(Configuration configuration) {
    if (configuration!= null && configuration.isMasterKeyPersisted()) {
      if (configuration.getMasterKeyLocation() == null) {
        throw new IllegalArgumentException("The master key file location must be specified if the master key is persisted");
      }
      initFromFile(configuration.getMasterKeyLocation());
    } else {
      initializeFromEnv();
    }
  }

  @Override
  public boolean isMasterKeyInitialized() {
    return master != null;
  }

  @Override
  public char[] getMasterSecret() {
    return master;
  }

  public static void main(String args[]) {
    String masterKey = "ThisissomeSecretPassPhrasse";
    String masterKeyLocation = AmbariPath.getPath("/var/lib/ambari-server/keys/master");
    boolean persistMasterKey = false;
    if (args != null && args.length > 0) {
      masterKey = args[0];
      if (args.length > 1) {
        masterKeyLocation = args[1];
      }
      if (args.length > 2 && !args[2].isEmpty()) {
        persistMasterKey = args[2].equalsIgnoreCase("true");
      }
    }

    final MasterKeyServiceImpl masterKeyService = new MasterKeyServiceImpl(masterKey);
    if (persistMasterKey && !masterKeyService.initializeMasterKeyFile(new File(masterKeyLocation), masterKey)) {
      System.exit(1);
    } else {
      System.exit(0);
    }
  }

  /**
   * Initializes the master key file.
   * <p/>
   * If the specified file already exists, it it tested to see if it is a master key file. If so, it
   * will be truncated and the new master key will be stored in the new file. If the file appears
   * to not be a master key file,no changes will be made. The user must manually remove the file if
   * deemed appropriate.
   *
   * @param masterKeyFile the file to write the master key to
   * @param masterKey     the master key
   * @return true if the master key was written to the specified file; otherwise false
   */
   public boolean initializeMasterKeyFile(File masterKeyFile, String masterKey) {
    LOG.debug("Persisting master key into {}", masterKeyFile.getAbsolutePath());

    String encryptedMasterKey = null;
    if (masterKey != null) {
      try {
        encryptedMasterKey = encryptionService.encrypt(masterKey, MASTER_PASSPHRASE);
      } catch (Exception e) {
        LOG.error(String.format("Failed to encrypt master key, no changes have been made: %s", e.getLocalizedMessage()), e);
        return false;
      }
    }

    if (masterKeyFile.exists()) {
      if ((masterKeyFile.length() == 0) || isMasterKeyFile(masterKeyFile)) {
        LOG.info(String.format("Master key file exists at %s, resetting.", masterKeyFile.getAbsolutePath()));
        try (FileOutputStream fos = new FileOutputStream(masterKeyFile); FileChannel fileChannel = fos.getChannel();) {
          fileChannel.truncate(0);
        } catch (FileNotFoundException e) {
          LOG.error(String.format("Failed to open key file at %s: %s", masterKeyFile.getAbsolutePath(), e.getLocalizedMessage()), e);
        } catch (IOException e) {
          LOG.error(String.format("Failed to reset key file at %s: %s", masterKeyFile.getAbsolutePath(), e.getLocalizedMessage()), e);
        }
      } else {
        LOG.info(String.format("File exists at %s, but may not be a master key file. " +
            "It must be manually removed before this file location can be used", masterKeyFile.getAbsolutePath()));
        return false;
      }
    }

    if (encryptedMasterKey != null) {
      try {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(MASTER_PERSISTENCE_TAG_PREFIX + TimeStamp.getCurrentTime().toDateString());
        lines.add(encryptedMasterKey);
        FileUtils.writeLines(masterKeyFile, "UTF8", lines);

        // restrict os permissions to only the user running this process
        protectAccess(masterKeyFile);
      } catch (IOException e) {
        LOG.error(String.format("Failed to persist master key to %s: %s ", masterKeyFile.getAbsolutePath(), e.getLocalizedMessage()), e);
        return false;
      }
    }

    return true;
  }

  /**
   * Determines if the specified file is a "master key" file by checking the file header to see if it
   * matches an expected value.
   * <p/>
   * The "master key" file is expected to have a header (or first line) that starts with "#1.0#". If it,
   * it is assumed to be a "master key" file, otherwise it is assumed to not be.
   *
   * @param file the file to test
   * @return true if the file is identitified as "master key" file; otherwise false
   */
  private static boolean isMasterKeyFile(File file) {
    try (FileReader reader = new FileReader(file)) {
      char[] buffer = new char[MASTER_PERSISTENCE_TAG_PREFIX.length()];
      return (reader.read(buffer) == buffer.length) && Arrays.equals(buffer, MASTER_PERSISTENCE_TAG_PREFIX.toCharArray());
    } catch (Exception e) {
      // Ignore, assume the file is not a master key file...
    }

    return false;
  }


  /**
   * Ensures that the owner of this process is the only local user account able to read and write to
   * the specified file or read, write to, and execute the specified directory.
   *
   * @param file the file or directory for which to modify access
   */
  private static void protectAccess(File file) throws AmbariException {
    if (file.exists()) {
      if (!file.setReadable(false, false) || !file.setReadable(true, true)) {
        String message = String.format("Failed to set %s readable only by current user", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (!file.setWritable(false, false) || !file.setWritable(true, true)) {
        String message = String.format("Failed to set %s writable only by current user", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (file.isDirectory()) {
        if (!file.setExecutable(false, false) || !file.setExecutable(true, true)) {
          String message = String.format("Failed to set %s executable by current user", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } else {
        if (!file.setExecutable(false, false)) {
          String message = String.format("Failed to set %s not executable", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      }
    }
  }

  private void initializeFromEnv() {
    String key;
    Map<String, String> envVariables = System.getenv();
    if (envVariables != null && !envVariables.isEmpty()) {
      key = envVariables.get(Configuration.MASTER_KEY_ENV_PROP);
      if (key == null || key.isEmpty()) {
        String keyPath = envVariables.get(Configuration.MASTER_KEY_LOCATION.getKey());
        if (keyPath != null && !keyPath.isEmpty()) {
          File keyFile = new File(keyPath);
          if (keyFile.exists()) {
            try {
              initializeFromFile(keyFile);
            } catch (Exception e) {
              LOG.error("Cannot read master key from file: " + keyPath);
              e.printStackTrace();
            }
          }
        } else {
          LOG.error("Cannot read master key property {1} or master key file property {3} from environment");
        }
      } else {
        master = key.toCharArray();
      }
    }
  }

  private void initializeFromFile(File masterFile) throws Exception {
    try {
      List<String> lines = FileUtils.readLines(masterFile, "UTF8");
      String tag = lines.get(0);
      LOG.info("Loading from persistent master: " + tag);
      master = encryptionService.decrypt(lines.get(1), MASTER_PASSPHRASE).toCharArray();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
