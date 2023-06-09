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
 * Interface that defines implementation for selectors assigned to a ResourcePool. ResourcePoolSelector helps to
 * evaluate if a given query can be admitted into a ResourcePool or not. Based on the assigned selector type to a
 * ResourcePool it uses the query metadata with it's own configured values and make a decision for a query. The
 * SelectorType defines all the supported ResourcePoolSelector which can be assigned to a ResourcePool. The
 * configuration of a selector is of type:
 * <code><pre>
 * selector: {
 *   SelectorType:SelectorValue
 * }
 * where SelectorValue can be a string (for SelectorType tag),
 *       object (for SelectorType acl and not_equal) and
 *       list of objects (for SelectorType and, or)
 * when selector config is absent then a DefaultSelector is associated with the ResourcePool
 * </pre></code>
 */
public interface ResourcePoolSelector {

  enum SelectorType {
    UNKNOWN,
    DEFAULT,
    TAG,
    ACL,
    OR,
    AND,
    NOT_EQUAL;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  SelectorType getSelectorType();

  boolean isQuerySelected(QueryContext queryContext);
}
