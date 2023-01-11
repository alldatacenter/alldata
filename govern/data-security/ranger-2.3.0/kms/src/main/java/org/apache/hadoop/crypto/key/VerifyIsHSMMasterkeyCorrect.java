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

public class VerifyIsHSMMasterkeyCorrect {
        private static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
        private static final String PARTITION_PASSWORD = "ranger.ks.hsm.partition.password";
        private static final String PARTITION_NAME = "ranger.ks.hsm.partition.name";
        private static final String HSM_TYPE = "ranger.ks.hsm.type";

        public static void main(String[] args) throws Throwable {
                if (args.length < 2) {
                        System.err.println("Invalid number of parameters found.");
                        System.exit(1);
                }
                try {
                        String hsmType = args[0];
                        if (hsmType == null || hsmType.trim().isEmpty()) {
                                System.err.println("HSM Type does not exists.");
                                System.exit(1);
                        }

                        String partitionName = args[1];
                        if (partitionName == null || partitionName.trim().isEmpty()) {
                                System.err.println("Partition name does not exists.");
                                System.exit(1);
                        }
                        new VerifyIsHSMMasterkeyCorrect().getHSMMasterkey(hsmType, partitionName);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void getHSMMasterkey(String hsmType, String partitionName) {
                char[] partitionPassword = null;
                try {
                        partitionPassword = ConsoleUtil
                                        .getPasswordFromConsole("Enter Password for the Partition " + partitionName + " : ");

                        Configuration conf = RangerKeyStoreProvider.getDBKSConf();
                        conf.set(HSM_TYPE, hsmType);
                        conf.set(PARTITION_NAME, partitionName);
                        conf.set(PARTITION_PASSWORD, String.valueOf(partitionPassword));
                        String password = conf.get(ENCRYPTION_KEY);

                        RangerKMSDB rangerkmsDb = new RangerKMSDB(conf);
                        DaoManager daoManager = rangerkmsDb.getDaoManager();
                        RangerKeyStore dbStore = new RangerKeyStore(daoManager);

                        // Get Master Key from HSM
                        RangerHSM rangerHSM = new RangerHSM(conf);
                        String hsmMasterKey = rangerHSM.getMasterKey(password);
                        if(hsmMasterKey == null){
                                // Master Key does not exists
                        throw new IOException("Ranger MasterKey does not exists in HSM!!!");
                        }

                        dbStore.engineLoad(null, hsmMasterKey.toCharArray());
                        System.out.println("KMS keystore engine loaded successfully.");
                } catch (Throwable t) {
                        throw new RuntimeException("Unable to load keystore engine with given password or Masterkey was tampered.", t);
                }

        }

}
