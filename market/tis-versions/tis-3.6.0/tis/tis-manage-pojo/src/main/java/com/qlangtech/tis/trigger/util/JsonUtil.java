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
package com.qlangtech.tis.trigger.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qlangtech.tis.extension.impl.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-11-14
 */
public class JsonUtil {

    private JsonUtil() {
    }

    /**
     * 将map中的内容序反列化成一个map结果
     *
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends HashMap<String, String>> T deserialize(String value, T object) {
        try {
            JSONTokener tokener = new JSONTokener(value);
            JSONObject json = new JSONObject(tokener);
            Iterator it = json.keys();
            String key = null;
            while (it.hasNext()) {
                key = (String) it.next();
                object.put(key, json.getString(key));
            }
            return object;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将一个map对象序列化成一个json字符串
     *
     * @param param
     * @return
     */
    public static String serialize(Map<String, String> param) {
        JSONObject json = new JSONObject(param);
        return toString(json);
    }

    static {

        com.alibaba.fastjson.serializer.ObjectSerializer serializer = new ObjectSerializer() {
            @Override
            public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
                try {
                    //  SerializeWriter out = serializer.out;

                    UnCacheString value = (UnCacheString) object;
                    Objects.requireNonNull(value, "callable of " + fieldName + " can not be null");

                    //  out.writeString(value.getValue());

                    serializer.write(value.getValue());

                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };

        SerializeConfig.globalInstance.put(UnCacheString.class, serializer);
    }

    public static final class UnCacheString<T> {
        private final Callable<T> valGetter;

        public UnCacheString(Callable<T> valGetter) {
            this.valGetter = valGetter;
        }

        public T getValue() {
            try {
                return this.valGetter.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String toString(Object json) {


        return com.alibaba.fastjson.JSON.toJSONString(
                json, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
    }


    public static void assertJSONEqual(Class<?> invokeClass, String assertFileName, String actual, IAssert azzert) {
//        String expectJson = com.alibaba.fastjson.JSON.toJSONString(
//                JSON.parseObject(IOUtils.loadResourceFromClasspath(invokeClass, assertFileName))
//                , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat, SerializerFeature.MapSortField);
//        System.out.println(assertFileName + "\n" + expectJson);
//        String actualJson = com.alibaba.fastjson.JSON.toJSONString(JSON.parseObject(actual)
//                , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat, SerializerFeature.MapSortField);
//        azzert.assertEquals("assertFile:" + assertFileName, expectJson, actualJson);


        assertJSONEqual(invokeClass, assertFileName, JSON.parseObject(actual), azzert);
    }

    public static void assertJSONEqual(Class<?> invokeClass, String assertFileName, com.alibaba.fastjson.JSONObject actual, IAssert azzert) {
        String expectJson = com.alibaba.fastjson.JSON.toJSONString(
                JSON.parseObject(IOUtils.loadResourceFromClasspath(invokeClass, assertFileName))
                , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat, SerializerFeature.MapSortField);
        System.out.println(assertFileName + "\n" + expectJson);
        String actualJson = com.alibaba.fastjson.JSON.toJSONString(actual
                , SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat, SerializerFeature.MapSortField);
        azzert.assertEquals("assertFile:" + assertFileName, expectJson, actualJson);
    }

    public interface IAssert {
        public void assertEquals(String message, String expected, String actual);
    }


}
