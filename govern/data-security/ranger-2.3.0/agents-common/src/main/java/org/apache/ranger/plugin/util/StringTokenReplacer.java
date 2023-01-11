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

import java.util.Map;

public class StringTokenReplacer {
    private final char startChar;
    private final char endChar;
    private final char escapeChar;
    private final String tokenPrefix;

    public StringTokenReplacer(char startChar, char endChar, char escapeChar, String tokenPrefix) {
        this.startChar  = startChar;
        this.endChar    = endChar;
        this.escapeChar = escapeChar;
        this.tokenPrefix = tokenPrefix;
    }

    public static boolean hasToken(String value, char startDelimiterChar, char endDelimiterChar, char escapeChar) {
        return value != null && (value.indexOf(escapeChar) != -1 || (value.indexOf(startDelimiterChar) != -1 && value.indexOf(endDelimiterChar) != -1));
    }

    public String replaceTokens(String value, Map<String, Object> tokens) {
        if(tokens == null || tokens.size() < 1 || !hasToken(value, startChar, endChar, escapeChar)) {
            return value;
        }

        StringBuilder ret   = new StringBuilder();
        StringBuilder token = null;

        for(int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if(c == escapeChar) {
                i++;
                if(i < value.length()) {
                    c = value.charAt(i);
                    if (token != null) {
                        // if next char is not the escape char or endChar, retain the escapeChar
                        if (c != escapeChar && c != endChar) {
                            token.append(escapeChar);
                        }
                        token.append(c);
                    } else {
                        // if next char is not the escape char or startChar, retain the escapeChar
                        if (c != escapeChar && c != startChar) {
                            ret.append(escapeChar);
                        }
                        ret.append(c);
                    }
                } else {
                    if (token != null) {
                        token.append(escapeChar);
                    } else {
                        ret.append(escapeChar);
                    }
                }
                continue;
            }

            if(token == null) { // not in token
                if(c == startChar) {
                    token = new StringBuilder();
                } else {
                    ret.append(c);
                }
            } else { // in token
                if(c == endChar) {
                    String rawToken = token.toString();
                    if (tokenPrefix.length() == 0 || rawToken.startsWith(tokenPrefix)) {
                        Object replaced = RangerAccessRequestUtil.getTokenFromContext(tokens, rawToken.substring(tokenPrefix.length()));
                        if (replaced != null) {
                            ret.append(replaced.toString());
                        } else {
                            ret.append(startChar).append(token).append(endChar);
                        }
                    } else {
                        ret.append(startChar).append(token).append(endChar);
                    }
                    token = null;
                } else {
                    token.append(c);
                }
            }
        }

        if(token != null) { // if no endChar is found
            ret.append(startChar).append(token);
        }

        return ret.toString();
    }  
}
