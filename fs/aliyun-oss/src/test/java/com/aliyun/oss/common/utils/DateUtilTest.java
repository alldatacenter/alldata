/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.aliyun.oss.common.utils;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class DateUtilTest {

    @Test
    public void testFormatGMTDate() {
        String expectedRegex = "\\w{3}, \\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT";

        String actual = DateUtil.formatRfc822Date(new Date());
        assertTrue(actual.matches(expectedRegex));
    }

    @Test
    public void testFormatIso8601Date() {
        String expectedRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

        String actual = DateUtil.formatAlternativeIso8601Date(new Date());
        assertTrue(actual.matches(expectedRegex));
    }

    @Test
    public void testParseIso8601Date() {
        try {
            DateUtil.parseIso8601Date("invalid");
            assertTrue(false);
        } catch (ParseException e) {
            assertTrue(true);
        }
    }
}
