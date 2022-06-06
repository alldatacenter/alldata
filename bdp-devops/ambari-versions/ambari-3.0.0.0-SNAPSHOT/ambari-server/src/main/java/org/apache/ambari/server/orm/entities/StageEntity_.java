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

package org.apache.ambari.server.orm.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.apache.ambari.server.controller.internal.StageResourceProvider;

/**
 * The {@link StageEntity_} is a strongly typed metamodel for creating
 * {@link javax.persistence.criteria.CriteriaQuery} for {@link StageEntity}.
 */
@StaticMetamodel(StageEntity.class)
public class StageEntity_ {
  public static volatile SingularAttribute<StageEntity, Long> clusterId;
  public static volatile SingularAttribute<StageEntity, Long> requestId;
  public static volatile SingularAttribute<StageEntity, Long> stageId;

  public static volatile SingularAttribute<StageEntity, String> logInfo;
  public static volatile SingularAttribute<StageEntity, String> requestContext;

  public static volatile SingularAttribute<StageEntity, byte[]> commandParamsStage;
  public static volatile SingularAttribute<StageEntity, byte[]> hostParamsStage;

  public static volatile SingularAttribute<StageEntity, RequestEntity> request;

  /**
   * Gets a mapping of between a resource provider property.
   * <p/>
   * This is used when converting an Ambari {@link org.apache.ambari.server.controller.spi.Predicate} into a JPA
   * {@link javax.persistence.criteria.Predicate} and we need a type-safe
   * conversion between "category/property" and JPA field names.
   * <p/>
   * Multiple {@link SingularAttribute} instances can be chained together in
   * order to provide an {@code entity.subEntity.field} reference.
   *
   * @return a mapping of between a resource provider property
   */
  public static Map<String, List<? extends SingularAttribute<StageEntity, ?>>> getPredicateMapping() {
    Map<String, List<? extends SingularAttribute<StageEntity, ?>>> mapping = new HashMap<>();

    mapping.put(StageResourceProvider.STAGE_REQUEST_ID,
        Collections.singletonList(requestId));

    mapping.put(StageResourceProvider.STAGE_STAGE_ID,
        Collections.singletonList(stageId));

    mapping.put(StageResourceProvider.STAGE_LOG_INFO,
        Collections.singletonList(logInfo));

    mapping.put(StageResourceProvider.STAGE_CONTEXT,
        Collections.singletonList(requestContext));

    mapping.put(StageResourceProvider.STAGE_COMMAND_PARAMS,
        Collections.singletonList(commandParamsStage));

    mapping.put(StageResourceProvider.STAGE_HOST_PARAMS,
        Collections.singletonList(hostParamsStage));

    return mapping;
  }
}
