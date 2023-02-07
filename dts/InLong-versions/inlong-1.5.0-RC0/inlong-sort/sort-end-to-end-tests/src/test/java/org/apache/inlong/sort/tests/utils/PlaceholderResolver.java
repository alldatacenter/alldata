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

package org.apache.inlong.sort.tests.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A file placeholder replacement tool.
 */
public class PlaceholderResolver {

    /**
     * Default placeholder prefix
     */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /**
     * Default placeholder suffix
     */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /**
     * Default singleton resolver
     */
    private static PlaceholderResolver defaultResolver = new PlaceholderResolver();

    /**
     * Placeholder prefix
     */
    private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    /**
     * Placeholder suffix
     */
    private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    private PlaceholderResolver() {

    }

    private PlaceholderResolver(String placeholderPrefix, String placeholderSuffix) {
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
    }

    public static PlaceholderResolver getDefaultResolver() {
        return defaultResolver;
    }

    public static PlaceholderResolver getResolver(String placeholderPrefix, String placeholderSuffix) {
        return new PlaceholderResolver(placeholderPrefix, placeholderSuffix);
    }

    /**
     * Replace template string with special placeholder according to replace function.
     * @param content  template string with special placeholder
     * @param rule  placeholder replacement rule
     * @return new replaced string
     */
    public String resolveByRule(String content, Function<String, String> rule) {
        int start = content.indexOf(this.placeholderPrefix);
        if (start == -1) {
            return content;
        }
        StringBuilder result = new StringBuilder(content);
        while (start != -1) {
            int end = result.indexOf(this.placeholderSuffix, start);
            // get placeholder actual value (e.g. ${id}, get the value represent id)
            String placeholder = result.substring(start + this.placeholderPrefix.length(), end);
            // replace placeholder value
            String replaceContent = placeholder.trim().isEmpty() ? "" : rule.apply(placeholder);
            result.replace(start, end + this.placeholderSuffix.length(), replaceContent);
            start = result.indexOf(this.placeholderPrefix, start + replaceContent.length());
        }
        return result.toString();
    }

    /**
     * Replace template string with special placeholder according to replace function.
     * @param file  template file with special placeholder
     * @param rule  placeholder replacement rule
     * @return new replaced string
     */
    public Path resolveByRule(Path file, Function<String, String> rule) {
        try {
            List<String> newContents = Files.readAllLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .map(content -> resolveByRule(content, rule))
                    .collect(Collectors.toList());
            Path newPath = Paths.get(file.getParent().toString(), file.getFileName() + "$");
            Files.write(newPath, String.join(System.lineSeparator(), newContents).getBytes(StandardCharsets.UTF_8));
            return newPath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replace template string with special placeholder according to properties file.
     * Key is the content of the placeholder <br/><br/>
     * e.g: content = product:${id}:detail:${did}<br/>
     *      valueMap = id -> 1; pid -> 2<br/>
     *      return: product:1:detail:2<br/>
     *
     * @param content template string with special placeholder
     * @param valueMap placeholder replacement map
     * @return new replaced string
     */
    public String resolveByMap(String content, final Map<String, Object> valueMap) {
        return resolveByRule(content, placeholderValue -> String.valueOf(valueMap.get(placeholderValue)));
    }

    /**
     * Replace template string with special placeholder according to properties file.
     * Key is the content of the placeholder <br/><br/>
     * e.g: content = product:${id}:detail:${did}<br/>
     *      valueMap = id -> 1; pid -> 2<br/>
     *      return: product:1:detail:2<br/>
     *
     * @param file template string with special placeholder
     * @param valueMap placeholder replacement map
     * @return new replaced string
     */
    public Path resolveByMap(Path file, final Map<String, Object> valueMap) {
        return resolveByRule(file, placeholderValue -> String.valueOf(valueMap.get(placeholderValue)));
    }
}
