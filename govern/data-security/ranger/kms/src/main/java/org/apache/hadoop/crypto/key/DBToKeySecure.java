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

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.kms.dao.DaoManager;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class DBToKeySecure {

        private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
        private static final String KEYSECURE_MASTERKEY_NAME = "ranger.kms.keysecure.masterkey.name";
//	private static final String KEYSECURE_PROTOCOL = "ranger.kms.keysecure.protocol";
        private static final String KEYSECURE_LOGIN = "ranger.kms.keysecure.login";
        private static final String CFGFILEPATH = "ranger.kms.keysecure.sunpkcs11.cfg.filepath";

        public static void showUsage() {
                System.err
                                .println("USAGE: java "
                                                + DBToKeySecure.class.getName()
                                                + " <keySecureMasterKeyName> <keySecureUsername> <keySecurePassword> <sunpkcs11CfgFilePath>");
        }

        public static void main(String[] args) {

                if (args.length < 4) {
                        System.err.println("Invalid number of parameters found.");
                        showUsage();
                        System.exit(1);
                } else {

                        Configuration conf = RangerKeyStoreProvider.getDBKSConf();

                        String keyName = args[0];
                        if (keyName == null || keyName.trim().isEmpty()) {
                                System.err.println("Key Secure master key name not provided.");
                                showUsage();
                                System.exit(1);
                        }

                        String username = args[1];
                        if (username == null || username.trim().isEmpty()) {
                                System.err.println("Key Secure username not provided.");
                                showUsage();
                                System.exit(1);
                        }
                        String password = args[2];
                        if (password == null || password.trim().isEmpty()) {
                                System.err.println("Key Secure password not provided.");
                                showUsage();
                                System.exit(1);
                        }

                        String cfgFilePath = args[3];
                        if (cfgFilePath == null || cfgFilePath.trim().isEmpty()) {
                                System.err.println("sunpkcs11 Configuration File Path not provided");
                                showUsage();
                                System.exit(1);
                        }

                        boolean result = new DBToKeySecure().doExportMKToKeySecure(keyName, username, password, cfgFilePath,  conf);
                        if (result) {
                                System.out
                                                .println("Master Key from Ranger KMS DB has been successfully imported into Key Secure.");
                        } else {
                                System.out
                                                .println("Import of Master Key from DB has been unsuccessful.");
                                System.exit(1);
                        }
                        System.exit(0);
                }
        }

        private boolean doExportMKToKeySecure(String keyName, String username, String password, String cfgFilePath, Configuration conf) {
                try {
                        String keySecureMKPassword = conf.get(ENCRYPTION_KEY);
                        if (keySecureMKPassword == null
                                        || keySecureMKPassword.trim().equals("")
                                        || keySecureMKPassword.trim().equals("_")
                                        || keySecureMKPassword.trim().equals("crypted")) {
                                throw new IOException("Master Key Jceks does not exists");
                        }

                        conf.set(CFGFILEPATH, cfgFilePath);
                        conf.set(KEYSECURE_MASTERKEY_NAME, keyName);
                        conf.set(KEYSECURE_LOGIN,username + ":" + password);

                        RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
                        DaoManager daoManager = rangerkmsDb.getDaoManager();
                        String mkPassword = conf.get(ENCRYPTION_KEY);

                        // Get Master Key from Ranger DB
                        RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
                        String mkey = rangerMasterKey.getMasterKey(mkPassword);
                        byte[] key = Base64.decode(mkey);

                        if (conf != null) {
                                RangerSafenetKeySecure rangerSafenetKeySecure = new RangerSafenetKeySecure(
                                                conf);
                                return rangerSafenetKeySecure.setMasterKey(password, key,conf);
                        }

                        return false;
                } catch (Throwable t) {
                        throw new RuntimeException(
                                        "Unable to import Master key from Ranger DB to KeySecure ",
                                        t);
                }

        }

}
