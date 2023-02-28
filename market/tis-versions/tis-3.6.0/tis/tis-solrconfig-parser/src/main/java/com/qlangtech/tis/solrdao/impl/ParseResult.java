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

package com.qlangtech.tis.solrdao.impl;

import com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType;
import com.qlangtech.tis.runtime.module.misc.ISearchEngineTokenizerType;
import com.qlangtech.tis.runtime.module.misc.TokenizerType;
import com.qlangtech.tis.runtime.module.misc.VisualType;
import com.qlangtech.tis.solrdao.ISchema;
import com.qlangtech.tis.solrdao.SolrFieldsParser;
import com.qlangtech.tis.solrdao.extend.IndexBuildHook;
import com.qlangtech.tis.solrdao.extend.ProcessorSchemaField;
import com.qlangtech.tis.solrdao.pojo.PSchemaField;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import java.util.*;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 16:19
 **/
public class ParseResult implements ISchema {
    public static final Set<String> reserved_words = new HashSet<String>();
    public static final StringBuffer reservedWordsBuffer = new StringBuffer();

    static {
        reserved_words.add("_val_");
        reserved_words.add("fq");
        reserved_words.add("docId");
        reserved_words.add("score");
        reserved_words.add("q");
        reserved_words.add("boost");
        for (String s : reserved_words) {
            reservedWordsBuffer.append("'").append(s).append("' ");
        }
    }

    public static final String DEFAULT = "default";


    public SolrFieldsParser.SchemaFields dFields = new SolrFieldsParser.SchemaFields();

    @Override
    public void clearFields() {
        this.dFields.clear();
    }

    public Set<String> dFieldsNames;

    public final Map<String, SolrFieldsParser.SolrType> types = new HashMap<>();

    private String uniqueKey;
    private String sharedKey;

    private final boolean shallValidate;

    private final List<ProcessorSchemaField> processorSchemasFields = new LinkedList<>();

    private final List<IndexBuildHook> indexBuildHooks = new LinkedList<IndexBuildHook>();

    private String indexBuilder;

    public String getIndexBuilder() {
        return indexBuilder;
    }

