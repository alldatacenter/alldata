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

/**
 * Aliases table. Provides API for managing and obtaining aliases.
 */
public interface Aliases {
  /**
   * Key of {@link this} aliases table.
   */
  String getKey();

  /**
   * Returns value from aliases table that corresponds to provided alias.
   *
   * @param alias alias of the value to obtain
   * @return value from aliases table that corresponds to provided alias
   */
  String get(String alias);

  /**
   * Associates provided alias with provided alias in aliases table.
   *
   * @param alias   alias of the value to associate with
   * @param value   value that will be associated with provided alias
   * @param replace whether existing value for the same alias should be replaced
   * @return {@code true} if provided alias was associated with
   * the provided value in aliases table
   */
  boolean put(String alias, String value, boolean replace);

  /**
   * Removes value for specified alias from aliases table.
   * @param alias alias of the value to remove
   * @return {@code true} if the value associated with
   * provided alias was removed from the aliases table
   */
  boolean remove(String alias);

  /**
   * Returns iterator for all entries of {@link this} aliases table.
   */
  Iterator<Map.Entry<String, String>> getAllAliases();
}
