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

package org.apache.inlong.manager.web.config;

import org.apache.inlong.manager.web.WebBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for rest template config.
 */
public class RestTemplateConfigTest extends WebBaseTest {

    private final int maxTotal = 5000;
    private final int defaultMaxPerRoute = 2000;
    private final int validateAfterInactivity = 5000;
    private final int connectionTimeout = 3000;
    private final int readTimeout = 10000;
    private final int connectionRequestTimeout = 3000;

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    @Test
    public void configValue() {
        Assertions.assertEquals(maxTotal, restTemplateConfig.getMaxTotal());
        Assertions.assertEquals(defaultMaxPerRoute, restTemplateConfig.getDefaultMaxPerRoute());
        Assertions.assertEquals(validateAfterInactivity, restTemplateConfig.getValidateAfterInactivity());
        Assertions.assertEquals(connectionTimeout, restTemplateConfig.getConnectionTimeout());
        Assertions.assertEquals(readTimeout, restTemplateConfig.getReadTimeout());
        Assertions.assertEquals(connectionRequestTimeout, restTemplateConfig.getConnectionRequestTimeout());
    }

}