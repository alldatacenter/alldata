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

package org.apache.atlas.discovery;


import org.apache.atlas.SortOrder;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.*;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;

import java.util.List;
import java.util.Set;

public interface AtlasDiscoveryService {
    /**
     *
     * @param query search query in DSL format.
     * @param limit number of resultant rows (for pagination). [ limit > 0 ] and [ limit < maxlimit ]. -1 maps to atlas.search.defaultlimit property.
     * @param offset offset to the results returned (for pagination). [ offset >= 0 ]. -1 maps to offset 0.
     * @return AtlasSearchResult
     */
    AtlasSearchResult searchUsingDslQuery(String query, int limit, int offset) throws AtlasBaseException;

    /**
     *
     * @param query query
     * @param typeName type name
     * @param classification classification
     * @return Query in DSL form
     */
    String getDslQueryUsingTypeNameClassification(String query, String typeName, String classification);

    /**
     *
     * @param query search query.
     * @param excludeDeletedEntities exclude deleted entities in search result.
     * @param limit number of resultant rows (for pagination). [ limit > 0 ] and [ limit < maxlimit ]. -1 maps to atlas.search.defaultlimit property.
     * @param offset offset to the results returned (for pagination). [ offset >= 0 ]. -1 maps to offset 0.
     * @return AtlasSearchResult
     */
    AtlasSearchResult searchUsingFullTextQuery(String query, boolean excludeDeletedEntities, int limit, int offset) throws AtlasBaseException;

    /**
     *
     * @param query search query.
     * @param type entity type.
     * @param classification classification name.
     * @param attrName attribute name.
     * @param attrValuePrefix attribute value prefix.
     * @param excludeDeletedEntities exclude deleted entities in search result.
     * @param limit number of resultant rows (for pagination). [ limit > 0 ] and [ limit < maxlimit ]. -1 maps to atlas.search.defaultlimit property.
     * @param offset offset to the results returned (for pagination). [ offset >= 0 ]. -1 maps to offset 0.
     * @return AtlasSearchResult
     */
    AtlasSearchResult searchUsingBasicQuery(String query, String type, String classification, String attrName,
                                            String attrValuePrefix, boolean excludeDeletedEntities, int limit, int offset) throws AtlasBaseException;

    /**
     * Search for entities matching the search criteria
     * @param searchParameters Search criteria
     * @return Matching entities
     * @throws AtlasBaseException
     */
    AtlasSearchResult searchWithParameters(SearchParameters searchParameters) throws AtlasBaseException;

    /**
     *
     * @param guid unique ID of the entity.
     * @param relation relation name.
     * @param getApproximateCount
     * @param searchParameters
     * @return AtlasSearchResult
     */
     AtlasSearchResult searchRelatedEntities(String guid, String relation, boolean getApproximateCount, SearchParameters searchParameters) throws AtlasBaseException;

    /**
     *
     *
     * @param savedSearch Search to be saved
     * @throws AtlasBaseException
     */
    AtlasUserSavedSearch addSavedSearch(String currentUser, AtlasUserSavedSearch savedSearch) throws AtlasBaseException;

    /**
     *
     * @param savedSearch Search to be saved
     * @throws AtlasBaseException
     */
    AtlasUserSavedSearch updateSavedSearch(String currentUser, AtlasUserSavedSearch savedSearch) throws AtlasBaseException;

    /**
     *
     * @param userName Name of the user for whom the saved searches are to be retrieved
     * @return List of saved searches for the user
     * @throws AtlasBaseException
     */
    List<AtlasUserSavedSearch> getSavedSearches(String currentUser, String userName) throws AtlasBaseException;

    /**
     *
     * @param guid Guid for the saved search
     * @return Search object identified by the guid
     * @throws AtlasBaseException
     */
    AtlasUserSavedSearch getSavedSearchByGuid(String currentUser, String guid) throws AtlasBaseException;

    /**
     *
     * @param userName Name of the user who the search belongs
     * @param searchName Name of the search to be retrieved
     * @return Search object identified by the name
     * @throws AtlasBaseException
     */
    AtlasUserSavedSearch getSavedSearchByName(String currentUser, String userName, String searchName) throws AtlasBaseException;

    /**
     *
     * @param currentUser User requesting the operation
     * @param guid Guid used to look up Saved Search
     * @throws AtlasBaseException
     */
    void deleteSavedSearch(String currentUser, String guid) throws AtlasBaseException;

    /**
     * Search for entities matching the search criteria
     * @param searchParameters Search criteria
     * @return Matching entities
     * @throws AtlasBaseException
     */
    AtlasQuickSearchResult quickSearch(QuickSearchParameters searchParameters) throws AtlasBaseException;

    /**
     * Should return top 5 suggestion strings for the given prefix.
     * @param prefixString the prefix string
     * @param fieldName field from which to retrieve suggestions
     * @return top 5 suggestion strings for the given prefix.
     */
    AtlasSuggestionsResult getSuggestions(String prefixString, String fieldName);
}
