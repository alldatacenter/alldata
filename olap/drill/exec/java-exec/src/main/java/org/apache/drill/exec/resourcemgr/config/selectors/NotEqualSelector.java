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

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;

/**
 * Simple selector whose value is another Simple or Complex Selectors. It does NOT of result of its child selector
 * configured with it to evaluate if a query can be admitted to it's ResourcePool or not.
 *
 * Example configuration is of form:
 * <code><pre>
 * selector: {
 *   not_equal: {tag: "BITool"}
 * }
 * </pre></code>
 */
public class NotEqualSelector extends AbstractResourcePoolSelector {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NotEqualSelector.class);

  private final ResourcePoolSelector poolSelector;

  NotEqualSelector(Config selectorValue) throws RMConfigException {
    super(SelectorType.NOT_EQUAL);
    poolSelector = ResourcePoolSelectorFactory.createSelector(selectorValue);
  }

  @Override
  public boolean isQuerySelected(QueryContext queryContext) {
    logger.debug("Query {} is evaluated for not_equal of selector type {}", queryContext.getQueryId(),
      poolSelector.getSelectorType().toString());
    return !poolSelector.isQuerySelected(queryContext);
  }

  @VisibleForTesting
  public ResourcePoolSelector getPoolSelector() {
    return poolSelector;
  }

  @Override
  public String toString() {
    return "{ SelectorType: " + super.toString() + ", of selector " + poolSelector.toString() + " }";
  }
}
