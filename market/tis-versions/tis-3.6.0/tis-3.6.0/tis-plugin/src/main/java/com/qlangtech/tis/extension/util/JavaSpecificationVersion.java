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

package com.qlangtech.tis.extension.util;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-10 09:08
 **/

import javax.annotation.Nonnull;

public class JavaSpecificationVersion extends VersionNumber {
    private static final String JAVA_SPEC_VERSION_PROPERTY_NAME = "java.specification.version";
    public static final JavaSpecificationVersion JAVA_5 = new JavaSpecificationVersion("1.5");
    public static final JavaSpecificationVersion JAVA_6 = new JavaSpecificationVersion("1.6");
    public static final JavaSpecificationVersion JAVA_7 = new JavaSpecificationVersion("1.7");
    public static final JavaSpecificationVersion JAVA_8 = new JavaSpecificationVersion("1.8");
    public static final JavaSpecificationVersion JAVA_9 = new JavaSpecificationVersion("9");
    public static final JavaSpecificationVersion JAVA_10 = new JavaSpecificationVersion("10");
    public static final JavaSpecificationVersion JAVA_11 = new JavaSpecificationVersion("11");
    public static final JavaSpecificationVersion JAVA_12 = new JavaSpecificationVersion("12");
    public static final JavaSpecificationVersion JAVA_13 = new JavaSpecificationVersion("13");

    public JavaSpecificationVersion(@Nonnull String version) throws NumberFormatException {
        super(normalizeVersion(version));
    }

    @Nonnull
    private static String normalizeVersion(@Nonnull String input) throws NumberFormatException {
        input = input.trim();
        if (input.startsWith("1.")) {
            String[] split = input.split("\\.");
            if (split.length != 2) {
                throw new NumberFormatException("Malformed old Java Specification Version. There should be exactly one dot and something after it: " + input);
            }

            input = split[1];
        }

        int majorVersion = Integer.parseInt(input);
        return majorVersion > 8 ? input : "1." + input;
    }

    @Nonnull
    public static JavaSpecificationVersion forCurrentJVM() throws NumberFormatException {
        String value = System.getProperty("java.specification.version");
        if (value == null) {
            throw new IllegalStateException("Missing mandatory JVM system property: java.specification.version");
        } else {
            return new JavaSpecificationVersion(value);
        }
    }
}
