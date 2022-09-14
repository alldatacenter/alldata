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

package org.apache.inlong.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * MaskDataUtils unit test
 */
public class MaskDataUtilsTest {

    @Test
    public void testMaskDataUtils() throws Exception {
        String unmasked = "{\n"
                + "  \"password\": \"inlong\",\n"
                + "  \"pwd\": \"inlong\",\n"
                + "  \"pass\": \"inlong\",\n"
                + "  \"token\": \"inlong\",\n"
                + "  \"secret_token\": \"inlong\",\n"
                + "  \"secretToken\": \"inlong\",\n"
                + "  \"secret_id\": \"inlong\",\n"
                + "  \"secretId\": \"inlong\",\n"
                + "  \"secret_key\": \"inlong\",\n"
                + "  \"secretKey\": \"inlong\",\n"
                + "  \"public_key\": \"inlong\",\n"
                + "  \"publicKey\": \"inlong\"\n"
                + "}";
        String masked = "{\n"
                + "  \"password\": \"******\",\n"
                + "  \"pwd\": \"******\",\n"
                + "  \"pass\": \"******\",\n"
                + "  \"token\": \"******\",\n"
                + "  \"secret_token\": \"******\",\n"
                + "  \"secretToken\": \"******\",\n"
                + "  \"secret_id\": \"******\",\n"
                + "  \"secretId\": \"******\",\n"
                + "  \"secret_key\": \"******\",\n"
                + "  \"secretKey\": \"******\",\n"
                + "  \"public_key\": \"******\",\n"
                + "  \"publicKey\": \"******\"\n"
                + "}";
        StringBuilder buffer = new StringBuilder(unmasked);
        MaskDataUtils.mask(buffer);
        assertEquals(masked, buffer.toString());
    }

}
