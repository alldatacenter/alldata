/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

import org.apache.atlas.model.metrics.AtlasMetrics;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;

import java.util.Arrays;


/**
 * An application that allows users to run admin commands against an Atlas server.
 *
 * The application uses {@link AtlasClient} to send REST requests to the Atlas server. The details of connections
 * and other configuration is specified in the Atlas properties file.
 * Exit status of the application will be as follows:
 * <li>0: successful execution</li>
 * <li>1: error in options used for the application</li>
 * <li>-1/255: application error</li>
 */
public class AtlasAdminClient {

    private static final Option STATUS      = new Option("status", false, "Get the status of an atlas instance");
    private static final Option STATS       = new Option("stats", false, "Get the metrics of an atlas instance");
    private static final Option CREDENTIALS = new Option("u", true, "Authorized atlas user credentials (<user>:<password>)");

    private static final Options OPTIONS = new Options();

    private static final int INVALID_OPTIONS_STATUS = 1;
    private static final int PROGRAM_ERROR_STATUS = -1;

    static {
        OPTIONS.addOption(STATUS);
        OPTIONS.addOption(STATS);
        OPTIONS.addOption(CREDENTIALS);
    }

    public static void main(String[] args) throws AtlasException, ParseException {
        AtlasAdminClient atlasAdminClient = new AtlasAdminClient();
        int result = atlasAdminClient.run(args);
        System.exit(result);
    }

    private int run(String[] args) throws AtlasException {
        CommandLine commandLine = parseCommandLineOptions(args);
        Configuration configuration = ApplicationProperties.get();
        String[] atlasServerUri = configuration.getStringArray(AtlasConstants.ATLAS_REST_ADDRESS_KEY);

        if (atlasServerUri == null || atlasServerUri.length == 0) {
            atlasServerUri = new String[] { AtlasConstants.DEFAULT_ATLAS_REST_ADDRESS };
        }

        return handleCommand(commandLine, atlasServerUri);
    }

    private int handleCommand(CommandLine commandLine, String[] atlasServerUri) throws AtlasException {
        AtlasClient atlasClient;

        String[] providedUserPassword = getUserPassword(commandLine);

        int cmdStatus = PROGRAM_ERROR_STATUS;
        if (commandLine.hasOption(STATUS.getOpt())) {
            atlasClient = initAtlasClient(atlasServerUri, providedUserPassword); // Status is open API, no auth needed
            try {
                System.out.println(atlasClient.getAdminStatus());
                cmdStatus = 0;
            } catch (AtlasServiceException e) {
                System.err.println("Could not retrieve status of the server at " + Arrays.toString(atlasServerUri));
                printStandardHttpErrorDetails(e);
            }
        } else if (commandLine.hasOption(STATS.getOpt())) {
            atlasClient = initAtlasClient(atlasServerUri, providedUserPassword); // Stats/metrics is open API, no auth needed
            try {
                AtlasMetrics atlasMetrics = atlasClient.getAtlasMetrics();
                String json = AtlasType.toJson(atlasMetrics);
                System.out.println(json);
                cmdStatus = 0;
            } catch (AtlasServiceException e) {
                System.err.println("Could not retrieve metrics of the server at " + Arrays.toString(atlasServerUri));
                printStandardHttpErrorDetails(e);
            }
        } else {
            System.err.println("Unsupported option. Refer to usage for valid options.");
            printUsage();
        }

        return cmdStatus;
    }

    private String[] getUserPassword(CommandLine commandLine) {
        String[] basicAuthUsernamePassword = null;

        // Parse the provided username password
        if (commandLine.hasOption(CREDENTIALS.getOpt())) {
            String value = commandLine.getOptionValue(CREDENTIALS.getOpt());
            if (value != null) {
                basicAuthUsernamePassword = value.split(":");
            }
        }
        if (basicAuthUsernamePassword == null || basicAuthUsernamePassword.length != 2) {
            System.err.println("Invalid credentials. Format: <user>:<password>");
        }
        return basicAuthUsernamePassword;
    }

    private AtlasClient initAtlasClient(final String[] atlasServerUri, final String[] providedUserNamePassword) throws AtlasException {
        AtlasClient atlasClient;

        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            if (providedUserNamePassword == null || providedUserNamePassword.length < 2) {
                atlasClient = new AtlasClient(atlasServerUri, AuthenticationUtil.getBasicAuthenticationInput());
            } else {
                atlasClient = new AtlasClient(atlasServerUri, providedUserNamePassword);
            }
        } else {
            atlasClient = new AtlasClient(atlasServerUri);
        }
        return atlasClient;
    }

    private void printStandardHttpErrorDetails(AtlasServiceException e) {
        System.err.println("Error details: ");
        System.err.println("HTTP Status: " + e.getStatus().getStatusCode() + ","
                + e.getStatus().getReasonPhrase());
        System.err.println("Exception message: " + e.getMessage());
    }

    private CommandLine parseCommandLineOptions(String[] args) {
        if (args.length == 0) {
            printUsage();
        }
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(OPTIONS, args);
        } catch (ParseException e) {
            System.err.println("Could not parse command line options. " + e.getMessage());
            printUsage();
        }
        return commandLine;
    }

    private void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("atlas_admin.py", OPTIONS);
        System.exit(AtlasAdminClient.INVALID_OPTIONS_STATUS);
    }

}
