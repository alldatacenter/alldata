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

package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.commons.lang.Validate;

@NotThreadSafe
public class DeleteStatusMetaData implements RequestStatusMetaData {
  private Set<String> deletedKeys;
  private Map<String, Exception> exceptionMap;
  public DeleteStatusMetaData() {
    this.deletedKeys = new HashSet<>();
    this.exceptionMap = new HashMap<>();
  }

  public void addDeletedKey(String key) {
    Validate.notEmpty(key, "Key should not be empty");
    deletedKeys.add(key);
  }

  public Set<String> getDeletedKeys() {
    return Collections.unmodifiableSet(deletedKeys);
  }

  public void addException(String key, Exception exception) {
    Validate.notEmpty(key, "Key should not be empty");
    Validate.notNull(exception, "Exception cannot be null");
    exceptionMap.put(key, exception);
  }

  public Map<String, Exception> getExceptionForKeys() {
    return Collections.unmodifiableMap(exceptionMap);
  }
}
