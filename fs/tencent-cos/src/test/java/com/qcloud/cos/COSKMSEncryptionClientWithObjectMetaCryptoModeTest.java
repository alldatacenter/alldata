package com.qcloud.cos;

import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoMode;
import com.qcloud.cos.internal.crypto.CryptoStorageMode;
import com.qcloud.cos.internal.crypto.KMSEncryptionMaterials;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class COSKMSEncryptionClientWithObjectMetaCryptoModeTest
        extends AbstractCOSEncryptionClientTest{

    private static void initEncryptionInfo() {
        // set cmk in prop file
        encryptionMaterials = new KMSEncryptionMaterials("null");
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
