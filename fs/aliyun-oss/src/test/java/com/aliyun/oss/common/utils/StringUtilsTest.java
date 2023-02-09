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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the StringUtils class.
 */
public class StringUtilsTest {

    /**
     * Tests that we can correctly convert Bytes to strings.
     */
    @Test
    public void testFromByte() {
        assertEquals("123", StringUtils.fromByte(new Byte("123")));
        assertEquals("-99", StringUtils.fromByte(new Byte("-99")));
    }

    @Test(timeout = 10 * 1000)
    public void replace_ReplacementStringContainsMatchString_DoesNotCauseInfiniteLoop() {
        assertEquals("aabc", StringUtils.replace("abc", "a", "aa"));
    }

    @Test
    public void replace_EmptyReplacementString_RemovesAllOccurencesOfMatchString() {
        assertEquals("bbb", StringUtils.replace("ababab", "a", ""));
    }

    @Test
    public void replace_MatchNotFound_ReturnsOriginalString() {
        assertEquals("abc", StringUtils.replace("abc", "d", "e"));
    }

    @Test
    public void lowerCase_NonEmptyString() {
        String input = "x-amz-InvocAtion-typE";
        String expected = "x-amz-invocation-type";
        assertEquals(expected, StringUtils.lowerCase(input));
    }

    @Test
    public void lowerCase_NullString() {
        assertNull(StringUtils.lowerCase(null));
    }

    @Test
    public void lowerCase_EmptyString() {
        assertEquals(StringUtils.lowerCase(""), "");
    }

    @Test
    public void upperCase_NonEmptyString() {
        String input = "dHkdjj139_)(e";
        String expected = "DHKDJJ139_)(E";
        assertEquals(expected, StringUtils.upperCase(input));
    }

    @Test
    public void upperCase_NullString() {
        assertNull(StringUtils.upperCase((null)));
    }

    @Test
    public void upperCase_EmptyString() {
        assertEquals(StringUtils.upperCase(""), "");
    }

    @Test
    public void testCompare() {
        assertTrue(StringUtils.compare("truck", "Car") > 0);
        assertTrue(StringUtils.compare("", "dd") < 0);
        assertTrue(StringUtils.compare("dd", "") > 0);
        assertEquals(0, StringUtils.compare("", ""));
        assertTrue(StringUtils.compare(" ", "") > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompare_String1Null() {
        String str1 = null;
        String str2 = "test";
        StringUtils.compare(str1, str2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompare_String2Null() {
        String str1 = "test";
        String str2 = null;
        StringUtils.compare(str1, str2);
    }

    @Test
    public void testAppendAndCompact() {
        String[] pieces = {" ", "\t", "\n", "\u000b", "\r", "\f", "word", "foo", "bar", "baq"};
        int ITERATIONS = 10000;
        Random rng = new Random();

        for (int i = 0; i < ITERATIONS; i++) {
            int parts = rng.nextInt(10);
            String s = "";
            for (int j = 0; j < parts; j++) {
                s = s + pieces[rng.nextInt(pieces.length)];
            }

            StringBuilder sb = new StringBuilder();
            StringUtils.appendCompactedString(sb, s);
            String compacted = s.replaceAll("\\s+", " ");
            assertEquals('[' + compacted + ']', sb.toString(), compacted);
        }
    }

    @Test
    public void begins_with_ignore_case() {
        assertTrue(StringUtils.beginsWithIgnoreCase("foobar", "FoO"));
    }

    @Test
    public void begins_with_ignore_case_returns_false_when_seq_doesnot_match() {
        assertFalse(StringUtils.beginsWithIgnoreCase("foobar", "baz"));
    }

    @Test
    public void hasValue() {
        assertTrue(StringUtils.hasValue("something"));
        assertFalse(StringUtils.hasValue(null));
        assertFalse(StringUtils.hasValue(""));
    }

    @Test
    public void testToFromValue() {
        StringBuilder value;

        value = new StringBuilder();
        value.append(1);
        assertEquals(new Integer(1), StringUtils.toInteger(value));

        value = new StringBuilder();
        value.append("hello");
        assertEquals("hello", StringUtils.toString(value));

        value = new StringBuilder();
        value.append("false");
        assertEquals(false, StringUtils.toBoolean(value));

        assertEquals(BigInteger.valueOf(123), StringUtils.toBigInteger("123"));

        assertEquals(BigDecimal.valueOf(123), StringUtils.toBigDecimal("123"));

        assertEquals("123", StringUtils.fromInteger(123));
        assertEquals("123", StringUtils.fromLong((long) 123));
        assertEquals("hello", StringUtils.fromString("hello"));
        assertEquals("false", StringUtils.fromBoolean(false));
        assertEquals("123", StringUtils.fromBigInteger(BigInteger.valueOf(123)));
        assertEquals("123", StringUtils.fromBigDecimal(BigDecimal.valueOf(123)));
        assertEquals("123.1", StringUtils.fromFloat((float) 123.1));
        assertEquals("123.2", StringUtils.fromDouble((double) 123.2));
    }

    @Test
    public void testJoinString() {
        String part1 = "hello";
        String part2 = "world";
        String join = StringUtils.join("-", part1, part2);
        assertEquals("hello-world", join);

        Collection<String> collection = new ArrayList<String>();
        collection.add("hello");
        collection.add("world");
        join = StringUtils.join("-", collection);
        assertEquals("hello-world", join);
    }
}
