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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.api.services.parsers.JsonRequestBodyParser;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.CsvSerializer;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.utils.RetryHelper;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides common functionality to all services.
 */
public abstract class BaseService {
  public final static MediaType MEDIA_TYPE_TEXT_CSV_TYPE = new MediaType("text", "csv");

  public static final String MSG_SUCCESSFUL_OPERATION = "Successful operation";
  public static final String MSG_REQUEST_ACCEPTED = "Request is accepted, but not completely processed yet";
  public static final String MSG_INVALID_ARGUMENTS = "Invalid arguments";
  public static final String MSG_INVALID_REQUEST = "Invalid request";
  public static final String MSG_CLUSTER_NOT_FOUND = "Cluster not found";
  public static final String MSG_CLUSTER_OR_HOST_NOT_FOUND = "Cluster or host not found";
  public static final String MSG_NOT_AUTHENTICATED = "Not authenticated";
  public static final String MSG_PERMISSION_DENIED = "Not permitted to perform the operation";
  public static final String MSG_SERVER_ERROR = "Internal server error";
  public static final String MSG_RESOURCE_ALREADY_EXISTS = "The requested resource already exists.";
  public static final String MSG_RESOURCE_NOT_FOUND = "The requested resource doesn't exist.";

  public static final String QUERY_FIELDS = "fields";
  public static final String QUERY_FILTER_DESCRIPTION = "Filter fields in the response (identifier fields are mandatory)";
  public static final String QUERY_SORT = "sortBy";
  public static final String QUERY_SORT_DESCRIPTION = "Sort resources in result by (asc | desc)";
  public static final String QUERY_PAGE_SIZE = "page_size";
  public static final String QUERY_PAGE_SIZE_DESCRIPTION = "The number of resources to be returned for the paged response.";
  public static final String DEFAULT_PAGE_SIZE = "10";
  public static final String QUERY_FROM = "from";
  public static final String QUERY_FROM_DESCRIPTION = "The starting page resource (inclusive).  \"start\" is also accepted.";
  public static final String QUERY_FROM_VALUES = "range[0, infinity]";
  public static final String DEFAULT_FROM = "0";
  public static final String QUERY_TO = "to";
  public static final String QUERY_TO_DESCRIPTION = "The ending page resource (inclusive).  \"end\" is also accepted.";
  public static final String QUERY_TO_TYPE = "integer";
  public static final String QUERY_TO_VALUES = "range[1, infinity]";
  public static final String QUERY_PREDICATE = "{predicate}";
  public static final String QUERY_PREDICATE_DESCRIPTION = "The predicate to filter resources by. Omitting the predicate will " +
      "match all resources.";

  public static final String RESPONSE_CONTAINER_LIST = "List";

  public static final String DATA_TYPE_INT = "integer";
  public static final String DATA_TYPE_STRING = "string";

  public static final String PARAM_TYPE_QUERY = "query";
  public static final String PARAM_TYPE_BODY = "body";

  public static final String FIELDS_SEPARATOR = ", ";

  private final static Logger LOG = LoggerFactory.getLogger(BaseService.class);

  /**
   * Factory for creating resource instances.
   */
  private ResourceInstanceFactory m_resourceFactory = new ResourceInstanceFactoryImpl();

  /**
   * Result serializer.
   */
  private ResultSerializer m_serializer = new JsonSerializer();

  protected static RequestAuditLogger requestAuditLogger;

  public static void init(RequestAuditLogger instance) {
    requestAuditLogger = instance;
  }

