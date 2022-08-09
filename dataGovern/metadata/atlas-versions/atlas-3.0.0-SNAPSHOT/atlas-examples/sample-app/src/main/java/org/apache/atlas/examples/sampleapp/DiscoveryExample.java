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
package org.apache.atlas.examples.sampleapp;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.discovery.AtlasQuickSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;

import java.util.List;

public class DiscoveryExample {
    private static final String[] DSL_QUERIES = new String[] { "from DataSet", "from Process" };

    private final AtlasClientV2 client;

    DiscoveryExample(AtlasClientV2 client) {
        this.client = client;
    }

    public void testSearch() {
        for (String dslQuery : DSL_QUERIES) {
            try {
                AtlasSearchResult       result      = client.dslSearchWithParams(dslQuery, 10, 0);
                List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
                int                     resultCount = entities == null ? 0 : entities.size();

                SampleApp.log("DSL Query: " + dslQuery);
                SampleApp.log("  result count: " + resultCount);

                for (int i = 0; i < resultCount; i++) {
                    SampleApp.log("  result # " + (i + 1) + ": " + entities.get(i));
                }
            } catch (Exception e) {
                SampleApp.log("query -: " + dslQuery + " failed");
            }
        }
    }

    public void quickSearch(String searchString) {
        try {
            AtlasQuickSearchResult  result      = client.quickSearch(searchString, SampleAppConstants.TABLE_TYPE, false, 2, 0);
            List<AtlasEntityHeader> entities    = result != null && result.getSearchResults() != null ? result.getSearchResults().getEntities() : null;
            int                     resultCount = entities == null ? 0 : entities.size();

            SampleApp.log("Quick search: query-string=" + searchString);
            SampleApp.log("  result count: " + resultCount);

            for (int i = 0; i < resultCount; i++) {
                SampleApp.log("  result # " + (i + 1) + ": " + entities.get(i));
            }
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
    }

    public void basicSearch(String typeName, String classification, String query) {
        try {
            AtlasSearchResult       result      = client.basicSearch(typeName, classification, query, false, 2, 0);
            List<AtlasEntityHeader> entities    = result != null ? result.getEntities() : null;
            int                     resultCount = entities == null ? 0 : entities.size();

            SampleApp.log("Basic search: typeName=" + typeName + ", classification=" + classification + ", query=" + query);
            SampleApp.log("  result count: " + resultCount);

            for (int i = 0; i < resultCount; i++) {
                SampleApp.log("  result # " + (i + 1) + ": " + entities.get(i));
            }
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
    }
}
