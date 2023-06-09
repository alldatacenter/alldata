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

import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-15 09:53
 **/
public class SchemaMetaContent {
    public byte[] content;


    public ISchema parseResult;

    public ISchema getParseResult() {
        return this.parseResult;
    }

    protected void appendExtraProps(com.alibaba.fastjson.JSONObject schema) {
    }


    /**
     * 取得普通模式多字段
     *
     * @throws Exception
     */
    public com.alibaba.fastjson.JSONObject toJSON() {
        SchemaMetaContent result = this;
        // ISchema parseResult = result.parseResult;
        ISchema parseResult = result.parseResult;
        final com.alibaba.fastjson.JSONObject schema = new com.alibaba.fastjson.JSONObject();
        this.appendExtraProps(schema);

        // 设置原生schema的内容
        if (result.content != null) {
            schema.put("schemaXmlContent", new String(result.content, TisUTF8.get()));
        }
        String sharedKey = StringUtils.trimToEmpty(parseResult.getSharedKey());
        String pk = StringUtils.trimToEmpty(parseResult.getUniqueKey());
        schema.put("shareKey", sharedKey);
        schema.put("uniqueKey", pk);
        com.alibaba.fastjson.JSONArray fields = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject f = null;
        int id = 0;
        // String type = null;

        for (ISchemaField field : parseResult.getSchemaFields()) {

            // for (PSchemaField field : parseResult.dFields) {
            f = new com.alibaba.fastjson.JSONObject();
            // 用于标示field 頁面操作過程中不能變
            // 0 开始
            f.put("id", id++);
            // 用于表示UI上的行号
            // 1 开始
            f.put("index", id);
            // f.put("uniqueKey", id++);
            f.put("sharedKey", field.isSharedKey());
            f.put("uniqueKey", field.isUniqueKey());
            f.put("name", field.getName());
            // f.put("inputDisabled", field.inputDisabled);
            // f.put("rangequery", false);
            f.put("defaultVal", StringUtils.trimToNull(field.getDefaultValue()));
            //f.put("fieldtype", field.getTisFieldType());

            field.serialVisualType2Json(f);

//            if (field.getType() != null) {
//                serialVisualType2Json(f, field.getType());
//            } else {
//                throw new IllegalStateException("field:" + field.getName() + " 's fieldType is can not be null");
//            }
            f.put("docval", field.isDocValue());
            f.put("indexed", field.isIndexed());
            f.put("multiValue", field.isMultiValue());
            f.put("required", field.isRequired());
            f.put("stored", field.isStored());
            fields.add(f);
        }
        schema.put("fields", fields);


        schema.put("fieldtypes", parseResult.serialTypes());
        // this.setBizResult(context, schema);
        return schema;
    }
}
