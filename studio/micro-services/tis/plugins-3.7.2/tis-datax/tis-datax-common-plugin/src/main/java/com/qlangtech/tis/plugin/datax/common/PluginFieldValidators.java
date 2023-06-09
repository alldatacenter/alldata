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

package com.qlangtech.tis.plugin.datax.common;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-13 13:20
 **/
public class PluginFieldValidators {
    final static Map<String, ValValidator> avalibleCsvReaderKeys;
    private static ValValidator boolValidator = new TypedValidator(Boolean.class);

    private static ValValidator charValidator = new TypedValidator(String.class) {
        @Override
        public boolean validate(String key, IFieldErrorHandler msgHandler, Context context, String fieldName, Object val) {
            boolean pass = super.validate(key, msgHandler, context, fieldName, val);
            if (pass) {
                String v = StringEscapeUtils.unescapeJava((String) val);
                if (StringUtils.length(v) > 1) {
                    return false;
                }
            }
            return pass;
        }
    };

    static {
        ImmutableMap.Builder<String, ValValidator> builder = ImmutableMap.builder();
        builder.put("caseSensitive", boolValidator);
        builder.put("textQualifier", charValidator);
        builder.put("trimWhitespace", boolValidator);
        builder.put("useTextQualifier", boolValidator);
        builder.put("delimiter", charValidator);
        builder.put("recordDelimiter", charValidator);
        builder.put("comment", charValidator);
        builder.put("useComments", boolValidator);
        builder.put("escapeMode", new TypedValidator(Integer.class));
        builder.put("safetySwitch", boolValidator);
        builder.put("skipEmptyRecords", boolValidator);
        builder.put("captureRawRecord", boolValidator);
        avalibleCsvReaderKeys = builder.build();
    }

    private interface ValValidator {
        public boolean validate(String key, IFieldErrorHandler msgHandler, Context context, String fieldName, Object val);
    }


    public static boolean validateCsvReaderConfig(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
        try {
            JSONObject cfg = JSON.parseObject(value);

            ValValidator validator = null;
            for (Map.Entry<String, Object> entry : cfg.entrySet()) {
                if ((validator = avalibleCsvReaderKeys.get(entry.getKey())) == null) {
                    msgHandler.addFieldError(context, fieldName, "key'" + entry.getKey() + "'是不可接受的");
                    return false;
                }
                if (!validator.validate(entry.getKey(), msgHandler, context, fieldName, entry.getValue())) {
                    return false;
                }
            }
        } catch (Throwable e) {
            msgHandler.addFieldError(context, fieldName, e.getMessage());
            return false;
        }
        return true;
    }


    private static class TypedValidator implements ValValidator {
        private final Class<?> clazz;

        public TypedValidator(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean validate(String key, IFieldErrorHandler msgHandler, Context context, String fieldName, Object val) {
            if (val == null) {
                msgHandler.addFieldError(context, fieldName, "'key':" + key + " 对应的值不能为空");
                return false;
            }
            if (!clazz.isAssignableFrom(val.getClass())) {
                msgHandler.addFieldError(context, fieldName, "'key':" + key + " 对应的值必须为" + clazz.getSimpleName() + "类型");
                return false;
            }
            return true;
        }
    }
}
