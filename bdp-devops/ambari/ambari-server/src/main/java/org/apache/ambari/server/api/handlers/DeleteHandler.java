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

import org.apache.ambari.server.ConfigGroupNotFoundException;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.DeleteResultMetadata;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;

/**
 * Responsible for delete requests.
 */
public class DeleteHandler extends BaseManagementHandler implements RequestHandler {

  @Override
  protected Result persist(ResourceInstance resource, RequestBody body) {
    Result result;
      try {

        RequestStatus status = getPersistenceManager().delete(resource, body);
        result = createResult(status);

        if (result.isSynchronous()) {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
        } else {
          result.setResultStatus(new ResultStatus(ResultStatus.STATUS.ACCEPTED));
        }
      } catch (AuthorizationException e) {
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.FORBIDDEN, e.getMessage()));
      } catch (SystemException e) {
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
      } catch (NoSuchParentResourceException e) {
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e));
      } catch (NoSuchResourceException e) {
        if (resource.isCollectionResource()) {
          //todo: The query didn't match any resource so no resources were updated.
          //todo: 200 may be ok but we need to return a collection
          //todo: of resources that were updated.
          result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, e));
        } else if (e.getCause() instanceof ConfigGroupNotFoundException){
          result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.ACCEPTED, e));
        } else {
          result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e));
        }
      } catch (UnsupportedPropertyException e) {
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e));
      }

    return result;
  }

  @Override
  protected ResultMetadata convert(RequestStatusMetaData requestStatusMetaData) {
    if (requestStatusMetaData == null) {
      return null;
    }

    if (!(requestStatusMetaData instanceof DeleteStatusMetaData)) {
      throw new IllegalArgumentException(
		  String.format("Wrong status details class received - expecting: %s; actual: %s", 
			   DeleteStatusMetaData.class, requestStatusMetaData.getClass()));
    }

    DeleteStatusMetaData statusDetails = (DeleteStatusMetaData) requestStatusMetaData;
    DeleteResultMetadata resultDetails = new DeleteResultMetadata();
    resultDetails.addDeletedKeys(statusDetails.getDeletedKeys());
    resultDetails.addExceptions(statusDetails.getExceptionForKeys());
    return resultDetails;
  }
}
