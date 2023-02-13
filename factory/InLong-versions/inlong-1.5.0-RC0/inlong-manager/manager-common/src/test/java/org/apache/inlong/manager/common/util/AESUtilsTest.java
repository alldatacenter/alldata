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

package org.apache.inlong.manager.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * AES encryption and decryption util test.
 */
public class AESUtilsTest {

    @Test
    public void testEncryptDecryptDirectly() throws Exception {
        byte[] key = "key-123".getBytes(StandardCharsets.UTF_8);
        String plainText = "hello, inlong";
        byte[] cipheredBytes = AESUtils.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key);
        byte[] decipheredBytes = AESUtils.decrypt(cipheredBytes, key);
        Assertions.assertEquals(plainText, new String(decipheredBytes, StandardCharsets.UTF_8));
    }

    @Test
    void testEncryptDecryptByConfigVersion() throws Exception {
        String plainText = "hello, inlong again";
        Integer version = AESUtils.getCurrentVersion(null);
        String cipheredText = AESUtils.encryptToString(plainText.getBytes(StandardCharsets.UTF_8), version);
        byte[] decipheredBytes = AESUtils.decryptAsString(cipheredText, version);
        Assertions.assertEquals(plainText, new String(decipheredBytes, StandardCharsets.UTF_8));
    }

    @Test
    void testEncryptDecryptByNullVersion() throws Exception {
        String plainText = "hello, inlong again";

        // when version is null no encryption is performed
        String cipheredText = AESUtils.encryptToString(plainText.getBytes(StandardCharsets.UTF_8), null);
        Assertions.assertEquals(plainText, cipheredText);

        // when version is null no decryption is performed
        byte[] decipheredBytes = AESUtils.decryptAsString(cipheredText, null);
        Assertions.assertEquals(plainText, new String(decipheredBytes, StandardCharsets.UTF_8));
    }
}
