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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.plugin.writer.elasticsearchwriter.ESFieldType;
import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.runtime.module.misc.ISearchEngineTokenizerType;
import com.qlangtech.tis.runtime.module.misc.VisualType;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-12 16:43
 **/
public enum EsTokenizerType implements ISearchEngineTokenizerType {

    //NULL(StringUtils.lowerCase(ESFieldType.STRING.name()), "无分词"),
    NULL("text", "无分词"),
    STANDARD("standard", "Standard"),
    SIMPLE("simple", "Simple"),
    WHITESPACE("whitespace", "Whitespace"),
    STOP("stop", "Stop"),
    KEYWORD("keyword", "Keyword"),
    PATTERN("pattern", "Pattern"),
    FINGERPRINT("fingerprint", "Fingerprint");

    public static final Map<String, VisualType> visualTypeMap;

    static {
        ImmutableMap.Builder<String, VisualType> visualTypeMapBuilder = new ImmutableMap.Builder<>();
        String typeName = null;
        VisualType type = null;
        for (ESFieldType t : com.alibaba.datax.plugin.writer.elasticsearchwriter.ESFieldType.values()) {
//            if (t == ESFieldType.STRING) {
//                // 作为tokener的一个类型
//                continue;
//            }
            typeName = StringUtils.lowerCase(t.name());
            if (t == getTokenizerType()) {
                type = new VisualType(typeName, true) {
                    @Override
                    public ISearchEngineTokenizerType[] getTokenerTypes() {
                        return EsTokenizerType.values();
                    }
                };
            } else {
                type = new VisualType(typeName, false);
            }
            visualTypeMapBuilder.put(typeName, type);
        }
        visualTypeMap = visualTypeMapBuilder.build();
    }

    public static ESFieldType getTokenizerType() {
        return ESFieldType.TEXT;
    }

    private final String key;
    private final String desc;

    /**
     * @param key
     * @param desc
     */
    private EsTokenizerType(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public static EsTokenizerType parse(String type) {
        for (EsTokenizerType t : EsTokenizerType.values()) {
            if (StringUtils.equals(type, t.key)) {
                return t;
            }
        }
        return null;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }
}