    public void setIndexBuilder(String indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    // 索引构建的实现类
    private String indexMakerClassName = DEFAULT;

    // 对应接口的实现类
    private String documentCreatorType = DEFAULT;

    public List<ProcessorSchemaField> getProcessorSchemas() {
        return processorSchemasFields;
    }

    public Set<String> getFieldNameSet() {
        return this.dFieldsNames;
    }

    public void addProcessorSchema(ProcessorSchemaField processorSchema) {
        processorSchemasFields.add(processorSchema);
    }

    public void addIndexBuildHook(IndexBuildHook indexBuildHook) {
        this.indexBuildHooks.add(indexBuildHook);
    }

    public List<IndexBuildHook> getIndexBuildHooks() {
        return Collections.unmodifiableList(indexBuildHooks);
    }

    public ParseResult(boolean shallValidate) {
        this.shallValidate = shallValidate;
    }

    public List<String> errlist = new ArrayList<String>();

    public Collection<SolrFieldsParser.SolrType> getFieldTypes() {
        return types.values();
    }

    public Collection<String> getFieldTypesKey() {
        return types.keySet();
    }

    public boolean containType(String typeName) {
        return this.types.containsKey(typeName);
    }

    public void addFieldType(String typeName, SolrFieldsParser.SolrType type) {
        if (StringUtils.isBlank(typeName)) {
            throw new IllegalArgumentException("param typeName can not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("param type can not be null");
        }
        types.put(typeName, type);
    }

    public SolrFieldsParser.SolrType getTisType(String fieldName) {
        SolrFieldsParser.SolrType t = types.get(fieldName);
        if (t == null) {
            throw new IllegalStateException("fieldName:" + fieldName + " relevant FieldType can not be null");
        }
        return t;
    }

    public String getErrorSummary() {
        StringBuffer summary = new StringBuffer();
        for (String err : errlist) {
            summary.append(err);
            summary.append("\n");
        }
        return summary.toString();
    }

    public boolean isValid() {
//        if (!this.errlist.isEmpty()) {
//            return false;
//        }
//        if (!shallValidate) {
//            return true;
//        }
//        for (com.qlangtech.tis.solrdao.pojo.PSchemaField field : dFields) {
//            if (!field.isIndexed() && !field.isStored() && !field.isDocValue()) {
//                errlist.add(ISchema.getFieldPropRequiredErr(field.getName()));
//            }
//            String fieldName = StringUtils.lowerCase(field.getName());
//            if (reserved_words.contains(fieldName)) {
//                errlist.add("字段名称:" + field.getName() + "不能命名成系统保留字符" + reservedWordsBuffer);
//            }
//        }
        return CollectionUtils.isEmpty(errlist = ISchema.validateSchema(this.dFields));
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    @Override
    public String getSharedKey() {
        return this.sharedKey;
    }

    public void setSharedKey(String sharedkey) {
        this.sharedKey = sharedkey;
    }

    @Override
    public List<PSchemaField> getSchemaFields() {
        return this.dFields;
//        List<ISchemaField> result = new ArrayList<>();
//        for (PSchemaField f : this.dFields) {
//            result.add(f);
//        }
//        return result;
    }

    public String getIndexMakerClassName() {
        return this.indexMakerClassName;
    }

    public void setIndexMakerClassName(String indexMakerClassName) {
        this.indexMakerClassName = indexMakerClassName;
    }

    public String getDocumentCreatorType() {
        return this.documentCreatorType;
    }

    public void setDocumentCreatorType(String documentCreatorType) {
        this.documentCreatorType = StringUtils.defaultIfBlank(documentCreatorType, "default");
    }

    /**
     * 添加保留字段
     */
    public void addReservedFields() {
        SolrFieldsParser.SolrType strType = this.getTisType(ReflectSchemaFieldType.STRING.literia);
        SolrFieldsParser.SolrType longType = this.getTisType("long");
        PSchemaField verField = new PSchemaField();
        verField.setName("_version_");
        verField.setDocValue(true);
        verField.setStored(true);
        verField.setType(longType);
        this.dFields.add(verField);

        PSchemaField textField = new PSchemaField();
        textField.setName("text");
        textField.setDocValue(false);
        textField.setMltiValued(true);
        textField.setStored(false);
        textField.setIndexed(true);
        textField.setType(strType);
        this.dFields.add(textField);
    }

    /**
     * @param
     * @throws JSONException
     */
    @Override
    public com.alibaba.fastjson.JSONArray serialTypes() {

        com.alibaba.fastjson.JSONArray types = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject f = null;
        com.alibaba.fastjson.JSONArray tokens = null;
        com.alibaba.fastjson.JSONObject tt = null;
        SolrFieldsParser.SolrType solrType = null;
        // Set<String> typesSet = new HashSet<String>();
        for (Map.Entry<String, VisualType> t : TokenizerType.visualTypeMap.entrySet()) {
            f = new com.alibaba.fastjson.JSONObject();
            f.put("name", t.getKey());
            f.put("split", t.getValue().isSplit());
            tokens = new com.alibaba.fastjson.JSONArray();
            if (t.getValue().isSplit()) {
                // 默认类型
                for (ISearchEngineTokenizerType tokenType : t.getValue().getTokenerTypes()) {
                    tt = new com.alibaba.fastjson.JSONObject();
                    tt.put("key", tokenType.getKey());
                    tt.put("value", tokenType.getDesc());
                    tokens.add(tt);
                }
                // 外加类型
                for (Map.Entry<String, SolrFieldsParser.SolrType> entry : this.types.entrySet()) {
                    solrType = entry.getValue();
                    if (solrType.tokenizerable) {
                        tt = new com.alibaba.fastjson.JSONObject();
                        tt.put("key", entry.getKey());
                        tt.put("value", entry.getKey());
                        tokens.add(tt);
                    }
                }
                f.put("tokensType", tokens);
            }
            types.add(f);
        }

        for (Map.Entry<String, SolrFieldsParser.SolrType> entry : this.types.entrySet()) {
            solrType = entry.getValue();
            if (!solrType.tokenizerable && !TokenizerType.isContain(entry.getKey())) {
                f = new com.alibaba.fastjson.JSONObject();
                f.put("name", entry.getKey());
                types.add(f);
            }
        }
        // schema.put("fieldtypes", types);
        return types;
    }

    @Override
    public List<String> getErrors() {
        return null;
    }
}
