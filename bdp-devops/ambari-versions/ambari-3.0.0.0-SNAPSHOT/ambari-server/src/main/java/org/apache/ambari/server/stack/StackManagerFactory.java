/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.stack;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.ambari.server.state.stack.OsFamily;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * The {@link StackManagerFactory} is used along with {@link AssistedInject} to
 * build instances of {@link StackManager}.
 */
public interface StackManagerFactory {

  /**
   * @param stackRoot
   *          the root of the stack (not {@code null}).
   * @param commonServicesRoot
   *          the root of the common services from which other stack services
   *          are extended (not {@code null}).
   * @param extensionRoot
   *          the root of the extensions (not {@code null}).
   * @param osFamily
   *          the list of all parsed OS families (not {@code null}).
   * @return a stack manager instance which contains all parsed stacks.
   */
  StackManager create(@Assisted("stackRoot") File stackRoot,
      @Nullable @Assisted("commonServicesRoot") File commonServicesRoot,
      @Assisted("extensionRoot") @Nullable File extensionRoot,
      OsFamily osFamily, boolean validate);
}
