/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.crypto.key.kms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;

import org.apache.hadoop.crypto.key.RangerKeyStore;
import org.apache.ranger.kms.dao.DaoManager;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerKeyStore {

        String fileFormat = "jceks";
        String keyStoreFileName = "KmsKeyStoreFile";
        char[] storePass = "none".toCharArray();
        char[] keyPass = "none".toCharArray();
        char[] masterKey = "MasterPassword".toCharArray();

        @Before
        public void checkFileIfExists() {
                deleteKeyStoreFile();
        }

        @After
        public void cleanKeystoreFile() {
                deleteKeyStoreFile();
        }

        @Test(expected=IOException.class)
        public void testInvalidKey1() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "enckey:1";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        @Test(expected=IOException.class)
        public void testInvalidKey2() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "1%enckey";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        @Test(expected=IOException.class)
        public void testInvalidKey3() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "1 enckey";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        @Test(expected=IOException.class)
        public void testInvalidKey4() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "_1-enckey";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        @Test
        public void testValidKey1() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "enckey_1-test";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        @Test
        public void testValidKey2() throws NoSuchAlgorithmException,
                        CertificateException, IOException, KeyStoreException {

                DaoManager daoManager = Mockito.mock(DaoManager.class);
                RangerKeyStore rangerKeyStore = new RangerKeyStore(daoManager);
                String keyValue = "1-enckey_test";
                InputStream inputStream = generateKeyStoreFile(keyValue);
                rangerKeyStore.engineLoadKeyStoreFile(inputStream, storePass, keyPass, masterKey, fileFormat);
                inputStream.close();
        }

        private InputStream generateKeyStoreFile(String keyValue) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
                FileOutputStream stream = new FileOutputStream(new File(keyStoreFileName));
                KeyStore ks;
                try {
                        ks = KeyStore.getInstance(fileFormat);
                        if (ks != null) {
                                ks.load(null, storePass);
                                String alias = keyValue;

                                KeyGenerator kg = KeyGenerator.getInstance("AES");
                                kg.init(256);
                                Key key = kg.generateKey();
                                ks.setKeyEntry(alias, key, keyPass, null);
                                ks.store(stream, storePass);
                        }
                        return new FileInputStream(new File(keyStoreFileName));
                } catch (Throwable t) {
                        throw new IOException(t);
                } finally {
			stream.close();
                }
        }

        private void deleteKeyStoreFile() {
                File f = new File(keyStoreFileName);
                if (f.exists()) {
                        boolean bol = f.delete();
                        if(!bol){
				System.out.println("Keystore File was not deleted successfully.");
                        }
                }
        }
}
