package com.qcloud.cos;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoMode;
import com.qcloud.cos.internal.crypto.CryptoStorageMode;
import com.qcloud.cos.internal.crypto.EncryptionMaterials;

public class ASymmetricCOSEncryptionClientWithInstructionCryptoModeTest
        extends AbstractCOSEncryptionClientTest {

    private static void initEncryptionInfo() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyGenerator.generateKeyPair();

        encryptionMaterials = new EncryptionMaterials(keyPair);
        cryptoConfiguration = new CryptoConfiguration(CryptoMode.AuthenticatedEncryption)
                .withStorageMode(CryptoStorageMode.ObjectMetadata);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initEncryptionInfo();
        AbstractCOSEncryptionClientTest.setUpBeforeClass();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSEncryptionClientTest.tearDownAfterClass();
    }
}
