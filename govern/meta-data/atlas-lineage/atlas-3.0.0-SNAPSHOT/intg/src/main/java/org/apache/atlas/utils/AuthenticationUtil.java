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
package org.apache.atlas.utils;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Console;

/**
 * Util class for Authentication.
 */
public final class AuthenticationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationUtil.class);

    private AuthenticationUtil() {
    }

    public static boolean isKerberosAuthenticationEnabled() {
        return isKerberosAuthenticationEnabled((UserGroupInformation) null);
    }

    public static boolean isKerberosAuthenticationEnabled(UserGroupInformation ugi) {
        boolean defaultValue = ugi != null && ugi.hasKerberosCredentials();

        try {
            return isKerberosAuthenticationEnabled(ApplicationProperties.get(), defaultValue);
        } catch (AtlasException e) {
            LOG.error("Error while isKerberosAuthenticationEnabled ", e);
        }

        return defaultValue;
    }

    public static boolean isKerberosAuthenticationEnabled(Configuration atlasConf) {
        return isKerberosAuthenticationEnabled(atlasConf, false);
    }

    public static boolean isKerberosAuthenticationEnabled(Configuration atlasConf, boolean defaultValue) {
        return atlasConf.getBoolean("atlas.authentication.method.kerberos", defaultValue);
    }

    public static boolean includeHadoopGroups(){
        boolean includeHadoopGroups = false;

        try {
            Configuration configuration = ApplicationProperties.get();

            includeHadoopGroups = configuration.getBoolean("atlas.authentication.ugi-groups.include-hadoop-groups", includeHadoopGroups);
        } catch (AtlasException e) {
            LOG.error("AuthenticationUtil::includeHadoopGroups(). Error while loading atlas application properties ", e);
        }

        return includeHadoopGroups;
    }

    public static String[] getBasicAuthenticationInput() {
        String username = null;
        String password = null;

        try {
            Console console = System.console();
            if (console == null) {
                System.err.println("Couldn't get a console object for user input");
                System.exit(1);
            }

            username = console.readLine("Enter username for atlas :- ");

            char[] pwdChar = console.readPassword("Enter password for atlas :- ");
            if(pwdChar != null) {
                password = new String(pwdChar);
            }

        } catch (Exception e) {
            System.out.print("Error while reading user input");
            System.exit(1);
        }
        return new String[]{username, password};
    }

}
