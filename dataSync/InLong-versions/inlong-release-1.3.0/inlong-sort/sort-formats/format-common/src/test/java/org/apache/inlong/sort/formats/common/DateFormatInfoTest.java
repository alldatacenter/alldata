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

import java.sql.Date;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for {@link DateFormatInfo}.
 */
public class DateFormatInfoTest extends FormatInfoTestBase {

    @Override
    Collection<FormatInfo> createFormatInfos() {
        return Collections.singletonList(new DateFormatInfo("YYYY-MM-DD"));
    }

    @Test
    public void testSerialize() {
        Date date = Date.valueOf("2020-03-22");

        assertEquals("2020-03-22", new DateFormatInfo("yyyy-MM-dd").serialize(date));
        assertEquals("22/03/2020", new DateFormatInfo("dd/MM/yyyy").serialize(date));
        assertEquals("2020-03-22", new DateFormatInfo().serialize(date));
    }

    @Test
    public void testDeserialize() throws ParseException {
        Date date = Date.valueOf("2020-03-22");

        assertEquals(date, new DateFormatInfo("yyyy-MM-dd").deserialize("2020-03-22"));
        assertEquals(date, new DateFormatInfo("dd/MM/yyyy").deserialize("22/03/2020"));
        assertEquals(date, new DateFormatInfo().deserialize("2020-03-22"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new DateFormatInfo("MINUTES");
    }

    @Test(expected = ParseException.class)
    public void testUnmatchedText() throws ParseException {
        new DateFormatInfo("yyyy-MM-dd").deserialize("22/03/2020");
    }
}
