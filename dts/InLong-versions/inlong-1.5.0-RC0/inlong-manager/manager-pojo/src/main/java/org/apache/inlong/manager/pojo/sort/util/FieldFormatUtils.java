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

import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.fieldformat.ArrayFormat;
import org.apache.inlong.manager.pojo.fieldformat.BinaryFormat;
import org.apache.inlong.manager.pojo.fieldformat.DecimalFormat;
import org.apache.inlong.manager.pojo.fieldformat.MapFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat.Element;
import org.apache.inlong.manager.pojo.fieldformat.VarBinaryFormat;

import java.util.List;

public class FieldFormatUtils {

    public static String createBinaryFormat(int length) {
        return JsonUtils.toJsonString(new BinaryFormat(length));
    }

    public static BinaryFormat parseBinaryFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, BinaryFormat.class);
    }

    public static String createVarBinaryFormat(int length) {
        return JsonUtils.toJsonString(new VarBinaryFormat(length));
    }

    public static VarBinaryFormat parseVarBinaryFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, VarBinaryFormat.class);
    }

    public static String createDecimalFormat(int precision, int scale) {
        return JsonUtils.toJsonString(new DecimalFormat(precision, scale));
    }

    public static DecimalFormat parseDecimalFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, DecimalFormat.class);
    }

    public static String createArrayFormat(FieldType elementType, String elementFormat) {
        return JsonUtils.toJsonString(new ArrayFormat(elementType, elementFormat));
    }

    public static ArrayFormat parseArrayFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, ArrayFormat.class);
    }

    public static String createMapFormat(FieldType keyType, String keyFormat, FieldType valueType, String valueFormat) {
        return JsonUtils.toJsonString(new MapFormat(keyType, keyFormat, valueType, valueFormat));
    }

    public static MapFormat parseMapFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, MapFormat.class);
    }

    public static String createStructFormat(List<Element> elements) {
        return JsonUtils.toJsonString(new StructFormat(elements));
    }

    public static StructFormat parseStructFormat(String formatJson) {
        return JsonUtils.parseObject(formatJson, StructFormat.class);
    }
}
