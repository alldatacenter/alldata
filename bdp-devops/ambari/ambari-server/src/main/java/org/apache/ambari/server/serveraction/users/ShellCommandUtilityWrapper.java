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

package org.apache.ambari.server.serveraction.users;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.utils.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the {@link org.apache.ambari.server.utils.ShellCommandUtil} utility.
 * Simply delegates to the utility. It's intended to be used instead of directly invoking the utility class.
 *
 * Classes using the wrapper will be decoupled from the static utility; thus can be used and tested easily.
 */
@Singleton
public class ShellCommandUtilityWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShellCommandUtilityWrapper.class);

  @Inject
  public ShellCommandUtilityWrapper() {
    super();
  }

  public ShellCommandUtil.Result runCommand(String[] args) throws IOException, InterruptedException {
    LOGGER.info("Running command: {}", args);
    return ShellCommandUtil.runCommand(args);
  }

  public ShellCommandUtil.Result runCommand(String[] args, Map<String, String> vars) throws IOException, InterruptedException {
    LOGGER.info("Running command: {}, variables: {}", args, vars);
    return ShellCommandUtil.runCommand(args, vars);
  }

}
