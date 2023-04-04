/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.utils.terminal;

/**
 * A interface for {@link SimpleShellTerminal} to define your own processing logic
 */
public interface TerminalService {

  /**
   * Method to resolve all line input by console.
   */
  void resolve(String line, TerminalOutput terminalOutput) throws Exception;

  /**
   * The method will be called when the console is closed
   */
  void close();

  /**
   * Print welcome statement to the console.
   */
  String welcome();

  /**
   * These keywords will have character completion applied.
   */
  String[] keyWord();

  /**
   * The console enters the prompt on the left.
   */
  String prompt();

}
