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

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.Bucket;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.aliyun.IHttpToken;
import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.AuthToken;
import com.qlangtech.tis.plugin.HttpEndpoint;
import com.qlangtech.tis.plugin.aliyun.AccessKey;
import com.qlangtech.tis.plugin.aliyun.NoneToken;
import com.qlangtech.tis.plugin.aliyun.UsernamePassword;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.PluginFieldValidators;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 **/
@Public
public class DataXOssReader extends DataxReader {

    private static final Logger logger = LoggerFactory.getLogger(DataXOssReader.class);

    private static final String DATAX_NAME = "OSS";
    public static final Pattern PATTERN_OSS_OBJECT_NAME = Pattern.compile("([\\w\\d]+/)*([\\w\\d]+|(\\*)){1}");
    public static final Pattern pattern_oss_bucket = Pattern.compile("[a-zA-Z]{1}[\\da-zA-Z_\\-]+");

    public static final String FIELD_ENDPOINT = "endpoint";
    public static final String FIELD_BUCKET = "bucket";


    @FormField(ordinal = 0, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String endpoint;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String bucket;
    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String object;
    @FormField(ordinal = 5, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String column;
    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String fieldDelimiter;
    @FormField(ordinal = 7, type = FormFieldType.ENUM, validate = {})
    public String compress;
    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {})
    public String encoding;
    @FormField(ordinal = 9, type = FormFieldType.INPUTTEXT, validate = {})
    public String nullFormat;
    @FormField(ordinal = 10, type = FormFieldType.ENUM, validate = {})
    public Boolean skipHeader;
    @FormField(ordinal = 11, type = FormFieldType.TEXTAREA, validate = {})
    public String csvReaderConfig;

    @FormField(ordinal = 12, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    public String getTaskName() {
        return StringUtils.replace(StringUtils.remove(this.object, "*"), "/", "-");
    }

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXOssReader.class, "DataXOssReader-tpl.json");
    }


    @Override
    public IGroupChildTaskIterator getSubTasks() {
        return IGroupChildTaskIterator.create(new OSSReaderContext(this));
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public boolean hasMulitTable() {
        return false;
    }

    @Override
    public List<ParseColsResult.DataXReaderTabMeta> getSelectedTabs() {
        DefaultContext context = new DefaultContext();
        ParseColsResult parseOSSColsResult = ParseColsResult.parseColsCfg(this.getTaskName(), new MockFieldErrorHandler(), context, StringUtils.EMPTY, this.column);
        if (!parseOSSColsResult.success) {
            throw new IllegalStateException("parseOSSColsResult must be success");
        }
        return Collections.singletonList(parseOSSColsResult.tabMeta);

    }


    @Override
    public List<String> getTablesInDB() {
        throw new UnsupportedOperationException();
    }

    public IHttpToken getOSSConfig() {
        return IHttpToken.getToken(this.endpoint);
    }


    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxReaderDescriptor {
        public DefaultDescriptor() {
            super();
            registerSelectOptions(FIELD_ENDPOINT, () -> ParamsConfig.getItems(IHttpToken.KEY_DISPLAY_NAME));
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.AliyunOSS;
        }

        @Override
        public boolean isRdbms() {
            return false;
        }


        public boolean validateEndpoint(IFieldErrorHandler msgHandler, Context context, String fieldName, String endpoint) {
            HttpEndpoint end = (HttpEndpoint) IHttpToken.getToken(endpoint);
            return end.accept(new AuthToken.Visitor<Boolean>() {
                @Override
                public Boolean visit(NoneToken noneToken) {
                    Validator.require.validate(msgHandler, context, fieldName, null);
                    return false;
                }
                @Override
                public Boolean visit(AccessKey accessKey) {
                    return true;
                }

                @Override
                public Boolean visit(UsernamePassword accessKey) {
                    msgHandler.addFieldError(context, fieldName, "不支持使用用户名/密码认证方式");
                    return false;
                }
            });
        }


        public boolean validateFieldDelimiter(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return validateFileDelimiter(msgHandler, context, fieldName, value);
        }

        public boolean validateCsvReaderConfig(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return PluginFieldValidators.validateCsvReaderConfig(msgHandler, context, fieldName, value);
        }

        public boolean validateColumn(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            return ParseColsResult.parseColsCfg(StringUtils.EMPTY, msgHandler, context, fieldName, value).success;
        }

        public boolean validateBucket(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return validateOSSBucket(msgHandler, context, fieldName, value);
        }

        public boolean validateObject(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            return validateOSSObject(msgHandler, context, fieldName, value);
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            //  return super.validate(msgHandler, context, postFormVals);
            return verifyFormOSSRelative(msgHandler, context, postFormVals);
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }


    public static boolean verifyFormOSSRelative(IControlMsgHandler msgHandler, Context context, Descriptor.PostFormVals postFormVals) {
        String endpoint = postFormVals.getField(FIELD_ENDPOINT);
        String bucket = postFormVals.getField(FIELD_BUCKET);
        HttpEndpoint end = (HttpEndpoint) IHttpToken.getToken(endpoint);
        try {
            OSS ossClient = end.accept(new AuthToken.Visitor<OSS>() {
                @Override
                public OSS visit(AccessKey accessKey) {
                    return new OSSClientBuilder().build(end.getEndpoint(), accessKey.getAccessKeyId(), accessKey.getAccessKeySecret());
                }
            });

            List<Bucket> buckets = ossClient.listBuckets();
            if (buckets.size() < 1) {
                msgHandler.addErrorMessage(context, "buckets不能为空");
                return false;
            }
            Optional<Bucket> bucketFind = buckets.stream().filter((b) -> StringUtils.equals(bucket, b.getName())).findFirst();
            if (!bucketFind.isPresent()) {
                //  msgHandler.addErrorMessage(context, );
                msgHandler.addFieldError(context, FIELD_BUCKET, "还未创建bucket:" + bucket);
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static boolean validateFileDelimiter(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
        value = StringEscapeUtils.unescapeJava(value);
        if (value.length() > 1) {
            logger.error(fieldName + " value:{}", value);
            msgHandler.addFieldError(context, fieldName, "分割符必须为char类型");
            return false;
        }
        return true;
    }

    public static boolean validateOSSBucket(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
        Matcher matcher = pattern_oss_bucket.matcher(value);
        if (!matcher.matches()) {
            msgHandler.addFieldError(context, fieldName, "必须符合格式：" + pattern_oss_bucket.toString());
            return false;
        }
        return true;
    }

    public static boolean validateOSSObject(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
        Matcher m = PATTERN_OSS_OBJECT_NAME.matcher(value);
        if (!m.matches()) {
            msgHandler.addFieldError(context, fieldName, "必须符合格式：" + PATTERN_OSS_OBJECT_NAME.toString());
            return false;
        }
        return true;
    }
}
