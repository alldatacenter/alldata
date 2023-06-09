/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.qlangtech.tis.runtime.module.misc.ISearchEngineTokenizerType;
import com.qlangtech.tis.runtime.module.misc.VisualType;
import com.qlangtech.tis.solrdao.ISchema;
import com.qlangtech.tis.solrdao.SolrFieldsParser;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 16:22
 **/
public class ESSchema implements ISchema {


    private String uniqueKey;
    private String sharedKey;
    public List<ESField> fields = Lists.newArrayList();


    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public List<ESField> getFields() {
        return this.fields;
    }

    public void setFields(List<ESField> fields) {
        this.fields = fields;
    }

    @Override
    public List<ESField> getSchemaFields() {
        // List<ISchemaField> fields = Lists.newArrayList();
        return fields;
    }

    @Override
    public void clearFields() {
        this.fields.clear();
    }

    @Override
    public String getUniqueKey() {
        return this.uniqueKey;
    }

    @Override
    public String getSharedKey() {
        return this.sharedKey;
    }

    @Override
    public JSONArray serialTypes() {

        JSONArray types = new JSONArray();
        com.alibaba.fastjson.JSONObject f = null;
        JSONArray tokens = null;
        com.alibaba.fastjson.JSONObject tt = null;
        SolrFieldsParser.SolrType solrType = null;
        // Set<String> typesSet = new HashSet<String>();
        for (Map.Entry<String, VisualType> t : EsTokenizerType.visualTypeMap.entrySet()) {
            f = new com.alibaba.fastjson.JSONObject();
            f.put("name", t.getKey());
            f.put("split", t.getValue().isSplit());
            tokens = new JSONArray();
            if (t.getValue().isSplit()) {
                // 默认类型
                for (ISearchEngineTokenizerType tokenType : t.getValue().getTokenerTypes()) {
                    tt = new com.alibaba.fastjson.JSONObject();
                    tt.put("key", tokenType.getKey());
                    tt.put("value", tokenType.getDesc());
                    tokens.add(tt);
                }
                // 外加类型
//                for (Map.Entry<String, SolrFieldsParser.SolrType> entry : this.types.entrySet()) {
//                    solrType = entry.getValue();
//                    if (solrType.tokenizerable) {
//                        tt = new com.alibaba.fastjson.JSONObject();
//                        tt.put("key", entry.getKey());
//                        tt.put("value", entry.getKey());
//                        tokens.add(tt);
//                    }
//                }
                f.put("tokensType", tokens);
            }
            types.add(f);
        }

//        for (Map.Entry<String, SolrFieldsParser.SolrType> entry : this.types.entrySet()) {
//            solrType = entry.getValue();
//            if (!solrType.tokenizerable && !TokenizerType.isContain(entry.getKey())) {
//                f = new com.alibaba.fastjson.JSONObject();
//                f.put("name", entry.getKey());
//                types.add(f);
//            }
//        }
        // schema.put("fieldtypes", types);
        return types;
    }

    public List<String> errlist = new ArrayList<String>();

//    public String getErrorSummary() {
//        StringBuffer summary = new StringBuffer();
//        for (String err : errlist) {
//            summary.append(err);
//            summary.append("\n");
//        }
//        return summary.toString();
//    }

    @Override
    public boolean isValid() {
        if (!this.errlist.isEmpty()) {
            return false;
        }
        return CollectionUtils.isEmpty(this.errlist = ISchema.validateSchema(this.fields));
    }

    @Override
    public List<String> getErrors() {
        return Collections.unmodifiableList(this.errlist);
    }
}
