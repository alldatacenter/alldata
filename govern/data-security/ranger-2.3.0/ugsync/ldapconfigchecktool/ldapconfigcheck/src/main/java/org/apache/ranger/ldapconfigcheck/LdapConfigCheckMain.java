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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.lang.NullArgumentException;

public class LdapConfigCheckMain {

    private static final String LOG_FILE = "ldapConfigCheck.log";
    private static final String AMBARI_PROPERTIES = "ambari.properties";
    private static final String INSTALL_PROPERTIES = "install.properties";

    public static void main(String[] args) {

        CommandLineOptions cli = new CommandLineOptions(args);
        cli.parse();
        String inFileName = cli.getInput();
        String outputDir = cli.getOutput();
        if (!outputDir.endsWith("/")) {
            outputDir = outputDir.concat("/");
        }

        LdapConfig config = new LdapConfig(inFileName, cli.getBindPassword());
        if (cli.getLdapUrl() != null && !cli.getLdapUrl().isEmpty()) {
            config.updateInputPropFile(cli.getLdapUrl(), cli.getBindDn(), cli.getBindPassword(),
                    cli.getUserSearchBase(), cli.getUserSearchFilter(), cli.getAuthUser(), cli.getAuthPass());
        }

        PrintStream logFile = null;
        PrintStream ambariProps = null;
        PrintStream installProps = null;
        LdapContext ldapContext = null;

        try {
            logFile = new PrintStream(new File(outputDir + LOG_FILE));
            ambariProps = new PrintStream(new File(outputDir + AMBARI_PROPERTIES));
            installProps = new PrintStream(new File(outputDir + INSTALL_PROPERTIES));

            UserSync userSyncObj = new UserSync(config, logFile, ambariProps, installProps);

            String bindDn = config.getLdapBindDn();

            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, config.getLdapUrl());
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, cli.getBindPassword());
            env.put(Context.SECURITY_AUTHENTICATION, config.getLdapAuthenticationMechanism());
            env.put(Context.REFERRAL, "follow");

            ldapContext = new InitialLdapContext(env, null);

            if (config.isPagedResultsEnabled())   {
                ldapContext.setRequestControls(new Control[]{
                        new PagedResultsControl(config.getPagedResultsSize(), Control.CRITICAL) });
            }

            String retrieveValues = "all";

            if (cli.getDiscoverProperties() != null) {
                retrieveValues = cli.getDiscoverProperties();
                if (cli.getDiscoverProperties().equalsIgnoreCase("users")) {
                    userSyncObj.findUserProperties(ldapContext);
                } else if (cli.getDiscoverProperties().equalsIgnoreCase("groups")) {
                    userSyncObj.findGroupProperties(ldapContext);
                } else {
                    findAllUserSyncProperties(ldapContext, userSyncObj);
                }
            }else if (cli.getRetrieveValues() != null){
                retrieveValues = cli.getRetrieveValues();

            } else {
                cli.help();
            }

            if (cli.isAuthEnabled()) {
                authenticate(userSyncObj, config, logFile, ambariProps, installProps);
            }

            retrieveUsersGroups(ldapContext, userSyncObj, retrieveValues);

