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
package com.qlangtech.tis.plugin.ds;

import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.runtime.module.misc.TokenizerType;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-27 13:28
 */
public enum ReflectSchemaFieldType {
    STRING("string"), INT("int"), FLOAT("float"), LONG("long") //
    , DOUBLE("double"), IK("ik"), TEXT_WS("text_ws"), LIKE("like"), PINYIN("pinyin")//
    , DATE("date"), TIMESTAMP("timestamp");

    public static List<Option> all() {
        List<Option> all = Lists.newArrayList();
        TokenizerType tokenizerType = null;
        for (ReflectSchemaFieldType ft : ReflectSchemaFieldType.values()) {
            tokenizerType = TokenizerType.parse(ft.literia);
            all.add(new Option(tokenizerType != null ? tokenizerType.getDesc() : ft.literia, ft.literia));
        }
        return all;
    }

    public final String literia;

    private ReflectSchemaFieldType(String literia) {
        this.literia = literia;
    }
}
