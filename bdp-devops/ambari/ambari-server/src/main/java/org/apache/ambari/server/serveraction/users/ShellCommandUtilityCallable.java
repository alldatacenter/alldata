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

import java.util.concurrent.Callable;

import javax.inject.Singleton;

import org.apache.ambari.server.utils.ShellCommandUtil;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Wraps a shell command execution into a callable.
 */
@Singleton
public class ShellCommandUtilityCallable implements Callable<ShellCommandUtil.Result> {

  private String[] args;

  @AssistedInject
  public ShellCommandUtilityCallable(@Assisted String[] args) {
    this.args = args;
  }

  @Override
  public ShellCommandUtil.Result call() throws Exception {
    return ShellCommandUtil.runCommand(args);
  }
}
