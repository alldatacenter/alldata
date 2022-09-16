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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;

/**
 * Unit tests for {@link RowFormatInfo}.
 */
public class RowFormatInfoTest extends FormatInfoTestBase {

    @Override
    Collection<FormatInfo> createFormatInfos() {
        RowFormatInfo formatInfo1 =
                new RowFormatInfo(
                        new String[]{"f1", "f2", "f3"},
                        new FormatInfo[]{
                                StringFormatInfo.INSTANCE,
                                IntFormatInfo.INSTANCE,
                                StringFormatInfo.INSTANCE
                        }
                );

        RowFormatInfo formatInfo2 =
                new RowFormatInfo(
                        new String[]{"f1"},
                        new FormatInfo[]{
                                new MapFormatInfo(
                                        StringFormatInfo.INSTANCE,
                                        IntFormatInfo.INSTANCE
                                )
                        }
                );

        RowFormatInfo formatInfo3 =
                new RowFormatInfo(
                        new String[]{"f1", "f2"},
                        new FormatInfo[]{
                                new RowFormatInfo(
                                        new String[]{"f1"},
                                        new FormatInfo[]{IntFormatInfo.INSTANCE}
                                ),
                                new MapFormatInfo(
                                        StringFormatInfo.INSTANCE,
                                        new TimeFormatInfo("hh:mm:ss.SSS")
                                )
                        }
                );

        return Arrays.asList(formatInfo1, formatInfo2, formatInfo3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateFields() {
        new RowFormatInfo(
                new String[]{"f", "f"},
                new FormatInfo[]{
                        StringFormatInfo.INSTANCE,
                        IntFormatInfo.INSTANCE
                }
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInconsistentFields() {
        new RowFormatInfo(
                new String[]{"f"},
                new FormatInfo[]{
                        StringFormatInfo.INSTANCE,
                        IntFormatInfo.INSTANCE
                }
        );
    }
}
