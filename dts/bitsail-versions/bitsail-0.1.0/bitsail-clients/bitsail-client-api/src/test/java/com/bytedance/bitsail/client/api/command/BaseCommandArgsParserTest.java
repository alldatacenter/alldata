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

package com.bytedance.bitsail.client.api.command;

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Created 2022/8/11
 */
public class BaseCommandArgsParserTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void testBaseCommandArgs() {
    String[] args = new String[] {"--engine", "flink", "-d", "-sae"};

    BaseCommandArgs baseCommandArgs = new BaseCommandArgs();
    String[] strings = CommandArgsParser.parseArguments(args, baseCommandArgs);

    assertEquals(1, strings.length);
    assertEquals(strings[0], "-sae");

    args = new String[] {"--engine", "flink", "--conf", "test", "--props", "key=value"};
    strings = CommandArgsParser.parseArguments(args, baseCommandArgs);
    assertEquals(baseCommandArgs.getProperties().size(), 1);
    assertEquals(0, strings.length);
  }

  @Test(expected = ParameterException.class)
  public void testDynamicParameter() {
    String[] args = new String[] {"--engine", "flink", "--props", "key"};
    BaseCommandArgs baseCommandArgs = new BaseCommandArgs();
    CommandArgsParser.parseArguments(args, baseCommandArgs);
    exceptionRule.expect(ParameterException.class);
    exceptionRule.expectMessage("Dynamic parameter expected a value of the form a=b but got");
  }
}