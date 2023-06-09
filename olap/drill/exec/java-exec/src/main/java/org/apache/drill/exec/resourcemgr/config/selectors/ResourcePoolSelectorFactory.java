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
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelector.SelectorType;

public class ResourcePoolSelectorFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourcePoolSelectorFactory.class);

  public static ResourcePoolSelector createSelector(Config selectorConfig) throws RMConfigException {
    ResourcePoolSelector poolSelector = null;
    String selectorType = SelectorType.DEFAULT.toString();
    try {
      if (selectorConfig == null) {
        poolSelector = new DefaultSelector();
      } else if (selectorConfig.hasPath(SelectorType.TAG.toString())) {
        selectorType = SelectorType.TAG.toString();
        poolSelector = new TagSelector(selectorConfig.getString(selectorType));
      } else if (selectorConfig.hasPath(SelectorType.ACL.toString())) {
        selectorType = SelectorType.ACL.toString();
        poolSelector = new AclSelector(selectorConfig.getConfig(selectorType));
      } else if (selectorConfig.hasPath(SelectorType.OR.toString())) {
        selectorType = SelectorType.OR.toString();
        poolSelector = new OrSelector(selectorConfig.getConfigList(selectorType));
      } else if (selectorConfig.hasPath(SelectorType.AND.toString())) {
        selectorType = SelectorType.AND.toString();
        poolSelector = new AndSelector(selectorConfig.getConfigList(selectorType));
      } else if (selectorConfig.hasPath(SelectorType.NOT_EQUAL.toString())) {
        selectorType = SelectorType.NOT_EQUAL.toString();
        poolSelector = new NotEqualSelector(selectorConfig.getConfig(selectorType));
      }
    } catch (Exception ex) {
      throw new RMConfigException(String.format("There is an error with value configuration for selector type %s",
        selectorType), ex);
    }

    // if here means either a selector is chosen or wrong configuration
    if (poolSelector == null) {
      throw new RMConfigException(String.format("Configured selector is either empty or not supported. [Details: " +
        "SelectorConfig: %s]", selectorConfig));
    }

    logger.debug("Created selector of type {}", poolSelector.getSelectorType().toString());
    return poolSelector;
  }
}
