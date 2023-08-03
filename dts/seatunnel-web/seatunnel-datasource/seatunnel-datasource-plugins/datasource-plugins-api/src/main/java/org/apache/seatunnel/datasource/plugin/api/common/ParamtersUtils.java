/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.datasource.plugin.api.common;

import org.apache.seatunnel.common.utils.JsonUtils;

import java.util.Map;

public class ParamtersUtils {
    /**
     * for some parameters, we need to convert them to {@link Map} eg: s3Options { "access.value":
     * "org.apache.hadoop.fs.s3a.S3AFileSystem", "access.key": "AKIAIOSFODNN7EXAMPLE",
     * "hadoop_s3_properties": " fs.s3a.impl = org.apache.hadoop.fs.s3a.S3AFileSystem
     * fs.s3a.access.key = AKIAIOSFODNN7EXAMPLE "
     *
     * <p>Convert parameters to {@link Map}
     *
     * @param parameters parameters {@link Map}
     * @return {@link Map}
     */
    public static Map<String, Object> convertParams(Map<String, String> parameters) {
        String json = JsonUtils.toJsonString(parameters);
        return JsonUtils.toMap(json, String.class, Object.class);
    }
}
