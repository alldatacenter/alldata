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

import com.microsoft.azure.keyvault.KeyVaultClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.apache.hadoop.conf.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.crypto.key.KeyProvider.Metadata;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider.KeyMetadata;
import org.apache.ranger.entity.XXRangerKeyStore;
import org.apache.ranger.kms.dao.DaoManager;
import org.apache.ranger.kms.dao.RangerKMSDao;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the Database store implementation.
 */

public class RangerKeyStore extends KeyStoreSpi {
    private static final Logger logger = LoggerFactory.getLogger(RangerKeyStore.class);

    private static final String  KEY_METADATA            = "KeyMetadata";
    private static final String  KEY_NAME_VALIDATION     = "[a-z,A-Z,0-9](?!.*--)(?!.*__)(?!.*-_)(?!.*_-)[\\w\\-\\_]*";
    private static final Pattern pattern                 = Pattern.compile(KEY_NAME_VALIDATION);
    private static final String  AZURE_KEYVAULT_ENABLED  = "ranger.kms.azurekeyvault.enabled";
    private static final String  METADATA_FIELDNAME      = "metadata";
    private static final int     NUMBER_OF_BITS_PER_BYTE = 8;
    private static final String  SECRET_KEY_HASH_WORD    = "Apache Ranger";

    private final    RangerKMSDao        kmsDao;
    private final    RangerKMSMKI        masterKeyProvider;
    private final    boolean             keyVaultEnabled;
    private volatile Map<String, Object> keyEntries   = new ConcurrentHashMap<>();
    private final    Map<String, Object> deltaEntries = new ConcurrentHashMap<>();


    public RangerKeyStore(DaoManager daoManager) {
        this(daoManager, false, null);
    }

    public RangerKeyStore(DaoManager daoManager, Configuration conf, KeyVaultClient kvClient) {
        this.kmsDao            = daoManager != null ? daoManager.getRangerKMSDao() : null;
        this.masterKeyProvider = new RangerAzureKeyVaultKeyGenerator(conf, kvClient);
        this.keyVaultEnabled   = (conf != null && StringUtils.equalsIgnoreCase(conf.get(AZURE_KEYVAULT_ENABLED), "true"));
    }

