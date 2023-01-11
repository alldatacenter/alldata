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

package org.apache.hadoop.crypto.key;

import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.kms.dao.DaoManager;
import org.apache.ranger.kms.dao.RangerMasterKeyDao;
import org.apache.ranger.plugin.util.XMLUtils;
import org.apache.ranger.entity.XXRangerMasterKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class RangerMasterKey implements RangerKMSMKI {

    private static final Logger logger = LoggerFactory.getLogger(RangerMasterKey.class);

    private static final String DEFAULT_MK_CIPHER = "AES";
    private static final int DEFAULT_MK_KeySize = 256;
    private static final int DEFAULT_SALT_SIZE = 8;
    private static final String DEFAULT_SALT = "abcdefghijklmnopqrstuvwxyz01234567890";
    private static final String DEFAULT_CRYPT_ALGO = "PBEWithMD5AndTripleDES";
    private static final int DEFAULT_ITERATION_COUNT = 1000;
    private static String password = null;
    private static String DEFAULT_MD_ALGO;

    public static final String DBKS_SITE_XML = "dbks-site.xml";
    private static Properties serverConfigProperties = new Properties();

    public static String MK_CIPHER;
    public static Integer MK_KeySize = 0;
    public static Integer SALT_SIZE = 0;
    public static String SALT;
    public static String PBE_ALGO;
    public static String MD_ALGO;
    public static Integer ITERATION_COUNT = 0;
    public static String paddingString;

    private DaoManager daoManager;

    public RangerMasterKey() {
    }

    public RangerMasterKey(DaoManager daoManager) {
        this.daoManager = daoManager;
    }

    protected static String getConfig(String key, String defaultValue) {
        String value = serverConfigProperties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            // Value not found in properties file, let's try to get from
            // System's property
            value = System.getProperty(key);
        }
        if (value == null || value.trim().isEmpty()) {
            value = defaultValue;
        }
        return value;
    }

    protected static int getIntConfig(String key, int defaultValue) {
        int ret = defaultValue;
        String retStr = serverConfigProperties.getProperty(key);
        try {
            if (retStr != null) {
                ret = Integer.parseInt(retStr);
            }
        } catch (Exception err) {
            logger.warn(retStr + " can't be parsed to int. Reason: " + err.toString());
        }
        return ret;
    }

    /**
     * To get Master Key
     *
     * @param password password to be used for decryption
     * @return Decrypted Master Key
     * @throws Throwable
     */
    @Override
    public String getMasterKey(String password) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.getMasterKey()");
        }
        logger.info("Getting Master Key");
        List result = getEncryptedMK();
        String encryptedPassString = null;
        byte masterKeyByte[] = null;
        if (CollectionUtils.isNotEmpty(result) && result.size() == 2) {
            masterKeyByte = (byte[]) result.get(0);
            encryptedPassString = (String) result.get(1);
        } else if (CollectionUtils.isNotEmpty(result)) {
            masterKeyByte = (byte[]) result.get(0);
        }
        if (masterKeyByte != null && masterKeyByte.length > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("<== RangerMasterKey.getMasterKey()");
            }
            return decryptMasterKey(masterKeyByte, password, encryptedPassString);
        } else {
            throw new Exception("No Master Key Found");
        }
    }

    public SecretKey getMasterSecretKey(String password) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.getMasterSecretKey()");
        }
        logger.info("Getting Master Key");
        List result = getEncryptedMK();
        String encryptedPassString = null;
        byte masterKeyByte[] = null;
        if (CollectionUtils.isNotEmpty(result) && result.size() == 2) {
            masterKeyByte = (byte[]) result.get(0);
            encryptedPassString = (String) result.get(1);
        } else if (CollectionUtils.isNotEmpty(result)) {
            masterKeyByte = (byte[]) result.get(0);
        }
        if (masterKeyByte != null && masterKeyByte.length > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("<== RangerMasterKey.getMasterSecretKey()");
            }
            return decryptMasterKeySK(masterKeyByte, password, encryptedPassString);
        } else {
            throw new Exception("No Master Key Found");
        }
    }

    /**
     * Generate the master key, encrypt it and save it in the database
     *
     * @param password password to be used for encryption
     * @return true if the master key was successfully created false if master
     * key generation was unsuccessful or the master key already exists
     * @throws Throwable
     */

    public void init() {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.init()");
        }
        XMLUtils.loadConfig(DBKS_SITE_XML, serverConfigProperties);
        DEFAULT_MD_ALGO = getConfig("ranger.keystore.file.type", KeyStore.getDefaultType()).equalsIgnoreCase("bcfks") ? "SHA-512" : "MD5";
        MK_CIPHER = getConfig("ranger.kms.service.masterkey.password.cipher", DEFAULT_MK_CIPHER);
        MK_KeySize = getIntConfig("ranger.kms.service.masterkey.password.size", DEFAULT_MK_KeySize);
        SALT_SIZE = getIntConfig("ranger.kms.service.masterkey.password.salt.size", DEFAULT_SALT_SIZE);
        SALT = getConfig("ranger.kms.service.masterkey.password.salt", DEFAULT_SALT);
        PBE_ALGO = getConfig("ranger.kms.service.masterkey.password.encryption.algorithm", DEFAULT_CRYPT_ALGO);
        MD_ALGO = getConfig("ranger.kms.service.masterkey.password.md.algorithm", DEFAULT_MD_ALGO);
        ITERATION_COUNT = getIntConfig("ranger.kms.service.masterkey.password.iteration.count",
                DEFAULT_ITERATION_COUNT);
        paddingString = Joiner.on(",").skipNulls().join(MK_CIPHER, MK_KeySize, SALT_SIZE, PBE_ALGO, MD_ALGO,
                ITERATION_COUNT, SALT);
    }

    @Override
    public boolean generateMasterKey(String password) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.generateMasterKey()");
        }
        logger.info("Generating Master Key...");
        init();
        String encryptedMasterKey = encryptMasterKey(password);
        String savedKey = saveEncryptedMK(paddingString + "," + encryptedMasterKey, daoManager);
        if (savedKey != null && !savedKey.trim().equals("")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Master Key Created with id = " + savedKey);
                logger.debug("<== RangerMasterKey.generateMasterKey()");
            }
            return true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.generateMasterKey()");
        }
        return false;
    }

    public boolean generateMKFromHSMMK(String password, byte[] key) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.generateMKFromHSMMK()");
        }
        init();
        String encryptedMasterKey = encryptMasterKey(password, key);
        String savedKey = saveEncryptedMK(paddingString + "," + encryptedMasterKey, daoManager);
        if (savedKey != null && !savedKey.trim().equals("")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Master Key Created with id = " + savedKey);
                logger.debug("<== RangerMasterKey.generateMKFromHSMMK()");
            }
            return true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.generateMKFromHSMMK()");
        }
        return false;
    }

    private String decryptMasterKey(byte masterKey[], String password, String encryptedPassString) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.decryptMasterKey()");
            logger.debug("Decrypting Master Key...");
        }
        if (encryptedPassString == null) {
            getPasswordParam(password);
        }
        PBEKeySpec pbeKeyspec = getPBEParameterSpec(password);
        byte[] masterKeyFromDBDecrypted = decryptKey(masterKey, pbeKeyspec);
        SecretKey masterKeyFromDB = getMasterKeyFromBytes(masterKeyFromDBDecrypted);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.decryptMasterKey()");
        }
        return Base64.encode(masterKeyFromDB.getEncoded());
    }

    public static void getPasswordParam(String paddedEncryptedPwd) {
        String[] encryptedPwd = null;
        if (paddedEncryptedPwd != null && paddedEncryptedPwd.contains(",")) {
            encryptedPwd = Lists.newArrayList(Splitter.on(",").split(paddedEncryptedPwd)).toArray(new String[0]);
        }
        if (encryptedPwd != null && encryptedPwd.length >= 7) {
            int index = 0;
            MK_CIPHER = encryptedPwd[index];
            MK_KeySize = Integer.parseInt(encryptedPwd[++index]);
            SALT_SIZE = Integer.parseInt(encryptedPwd[++index]);
            PBE_ALGO = encryptedPwd[++index];
            MD_ALGO = encryptedPwd[++index];
            ITERATION_COUNT = Integer.parseInt(encryptedPwd[++index]);
            SALT = encryptedPwd[++index];
            password = encryptedPwd[++index];
        } else {
            MK_CIPHER = DEFAULT_MK_CIPHER;
            MK_KeySize = DEFAULT_MK_KeySize;
            SALT_SIZE = DEFAULT_SALT_SIZE;
            PBE_ALGO = DEFAULT_CRYPT_ALGO;
            MD_ALGO = DEFAULT_MD_ALGO;
            password = paddedEncryptedPwd;
            SALT = password;
            if (password != null) {
                ITERATION_COUNT = password.toCharArray().length + 1;
            }
        }
    }

        public boolean generateMKFromKeySecureMK(String password, byte[] key)
                        throws Throwable {
                if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.generateMKFromKeySecureMK()");
        }
                init();
                String encryptedMasterKey = encryptMasterKey(password, key);
                String savedKey = saveEncryptedMK(paddingString + "," + encryptedMasterKey, daoManager);
                if (savedKey != null && !savedKey.trim().equals("")) {
                        logger.debug("Master Key Created with id = " + savedKey);
                        return true;
                }
                if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.generateMKFromKeySecureMK()");
        }
                return false;
        }

    private SecretKey decryptMasterKeySK(byte masterKey[], String password, String encryptedPassString)
            throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.decryptMasterKeySK()");
        }
        if (encryptedPassString == null) {
            getPasswordParam(password);
        }
        PBEKeySpec pbeKeyspec = getPBEParameterSpec(password);
        byte[] masterKeyFromDBDecrypted = decryptKey(masterKey, pbeKeyspec);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.decryptMasterKeySK()");
        }
        return getMasterKeyFromBytes(masterKeyFromDBDecrypted);
    }

    private List getEncryptedMK() throws Base64DecodingException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.getEncryptedMK()");
        }
        try {
            if (daoManager != null) {
                ArrayList ret = new ArrayList<>();
                RangerMasterKeyDao rangerKMSDao = new RangerMasterKeyDao(daoManager);
                List<XXRangerMasterKey> lstRangerMasterKey = rangerKMSDao.getAll();
                if (lstRangerMasterKey.size() < 1) {
                    throw new Exception("No Master Key exists");
                } else if (lstRangerMasterKey.size() > 1) {
                    throw new Exception("More than one Master Key exists");
                } else {
                    XXRangerMasterKey rangerMasterKey = rangerKMSDao.getById(lstRangerMasterKey.get(0).getId());
                    String masterKeyStr = rangerMasterKey.getMasterKey();
                    if (masterKeyStr.contains(",")) {
                        getPasswordParam(masterKeyStr);
                        ret.add(Base64.decode(password));
                        ret.add(masterKeyStr);
                        if (logger.isDebugEnabled()) {
                            logger.debug("<== RangerMasterKey.getEncryptedMK()");
                        }
                        return ret;
                    } else {
                        ret.add(Base64.decode(masterKeyStr));
                        if (logger.isDebugEnabled()) {
                            logger.debug("<== RangerMasterKey.getEncryptedMK()");
                        }
                        return ret;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unable to Retrieving Master Key from database!!! or ", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.getEncryptedMK()");
        }
        return null;
    }

    private String saveEncryptedMK(String encryptedMasterKey, DaoManager daoManager) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.saveEncryptedMK()");
        }
        XXRangerMasterKey xxRangerMasterKey = new XXRangerMasterKey();
        xxRangerMasterKey.setCipher(MK_CIPHER);
        xxRangerMasterKey.setBitLength(MK_KeySize);
        xxRangerMasterKey.setMasterKey(encryptedMasterKey);
        try {
            if (daoManager != null) {
                RangerMasterKeyDao rangerKMSDao = new RangerMasterKeyDao(daoManager);
                Long l = rangerKMSDao.getAllCount();
                if (l < 1) {
                    XXRangerMasterKey rangerMasterKey = rangerKMSDao.create(xxRangerMasterKey);
                    if (logger.isDebugEnabled()) {
                        logger.debug("<== RangerMasterKey.saveEncryptedMK()");
                    }
                    return rangerMasterKey.getId().toString();
                }
            }
        } catch (Exception e) {
            logger.error("Error while saving master key in Database!!! ", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.saveEncryptedMK()");
        }
        return null;
    }

    private String encryptMasterKey(String password) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.encryptMasterKey()");
        }
        Key secretKey = generateMasterKey();
        PBEKeySpec pbeKeySpec = getPBEParameterSpec(password);
        byte[] masterKeyToDB = encryptKey(secretKey.getEncoded(), pbeKeySpec);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.encryptMasterKey()");
        }
        return Base64.encode(masterKeyToDB);
    }

    private String encryptMasterKey(String password, byte[] secretKey) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.encryptMasterKey()");
        }
        PBEKeySpec pbeKeySpec = getPBEParameterSpec(password);
        byte[] masterKeyToDB = encryptKey(secretKey, pbeKeySpec);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerMasterKey.encryptMasterKey()");
        }
        return Base64.encode(masterKeyToDB);
    }

    private Key generateMasterKey() throws NoSuchAlgorithmException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.generateMasterKey()");
        }
        KeyGenerator kg = KeyGenerator.getInstance(MK_CIPHER);
        kg.init(MK_KeySize);
        return kg.generateKey();
    }

    private PBEKeySpec getPBEParameterSpec(String password) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.getPBEParameterSpec()");
        }
        MessageDigest md = MessageDigest.getInstance(MD_ALGO);
        byte[] saltGen = md.digest(SALT.getBytes());
        byte[] salt = new byte[SALT_SIZE];
        System.arraycopy(saltGen, 0, salt, 0, SALT_SIZE);
        return new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT);
    }

    private byte[] encryptKey(byte[] data, PBEKeySpec keyspec) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.encryptKey()");
        }
        SecretKey key = getPasswordKey(keyspec);
        if (keyspec.getSalt() != null) {
            PBEParameterSpec paramSpec = new PBEParameterSpec(keyspec.getSalt(), keyspec.getIterationCount());
            Cipher c = Cipher.getInstance(key.getAlgorithm());
            c.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            return c.doFinal(data);
        }
        return null;
    }

    private SecretKey getPasswordKey(PBEKeySpec keyspec) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerMasterKey.getPasswordKey()");
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGO);
        return factory.generateSecret(keyspec);
    }

    private byte[] decryptKey(byte[] encrypted, PBEKeySpec keyspec) throws Throwable {
        SecretKey key = getPasswordKey(keyspec);
        if (keyspec.getSalt() != null) {
            PBEParameterSpec paramSpec = new PBEParameterSpec(keyspec.getSalt(), keyspec.getIterationCount());
            Cipher c = Cipher.getInstance(key.getAlgorithm());
            c.init(Cipher.DECRYPT_MODE, key, paramSpec);
            return c.doFinal(encrypted);
        }
        return null;
    }

    private SecretKey getMasterKeyFromBytes(byte[] keyData) throws Throwable {
        return new SecretKeySpec(keyData, MK_CIPHER);
    }

    public Map<String, String> getPropertiesWithPrefix(Properties props, String prefix) {
        Map<String, String> prefixedProperties = new HashMap<String, String>();

        if (props != null && prefix != null) {
            for (String key : props.stringPropertyNames()) {
                if (key == null) {
                    continue;
                }

                String val = props.getProperty(key);

                if (key.startsWith(prefix)) {
                    key = key.substring(prefix.length());

                    if (key != null) {
                        prefixedProperties.put(key, val);
                    }
                }
            }
        }

        return prefixedProperties;
    }
}
