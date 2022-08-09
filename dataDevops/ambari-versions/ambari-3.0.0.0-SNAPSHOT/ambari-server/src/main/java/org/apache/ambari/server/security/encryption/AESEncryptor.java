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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryptor {
  private static final int ITERATION_COUNT = 65536;
  private static final int KEY_LENGTH = 128;
  private Cipher ecipher;
  private Cipher dcipher;
  private SecretKey secret;
  private byte[] salt = null;
  private char[] passPhrase = null;

  public AESEncryptor(String passPhrase) {
    try {
      this.passPhrase = passPhrase.toCharArray();
      salt = new byte[8];
      SecureRandom rnd = new SecureRandom();
      rnd.nextBytes(salt);

      SecretKey tmp = getKeyFromPassword(passPhrase);
      secret = new SecretKeySpec(tmp.getEncoded(), "AES");

      ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      ecipher.init(Cipher.ENCRYPT_MODE, secret);

      dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      byte[] iv = ecipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
      dcipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidParameterSpecException | InvalidKeyException | NoSuchPaddingException e) {
      e.printStackTrace();
    }
  }

  public SecretKey getKeyFromPassword(String passPhrase) {
    return getKeyFromPassword(passPhrase, salt);
  }

  public SecretKey getKeyFromPassword(String passPhrase, byte[] salt) {
    SecretKeyFactory factory;
    SecretKey key = null;
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(passPhrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
      key = factory.generateSecret(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      e.printStackTrace();
    }

    return key;
  }

  public EncryptionResult encrypt(String encrypt) {
    try {
      byte[] bytes = encrypt.getBytes("UTF8");
      EncryptionResult atom = encrypt(bytes);
      return atom;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public EncryptionResult encrypt(byte[] plain) {
    try {
      return new EncryptionResult(salt, ecipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV(), ecipher.doFinal(plain));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] decrypt(byte[] salt, byte[] iv, byte[] encrypt) {
    try {
      SecretKey tmp = getKeyFromPassword(new String(passPhrase), salt);
      secret = new SecretKeySpec(tmp.getEncoded(), "AES");

      dcipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
      return dcipher.doFinal(encrypt);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
