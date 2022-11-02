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
package org.apache.atlas.util;

import org.apache.atlas.web.dao.UserDao;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.SERVER_CERT_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_PASSWORD_KEY;

/**
 * A utility class for generating a credential provider containing the entries required for supporting the SSL
 * implementation
 * of the DGC server.
 */
public class CredentialProviderUtility {
    private static final String[] KEYS = new String[] { KEYSTORE_PASSWORD_KEY, TRUSTSTORE_PASSWORD_KEY, SERVER_CERT_PASSWORD_KEY };
    public static abstract class TextDevice {
        public abstract void printf(String fmt, Object... params);

        public abstract String readLine(String fmt, Object... args);

        public abstract char[] readPassword(String fmt, Object... args);

    }

    private static TextDevice DEFAULT_TEXT_DEVICE = new TextDevice() {
        Console console = System.console();

        @Override
        public void printf(String fmt, Object... params) {
            console.printf(fmt, params);
        }

        @Override
        public String readLine(String fmt, Object... args) {
            return console.readLine(fmt, args);
        }

        @Override
        public char[] readPassword(String fmt, Object... args) {
            return console.readPassword(fmt, args);
        }
    };

    public static TextDevice textDevice = DEFAULT_TEXT_DEVICE;

    public static void main(String[] args) throws IOException {
        try {
            CommandLine cmd                    = new DefaultParser().parse(createOptions(), args);
            boolean     generatePasswordOption = cmd.hasOption("g");
            String      key                    = cmd.getOptionValue("k");
            char[]      cred                   = null;
            String      providerPath           = cmd.getOptionValue("f");

            if (cmd.hasOption("p")) {
                cred = cmd.getOptionValue("p").toCharArray();
            }

            if (generatePasswordOption) {
                String userName = cmd.getOptionValue("u");
                String password = cmd.getOptionValue("p");
                if (userName != null && password != null) {
                    String  encryptedPassword = UserDao.encrypt(password);
                    boolean silentOption      = cmd.hasOption("s");

                    if (silentOption) {
                        System.out.println(encryptedPassword);
                    } else {
                        System.out.println("Your encrypted password is  : " + encryptedPassword);
                    }
                } else {
                    System.out.println("Please provide username and password as input. Usage: cputil.py -g -u <username> -p <password>");
                }

                return;
            }

            if (key != null && cred != null && providerPath != null) {
                if (!StringUtils.isEmpty(String.valueOf(cred))) {
                    Configuration conf = new Configuration(false);
                    conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerPath);
                    CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);
                    provider.createCredentialEntry(key, cred);
                    provider.flush();
                    System.out.println("Password is stored in Credential Provider");
                } else {
                    System.out.println("Please enter a valid password");
                }
                return;
            }
        } catch (Exception e) {
            System.out.println("Exception while generatePassword  " + e.getMessage());
            return;
        }

        // prompt for the provider name
        CredentialProvider provider = getCredentialProvider(textDevice);

        if(provider != null) {
            for (String key : KEYS) {
                char[] cred = getPassword(textDevice, key);

                // create a credential entry and store it
                if (provider.getCredentialEntry(key) != null) {
                    String  choice    = textDevice.readLine("Entry for %s already exists.  Overwrite? (y/n) [y]:", key);
                    boolean overwrite = StringUtils.isEmpty(choice) || choice.equalsIgnoreCase("y");

                    if (overwrite) {
                        provider.deleteCredentialEntry(key);
                        provider.flush();
                        provider.createCredentialEntry(key, cred);
                        provider.flush();

                        textDevice.printf("Entry for %s was overwritten with the new value.\n", key);
                    } else {
                        textDevice.printf("Entry for %s was not overwritten.\n", key);
                    }
                } else {
                    provider.createCredentialEntry(key, cred);

                    provider.flush();
                }
            }
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("k", "ldapkey", true, "key");
        options.addOption("f", "ldapPath", true, "path");
        options.addOption("g", "generatePassword", false, "Generate Password");
        options.addOption("s", "silent", false, "Silent");
        options.addOption("u", "username", true, "UserName");
        options.addOption("p", "password", true, "Password");

        return options;
    }

    /**
     * Retrieves a password from the command line.
     * @param textDevice  the system console.
     * @param key   the password key/alias.
     * @return the password.
     */
    private static char[] getPassword(TextDevice textDevice, String key) {
        char[] ret;

        while (true) {
            char[]  passwd1 = textDevice.readPassword("Please enter the password value for %s:", key);
            char[]  passwd2 = textDevice.readPassword("Please enter the password value for %s again:", key);
            boolean isMatch = Arrays.equals(passwd1, passwd2);

            if (!isMatch) {
                textDevice.printf("Password entries don't match. Please try again.\n");
            } else {
                if (passwd1 == null || passwd1.length == 0) {
                    textDevice.printf("An empty password is not valid.  Please try again.\n");
                } else {
                    ret = passwd1;

                    if (passwd2 != null) {
                        Arrays.fill(passwd2, ' ');
                    }

                    break;
                }
            }

            if (passwd1 != null) {
                Arrays.fill(passwd1, ' ');
            }

            if (passwd2 != null) {
                Arrays.fill(passwd2, ' ');
            }
        }

        return ret;
    }

    /**\
     * Returns a credential provider for the entered JKS path.
     * @param textDevice the system console.
     * @return the Credential provider
     * @throws IOException
     */
    private static CredentialProvider getCredentialProvider(TextDevice textDevice) throws IOException {
        String providerPath = textDevice.readLine("Please enter the full path to the credential provider:");

        if (providerPath != null) {
            Configuration conf = new Configuration(false);

            conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerPath);

            return CredentialProviderFactory.getProviders(conf).get(0);
        }

        return null;
    }
}