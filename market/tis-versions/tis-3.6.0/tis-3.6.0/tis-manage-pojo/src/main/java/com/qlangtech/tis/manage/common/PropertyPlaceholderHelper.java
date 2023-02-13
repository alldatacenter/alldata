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
package com.qlangtech.tis.manage.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility class for working with Strings that have placeholder values in them.
 * A placeholder takes the form <code>${name}</code>. Using
 * <code>PropertyPlaceholderHelper</code> these placeholders can be substituted
 * for user-supplied values.
 * <p>
 * Values for substitution can be supplied using a {@link Properties} instance
 * or using a {@link PlaceholderResolver}.
 * @since 3.0
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class PropertyPlaceholderHelper {

    private static final Logger logger = LoggerFactory.getLogger(PropertyPlaceholderHelper.class);

    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<String, String>(4);

    private static final PropertyPlaceholderHelper defaultHelper = new PropertyPlaceholderHelper("${", "}");

    public static void main(String[] args) {
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> params = new HashMap<>();
        params.put("name", "baisui");
        System.out.println(helper.replacePlaceholders("hello ${name}", new PlaceholderResolver() {

            @Override
            public String resolvePlaceholder(String placeholderName) {
                return params.get(placeholderName);
            }
        }));
    }

    /**
     * 默认替换参数
     *
     * @param value
     * @param
     * @return
     */
    public static String replace(String value, PlaceholderResolver paramsResolver) {
        return defaultHelper.replacePlaceholders(value, paramsResolver);
    }

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    private final String placeholderPrefix;

    private final String placeholderSuffix;

    private final String simplePrefix;

    private final String valueSeparator;

    private final boolean ignoreUnresolvablePlaceholders;

    /**
     * Creates a new <code>PropertyPlaceholderHelper</code> that uses the supplied
     * prefix and suffix. Unresolvable placeholders are ignored.
     *
     * @param placeholderPrefix
     *            the prefix that denotes the start of a placeholder.
     * @param placeholderSuffix
     *            the suffix that denotes the end of a placeholder.
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * Creates a new <code>PropertyPlaceholderHelper</code> that uses the supplied
     * prefix and suffix.
     *
     * @param placeholderPrefix
     *            the prefix that denotes the start of a placeholder
     * @param placeholderSuffix
     *            the suffix that denotes the end of a placeholder
     * @param valueSeparator
     *            the separating character between the placeholder variable and the
     *            associated default value, if any
     * @param ignoreUnresolvablePlaceholders
     *            indicates whether unresolvable placeholders should be ignored
     *            (<code>true</code>) or cause an exception (<code>false</code>).
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix, String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
        notNull(placeholderPrefix, "placeholderPrefix must not be null");
        notNull(placeholderSuffix, "placeholderSuffix must not be null");
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * Replaces all placeholders of format <code>${name}</code> with the
     * corresponding property from the supplied {@link Properties}.
     *
     * @param value
     *            the value containing the placeholders to be replaced.
     * @param properties
     *            the <code>Properties</code> to use for replacement.
     * @return the supplied value with placeholders replaced inline.
     */
    public String replacePlaceholders(String value, final Properties properties) {
        notNull(properties, "Argument 'properties' must not be null.");
        return replacePlaceholders(value, new PlaceholderResolver() {

            public String resolvePlaceholder(String placeholderName) {
                return properties.getProperty(placeholderName);
            }
        });
    }

    /**
     * Replaces all placeholders of format <code>${name}</code> with the value
     * returned from the supplied {@link PlaceholderResolver}.
     *
     * @param value
     *            the value containing the placeholders to be replaced.
     * @param placeholderResolver
     *            the <code>PlaceholderResolver</code> to use for replacement.
     * @return the supplied value with placeholders replaced inline.
     */
    public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
        notNull(value, "Argument 'value' must not be null.");
        return parseStringValue(value, placeholderResolver, new HashSet<String>());
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    protected String parseStringValue(String strVal, PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {
        StringBuilder buf = new StringBuilder(strVal);
        int startIndex = strVal.indexOf(this.placeholderPrefix);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                if (!visitedPlaceholders.add(placeholder)) {
                    throw new IllegalArgumentException("Circular placeholder reference '" + placeholder + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
                // Now obtain the value for the fully resolved key...
                String propVal = placeholderResolver.resolvePlaceholder(placeholder);
                if (propVal == null && this.valueSeparator != null) {
                    int separatorIndex = placeholder.indexOf(this.valueSeparator);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                        propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                    buf.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resolved placeholder '" + placeholder + "'");
                    }
                    startIndex = buf.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                } else if (this.ignoreUnresolvablePlaceholders) {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                } else {
                    throw new IllegalArgumentException("Could not resolve placeholder '" + placeholder + "'");
                }
                visitedPlaceholders.remove(placeholder);
            } else {
                startIndex = -1;
            }
        }
        return buf.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, this.simplePrefix)) {
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Strategy interface used to resolve replacement values for placeholders
     * contained in Strings.
     *
     * @see PropertyPlaceholderHelper
     */
    public static interface PlaceholderResolver {

        /**
         * Resolves the supplied placeholder name into the replacement value.
         *
         * @param placeholderName
         *            the name of the placeholder to resolve.
         * @return the replacement value or <code>null</code> if no replacement is to be
         *         made.
         */
        String resolvePlaceholder(String placeholderName);
    }
}
