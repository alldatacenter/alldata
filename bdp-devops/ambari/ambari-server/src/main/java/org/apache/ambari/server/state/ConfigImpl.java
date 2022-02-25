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

package org.apache.ambari.server.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

import javax.annotation.Nullable;

import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;

public class ConfigImpl implements Config {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(ConfigImpl.class);

  /**
   * A label for {@link #propertyLock} to use with the {@link LockFactory}.
   */
  private static final String PROPERTY_LOCK_LABEL = "configurationPropertyLock";

  public static final String GENERATED_TAG_PREFIX = "generatedTag_";

  private final long configId;
  private final Cluster cluster;
  private final StackId stackId;
  private final String type;
  private final String tag;
  private final Long version;

  /**
   * The properties of this configuration. This cannot be a
   * {@link ConcurrentMap} since we allow null values. Therefore, it must be
   * synchronized externally.
   */
  private Map<String, String> properties;

  /**
   * A lock for reading/writing of {@link #properties} concurrently.
   *
   * @see #properties
   */
  private final ReadWriteLock propertyLock;

  /**
   * The property attributes for this configuration.
   */
  private Map<String, Map<String, String>> propertiesAttributes;

  private Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes;

  private final ClusterDAO clusterDAO;

  private final Gson gson;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  private final AmbariEventPublisher eventPublisher;

  @AssistedInject
  ConfigImpl(@Assisted Cluster cluster, @Assisted("type") String type,
             @Assisted("tag") @Nullable String tag,
             @Assisted Map<String, String> properties,
             @Assisted @Nullable Map<String, Map<String, String>> propertiesAttributes,
             ClusterDAO clusterDAO, StackDAO stackDAO,
             Gson gson, AmbariEventPublisher eventPublisher, LockFactory lockFactory,
             @Named("ConfigPropertiesEncryptor") Encryptor<Config> configPropertiesEncryptor) {
    this(cluster.getDesiredStackVersion(), cluster, type, tag, properties, propertiesAttributes,
        clusterDAO, stackDAO, gson, eventPublisher, lockFactory, configPropertiesEncryptor);
  }


  @AssistedInject
  ConfigImpl(@Assisted @Nullable StackId stackId, @Assisted Cluster cluster, @Assisted("type") String type,
             @Assisted("tag") @Nullable String tag,
             @Assisted Map<String, String> properties,
             @Assisted @Nullable Map<String, Map<String, String>> propertiesAttributes,
             ClusterDAO clusterDAO, StackDAO stackDAO,
             Gson gson, AmbariEventPublisher eventPublisher, LockFactory lockFactory,
             @Named("ConfigPropertiesEncryptor") Encryptor<Config> configPropertiesEncryptor) {

    propertyLock = lockFactory.newReadWriteLock(PROPERTY_LOCK_LABEL);

    this.cluster = cluster;
    this.type = type;
    this.properties = properties;
    configPropertiesEncryptor.encryptSensitiveData(this);

    // only set this if it's non-null
    this.propertiesAttributes = null == propertiesAttributes ? null
        : new HashMap<>(propertiesAttributes);

    this.clusterDAO = clusterDAO;
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
    this.eventPublisher = eventPublisher;
    version = cluster.getNextConfigVersion(type);

    // tag is nullable from factory but not in the DB, so ensure we generate something
    tag = StringUtils.isBlank(tag) ? UUID.randomUUID().toString() : tag;
    this.tag = tag;

    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    ClusterConfigEntity entity = new ClusterConfigEntity();
    entity.setClusterEntity(clusterEntity);
    entity.setClusterId(cluster.getClusterId());
    entity.setType(type);
    entity.setVersion(version);
    entity.setTag(this.tag);
    entity.setTimestamp(System.currentTimeMillis());
    entity.setStack(stackEntity);
    entity.setData(this.gson.toJson(this.properties));

    if (null != propertiesAttributes) {
      entity.setAttributes(this.gson.toJson(propertiesAttributes));
    }

    // when creating a brand new config without a backing entity, use the
    // cluster's desired stack as the config's stack
    this.stackId = stackId;
    propertiesTypes = cluster.getConfigPropertiesTypes(type);
    persist(entity);

    configId = entity.getConfigId();
    configPropertiesEncryptor.decryptSensitiveData(this);
  }

  @AssistedInject
  ConfigImpl(@Assisted Cluster cluster, @Assisted ClusterConfigEntity entity,
      ClusterDAO clusterDAO, Gson gson, AmbariEventPublisher eventPublisher,
      LockFactory lockFactory,  @Named("ConfigPropertiesEncryptor") Encryptor<Config> configPropertiesEncryptor) {
    propertyLock = lockFactory.newReadWriteLock(PROPERTY_LOCK_LABEL);

    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.gson = gson;
    this.eventPublisher = eventPublisher;
    configId = entity.getConfigId();

    type = entity.getType();
    tag = entity.getTag();
    version = entity.getVersion();

    // when using an existing entity, use the actual value of the entity's stack
    stackId = new StackId(entity.getStack());

    propertiesTypes = cluster.getConfigPropertiesTypes(type);

    // incur the hit on deserialization since this business object is stored locally
    try {
      Map<String, String> deserializedProperties = gson.<Map<String, String>> fromJson(
          entity.getData(), Map.class);

      if (null == deserializedProperties) {
        deserializedProperties = new HashMap<>();
      }

      properties = deserializedProperties;
      configPropertiesEncryptor.decryptSensitiveData(this);
    } catch (JsonSyntaxException e) {
      LOG.error("Malformed configuration JSON stored in the database for {}/{}", entity.getType(),
          entity.getTag());
    }

    // incur the hit on deserialization since this business object is stored locally
    try {
      Map<String, Map<String, String>> deserializedAttributes = gson.<Map<String, Map<String, String>>> fromJson(
          entity.getAttributes(), Map.class);

      if (null != deserializedAttributes) {
        propertiesAttributes = new HashMap<>(deserializedAttributes);
      }
    } catch (JsonSyntaxException e) {
      LOG.error("Malformed configuration attribute JSON stored in the database for {}/{}",
          entity.getType(), entity.getTag());
    }
  }

