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
package org.apache.hadoop.crypto.key;

import java.security.KeyStore;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.credentialapi.CredentialReader;
import org.apache.ranger.kms.dao.DaoManager;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class KeySecureToRangerDBMKUtil {
        private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
        private static final String KEYSECURE_USERNAME = "ranger.kms.keysecure.login.username";
    private static final String KEYSECURE_PASSWORD = "ranger.kms.keysecure.login.password";
    private static final String KEYSECURE_PASSWORD_ALIAS = "ranger.kms.keysecure.login.password.alias";
    private static final String KEYSECURE_LOGIN = "ranger.kms.keysecure.login";
    private static final String CREDENTIAL_PATH = "ranger.ks.jpa.jdbc.credential.provider.path";

        public static void showUsage() {
                System.err.println("USAGE: java " + KeySecureToRangerDBMKUtil.class.getName() + " <KMS master key password>");
        }

        public static void main(String[] args) {

                                if (args.length != 1) {
                                        System.err.println("Invalid number of parameters found.");
                                        showUsage();
                                        System.exit(1);
                                }
                                else {
                                        String kmsMKPassword = args[0];
                                        if (kmsMKPassword == null || kmsMKPassword.trim().isEmpty()) {
                                                System.err.println("KMS master key password not provided");
                                                showUsage();
                                                System.exit(1);
                                        }

                                        new KeySecureToRangerDBMKUtil().doImportMKFromKeySecure(kmsMKPassword);
                                                System.out.println("Master Key from Key Secure has been successfully imported into Ranger KMS DB.");
                                }
                        }

        private void doImportMKFromKeySecure(String kmsMKPassword) {
                try {
                        Configuration conf = RangerKeyStoreProvider.getDBKSConf();
                        conf.set(ENCRYPTION_KEY, kmsMKPassword);
                        getFromJceks(conf, CREDENTIAL_PATH, KEYSECURE_PASSWORD_ALIAS, KEYSECURE_PASSWORD);
                        String keySecureLoginCred = conf.get(KEYSECURE_USERNAME).trim() + ":" + conf.get(KEYSECURE_PASSWORD);
                        conf.set(KEYSECURE_LOGIN, keySecureLoginCred);

                        RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
                        DaoManager daoManager = rangerkmsDb.getDaoManager();
                        String password = conf.get(ENCRYPTION_KEY);

                        RangerSafenetKeySecure rangerSafenetKeySecure = new RangerSafenetKeySecure(
                                        conf);
                        String mKey = rangerSafenetKeySecure.getMasterKey(password);

                        byte[] key = Base64.decode(mKey);

                        // Put Master Key in Ranger DB
                        RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
                        rangerMasterKey.generateMKFromKeySecureMK(password, key);

                } catch (Throwable t) {
                        throw new RuntimeException(
                                        "Unable to migrate Master key from KeySecure to Ranger DB",
                                        t);

                }
        }

        private static void getFromJceks(Configuration conf, String path, String alias, String key) {

        //update credential from keystore
        if (conf != null) {
            String pathValue = conf.get(path);
            String aliasValue = conf.get(alias);
            if (pathValue != null && aliasValue != null) {
                String xaDBPassword = CredentialReader.getDecryptedString(pathValue.trim(), aliasValue.trim(), KeyStore.getDefaultType());
                if (xaDBPassword != null && !xaDBPassword.trim().isEmpty() &&
                        !xaDBPassword.trim().equalsIgnoreCase("none")) {
                    conf.set(key, xaDBPassword);
                }
            }
        }
    }


}
