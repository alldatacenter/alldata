/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import org.apache.ambari.server.utils.TextEncoding;

/**
 * This interface provides useful methods to encrypt/decrypt sensitive
 * information. As of now the underlying implementation uses AES encryption
 * which may be changed in the future (or you will have the option to plugin
 * your own security algorithm).
 * 
 * Text encoding takes places during encryption thus decryption starts with text
 * decoding of the same encoding type. Currently the following text encoding
 * types are supported:
 * <ul>
 * <li>BASE_64 (default)
 * <li>BinHex
 * </ul>
 *
 * @see TextEncoding
 *
 */
public interface EncryptionService {

  /**
   * Encrypts the given text using Ambari's master key found in the environment.
   * The returned value will be encoded with BASE_64 encoding.
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @return the String representation of the encrypted text
   * @throws Exception
   *           in case any error happened during the encryption process
   */
  String encrypt(String toBeEncrypted);

  /**
   * Encrypts the given text using Ambari's master key found in the environment.
   * The returned value will be encoded with the given text encoding.
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @param textEncoding
   *          the text encoding which the encrypted text is encoded with
   * @return the String representation of the encrypted text
   */
  String encrypt(String toBeEncrypted, TextEncoding textEncoding);

  /**
   * Encrypts the given text using the given key. The returned value will be
   * encoded with BASE_64 encoding.
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @param key
   *          the key to be used for encryption
   * @return the String representation of the encrypted text
   *           in case any error happened during the encryption process
   */
  String encrypt(String toBeEncrypted, String key);

  /**
   * Encrypts the given text using the given key.The returned value will be
   * encoded with the given text encoding.
   * 
   * @param toBeEncrypted
   *          the text to be encrypted
   * @param key
   *          the key to be used for encryption
   * @param textEncoding
   *          the text encoding which the encrypted text is encoded with
   * @return the String representation of the encrypted text
   */
  String encrypt(String toBeEncrypted, String key, TextEncoding textEncoding);

  /**
   * @return the default encryption key used by this encryption service
   */
  String getAmbariMasterKey();

  /**
   * Decrypts the given text (must be encoded with BASE_64 encoding) using
   * Ambari's master key found in the environment.
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @return the String representation of the decrypted text
   */
  String decrypt(String toBeDecrypted);

  /**
   * Decrypts the given text (must be encoded with the given text encoding) using
   * Ambari's master key found in the environment.
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @param textEncoding
   *          the text encoding which <code>toBeDecrypted</code> is encoded with
   * @return the String representation of the decrypted text
   */
  String decrypt(String toBeDecrypted, TextEncoding textEncoding);

  /**
   * Decrypts the given text (must be encoded with BASE_64 encoding) using the
   * given key.
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @param key
   *          the key to be used for decryption
   * @return the String representation of the decrypted text
   */
  String decrypt(String toBeDecrypted, String key);

  /**
   * Decrypts the given text (must be encoded with the given text encoding) using
   * the given key.
   * 
   * @param toBeDecrypted
   *          the text to be decrypted
   * @param key
   *          the key to be used for decryption
   * @param textEncoding
   *          the text encoding which <code>toBeDecrypted</code> is encoded with
   * @return the String representation of the decrypted text
   */
  String decrypt(String toBeDecrypted, String key, TextEncoding textEncoding);

}
