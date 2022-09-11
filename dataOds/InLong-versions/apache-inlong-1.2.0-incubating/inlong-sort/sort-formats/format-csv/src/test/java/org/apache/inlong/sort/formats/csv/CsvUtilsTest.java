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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.csv;

import org.apache.inlong.sort.formats.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for csv splitting and concating.
 */
public class CsvUtilsTest {

    @Test
    public void testSplitNormal() {

        Assert.assertArrayEquals(
                new String[]{"a", "b", "c", "d"},
                StringUtils.splitCsv("a|b|c|d", '|', null, null)
        );

        Assert.assertArrayEquals(
                new String[]{"", "a", "b", "c", "d"},
                StringUtils.splitCsv("|a|b|c|d", '|', null, null)
        );

        Assert.assertArrayEquals(
                new String[]{"a", "b", "c", "d", ""},
                StringUtils.splitCsv("a|b|c|d|", '|', null, null)
        );

        Assert.assertArrayEquals(
                new String[]{"a", "b|c", "d"},
                StringUtils.splitCsv("a|b\\|c|d", '|', '\\', null)
        );

        Assert.assertArrayEquals(
                new String[]{"a", "b\\", "c", "d"},
                StringUtils.splitCsv("a|b\\\\|c|d", '|', '\\', null)
        );

        Assert.assertArrayEquals(
                new String[]{"a", "b|c", "d"},
                StringUtils.splitCsv("a|\"b|c\"|d", '|', '\\', '\"')
        );

        Assert.assertArrayEquals(
                new String[]{"a", "|b|c|", "d"},
                StringUtils.splitCsv("a|\"|b|c|\"|d", '|', '\\', '\"')
        );

        Assert.assertArrayEquals(
                new String[]{"a", "b\\c", "d"},
                StringUtils.splitCsv("a|\"b\\c\"|d", '|', '\\', '\"')
        );

        Assert.assertArrayEquals(
                new String[]{"a", "\"b", "c\"", "d"},
                StringUtils.splitCsv("a|\\\"b|c\\\"|d", '|', '\\', '\"')
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitUnclosedEscaping() {
        StringUtils.splitCsv("a|b\\", '|', '\\', '\"');
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitUnclosedQuoting() {
        StringUtils.splitCsv("a|b\"", '|', '\\', '\"');
    }

    @Test
    public void testConcatNormal() {
        Assert.assertEquals(
                "a|b|c|d",
                StringUtils.concatCsv(new String[]{"a", "b", "c", "d"}, '|', null, null)
        );

        Assert.assertEquals(
                "a|\\|b|c|d",
                StringUtils.concatCsv(new String[]{"a", "|b", "c", "d"}, '|', '\\', null)
        );

        Assert.assertEquals(
                "a|\\\\b|c|d",
                StringUtils.concatCsv(new String[]{"a", "\\b", "c", "d"}, '|', '\\', null)
        );

        Assert.assertEquals(
                "a|\\\"b|c|d",
                StringUtils.concatCsv(new String[]{"a", "\"b", "c", "d"}, '|', '\\', '\"')
        );

        Assert.assertEquals(
                "a|\"|\"b|c|d",
                StringUtils.concatCsv(new String[]{"a", "|b", "c", "d"}, '|', null, '\"')
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConcatNoEscapingAndQuoting() {
        StringUtils.concatCsv(new String[]{"a", "|b", "c", "d"}, '|', null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConcatNoEscaping() {
        StringUtils.concatCsv(new String[]{"a", "\"b", "c", "d"}, '|', null, '\"');
    }
}