            if (ldapContext != null) {
                ldapContext.close();
            }

        } catch (FileNotFoundException fe) {
            System.out.println(fe.getMessage());
        } catch (IOException ioe) {
            logFile.println("ERROR: Failed while setting the paged results controls\n" + ioe);
        } catch (NamingException ne) {
            System.out.println("ERROR: Failed to perfom ldap bind. Please verify values for " +
                    "ranger.usersync.ldap.binddn and ranger.usersync.ldap.ldapbindpassword\n" + ne);
        } catch (Throwable t) {
            if (logFile != null) {
                logFile.println("ERROR: Connection failed: " + t.getMessage());
            } else {
                System.out.println("ERROR: Connection failed: " + t.getMessage());
            }
        } finally {
            if (logFile != null) {
                logFile.close();
            }
            if (ambariProps != null) {
                ambariProps.close();
            }
            if (installProps != null) {
                installProps.close();
            }
            try {
            	if (ldapContext != null) {
            		ldapContext.close();
            	}
            } catch (NamingException ne){
            	System.out.println("Failed to close LdapContext!");
            }
        }
    }

    private static void findAllUserSyncProperties(LdapContext ldapContext, UserSync userSyncObj) throws Throwable {

        userSyncObj.findUserProperties(ldapContext);
        userSyncObj.findGroupProperties(ldapContext);
    }

    private static void authenticate(UserSync userSyncObj, LdapConfig config,
                                     PrintStream logFile, PrintStream ambariProps,
                                     PrintStream installProps) throws Throwable{
        AuthenticationCheck auth = new AuthenticationCheck(config.getLdapUrl(), userSyncObj, logFile, ambariProps, installProps);

        auth.discoverAuthProperties();

        String msg;
        if (config.getAuthUsername() == null || config.getAuthUsername().isEmpty()) {
            msg = "ranger.admin.auth.sampleuser ";
            throw new NullArgumentException(msg);
        }

        if (config.getAuthPassword() == null || config.getAuthPassword().isEmpty()) {
            msg = "ranger.admin.auth.samplepassword ";
            throw new NullArgumentException(msg);
        }

        if (auth.isAuthenticated(config.getLdapUrl(), config.getLdapBindDn(), config.getLdapBindPassword(),
                config.getAuthUsername(), config.getAuthPassword())) {
            logFile.println("INFO: Authentication verified successfully");
        } else {
            logFile.println("ERROR: Failed to authenticate " + config.getAuthUsername());
        }
    }

    private static void retrieveUsersGroups(LdapContext ldapContext, UserSync userSyncObj,
                                            String retrieve) throws Throwable {
        String msg;
        if (retrieve == null || userSyncObj == null || ldapContext == null) {
            msg = "Input validation failed while retrieving Users or Groups";
            throw new NullArgumentException(msg);
        }

        if (retrieve.equalsIgnoreCase("users")) {
            retrieveUsers(ldapContext, userSyncObj);
        } else if (retrieve.equalsIgnoreCase("groups")){
            retrieveGroups(ldapContext, userSyncObj);
        } else {
            // retrieve both
            retrieveUsers(ldapContext, userSyncObj);
            retrieveGroups(ldapContext, userSyncObj);
        }
    }

    private static void retrieveUsers(LdapContext ldapContext, UserSync userSyncObj) throws Throwable {
        String msg;
        if (userSyncObj.getUserNameAttribute() == null || userSyncObj.getUserNameAttribute().isEmpty()) {
            msg = "ranger.usersync.ldap.user.nameattribute ";
            throw new NullArgumentException(msg);
        }
        if (userSyncObj.getUserObjClassName() == null || userSyncObj.getUserObjClassName().isEmpty()) {
            msg = "ranger.usersync.ldap.user.objectclass ";
            throw new NullArgumentException(msg);
        }
        if ((userSyncObj.getUserSearchBase() == null || userSyncObj.getUserSearchBase().isEmpty()) &&
                (userSyncObj.getSearchBase() == null || userSyncObj.getSearchBase().isEmpty())) {
            msg = "ranger.usersync.ldap.user.searchbase and " +
                    "ranger.usersync.ldap.searchBase ";
            throw new NullArgumentException(msg);
        }
        userSyncObj.getAllUsers(ldapContext);
    }

    private static void retrieveGroups(LdapContext ldapContext, UserSync userSyncObj) throws Throwable {
        String msg;
        if (userSyncObj.getGroupNameAttrName() == null || userSyncObj.getGroupNameAttrName().isEmpty()) {
            msg = "ranger.usersync.group.nameattribute ";
            throw new NullArgumentException(msg);
        }
        if (userSyncObj.getGroupObjClassName() == null || userSyncObj.getGroupObjClassName().isEmpty()) {
            msg = "ranger.usersync.group.objectclass ";
            throw new NullArgumentException(msg);
        }
        if (userSyncObj.getGroupMemberName() == null || userSyncObj.getGroupMemberName().isEmpty()) {
            msg = "ranger.usersync.group.memberattributename ";
            throw new NullArgumentException(msg);
        }
        if ((userSyncObj.getGroupSearchBase() == null || userSyncObj.getGroupSearchBase().isEmpty()) &&
                (userSyncObj.getSearchBase() == null || userSyncObj.getSearchBase().isEmpty())) {
            msg = "ranger.usersync.group.searchbase and " +
                    "ranger.usersync.ldap.searchBase ";
            throw new NullArgumentException(msg);
        }
        userSyncObj.getAllGroups(ldapContext);
    }


}

