package com.aliyun.oss.model;

/**
 * Server-side Data Encryption Algorithm.
 */
public enum DataEncryptionAlgorithm {
    SM4("SM4");

    private final String algorithm;

    public String getAlgorithm() {
        return algorithm;
    }

    private DataEncryptionAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return algorithm;
    }

    public static DataEncryptionAlgorithm fromString(String algorithm) {
        if (algorithm == null)
            return null;
        for (DataEncryptionAlgorithm e: values()) {
            if (e.getAlgorithm().equals(algorithm))
                return e;
        }
        throw new IllegalArgumentException("Unsupported data encryption algorithm " + algorithm);
    }
}
