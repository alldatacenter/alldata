package com.aliyun.oss.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.crypto.CryptoCipher;

public abstract class CryptoScheme {
    // Enable bouncy castle provider
    static {
        CryptoRuntime.enableBouncyCastle();
    }

    public static final int BLOCK_SIZE = 16;
    public static final CryptoScheme AES_CTR = new AesCtr();

    public abstract String getKeyGeneratorAlgorithm();

    public abstract int getKeyLengthInBits();

    public abstract String getContentChiperAlgorithm();

    public abstract int getContentChiperIVLength();

    public abstract byte[] adjustIV(byte[] iv, long dataStartPos);

    /**
     * This is a factory method to create CryptoCipher.
     *
     * @param cipher
     *            cipher.
     * @param cek
     *            secret key.
     * @param cipherMode
     *            cipher mode.
     *
     * @return The {@link CryptoCipher} instance.
     */
    public CryptoCipher newCryptoCipher(Cipher cipher, SecretKey cek, int cipherMode) {
        return new CryptoCipher(cipher, this, cek, cipherMode);
    }

    public CryptoCipher createCryptoCipher(SecretKey cek, byte[] iv, int cipherMode, Provider provider) {
        try {
            Cipher cipher = null;
            if (provider != null) {
                cipher = Cipher.getInstance(getContentChiperAlgorithm(), provider);
            } else {
                cipher = Cipher.getInstance(getContentChiperAlgorithm());
            }
            cipher.init(cipherMode, cek, new IvParameterSpec(iv));
            return newCryptoCipher(cipher, cek, cipherMode);
        } catch (Exception e) {
            throw new ClientException("Unable to build cipher: " + e.getMessage(), e);
        }
    }

    /**
     * Increment the rightmost 64 bits of a 16-byte counter by the specified delta.
     * Both the specified delta and the resultant value must stay within the
     * capacity of 64 bits. (Package private for testing purposes.)
     *
     * @param counter
     *            a 16-byte counter.
     * @param blockDelta
     *            the number of blocks (16-byte) to increment
     * @return Return a new 16-byte counter.
     */
    public static byte[] incrementBlocks(byte[] counter, long blockDelta) {
        if (blockDelta == 0)
            return counter;
        if (counter == null || counter.length != 16)
            throw new IllegalArgumentException();

        ByteBuffer bb = ByteBuffer.allocate(8);
        for (int i = 12; i <= 15; i++)
            bb.put(i - 8, counter[i]);
        long val = bb.getLong() + blockDelta; // increment by delta
        bb.rewind();
        byte[] result = bb.putLong(val).array();

        for (int i = 8; i <= 15; i++)
            counter[i] = result[i - 8];
        return counter;
    }

    public static CryptoScheme fromCEKAlgo(String cekAlgo) {
        if (AES_CTR.getContentChiperAlgorithm().equals(cekAlgo)) {
            return AES_CTR;
        }
        throw new UnsupportedOperationException("Unsupported content encryption scheme: " + cekAlgo);
    }
}
