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
package org.apache.atlas;

import org.apache.atlas.security.SecurityProperties;
import org.apache.atlas.util.CredentialProviderUtility;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.apache.hadoop.security.alias.JavaKeyStoreProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class CredentialProviderUtilityIT {

    private char[] defaultPass = new char[]{'k', 'e', 'y', 'p', 'a', 's', 's'};

    @Test(enabled=false)
    public void testEnterValidValues() throws Exception {
        Path testPath = null;
        try {
            testPath = new Path(Files.createTempDirectory("tempproviders").toString(), "test.jks");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new File(testPath.toUri().getPath()).delete();
        final Path finalTestPath = testPath;
        CredentialProviderUtility.textDevice = new CredentialProviderUtility.TextDevice() {
            @Override
            public void printf(String fmt, Object... params) {
                System.out.print(String.format(fmt, params));
            }

            public String readLine(String fmt, Object... args) {
                return JavaKeyStoreProvider.SCHEME_NAME + "://file/" + finalTestPath.toString();
            }

            @Override
            public char[] readPassword(String fmt, Object... args) {
                return defaultPass;
            }
        };

        CredentialProviderUtility.main(new String[]{});

        String providerUrl = JavaKeyStoreProvider.SCHEME_NAME + "://file/" + testPath.toUri();
        Configuration conf = new Configuration(false);

        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerUrl);

        CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);

        CredentialProvider.CredentialEntry entry =
                provider.getCredentialEntry(SecurityProperties.KEYSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.TRUSTSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.SERVER_CERT_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
    }

    protected void assertCredentialEntryCorrect(CredentialProvider.CredentialEntry entry) {
        assertCredentialEntryCorrect(entry, defaultPass);
    }

    protected void assertCredentialEntryCorrect(CredentialProvider.CredentialEntry entry, char[] password) {
        Assert.assertNotNull(entry);
        Assert.assertEquals(entry.getCredential(), password);
    }

    @Test(enabled=false)
    public void testEnterEmptyValues() throws Exception {
        Path testPath = null;
        try {
            testPath = new Path(Files.createTempDirectory("tempproviders").toString(), "test.jks");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new File(testPath.toUri().getPath()).delete();
        final Path finalTestPath = testPath;
        CredentialProviderUtility.textDevice = new CredentialProviderUtility.TextDevice() {

            private Random random = new Random();

            @Override
            public void printf(String fmt, Object... params) {
                System.out.print(String.format(fmt, params));
            }

            public String readLine(String fmt, Object... args) {
                return JavaKeyStoreProvider.SCHEME_NAME + "://file/" + finalTestPath.toString();
            }

            @Override
            public char[] readPassword(String fmt, Object... args) {
                List<char[]> responses = new ArrayList<>();
                responses.add(new char[0]);
                responses.add(defaultPass);

                int size = responses.size();
                int item = random.nextInt(size);
                return responses.get(item);
            }
        };

        CredentialProviderUtility.main(new String[]{});

        String providerUrl = JavaKeyStoreProvider.SCHEME_NAME + "://file/" + testPath.toUri();
        Configuration conf = new Configuration(false);

        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerUrl);

        CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);

        CredentialProvider.CredentialEntry entry =
                provider.getCredentialEntry(SecurityProperties.KEYSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.TRUSTSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.SERVER_CERT_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
    }

    @Test(enabled=false)
    public void testEnterMismatchedValues() throws Exception {
        Path testPath = null;
        try {
            testPath = new Path(Files.createTempDirectory("tempproviders").toString(), "test.jks");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new File(testPath.toUri().getPath()).delete();
        final Path finalTestPath = testPath;
        CredentialProviderUtility.textDevice = new CredentialProviderUtility.TextDevice() {

            int i = 0;

            @Override
            public void printf(String fmt, Object... params) {
                System.out.print(String.format(fmt, params));
            }

            public String readLine(String fmt, Object... args) {
                return JavaKeyStoreProvider.SCHEME_NAME + "://file/" + finalTestPath.toString();
            }

            @Override
            public char[] readPassword(String fmt, Object... args) {
                List<char[]> responses = new ArrayList<>();
                responses.add(defaultPass);
                responses.add(new char[]{'b', 'a', 'd', 'p', 'a', 's', 's'});
                responses.add(defaultPass);

                int item = i % 3;
                i++;
                return responses.get(item);
            }
        };

        CredentialProviderUtility.main(new String[]{});

        String providerUrl = JavaKeyStoreProvider.SCHEME_NAME + "://file/" + testPath.toUri();
        Configuration conf = new Configuration(false);

        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerUrl);

        CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);

        CredentialProvider.CredentialEntry entry =
                provider.getCredentialEntry(SecurityProperties.KEYSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.TRUSTSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
        entry = provider.getCredentialEntry(SecurityProperties.SERVER_CERT_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry);
    }

    @Test(enabled=false)
    public void testOverwriteValues() throws Exception {
        Path testPath = null;
        try {
            testPath = new Path(Files.createTempDirectory("tempproviders").toString(), "test.jks");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new File(testPath.toUri().getPath()).delete();
        final Path finalTestPath = testPath;
        CredentialProviderUtility.textDevice = new CredentialProviderUtility.TextDevice() {
            @Override
            public void printf(String fmt, Object... params) {
                System.out.print(String.format(fmt, params));
            }

            public String readLine(String fmt, Object... args) {
                return JavaKeyStoreProvider.SCHEME_NAME + "://file/" + finalTestPath.toString();
            }

            @Override
            public char[] readPassword(String fmt, Object... args) {
                return defaultPass;
            }
        };

        CredentialProviderUtility.main(new String[]{});

        // now attempt to overwrite values
        CredentialProviderUtility.textDevice = new CredentialProviderUtility.TextDevice() {

            int i = 0;

            @Override
            public void printf(String fmt, Object... params) {
                System.out.print(String.format(fmt, params));
            }

            public String readLine(String fmt, Object... args) {
                return i++ == 0 ? JavaKeyStoreProvider.SCHEME_NAME + "://file/" + finalTestPath.toString() : "y";
            }

            @Override
            public char[] readPassword(String fmt, Object... args) {
                return new char[]{'n', 'e', 'w', 'p', 'a', 's', 's'};
            }
        };

        CredentialProviderUtility.main(new String[]{});

        String providerUrl = JavaKeyStoreProvider.SCHEME_NAME + "://file/" + testPath.toUri();
        Configuration conf = new Configuration(false);

        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerUrl);

        CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);

        char[] newpass = "newpass".toCharArray();
        CredentialProvider.CredentialEntry entry =
                provider.getCredentialEntry(SecurityProperties.KEYSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry, newpass);
        entry = provider.getCredentialEntry(SecurityProperties.TRUSTSTORE_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry, newpass);
        entry = provider.getCredentialEntry(SecurityProperties.SERVER_CERT_PASSWORD_KEY);
        assertCredentialEntryCorrect(entry, newpass);
    }
}
