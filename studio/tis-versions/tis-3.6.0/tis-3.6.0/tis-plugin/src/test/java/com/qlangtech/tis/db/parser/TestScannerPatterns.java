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

package com.qlangtech.tis.db.parser;

import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-11 10:20
 **/
public class TestScannerPatterns extends TestCase {
    public void testTT_HOSTMatch() {
        Pattern pattern = ScannerPatterns.TokenTypes.TT_HOST.createPattern();
        Matcher matcher = pattern.matcher("localhost");
        // System.out.println(matcher.matches());
        assertTrue(matcher.matches());
        matcher = pattern.matcher("192.168.28.200");
        assertTrue(matcher.matches());
        matcher = pattern.matcher("localhostt");
        assertFalse(matcher.matches());

        matcher = pattern.matcher("baisui.com");
        assertTrue(matcher.matches());

        matcher = pattern.matcher(":localhost");
        assertFalse(matcher.find());
    }
}
