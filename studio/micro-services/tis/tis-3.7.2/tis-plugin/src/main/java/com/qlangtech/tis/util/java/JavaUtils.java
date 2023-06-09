/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.util.java;

//import hudson.util.VersionNumber;
//import io.jenkins.lib.versionnumber.JavaSpecificationVersion;
//import org.kohsuke.accmod.Restricted;
//import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Utility class for Java environment management and checks.
 * @author Oleg Nenashev
 */
public class JavaUtils {

    private JavaUtils() {
        // Cannot construct
    }

    /**
     * Check whether the current JVM is running with Java 8 or below
     * @return {@code true} if it is Java 8 or older version
     */
    public static boolean isRunningWithJava8OrBelow() {
        String javaVersion = getCurrentRuntimeJavaVersion();
        return javaVersion.startsWith("1.");
    }

    /**
     * Check whether the current JVM is running with Java 9 or above.
     * @return {@code true} if it is Java 9 or above
     */
    public static boolean isRunningWithPostJava8() {
        String javaVersion = getCurrentRuntimeJavaVersion();
        return !javaVersion.startsWith("1.");
    }

//    /**
//     * Returns the JVM's current version as a {@link 'VersionNumber'} instance.
//     */
//    public static JavaSpecificationVersion getCurrentJavaRuntimeVersionNumber() {
//        return JavaSpecificationVersion.forCurrentJVM();
//    }

    /**
     * Returns the JVM's current version as a {@link String}.
     * See https://openjdk.java.net/jeps/223 for the expected format.
     * <ul>
     *     <li>Until Java 8 included, the expected format should be starting with {@code 1.x}</li>
     *     <li>Starting with Java 9, cf. JEP-223 linked above, the version got simplified in 9.x, 10.x, etc.</li>
     * </ul>
     *
     * @see System#getProperty(String)
     */
    public static String getCurrentRuntimeJavaVersion() {
        // TODO: leverage Runtime.version() once on Java 9+
        return System.getProperty("java.specification.version");
    }
}
