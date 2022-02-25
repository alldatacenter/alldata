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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.controller.internal.TaskResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;


/**
 * This class exists so that JPQL can use static singular attributes that are strongly typed
 * as opposed to Java reflection like HostRoleCommandEntity.get("fieldname")
 */
@javax.persistence.metamodel.StaticMetamodel(HostRoleCommandEntity.class)
public class HostRoleCommandEntity_ {
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> taskId;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> requestId;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> stageId;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> hostId;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> role;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> event;
  public static volatile SingularAttribute<HostRoleCommandEntity, Integer> exitcode;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> status;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> stdError;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> stdOut;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> outputLog;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> errorLog;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> structuredOut;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> startTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> endTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> lastAttemptTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Short> attemptCount;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> roleCommand;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> commandDetail;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> customCommandName;
  public static volatile SingularAttribute<HostRoleCommandEntity, HostEntity> host;

  /**
   * Gets a mapping of between a resource provider property, like
   * {@link TaskResourceProvider#TASK_ID_PROPERTY_ID} to a metamodel
   * {@link SingularAttribute}.
   * <p/>
   * This is used when converting an Ambari {@link Predicate} into a JPA
   * {@link javax.persistence.criteria.Predicate} and we need a type-safe
   * conversion between "category/property" and JPA field names.
   * <p/>
   * Multiple {@link SingularAttribute} instances can be chained together in
   * order to provide an {@code entity.subEntity.field} reference.
   *
   * @return
   */
  public static Map<String, List<? extends SingularAttribute<?, ?>>> getPredicateMapping() {
    Map<String, List<? extends SingularAttribute<?, ?>>> mapping = new HashMap<>();

    mapping.put(TaskResourceProvider.TASK_ID_PROPERTY_ID,
        Collections.singletonList(taskId));

    mapping.put(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID,
        Collections.singletonList(requestId));

    mapping.put(TaskResourceProvider.TASK_STAGE_ID_PROPERTY_ID,
        Collections.singletonList(stageId));

    mapping.put(TaskResourceProvider.TASK_HOST_NAME_PROPERTY_ID,
        Arrays.asList(host, HostEntity_.hostId));

    mapping.put(TaskResourceProvider.TASK_ROLE_PROPERTY_ID,
        Collections.singletonList(role));

    mapping.put(TaskResourceProvider.TASK_EXIT_CODE_PROPERTY_ID,
        Collections.singletonList(exitcode));

    mapping.put(TaskResourceProvider.TASK_STATUS_PROPERTY_ID,
        Collections.singletonList(status));

    mapping.put(TaskResourceProvider.TASK_START_TIME_PROPERTY_ID,
        Collections.singletonList(startTime));

    mapping.put(TaskResourceProvider.TASK_END_TIME_PROPERTY_ID,
        Collections.singletonList(endTime));

    mapping.put(TaskResourceProvider.TASK_ATTEMPT_CNT_PROPERTY_ID,
        Collections.singletonList(attemptCount));

    mapping.put(TaskResourceProvider.TASK_COMMAND_PROPERTY_ID,
        Collections.singletonList(roleCommand));

    mapping.put(TaskResourceProvider.TASK_CUST_CMD_NAME_PROPERTY_ID,
        Collections.singletonList(customCommandName));

    return mapping;
  }
}

