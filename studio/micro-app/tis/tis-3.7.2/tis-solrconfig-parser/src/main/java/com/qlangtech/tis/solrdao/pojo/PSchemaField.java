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
package com.qlangtech.tis.solrdao.pojo;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType;
import com.qlangtech.tis.runtime.module.misc.TokenizerType;
import com.qlangtech.tis.runtime.module.misc.VisualType;
import com.qlangtech.tis.solrdao.ISchemaField;
import com.qlangtech.tis.solrdao.SolrFieldsParser;
import com.qlangtech.tis.solrdao.SolrFieldsParser.SolrType;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class PSchemaField extends BasicSchemaField {


    private SolrType type;

    // 是否为动态字段
    private boolean dynamic;
    private String defaultValue;


    private boolean useDocValuesAsStored;

    public boolean isUseDocValuesAsStored() {
        return useDocValuesAsStored;
    }

    public void setUseDocValuesAsStored(boolean useDocValuesAsStored) {
        this.useDocValuesAsStored = useDocValuesAsStored;
    }


    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }


    public String getSetMethodName() {
        return "set" + StringUtils.capitalize(this.getPropertyName());
    }

    public String getGetterMethodName() {
        return "get" + StringUtils.capitalize(this.getPropertyName());
    }

    public String getFileTypeLiteria() {
        if (this.isMltiValued()) {
            return "List<" + this.getSimpleType() + ">";
        } else {
            return this.getSimpleType();
        }
    }

    public String getPropertyName() {
        StringBuilder result = new StringBuilder();
        boolean isLetterGap = false;
        char[] nameChar = this.name.toCharArray();
        for (int i = 0; i < nameChar.length; i++) {
            if (isLetterGap) {
                result.append(Character.toUpperCase(nameChar[i]));
                isLetterGap = ('_' == nameChar[i]);
                continue;
            }
            if (isLetterGap = ('_' == nameChar[i])) {
                continue;
            }
            // if (isLetterGap) {
            // result.append(Character.toUpperCase(this.name.charAt(i)));
            // } else {
            result.append(nameChar[i]);
            // }
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }


    public SolrType getType() {
        return type;
    }

    public String getSimpleType() {
        return this.getType().getJavaType().getSimpleName();
    }

    public void setType(SolrType type) {
        this.type = type;
    }


    @Override
    public boolean isStored() {
        return useDocValuesAsStored || super.isStored();
    }


    @Override
    public String getTisFieldTypeName() {
        return this.type.getSType().getName();
    }

    @Override
    public void serialVisualType2Json(JSONObject f) {

        if (this.getType() == null) {
            throw new IllegalStateException("field:" + this.getName() +
                    " 's fieldType is can not be null");
        }
//            serialVisualType2Json(f, this.getType());
        SolrFieldsParser.SolrType solrType = this.getType();
        String type = solrType.getSType().getName();
        TokenizerType tokenizerType = TokenizerType.parse(type);
        if (tokenizerType == null) {
            // 非分词字段
            if (solrType.tokenizerable) {
                setStringType(f, type, ReflectSchemaFieldType.STRING.literia);
            } else {
                f.put("split", false);
                VisualType vtype = TokenizerType.visualTypeMap.get(type);
                if (vtype != null) {
                    f.put(ISchemaField.KEY_FIELD_TYPE, vtype.getType());
                    return;
                }
                f.put(ISchemaField.KEY_FIELD_TYPE, type);
            }
        } else {
            // 分词字段
            setStringType(f, tokenizerType.getKey(), ReflectSchemaFieldType.STRING.literia);
        }
//        } else {
//            throw new IllegalStateException("field:" + this.getName() + " 's fieldType is can not be null");
//        }
    }


    @Override
    public String toString() {
        return "{ name='" + name + '\'' +
                ", indexed=" + isIndexed() +
                ", stored=" + isStored() +
                ", docValue=" + isDocValue() +
                '}';
    }
}
