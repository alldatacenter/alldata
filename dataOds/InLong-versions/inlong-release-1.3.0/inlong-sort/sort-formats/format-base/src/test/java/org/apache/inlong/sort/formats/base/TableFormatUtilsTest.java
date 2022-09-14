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

package org.apache.inlong.sort.formats.base;

import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.junit.Test;

import static org.apache.inlong.sort.formats.base.TableFormatUtils.deserializeBasicField;
import static org.apache.inlong.sort.formats.base.TableFormatUtils.serializeBasicField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link TableFormatUtils}.
 */
public class TableFormatUtilsTest {

    @Test
    public void testDeserializeStringWithoutNullLiteral() {
        Object result1 =
                deserializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "data",
                        null
                );
        assertEquals("data", result1);

        Object result2 =
                deserializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "",
                        null
                );
        assertEquals("", result2);
    }

    @Test
    public void testSerializeStringWithoutNullLiteral() {
        String result1 =
                serializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "data",
                        null
                );
        assertEquals("data", result1);

        String result2 =
                serializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "",
                        null
                );
        assertEquals("", result2);
    }

    @Test
    public void testDeserializeStringWithNullLiteral() {
        Object result1 =
                deserializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "data",
                        "n/a"
                );
        assertEquals("data", result1);

        Object result2 =
                deserializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "",
                        "n/a"
                );
        assertEquals("", result2);

        Object result3 =
                deserializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "n/a",
                        "n/a"
                );
        assertNull(result3);
    }

    @Test
    public void testSerializeStringWithNullLiteral() {
        String result1 =
                serializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "data",
                        "n/a"
                );
        assertEquals("data", result1);

        String result2 =
                serializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        "",
                        "n/a"
                );
        assertEquals("", result2);

        String result3 =
                serializeBasicField(
                        "f",
                        StringFormatInfo.INSTANCE,
                        null,
                        "n/a"
                );
        assertEquals("n/a", result3);
    }

    @Test
    public void testDeserializeNumberWithoutNullLiteral() {
        Object result1 =
                deserializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        "1",
                        null
                );
        assertEquals(1, result1);

        Object result2 =
                deserializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        "",
                        null
                );
        assertNull(result2);
    }

    @Test
    public void testSerializeNumberWithoutNullLiteral() {
        String result1 =
                serializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        1,
                        null
                );
        assertEquals("1", result1);

        String result2 =
                serializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        null,
                        null
                );
        assertEquals("", result2);
    }

    @Test
    public void testDeserializeNumberWithNullLiteral() {
        Object result1 =
                deserializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        "1",
                        "n/a"
                );
        assertEquals(1, result1);

        try {
            deserializeBasicField(
                    "f",
                    IntFormatInfo.INSTANCE,
                    "",
                    "n/a"
            );
            fail("The method is expected to throw an exception.");
        } catch (Exception e) {
            // ignored
        }

        Object result2 =
                deserializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        "n/a",
                        "n/a"
                );
        assertNull(result2);
    }

    @Test
    public void testSerializeNumberWithNullLiteral() {
        String result1 =
                serializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        1,
                        "n/a"
                );
        assertEquals("1", result1);

        String result2 =
                serializeBasicField(
                        "f",
                        IntFormatInfo.INSTANCE,
                        null,
                        "n/a"
                );
        assertEquals("n/a", result2);
    }
}
