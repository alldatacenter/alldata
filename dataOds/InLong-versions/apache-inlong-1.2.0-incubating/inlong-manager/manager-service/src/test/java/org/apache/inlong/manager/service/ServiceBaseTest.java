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

package org.apache.inlong.manager.service;

import org.apache.inlong.manager.test.BaseTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Test class for base test service.
 */
@SpringBootApplication
@SpringBootTest(classes = ServiceBaseTest.class)
@EnableWebMvc
public class ServiceBaseTest extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBaseTest.class);

    public static final String GLOBAL_GROUP_ID = "global_group";
    public static final String GLOBAL_STREAM_ID = "global_stream";
    public static final String GLOBAL_OPERATOR = "admin";

    @Test
    public void test() {
        LOGGER.info("The test class cannot be empty, otherwise 'No runnable methods exception' will be reported");
    }
}
