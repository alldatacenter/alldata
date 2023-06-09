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
package org.apache.drill.exec.alias;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of {@link AliasRegistry} that does nothing.
 */
public class NoopAliasRegistry implements AliasRegistry {
  public static final AliasRegistry INSTANCE = new NoopAliasRegistry();

  private NoopAliasRegistry() {
  }

  public Iterator<Map.Entry<String, Aliases>> getAllAliases() {
    return Collections.emptyIterator();
  }

  @Override
  public Aliases getUserAliases(String userName) {
    return EmptyAliases.INSTANCE;
  }

  @Override
  public void createUserAliases(String userName) {
    throw new UnsupportedOperationException("Cannot create user aliases for NoopAliasRegistry");
  }

  @Override
  public void createPublicAliases() {
    throw new UnsupportedOperationException("Cannot create user aliases for NoopAliasRegistry");
  }

  @Override
  public void deleteUserAliases(String userName) {
  }

  @Override
  public void deletePublicAliases() {
  }

  @Override
  public Aliases getPublicAliases() {
    return EmptyAliases.INSTANCE;
  }

  @Override
  public void close() throws Exception {
  }
}
