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
import com.qlangtech.tis.solrdao.ISchemaField;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 16:30
 **/
public abstract class BasicSchemaField implements ISchemaField {

    // 分词类型
    private String tokenizerType;

    protected String name;

    private boolean indexed;

    private boolean stored;

    private boolean required;

    private boolean mltiValued;

    private boolean docValue;

    private boolean sharedKey;
    private boolean uniqueKey;

    @Override
    public boolean isSharedKey() {
        return this.sharedKey;
    }

    @Override
    public boolean isUniqueKey() {
        return this.uniqueKey;
    }

    public void setSharedKey(boolean sharedKey) {
        this.sharedKey = sharedKey;
    }

    public void setUniqueKey(boolean uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public boolean isDocValue() {
        return docValue;
    }

    public void setDocValue(boolean docValue) {
        this.docValue = docValue;
    }

    private String defaultValue;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }


    @Override
    public boolean equals(Object obj) {
        return StringUtils.equals(name, ((BasicSchemaField) obj).name);
    }


    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public boolean isMltiValued() {
        return mltiValued;
    }

    @Override
    public boolean isMultiValue() {
        return isMltiValued();
    }

    public void setMltiValued(boolean mltiValued) {
        this.mltiValued = mltiValued;
    }

    @Override
    public boolean isStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }


    /**
     * 分词类型
     *
     * @param tokenizerType
     */
    public void setTokenizerType(String tokenizerType) {
        this.tokenizerType = tokenizerType;
    }

    @Override
    public String getTokenizerType() {
        return this.tokenizerType;
    }


    protected static void setStringType(JSONObject f, String tokenizerType, String tokenizerTypeName) {
        f.put("split", true);
        f.put(ISchemaField.KEY_FIELD_TYPE, StringUtils.lowerCase(tokenizerTypeName));
        f.put("tokenizerType", tokenizerType);
    }

}
