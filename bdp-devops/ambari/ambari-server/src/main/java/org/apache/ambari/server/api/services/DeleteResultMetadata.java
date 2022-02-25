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

package org.apache.ambari.server.api.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.commons.lang.Validate;

/**
 * Implementation of ResultDetails for DELETE API requests.
 */
public class DeleteResultMetadata implements ResultMetadata {
  //Keys for all successful delete resources.
  private final Set<String> deletedKeys;

  //ResultStatus for keys which threw exception during delete.
  private final Map<String, ResultStatus> excptions;

  public DeleteResultMetadata() {
    this.deletedKeys = new HashSet<>();
    this.excptions = new HashMap<>();
  }

  /**
   * Add successfully deleted key.
   * @param key - successfully deleted key.
   */
  public void addDeletedKey(String key) {
    Validate.notNull(key);
    deletedKeys.add(key);
  }

  /**
   * Add successfully deleted keys.
   * @param keys - successfully deleted keys.
   */
  public void addDeletedKeys(Collection<String> keys) {
    Validate.notNull(keys);
    deletedKeys.addAll(keys);
  }

  /**
   * Add exception thrown during delete of a key.
   * @param key - successfully deleted keys.
   * @param ex - exception
   */
  public void addException(String key, Exception ex) {
    Validate.notNull(key);
    Validate.notNull(ex);
    ResultStatus resultStatus = getResultStatusForException(ex);
    excptions.put(key, resultStatus);
  }

  /**
   * Add exception thrown during delete of keys.
   */
  public void addExceptions(Map<String, Exception> exceptionKeyMap) {
    if (exceptionKeyMap == null) {
      return;
    }

    for (Map.Entry<String, Exception> exceptionEntry : exceptionKeyMap.entrySet()) {
      ResultStatus resultStatus = getResultStatusForException(exceptionEntry.getValue());
      excptions.put(exceptionEntry.getKey(), resultStatus);
    }
  }

  public Set<String> getDeletedKeys() {
    return Collections.unmodifiableSet(deletedKeys);
  }

  public Map<String, ResultStatus> getExcptions() {
    return Collections.unmodifiableMap(excptions);
  }

  /**
   * Factory method to create {@link ResultStatus} object for Exception.
   *
   * @param ex - exception
   * @return ResultStatus
   */
  private ResultStatus getResultStatusForException(Exception ex) {
    Validate.notNull(ex);
    if (ex.getClass() == AuthorizationException.class) {
      return new ResultStatus(ResultStatus.STATUS.FORBIDDEN, ex);
    } else if (ex.getClass() == SystemException.class) {
      return new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, ex);
    } else if (ex instanceof ObjectNotFoundException) {
      return new ResultStatus(ResultStatus.STATUS.NOT_FOUND, ex);
    } else if (ex.getClass() == UnsupportedPropertyException.class) {
      return new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, ex);
    } else {
      return new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, ex);
    }
  }
}
