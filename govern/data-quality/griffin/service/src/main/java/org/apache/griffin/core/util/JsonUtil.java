/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(JsonUtil.class);

    public static String toJson(Object obj) throws JsonProcessingException {
        if (obj == null) {
            LOGGER.warn("Object cannot be empty!");
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

    public static String toJsonWithFormat(Object obj)
        throws JsonProcessingException {
        if (obj == null) {
            LOGGER.warn("Object to be formatted cannot be empty!");
            return null;
        }
        ObjectWriter mapper = new ObjectMapper().writer()
            .withDefaultPrettyPrinter();
        return mapper.writeValueAsString(obj);
    }

    public static <T> T toEntity(String jsonStr, Class<T> type)
        throws IOException {
        if (StringUtils.isEmpty(jsonStr)) {
            LOGGER.warn("Json string {} is empty!", type);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);
        return mapper.readValue(jsonStr, type);
    }

    public static <T> T toEntity(File file, TypeReference type)
        throws IOException {
        if (file == null) {
            LOGGER.warn("File cannot be empty!");
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, type);
    }

    public static <T> T toEntity(InputStream in, TypeReference type)
        throws IOException {
        if (in == null) {
            throw new NullPointerException("Input stream cannot be null.");
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in, type);
    }

    public static <T> T toEntity(String jsonStr, TypeReference type)
        throws IOException {
        if (StringUtils.isEmpty(jsonStr)) {
            LOGGER.warn("Json string {} is empty!", type);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonStr, type);
    }

}
