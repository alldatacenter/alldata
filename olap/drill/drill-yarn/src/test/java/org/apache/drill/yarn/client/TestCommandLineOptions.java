/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.client;

import org.apache.drill.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestCommandLineOptions extends BaseTest {
  @Test
  public void testOptions() {
    CommandLineOptions opts = new CommandLineOptions();
    opts.parse(new String[] {});
    assertNull(opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "-h" });
    assertEquals(CommandLineOptions.Command.HELP, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "-?" });
    assertEquals(CommandLineOptions.Command.HELP, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "help" });
    assertEquals(CommandLineOptions.Command.HELP, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "start" });
    assertEquals(CommandLineOptions.Command.START, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "stop" });
    assertEquals(CommandLineOptions.Command.STOP, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "status" });
    assertEquals(CommandLineOptions.Command.STATUS, opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "resize" });
    assertNull(opts.getCommand());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "resize", "10" });
    assertEquals(CommandLineOptions.Command.RESIZE, opts.getCommand());
    assertEquals("", opts.getResizePrefix());
    assertEquals(10, opts.getResizeValue());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "resize", "+2" });
    assertEquals(CommandLineOptions.Command.RESIZE, opts.getCommand());
    assertEquals("+", opts.getResizePrefix());
    assertEquals(2, opts.getResizeValue());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "resize", "-3" });
    assertEquals(CommandLineOptions.Command.RESIZE, opts.getCommand());
    assertEquals("-", opts.getResizePrefix());
    assertEquals(3, opts.getResizeValue());

    opts = new CommandLineOptions();
    opts.parse(new String[] { "myDrill" });
    assertNull(opts.getCommand());
  }
}
