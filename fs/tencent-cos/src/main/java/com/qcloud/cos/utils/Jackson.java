/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.qcloud.cos.exception.CosClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public enum Jackson {
    ;
    private static final Logger LOG = LoggerFactory.getLogger(Jackson.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final ObjectWriter writer = objectMapper.writer();
    private static final ObjectWriter prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();

    public static String toJsonPrettyString(Object value) {
        try {
            return prettyWriter.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String toJsonString(Object value) {
        try {
            return writer.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the deserialized object from the given json string and target
     * class; or null if the given json string is null.
     */
    public static <T> T fromJsonString(String json, Class<T> clazz) {
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new CosClientException("Unable to parse Json String.", e);
        }
    }

    public static <T> T fromSensitiveJsonString(String json, Class<T> clazz) {
        if (null == json) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            if (e instanceof JsonProcessingException) {
                LOG.warn("Failed to parse the json string.", e);
                throw new CosClientException("Unable to parse the json string. See warn logs for the exact error " +
                        "details, which may include sensitive information.");
            }

            throw new CosClientException("Unable to parse the json string.", e);
        }
    }

    public static JsonNode jsonNodeOf(String json) {
        return fromJsonString(json, JsonNode.class);
    }

    public static JsonGenerator jsonGeneratorOf(Writer writer) throws IOException {
        return new JsonFactory().createGenerator(writer);
    }

    public static <T> T loadFrom(File file, Class<T> clazz) throws IOException {
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static ObjectWriter getWriter() {
        return writer;
    }

    public static ObjectWriter getPrettywriter() {
        return prettyWriter;
    }
}

