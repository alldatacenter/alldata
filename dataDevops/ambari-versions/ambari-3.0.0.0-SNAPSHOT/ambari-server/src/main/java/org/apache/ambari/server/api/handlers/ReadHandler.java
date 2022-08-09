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

import java.util.Map;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for read requests.
 */
public class ReadHandler implements RequestHandler {

  /**
   * Logger instance.
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(ReadHandler.class);

  @Override
  public Result handleRequest(Request request) {
    Query query = request.getResource().getQuery();

    query.setPageRequest(request.getPageRequest());
    query.setSortRequest(request.getSortRequest());
    query.setRenderer(request.getRenderer());

    // If the request body exists, copy the requstInfoProperties from it.  This map should contain
    // the _directives_ specified in the request.
    RequestBody body = request.getBody();
    if(body != null) {
      query.setRequestInfoProps(body.getRequestInfoProperties());
    }

    try {
      addFieldsToQuery(request, query);
    } catch (IllegalArgumentException e) {
      return new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
    }

    Result result;
    Predicate p = null;
    try {
      p = request.getQueryPredicate();
      query.setUserPredicate(p);

      result = query.execute();
      result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    } catch (AuthorizationException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.FORBIDDEN, e.getMessage()));
    } catch (SystemException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
    } catch (NoSuchParentResourceException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e.getMessage()));
    } catch (UnsupportedPropertyException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
    } catch (NoSuchResourceException e) {
      if (p == null) {
        // no predicate specified, resource requested by id
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e.getMessage()));
      } else {
        // resource(s) requested using predicate
        result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, e));
        result.getResultTree().setProperty("isCollection", "true");
      }
    } catch (IllegalArgumentException e) {
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST,
          "Invalid Request: " + e.getMessage()));
      LOG.error("Bad request: ", e);
    }  catch (RuntimeException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught a runtime exception executing a query", e);
      }
      //result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e));
      throw e;
    }
    return result;
  }

  /**
   * Add partial response fields to the provided query.
   *
   * @param request  the current request
   * @param query    the associated query
   */
  private void addFieldsToQuery(Request request, Query query) {
    //Partial response
    for (Map.Entry<String, TemporalInfo> entry : request.getFields().entrySet()) {
      // Iterate over map and add props/temporalInfo
      String propertyId = entry.getKey();
      query.addProperty(propertyId, entry.getValue());
    }
  }
}
