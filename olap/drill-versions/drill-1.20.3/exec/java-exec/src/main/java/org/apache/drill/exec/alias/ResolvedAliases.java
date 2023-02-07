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

import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Aliases table that fallback user aliases calls to public aliases
 * if alias not found in user aliases table.
 */
public class ResolvedAliases implements Aliases {
  private final Aliases userAliases;

  private final Supplier<Aliases> publicAliases;

  public ResolvedAliases(Aliases userAliases, Supplier<Aliases> publicAliases) {
    this.userAliases = userAliases;
    this.publicAliases = publicAliases;
  }

  @Override
  public String getKey() {
    return userAliases.getKey();
  }

  @Override
  public String get(String alias) {
    String value = userAliases.get(alias);
    return value != null ? value : publicAliases.get().get(alias);
  }

  @Override
  public boolean put(String alias, String value, boolean replace) {
    return userAliases.put(alias, value, replace);
  }

  @Override
  public boolean remove(String alias) {
    return userAliases.remove(alias);
  }

  @Override
  public Iterator<Map.Entry<String, String>> getAllAliases() {
    return userAliases.getAllAliases();
  }
}
