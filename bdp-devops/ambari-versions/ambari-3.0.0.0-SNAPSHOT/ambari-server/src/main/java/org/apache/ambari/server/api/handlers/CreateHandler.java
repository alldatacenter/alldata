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

package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.internal.OperationStatusMetaData;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Responsible for create requests.
 */
public class CreateHandler extends BaseManagementHandler {

  private final static Logger LOG = LoggerFactory.getLogger(CreateHandler.class);

  @Override
  protected Result persist(ResourceInstance resource, RequestBody body) {
    Result result;
    try {
      RequestStatus status = getPersistenceManager().create(resource, body);

      result = createResult(status);

      if (result.isSynchronous()) {
        if (resource.getResourceDefinition().isCreatable()) {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.CREATED));
        } else {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
        }
      } else {
        result.setResultStatus(new ResultStatus(ResultStatus.STATUS.ACCEPTED));
      }

    } catch (AuthorizationException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.FORBIDDEN, e.getMessage()));
    } catch (UnsupportedPropertyException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
      LOG.error("Bad request received: " + e.getMessage());
    } catch (NoSuchParentResourceException e) {
      //todo: is this the correct status code?
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e.getMessage()));
    } catch (SystemException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught a system exception while attempting to create a resource: {}", e.getMessage(), e);
      }
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e.getMessage()));
    } catch (ResourceAlreadyExistsException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.CONFLICT, e.getMessage()));
    } catch(IllegalArgumentException e) {
      LOG.error("Bad request received: " + e.getMessage(), e);
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
    } catch (RuntimeException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught a runtime exception while attempting to create a resource: {}", e.getMessage(), e);
      }
      //result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e.getMessage()));
      throw e;
    }
    return result;
  }

  @Override
  protected ResultMetadata convert(RequestStatusMetaData requestStatusMetaData) {
    if (requestStatusMetaData == null) {
      return null;
    }

    if (requestStatusMetaData.getClass() == OperationStatusMetaData.class) {
      return (OperationStatusMetaData) requestStatusMetaData;
    } else {
      throw new IllegalArgumentException(String.format("RequestStatusDetails is of an expected type: %s",
          requestStatusMetaData.getClass().getName()));
    }
  }
}
