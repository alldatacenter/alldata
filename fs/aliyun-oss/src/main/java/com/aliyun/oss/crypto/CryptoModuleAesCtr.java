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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import com.aliyun.oss.ClientException;

/**
 * The crypto module specified to AES_CTR crypto shecme.
 * that it will encrypt content with AES_CTR algothrim.
 */
class CryptoModuleAesCtr extends CryptoModuleBase {    
    CryptoModuleAesCtr(OSSDirect OSS,
                     EncryptionMaterials encryptionMaterials,
                     CryptoConfiguration cryptoConfig) {
        super(OSS, encryptionMaterials, cryptoConfig);
    }

    /**
     * @return an array of bytes representing the content crypto cipher start counter.
     */
    @Override
    final byte[] generateIV() {
        final byte[] iv = new byte[contentCryptoScheme.getContentChiperIVLength()];
        cryptoConfig.getSecureRandom().nextBytes(iv);
        if (cryptoConfig.getContentCryptoMode().equals(ContentCryptoMode.AES_CTR_MODE)) {
            for (int i = 8; i < 12; i++) {
                iv[i] = 0;
            }
        }
        return iv;
    }

    /**
     *Creates a cipher from a {@link ContentCryptoMaterial} instance, it used to encrypt/decrypt data.
     *
     * @param cekMaterial
     *             It provides the cek iv and crypto algorithm to build an crypto cipher.
     * @param cipherMode
     *             Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @param cryptoRange
     *             The first element of the crypto range is the offset of the acquired object,
     *             and it should be allgned with cipher block if it was not null.
     * @param skipBlock
     *              the number of blocks should be skiped when the cipher created.
     * @return a {@link CryptoCipher} instance for encrypt/decrypt data.         
     */
    final CryptoCipher createCryptoCipherFromContentMaterial(ContentCryptoMaterial cekMaterial, int cipherMode,
            long[] cryptoRange, long skipBlock) {
        if (cipherMode != Cipher.ENCRYPT_MODE && cipherMode != Cipher.DECRYPT_MODE) {
            throw new ClientException("Invalid cipher mode.");
        }
        byte[] iv = cekMaterial.getIV();
        SecretKey cek = cekMaterial.getCEK();
        String cekAlgo = cekMaterial.getContentCryptoAlgorithm();
        CryptoScheme tmpContentCryptoScheme = CryptoScheme.fromCEKAlgo(cekAlgo);
        // Adjust the IV if needed
        boolean isRangeGet = (cryptoRange != null);
        if (isRangeGet) {
            iv = tmpContentCryptoScheme.adjustIV(iv, cryptoRange[0]);
        } else if (skipBlock > 0) {
            iv = CryptoScheme.incrementBlocks(iv, skipBlock);
        }
        return tmpContentCryptoScheme.createCryptoCipher(cek, iv, cipherMode, cryptoConfig.getContentCryptoProvider());
    }
}
