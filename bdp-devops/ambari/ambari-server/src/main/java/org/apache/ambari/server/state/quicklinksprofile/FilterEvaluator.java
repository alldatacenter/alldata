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

package org.apache.ambari.server.state.quicklinksprofile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.state.quicklinks.Link;

/**
 * Groups quicklink filters that are on the same level (e.g. a global evaluator or an evaluator for the "HDFS" service,
 * etc.). The evaluator pick the most applicable filter for a given quick link. If no applicable filter is found, it
 * returns {@link Optional#empty()}.
 * <p>
 *   Filter evaluation order is the following:
 *   <ol>
 *     <li>First, link name filters are evaluated. These match links by name.</li>
 *     <li>If there is no matching link name filter, link attribute filters are evaluated next. "Hide" type filters
 *     take precedence to "show" type filters.</li>
 *     <li>Finally, the match-all filter is evaluated, provided it exists.</li>
 *   </ol>
 * </p>
 */
class FilterEvaluator {
  private final Map<String, Boolean> linkNameFilters = new HashMap<>();
  private final Set<String> showAttributes = new HashSet<>();
  private final Set<String> hideAttributes = new HashSet<>();
  private Optional<Boolean> acceptAllFilter = Optional.empty();

  FilterEvaluator(List<Filter> filters) throws QuickLinksProfileEvaluationException {
    for (Filter filter: DefaultQuickLinkVisibilityController.nullToEmptyList(filters)) {
      if (filter instanceof LinkNameFilter) {
        String linkName = ((LinkNameFilter)filter).getLinkName();
        if (linkNameFilters.containsKey(linkName) && linkNameFilters.get(linkName) != filter.isVisible()) {
          throw new QuickLinksProfileEvaluationException("Contradicting filters for link name [" + linkName + "]");
        }
        linkNameFilters.put(linkName, filter.isVisible());
      }
      else if (filter instanceof LinkAttributeFilter) {
        String linkAttribute = ((LinkAttributeFilter)filter).getLinkAttribute();
        if (filter.isVisible()) {
          showAttributes.add(linkAttribute);
        }
        else {
          hideAttributes.add(linkAttribute);
        }
        if (showAttributes.contains(linkAttribute) && hideAttributes.contains(linkAttribute)) {
          throw new QuickLinksProfileEvaluationException("Contradicting filters for link attribute [" + linkAttribute + "]");
        }
      }
      // If none of the above, it is an accept-all filter. We expect only one of this type for an Evaluator
      else {
        if (acceptAllFilter.isPresent() && !acceptAllFilter.get().equals(filter.isVisible())) {
          throw new QuickLinksProfileEvaluationException("Contradicting accept-all filters.");
        }
        acceptAllFilter = Optional.of(filter.isVisible());
      }
    }
  }

  /**
   * @param quickLink the link to evaluate
   * @return Three way evaluation result, which can be one of these:
   *    show: Optional.of(true), hide: Optional.of(false), don't know: absent optional
   */
  Optional<Boolean> isVisible(Link quickLink) {
    // process first priority filters based on link name
    if (linkNameFilters.containsKey(quickLink.getName())) {
      return Optional.of(linkNameFilters.get(quickLink.getName()));
    }

    // process second priority filters based on link attributes
    // 'hide' rules take precedence over 'show' rules
    for (String attribute: DefaultQuickLinkVisibilityController.nullToEmptyList(quickLink.getAttributes())) {
      if (hideAttributes.contains(attribute)) return Optional.of(false);
    }
    for (String attribute: DefaultQuickLinkVisibilityController.nullToEmptyList(quickLink.getAttributes())) {
      if (showAttributes.contains(attribute)) return Optional.of(true);
    }

    // accept all filter (if exists) is the last priority
    return acceptAllFilter;
  }
}
