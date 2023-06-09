/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.db.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ScannerPatterns {

    public static final String HOST_KEY = "host";

    public enum TokenTypes {
        //
        TT_HOST_DESC("^" + HOST_KEY, true),
        //
        TT_PASSWORD("^password", true),
        //
        TT_USERNAME("^username", true),
        //
        TT_PORT("^port", true),
        //
        TT_MYSQL("^mysql", true),
        //
        TT_LEFT("^\\{", true),
        //
        TT_RIGHT("^\\}", true),
        //
        TT_DBDESC_SPLIT("^,", true),
        //
        TT_RANGE_LEFT("^\\[", true),
        //
        TT_RANGE_RIGHT("^\\]", true),
        //
        TT_RANGE_MINUS("^-", true),
        // http://www.regular-expressions.info/ip.html
        TT_IP(//
                "^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])", true),
        //
        TT_RANGE_NUMBER("^\\d+", true),
        //
        TT_WHITESPACE("^(\\s)+", false),
        TT_HOST(//
                "(^([a-z0-9][a-z0-9\\-]{0,61}[a-z0-9])(\\.([a-z0-9][a-z0-9\\-]{0,61}[a-z0-9]))+)|^localhost", true),
        //
        TT_COLON("^:", true),
        //
        TT_IDENTIFIER("^(\\S+)[\\r|\\n]?", true, 1),
        TT_EOF("^EOF", false);

        private final String regExpattern;

        private final boolean outputToken;

        private final int gourpIndex;

        public int getGourpIndex() {
            return gourpIndex;
        }

        TokenTypes(String regExpattern, boolean outputToken) {
            this(regExpattern, outputToken, -1);
        }

        TokenTypes(String regExpattern, boolean outputToken, int group) {
            this.regExpattern = regExpattern;
            this.outputToken = outputToken;
            this.gourpIndex = group;
        }

        public Pattern createPattern() {
            return Pattern.compile(regExpattern);
        }

    }

    private static List<ScanRecognizer> patternMatchers;

    public static List<ScanRecognizer> loadPatterns() {
        Pattern pattern = null;
        if (patternMatchers == null) {
            synchronized (ScannerPatterns.class) {
                if (patternMatchers == null) {
                    patternMatchers = new ArrayList<ScanRecognizer>();
                    for (TokenTypes t : TokenTypes.values()) {
                        pattern = t.createPattern();
                        patternMatchers.add(new ScanRecognizer(t, pattern, t.outputToken));
                    }
                    patternMatchers = Collections.unmodifiableList(patternMatchers);
                }
            }
        }
        return patternMatchers;
    }

    public static void main(String[] args) {
    }
}
