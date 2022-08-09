/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.audit;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.service.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract base class should be used when adding support for an audit storage backend.
 */
public abstract class AbstractStorageBasedAuditRepository implements Service, EntityAuditRepository, ActiveStateChangeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseBasedAuditRepository.class);

  private   static final String AUDIT_REPOSITORY_MAX_SIZE_PROPERTY = "atlas.hbase.client.keyvalue.maxsize";
  private   static final String AUDIT_EXCLUDE_ATTRIBUTE_PROPERTY   = "atlas.audit.hbase.entity";
  private   static final long   ATLAS_HBASE_KEYVALUE_DEFAULT_SIZE  = 1024 * 1024;
  public    static final String CONFIG_PREFIX                      = "atlas.audit";
  public    static final String CONFIG_PERSIST_ENTITY_DEFINITION   = CONFIG_PREFIX + ".persistEntityDefinition";
  protected static final String FIELD_SEPARATOR                    = ":";

  protected static Configuration      APPLICATION_PROPERTIES       = null;
  protected Map<String, List<String>> auditExcludedAttributesCache = new HashMap<>();
  protected static boolean            persistEntityDefinition;

  static {
    try {
      persistEntityDefinition = ApplicationProperties.get().getBoolean(CONFIG_PERSIST_ENTITY_DEFINITION, false);
    } catch (AtlasException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void instanceIsActive() throws AtlasException { LOG.info("Reacting to active: No action for now."); }

  @Override
  public void instanceIsPassive() {
    LOG.info("Reacting to passive: No action for now.");
  }

  @Override
  public int getHandlerOrder() {
    return HandlerOrder.AUDIT_REPOSITORY.getOrder();
  }

  @Override
  public void putEventsV1(EntityAuditEvent... events) throws AtlasException {
    putEventsV1(Arrays.asList(events));
  }

  @Override
  public void putEventsV2(EntityAuditEventV2... events) throws AtlasBaseException {
    putEventsV2(Arrays.asList(events));
  }

  @Override
  public List<Object> listEvents(String entityId, String startKey, short maxResults) throws AtlasBaseException {
    List ret = listEventsV2(entityId, null, startKey, maxResults);

    try {
      if (CollectionUtils.isEmpty(ret)) {
        ret = listEventsV1(entityId, startKey, maxResults);
      }
    } catch (AtlasException e) {
      throw new AtlasBaseException(e);
    }

    return ret;
  }

  @Override
  public long repositoryMaxSize() {
    long ret;
    initApplicationProperties();

    if (APPLICATION_PROPERTIES == null) {
      ret = ATLAS_HBASE_KEYVALUE_DEFAULT_SIZE;
    } else {
      ret = APPLICATION_PROPERTIES.getLong(AUDIT_REPOSITORY_MAX_SIZE_PROPERTY, ATLAS_HBASE_KEYVALUE_DEFAULT_SIZE);
    }

    return ret;
  }

  @Override
  public List<String> getAuditExcludeAttributes(String entityType) {
    List<String> ret = null;

    initApplicationProperties();

    if (auditExcludedAttributesCache.containsKey(entityType)) {
      ret = auditExcludedAttributesCache.get(entityType);
    } else if (APPLICATION_PROPERTIES != null) {
      String[] excludeAttributes = APPLICATION_PROPERTIES.getStringArray(AUDIT_EXCLUDE_ATTRIBUTE_PROPERTY + "." +
          entityType + "." +  "attributes.exclude");

      if (excludeAttributes != null) {
        ret = Arrays.asList(excludeAttributes);
      }

      auditExcludedAttributesCache.put(entityType, ret);
    }

    return ret;
  }

  protected void initApplicationProperties() {
    if (APPLICATION_PROPERTIES == null) {
      try {
        APPLICATION_PROPERTIES = ApplicationProperties.get();
      } catch (AtlasException ex) {
        // ignore
      }
    }
  }

  /**
   * Only should be used to initialize Application properties for testing.
   *
   * @param config
   */
  @VisibleForTesting
  protected void setApplicationProperties(Configuration config) {
    APPLICATION_PROPERTIES = config;
  }

  protected byte[] getKey(String id, Long ts, int index) {
    assert id != null  : "entity id can't be null";
    assert ts != null  : "timestamp can't be null";
    String keyStr = id + FIELD_SEPARATOR + ts + FIELD_SEPARATOR + index + FIELD_SEPARATOR + System.currentTimeMillis();
    return Bytes.toBytes(keyStr);
  }

}