  /**
   * Requests are funneled through this method so that common logic can be executed.
   * Creates a request instance and invokes it's process method.  Uses the default
   * media type.
   *
   * @param headers      http headers
   * @param body         http body
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param resource     resource instance that is being acted on
   *
   * @return the response of the operation in serialized form
   */
  protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo,
                                   Request.Type requestType, ResourceInstance resource) {

    return handleRequest(headers, body, uriInfo, requestType, null, resource);
  }

  /**
   * Requests are funneled through this method so that common logic can be executed.
   * Creates a request instance and invokes it's process method.
   *
   * @param headers      http headers
   * @param body         http body
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param mediaType    the requested media type; may be null
   * @param resource     resource instance that is being acted on
   *
   * @return the response of the operation in serialized form
   */
  protected Response handleRequest(HttpHeaders headers, String body,
                                   UriInfo uriInfo, Request.Type requestType,
                                   MediaType mediaType, ResourceInstance resource) {

    // original request and initial result
    RequestBody rb = new RequestBody();
    rb.setBody(body);
    Request request = getRequestFactory().createRequest(headers, rb, uriInfo, requestType, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    try {
      Set<RequestBody> requestBodySet = getBodyParser().parse(body);

      Iterator<RequestBody> iterator = requestBodySet.iterator();
      while (iterator.hasNext() && result.getStatus().getStatus().equals(ResultStatus.STATUS.OK)) {
        RequestBody requestBody = iterator.next();

        request = getRequestFactory().createRequest(
            headers, requestBody, uriInfo, requestType, resource);

        result  = request.process();
        // if it is not OK, then it is logged below
        if(ResultStatus.STATUS.OK.equals(result.getStatus().getStatus())) {
          requestAuditLogger.log(request, result);
        }
      }

      if(requestBodySet.isEmpty() || !ResultStatus.STATUS.OK.equals(result.getStatus().getStatus())) {
        requestAuditLogger.log(request, result);
      }
    } catch (BodyParseException e) {
      result =  new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
      LOG.error("Bad request received: " + e.getMessage());
      requestAuditLogger.log(request, result);
    } catch (Throwable t) {
      requestAuditLogger.log(request, new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, t.getMessage())));
      throw t;
    }

    ResultSerializer serializer = mediaType == null ? getResultSerializer() : getResultSerializer(mediaType);

    Response.ResponseBuilder builder = Response.status(result.getStatus().getStatusCode()).entity(
        serializer.serialize(result));

    if (mediaType != null) {
      builder.type(mediaType);
    }

    RetryHelper.clearAffectedClusters();
    return builder.build();
  }

  /**
   * Obtain the factory from which to create Request instances.
   *
   * @return the Request factory
   */
  RequestFactory getRequestFactory() {
    return new RequestFactory();
  }

  /**
   * Create a resource instance.
   *
   * @param type    the resource type
   * @param mapIds  all primary and foreign key properties and values
   *
   * @return a newly created resource instance
   */
  protected ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {
    return m_resourceFactory.createResource(type, mapIds);
  }

  /**
   * Get a serializer for the given media type.
   *
   * @param mediaType  the media type
   *
   * @return the result serializer
   */
  protected ResultSerializer getResultSerializer(final MediaType mediaType) {

    final ResultSerializer serializer = getResultSerializer();

    if (mediaType.equals(MediaType.TEXT_PLAIN_TYPE)){
      return new ResultSerializer() {
        @Override
        public Object serialize(Result result) {
          return serializer.serialize(result).toString();
        }

        @Override
        public Object serializeError(ResultStatus error) {
          return serializer.serializeError(error).toString();
        }
      };
    } else if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)){
      return new ResultSerializer() {
        @Override
        public Object serialize(Result result) {
          return JSON.parse(serializer.serialize(result).toString());
        }

        @Override
        public Object serializeError(ResultStatus error) {
          return JSON.parse(serializer.serializeError(error).toString());
        }
      };
    }
    else if (mediaType.equals(MEDIA_TYPE_TEXT_CSV_TYPE)) {
      return new CsvSerializer();
    }

    throw new IllegalArgumentException("The media type " + mediaType + " is not supported.");
  }

  /**
   * Get the default serializer.
   *
   * @return the default serializer
   */
  protected ResultSerializer getResultSerializer() {
    return m_serializer;
  }

  protected RequestBodyParser getBodyParser() {
    return new JsonRequestBodyParser();
  }
}
