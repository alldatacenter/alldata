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
package org.shai.xmodifier.util;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Shenghai.Geng
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class XPathPatternQuoter {

    private Map<Character, Character> quoterMap = new HashMap<Character, Character>();

    private Map<Character, Boolean> statusMap = new HashMap<Character, Boolean>();

    /**
     * Check specified character is in quotes collection
     *
     * @param c Character use to check
     */
    public void check(char c) {
        for (Map.Entry<Character, Character> entry : quoterMap.entrySet()) {
            Character start = entry.getKey();
            Character end = entry.getValue();
            if (c == start) {
                Boolean status = statusMap.get(start);
                if (status == null) {
                    status = false;
                }
                statusMap.put(start, !status);
                break;
            }
            if (c == end) {
                Boolean status = statusMap.get(start);
                if (status == null) {
                    status = false;
                }
                statusMap.put(start, !status);
                break;
            }
        }
    }

    /**
     * Check quotes status.
     *
     * @return true if the character is used.
     */
    public boolean isQuoting() {
        for (Map.Entry<Character, Boolean> entry : statusMap.entrySet()) {
            if (entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add quotes
     *
     * @param start
     * @param end
     */
    public void addQuoter(char start, char end) {
        quoterMap.put(start, end);
    }
}