    public RangerKeyStore(DaoManager daoManager, boolean keyVaultEnabled, RangerKMSMKI masterKeyProvider) {
        this.kmsDao            = daoManager != null ? daoManager.getRangerKMSDao() : null;
        this.masterKeyProvider = masterKeyProvider;
        this.keyVaultEnabled   = keyVaultEnabled;
    }

    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineGetKey({})", alias);
        }

        alias = convertAlias(alias);

        Object entry = keyEntries.get(alias);

        Key ret = null;

        if (entry instanceof SecretKeyEntry) {
            try {
                ret = unsealKey(((SecretKeyEntry) entry).sealedKey, password);
            } catch (Exception e) {
                logger.error("engineGetKey({}) error", alias, e);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineGetKey({}): ret={}", alias, ret);
        }

        return ret;
    }

    public byte[] engineGetDecryptedZoneKeyByte(String alias) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineGetDecryptedZoneKeyByte({})", alias);
        }

        alias = convertAlias(alias);

        Object entry = keyEntries.get(alias);

        byte[] ret = null;

        try {
            if (entry instanceof SecretKeyByteEntry) {
                SecretKeyByteEntry key = (SecretKeyByteEntry) entry;

                ret = masterKeyProvider.decryptZoneKey(key.key);
            }
        } catch (Exception ex) {
            throw new Exception("Error while decrypting zone key. Name : " + alias + " Error : " + ex);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineGetDecryptedZoneKeyByte({}): ret={}", alias, ret);
        }

        return ret;
    }

    public Key engineGetDecryptedZoneKey(String alias) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineGetDecryptedZoneKey({})", alias);
        }

        byte[]   decryptKeyByte = engineGetDecryptedZoneKeyByte(alias);
        Metadata metadata       = engineGetKeyMetadata(alias);
        Key      ret            = new KeyByteMetadata(metadata, decryptKeyByte);

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineGetDecryptedZoneKey({}): ret={}", alias, ret);
        }

        return ret;
    }

    public Metadata engineGetKeyMetadata(String alias) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineGetKeyMetadata({})", alias);
        }

        alias = convertAlias(alias);

        Object entry = keyEntries.get(alias);

        Metadata ret = null;

        if (entry instanceof SecretKeyByteEntry) {
            SecretKeyByteEntry  key           = (SecretKeyByteEntry) entry;
            ObjectMapper        mapper        = new ObjectMapper();
            Map<String, String> attributesMap = null;

            try {
                attributesMap = mapper.readValue(key.attributes, new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                logger.error("engineGetKeyMetadata({}): invalid attribute string data", alias, e);
            }

            ret = new Metadata(key.cipher_field, key.bit_length, key.description, attributesMap, key.date, key.version);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineGetKeyMetadata({}): ret={}", alias, ret);
        }

        return ret;
    }

    public void addSecureKeyByteEntry(String alias, Key key, String cipher, int bitLength, String description, int version, String attributes) throws KeyStoreException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> addSecureKeyByteEntry({})", alias);
        }

        SecretKeyByteEntry entry;

        try {
            entry = new SecretKeyByteEntry(masterKeyProvider.encryptZoneKey(key), cipher, bitLength, description, version, attributes);
        } catch (Exception e) {
            logger.error("addSecureKeyByteEntry({})", alias, e);

            throw new KeyStoreException(e.getMessage());
        }

        alias = convertAlias(alias);

        deltaEntries.put(alias, entry);
        keyEntries.put(alias, entry);

        if (logger.isDebugEnabled()) {
            logger.debug("<== addSecureKeyByteEntry({})", alias);
        }
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineGetCreationDate({})", alias);
        }

        alias = convertAlias(alias);

        Object entry = keyEntries.get(alias);
        Date   ret   = null;

        if (entry != null) {
            KeyEntry keyEntry = (KeyEntry) entry;

            if (keyEntry.date != null) {
                ret = new Date(keyEntry.date.getTime());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineGetCreationDate({}): ret={}", alias, ret);
        }

        return ret;
    }

    public void addKeyEntry(String alias, Key key, char[] password, String cipher, int bitLength, String description, int version, String attributes) throws KeyStoreException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> addKeyEntry({})", alias);
        }

        SecretKeyEntry entry;

        try {
            entry = new SecretKeyEntry(sealKey(key, password), cipher, bitLength, description, version, attributes);
        } catch (Exception e) {
            logger.error("addKeyEntry({}) error", alias, e);

            throw new KeyStoreException(e.getMessage());
        }

        alias = convertAlias(alias);

        deltaEntries.put(alias, entry);
        keyEntries.put(alias, entry);

        if (logger.isDebugEnabled()) {
            logger.debug("<== addKeyEntry({})", alias);
        }
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineDeleteEntry({})", alias);
        }

        alias = convertAlias(alias);

        dbOperationDelete(alias);

        keyEntries.remove(alias);
        deltaEntries.remove(alias);

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineDeleteEntry({})", alias);
        }
    }

    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(new HashSet<>(keyEntries.keySet()));
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        alias = convertAlias(alias);

        boolean ret = keyEntries.containsKey(alias);

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineContainsAlias({}): ret={}", alias, ret);
        }

        return ret;
    }

    @Override
    public int engineSize() {
        int ret = keyEntries.size();

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineSize(): ret={}", ret);
        }

        return ret;
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineStore()");
        }

        if (keyVaultEnabled) {
            for (Entry<String, Object> entry : deltaEntries.entrySet()) {
                Long               creationDate     = ((SecretKeyByteEntry) entry.getValue()).date.getTime();
                SecretKeyByteEntry secretSecureKey  = (SecretKeyByteEntry) entry.getValue();
                XXRangerKeyStore   xxRangerKeyStore = mapObjectToEntity(entry.getKey(), creationDate, secretSecureKey.key,
                                                                        secretSecureKey.cipher_field, secretSecureKey.bit_length,
                                                                        secretSecureKey.description, secretSecureKey.version,
                                                                        secretSecureKey.attributes);
                dbOperationStore(xxRangerKeyStore);
            }
        } else {
            // password is mandatory when storing
            if (password == null) {
                throw new IllegalArgumentException("Ranger Master Key can't be null");
            }

            MessageDigest md       = getKeyedMessageDigest(password);
            byte          digest[] = md.digest();

            for (Entry<String, Object> entry : deltaEntries.entrySet()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try (DataOutputStream   dos = new DataOutputStream(new DigestOutputStream(baos, md));
                     ObjectOutputStream oos = new ObjectOutputStream(dos)) {
                    oos.writeObject(((SecretKeyEntry) entry.getValue()).sealedKey);

                    dos.write(digest);
                    dos.flush();

                    Long             creationDate     = ((SecretKeyEntry) entry.getValue()).date.getTime();
                    SecretKeyEntry   secretKey        = (SecretKeyEntry) entry.getValue();
                    XXRangerKeyStore xxRangerKeyStore = mapObjectToEntity(entry.getKey(), creationDate, baos.toByteArray(),
                                                                          secretKey.cipher_field, secretKey.bit_length,
                                                                          secretKey.description, secretKey.version,
                                                                          secretKey.attributes);
                    dbOperationStore(xxRangerKeyStore);
                }
            }
        }

        deltaEntries.clear();

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineStore()");
        }
    }

    public void dbOperationStore(XXRangerKeyStore rangerKeyStore) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> dbOperationStore({})", rangerKeyStore.getAlias());
        }

        try {
            if (kmsDao != null) {
                XXRangerKeyStore xxRangerKeyStore = kmsDao.findByAlias(rangerKeyStore.getAlias());
                boolean          keyStoreExists   = true;

                if (xxRangerKeyStore == null) {
                    xxRangerKeyStore = new XXRangerKeyStore();
                    keyStoreExists   = false;
                }

                xxRangerKeyStore = mapToEntityBean(rangerKeyStore, xxRangerKeyStore);

                if (keyStoreExists) {
                    kmsDao.update(xxRangerKeyStore);
                } else {
                    kmsDao.create(xxRangerKeyStore);
                }
            }
        } catch (Exception e) {
            logger.error("dbOperationStore({}) error", rangerKeyStore.getAlias(), e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== dbOperationStore({})", rangerKeyStore.getAlias());
        }
    }

    @Override
    public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineLoad()");
        }

        List<XXRangerKeyStore> rangerKeyDetails = dbOperationLoad();

        if (rangerKeyDetails == null || rangerKeyDetails.size() < 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("RangerKeyStore might be null or key is not present in the database.");
            }

            return;
        }

        Map<String, Object> keyEntries = new ConcurrentHashMap<>();

        if (keyVaultEnabled) {
            for (XXRangerKeyStore rangerKey : rangerKeyDetails) {
                String             encodedStr  = rangerKey.getEncoded();
                byte[]             encodedByte = DatatypeConverter.parseBase64Binary(encodedStr);
                String             alias       = rangerKey.getAlias();
                SecretKeyByteEntry entry       = new SecretKeyByteEntry(new Date(rangerKey.getCreatedDate()), encodedByte,
                                                                        rangerKey.getCipher(), rangerKey.getBitLength(),
                                                                        rangerKey.getDescription(), rangerKey.getVersion(),
                                                                        rangerKey.getAttributes());

                logger.debug("engineLoad(): loaded key {}", rangerKey.getAlias());

                keyEntries.put(alias, entry);
            }
        } else {
            MessageDigest md = null;

            if (password != null) {
                md = getKeyedMessageDigest(password);
            }

            byte computed[] = {};

            if (md != null) {
                computed = md.digest();
            }

            for (XXRangerKeyStore rangerKey : rangerKeyDetails) {
                String encoded = rangerKey.getEncoded();
                byte[] data    = DatatypeConverter.parseBase64Binary(encoded);

                if (data != null && data.length > 0) {
                    stream = new ByteArrayInputStream(data);
                } else {
                    logger.error("No Key found for alias {}", rangerKey.getAlias());
                }

                if (computed != null) {
                    int counter = 0;

                    for (int i = computed.length - 1; i >= 0; i--) {
                        if (computed[i] != data[data.length - (1 + counter)]) {
                            Throwable t = new UnrecoverableKeyException("Password verification failed");

                            logger.error("Keystore was tampered with, or password was incorrect.", t);

                            throw new IOException("Keystore was tampered with, or password was incorrect", t);
                        } else {
                            counter++;
                        }
                    }
                }

                SealedObject sealedKey;

                // read the (entry creation) date
                // read the sealed key
                try (DataInputStream dis = password != null ? new DataInputStream(new DigestInputStream(stream, md)) : new DataInputStream(stream);
                     ObjectInputStream ois = new ObjectInputStream(dis)) {
                    sealedKey = (SealedObject) ois.readObject();
                } catch (ClassNotFoundException cnfe) {
                    throw new IOException(cnfe.getMessage());
                }

                SecretKeyEntry entry = new SecretKeyEntry(new Date(rangerKey.getCreatedDate()), sealedKey, rangerKey.getCipher(),
                                                          rangerKey.getBitLength(), rangerKey.getDescription(), rangerKey.getVersion(),
                                                          rangerKey.getAttributes());

                logger.debug("engineLoad(): loaded key {}", rangerKey.getAlias());

                // Add the entry to the list
                keyEntries.put(rangerKey.getAlias(), entry);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("engineLoad(): loaded {} keys", keyEntries.size());
        }

        this.keyEntries = keyEntries;

        if (logger.isDebugEnabled()) {
            logger.debug("engineLoad(): keyEntries switched with {} keys", keyEntries.size());
        }
    }

    @Override
    public void engineSetKeyEntry(String arg0, byte[] arg1, Certificate[] arg2) {
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        return null;
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        return null;
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        return null;
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        return false;
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        return false;
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) {
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
    }

    //
    // The method is created to support JKS migration (from hadoop-common KMS keystore to RangerKMS keystore)
    //
    public void engineLoadKeyStoreFile(InputStream stream, char[] storePass, char[] keyPass, char[] masterKey, String fileFormat) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineLoadKeyStoreFile()");
        }

        if (keyVaultEnabled) {
            try {
                KeyStore ks = KeyStore.getInstance(fileFormat);

                ks.load(stream, storePass);

                deltaEntries.clear();

                for (Enumeration<String> name = ks.aliases(); name.hasMoreElements();) {
                    final String    alias       = name.nextElement();
                    final String[]  aliasSplits = alias.split("@");
                    Key             k           = ks.getKey(alias, keyPass);
                    final String    cipher_field;
                    final int       bit_length;
                    final int       version;
                    final SecretKey secretKey;


                    if (k instanceof JavaKeyStoreProvider.KeyMetadata) {
                        JavaKeyStoreProvider.KeyMetadata keyMetadata = (JavaKeyStoreProvider.KeyMetadata) k;
                        Field                            f           = JavaKeyStoreProvider.KeyMetadata.class.getDeclaredField(METADATA_FIELDNAME);

                        f.setAccessible(true);

                        Metadata metadata = (Metadata) f.get(keyMetadata);

                        bit_length   = metadata.getBitLength();
                        cipher_field = metadata.getAlgorithm();
                        version      = metadata.getVersions();

                        Constructor<RangerKeyStoreProvider.KeyMetadata> constructor = RangerKeyStoreProvider.KeyMetadata.class.getDeclaredConstructor(Metadata.class);

                        constructor.setAccessible(true);

                        k = constructor.newInstance(metadata);

                        secretKey = new SecretKeySpec(k.getEncoded(), getAlgorithm(metadata.getAlgorithm()));
                    } else if (k instanceof KeyByteMetadata) {
                        Metadata metadata = ((KeyByteMetadata) k).metadata;

                        cipher_field = metadata.getCipher();
                        version      = metadata.getVersions();
                        bit_length   = metadata.getBitLength();

                        if (k.getEncoded() != null && k.getEncoded().length > 0) {
                            secretKey = new SecretKeySpec(k.getEncoded(), getAlgorithm(metadata.getAlgorithm()));
                        } else {
                            KeyGenerator keyGenerator = KeyGenerator.getInstance(getAlgorithm(metadata.getCipher()));

                            keyGenerator.init(metadata.getBitLength());

                            byte[] keyByte = keyGenerator.generateKey().getEncoded();

                            secretKey = new SecretKeySpec(keyByte, getAlgorithm(metadata.getCipher()));
                        }
                    } else if (k instanceof KeyMetadata) {
                        Metadata metadata = ((KeyMetadata) k).metadata;

                        bit_length   = metadata.getBitLength();
                        cipher_field = metadata.getCipher();
                        version      = metadata.getVersions();

                        if (k.getEncoded() != null && k.getEncoded().length > 0) {
                            secretKey = new SecretKeySpec(k.getEncoded(), getAlgorithm(metadata.getAlgorithm()));
                        } else {
                            KeyGenerator keyGenerator = KeyGenerator.getInstance(getAlgorithm(metadata.getCipher()));

                            keyGenerator.init(metadata.getBitLength());

                            byte[] keyByte = keyGenerator.generateKey().getEncoded();

                            secretKey = new SecretKeySpec(keyByte, getAlgorithm(metadata.getCipher()));
                        }
                    } else {
                        bit_length   = (k.getEncoded().length * NUMBER_OF_BITS_PER_BYTE);
                        cipher_field = k.getAlgorithm();

                        if (aliasSplits.length == 2) {
                            version = Integer.parseInt(aliasSplits[1]) + 1;
                        } else {
                            version = 1;
                        }

                        if (k.getEncoded() != null && k.getEncoded().length > 0) {
                            secretKey = new SecretKeySpec(k.getEncoded(), getAlgorithm(k.getAlgorithm()));
                        } else {
                            secretKey = null;
                        }
                    }

                    String keyName = aliasSplits[0];

                    validateKeyName(keyName);

                    String             attributes  = "{\"key.acl.name\":\"" + keyName + "\"}";
                    byte[]             key         = masterKeyProvider.encryptZoneKey(secretKey);
                    Date               date        = ks.getCreationDate(alias);
                    String             description = k.getFormat() + " - " + ks.getType();
                    SecretKeyByteEntry entry       = new SecretKeyByteEntry(date, key, cipher_field, bit_length, description, version, attributes);

                    deltaEntries.put(alias, entry);
                }
            } catch (Throwable t) {
                logger.error("Unable to load keystore file ", t);

                throw new IOException(t);
            }
        } else {
            try {
                KeyStore ks = KeyStore.getInstance(fileFormat);

                ks.load(stream, storePass);

                deltaEntries.clear();

                for (Enumeration<String> name = ks.aliases(); name.hasMoreElements();) {
                    String          alias       = name.nextElement();
                    final String[]  aliasSplits = alias.split("@");
                    Key             k           = ks.getKey(alias, keyPass);
                    final String    cipher_field;
                    final int       bit_length;
                    final int       version;

                    if (k instanceof JavaKeyStoreProvider.KeyMetadata) {
                        JavaKeyStoreProvider.KeyMetadata keyMetadata = (JavaKeyStoreProvider.KeyMetadata) k;
                        Field                            f           = JavaKeyStoreProvider.KeyMetadata.class.getDeclaredField(METADATA_FIELDNAME);

                        f.setAccessible(true);

                        Metadata metadata = (Metadata) f.get(keyMetadata);

                        bit_length   = metadata.getBitLength();
                        cipher_field = metadata.getAlgorithm();
                        version      = metadata.getVersions();

                        Constructor<RangerKeyStoreProvider.KeyMetadata> constructor = RangerKeyStoreProvider.KeyMetadata.class.getDeclaredConstructor(Metadata.class);

                        constructor.setAccessible(true);

                        k = constructor.newInstance(metadata);
                    } else if (k instanceof KeyMetadata) {
                        Metadata metadata = ((KeyMetadata) k).metadata;

                        bit_length   = metadata.getBitLength();
                        cipher_field = metadata.getCipher();
                        version      = metadata.getVersions();
                    } else {
                        bit_length   = (k.getEncoded().length * NUMBER_OF_BITS_PER_BYTE);
                        cipher_field = k.getAlgorithm();
                        version      = (aliasSplits.length == 2) ? (Integer.parseInt(aliasSplits[1]) + 1) : 1;
                    }

                    String keyName = aliasSplits[0];

                    validateKeyName(keyName);

                    SealedObject sealedKey;

                    try {
                        Class<?>       c           = Class.forName("com.sun.crypto.provider.KeyProtector");
                        Constructor<?> constructor = c.getDeclaredConstructor(char[].class);

                        constructor.setAccessible(true);

                        Object o = constructor.newInstance(masterKey);

                        // seal and store the key
                        Method m = c.getDeclaredMethod("seal", Key.class);

                        m.setAccessible(true);

                        sealedKey = (SealedObject) m.invoke(o, k);
                    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                             InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        logger.error(e.getMessage());

                        throw new IOException(e.getMessage());
                    }

                    String attributes  = "{\"key.acl.name\":\"" + keyName + "\"}";
                    String description = k.getFormat() + " - " + ks.getType();

                    SecretKeyEntry entry = new SecretKeyEntry(ks.getCreationDate(alias), sealedKey, cipher_field, bit_length, description, version, attributes);

                    deltaEntries.put(alias, entry);
                }
            } catch (Throwable t) {
                logger.error("Unable to load keystore file ", t);
                throw new IOException(t);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== engineLoadKeyStoreFile()");
        }
    }

    public void engineLoadToKeyStoreFile(OutputStream stream, char[] storePass, char[] keyPass, char[] masterKey, String fileFormat) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> engineLoadToKeyStoreFile()");
        }

        try {
            KeyStore ks = KeyStore.getInstance(fileFormat);

            if (ks != null) {
                ks.load(null, storePass);

                engineLoad(null, masterKey);

                for (Enumeration<String> e = engineAliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    Key    key;

                    if(keyVaultEnabled){
                        key = engineGetDecryptedZoneKey(alias);
                    } else {
                        key = engineGetKey(alias, masterKey);

                        if (key instanceof KeyMetadata) {
                            Metadata meta = ((KeyMetadata) key).metadata;

                            if (meta != null) {
                                key = new KeyMetadata(meta);
                            }
                        }
                    }

                    ks.setKeyEntry(alias, key, keyPass, null);
                }

                ks.store(stream, storePass);
            }
        } catch (Throwable t) {
            logger.error("Unable to load keystore file", t);

            throw new IOException(t);
        }
    }

    public XXRangerKeyStore convertKeysBetweenRangerKMSAndGCP(String alias, Key key, RangerKMSMKI rangerGCPProvider) {
        return this.convertKeysBetweenRangerKMSAndHSM(alias, key, rangerGCPProvider);
    }

    public XXRangerKeyStore convertKeysBetweenRangerKMSAndAzureKeyVault(String alias, Key key, RangerKMSMKI rangerKVKeyGenerator) {
        return this.convertKeysBetweenRangerKMSAndHSM(alias, key, rangerKVKeyGenerator);
    }

    public String getAlgorithm(String cipher) {
        int slash = cipher.indexOf(47);

        if (slash == -1) {
            return cipher;
        }

        return cipher.substring(0, slash);
    }

    private void validateKeyName(String name) {
        Matcher matcher = pattern.matcher(name);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Key Name : " + name + ", should start with alpha/numeric letters and can have special characters - (hypen) or _ (underscore)");
        }
    }

    private Object getKeyEntry(String alias) {
        return keyEntries.get(alias);
    }

    private XXRangerKeyStore convertKeysBetweenRangerKMSAndHSM(String alias, Key key, RangerKMSMKI rangerMKeyProvider) {
        try {
            SecretKeyEntry   secretKey = (SecretKeyEntry) getKeyEntry(alias);
            XXRangerKeyStore xxRangerKeyStore;

            if (key instanceof KeyMetadata) {
                Metadata     meta         = ((KeyMetadata) key).metadata;
                KeyGenerator keyGenerator = KeyGenerator.getInstance(getAlgorithm(meta.getCipher()));

                keyGenerator.init(meta.getBitLength());

                byte[] keyByte      = keyGenerator.generateKey().getEncoded();
                Key    ezkey        = new SecretKeySpec(keyByte, getAlgorithm(meta.getCipher()));
                byte[] encryptedKey = rangerMKeyProvider.encryptZoneKey(ezkey);
                Long   creationDate = new Date().getTime();
                String attributes   = secretKey.attributes;

                xxRangerKeyStore = mapObjectToEntity(alias, creationDate, encryptedKey, meta.getCipher(),
                                                     meta.getBitLength(), meta.getDescription(), meta.getVersions(), attributes);
            } else {
                byte[]   encryptedKey = rangerMKeyProvider.encryptZoneKey(key);
                Long     creationDate = secretKey.date.getTime();
                int      version      = secretKey.version;
                String[] aliasSplits  = alias.split("@");

                if ((aliasSplits.length == 2) && (((Integer.parseInt(aliasSplits[1])) + 1) != secretKey.version)) {
                    version++;
                }

                xxRangerKeyStore = mapObjectToEntity(alias, creationDate, encryptedKey, secretKey.cipher_field,
                                                     secretKey.bit_length, secretKey.description, version, secretKey.attributes);
            }

            return xxRangerKeyStore;
        } catch (Throwable t) {
            throw new RuntimeException("Migration failed between key secure and Ranger DB : ", t);
        }
    }

    private XXRangerKeyStore mapObjectToEntity(String alias, Long creationDate, byte[] byteArray, String cipher_field, int bit_length, String description, int version, String attributes) {
        XXRangerKeyStore xxRangerKeyStore = new XXRangerKeyStore();

        xxRangerKeyStore.setAlias(alias);
        xxRangerKeyStore.setCreatedDate(creationDate);
        xxRangerKeyStore.setEncoded(DatatypeConverter.printBase64Binary(byteArray));
        xxRangerKeyStore.setCipher(cipher_field);
        xxRangerKeyStore.setBitLength(bit_length);
        xxRangerKeyStore.setDescription(description);
        xxRangerKeyStore.setVersion(version);
        xxRangerKeyStore.setAttributes(attributes);

        return xxRangerKeyStore;
    }

    private void dbOperationDelete(String alias) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> dbOperationDelete({})", alias);
        }

        try {
            if (kmsDao != null) {
                kmsDao.deleteByAlias(alias);
            }
        } catch (Exception e) {
            logger.error("dbOperationDelete({}) error", alias, e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== dbOperationDelete({})", alias);
        }
    }

    private XXRangerKeyStore mapToEntityBean(XXRangerKeyStore rangerKMSKeyStore, XXRangerKeyStore xxRangerKeyStore) {
        xxRangerKeyStore.setAlias(rangerKMSKeyStore.getAlias());
        xxRangerKeyStore.setCreatedDate(rangerKMSKeyStore.getCreatedDate());
        xxRangerKeyStore.setEncoded(rangerKMSKeyStore.getEncoded());
        xxRangerKeyStore.setCipher(rangerKMSKeyStore.getCipher());
        xxRangerKeyStore.setBitLength(rangerKMSKeyStore.getBitLength());
        xxRangerKeyStore.setDescription(rangerKMSKeyStore.getDescription());
        xxRangerKeyStore.setVersion(rangerKMSKeyStore.getVersion());
        xxRangerKeyStore.setAttributes(rangerKMSKeyStore.getAttributes());

        return xxRangerKeyStore;
    }

    private List<XXRangerKeyStore> dbOperationLoad() {
        if (logger.isDebugEnabled()) {
            logger.debug("==> dbOperationLoad()");
        }

        List<XXRangerKeyStore> ret = null;

        try {
            if (kmsDao != null) {
                ret = kmsDao.getAllKeys();
            }
        } catch (Exception e) {
            logger.error("dbOperationLoad() error", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== dbOperationLoad(): count={}", (ret != null ? ret.size() : 0));
        }

        return ret;
    }

    /**
     * To guard against tampering with the keystore, we append a keyed
     * hash with a bit of whitener.
     */
    private MessageDigest getKeyedMessageDigest(char[] aKeyPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md               = MessageDigest.getInstance("SHA");
        byte[]        keyPasswordBytes = new byte[aKeyPassword.length * 2];

        for (int i = 0, j = 0; i < aKeyPassword.length; i++) {
            keyPasswordBytes[j++] = (byte) (aKeyPassword[i] >> 8);
            keyPasswordBytes[j++] = (byte) aKeyPassword[i];
        }

        md.update(keyPasswordBytes);

        Arrays.fill(keyPasswordBytes, (byte) 0);

        md.update(SECRET_KEY_HASH_WORD.getBytes(StandardCharsets.UTF_8));

        return md;
    }

    private String convertAlias(String alias) {
        return alias.toLowerCase();
    }

    private SealedObject sealKey(Key key, char[] password) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> sealKey()");
        }

        // Create SecretKey
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndTripleDES");
        PBEKeySpec       pbeKeySpec       = new PBEKeySpec(password);
        SecretKey        secretKey        = secretKeyFactory.generateSecret(pbeKeySpec);

        pbeKeySpec.clearPassword();

        // Generate random bytes, set up the PBEParameterSpec, seal the key
        SecureRandom random = new SecureRandom();
        byte[]       salt   = new byte[8];

        random.nextBytes(salt);

        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, 20);
        Cipher           cipher  = Cipher.getInstance("PBEWithMD5AndTripleDES");

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, pbeSpec);

        RangerSealedObject ret = new RangerSealedObject(key, cipher);

        if (logger.isDebugEnabled()) {
            logger.debug("<== sealKey(): ret={}", ret);
        }

        return ret;
    }

    private Key unsealKey(SealedObject sealedKey, char[] password) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> unsealKey()");
        }

        // Create SecretKey
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndTripleDES");
        PBEKeySpec       pbeKeySpec       = new PBEKeySpec(password);
        SecretKey        secretKey        = secretKeyFactory.generateSecret(pbeKeySpec);

        pbeKeySpec.clearPassword();

        // Get the AlgorithmParameters from RangerSealedObject
        AlgorithmParameters algorithmParameters;

        if (sealedKey instanceof RangerSealedObject) {
            algorithmParameters = ((RangerSealedObject) sealedKey).getParameters();
        } else {
            algorithmParameters = new RangerSealedObject(sealedKey).getParameters();
        }

        // Unseal the Key
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndTripleDES");

        cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameters);

        Key ret = (Key) sealedKey.getObject(cipher);

        if (logger.isDebugEnabled()) {
            logger.debug("<== unsealKey(): ret={}", ret);
        }

        return ret;
    }

    // keys
    private static class KeyEntry {
        Date date = new Date(); // the creation date of this entry
    }

    // Secret key
    private static final class SecretKeyEntry {
        final Date         date; // the creation date of this entry
        final SealedObject sealedKey;
        final String       cipher_field;
        final int          bit_length;
        final String       description;
        final String       attributes;
        final int          version;

        SecretKeyEntry(SealedObject sealedKey, String cipher, int bitLength, String description, int version, String attributes) {
            this(new Date(), sealedKey, cipher, bitLength, description, version, attributes);
        }

        SecretKeyEntry(Date date, SealedObject sealedKey, String cipher, int bitLength, String description, int version, String attributes) {
            this.date         = date;
            this.sealedKey    = sealedKey;
            this.cipher_field = cipher;
            this.bit_length   = bitLength;
            this.description  = description;
            this.version      = version;
            this.attributes   = attributes;
        }
    }

    private static final class SecretKeyByteEntry {
        final Date   date;
        final byte[] key;
        final String cipher_field;
        final int    bit_length;
        final String description;
        final String attributes;
        final int    version;

        SecretKeyByteEntry(byte[] key, String ciper, int bitLength, String description, int version, String attributes) {
            this(new Date(), key, ciper, bitLength, description, version, attributes);
        }

        SecretKeyByteEntry(Date date, byte[] key, String ciper, int bitLength, String description, int version, String attributes) {
            this.date         = date;
            this.key          = key;
            this.cipher_field = ciper;
            this.bit_length   = bitLength;
            this.description  = description;
            this.version      = version;
            this.attributes   = attributes;

        }
    }

    /**
     * Encapsulate the encrypted key, so that we can retrieve the AlgorithmParameters object on the decryption side
     */
    private static class RangerSealedObject extends SealedObject {
        private static final long serialVersionUID = -7551578543434362070L;

        /**
         *
         */
        protected RangerSealedObject(SealedObject so) {
            super(so);
        }

        protected RangerSealedObject(Serializable object, Cipher cipher) throws IllegalBlockSizeException, IOException {
            super(object, cipher);
        }

        public AlgorithmParameters getParameters() throws NoSuchAlgorithmException, IOException {
            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("PBEWithMD5AndTripleDES");

            algorithmParameters.init(super.encodedParams);

            return algorithmParameters;
        }

    }

    public static class KeyByteMetadata implements Key, Serializable {
        private Metadata metadata;
        private byte[]   keyByte;

        private final static long serialVersionUID = 8405872419967874451L;

        private KeyByteMetadata(Metadata meta, byte[] encoded) {
            this.metadata = meta;
            this.keyByte  = encoded;
        }

        @Override
        public String getAlgorithm() {
            return metadata.getCipher();
        }

        @Override
        public String getFormat() {
            return KEY_METADATA;
        }

        @Override
        public byte[] getEncoded() {
            return this.keyByte;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            byte[] serialized = metadata.serialize();

            out.writeInt(serialized.length);
            out.write(serialized);
            out.writeInt(keyByte.length);
            out.write(keyByte);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
              byte[] metadataBuf = new byte[in.readInt()];

              in.readFully(metadataBuf);

              byte[] keybyteBuf = new byte[in.readInt()];

              in.readFully(keybyteBuf);

              metadata = new Metadata(metadataBuf);
            keyByte = keybyteBuf;
        }
    }
}
