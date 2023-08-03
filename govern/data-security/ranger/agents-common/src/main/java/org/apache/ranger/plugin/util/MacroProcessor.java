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

package org.apache.ranger.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MacroProcessor.class);

    private final Map<String, String> macrosMap;
    private final Pattern             macrosPattern;

    public MacroProcessor(Map<String, String> macrosMap) {
        this.macrosMap     = macrosMap;
        this.macrosPattern = getMacrosPattern(this.macrosMap);
    }

    public String expandMacros(String expr) {
        StringBuffer ret = null;

        if (expr != null) {
            Matcher matcher = macrosPattern.matcher(expr);

            while (matcher.find()) {
                if (ret == null) {
                    ret = new StringBuffer();
                }

                String keyword  = matcher.group();
                String replacer = macrosMap.get(keyword);

                matcher.appendReplacement(ret, replacer);
            }

            if (ret == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("expandMacros({}): no match found!", expr);
                }
            } else {
                matcher.appendTail(ret);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("expandMacros({}): match found. ret={}", expr, ret);
                }
            }
        }

        return ret != null ? ret.toString() : expr;
    }

    private Pattern getMacrosPattern(Map<String, String> macros) {
        StringBuilder sb  = new StringBuilder();
        String        sep = "\\b(";

        for (String macro : macros.keySet()) {
            sb.append(sep).append(macro);

            sep = "|";
        }

        sb.append(")\\b");

        return Pattern.compile(sb.toString());
    }
}
