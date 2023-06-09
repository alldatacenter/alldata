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
 * Implementation of aliases table that doesn't hold or return information.
 */
public class EmptyAliases implements Aliases {
  public static final EmptyAliases INSTANCE = new EmptyAliases();

  @Override
  public String getKey() {
    return null;
  }

  @Override
  public String get(String alias) {
    return null;
  }

  @Override
  public boolean put(String alias, String value, boolean replace) {
    throw new UnsupportedOperationException("EmptyAliases cannot be set");
  }

  @Override
  public boolean remove(String alias) {
    return false;
  }

  @Override
  public Iterator<Map.Entry<String, String>> getAllAliases() {
    return Collections.emptyIterator();
  }
}
