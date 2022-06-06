/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.util;

import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.discovery.SearchContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AtlasService
public class SearchTracker {
    private Map<String, SearchContext> activeSearches = new HashMap<>();

    /**
     *
     * @param context
     */
    public String add(SearchContext context) {
        String searchId = Thread.currentThread().getName();

        activeSearches.put(searchId, context);

        return searchId;
    }

    /**
     *
     * @param searchId
     * @return
     */
    public SearchContext terminate(String searchId) {
        SearchContext ret = null;

        if (activeSearches.containsKey(searchId)) {
            SearchContext pipelineToTerminate = activeSearches.remove(searchId);

            pipelineToTerminate.terminateSearch(true);

            ret = pipelineToTerminate;
        }

        return ret;
    }

    public SearchContext remove(String id) {
        return activeSearches.remove(id);
    }

    /**
     *
     * @return
     */
    public Set<String> getActiveSearches() {
        return activeSearches.keySet();
    }
}
