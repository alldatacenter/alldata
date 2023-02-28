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

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.runtime.module.misc.VisualType;
import com.qlangtech.tis.solrdao.ISchemaField;
import com.qlangtech.tis.solrdao.pojo.BasicSchemaField;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 16:38
 **/
public class ESField extends BasicSchemaField {

    private VisualType type;

    public VisualType getType() {
        return type;
    }

    public void setType(VisualType type) {
        this.type = type;
    }

    @Override
    public String getTisFieldTypeName() {
        return type.getType();
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void serialVisualType2Json(JSONObject f) {
        if (this.getType() == null) {
            throw new IllegalStateException("field:" + this.getName() + " 's fieldType is can not be null");
        }

        VisualType esType = this.getType();
        String type = esType.type;
        EsTokenizerType tokenizerType = EsTokenizerType.parse(this.getTokenizerType());
        if (tokenizerType == null) {
            // 非分词字段
            if (esType.isSplit()) {
                setStringType(f, type, EsTokenizerType.getTokenizerType().name());
            } else {
                f.put("split", false);
                VisualType vtype = EsTokenizerType.visualTypeMap.get(type);
                if (vtype != null) {
                    f.put(ISchemaField.KEY_FIELD_TYPE, vtype.getType());
                    return;
                }
                f.put(ISchemaField.KEY_FIELD_TYPE, type);
            }
        } else {
            // 分词字段
            setStringType(f, tokenizerType.getKey(), EsTokenizerType.getTokenizerType().name());
        }
    }
}
