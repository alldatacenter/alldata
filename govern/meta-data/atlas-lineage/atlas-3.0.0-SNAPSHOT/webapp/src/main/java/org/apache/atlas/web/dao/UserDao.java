/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.dao;

import com.google.common.annotations.VisibleForTesting;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.security.NoSuchAlgorithmException;
import javax.annotation.PostConstruct;
import org.apache.atlas.web.security.AtlasAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.stereotype.Repository;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.web.model.User;
import org.apache.commons.configuration.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.security.MessageDigest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.StringUtils;


@Repository
public class UserDao {
    private static final Logger LOG = LoggerFactory.getLogger(UserDao.class);

    private static final String             DEFAULT_USER_CREDENTIALS_PROPERTIES = "users-credentials.properties";
    private static       boolean            v1ValidationEnabled = true;
    private static       boolean            v2ValidationEnabled = true;

    private Properties userLogins = new Properties();

    @PostConstruct
    public void init() {
        loadFileLoginsDetails();
    }

    void loadFileLoginsDetails() {
        userLogins.clear();

        InputStream inStr = null;

        try {
            Configuration configuration = ApplicationProperties.get();

            v1ValidationEnabled = configuration.getBoolean("atlas.authentication.method.file.v1-validation.enabled", true);
            v2ValidationEnabled = configuration.getBoolean("atlas.authentication.method.file.v2-validation.enabled", true);

            inStr = ApplicationProperties.getFileAsInputStream(configuration, "atlas.authentication.method.file.filename", DEFAULT_USER_CREDENTIALS_PROPERTIES);

            userLogins.load(inStr);
        } catch (IOException | AtlasException e) {
            LOG.error("Error while reading user.properties file", e);

            throw new RuntimeException(e);
        } finally {
            if (inStr != null) {
                try {
                    inStr.close();
                } catch (Exception excp) {
                    // ignore
                }
            }
        }
    }

    public User loadUserByUsername(final String username) throws AuthenticationException {
        String userdetailsStr = userLogins.getProperty(username);

        if (userdetailsStr == null || userdetailsStr.isEmpty()) {
            throw new UsernameNotFoundException("Username not found." + username);
        }

        String   password = "";
        String   role     = "";
        String[] dataArr  = userdetailsStr.split("::");

        if (dataArr != null && dataArr.length == 2) {
            role     = dataArr[0];
            password = dataArr[1];
        } else {
            LOG.error("User role credentials is not set properly for {}", username);

            throw new AtlasAuthenticationException("User role credentials is not set properly for " + username );
        }

        List<GrantedAuthority> grantedAuths = new ArrayList<>();

        if (StringUtils.hasText(role)) {
            grantedAuths.add(new SimpleGrantedAuthority(role));
        } else {
            LOG.error("User role credentials is not set properly for {}", username);

            throw new AtlasAuthenticationException("User role credentials is not set properly for " + username );
        }

        User userDetails = new User(username, password, grantedAuths);

        return userDetails;
    }

    @VisibleForTesting
    public void setUserLogins(Properties userLogins) {
        this.userLogins = userLogins;
    }

    public static String encrypt(String password) {
        String ret = null;

        try {
            ret = BCrypt.hashpw(password, BCrypt.gensalt());
        } catch (Throwable excp) {
            LOG.warn("UserDao.encrypt(): failed", excp);
        }

        return ret;
    }

    public static boolean checkEncrypted(String password, String encryptedPwd, String userName) {
        boolean ret = checkPasswordBCrypt(password, encryptedPwd);

        if (!ret && v2ValidationEnabled) {
            ret = checkPasswordSHA256WithSalt(password, encryptedPwd, userName);
        }

        if (!ret && v1ValidationEnabled) {
            ret = checkPasswordSHA256(password, encryptedPwd);
        }

        return ret;
    }

    private static boolean checkPasswordBCrypt(String password, String encryptedPwd) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("checkPasswordBCrypt()");
        }

        boolean ret = false;

        try {
            ret = BCrypt.checkpw(password, encryptedPwd);
        } catch (Throwable excp) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("checkPasswordBCrypt(): failed", excp);
            }
        }

        return ret;
    }

    private static boolean checkPasswordSHA256WithSalt(String password, String encryptedPwd, String salt) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("checkPasswordSHA256WithSalt()");
        }

        boolean ret = false;

        try {
            String hash = encodePassword(password, salt);

            ret = hash != null && hash.equals(encryptedPwd);
        } catch (Throwable excp) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("checkPasswordSHA256WithSalt(): failed", excp);
            }
        }

        return ret;
    }

    private static boolean checkPasswordSHA256(String password, String encryptedPwd) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("checkPasswordSHA256()");
        }

        boolean ret = false;

        try {
            String hash = getSha256Hash(password);

            ret = hash != null && hash.equals(encryptedPwd);
        } catch (Throwable excp) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("checkPasswordSHA256(): failed", excp);
            }
        }

        return ret;
    }

    private static String getSha256Hash(String base) throws AtlasAuthenticationException {
        try {
            MessageDigest digest    = MessageDigest.getInstance("SHA-256");
            byte[]        hash      = digest.digest(base.getBytes("UTF-8"));
            StringBuffer  hexString = new StringBuffer();

            for (byte aHash : hash) {
                String hex = Integer.toHexString(0xff & aHash);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new AtlasAuthenticationException("Exception while encoding password.", ex);
        }
    }

    public static String encodePassword(String rawPass, Object salt) {
        String saltedPass = mergePasswordAndSalt(rawPass, salt, false);
        MessageDigest messageDigest = getMessageDigest();
        byte[] digest = messageDigest.digest(Utf8.encode(saltedPass));

        return new String(Hex.encode(digest));
    }

    protected static final MessageDigest getMessageDigest() throws IllegalArgumentException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException var2) {
            throw new IllegalArgumentException("No such algorithm [SHA-256 ]");
        }
    }

    protected static String mergePasswordAndSalt(String password, Object salt, boolean strict) {
        if (!StringUtils.hasText(password)) {
            password = "";
        }

        if (strict && salt != null && (salt.toString().lastIndexOf("{") != -1 || salt.toString().lastIndexOf("}") != -1)) {
            throw new IllegalArgumentException("Cannot use { or } in salt.toString()");
        } else {
            return StringUtils.hasText(salt.toString()) ? password + "{" + salt.toString() + "}" : password;
        }
    }

}