/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.sort.util;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.pojo.fieldformat.ArrayFormat;
import org.apache.inlong.manager.pojo.fieldformat.BinaryFormat;
import org.apache.inlong.manager.pojo.fieldformat.DecimalFormat;
import org.apache.inlong.manager.pojo.fieldformat.MapFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat.Element;
import org.apache.inlong.manager.pojo.fieldformat.VarBinaryFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FieldFormatUtilsTest {

    @Test
    public void testDecimalFormat() {
        String formatJson = FieldFormatUtils.createDecimalFormat(5, 3);
        DecimalFormat format = FieldFormatUtils.parseDecimalFormat(formatJson);
        Assertions.assertTrue(format.getPrecision() == 5);
        Assertions.assertTrue(format.getScale() == 3);
    }

    @Test
    public void testArrayFormatOfStringElement() {
        String formatJson = FieldFormatUtils.createArrayFormat(FieldType.STRING, null);
        ArrayFormat format = FieldFormatUtils.parseArrayFormat(formatJson);
        Assertions.assertTrue(format.getElementType() == FieldType.STRING);
        Assertions.assertTrue(format.getElementFormat() == null);
    }

    @Test
    public void testArrayFormatOfDecimalElement() {
        String elementFormatJson = FieldFormatUtils.createDecimalFormat(5, 3);
        String formatJson = FieldFormatUtils.createArrayFormat(FieldType.DECIMAL, elementFormatJson);
        ArrayFormat format = FieldFormatUtils.parseArrayFormat(formatJson);
        Assertions.assertTrue(format.getElementType() == FieldType.DECIMAL);
        DecimalFormat elementFormat = FieldFormatUtils.parseDecimalFormat(format.getElementFormat());
        Assertions.assertTrue(elementFormat.getPrecision() == 5);
        Assertions.assertTrue(elementFormat.getScale() == 3);
    }

    @Test
    public void testMapFormat() {
        String formatJson = FieldFormatUtils.createMapFormat(FieldType.STRING, null, FieldType.BIGINT, null);
        MapFormat format = FieldFormatUtils.parseMapFormat(formatJson);
        Assertions.assertTrue(format.getKeyType() == FieldType.STRING);
        Assertions.assertTrue(format.getKeyFormat() == null);
        Assertions.assertTrue(format.getValueType() == FieldType.BIGINT);
        Assertions.assertTrue(format.getValueFormat() == null);
    }

    @Test
    public void testStructFormat() {
        List<FieldType> elementTypes = Lists.newArrayList(FieldType.BIGINT, FieldType.STRING);
        List<String> elementFormats = Lists.newArrayList(null, null);
        Element id = new Element("id", FieldType.INT, null);
        Element name = new Element("name", FieldType.STRING, null);
        String formatJson = FieldFormatUtils.createStructFormat(Lists.newArrayList(id, name));
        StructFormat format = FieldFormatUtils.parseStructFormat(formatJson);
        Assertions.assertTrue(format.getElements().size() == 2);
        Assertions.assertTrue(format.getElements().get(0).getFieldName().equals("id"));
        Assertions.assertTrue(format.getElements().get(0).getFieldType() == FieldType.INT);
    }

    @Test
    public void testBinaryFormat() {
        String formatJson = FieldFormatUtils.createBinaryFormat(5);
        BinaryFormat format = FieldFormatUtils.parseBinaryFormat(formatJson);
        Assertions.assertEquals(5, (int) format.getLength());
    }

    @Test
    public void testVarBinaryFormat() {
        String formatJson = FieldFormatUtils.createVarBinaryFormat(5);
        VarBinaryFormat format = FieldFormatUtils.parseVarBinaryFormat(formatJson);
        Assertions.assertEquals(5, (int) format.getLength());
    }
}
