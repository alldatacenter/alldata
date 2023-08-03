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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ParamtersUtilsTest {

    @Test
    public void testConvertParams() {
        Map<String, Object> s3Options = new HashMap<>();
        s3Options.put("access.key", "myaccess");
        s3Options.put("access.value", "myvalue");
        Map<String, String> hadoopConfig = new HashMap<>();
        hadoopConfig.put("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        s3Options.put("hadoopConfig", hadoopConfig);
        String s3OptionsJson = JsonUtils.toJsonString(s3Options);
        Map<String, String> s3OptionsMap =
                JsonUtils.toMap(s3OptionsJson, String.class, String.class);

        Map<String, Object> s3OptionsMapConvertResult = ParamtersUtils.convertParams(s3OptionsMap);
        Assertions.assertEquals(s3OptionsMapConvertResult.get("hadoopConfig"), hadoopConfig);
        Assertions.assertEquals(
                s3OptionsMapConvertResult.get("access.key"), s3Options.get("access.key"));
        Assertions.assertEquals(
                s3OptionsMapConvertResult.get("access.value"), s3Options.get("access.value"));
    }
}
