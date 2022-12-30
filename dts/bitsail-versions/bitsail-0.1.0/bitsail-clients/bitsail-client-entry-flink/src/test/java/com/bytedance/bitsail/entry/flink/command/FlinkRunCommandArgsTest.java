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

package com.bytedance.bitsail.entry.flink.command;

import com.bytedance.bitsail.client.api.command.CommandArgsParser;

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class FlinkRunCommandArgsTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test(expected = ParameterException.class)
  public void requiredParameterTest() {
    FlinkRunCommandArgs flinkRunCommandArgs = new FlinkRunCommandArgs();
    String[] args = new String[] {
        "--queue", "test",
        "--deployment-mode", "yarn-per-job"
    };
    CommandArgsParser.parseArguments(args, flinkRunCommandArgs);
    exceptionRule.expect(ParameterException.class);
    exceptionRule.expectMessage("The following option is required: [--execution-mode]");

    args = new String[] {
        "--queue", "test",
        "--execution-mode", "run"};
    CommandArgsParser.parseArguments(args, flinkRunCommandArgs);
    exceptionRule.expect(ParameterException.class);
    exceptionRule.expectMessage("The following option is required: [--deployment-mode]");
  }

  @Test
  public void unknownParameterTest() {
    String[] args = new String[] {
        "--execution-mode", "run",
        "--queue", "test",
        "--deployment-mode", "yarn-per-job",
        "--priority", "1",
        "--key", "value"
    };
    FlinkRunCommandArgs flinkRunCommandArgs = new FlinkRunCommandArgs();
    String[] unknownArgs = CommandArgsParser.parseArguments(args, flinkRunCommandArgs);
    assertEquals(unknownArgs.length, 2);
  }
}
