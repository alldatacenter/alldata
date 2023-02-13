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

package org.apache.inlong.manager.service.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for deal with jar hell.
 */
public class JarHellTest {

    @Test
    public void testJavaVersion() {
        JarHell.checkJavaVersion("test_java", "1.8");
        try {
            JarHell.checkJavaVersion("test_java", "1.81");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
            String msg = e.getMessage();
            Assertions.assertTrue(msg.contains("requires Java 1.8"));
        }
    }

}
