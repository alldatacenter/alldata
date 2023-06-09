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

package com.qlangtech.tis.plugin.solr.schema;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Sets;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支持json的FieldType
 *
 * @author: baisui 百岁
 * @create: 2021-01-26 12:36
 **/
public class JSONFieldTypeFactory extends FieldTypeFactory {
    private static final Logger logger = LoggerFactory.getLogger(JSONFieldTypeFactory.class);

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, validate = {})
    public String includeKeys;

    @FormField(ordinal = 2, validate = {Validator.require, Validator.identity})
    public String extraPrefix;

    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public String extraType;

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public boolean extraStored;

    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public boolean extraIndexed;

    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public boolean extraDocVal;


    @Override
    public void process(org.jdom2.Document document2, com.yushu.tis.xmodifier.XModifier modifier) {
        // super.process(document2, modifier);
        //  modifySchemaProperty(String.format("/fields/dynamicField[@name='%s']/@%s", field.getName(), key), value, modifier);

        modifier.addModify(String.format("/fields/dynamicField[@name='%s']/@type", extraPrefix + "*"), extraType);
        modifier.addModify(String.format("/fields/dynamicField[@name='%s']/@stored", extraPrefix + "*"), String.valueOf(extraStored));
        modifier.addModify(String.format("/fields/dynamicField[@name='%s']/@indexed", extraPrefix + "*"), String.valueOf(extraIndexed));
        modifier.addModify(String.format("/fields/dynamicField[@name='%s']/@docValues", extraPrefix + "*"), String.valueOf(extraDocVal));
    }

    @Override
    public boolean forStringTokenizer() {
        return true;
    }

    @Override
    public ISolrFieldType createInstance() {
        JSONField jsonField = new JSONField();
        jsonField.propPrefix = this.extraPrefix;
        if (StringUtils.isNotEmpty(includeKeys)) {
            jsonField.includeKeys = Sets.newHashSet(StringUtils.split(includeKeys, ","));
        }
        logger.info("create json field,name:" + this.name + ",propPrefix:" + extraPrefix + ",includeKeys:" + includeKeys);
        return jsonField;
    }


//    public static void main(String[] args) {
//        Matcher matcher = pattern_includeKeys.matcher("aaa,b_Bbm,ccc");
//        System.out.println(matcher.matches());
//    }

    private static class JSONField extends StrField implements ISolrFieldType {
        private String propPrefix;
        private Set<String> includeKeys;

        @Override
        protected void setArgs(IndexSchema schema, Map<String, String> args) {
            super.setArgs(schema, args);
        }

        @Override
        public List<IndexableField> createFields(SchemaField sf, Object value) {
            List<IndexableField> result = new ArrayList<>();
            String textValue = String.valueOf(value);
            if (value == null || !StringUtils.startsWith(textValue, "{")) {
                return Collections.emptyList();
            }
            JSONTokener tokener = new JSONTokener(textValue);
            JSONObject json = null;
            if (includeKeys != null && !this.includeKeys.isEmpty()) {
                json = new JSONObject(tokener, this.includeKeys.toArray(new String[]{}));
            } else {
                json = new JSONObject(tokener);
            }
            if ((sf.getProperties() & STORED) > 0) {
                result.add(this.createField(new SchemaField(sf.getName(), sf.getType()
                        , OMIT_NORMS | OMIT_TF_POSITIONS | STORED, ""), json.toString()));
            }
            if (StringUtils.isNotEmpty(this.propPrefix)) {
                SchemaField field = null;
                String fieldValue = null;
                for (String key : json.keySet()) {
                    field = new SchemaField(propPrefix + key, sf.getType(), OMIT_NORMS | OMIT_TF_POSITIONS | STORED | INDEXED, "");
                    fieldValue = json.getString(key);
                    if ("null".equalsIgnoreCase(fieldValue) || (includeKeys != null && !this.includeKeys.contains(key))) {
                        continue;
                    }
                    result.add(this.createField(field, fieldValue));
                }
            }
            return result;
        }

        @Override
        public boolean isPolyField() {
            return true;
        }
    }


    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<FieldTypeFactory> {
        static final Pattern pattern_includeKeys = Pattern.compile("(,?[a-zA-Z]{1}[\\da-zA-Z_\\-]+)+");

        @Override
        public String getDisplayName() {
            return "json";
        }

        public boolean validateIncludeKeys(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            Matcher matcher = pattern_includeKeys.matcher(value);
            if (!matcher.matches()) {
                msgHandler.addFieldError(context, fieldName, "不符合规范" + pattern_includeKeys);
                return false;
            }
            return true;
        }
    }
}
