/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.crypto.key;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * This Class is for HSM Keystore
 */
public class RangerSafenetKeySecure implements RangerKMSMKI {

        static final Logger logger = LoggerFactory.getLogger(RangerSafenetKeySecure.class);

        private final String alias;
        private final String providerType;
        private KeyStore myStore;
        private final String adp;
        private Provider provider;
        private static final String MK_ALGO = "AES";
        private final int mkSize;
        private static final int MK_KeySize = 256;
        private String pkcs11CfgFilePath = null;
        private static final String CFGFILEPATH = "ranger.kms.keysecure.sunpkcs11.cfg.filepath";
        private static final String MK_KEYSIZE = "ranger.kms.keysecure.masterkey.size";
        private static final String ALIAS = "ranger.kms.keysecure.masterkey.name";
        private static final String PROVIDER = "ranger.kms.keysecure.provider.type";
        private static final String KEYSECURE_LOGIN = "ranger.kms.keysecure.login";

	public RangerSafenetKeySecure(Configuration conf) throws Exception {
		mkSize = conf.getInt(MK_KEYSIZE, MK_KeySize);
		alias = conf.get(ALIAS, "RANGERMK");
		providerType = conf.get(PROVIDER, "SunPKCS11");
		adp = conf.get(KEYSECURE_LOGIN);
		pkcs11CfgFilePath = conf.get(CFGFILEPATH);
		/*
		 * Method sun.security.pkcs11.SunPKCS11 is supported till Java 8.
		 * Provider.configure() method is available from Java 9 onwards and does not have Backward compatibility.
		 * We need to remove Java 8 scenario and keep only Java 9+ once we completely upgrade to JAVA 9+.
		 * */
		try {
			int javaVersion = getJavaVersion();
			/*Minimum java requirement for Ranger KMS is Java 8 and Maximum java supported by Ranger KMS is Java 11*/
			if(javaVersion <= 8){
				Class<?> providerClass = Class.forName("sun.security.pkcs11.SunPKCS11");
				Constructor<?> constructor = providerClass.getConstructor(String.class);
				provider = (Provider) constructor.newInstance(pkcs11CfgFilePath);
			}else if(javaVersion > 8){
				Class<Provider> cls = Provider.class;
				Method configureMethod = cls.getDeclaredMethod("configure", String.class);
				provider = Security.getProvider(providerType);
				if(provider != null){
					provider = (Provider) configureMethod.invoke(provider,pkcs11CfgFilePath);
				}
			}

			if(provider != null){
				Security.addProvider(provider);
				myStore = KeyStore.getInstance("PKCS11", provider);
			}else{
				logger.error("Provider was not initialize for Ranger Safenet Key Secure.");
			}
			if (myStore != null) {
				myStore.load(null, adp.toCharArray());
			} else {
				logger.error("Safenet Keysecure not found. Please verify the Ranger KMS Safenet Keysecure configuration setup.");
			}
		}catch (NoSuchMethodException e) {
			throw new NoSuchMethodException(
					"Unexpected NoSuchMethodException while loading keystore : "
							+ e.getMessage());
		}catch (NoSuchAlgorithmException nsae) {
			throw new NoSuchAlgorithmException(
					"Unexpected NoSuchAlgorithmException while loading keystore : "
							+ nsae.getMessage());
		} catch (CertificateException e) {
			throw new CertificateException(
					"Unexpected CertificateException while loading keystore : "
							+ e.getMessage());
		} catch (IOException e) {
			throw new IOException(
					"Unexpected IOException while loading keystore : "
							+ e.getMessage());
		}
	}

        @Override
        public boolean generateMasterKey(String password){
                if (myStore != null) {
                        KeyGenerator keyGen = null;
                        SecretKey aesKey = null;
                        try {
                                boolean result = myStore.containsAlias(alias);

                                if (!result) {
                                        keyGen = KeyGenerator.getInstance(MK_ALGO, provider);
                                        keyGen.init(mkSize);
                                        aesKey = keyGen.generateKey();
                                        myStore.setKeyEntry(alias, aesKey, password.toCharArray(),
                                                        (java.security.cert.Certificate[]) null);
                                        return true;
                                } else {
                                        return true;
                                }

                        } catch (Exception e) {
                                logger.error("generateMasterKey : Exception during Ranger Master Key Generation - "
                                                + e);
                                return false;
                        }
                }
                return false;
        }

        @Override
        public String getMasterKey(String password) throws Throwable {
                if (myStore != null) {
                        try {
                                boolean result = myStore.containsAlias(alias);
                                if (result) {
                                        SecretKey key = (SecretKey) myStore.getKey(alias,
                                                        password.toCharArray());
                                        if (key != null) {
                                                return Base64.encode(key.getEncoded());
                                        }

                                }
                        } catch (Exception e) {
                                logger.error("getMasterKey : Exception searching for Ranger Master Key - "
                                                + e.getMessage());
                        }
                }
                return null;
        }

        public boolean setMasterKey(String password, byte[] key, Configuration conf) {
                if (myStore != null) {
                        try {
                                Key aesKey = new SecretKeySpec(key, MK_ALGO);
                                myStore.setKeyEntry(alias, aesKey, password.toCharArray(),
                                                (java.security.cert.Certificate[]) null);
                                return true;
                        } catch (Exception e) {
                                logger.error("setMasterKey : Exception while setting Master Key - "
                                                + e.getMessage());
                        }
                }
                return false;
        }

		private int getJavaVersion() {
			/*
			 Java 8 or lower: 1.6.0_23, 1.7.0, 1.7.0_80, 1.8.0_211
			 Java 9 or higher: 9.0.1, 11.0.4
			*/
			String version = System.getProperty("java.version");
			if (version.startsWith("1.")) {
				version = version.substring(2, 3);
			} else {
				int dot = version.indexOf(".");
				if (dot != -1) {
					version = version.substring(0, dot);
				}
			}
			return Integer.parseInt(version);
		}

}
