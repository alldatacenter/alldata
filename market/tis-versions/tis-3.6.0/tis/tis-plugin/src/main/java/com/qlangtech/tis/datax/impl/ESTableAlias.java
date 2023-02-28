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

package com.qlangtech.tis.datax.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.manage.common.TisUTF8;

import java.util.Collections;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-16 16:41
 **/
public class ESTableAlias extends IDataxProcessor.TableMap {
    public static final String KEY_COLUMN = "column";
    public static final String MAX_READER_TABLE_SELECT_COUNT = "maxReaderTableCount";

    // json 格式
    private String schemaContent;

    public ESTableAlias() {
        super(Collections.emptyList());
    }

    @Override
    public List<CMeta> getSourceCols() {
        List<CMeta> colsMeta = Lists.newArrayList();
        CMeta colMeta = null;
        JSONArray cols = getSchemaCols();
        JSONObject col = null;
        for (int i = 0; i < cols.size(); i++) {
            col = cols.getJSONObject(i);
            colMeta = new CMeta() {

                @Override
                public DataType getType() {
                    //return super.getType();
                    throw new UnsupportedOperationException();
                }
            };
            colMeta.setName(col.getString("name"));
            colMeta.setPk(col.getBoolean("pk"));
            colsMeta.add(colMeta);
        }
        return colsMeta;
    }

    public JSONObject getSchema() {
        return JSON.parseObject(schemaContent);
    }

    public JSONArray getSchemaCols() {
        JSONObject schema = this.getSchema();
        JSONArray cols = schema.getJSONArray(KEY_COLUMN);
        return cols;
    }

    public byte[] getSchemaByteContent() {
        return schemaContent.getBytes(TisUTF8.get());
    }

    public String getSchemaContent(){
        return this.schemaContent;
    }

    public void setSchemaContent(String schemaContent) {
        this.schemaContent = schemaContent;
    }
}
