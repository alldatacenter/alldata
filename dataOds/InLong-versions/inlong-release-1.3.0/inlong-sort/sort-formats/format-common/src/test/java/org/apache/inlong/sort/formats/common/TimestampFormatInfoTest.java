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

package org.apache.inlong.sort.formats.common;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

/**
 * Unit tests for {@link TimestampFormatInfo}.
 */
public class TimestampFormatInfoTest extends FormatInfoTestBase {

    @Override
    Collection<FormatInfo> createFormatInfos() {
        return Collections.singletonList(new TimestampFormatInfo("MILLIS"));
    }

    @Test
    public void testSerialize() {
        Timestamp timestamp = Timestamp.valueOf("2020-03-22 11:12:13");

        assertEquals("2020-03-22 11:12:13", new TimestampFormatInfo("yyyy-MM-dd hh:mm:ss").serialize(timestamp));
        assertEquals("22/03/2020 11:12:13", new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss").serialize(timestamp));
        assertEquals("2020-03-22 11:12:13", new TimestampFormatInfo().serialize(timestamp));
    }

    @Test
    public void testDeserialize() throws ParseException {
        Timestamp timestamp = Timestamp.valueOf("2020-03-22 11:12:13");

        assertEquals(timestamp, new TimestampFormatInfo("yyyy-MM-dd hh:mm:ss").deserialize("2020-03-22 11:12:13"));
        assertEquals(timestamp, new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss").deserialize("22/03/2020 11:12:13"));
        assertEquals(timestamp, new TimestampFormatInfo().deserialize("2020-03-22 11:12:13"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new TimestampFormatInfo("MINUTES");
    }

    @Test(expected = ParseException.class)
    public void testUnmatchedText() throws ParseException {
        new TimestampFormatInfo("HH:mm:ss yyyy-MM-dd").deserialize("2020-03-22 11:12:13");
    }
}
