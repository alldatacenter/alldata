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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the Database store implementation.
 */

public class RangerKeyStore extends KeyStoreSpi {

    static final Logger logger = LoggerFactory.getLogger(RangerKeyStore.class);
    private static final String KEY_METADATA = "KeyMetadata";
    private static final String KEY_NAME_VALIDATION = "[a-z,A-Z,0-9](?!.*--)(?!.*__)(?!.*-_)(?!.*_-)[\\w\\-\\_]*";
    private static final Pattern pattern = Pattern.compile(KEY_NAME_VALIDATION);
    private static final String AZURE_KEYVAULT_ENABLED = "ranger.kms.azurekeyvault.enabled";
    private boolean keyVaultEnabled = false;

    private DaoManager daoManager;
    private RangerKMSMKI masterKeyProvider;

    // keys
    private static class KeyEntry {
        Date date = new Date(); // the creation date of this entry
    }

    // Secret key
	private static final class SecretKeyEntry {
		Date date = new Date(); // the creation date of this entry
		SealedObject sealedKey;
		String cipher_field;
		int bit_length;
		String description;
		String attributes;
		int version;
	}

	private static final class SecretKeyByteEntry {
		Date date = new Date();
		byte[] key;
		String cipher_field;
		int bit_length;
		String description;
		String attributes;
		int version;
	}

    private Map<String, Object> keyEntries = new ConcurrentHashMap<>();
    private Map<String, Object> deltaEntries = new ConcurrentHashMap<>();

    public RangerKeyStore() {
    }

    public RangerKeyStore(DaoManager daoManager) {
        this.daoManager = daoManager;
    }

    public RangerKeyStore(DaoManager daoManager, Configuration conf, KeyVaultClient kvClient) {
        this.daoManager = daoManager;
        this.masterKeyProvider = new RangerAzureKeyVaultKeyGenerator(conf, kvClient);
        if(conf != null
				&& StringUtils.isNotEmpty(conf
						.get(AZURE_KEYVAULT_ENABLED))
				&& conf.get(AZURE_KEYVAULT_ENABLED).equalsIgnoreCase(
						"true")){
            keyVaultEnabled = true;
        }
    }

    public RangerKeyStore(DaoManager daoManager, boolean keyVaultEnabled, RangerKMSMKI masterKeyProvider) {
        this.daoManager = daoManager;
        this.masterKeyProvider = masterKeyProvider;
        this.keyVaultEnabled = keyVaultEnabled;
    }

