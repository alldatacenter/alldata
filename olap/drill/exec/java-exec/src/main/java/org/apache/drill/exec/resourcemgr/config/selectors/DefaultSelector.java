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

import org.apache.drill.exec.ops.QueryContext;

/**
 * When selector configuration is absent for a ResourcePool then it is associated with a DefaultSelector. It acts as
 * a sink for all the queries which means all the queries will be selected by this default selector.
 */
public class DefaultSelector extends AbstractResourcePoolSelector {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSelector.class);

  DefaultSelector() {
    super(SelectorType.DEFAULT);
  }

  @Override
  public SelectorType getSelectorType() {
    return SELECTOR_TYPE;
  }

  @Override
  public boolean isQuerySelected(QueryContext queryContext) {
    logger.debug("Query {} is selected by this Default selector", queryContext.getQueryId());
    return true;
  }

  @Override
  public String toString() {
    return "{SelectorType: " + super.toString() + "}";
  }
}
