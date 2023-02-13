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

package org.apache.inlong.sort.base.format;

import com.google.common.base.Preconditions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Dynamic schema format base test
 *
 * @param <T>
 */
public abstract class DynamicSchemaFormatBaseTest<T> {

    private String source;
    private AbstractDynamicSchemaFormat<T> dynamicSchemaFormat;
    private Map<String, String> expectedValues;

    protected abstract String getSource();

    protected abstract Map<String, String> getExpectedValues();

    protected abstract AbstractDynamicSchemaFormat<T> getDynamicSchemaFormat();

    @Before
    public void init() {
        source = Preconditions.checkNotNull(getSource(), "Source is null");
        expectedValues = Preconditions.checkNotNull(getExpectedValues(), "Expected values is null");
        dynamicSchemaFormat = Preconditions.checkNotNull(getDynamicSchemaFormat(), "Dynamic schema format is null");
    }

    @Test
    public void testParse() throws IOException {
        T data = dynamicSchemaFormat.deserialize(source.getBytes(StandardCharsets.UTF_8));
        for (Entry<String, String> kvs : expectedValues.entrySet()) {
            String result = dynamicSchemaFormat.parse(data, kvs.getKey());
            Assert.assertEquals(kvs.getValue(), result);
        }
    }

}
