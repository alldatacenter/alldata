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
package com.qlangtech.tis.solrdao;

import com.alibaba.fastjson.JSONObject;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年5月8日
 */
public interface ISchemaField {

    String KEY_FIELD_TYPE = "fieldtype";

    String KEY_NAME = "name";
    String KEY_PK = "pk";
    String KEY_SHARE_KEY = "shareKey";
    String KEY_TYPE = "type";
    String KEY_ANALYZER = "analyzer";
    String KEY_INDEX = "index";
    String KEY_ARRAY = "array";
    String KEY_DOC_VALUES = "doc_values";
    String KEY_STORE = "store";


    String getName();

    public boolean isSharedKey();

    public boolean isUniqueKey();

    /**
     * 字段类型名称，不是全路径
     *
     * @return
     */
    String getTisFieldTypeName();

    String getTokenizerType();


    boolean isIndexed();

    boolean isStored();

    boolean isDocValue();

    boolean isRequired();

    // 是否是多值
    boolean isMultiValue();

    boolean isDynamic();

    /**
     * 默认值
     *
     * @return
     */
    String getDefaultValue();


    void serialVisualType2Json(JSONObject f);
}
