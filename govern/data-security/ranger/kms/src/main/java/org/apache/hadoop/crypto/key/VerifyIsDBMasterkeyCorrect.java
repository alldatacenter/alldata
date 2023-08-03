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

public class VerifyIsDBMasterkeyCorrect {
        RangerMasterKey rangerMasterKey;
        RangerKeyStore dbStore;
        DaoManager daoManager;

        public VerifyIsDBMasterkeyCorrect() throws Throwable {
                Configuration conf = RangerKeyStoreProvider.getDBKSConf();
                RangerKMSDB rangerKMSDB = new RangerKMSDB(conf);
                daoManager = rangerKMSDB.getDaoManager();
                dbStore = new RangerKeyStore(daoManager);
        }

        public static void main(String[] args) throws Throwable {
                if (args.length == 0) {
                        System.err.println("Invalid number of parameters found.");
                        System.exit(1);
                }
                try {
                        String password = args[0];
                        if (password == null || password.trim().isEmpty()) {
                                System.err.println("KMS Masterkey Password not provided.");
                                System.exit(1);
                        }
                        new VerifyIsDBMasterkeyCorrect().verifyMasterkey(password);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void verifyMasterkey(String pass) {
                try {
                        // Get Master Key from DB
                        rangerMasterKey = new RangerMasterKey(daoManager);
                        String masterKey = rangerMasterKey.getMasterKey(pass);
                        if(masterKey == null){
                                // Master Key does not exists
                        throw new IOException("Ranger MasterKey does not exists");
                        }
                        dbStore.engineLoad(null, masterKey.toCharArray());
                        System.out.println("KMS keystore engine loaded successfully.");
                } catch (Throwable e) {
                        throw new RuntimeException("Unable to load keystore engine with given password or Masterkey was tampered.", e);
                }
        }
}
