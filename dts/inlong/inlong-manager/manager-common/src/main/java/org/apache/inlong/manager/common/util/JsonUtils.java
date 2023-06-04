/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@UtilityClass
public class JsonUtils {

    public static final String PROJECT_PACKAGE = "org.apache.inlong.manager";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initJsonTypeDefine(OBJECT_MAPPER);
    }

    /**
     * Transform Java object to JSON string
     */
    @SneakyThrows
    public static String toJsonString(Object object) {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * Transform Java object to pretty JSON string
     */
    @SneakyThrows
    public static String toPrettyJsonString(Object object) {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * Transform Java object to JSON byte
     */
    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        return OBJECT_MAPPER.writeValueAsBytes(object);
    }

    /**
     * Parse JSON string to java object
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (IOException e) {
            log.error("json parse err for: " + text, e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON string to java object
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtils.isEmpty(bytes)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("json parse err for: " + Arrays.toString(bytes), e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON string to java object
     */
    public static <T> T parseObject(String text, JavaType javaType) {
        try {
            return OBJECT_MAPPER.readValue(text, javaType);
        } catch (IOException e) {
            log.error("json parse err for: " + text, e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON string to Java object.
     * <p/>
     * This method enhancements to {@link #parseObject(String, Class)},
     * as the above method can not solve this situation:
     *
     * <pre>
     *     OBJECT_MAPPER.readValue(jsonStr, Response&lt;PageInfo&lt;String>>.class)
     * </pre>
     *
     * @param text json string
     * @param typeReference The generic type is actually the parsed java type
     * @return java object;
     * @throws JsonException when parse error
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (IOException e) {
            log.error("json parse err for: " + text, e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON array to List
     */
    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StringUtils.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(text,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err for: " + text, e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON string to JsonNode
     */
    public static JsonNode parseTree(String text) {
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (IOException e) {
            log.error("json parse err for: " + text, e);
            throw new JsonException(e);
        }
    }

    /**
     * Parse JSON byte to JsonNode
     */
    public static JsonNode parseTree(byte[] text) {
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (IOException e) {
            log.error("json parse err for: " + Arrays.toString(text), e);
            throw new JsonException(e);
        }
    }

    /**
     * Init all classes that marked with JsonTypeInfo annotation
     */
    public static void initJsonTypeDefine(ObjectMapper objectMapper) {
        Reflections reflections = new Reflections(PROJECT_PACKAGE);
        Set<Class<?>> typeSet = reflections.getTypesAnnotatedWith(JsonTypeInfo.class);

        // Get all subtype of class which marked JsonTypeInfo annotation
        for (Class<?> type : typeSet) {
            Set<?> clazzSet = reflections.getSubTypesOf(type);
            if (CollectionUtils.isEmpty(clazzSet)) {
                continue;
            }
            // Register all subclasses
            clazzSet.stream()
                    .map(obj -> (Class<?>) obj)
                    // Skip the interface and abstract class
                    .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                    .forEach(clazz -> {
                        // Get the JsonTypeDefine annotation
                        JsonTypeDefine extendClassDefine = clazz.getAnnotation(JsonTypeDefine.class);
                        if (extendClassDefine == null) {
                            return;
                        }
                        // Register the subtype and use the NamedType to build the relation
                        objectMapper.registerSubtypes(new NamedType(clazz, extendClassDefine.value()));
                    });
        }
    }
}