    String convertAlias(String alias) {
        return alias.toLowerCase();
    }

    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.engineGetKey()");
        }
        Key key = null;
        Object entry = keyEntries.get(convertAlias(alias));

        if (!(entry instanceof SecretKeyEntry)) {
            return null;
        }
        try {
            key = unsealKey(((SecretKeyEntry) entry).sealedKey, password);
        } catch (Exception e) {
            logger.error("==> RangerKeyStore.engineGetKey() error: ", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerKeyStore.engineGetKey()");
        }
        return key;
    }
    
	public byte[] engineGetDecryptedZoneKeyByte(String alias) throws Exception {
		try {
			Object entry = keyEntries.get(convertAlias(alias));
			if (!(entry instanceof SecretKeyByteEntry)) {
				return null;
			}
			SecretKeyByteEntry key = (SecretKeyByteEntry) entry;
			byte[] decryptKeyByte = masterKeyProvider.decryptZoneKey(key.key);
			return decryptKeyByte;
		} catch (Exception ex) {
			throw new Exception("Error while decrpting zone key. Name : "
					+ alias + " Error : " + ex);
		}
	}
	
	public Key engineGetDecryptedZoneKey(String alias) throws Exception {
		byte[] decryptKeyByte = engineGetDecryptedZoneKeyByte(alias);
		Metadata metadata = engineGetKeyMetadata(alias); 
		Key k = new KeyByteMetadata(metadata, decryptKeyByte);
		return k;
	}
	
	public Metadata engineGetKeyMetadata(String alias) {
		Object entry = keyEntries.get(convertAlias(alias));
		if (!(entry instanceof SecretKeyByteEntry)) {
			return null;
		}
		SecretKeyByteEntry key = (SecretKeyByteEntry) entry;
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> attributesMap = null;
		try {
			attributesMap = mapper.readValue(key.attributes,
					new TypeReference<Map<String, String>>() {
					});
		} catch (JsonParseException e) {
			logger.error("Invalid attribute string data: " + e.getMessage());

		} catch (JsonMappingException e) {
			logger.error("Invalid attribute string data: " + e.getMessage());
		} catch (IOException e) {
			logger.error("Invalid attribute string data: " + e.getMessage());
		}
		Metadata meta = new Metadata(key.cipher_field, key.bit_length,
				key.description, attributesMap, key.date, key.version);
		return meta;
	}
	
	public void addSecureKeyByteEntry(String alias, Key key, String cipher,
			int bitLength, String description, int version, String attributes)
			throws KeyStoreException {
		SecretKeyByteEntry entry = new SecretKeyByteEntry();
		synchronized (deltaEntries) {
			try {
				entry.date = new Date();
				// encrypt and store the key
				entry.key = masterKeyProvider.encryptZoneKey(key);
				entry.cipher_field = cipher;
				entry.bit_length = bitLength;
				entry.description = description;
				entry.version = version;
				entry.attributes = attributes;
				deltaEntries.put(convertAlias(alias), entry);

			} catch (Exception e) {
				logger.error(e.getMessage());
				throw new KeyStoreException(e.getMessage());
			}
		}
		synchronized (keyEntries) {
			try {
				keyEntries.put(convertAlias(alias), entry);
			} catch (Exception e) {
				logger.error(e.getMessage());
				throw new KeyStoreException(e.getMessage());
			}
		}
	}
	
    @Override
    public Date engineGetCreationDate(String alias) {
        Object entry = keyEntries.get(convertAlias(alias));
        Date date = null;
        if (entry != null) {
            KeyEntry keyEntry = (KeyEntry) entry;
            if (keyEntry.date != null) {
                date = new Date(keyEntry.date.getTime());
            }
        }
        return date;
    }

    public void addKeyEntry(String alias, Key key, char[] password, String cipher, int bitLength, String description, int version, String attributes)
            throws KeyStoreException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.addKeyEntry()");
            logger.debug("Adding entry for alias:" + alias);
        }
        SecretKeyEntry entry = new SecretKeyEntry();
        synchronized (deltaEntries) {
            try {
                entry.date = new Date();
                // seal and store the key
                entry.sealedKey = sealKey(key, password);

                entry.cipher_field = cipher;
                entry.bit_length = bitLength;
                entry.description = description;
                entry.version = version;
                entry.attributes = attributes;
                deltaEntries.put(convertAlias(alias), entry);
            } catch (Exception e) {
                logger.error("==> RangerKeyStore.addKeyEntry() error: ", e);
                throw new KeyStoreException(e.getMessage());
            }
        }
        synchronized (keyEntries) {
            try {
                keyEntries.put(convertAlias(alias), entry);
            } catch (Exception e) {
                logger.error("==> RangerKeyStore.addKeyEntry() error: ", e);
                throw new KeyStoreException(e.getMessage());
            }
        }
    }

    private SealedObject sealKey(Key key, char[] password) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.sealKey()");
        }
        // Create SecretKey
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndTripleDES");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
        pbeKeySpec.clearPassword();

        // Generate random bytes + set up the PBEParameterSpec
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, 20);

        // Seal the Key
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndTripleDES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, pbeSpec);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerKeyStore.sealKey()");
        }
        return new RangerSealedObject(key, cipher);
    }

    private Key unsealKey(SealedObject sealedKey, char[] password) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.unsealKey()");
        }
        // Create SecretKey
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndTripleDES");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
        pbeKeySpec.clearPassword();

        // Get the AlgorithmParameters from RangerSealedObject
        AlgorithmParameters algorithmParameters = null;
        if (sealedKey instanceof RangerSealedObject) {
            algorithmParameters = ((RangerSealedObject) sealedKey).getParameters();
        } else {
            algorithmParameters = new RangerSealedObject(sealedKey).getParameters();
        }

        // Unseal the Key
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndTripleDES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameters);
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerKeyStore.unsealKey()");
        }
        return (Key) sealedKey.getObject(cipher);
    }

    @Override
    public void engineDeleteEntry(String alias)
            throws KeyStoreException {
        synchronized (keyEntries) {
            dbOperationDelete(convertAlias(alias));
            keyEntries.remove(convertAlias(alias));
        }
        synchronized (deltaEntries) {
            deltaEntries.remove(convertAlias(alias));
        }
    }


    private void dbOperationDelete(String alias) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.dbOperationDelete(" + alias + ")");
        }
        try {
            if (daoManager != null) {
                RangerKMSDao rangerKMSDao = new RangerKMSDao(daoManager);
                rangerKMSDao.deleteByAlias(alias);
            }
        } catch (Exception e) {
            logger.error("==> RangerKeyStore.dbOperationDelete() error : ", e);
        }
    }


    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(keyEntries.keySet());
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        return keyEntries.containsKey(convertAlias(alias));
    }

    @Override
    public int engineSize() {
        return keyEntries.size();
    }

    @Override
    public void engineStore(OutputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		if (logger.isDebugEnabled()) {
			logger.debug("==> RangerKeyStore.engineStore()");
		}
		synchronized (deltaEntries) {
			if (keyVaultEnabled) {
				for (Entry<String, Object> entry : deltaEntries.entrySet()) {
					Long creationDate = ((SecretKeyByteEntry) entry.getValue()).date
							.getTime();
					SecretKeyByteEntry secretSecureKey = (SecretKeyByteEntry) entry
							.getValue();
					XXRangerKeyStore xxRangerKeyStore = mapObjectToEntity(
							entry.getKey(), creationDate, secretSecureKey.key,
							secretSecureKey.cipher_field,
							secretSecureKey.bit_length,
							secretSecureKey.description,
							secretSecureKey.version, secretSecureKey.attributes);
					dbOperationStore(xxRangerKeyStore);
				}

			} else {
				// password is mandatory when storing
				if (password == null) {
					throw new IllegalArgumentException(
							"Ranger Master Key can't be null");
				}

				MessageDigest md = getKeyedMessageDigest(password);

				byte digest[] = md.digest();
				for (Entry<String, Object> entry : deltaEntries.entrySet()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(
							new DigestOutputStream(baos, md));

					ObjectOutputStream oos = null;
					try {

						oos = new ObjectOutputStream(dos);
						oos.writeObject(((SecretKeyEntry) entry.getValue()).sealedKey);

						dos.write(digest);
						dos.flush();
						Long creationDate = ((SecretKeyEntry) entry.getValue()).date
								.getTime();
						SecretKeyEntry secretKey = (SecretKeyEntry) entry
								.getValue();
						XXRangerKeyStore xxRangerKeyStore = mapObjectToEntity(
								entry.getKey(), creationDate,
								baos.toByteArray(), secretKey.cipher_field,
								secretKey.bit_length, secretKey.description,
								secretKey.version, secretKey.attributes);
						dbOperationStore(xxRangerKeyStore);
					} finally {
						if (oos != null) {
							oos.close();
						} else {
							dos.close();
						}
					}
				}
			}
			clearDeltaEntires();
		}
	}

    private XXRangerKeyStore mapObjectToEntity(String alias, Long creationDate,
                                               byte[] byteArray, String cipher_field, int bit_length,
                                               String description, int version, String attributes) {
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

    public void dbOperationStore(XXRangerKeyStore rangerKeyStore) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.dbOperationStore()");
        }
        try {
            if (daoManager != null) {
                RangerKMSDao rangerKMSDao = new RangerKMSDao(daoManager);
                XXRangerKeyStore xxRangerKeyStore = rangerKMSDao.findByAlias(rangerKeyStore.getAlias());
                boolean keyStoreExists = true;
                if (xxRangerKeyStore == null) {
                    xxRangerKeyStore = new XXRangerKeyStore();
                    keyStoreExists = false;
                }
                xxRangerKeyStore = mapToEntityBean(rangerKeyStore, xxRangerKeyStore);
                if (keyStoreExists) {
                    xxRangerKeyStore = rangerKMSDao.update(xxRangerKeyStore);
                } else {
                    xxRangerKeyStore = rangerKMSDao.create(xxRangerKeyStore);
                }
            }
        } catch (Exception e) {
            logger.error("==> RangerKeyStore.dbOperationStore() error : ", e);
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


    @Override
    public void engineLoad(InputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		if (logger.isDebugEnabled()) {
			logger.debug("==> RangerKeyStore.engineLoad()");
		}

		synchronized (keyEntries) {
			List<XXRangerKeyStore> rangerKeyDetails = dbOperationLoad();

			if (rangerKeyDetails == null || rangerKeyDetails.size() < 1) {
				if (logger.isDebugEnabled()) {
					logger.debug("RangerKeyStore might be null or key is not present in the database.");
				}
				return;
			}

			keyEntries.clear();
			if (keyVaultEnabled) {
				for (XXRangerKeyStore rangerKey : rangerKeyDetails) {
					String encodedStr = rangerKey.getEncoded();
					byte[] encodedByte = DatatypeConverter
							.parseBase64Binary(encodedStr);
					String alias;
					SecretKeyByteEntry entry = new SecretKeyByteEntry();
					alias = rangerKey.getAlias();
					entry.date = new Date(rangerKey.getCreatedDate());
					entry.cipher_field = rangerKey.getCipher();
					entry.bit_length = rangerKey.getBitLength();
					entry.description = rangerKey.getDescription();
					entry.version = rangerKey.getVersion();
					entry.attributes = rangerKey.getAttributes();
					entry.key = encodedByte;
					keyEntries.put(alias, entry);
				}
			} else {
				DataInputStream dis;
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
					byte[] data = DatatypeConverter.parseBase64Binary(encoded);

					if (data != null && data.length > 0) {
						stream = new ByteArrayInputStream(data);
					} else {
						logger.error("No Key found for alias "
								+ rangerKey.getAlias());
					}

					if (computed != null) {
						int counter = 0;
						for (int i = computed.length - 1; i >= 0; i--) {
							if (computed[i] != data[data.length - (1 + counter)]) {
								Throwable t = new UnrecoverableKeyException(
										"Password verification failed");
								logger.error(
										"Keystore was tampered with, or password was incorrect.",
										t);
								throw (IOException) new IOException(
										"Keystore was tampered with, or "
												+ "password was incorrect")
										.initCause(t);
							} else {
								counter++;
							}
						}
					}

					if (password != null) {
						dis = new DataInputStream(new DigestInputStream(stream,
								md));
					} else {
						dis = new DataInputStream(stream);
					}

					ObjectInputStream ois = null;
					try {
						String alias;

						SecretKeyEntry entry = new SecretKeyEntry();

						// read the alias
						alias = rangerKey.getAlias();

						// read the (entry creation) date
						entry.date = new Date(rangerKey.getCreatedDate());
						entry.cipher_field = rangerKey.getCipher();
						entry.bit_length = rangerKey.getBitLength();
						entry.description = rangerKey.getDescription();
						entry.version = rangerKey.getVersion();
						entry.attributes = rangerKey.getAttributes();
						// read the sealed key
						try {
							ois = new ObjectInputStream(dis);
							entry.sealedKey = (SealedObject) ois.readObject();
						} catch (ClassNotFoundException cnfe) {
							throw new IOException(cnfe.getMessage());
						}
						// Add the entry to the list
						keyEntries.put(alias, entry);
					} finally {
						if (ois != null) {
							ois.close();
						} else {
							dis.close();
						}
					}
				}
			}
		}
	}

    private List<XXRangerKeyStore> dbOperationLoad() throws IOException {
    	if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStore.dbOperationLoad()");
        }
        try {
            if (daoManager != null) {
                RangerKMSDao rangerKMSDao = new RangerKMSDao(daoManager);
                if (logger.isDebugEnabled()) {
                    logger.debug("<== RangerKeyStore.dbOperationLoad()");
                }
                return rangerKMSDao.getAllKeys();
            }
        } catch (Exception e) {
            logger.error("==> RangerKeyStore.dbOperationLoad() error:", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("<== RangerKeyStore.dbOperationLoad()");
        }
        return null;
    }

    /**
     * To guard against tampering with the keystore, we append a keyed
     * hash with a bit of whitener.
     */

    private final String SECRET_KEY_HASH_WORD = "Apache Ranger";

    private MessageDigest getKeyedMessageDigest(char[] aKeyPassword)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        int i, j;

        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] keyPasswordBytes = new byte[aKeyPassword.length * 2];
        for (i = 0, j = 0; i < aKeyPassword.length; i++) {
            keyPasswordBytes[j++] = (byte) (aKeyPassword[i] >> 8);
            keyPasswordBytes[j++] = (byte) aKeyPassword[i];
        }
        md.update(keyPasswordBytes);
        for (i = 0; i < keyPasswordBytes.length; i++)
            keyPasswordBytes[i] = 0;
        md.update(SECRET_KEY_HASH_WORD.getBytes("UTF8"));
        return md;
    }

    @Override
    public void engineSetKeyEntry(String arg0, byte[] arg1, Certificate[] arg2)
            throws KeyStoreException {
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
    public void engineSetCertificateEntry(String alias, Certificate cert)
            throws KeyStoreException {
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain) throws KeyStoreException {
    }

    //
    // The method is created to support JKS migration (from hadoop-common KMS keystore to RangerKMS keystore)
    //

    private static final String METADATA_FIELDNAME = "metadata";
    private static final int NUMBER_OF_BITS_PER_BYTE = 8;

    public void engineLoadKeyStoreFile(InputStream stream, char[] storePass,
                                       char[] keyPass, char[] masterKey, String fileFormat)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		if (logger.isDebugEnabled()) {
			logger.debug("==> RangerKeyStoreProvider.engineLoadKeyStoreFile()");
		}
		synchronized (deltaEntries) {
			KeyStore ks;
			if (keyVaultEnabled) {
				try {
					ks = KeyStore.getInstance(fileFormat);
					ks.load(stream, storePass);
					deltaEntries.clear();
					for (Enumeration<String> name = ks.aliases(); name
							.hasMoreElements();) {
						SecretKeyByteEntry entry = new SecretKeyByteEntry();
						String alias = (String) name.nextElement();
						Key k = ks.getKey(alias, keyPass);
						SecretKey secretKey = null;
						if (k instanceof JavaKeyStoreProvider.KeyMetadata) {
							JavaKeyStoreProvider.KeyMetadata keyMetadata = (JavaKeyStoreProvider.KeyMetadata) k;
							Field f = JavaKeyStoreProvider.KeyMetadata.class
									.getDeclaredField(METADATA_FIELDNAME);
							f.setAccessible(true);
							Metadata metadata = (Metadata) f.get(keyMetadata);
							entry.bit_length = metadata.getBitLength();
							entry.cipher_field = metadata.getAlgorithm();
							entry.version = metadata.getVersions();
							Constructor<RangerKeyStoreProvider.KeyMetadata> constructor = RangerKeyStoreProvider.KeyMetadata.class
									.getDeclaredConstructor(Metadata.class);
							constructor.setAccessible(true);
							RangerKeyStoreProvider.KeyMetadata nk = constructor
									.newInstance(metadata);
							k = nk;
							secretKey = new SecretKeySpec(k.getEncoded(),
									getAlgorithm(metadata.getAlgorithm()));
						} else if (k instanceof KeyByteMetadata) {
							Metadata metadata = ((KeyByteMetadata) k).metadata;
							entry.cipher_field = metadata.getCipher();
							entry.version = metadata.getVersions();
							entry.bit_length = metadata.getBitLength();
							if (k.getEncoded() != null && k.getEncoded().length > 0) {
								secretKey = new SecretKeySpec(k.getEncoded(),
										getAlgorithm(metadata.getAlgorithm()));
							} else {
								KeyGenerator keyGenerator = KeyGenerator
										.getInstance(getAlgorithm(metadata.getCipher()));
								keyGenerator.init(metadata.getBitLength());
								byte[] keyByte = keyGenerator.generateKey().getEncoded();
								secretKey = new SecretKeySpec(keyByte,
										getAlgorithm(metadata.getCipher()));
							}
						} else if (k instanceof KeyMetadata) {
							Metadata metadata = ((KeyMetadata) k).metadata;
							entry.bit_length = metadata.getBitLength();
							entry.cipher_field = metadata.getCipher();
							entry.version = metadata.getVersions();

							if (k.getEncoded() != null
									&& k.getEncoded().length > 0) {
								secretKey = new SecretKeySpec(k.getEncoded(),
										getAlgorithm(metadata.getAlgorithm()));
							} else {
								KeyGenerator keyGenerator = KeyGenerator
										.getInstance(getAlgorithm(metadata
												.getCipher()));
								keyGenerator.init(metadata.getBitLength());
								byte[] keyByte = keyGenerator.generateKey()
										.getEncoded();
								secretKey = new SecretKeySpec(keyByte,
										getAlgorithm(metadata.getCipher()));
							}

						}else {
							entry.bit_length = (k.getEncoded().length * NUMBER_OF_BITS_PER_BYTE);
							entry.cipher_field = k.getAlgorithm();
							if (alias.split("@").length == 2) {
								entry.version = Integer.parseInt(alias
										.split("@")[1]) + 1;
							} else {
								entry.version = 1;
							}
							
							if(k.getEncoded() != null && k.getEncoded().length > 0){
								secretKey = new SecretKeySpec(k.getEncoded(),
										getAlgorithm(k.getAlgorithm()));
							}
						}

						String keyName = alias.split("@")[0];
						validateKeyName(keyName);
						entry.attributes = "{\"key.acl.name\":\"" + keyName
								+ "\"}";
						entry.key = masterKeyProvider.encryptZoneKey(secretKey);
						entry.date = ks.getCreationDate(alias);
						entry.description = k.getFormat() + " - "
								+ ks.getType();
						deltaEntries.put(alias, entry);
					}
				} catch (Throwable t) {
					logger.error("Unable to load keystore file ", t);
					throw new IOException(t);
				}
			} else {
				try {
					ks = KeyStore.getInstance(fileFormat);
					ks.load(stream, storePass);
					deltaEntries.clear();
					for (Enumeration<String> name = ks.aliases(); name
							.hasMoreElements();) {
						SecretKeyEntry entry = new SecretKeyEntry();
						String alias = (String) name.nextElement();
						Key k = ks.getKey(alias, keyPass);

						if (k instanceof JavaKeyStoreProvider.KeyMetadata) {
							JavaKeyStoreProvider.KeyMetadata keyMetadata = (JavaKeyStoreProvider.KeyMetadata) k;
							Field f = JavaKeyStoreProvider.KeyMetadata.class
									.getDeclaredField(METADATA_FIELDNAME);
							f.setAccessible(true);
							Metadata metadata = (Metadata) f.get(keyMetadata);
							entry.bit_length = metadata.getBitLength();
							entry.cipher_field = metadata.getAlgorithm();
							entry.version = metadata.getVersions();
							Constructor<RangerKeyStoreProvider.KeyMetadata> constructor = RangerKeyStoreProvider.KeyMetadata.class
									.getDeclaredConstructor(Metadata.class);
							constructor.setAccessible(true);
							RangerKeyStoreProvider.KeyMetadata nk = constructor
									.newInstance(metadata);
							k = nk;
						} else if (k instanceof KeyMetadata) {
							Metadata metadata = ((KeyMetadata) k).metadata;
							entry.bit_length = metadata.getBitLength();
							entry.cipher_field = metadata.getCipher();
							entry.version = metadata.getVersions();
						} else {
							entry.bit_length = (k.getEncoded().length * NUMBER_OF_BITS_PER_BYTE);
							entry.cipher_field = k.getAlgorithm();
							entry.version = (alias.split("@").length == 2) ? (Integer
									.parseInt(alias.split("@")[1]) + 1) : 1;
						}
						String keyName = alias.split("@")[0];
						validateKeyName(keyName);
						entry.attributes = "{\"key.acl.name\":\"" + keyName
								+ "\"}";
						Class<?> c = null;
						Object o = null;
						try {
							c = Class
									.forName("com.sun.crypto.provider.KeyProtector");
							Constructor<?> constructor = c
									.getDeclaredConstructor(char[].class);
							constructor.setAccessible(true);
							o = constructor.newInstance(masterKey);
							// seal and store the key
							Method m = c.getDeclaredMethod("seal", Key.class);
							m.setAccessible(true);
							entry.sealedKey = (SealedObject) m.invoke(o, k);
						} catch (ClassNotFoundException | NoSuchMethodException
								| SecurityException | InstantiationException
								| IllegalAccessException
								| IllegalArgumentException
								| InvocationTargetException e) {
							logger.error(e.getMessage());
							throw new IOException(e.getMessage());
						}

						entry.date = ks.getCreationDate(alias);
						entry.description = k.getFormat() + " - "
								+ ks.getType();
						deltaEntries.put(alias, entry);
					}
				} catch (Throwable t) {
					logger.error("Unable to load keystore file ", t);
					throw new IOException(t);
				}
			}
		}
	}

    public void engineLoadToKeyStoreFile(OutputStream stream, char[] storePass,
                                         char[] keyPass, char[] masterKey, String fileFormat)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> RangerKeyStoreProvider.engineLoadToKeyStoreFile()");
        }

        synchronized (keyEntries) {
            KeyStore ks;
            try {
                ks = KeyStore.getInstance(fileFormat);
                if (ks != null) {
                    ks.load(null, storePass);
                    String alias = null;
                    engineLoad(null, masterKey);
                    Enumeration<String> e = engineAliases();
                    Key key;
                    while (e.hasMoreElements()) {
                        alias = e.nextElement();
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
                logger.error("Unable to load keystore file ", t);
                throw new IOException(t);
            }
        }
    }

    private void validateKeyName(String name) {
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Key Name : "
                            + name
                            + ", should start with alpha/numeric letters and can have special characters - (hypen) or _ (underscore)");
        }
    }

    public void clearDeltaEntires() {
        deltaEntries.clear();
    }
    
    private Object getKeyEntry(String alias) {
    	   	return keyEntries.get(alias);
    }

	private XXRangerKeyStore convertKeysBetweenRangerKMSAndHSM(String alias, Key key, RangerKMSMKI rangerMKeyProvider) {
		try {
			XXRangerKeyStore xxRangerKeyStore;
			SecretKeyEntry secretKey = (SecretKeyEntry) getKeyEntry(alias);
			if (key instanceof KeyMetadata) {
				Metadata meta = ((KeyMetadata) key).metadata;
				KeyGenerator keyGenerator = KeyGenerator
						.getInstance(getAlgorithm(meta.getCipher()));
				keyGenerator.init(meta.getBitLength());
				byte[] keyByte = keyGenerator.generateKey().getEncoded();
				Key ezkey = new SecretKeySpec(keyByte,
						getAlgorithm(meta.getCipher()));
				byte[] encryptedKey = rangerMKeyProvider
						.encryptZoneKey(ezkey);
				Long creationDate = new Date().getTime();
				String attributes = secretKey.attributes;
				xxRangerKeyStore = mapObjectToEntity(alias, creationDate,
						encryptedKey, meta.getCipher(), meta.getBitLength(),
						meta.getDescription(), meta.getVersions(),
						attributes);
			} else {
				byte[] encryptedKey = rangerMKeyProvider.encryptZoneKey(key);
				Long creationDate = secretKey.date.getTime();
				int version = secretKey.version;
				if ((alias.split("@").length == 2)
						&& (((Integer.parseInt(alias.split("@")[1])) + 1) != secretKey.version)) {
					version++;
				}
				xxRangerKeyStore = mapObjectToEntity(alias, creationDate,
						encryptedKey, secretKey.cipher_field,
						secretKey.bit_length, secretKey.description, version,
						secretKey.attributes);
			}
			return xxRangerKeyStore;
		} catch (Throwable t) {
			throw new RuntimeException(
					"Migration failed between key secure and Ranger DB : ", t);
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

    /**
     * Encapsulate the encrypted key, so that we can retrieve the AlgorithmParameters object on the decryption side
     */
    private static class RangerSealedObject extends SealedObject {

        /**
         *
         */
        private static final long serialVersionUID = -7551578543434362070L;

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
        private byte[] keyByte;
        
        private final static long serialVersionUID = 8405872419967874451L;

        private KeyByteMetadata(Metadata meta, byte[] encoded) {
            this.metadata = meta;
            this.keyByte = encoded;
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
              metadata = new Metadata(metadataBuf);
              byte[] keybyteBuf = new byte[in.readInt()];
              in.readFully(keybyteBuf);
              keyByte = keybyteBuf;
        }

    }
}
