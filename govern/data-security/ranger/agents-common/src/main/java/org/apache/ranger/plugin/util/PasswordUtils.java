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
package org.apache.ranger.plugin.util;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.jersey.core.util.Base64;
public class PasswordUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordUtils.class);

    private final String cryptAlgo;
    private String password;
    private final int iterationCount;
    private final char[] encryptKey;
    private final byte[] salt;
    private final byte[] iv;
    private static final String LEN_SEPARATOR_STR = ":";

    public static final String PBE_SHA512_AES_128 = "PBEWITHHMACSHA512ANDAES_128";

    public static final String DEFAULT_CRYPT_ALGO = "PBEWithMD5AndDES";
    public static final String DEFAULT_ENCRYPT_KEY = "tzL1AKl5uc4NKYaoQ4P3WLGIBFPXWPWdu1fRm9004jtQiV";
    public static final String DEFAULT_SALT = "f77aLYLo";
    public static final int DEFAULT_ITERATION_COUNT = 17;
    public static final byte[] DEFAULT_INITIAL_VECTOR = new byte[16];

	public static String encryptPassword(String aPassword) throws IOException {
		return build(aPassword).encrypt();
	}

	public static PasswordUtils build(String aPassword) {
		return new PasswordUtils(aPassword);
	}

    private String encrypt() throws IOException {
        String ret = null;
        String strToEncrypt = null;		
        if (password == null) {
            strToEncrypt = "";
        } else {
            strToEncrypt = password.length() + LEN_SEPARATOR_STR + password;
        }
        try {
            Cipher engine = Cipher.getInstance(cryptAlgo);
            PBEKeySpec keySpec = new PBEKeySpec(encryptKey);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(cryptAlgo);
            SecretKey key = skf.generateSecret(keySpec);
            engine.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, iterationCount, new IvParameterSpec(iv)));
            byte[] encryptedStr = engine.doFinal(strToEncrypt.getBytes());
            ret = new String(Base64.encode(encryptedStr));
        }
        catch(Throwable t) {
            LOG.error("Unable to encrypt password due to error", t);
            throw new IOException("Unable to encrypt password due to error", t);
        }
        return ret;
    }

        PasswordUtils(String aPassword) {
            String[] crypt_algo_array = null;
            byte[] SALT;
            char[] ENCRYPT_KEY;
		if (aPassword != null && aPassword.contains(",")) {
			crypt_algo_array = Lists.newArrayList(Splitter.on(",").split(aPassword)).toArray(new String[0]);
		}
		if (crypt_algo_array != null && crypt_algo_array.length > 4) {
			int index = 0;
			cryptAlgo = crypt_algo_array[index++]; // 0
			ENCRYPT_KEY = crypt_algo_array[index++].toCharArray(); // 1
			SALT = crypt_algo_array[index++].getBytes(); // 2
			iterationCount = Integer.parseInt(crypt_algo_array[index++]);// 3
			if (needsIv(cryptAlgo)) {
				iv = Base64.decode(crypt_algo_array[index++]);
			} else {
				iv = DEFAULT_INITIAL_VECTOR;
			}
			password = crypt_algo_array[index++];
			if (crypt_algo_array.length > index) {
				for (int i = index; i < crypt_algo_array.length; i++) {
					password = password + "," + crypt_algo_array[i];
				}
			}
		} else {
			cryptAlgo = DEFAULT_CRYPT_ALGO;
			ENCRYPT_KEY = DEFAULT_ENCRYPT_KEY.toCharArray();
			SALT = DEFAULT_SALT.getBytes();
			iterationCount = DEFAULT_ITERATION_COUNT;
			iv = DEFAULT_INITIAL_VECTOR;
			password = aPassword;
		}
            Map<String, String> env = System.getenv();
            String encryptKeyStr = env.get("ENCRYPT_KEY");
            if (encryptKeyStr == null) {
                encryptKey=ENCRYPT_KEY;
            }else{
                encryptKey=encryptKeyStr.toCharArray();
            }
            String saltStr = env.get("ENCRYPT_SALT");
            if (saltStr == null) {
                salt = SALT;
            }else{
                salt=saltStr.getBytes();
            }
        }

	public static String decryptPassword(String aPassword) throws IOException {
		return build(aPassword).decrypt();
	}

    private String decrypt() throws IOException {
        String ret = null;
        try {
            byte[] decodedPassword = Base64.decode(password);
            Cipher engine = Cipher.getInstance(cryptAlgo);
            PBEKeySpec keySpec = new PBEKeySpec(encryptKey);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(cryptAlgo);
            SecretKey key = skf.generateSecret(keySpec);
            engine.init(Cipher.DECRYPT_MODE, key,new PBEParameterSpec(salt, iterationCount, new IvParameterSpec(iv)));
            String decrypted = new String(engine.doFinal(decodedPassword));
            int foundAt = decrypted.indexOf(LEN_SEPARATOR_STR);
            if (foundAt > -1) {
                if (decrypted.length() > foundAt) {
                    ret = decrypted.substring(foundAt+1);
                }
                else {
                    ret = "";
                }
            }
            else {
                ret = null;
            }
        }
        catch(Throwable t) {
            LOG.error("Unable to decrypt password due to error", t);
            throw new IOException("Unable to decrypt password due to error", t);
        }
        return ret;
    }

	public static boolean needsIv(String cryptoAlgo) {
		if (Strings.isNullOrEmpty(cryptoAlgo))
			return false;

		return PBE_SHA512_AES_128.toLowerCase().equals(cryptoAlgo.toLowerCase())
				|| cryptoAlgo.toLowerCase().contains("aes_128") || cryptoAlgo.toLowerCase().contains("aes_256");
	}

	public static String generateIvIfNeeded(String cryptAlgo) throws NoSuchAlgorithmException {
		if (!needsIv(cryptAlgo))
			return null;
		return generateBase64EncodedIV();
	}

	private static String generateBase64EncodedIV() throws NoSuchAlgorithmException {
		byte[] iv = new byte[16];
		SecureRandom.getInstance("NativePRNGNonBlocking").nextBytes(iv);
		return new String(Base64.encode(iv));
	}

	public String getCryptAlgo() {
		return cryptAlgo;
	}

	public String getPassword() {
		return password;
	}

	public int getIterationCount() {
		return iterationCount;
	}

	public char[] getEncryptKey() {
		return encryptKey;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getIv() {
		return iv;
	}

	public String getIvAsString() {
		return new String(Base64.encode(getIv()));
	}
	public static String getDecryptPassword(String password) {
		String decryptedPwd = null;
		try {
			decryptedPwd = decryptPassword(password);
		} catch (Exception ex) {
			LOG.warn("Password decryption failed, trying original password string.");
			decryptedPwd = null;
		} finally {
			if (decryptedPwd == null) {
				decryptedPwd = password;
			}
		}
		return decryptedPwd;
	}
}
