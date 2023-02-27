/**
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
package org.apache.atlas.query.executors;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AttributeSearchResult;
import org.apache.atlas.query.GremlinQuery;
import org.apache.atlas.query.SelectClauseComposer;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectClauseProjections {
    private static final Logger LOG = LoggerFactory.getLogger(SelectClauseProjections.class);

    public static AtlasSearchResult usingList(final GremlinQuery queryInfo,
                                             final EntityGraphRetriever entityRetriever,
                                             final Collection<AtlasVertex> resultList) throws AtlasBaseException {
        AtlasSearchResult     ret                   = new AtlasSearchResult();
        SelectClauseComposer  selectClauseInfo      = queryInfo.getSelectComposer();
        AttributeSearchResult attributeSearchResult = new AttributeSearchResult();

        attributeSearchResult.setName(Arrays.stream(selectClauseInfo.getLabels()).collect(Collectors.toList()));

        Collection<List<Object>> values = getProjectionRows(resultList, selectClauseInfo, entityRetriever);

        if (values instanceof List) {
            attributeSearchResult.setValues((List) values);
        } else if (values instanceof Set) {
            attributeSearchResult.setValues(new ArrayList<>(values));
        }

        ret.setAttributes(attributeSearchResult);

        return ret;
    }

    public static AtlasSearchResult usingMap(final GremlinQuery gremlinQuery,
                                             final EntityGraphRetriever entityRetriever,
                                             final Map<String, Collection<AtlasVertex>> resultMap) throws AtlasBaseException {
        AtlasSearchResult     ret                   = new AtlasSearchResult();
        SelectClauseComposer  selectClauseInfo      = gremlinQuery.getSelectComposer();
        AttributeSearchResult attributeSearchResult = new AttributeSearchResult();

        attributeSearchResult.setName(Arrays.stream(selectClauseInfo.getLabels()).collect(Collectors.toList()));

        List<List<Object>> values = new ArrayList<>();

        for (Collection<AtlasVertex> value : resultMap.values()) {
            Collection<List<Object>> projectionRows = getProjectionRows(value, selectClauseInfo, entityRetriever);

            values.addAll(projectionRows);
        }

        attributeSearchResult.setValues(getSublistForGroupBy(gremlinQuery, values));
        ret.setAttributes(attributeSearchResult);

        return ret;
    }

    private static List<List<Object>> getSublistForGroupBy(GremlinQuery gremlinQuery, List<List<Object>> values) {
        int startIndex = gremlinQuery.getQueryMetadata().getResolvedOffset() - 1;

        if (startIndex < 0) {
            startIndex = 0;
        }

        int endIndex = startIndex + gremlinQuery.getQueryMetadata().getResolvedLimit();

        if (startIndex >= values.size()) {
            endIndex   = 0;
            startIndex = 0;
        }

        if (endIndex >= values.size()) {
            endIndex = values.size();
        }

        return values.subList(startIndex, endIndex);
    }

    private static Collection<List<Object>> getProjectionRows(final Collection<AtlasVertex> vertices,
                                                              final SelectClauseComposer selectClauseComposer,
                                                              final EntityGraphRetriever entityRetriever) throws AtlasBaseException {
        Collection<List<Object>> values = new HashSet<>();

        for (AtlasVertex vertex : vertices) {
            List<Object> row = new ArrayList<>();

            for (int idx = 0; idx < selectClauseComposer.getLabels().length; idx++) {
                if (selectClauseComposer.isMinIdx(idx)) {
                    row.add(computeMin(vertices, selectClauseComposer, idx));
                } else if (selectClauseComposer.isMaxIdx(idx)) {
                    row.add(computeMax(vertices, selectClauseComposer, idx));
                } else if (selectClauseComposer.isCountIdx(idx)) {
                    row.add(vertices.size());
                } else if (selectClauseComposer.isSumIdx(idx)) {
                    row.add(computeSum(vertices, selectClauseComposer, idx));
                } else {
                    if (selectClauseComposer.isPrimitiveAttribute(idx)) {
                        String propertyName = selectClauseComposer.getAttribute(idx);
                        Object value = vertex.getProperty(propertyName, Object.class);
                        row.add(value != null ? value : StringUtils.EMPTY);
                    } else {
                        row.add(entityRetriever.toAtlasEntityHeaderWithClassifications(vertex));
                    }
                }
            }

            values.add(row);
        }

        return values;
    }

    private static Number computeSum(final Collection<AtlasVertex> vertices, final SelectClauseComposer selectClauseComposer, final int idx) {
        if (!selectClauseComposer.isNumericAggregator(idx)) {
            return 0;
        }

        final String propertyName = selectClauseComposer.getAttribute(idx);
        double       sum          = 0;

        for (AtlasVertex vertex : vertices) {
            Number value = vertex.getProperty(propertyName, Number.class);

            if (value != null) {
                sum += value.doubleValue();
            } else {
                LOG.warn("Property: {} for vertex: {} not found!", propertyName, vertex.getId());
            }
        }

        return sum;
    }

    private static Object computeMax(final Collection<AtlasVertex> vertices, final SelectClauseComposer selectClauseComposer, final int idx) {
        final String propertyName = selectClauseComposer.getAttribute(idx);

        if (selectClauseComposer.isNumericAggregator(idx)) {
            AtlasVertex maxV = Collections.max(vertices, new VertexPropertyComparator(propertyName));

            return maxV.getProperty(propertyName, Object.class);
        } else {
            return Collections.max(vertices.stream().map(v -> v.getProperty(propertyName, String.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()), String.CASE_INSENSITIVE_ORDER);
        }
    }

    private static Object computeMin(final Collection<AtlasVertex> vertices, final SelectClauseComposer selectClauseComposer, final int idx) {
        final String propertyName = selectClauseComposer.getAttribute(idx);

        if (selectClauseComposer.isNumericAggregator(idx)) {
            AtlasVertex minV = Collections.min(vertices, new VertexPropertyComparator(propertyName));

            return minV.getProperty(propertyName, Object.class);
        } else {
            return Collections.min(vertices.stream()
                    .map(v -> v.getProperty(propertyName, String.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()), String.CASE_INSENSITIVE_ORDER);
        }
    }

    static class VertexPropertyComparator implements Comparator<AtlasVertex> {
        private String propertyName;

        public VertexPropertyComparator(final String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public int compare(final AtlasVertex o1, final AtlasVertex o2) {
            Object p1 = o1 == null ? null : o1.getProperty(propertyName, Object.class);
            Object p2 = o2 == null ? null : o2.getProperty(propertyName, Object.class);

            if (p1 == null && p2 == null) {
                return 0;
            } else  if (p1 == null) {
                return -1;
            } else if (p2 == null) {
                return 1;
            } else if (p1 instanceof String) {
                return (p2 instanceof String) ? ((String) p1).compareTo((String) p2) : 0;
            } else if (p1 instanceof Integer) {
                return (p2 instanceof Integer) ? ((Integer) p1).compareTo((Integer) p2) : 0;
            } else if (p1 instanceof Long) {
                return (p2 instanceof Long) ? ((Long) p1).compareTo((Long) p2) : 0;
            } else if (p1 instanceof Short) {
                return (p2 instanceof Short) ? ((Short) p1).compareTo((Short) p2) : 0;
            } else if (p1 instanceof Float) {
                return (p2 instanceof Float) ? ((Float) p1).compareTo((Float) p2) : 0;
            } else if (p1 instanceof Double) {
                return (p2 instanceof Double) ? ((Double) p1).compareTo((Double) p2) : 0;
            } else if (p1 instanceof Byte) {
                return (p2 instanceof Byte) ? ((Byte) p1).compareTo((Byte) p2) : 0;
            } else if (p1 instanceof BigInteger) {
                return (p2 instanceof BigInteger) ? ((BigInteger) p1).compareTo((BigInteger) p2) : 0;
            } else if (p1 instanceof BigDecimal) {
                return (p2 instanceof BigDecimal) ? ((BigDecimal) p1).compareTo((BigDecimal) p2) : 0;
            }

            return 0;
        }
    }
}
