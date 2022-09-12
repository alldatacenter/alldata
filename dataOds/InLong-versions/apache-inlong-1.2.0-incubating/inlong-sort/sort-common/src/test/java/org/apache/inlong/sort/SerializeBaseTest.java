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

package org.apache.inlong.sort;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test serialize base class
 *
 * @param <T> The test object type
 */
@Getter
public abstract class SerializeBaseTest<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private T testObject;

    /**
     * Get test object
     *
     * @return The test object
     */
    public abstract T getTestObject();

    /**
     * Init the test object
     */
    @Before
    public void init() {
        this.testObject = Preconditions.checkNotNull(getTestObject());
    }

    /**
     * Test serialize
     *
     * @throws JsonProcessingException The exception may throws when execute the method
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSerialize() throws JsonProcessingException {
        String serializeStr = objectMapper.writeValueAsString(testObject);
        T expected = (T) objectMapper.readValue(serializeStr, testObject.getClass());
        assertEquals(expected, testObject);
    }
}
