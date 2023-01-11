/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.ldapconfigcheck;

import org.apache.commons.cli.*;
import java.io.Console;

public class CommandLineOptions {

    private String[] args = null;
    private Options options = new Options();
    private String input = null;
    private String output = null;
    private String discoverProperties = null;
    private String retrieveValues = null;
    private boolean isAuthEnabled = true;
    private String ldapUrl = "";
    private String bindDn = "";
    private String bindPassword = "";
    private String userSearchBase = "";
    private String userSearchFilter = "";
    private String authUser = "";
    private String authPass = "";

    public CommandLineOptions(String[] args) {
        this.args = args;
        options.addOption("h", "help", false, "show help.");
        options.addOption("i", "inputfile", true, "Input file name");
        options.addOption("o", "outputdir", true, "Output directory");
        options.addOption("d", "discoverProperties", true, "{all|users|groups}");
        options.addOption("r", "retrieve", true, "{all|users|groups}");
        options.addOption("a", "noAuthentication", false, "Ignore authentication properties");
        options.addOption("p", true, "Ldap Bind Password");
        options.addOption("u", true, "Sample Authentication User Password");
    }

    public void parse() {
    	CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            // if (cmd.hasOption("h")) {
            //}

            if (cmd.hasOption("p")) {
            	bindPassword = cmd.getOptionValue("p");
            	if (bindPassword.trim().isEmpty()) {
            		System.out.println("Ldap Bind Password cannot be empty!");
            	}
            }

            if (cmd.hasOption("u")) {
                authPass = cmd.getOptionValue("u");
                if (authPass.trim().isEmpty()) {
                    System.out.println("Sample Authentication User Password cannot be empty!");
                }
            }

            if (cmd.hasOption("o")) {
                output = cmd.getOptionValue("o");
            } else {
                System.out.println("Missing o option for output directory");
                help();
            }

            if (cmd.hasOption("d")) {
                discoverProperties = cmd.getOptionValue("d");
                if (discoverProperties == null || (!discoverProperties.equalsIgnoreCase("all") &&
                        !discoverProperties.equalsIgnoreCase("users") && !discoverProperties.equalsIgnoreCase("groups"))) {
                    System.out.println("Unsupported value for option d");
                    help();
                }
            } else if (cmd.hasOption("r")) {
                retrieveValues = cmd.getOptionValue("r");
                if (retrieveValues == null || (!retrieveValues.equalsIgnoreCase("all")
                        && !retrieveValues.equalsIgnoreCase("users") && !retrieveValues.equalsIgnoreCase("groups"))) {
                    System.out.println("Unsupported value for option r");
                    help();
                }
            } else {
                //if (discoverProperties == null || discoverProperties.isEmpty()) {
                    System.out.println("Default to discover all usersync properties");
                    //help();
                	// If "d" or "r" option is not specified, then default to discover all usersync properties
                	discoverProperties = "all";
                //}
            }

            if (cmd.hasOption("a") || discoverProperties == null || (discoverProperties != null && !discoverProperties.equalsIgnoreCase("all"))) {
                isAuthEnabled = false;
            }

            if (cmd.hasOption("i")) {
                input = cmd.getOptionValue("i");
                if (input == null || input.isEmpty()) {
                    System.out.println("Please specify the input properties file name");
                    help();
                }

                if (bindPassword == null || bindPassword.trim().isEmpty()) {
            		System.out.println("Missing Ldap Bind Password!");
            	}

            } else {
                // Read the properties from CLI and write to the input properties file.
                input = LdapConfig.CONFIG_FILE;
                readCLI();
            }

        } catch (ParseException pe) {
            System.out.println("Failed to parse command line arguments " + pe);
            help();
        }
    }

    public void help() {
        // This prints out some help
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp("ldapConfigCheck", options);
        System.exit(0);
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {

        return output;
    }

    public String getDiscoverProperties() {
        return discoverProperties;
    }

    public boolean isAuthEnabled() {
        return isAuthEnabled;
    }

    public String getRetrieveValues() {
        return retrieveValues;
    }

    private void readCLI() {
        boolean repeat;
        Console console = System.console();
        do {
            repeat = false;
            System.out.print("Ldap url [ldap://ldap.example.com:389]: ");
            ldapUrl = console.readLine();
            if (ldapUrl == null || ldapUrl.isEmpty()) {
                System.out.println("Please enter valid ldap url.");
                repeat = true;
            }
        } while (repeat == true);
        do {
            repeat = false;
            System.out.print("Bind DN [cn=admin,ou=users,dc=example,dc=com]: ");
            bindDn = console.readLine();
            if (bindDn == null || bindDn.isEmpty()) {
                System.out.println("Please enter valid bindDn.");
                repeat = true;
            }
        } while (repeat == true);
        do {
            repeat = false;
            System.out.print("Bind Password: ");
            char[] password = console.readPassword();
            bindPassword = String.valueOf(password);
            if (bindPassword == null || bindPassword.isEmpty()) {
                System.out.println("Bind Password can't be empty.");
                repeat = true;
            }
        } while (repeat == true);
        System.out.print("User Search Base [ou=users,dc=example,dc=com]: ");
        userSearchBase = console.readLine();
        System.out.print("User Search Filter [cn=user1]: ");
        userSearchFilter = console.readLine();

        if (isAuthEnabled) {
            do {
                repeat = false;
                System.out.print("Sample Authentication User [user1]: ");
                authUser = console.readLine();
                if (authUser == null || authUser.isEmpty()) {
                    System.out.println("Sample Authentication user must not be empty!");
                    repeat = true;
                }
            } while (repeat == true);
            do {
                repeat = false;
                System.out.print("Sample Authentication Password: ");
                char[] password = console.readPassword();
                authPass = String.valueOf(password);
                if (authPass == null || authPass.isEmpty()) {
                    System.out.println("Sample Authentication password must not be empty!");
                    repeat = true;
                }
            } while (repeat == true);
        }
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public String getAuthUser() {
        return authUser;
    }

    public String getAuthPass() {
        return authPass;
    }
}
