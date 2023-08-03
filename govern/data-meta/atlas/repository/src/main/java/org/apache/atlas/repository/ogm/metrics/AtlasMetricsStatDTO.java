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
package org.apache.atlas.repository.ogm.metrics;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.metrics.AtlasMetrics;
import org.apache.atlas.model.metrics.AtlasMetricsStat;
import org.apache.atlas.repository.impexp.AuditsWriter;
import org.apache.atlas.repository.ogm.AbstractDataTransferObject;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.model.metrics.AtlasMetricsStat.METRICS_ID_PREFIX_PROPERTY;


/**
 * AtlasMetricsStatDTO is the bridge class in between AtlasMetricsStat and AtlasEntity.
 */
@Component
public class AtlasMetricsStatDTO extends AbstractDataTransferObject<AtlasMetricsStat> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasMetricsStatDTO.class);

    public static final String METRICS_ENTITY_TYPE_NAME  = "__AtlasMetricsStat";
    public static final String METRICS_ID_PROPERTY       = "metricsId";
    private static final String METRICS_PROPERTY         = "metrics";
    private static final String COLLECTION_TIME_PROPERTY = "collectionTime";
    private static final String TIME_TO_LIVE_PROPERTY    = "timeToLiveMillis";
    private static final String UNIQUE_NAME_PROPERTY     = "uniqueName";

    @Inject
    public AtlasMetricsStatDTO(AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasMetricsStat.class, METRICS_ENTITY_TYPE_NAME);
    }

    @Override
    public AtlasMetricsStat from(AtlasEntity entity) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasMetricsStatDTO.from({})", entity);
        }

        AtlasMetricsStat metricsStat = null;

        String jsonMetrics = (String) entity.getAttribute(METRICS_PROPERTY);

        if (StringUtils.isNotEmpty(jsonMetrics)) {
            metricsStat = new AtlasMetricsStat(AtlasType.fromJson(jsonMetrics, AtlasMetrics.class));
        }

        if (metricsStat == null) {
            LOG.error("MetricStat cannot be created without metric info. Null has been returned.");
        } else {
            metricsStat.setGuid(entity.getGuid());
            metricsStat.setMetricsId((String) entity.getAttribute(METRICS_ID_PROPERTY));

            metricsStat.setCollectionTime((long) entity.getAttribute(COLLECTION_TIME_PROPERTY));

            metricsStat.setTimeToLiveMillis((long) entity.getAttribute(TIME_TO_LIVE_PROPERTY));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasMetricsStatDTO.from() : {}", metricsStat);
        }

        return metricsStat;
    }

    @Override
    public AtlasMetricsStat from(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasMetricsStatDTO.from({})", entityWithExtInfo);
        }

        AtlasMetricsStat ret = from(entityWithExtInfo.getEntity());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasMetricsStatDTO.from() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity toEntity(AtlasMetricsStat obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasMetricsStatDTO.toEntity({})", obj);
        }

        AtlasEntity entity = getDefaultAtlasEntity(obj);

        entity.setAttribute(METRICS_ID_PROPERTY, getUniqueValue(obj));

        if (obj.getMetrics() != null) {
            entity.setAttribute(METRICS_PROPERTY, AtlasType.toJson(obj.getMetrics()));
        }

        entity.setAttribute(COLLECTION_TIME_PROPERTY, obj.getCollectionTime());
        entity.setAttribute(TIME_TO_LIVE_PROPERTY, obj.getTimeToLiveMillis());
        entity.setAttribute(UNIQUE_NAME_PROPERTY, getUniqueValue(obj));

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasMetricsStatDTO.toEntity() : {}", entity);
        }
        return entity;
    }

    @Override
    public AtlasEntityWithExtInfo toEntityWithExtInfo(AtlasMetricsStat obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasMetricsStatDTO.toEntityWithExtInfo({})", obj);
        }
        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(toEntity(obj));

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasMetricsStatDTO.toEntityWithExtInfo() : {}", ret);
        }
        return ret;
    }

    @Override
    public Map<String, Object> getUniqueAttributes(AtlasMetricsStat obj) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(METRICS_ID_PROPERTY, getUniqueValue(obj));
        return ret;
    }

    private String getUniqueValue(AtlasMetricsStat obj) {
        return METRICS_ID_PREFIX_PROPERTY + obj.getCollectionTime() + "@" + AuditsWriter.getCurrentClusterName();
    }
}
