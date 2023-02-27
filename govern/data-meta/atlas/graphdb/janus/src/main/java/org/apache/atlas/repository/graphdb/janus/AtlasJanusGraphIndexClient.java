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
package org.apache.atlas.repository.graphdb.janus;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasAggregationEntry;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AggregationContext;
import org.apache.atlas.repository.graphdb.AtlasGraphIndexClient;
import org.apache.atlas.repository.graphdb.AtlasGraphManagement;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.V2Request;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.janusgraph.diskstorage.es.ElasticSearch7Index;
import org.janusgraph.diskstorage.es.ElasticSearchClient;
import org.janusgraph.diskstorage.solr.Solr6Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static org.apache.atlas.repository.Constants.FREETEXT_REQUEST_HANDLER;
import static org.apache.atlas.repository.Constants.VERTEX_INDEX;

public class AtlasJanusGraphIndexClient implements AtlasGraphIndexClient {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasJanusGraphIndexClient.class);

    private static final FreqComparator FREQ_COMPARATOR              = new FreqComparator();
    private static final int            DEFAULT_SUGGESTION_COUNT     = 5;
    private static final int            MIN_FACET_COUNT_REQUIRED     = 1;
    private static final String         TERMS_PREFIX                 = "terms.prefix";
    private static final String         TERMS_FIELD                  = "terms.fl";
    private static final int            SOLR_HEALTHY_STATUS          = 0;
    private static final long           SOLR_STATUS_LOG_FREQUENCY_MS = 60000;//Prints SOLR DOWN status for every 1 min
    private static       long           prevIdxHealthCheckTime;


    private final Configuration configuration;


    public AtlasJanusGraphIndexClient(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isHealthy() {
        boolean isHealthy   = false;
        long    currentTime = System.currentTimeMillis();
        String  idxBackEnd  = configuration.getString(ApplicationProperties.INDEX_BACKEND_CONF);

        try {
            if (ApplicationProperties.INDEX_BACKEND_SOLR.equals(idxBackEnd)) {
                isHealthy = isSolrHealthy();
            } else if (ApplicationProperties.INDEX_BACKEND_ELASTICSEARCH.equals(idxBackEnd)) {
                isHealthy = isElasticsearchHealthy();
            }

            LOG.info("indexBackEnd={}; isHealthy={}", idxBackEnd, isHealthy);
        } catch (Exception exception) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Error: isHealthy", exception);
            }
        }

        if (!isHealthy && (prevIdxHealthCheckTime == 0 || currentTime - prevIdxHealthCheckTime > SOLR_STATUS_LOG_FREQUENCY_MS)) {
            LOG.info("Backend Health: Unhealthy!");

            prevIdxHealthCheckTime = currentTime;
        }

        return isHealthy;
    }

    @Override
    public void applySearchWeight(String collectionName, Map<String, Integer> indexFieldName2SearchWeightMap) {
        SolrClient solrClient = null;

        try {
            solrClient = Solr6Index.getSolrClient(); // get solr client using same settings as that of Janus Graph

            if (solrClient == null) {
                LOG.warn("AtlasJanusGraphIndexClient.applySearchWeight(): Non SOLR index stores are not supported yet.");

                return;
            }

            Solr6Index.Mode solrMode = Solr6Index.getSolrMode();

            //1) try updating request handler
            //2) if update fails, try creating request handler

            int       maxAttempts         = configuration != null ? configuration.getInt("index.client.apply.search.weight.max.attempts", 3) : 3;
            int       retryWaitIntervalMs = configuration != null ? configuration.getInt("index.client.apply.search.weight.retry.interval.ms", 1000) : 1000;
            Throwable lastExcp            = null;

            for (int i = 0; i < maxAttempts; i++) {
                if (i > 0) {
                    LOG.warn("Attempt #{} failed! Waiting for {}ms before retry", i, retryWaitIntervalMs);

                    try {
                        Thread.sleep(retryWaitIntervalMs);
                    } catch (Exception excp) {
                        // ignore
                    }
                }

                try {
                    LOG.info("Attempting to update free text request handler {} for collection {}", FREETEXT_REQUEST_HANDLER, collectionName);

                    updateFreeTextRequestHandler(solrClient, collectionName, indexFieldName2SearchWeightMap, solrMode);

                    LOG.info("Successfully updated free text request handler {} for collection {}..", FREETEXT_REQUEST_HANDLER, collectionName);

                    return;
                } catch (Throwable t) {
                    lastExcp = t;

                    LOG.warn("Error encountered in updating request handler {} for collection {}. Will attempt to create one", FREETEXT_REQUEST_HANDLER, collectionName);
                }

                try {
                    LOG.info("Attempting to create free text request handler {} for collection {}", FREETEXT_REQUEST_HANDLER, collectionName);

                    createFreeTextRequestHandler(solrClient, collectionName, indexFieldName2SearchWeightMap, solrMode);
                    LOG.info("Successfully created free text request handler {} for collection {}", FREETEXT_REQUEST_HANDLER, collectionName);

                    return;
                } catch (Throwable t) {
                    lastExcp = t;

                    LOG.warn("Error encountered in creating request handler {} for collection {}", FREETEXT_REQUEST_HANDLER, collectionName, t);
                }
            }

            String msg = String.format("Error encountered in creating/updating request handler %s for collection %s", FREETEXT_REQUEST_HANDLER, collectionName);

            throw lastExcp != null ? new RuntimeException(msg, lastExcp) : new RuntimeException(msg);
        } finally {
            LOG.debug("Releasing the solr client from usage.");

            Solr6Index.releaseSolrClient(solrClient);
        }
    }


    @Override
    public Map<String, List<AtlasAggregationEntry>> getAggregatedMetrics(AggregationContext aggregationContext) {
        SolrClient solrClient = null;

        try {
            solrClient = Solr6Index.getSolrClient(); // get solr client using same settings as that of Janus Graph

            if (solrClient == null) {
                LOG.warn("The indexing system is not solr based. Will return empty Aggregation metrics.");

                return Collections.EMPTY_MAP;
            }

            Set<String>         aggregationCommonFields = aggregationContext.getAggregationFieldNames();
            Set<AtlasAttribute> aggregationAttributes   = aggregationContext.getAggregationAttributes();
            Map<String, String> indexFieldNameCache     = aggregationContext.getIndexFieldNameCache();

            if (CollectionUtils.isEmpty(aggregationCommonFields)) {
                LOG.warn("There are no fields provided for aggregation purpose.");

                if (CollectionUtils.isEmpty(aggregationAttributes)) {
                    LOG.warn("There are no aggregation fields or attributes are provided. Will return empty metrics.");

                    return Collections.EMPTY_MAP;
                }
            }

            if (CollectionUtils.isEmpty(aggregationAttributes)) {
                LOG.warn("There no attributes provided for aggregation purpose.");
            }

            Map<String, String>   indexFieldName2PropertyKeyNameMap = new HashMap<>();
            AtlasSolrQueryBuilder solrQueryBuilder                  = new AtlasSolrQueryBuilder();

            solrQueryBuilder.withEntityTypes(aggregationContext.getSearchForEntityTypes())
                            .withQueryString(aggregationContext.getQueryString())
                            .withCriteria(aggregationContext.getFilterCriteria())
                            .withExcludedDeletedEntities(aggregationContext.isExcludeDeletedEntities())
                            .withIncludeSubTypes(aggregationContext.isIncludeSubTypes())
                            .withCommonIndexFieldNames(indexFieldNameCache);


            SolrQuery solrQuery      = new SolrQuery();
            String    finalSolrQuery = solrQueryBuilder.build();

            if(LOG.isDebugEnabled()) {
                LOG.debug("Final query string prepared is {}", finalSolrQuery);
            }

            solrQuery.setQuery(finalSolrQuery);
            solrQuery.setRequestHandler(FREETEXT_REQUEST_HANDLER);

            if (CollectionUtils.isNotEmpty(aggregationCommonFields)) {
                for (String propertyName : aggregationCommonFields) {
                    // resolve index field names for aggregation fields.
                    String indexFieldName = indexFieldNameCache.get(propertyName);

                    indexFieldName2PropertyKeyNameMap.put(indexFieldName, propertyName);

                    solrQuery.addFacetField(indexFieldName);
                }
            }

            if (CollectionUtils.isNotEmpty(aggregationAttributes)) {
                for (AtlasAttribute attribute : aggregationAttributes) {
                    String indexFieldName = attribute.getIndexFieldName();

                    indexFieldName2PropertyKeyNameMap.put(indexFieldName, attribute.getQualifiedName());

                    solrQuery.addFacetField(indexFieldName);
                }
            }

            solrQuery.setFacetMinCount(MIN_FACET_COUNT_REQUIRED);

            QueryResponse    queryResponse = solrClient.query(VERTEX_INDEX, solrQuery, SolrRequest.METHOD.POST);
            List<FacetField> facetFields   = queryResponse == null ? null : queryResponse.getFacetFields();

            if (CollectionUtils.isNotEmpty(facetFields)) {
                Map<String, List<AtlasAggregationEntry>> ret = new HashMap<>();

                for (FacetField facetField : facetFields) {
                    String                      indexFieldName = facetField.getName();
                    List<AtlasAggregationEntry> entries        = new ArrayList<>(facetField.getValueCount());
                    List<FacetField.Count>      values         = facetField.getValues();

                    for (FacetField.Count count : values) {
                        entries.add(new AtlasAggregationEntry(count.getName(), count.getCount()));
                    }

                    //get the original propertyName from the index field name.
                    String propertyKeyName = indexFieldName2PropertyKeyNameMap.get(indexFieldName);

                    ret.put(propertyKeyName, entries);
                }

                return ret;
            }
        } catch (Exception e) {
            LOG.error("Error encountered in getting the aggregation metrics. Will return empty aggregation.", e);
        } finally {
            Solr6Index.releaseSolrClient(solrClient);
        }

        return Collections.EMPTY_MAP;
    }

    @Override
    public void applySuggestionFields(String collectionName, List<String> suggestionProperties) {
        SolrClient solrClient = null;

        try {
            solrClient = Solr6Index.getSolrClient(); // get solr client using same settings as that of Janus Graph

            if (solrClient == null) {
                LOG.warn("The indexing system is not solr based. Suggestions feature will not be available.");

                return;
            }

            Solr6Index.Mode solrMode = Solr6Index.getSolrMode();

            //update the request handler
            performRequestHandlerAction(collectionName,
                                        solrClient,
                                        generatePayLoadForSuggestions(generateSuggestionsString(suggestionProperties)),
                                        solrMode);
        } catch (Throwable t) {
            String msg = String.format("Error encountered in creating the request handler '%s' for collection '%s'", Constants.TERMS_REQUEST_HANDLER, collectionName);

            LOG.error(msg, t);
        } finally {
            Solr6Index.releaseSolrClient(solrClient);
        }

        LOG.info("Applied suggestion fields request handler for collection {}.", collectionName);
    }

    @Override
    public List<String> getSuggestions(String prefixString, String indexFieldName) {
        SolrClient solrClient = null;

        try {
            solrClient = Solr6Index.getSolrClient(); // get solr client using same settings as that of Janus Graph

            if (solrClient == null) {
                LOG.warn("The indexing system is not solr based. Suggestions feature will not be available.");

                return Collections.EMPTY_LIST;
            }

            SolrQuery solrQuery = new SolrQuery();

            solrQuery.setRequestHandler(Constants.TERMS_REQUEST_HANDLER)
                     .setParam(TERMS_PREFIX, prefixString)
                     .setParam(CommonParams.OMIT_HEADER, true);

            if (StringUtils.isNotEmpty(indexFieldName)) {
                solrQuery.setParam(TERMS_FIELD, indexFieldName);
            }

            QueryResponse queryResponse = solrClient.query(VERTEX_INDEX, solrQuery);
            TermsResponse termsResponse = queryResponse == null? null: queryResponse.getTermsResponse();

            if(termsResponse == null) {
                LOG.info("Received null for terms response. Will return no suggestions.");

                return Collections.EMPTY_LIST;
            }

            Map<String, TermFreq> termsMap = new HashMap<>();

            for (List<TermsResponse.Term> fieldTerms : termsResponse.getTermMap().values()) {
                for (TermsResponse.Term fieldTerm : fieldTerms) {
                    TermFreq term = termsMap.get(fieldTerm.getTerm());

                    if (term == null) {
                        term = new TermFreq(fieldTerm.getTerm(), fieldTerm.getFrequency());

                        termsMap.put(term.getTerm(), term);
                    } else {
                        term.addFreq(fieldTerm.getFrequency());
                    }
                }
            }

            return getTopTerms(termsMap);
        } catch (SolrServerException | IOException e) {
            String msg = String.format("Error encountered in generating the suggestions. Ignoring the error", e);

            LOG.error(msg);
        } finally {
            Solr6Index.releaseSolrClient(solrClient);
        }

        return Collections.EMPTY_LIST;
    }

    private boolean isSolrHealthy() throws SolrServerException, IOException {
        SolrClient client = Solr6Index.getSolrClient();

        return client != null && client.ping(Constants.VERTEX_INDEX).getStatus() == SOLR_HEALTHY_STATUS;
    }

    private boolean isElasticsearchHealthy() throws IOException {
        ElasticSearchClient client           = ElasticSearch7Index.getElasticSearchClient();
        String              janusVertexIndex = ApplicationProperties.DEFAULT_INDEX_NAME + "_" + Constants.VERTEX_INDEX;

        return client != null && client.indexExists(janusVertexIndex);
    }

    private void graphManagementCommit(AtlasGraphManagement management) {
        try {
            management.commit();
        } catch (Exception ex) {
            LOG.warn("Graph transaction management commit failed; attempting rollback: {}", ex);

            graphManagementRollback(management);
        }
    }

    private void graphManagementRollback(AtlasGraphManagement management) {
        try {
            management.rollback();
        } catch (Exception ex) {
            LOG.warn("Graph transaction management rollback failed: {}", ex);
        }
    }

    @VisibleForTesting
    static List<String> getTopTerms(Map<String, TermFreq> termsMap) {
        final List<String> ret;

        if (MapUtils.isNotEmpty(termsMap)) {
            // Collect top high frequency terms.
            PriorityQueue<TermFreq> termsQueue = new PriorityQueue(termsMap.size(), FREQ_COMPARATOR);

            for (TermFreq term : termsMap.values()) {
                termsQueue.add(term);
            }

            ret = new ArrayList<>(DEFAULT_SUGGESTION_COUNT);

            while (!termsQueue.isEmpty()) {
                ret.add(termsQueue.poll().getTerm());

                if (ret.size() >= DEFAULT_SUGGESTION_COUNT) {
                    break;
                }
            }
        } else {
            ret = Collections.EMPTY_LIST;
        }

        return ret;
    }

    @VisibleForTesting
    static String generatePayLoadForFreeText(String action, String qfValue) {
        return String.format("{" +
                " %s :  { " +
                "       'name' : '%s', " +
                "       'class': 'solr.SearchHandler' , " +
                "       'defaults': " + "{" +
                "          'defType': 'edismax' , " +
                "          'rows':    100 , " +
                "          'lowercaseOperators': true , " +
                "          'qf': '%s' , " +
                "          'hl.fl': '*' , " +
                "          'hl.requireFieldMatch': true , " +
                "          'lowercaseOperators': true , " +
                "         }" +
                "    }" +
                "}", action, FREETEXT_REQUEST_HANDLER, qfValue);
    }

    @VisibleForTesting
    String generatePayLoadForSuggestions(String suggestionFieldsString) {
        return String.format("{\n" +
                " update-requesthandler :  { \n" +
                "       'name' :    '%s', \n" +
                "       'class':    'solr.SearchHandler' , \n" +
                "       'startup':  'lazy' ,\n" +
                "       'defaults': " + "{ \n" +
                "          'terms':       true , \n" +
                "          'distrib':     false , \n" +
                "          'terms.limit': 5 , \n" +
                "           'terms.fl'  : \n" +
                "              [\n" +
                "              %s \n" +
                "           ] \n" +
                "         }\n" +
                "       'components': " + "[ \n" +
                "           'terms' \n" +
                "        ] \n" +
                "    } \n" +
                "}", Constants.TERMS_REQUEST_HANDLER, suggestionFieldsString);

    }

    @VisibleForTesting
    protected static String generateSearchWeightString(Map<String, Integer> indexFieldName2SearchWeightMap) {
        StringBuilder searchWeightBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : indexFieldName2SearchWeightMap.entrySet()) {
            searchWeightBuilder.append(" ")
                               .append(entry.getKey())
                               .append("^")
                               .append(entry.getValue().intValue());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("generateSearchWeightString(fieldsCount={}): ret={}", indexFieldName2SearchWeightMap.size(), searchWeightBuilder.toString());
        }

        return searchWeightBuilder.toString();
    }

    @VisibleForTesting
    protected static String generateSuggestionsString(List<String> suggestionIndexFieldNames) {
        StringBuilder    ret      = new StringBuilder();
        Iterator<String> iterator = suggestionIndexFieldNames.iterator();

        while(iterator.hasNext()) {
            ret.append("'").append(iterator.next()).append("'");

            if(iterator.hasNext()) {
                ret.append(", ");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("generateSuggestionsString(fieldsCount={}): ret={}", suggestionIndexFieldNames.size(), ret.toString());
        }

        return ret.toString();
    }

    private SolrResponse updateFreeTextRequestHandler(SolrClient solrClient, String collectionName,
                                                    Map<String, Integer> indexFieldName2SearchWeightMap,
                                                    Solr6Index.Mode mode) throws IOException, SolrServerException, AtlasBaseException {
        String searchWeightString = generateSearchWeightString(indexFieldName2SearchWeightMap);
        String payLoadString      = generatePayLoadForFreeText("update-requesthandler", searchWeightString);

        return performRequestHandlerAction(collectionName, solrClient, payLoadString, mode);
    }

    private SolrResponse createFreeTextRequestHandler(SolrClient solrClient, String collectionName,
                                                    Map<String, Integer> indexFieldName2SearchWeightMap,
                                                    Solr6Index.Mode mode) throws IOException, SolrServerException, AtlasBaseException {
        String searchWeightString = generateSearchWeightString(indexFieldName2SearchWeightMap);
        String payLoadString      = generatePayLoadForFreeText("create-requesthandler", searchWeightString);

        return performRequestHandlerAction(collectionName, solrClient, payLoadString, mode);
    }

    private SolrResponse performRequestHandlerAction(String collectionName,
                                                   SolrClient solrClient,
                                                   String actionPayLoad,
                                                   Solr6Index.Mode mode) throws IOException, SolrServerException, AtlasBaseException {
        switch (mode) {
            case CLOUD:
                V2Request v2request = new V2Request.Builder(String.format("/collections/%s/config", collectionName))
                                                            .withMethod(SolrRequest.METHOD.POST)
                                                            .withPayload(actionPayLoad)
                                                            .build();

                return validateResponseForSuccess(v2request.process(solrClient));

            case HTTP:
                GenericSolrRequest          request       = new GenericSolrRequest(SolrRequest.METHOD.POST, String.format("/%s/config", collectionName), null);
                RequestWriter.ContentWriter contentWriter = new RequestWriter.StringPayloadContentWriter(actionPayLoad, "application/json; charset=UTF-8");

                request.setContentWriter(contentWriter);
                request.setUseV2(false);

                return validateResponseForSuccess(request.process(solrClient));

            default:
                throw new IllegalArgumentException("Unsupported Solr operation mode: " + mode);
        }
    }

    private SolrResponse validateResponseForSuccess(SolrResponse solrResponse) throws AtlasBaseException {
        if(solrResponse == null) {
            String msg = "Received null response .";

            LOG.error(msg);

            throw new AtlasBaseException(msg);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("V2 Response is {}", solrResponse.toString());
        }

        NamedList<Object> response = solrResponse.getResponse();

        if(response != null) {
            Object errorMessages = response.get("errorMessages");

            if(errorMessages != null) {
                LOG.error("Error encountered in performing request handler create/update");

                List<Object>        errorObjects = (List<Object>) errorMessages;
                Map<Object, Object> errObject    = (Map<Object, Object>) errorObjects.get(0);
                List<String>        msgs         = (List<String>) errObject.get("errorMessages");
                StringBuilder       sb           = new StringBuilder();

                for(String msg: msgs) {
                    sb.append(msg);
                }

                String errors = sb.toString();
                String msg    = String.format("Error encountered in performing response handler action. %s.", errors);

                LOG.error(msg);

                throw new AtlasBaseException(msg);
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Successfully performed response handler action. V2 Response is {}", solrResponse.toString());
                }
            }

        } else {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Did not receive any response from SOLR.");
            }
        }

        return solrResponse;
    }

        static final class TermFreq {
        private final String term;
        private       long   freq;

        public TermFreq(String term, long freq) {
            this.term = term;
            this.freq = freq;
        }

        public final String getTerm() { return term; }

        public final long getFreq() { return freq; }

        public final void addFreq(long val) { freq += val; }
   }

    static class FreqComparator implements Comparator<TermFreq> {
        @Override
        public int compare(TermFreq lhs, TermFreq rhs) {
            return Long.compare(rhs.getFreq(), lhs.getFreq());
        }
    }
}
