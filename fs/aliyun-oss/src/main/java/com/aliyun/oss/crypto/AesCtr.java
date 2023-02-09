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

public class AesCtr extends CryptoScheme {
    @Override public String getKeyGeneratorAlgorithm() {return "AES";}
    @Override public int getKeyLengthInBits() {return 256;}
    @Override public String getContentChiperAlgorithm() {return "AES/CTR/NoPadding";}
    @Override public int getContentChiperIVLength() {return 16;}

    @Override
    public byte[] adjustIV(byte[] iv, long dataStartPos) {
        if (iv.length != 16)
            throw new UnsupportedOperationException();

        final int blockSize = BLOCK_SIZE;
        long remainder = dataStartPos % blockSize;
        if (remainder != 0) {
            throw new IllegalArgumentException(
                    "Expected data start pos should be multiple of 16," + "but it was: " + dataStartPos);
        }

        long blockOffset = dataStartPos / blockSize;
        byte[] J0 = computeJ0(iv);
        return incrementBlocks(J0, blockOffset);
    }

    private byte[] computeJ0(byte[] nonce) {
        final int blockSize = BLOCK_SIZE;
        byte[] J0 = new byte[blockSize];
        System.arraycopy(nonce, 0, J0, 0, nonce.length);
        return J0;
    }
}
