/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.crypto.key.kms.server;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.hadoop.crypto.key.RangerKMSDB;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider;
import org.apache.hadoop.crypto.key.RangerMasterKey;
import org.apache.ranger.kms.dao.DaoManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test for the RangerMasterKey.
 */
public class RangerMasterKeyTest {
	private static final boolean UNRESTRICTED_POLICIES_INSTALLED;
	static {
		boolean ok = false;
		try {
			byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

			SecretKey key192 = new SecretKeySpec(
					new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
							0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
							0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
					"AES");
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, key192);
			c.doFinal(data);
			ok = true;
		} catch (Exception e) {
			//
		}
		UNRESTRICTED_POLICIES_INSTALLED = ok;
	}

    @BeforeClass
    public static void startServers() throws Exception {
    	if (!UNRESTRICTED_POLICIES_INSTALLED) {
    		return;
    	}
        DerbyTestUtils.startDerby();
    }

    @AfterClass
    public static void stopServers() throws Exception {
    	if (UNRESTRICTED_POLICIES_INSTALLED) {
    		DerbyTestUtils.stopDerby();
    	}
    }

    @Test
    public void testRangerMasterKey() throws Throwable {
    	if (!UNRESTRICTED_POLICIES_INSTALLED) {
    		return;
    	}
    	
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());

        RangerKMSDB rangerkmsDb = new RangerKMSDB(RangerKeyStoreProvider.getDBKSConf());
        DaoManager daoManager = rangerkmsDb.getDaoManager();

        String masterKeyPassword = "password0password0password0password0password0password0password0password0"
            + "password0password0password0password0password0password0password0password0password0password0"
            + "password0password0password0password0password0password0password0password0password0password0";

        RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
        Assert.assertTrue(rangerMasterKey.generateMasterKey(masterKeyPassword));
        Assert.assertNotNull(rangerMasterKey.getMasterKey(masterKeyPassword));

        try {
            rangerMasterKey.getMasterKey("badpass");
            Assert.fail("Failure expected on retrieving a key with the wrong password");
        } catch (Exception ex) {
            // expected
        }

        Assert.assertNotNull(rangerMasterKey.getMasterSecretKey(masterKeyPassword));

        try {
            rangerMasterKey.getMasterSecretKey("badpass");
            Assert.fail("Failure expected on retrieving a key with the wrong password");
        } catch (Exception ex) {
            // expected
        }
    }
}
