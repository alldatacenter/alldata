/*
 * Copyright 2022 ByteDance and/or its affiliates
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.core;

import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EngineTest {
  @Test
  public void testBase64ArgsToConfig() {
    String jobConf = "{\n" +
        "    \"key\":\"value\"\n" +
        "}";
    Engine engine = new Engine(
        new String[] {
            "-xjob_conf_in_base64", Base64.getEncoder().encodeToString(jobConf.getBytes())
        });
    assertTrue(engine.getBitSailConfiguration().fieldExists("key"));
    assertFalse(engine.getBitSailConfiguration().fieldExists("key1"));
  }

  @Test
  public void testConfPathToConfig() throws URISyntaxException {
    String confPath = Paths.get(this.getClass().getResource("/conf.json").toURI()).toString();
    Engine engine = new Engine(
        new String[] {
            "-xjob_conf", confPath
        });
    assertTrue(engine.getBitSailConfiguration().fieldExists("key"));
    assertFalse(engine.getBitSailConfiguration().fieldExists("key1"));
  }
}
