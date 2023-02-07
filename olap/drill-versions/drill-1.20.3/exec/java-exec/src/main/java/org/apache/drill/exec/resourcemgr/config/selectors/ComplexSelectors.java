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

import java.util.ArrayList;
import java.util.List;

public abstract class ComplexSelectors extends AbstractResourcePoolSelector {

  protected final List<ResourcePoolSelector> childSelectors = new ArrayList<>();

  ComplexSelectors(SelectorType type, List<? extends Config> selectorConfig) throws RMConfigException {
    super(type);
    parseAndCreateChildSelectors(selectorConfig);
  }

  private void parseAndCreateChildSelectors(List<? extends Config> childConfigs) throws RMConfigException {
    for (Config childConfig : childConfigs) {
      childSelectors.add(ResourcePoolSelectorFactory.createSelector(childConfig));
    }

    if (childSelectors.size() < 2) {
      throw new RMConfigException(String.format("For complex selector OR and AND it is expected to have atleast 2 " +
        "selectors in the list but found %d", childSelectors.size()));
    }
  }

  public abstract boolean isQuerySelected(QueryContext queryContext);

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ SelectorType: ").append(super.toString());
    sb.append(", of selectors [");
    for (ResourcePoolSelector childSelector : childSelectors) {
      sb.append(childSelector.toString()).append(", ");
    }
    sb.append("]}");
    return sb.toString();
  }
}
