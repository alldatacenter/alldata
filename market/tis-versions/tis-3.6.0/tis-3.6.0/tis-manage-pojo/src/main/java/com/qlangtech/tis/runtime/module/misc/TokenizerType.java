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
package com.qlangtech.tis.runtime.module.misc;

import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年1月6日下午6:31:17
 */
public enum TokenizerType implements ISearchEngineTokenizerType {
    NULL(ReflectSchemaFieldType.STRING.literia, "无分词") //
    , IK(ReflectSchemaFieldType.IK.literia, "IK分词") //
    , LIKE(ReflectSchemaFieldType.LIKE.literia, "LIKE分词") //
    , BLANK_SPLIT(ReflectSchemaFieldType.TEXT_WS.literia, "空格分词") //
    , PINGYIN(ReflectSchemaFieldType.PINYIN.literia, "拼音分词");

    public static final Map<String, VisualType> visualTypeMap;

    static {
        ImmutableMap.Builder<String, VisualType> visualTypeMapBuilder = new ImmutableMap.Builder<>();
        visualTypeMapBuilder.put(ReflectSchemaFieldType.STRING.literia, VisualType.STRING_TYPE);
        addNumericType(visualTypeMapBuilder, ReflectSchemaFieldType.DOUBLE.literia);
        addNumericType(visualTypeMapBuilder, ReflectSchemaFieldType.INT.literia);
        addNumericType(visualTypeMapBuilder, ReflectSchemaFieldType.FLOAT.literia);
        addNumericType(visualTypeMapBuilder, ReflectSchemaFieldType.LONG.literia);
        visualTypeMap = visualTypeMapBuilder.build();
    }

    private static void addNumericType(
            ImmutableMap.Builder<String, VisualType> visualTypeMapBuilder, String numericType) {
        VisualType type = new VisualType(numericType, false);
        visualTypeMapBuilder.put('p' + numericType, type);
    }


    public static boolean isContain(String key) {
        return parseVisualType(key) != null;
    }

    /**
     * @param key
     * @return
     */
    public static VisualType parseVisualType(String key) {
        VisualType result = visualTypeMap.get(key);
        if (result != null) {
            return result;
        }
        for (TokenizerType type : TokenizerType.values()) {
            if (StringUtils.equals(type.getKey(), key)) {
                return VisualType.STRING_TYPE;
            }
        }
        return null;
    }

    public static TokenizerType parse(String key) {
        for (TokenizerType type : TokenizerType.values()) {
            if (StringUtils.equals(type.key, key)) {
                return type;
            }
        }
        return null;
    }

    private final String key;

    private final String desc;

    /**
     * @param key
     * @param desc
     */
    private TokenizerType(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }
}
