/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.obs.services.internal.ServiceException;

public class JSONChange {
    /*
     * json转换成对象
     * 
     * @param:传入对象，json字符串
     * 
     * @return:Object
     */
    public static Object jsonToObj(Object obj, String jsonStr) throws ServiceException {
        MyObjectMapper mapper = ObjectMapperUtil.getInstance();
        try {
            obj = mapper.readValue(jsonStr, obj.getClass());
            return obj;
        } catch (JsonParseException e) {
            throw new ServiceException(" conversion JSON failed ", e);
        } catch (JsonMappingException e) {
            throw new ServiceException(" conversion JSON failed", e);
        } catch (IOException e) {
            throw new ServiceException(" conversion JSON failed", e);
        }
    }

    public static JsonNode readNodeFromJson(String jsonStr) throws ServiceException {
        MyObjectMapper mapper = ObjectMapperUtil.getInstance();
        try {
            JsonNode node = mapper.readTree(jsonStr);
            return node;
        } catch (IOException e) {
            throw new ServiceException(" read node failed", e);
        }
    }

    /*
     * 对象转换成json
     * 
     * @param:传入对象
     * 
     * @return:json字符串
     */
    public static String objToJson(Object obj) throws ServiceException {
        MyObjectMapper mapper = ObjectMapperUtil.getInstance();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ServiceException("conversion JSON failed", e);
        }
    }
    
    private static class ObjectMapperUtil {

        private static class ObjectMapperUtilInstance {
            private static final MyObjectMapper MAPPER = new MyObjectMapper();
        }

        public static MyObjectMapper getInstance() {
            return ObjectMapperUtilInstance.MAPPER;
        }
    }
    
    private static class MyObjectMapper extends ObjectMapper {
        private static final long serialVersionUID = 4563671462132723274L;

        // 默认构造函数
        public MyObjectMapper() {
            super();
            // 从JSON到java object
            // 反序列化
            // 禁止遇到空原始类型时抛出异常，用默认值代替。
            this.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            this.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
            // 禁止遇到未知（新）属性时报错，支持兼容扩展
            this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            this.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
            // 按时间戳格式读取日期
            this.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
            this.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
            this.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

            // 序列化
            // 禁止序列化空值
            this.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.ALWAYS, Include.NON_NULL));
            this.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            this.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
            this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            // 按时间戳格式生成日期
            this.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
            this.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
            // 不包含空值属性
            this.setSerializationInclusion(Include.NON_EMPTY);
            this.setSerializationInclusion(Include.NON_NULL);
            // this.configure(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME,
            // true);
            // 是否缩放排列输出，默认false，
            this.configure(SerializationFeature.INDENT_OUTPUT, false);
        }
    }
}
