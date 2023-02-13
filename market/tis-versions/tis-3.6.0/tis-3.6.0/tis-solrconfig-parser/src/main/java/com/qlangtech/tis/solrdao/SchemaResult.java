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

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.exec.IIndexMetaData;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.solrdao.impl.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-27 13:08
 */
public class SchemaResult extends SchemaMetaContent {

    private static final Logger logger = LoggerFactory.getLogger(SchemaResult.class);

    private boolean success = false;

    // 模板索引的id编号
    private int tplAppId;

    // protected final boolean xmlPost;

    public boolean isSuccess() {
        return success;
    }

    public SchemaResult faild() {
        this.success = false;
        return this;
    }

    public static SchemaResult create(ISchema parseResult, byte[] schemaContent) {
        SchemaResult schema = new SchemaResult();
        schema.parseResult = parseResult;
        schema.content = schemaContent;
        schema.success = true;
        return schema;
    }

    @Override
    protected void appendExtraProps(JSONObject schema) {
        if (this.getTplAppId() > 0) {
            schema.put("tplAppId", this.getTplAppId());
        }
    }

    /**
     * 解析提交的schemaxml 内容
     *
     * @param context
     * @return
     * @throws Exception
     */
    public static SchemaResult parseSchemaResult(IMessageHandler module, Context context, byte[] schemaContent, boolean shallValidate
            , ISchemaFieldTypeContext schemaPlugin, SolrFieldsParser.ParseResultCallback... parseResultCallback) {
        if (schemaContent == null) {
            throw new IllegalStateException("schemaContent can not be null");
        }
        if (schemaPlugin == null) {
            throw new IllegalArgumentException("param schemaPlugin can not be null");
        }
        ParseResult parseResult;
        try {
            IIndexMetaData meta = SolrFieldsParser.parse(() -> schemaContent, schemaPlugin, shallValidate);
            parseResult = meta.getSchemaParseResult();
            for (SolrFieldsParser.ParseResultCallback process : parseResultCallback) {
                process.process(Collections.emptyList(), parseResult);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            parseResult = new ParseResult(shallValidate);
            parseResult.errlist.add(e.getMessage());
        }
        if (!parseResult.isValid() || parseResult.errlist.size() > 0) {
            for (String err : parseResult.errlist) {
                module.addErrorMessage(context, err);
            }
            return create(null, schemaContent).faild();
        }
        return create(parseResult, schemaContent);
//        // new String(, getEncode());
//        result.content = schemaContent;
//        result.success = true;
//        result.parseResult = parseResult;
//        return result;
    }


    public int getTplAppId() {
        return tplAppId;
    }

    public void setTplAppId(int tplAppId) {
        this.tplAppId = tplAppId;
    }

    private SchemaResult() {
        super();
        //this.xmlPost = xmlPost;
    }
}
