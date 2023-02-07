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
package org.apache.drill.exec.resourcemgr.config.selectors;

import com.typesafe.config.Config;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;

import java.util.List;

/**
 * Complex selector whose value is list of other Simple or Complex Selectors. There has to be at least 2 other
 * selectors configured in the value list for this selector. It does OR operation on result of all the child selectors
 * configured with it to evaluate if a query can be admitted to it's ResourcePool or not.
 *
 * Example configuration is of form:
 * <code></><pre>
 * selector: {
 *   or: [{tag: "BITool"},{tag: "operational"}]
 * }
 * </pre></code>
 */
public class OrSelector extends ComplexSelectors {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrSelector.class);

  OrSelector(List<? extends Config> configValue) throws RMConfigException {
    super(SelectorType.OR, configValue);
  }

  @Override
  public boolean isQuerySelected(QueryContext queryContext) {
    for (ResourcePoolSelector childSelector : childSelectors) {
      // If we find any selector evaluating to true then no need to evaluate other selectors in the list
      if (childSelector.isQuerySelected(queryContext)) {
        logger.debug("Query {} is selected by the child selector of type {} in this OrSelector",
          queryContext.getQueryId(), childSelector.getSelectorType().toString());
        return true;
      }
    }

    return false;
  }
}