  /**
   * Constructor. This will create an instance suitable only for
   * representation/serialization as it is incomplete.
   *
   * @param type
   * @param tag
   * @param properties
   * @param propertiesAttributes
   * @param clusterDAO
   * @param gson
   * @param eventPublisher
   */
  @AssistedInject
  ConfigImpl(@Assisted("type") String type,
      @Assisted("tag") @Nullable String tag,
      @Assisted Map<String, String> properties,
      @Assisted @Nullable Map<String, Map<String, String>> propertiesAttributes, ClusterDAO clusterDAO,
      Gson gson, AmbariEventPublisher eventPublisher, LockFactory lockFactory) {

    propertyLock = lockFactory.newReadWriteLock(PROPERTY_LOCK_LABEL);

    this.tag = tag;
    this.type = type;
    this.properties = new HashMap<>(properties);
    this.propertiesAttributes = null == propertiesAttributes ? null
        : new HashMap<>(propertiesAttributes);
    this.clusterDAO = clusterDAO;
    this.gson = gson;
    this.eventPublisher = eventPublisher;

    cluster = null;
    configId = 0;
    version = 0L;
    stackId = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StackId getStackId() {
    return stackId;
  }

  @Override
  public Map<PropertyInfo.PropertyType, Set<String>> getPropertiesTypes() {
    return propertiesTypes;
  }

  @Override
  public void setPropertiesTypes(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes) {
    this.propertiesTypes = propertiesTypes;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getTag() {
    return tag;
  }

  @Override
  public Long getVersion() {
    return version;
  }

  @Override
  public Map<String, String> getProperties() {
    propertyLock.readLock().lock();
    try {
      return properties == null ? new HashMap<>() : new HashMap<>(properties);
    } finally {
      propertyLock.readLock().unlock();
    }
  }

  @Override
  public Map<String, Map<String, String>> getPropertiesAttributes() {
    return null == propertiesAttributes ? null
        : new HashMap<>(propertiesAttributes);
  }

  @Override
  public void setProperties(Map<String, String> properties) {
    propertyLock.writeLock().lock();
    try {
      this.properties = properties;
    } finally {
      propertyLock.writeLock().unlock();
    }
  }

  @Override
  public void setPropertiesAttributes(Map<String, Map<String, String>> propertiesAttributes) {
    this.propertiesAttributes = propertiesAttributes;
  }

  @Override
  public void updateProperties(Map<String, String> propertiesToUpdate) {
    propertyLock.writeLock().lock();
    try {
      properties.putAll(propertiesToUpdate);
    } finally {
      propertyLock.writeLock().unlock();
    }
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  @Override
  public List<Long> getServiceConfigVersions() {
    return serviceConfigDAO.getServiceConfigVersionsByConfig(cluster.getClusterId(), type, version);
  }

  @Override
  public void deleteProperties(List<String> propertyKeysToRemove) {
    propertyLock.writeLock().lock();
    try {
      Set<String> keySet = properties.keySet();
      keySet.removeAll(propertyKeysToRemove);
    } finally {
      propertyLock.writeLock().unlock();
    }
  }

  /**
   * Persist the entity and update the internal state relationships once the
   * transaction has been committed.
   */
  private void persist(ClusterConfigEntity entity) {
    persistEntitiesInTransaction(entity);

    // ensure that the in-memory state of the cluster is kept consistent
    cluster.addConfig(this);

    // re-load the entity associations for the cluster
    cluster.refresh();

    // broadcast the change event for the configuration
    ClusterConfigChangedEvent event = new ClusterConfigChangedEvent(cluster.getClusterName(),
        getType(), getTag(), getVersion());

    eventPublisher.publish(event);
  }

  /**
   * Persist the cluster and configuration entities in their own transaction.
   */
  @Transactional
  void persistEntitiesInTransaction(ClusterConfigEntity entity) {
    ClusterEntity clusterEntity = entity.getClusterEntity();

    clusterDAO.createConfig(entity);
    clusterEntity.getClusterConfigEntities().add(entity);

    // save the entity, forcing a flush to ensure the refresh picks up the
    // newest data
    clusterEntity = clusterDAO.merge(clusterEntity, true);
    LOG.info("Persisted config entity with id {} and cluster entity {}", entity.getConfigId(),
        clusterEntity.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void save() {
    ClusterConfigEntity entity = clusterDAO.findConfig(configId);

    // if the configuration was found, then update it
    if (null != entity) {
      ClusterEntity clusterEntity = clusterDAO.findById(entity.getClusterId());
      LOG.debug("Updating {} version {} with new configurations; a new version will not be created",
          getType(), getVersion());

      entity.setData(gson.toJson(getProperties()));

      // save the entity, forcing a flush to ensure the refresh picks up the
      // newest data
      clusterDAO.merge(clusterEntity, true);

      // re-load the entity associations for the cluster
      cluster.refresh();

      // broadcast the change event for the configuration
      ClusterConfigChangedEvent event = new ClusterConfigChangedEvent(cluster.getClusterName(),
          getType(), getTag(), getVersion());

      eventPublisher.publish(event);
    }
  }
}
