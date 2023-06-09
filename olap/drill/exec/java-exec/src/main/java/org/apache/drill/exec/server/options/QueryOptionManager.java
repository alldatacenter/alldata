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
package org.apache.drill.exec.server.options;

import org.apache.drill.common.map.CaseInsensitiveMap;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link OptionManager} that holds options within {@link org.apache.drill.exec.ops.QueryContext}.
 */
public class QueryOptionManager extends InMemoryOptionManager {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QueryOptionManager.class);

  public QueryOptionManager(OptionManager sessionOptions) {
    super(sessionOptions, CaseInsensitiveMap.<OptionValue>newHashMap());
  }

  @Override
  public OptionList getOptionList() {
    Map<String, OptionValue> optionMap = new HashMap<>();
    for (OptionValue option : fallback.getOptionList()) {
      optionMap.put(option.name, option);
    }
    for (OptionValue option : super.getOptionList()) {
      optionMap.put(option.name, option);
    }
    return new OptionList(optionMap.values());
  }

  @Override
  public OptionValue getDefault(String optionName) {
    return fallback.getDefault(optionName);
  }

  public SessionOptionManager getSessionOptionManager() {
    return (SessionOptionManager) fallback;
  }

  public OptionManager getOptionManager(OptionValue.OptionScope scope) {
    switch (scope) {
      case SYSTEM:
        return getSessionOptionManager().getSystemOptionManager();
      case SESSION:
        return getSessionOptionManager();
      case QUERY:
        return this;
      case BOOT:
        throw new UnsupportedOperationException("There is no option manager for " + OptionValue.OptionScope.BOOT);
      default:
        throw new UnsupportedOperationException("Invalid type: " + scope);
    }
  }

  @Override
  protected OptionValue.OptionScope getScope() {
    return OptionValue.OptionScope.QUERY;
  }
}
