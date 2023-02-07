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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.io.Charsets;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of UserAuthenticator that reads passwords from an htpasswd
 * formatted file.
 * <p>
 * Currently supports MD5, SHA-1, and plaintext passwords.
 * <p>
 * Use the htpasswd command line tool to create and modify htpasswd files.
 * <p>
 * By default this loads the passwords from <code>/opt/drill/conf/htpasswd</code>.  Users can change the path by
 * putting the absolute file path as <code>drill.exec.security.user.auth.htpasswd.path</code> in
 * <code>drill-override.conf</code>.
 * <p>
 * This is intended for situations where the list of users is relatively static, and you are running
 * drill in a container so using pam is not convenient.
 */
@UserAuthenticatorTemplate(type = "htpasswd")
public class HtpasswdFileUserAuthenticator implements UserAuthenticator {
  private static final Logger logger = LoggerFactory.getLogger(HtpasswdFileUserAuthenticator.class);
  private static final Pattern HTPASSWD_LINE_PATTERN = Pattern.compile("^([^:]+):([^:]+)");
  public static final String DEFAULT_HTPASSWD_AUTHENTICATOR_PATH = "/opt/drill/conf/htpasswd";

  private String path = DEFAULT_HTPASSWD_AUTHENTICATOR_PATH;
  private long lastModified;
  private long lastFileSize;
  private Map<String, String> userToPassword;

  @Override
  public void setup(DrillConfig drillConfig) throws DrillbitStartupException {
    if (drillConfig.hasPath(ExecConstants.HTPASSWD_AUTHENTICATOR_PATH)) {
      path = drillConfig.getString(ExecConstants.HTPASSWD_AUTHENTICATOR_PATH);
    }
  }

  /**
   * Check password against hash read from the file
   *
   * @param password User provided password
   * @param hash     Hash stored in the htpasswd file
   * @return true if the password matched the hash
   */
  public static boolean isPasswordValid(String password, String hash) {
    if (hash.startsWith("$apr1$")) {
      return hash.equals(Md5Crypt.apr1Crypt(password, hash));
    } else if (hash.startsWith("$1$")) {
      return hash.equals(Md5Crypt.md5Crypt(password.getBytes(Charsets.UTF_8), hash));
    } else if (hash.startsWith("{SHA}")) {
      return hash.substring(5).equals(Base64.getEncoder().encodeToString(DigestUtils.sha1(password)));
    } else if (hash.startsWith("$2y$")) {
      // bcrypt not supported currently
      return false;
    } else {
      return hash.equals(password);
    }
  }

  /**
   * Validate the given username and password against the password file
   *
   * @param username Username provided
   * @param password Password provided
   * @throws UserAuthenticationException If the username and password could not be validated
   */
  @Override
  public void authenticate(String username, String password) throws UserAuthenticationException {
    read();
    String hash = this.userToPassword.get(username);
    boolean credentialsAccepted = (hash != null && isPasswordValid(password, hash));
    if (!credentialsAccepted) {
      throw new UserAuthenticationException(String.format("htpasswd auth failed for user '%s'",
        username));
    }
  }

  /**
   * Read the password file into the map, if the file has changed since we last read it
   */
  protected synchronized void read() {
    File file = new File(path);
    long newLastModified = file.exists() ? file.lastModified() : 0;
    long newFileSize = file.exists() ? file.length() : 0;
    if (userToPassword == null || newLastModified != lastModified || newFileSize != lastFileSize) {
      HashMap<String, String> newMap = new HashMap<>();
      if(newFileSize != 0) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
              Matcher m = HTPASSWD_LINE_PATTERN.matcher(line);
              if (m.matches()) {
                newMap.put(m.group(1), m.group(2));
              }
            }
          }
        } catch (Exception e) {
          logger.error(MessageFormat.format("Failed to read htpasswd file at path {0}", file), e);
        }
      } else {
        logger.error(MessageFormat.format("Empty or missing htpasswd file at path {0}", file));
      }
      lastFileSize = newFileSize;
      lastModified = newLastModified;
      userToPassword = newMap;
    }
  }

  /**
   * Free resources associated with this authenticator
   */
  @Override
  public void close() {
    lastModified = 0;
    userToPassword = null;
  }
}
