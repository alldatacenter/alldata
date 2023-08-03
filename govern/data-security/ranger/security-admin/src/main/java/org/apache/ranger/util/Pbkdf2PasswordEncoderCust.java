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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.util;

import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.util.EncodingUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Pbkdf2PasswordEncoderCust implements PasswordEncoder {
    private static final int DEFAULT_HASH_WIDTH = 256;
    private static final int DEFAULT_ITERATIONS = 185000;
    private final BytesKeyGenerator saltGenerator;
    private final byte[] secret;
    private final int hashWidth;
    private final int iterations;
    private String algorithm;
    private boolean encodeHashAsBase64;

    public Pbkdf2PasswordEncoderCust(CharSequence secret) {
        this(secret, DEFAULT_ITERATIONS, DEFAULT_HASH_WIDTH);
    }

    public Pbkdf2PasswordEncoderCust(CharSequence secret, int iterations, int hashWidth) {
        this.saltGenerator = KeyGenerators.secureRandom(16);
        this.algorithm = Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512.name();
        this.secret = Utf8.encode(secret);
        this.iterations = iterations;
        this.hashWidth = hashWidth;
    }

    public void setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm secretKeyFactoryAlgorithm) {
        if (secretKeyFactoryAlgorithm == null) {
            throw new IllegalArgumentException("secretKeyFactoryAlgorithm cannot be null");
        } else {
            String algorithmName = secretKeyFactoryAlgorithm.name();

            try {
                SecretKeyFactory.getInstance(algorithmName);
            } catch (NoSuchAlgorithmException var4) {
                throw new IllegalArgumentException("Invalid algorithm '" + algorithmName + "'.", var4);
            }

            this.algorithm = algorithmName;
        }
    }

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = this.saltGenerator.generateKey();
        byte[] encoded = this.encode(rawPassword, salt);
        return this.encode(encoded);
    }

    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

    private String encode(byte[] bytes) {
        return this.encodeHashAsBase64 ? Utf8.decode(Base64.encode(bytes)) : String.valueOf(Hex.encode(bytes));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        byte[] digested = this.decode(encodedPassword);
        byte[] salt = EncodingUtils.subArray(digested, 0, this.saltGenerator.getKeyLength());
        return matches(digested, this.encode(rawPassword, salt));
    }
    
    private static boolean matches(byte[] expected, byte[] actual) {
    	return Arrays.equals(expected, actual);
    }


    private byte[] decode(String encodedBytes) {
        return this.encodeHashAsBase64 ? Base64.decode(Utf8.encode(encodedBytes)) : Hex.decode(encodedBytes);
    }

    private byte[] encode(CharSequence rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(), EncodingUtils.concatenate(new byte[][]{salt, this.secret}), this.iterations, this.hashWidth);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(this.algorithm);
            return EncodingUtils.concatenate(new byte[][]{salt, skf.generateSecret(spec).getEncoded()});
        } catch (GeneralSecurityException var5) {
            throw new IllegalStateException("Could not create hash", var5);
        }
    }
}
