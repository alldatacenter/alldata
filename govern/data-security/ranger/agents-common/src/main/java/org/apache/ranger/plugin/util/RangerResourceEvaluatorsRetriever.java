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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerResourceTrie;
import org.apache.ranger.plugin.policyresourcematcher.RangerResourceEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RangerResourceEvaluatorsRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(RangerResourceEvaluatorsRetriever.class);

    public static <T  extends RangerResourceEvaluator> Collection<T> getEvaluators(Map<String, RangerResourceTrie<T>> resourceTrie, Map<String, ?> resource) {
        return getEvaluators(resourceTrie, resource, RangerAccessRequest.ResourceMatchingScope.SELF);
    }

    public static <T  extends RangerResourceEvaluator> Collection<T> getEvaluators(Map<String, RangerResourceTrie<T>> resourceTrie, Map<String, ?> resource, RangerAccessRequest.ResourceMatchingScope scope) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerPolicyResourceEvaluatorsRetriever.getEvaluators(" + resource + ")");
        }
        Set<T> ret = null;

        if (MapUtils.isNotEmpty(resourceTrie) && MapUtils.isNotEmpty(resource)) {
            Set<String> resourceKeys = resource.keySet();

            List<Evaluators<T>> sortedEvaluators = new ArrayList<>(resourceKeys.size());

            for (String resourceDefName : resourceKeys) {
                RangerResourceTrie<T> trie = resourceTrie.get(resourceDefName);

                if (trie == null) {
                    continue;
                }

                Object resourceValues = resource.get(resourceDefName);

                Set<T> inheritedMatchers   = trie.getInheritedEvaluators();
                Set<T> matchersForResource = trie.getEvaluatorsForResource(resourceValues, scope);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("ResourceDefName:[" + resourceDefName + "], values:[" + resourceValues + "], resource-matchers:[" + matchersForResource + "], inherited-matchers:[" + inheritedMatchers + "]");
                }
                if (CollectionUtils.isEmpty(inheritedMatchers) && CollectionUtils.isEmpty(matchersForResource)) {
                    sortedEvaluators.clear();
                    break;
                }
                sortedEvaluators.add(new Evaluators<>(inheritedMatchers, matchersForResource));
            }

            if (CollectionUtils.isNotEmpty(sortedEvaluators)) {
                Collections.sort(sortedEvaluators);

                ret = sortedEvaluators.remove(0).getMatchers();

                for (Evaluators<T> evaluators : sortedEvaluators) {
                    if (CollectionUtils.isEmpty(evaluators.inheritedMatchers)) {
                        ret.retainAll(evaluators.resourceMatchers);
                    } else if (CollectionUtils.isEmpty(evaluators.resourceMatchers)) {
                        ret.retainAll(evaluators.inheritedMatchers);
                    } else {
                        Set<T> smaller = evaluators.getSmaller();
                        Set<T> bigger  = evaluators.getBigger();

                        Set<T> tmp = new HashSet<>(ret.size());

                        if (ret.size() < smaller.size()) {
                            ret.stream().filter(smaller::contains).forEach(tmp::add);
                            ret.stream().filter(bigger::contains).forEach(tmp::add);
                        } else {
                            smaller.stream().filter(ret::contains).forEach(tmp::add);
                            if (ret.size() < bigger.size()) {
                                ret.stream().filter(bigger::contains).forEach(tmp::add);
                            } else {
                                bigger.stream().filter(ret::contains).forEach(tmp::add);
                            }
                        }
                        ret = tmp;
                    }
                    if (ret.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerResourceEvaluatorsRetriever.getEvaluators(" + resource + ") : evaluator:[" + ret + "]");
        }
        return ret;
    }

    static class Evaluators<T> implements Comparable<Evaluators<T>> {
        private final Set<T> inheritedMatchers;
        private final Set<T> resourceMatchers;
        private final Set<T> smaller;
        private final Set<T> bigger;
        private final int    size;

        Evaluators(Set<T> inherited, Set<T> matched) {
            inheritedMatchers = inherited == null ? Collections.emptySet() : inherited;
            resourceMatchers  = matched   == null ? Collections.emptySet() : matched;
            size              = inheritedMatchers.size() + resourceMatchers.size();
            smaller           = inheritedMatchers.size() < resourceMatchers.size() ? inheritedMatchers : resourceMatchers;
            bigger            = smaller == inheritedMatchers ? resourceMatchers : inheritedMatchers;
        }

        // Should be called at most once
        Set<T> getMatchers() {
            Set<T> ret = new HashSet<>(size);

            ret.addAll(inheritedMatchers);
            ret.addAll(resourceMatchers);

            return ret;
        }

        Set<T> getSmaller() {
            return smaller;
        }

        Set<T> getBigger() {
            return bigger;
        }

        @Override
        public int compareTo(Evaluators<T> other) {
            return Integer.compare(size, other.size);
        }
    }
}
