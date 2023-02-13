/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.plugin;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class for deal with jar hell.
 */
public class JarHell {

    public static final JavaVersion CURRENT_VERSION = new JavaVersion(Lists.newArrayList(1, 8), null);

    private JarHell() {
    }

    /**
     * Check the validation for java version
     */
    public static void checkJavaVersion(String resource, String javaVersion) {
        JavaVersion version = parse(javaVersion);
        if (CURRENT_VERSION.compareTo(version) < 0) {
            throw new IllegalArgumentException(
                    String.format("%s requires Java %s, your system: %s", resource, CURRENT_VERSION, javaVersion));
        }
    }

    /**
     * Parse the java version from the given value
     */
    public static JavaVersion parse(String value) {
        Objects.requireNonNull(value);
        String prePart = null;
        if (!isValid(value)) {
            throw new IllegalArgumentException("Java version string [" + value + "] could not be parsed.");
        }
        List<Integer> version = new ArrayList<>();
        String[] parts = value.split("-");
        String[] numericComponents;
        if (parts.length == 1) {
            numericComponents = value.split("\\.");
        } else if (parts.length == 2) {
            numericComponents = parts[0].split("\\.");
            prePart = parts[1];
        } else {
            throw new IllegalArgumentException("Java version string [" + value + "] could not be parsed.");
        }

        for (String component : numericComponents) {
            version.add(Integer.valueOf(component));
        }
        return new JavaVersion(version, prePart);
    }

    /**
     * Check the string if is valid.
     */
    public static boolean isValid(String value) {
        return value.matches("^0*[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9]+)?$");
    }

    /**
     * Java version class
     */
    public static class JavaVersion {

        private final List<Integer> version;
        private final String prePart;

        private JavaVersion(List<Integer> version, String prePart) {
            this.prePart = prePart;
            if (version.size() >= 2 && version.get(0) == 1 && version.get(1) == 8) {
                // for Java 8 there is ambiguity since both 1.8 and 8 are supported,
                version = new ArrayList<>(version.subList(1, version.size()));
            }
            this.version = Collections.unmodifiableList(version);
        }

        public List<Integer> getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return StringUtils.join(version, '.') + "-" + prePart;
        }

        /**
         * Compare to other version
         */
        public int compareTo(JavaVersion o) {
            int len = Math.max(version.size(), o.version.size());
            for (int i = 0; i < len; i++) {
                int d = (i < version.size() ? version.get(i) : 0);
                int s = (i < o.version.size() ? o.version.get(i) : 0);
                if (s < d) {
                    return 1;
                }
                if (s > d) {
                    return -1;
                }
            }
            if (prePart != null && o.prePart == null) {
                return -1;
            } else if (prePart == null && o.prePart != null) {
                return 1;
            } else if (prePart != null) {
                return comparePrePart(prePart, o.prePart);
            }
            return 0;
        }

        private int comparePrePart(String prePart, String otherPrePart) {
            if (prePart.matches("\\d+")) {
                return otherPrePart.matches("\\d+")
                        ? (new BigInteger(prePart)).compareTo(new BigInteger(otherPrePart))
                        : -1;
            } else {
                return otherPrePart.matches("\\d+") ? 1 : prePart.compareTo(otherPrePart);
            }
        }
    }
}
