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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.api.predicate.QueryLexer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.internal.PageRequestImpl;
import org.apache.ambari.server.controller.internal.SortRequestImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.SortRequestProperty;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request implementation.
 */
public abstract class BaseRequest implements Request {

  /**
   * URI information
   */
  private UriInfo m_uriInfo;

  /**
   * Http headers
   */
  private HttpHeaders m_headers;

  /**
   * Http Body
   */
  private RequestBody m_body;

  /**
   * Remote address
   */
  private String m_remoteAddress;

  /**
   * Query Predicate
   */
  private Predicate m_predicate;

  /**
   * Associated resource definition
   */
  private ResourceInstance m_resource;

  /**
   * Default page size for pagination request.
   */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /**
   * Page size property key
   */
  public static final String PAGE_SIZE_PROPERTY_KEY = "Request_Info/max_results";

  /**
   * Sort order property key. (true - ASC , false - DESC)
   */
  public static final String ASC_ORDER_PROPERTY_KEY = "Request_Info/asc_order";

  /**
   * Associated resource renderer.
   * Will default to the default renderer if non is specified.
   */
  private Renderer m_renderer;

  /**
   *  Logger instance.
   */
  private final static Logger LOG = LoggerFactory.getLogger(Request.class);


  /**
   * Constructor.
   *
   * @param headers           http headers
   * @param body              http body
   * @param uriInfo           uri information
   * @param resource          associated resource definition
   *
   */
  public BaseRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    m_headers     = headers;
    m_uriInfo     = uriInfo;
    m_resource    = resource;
    m_body        = body;
    m_remoteAddress  = RequestUtils.getRemoteAddress();
  }

  @Override
  public Result process() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling API Request: '{}'", getURI());
    }

    Result result;
    try {
      parseRenderer();
      parseQueryPredicate();
      result = getRequestHandler().handleRequest(this);
    } catch (InvalidQueryException e) {
      String message = "Unable to compile query predicate: " + e.getMessage();
      LOG.error(message, e);
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, message));
    } catch (IllegalArgumentException e) {
      String message = "Invalid Request: " + e.getMessage();
      LOG.error(message, e);
      result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, message));
    }

    if (! result.getStatus().isErrorState()) {
      getResultPostProcessor().process(result);
    }

    return result;
  }

  @Override
  public ResourceInstance getResource() {
    return m_resource;
  }

  @Override
  public String getURI() {
    return m_uriInfo.getRequestUri().toASCIIString();
  }

  @Override
  public int getAPIVersion() {
    return 1;
  }

  @Override
  public Predicate getQueryPredicate() {
    return m_predicate;
  }

  @Override
  public Map<String, TemporalInfo> getFields() {
    Map<String, TemporalInfo> mapProperties;
    String partialResponseFields = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_FIELDS);
    if (partialResponseFields == null) {
      mapProperties = Collections.emptyMap();
    } else {
      Set<String> setMatches = new HashSet<>();
      // Pattern basically splits a string using ',' as the deliminator unless ',' is between '[' and ']'.
      // Actually, captures char sequences between ',' and all chars between '[' and ']' including ','.
      Pattern re = Pattern.compile("[^,\\[]*?\\[[^\\]]*?\\]|[^,]+");
      Matcher m = re.matcher(partialResponseFields);
      while (m.find()){
        for (int groupIdx = 0; groupIdx < m.groupCount() + 1; groupIdx++) {
          setMatches.add(m.group(groupIdx));
        }
      }

      mapProperties = new HashMap<>(setMatches.size());
      for (String field : setMatches) {
        TemporalInfo temporalInfo = null;
        if (field.contains("[")) {
          String[] temporalData = field.substring(field.indexOf('[') + 1,
              field.indexOf(']')).split(",");
          field = field.substring(0, field.indexOf('['));
          long start = Long.parseLong(temporalData[0].trim());
          long end   = -1;
          long step  = -1;
          if (temporalData.length >= 2) {
            end = Long.parseLong(temporalData[1].trim());
            if (temporalData.length == 3) {
              step = Long.parseLong(temporalData[2].trim());
            }
          }
          temporalInfo = new TemporalInfoImpl(start, end, step);
        }
        mapProperties.put(field, temporalInfo);
      }
    }

    return mapProperties;
  }

  @Override
  public Renderer getRenderer() {
   return m_renderer;
  }

  @Override
  public Map<String, List<String>> getHttpHeaders() {
    return m_headers.getRequestHeaders();
  }

  @Override
  public PageRequest getPageRequest() {

    String pageSize = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_PAGE_SIZE);
    String from     = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_FROM);
    String to       = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_TO);

    if (pageSize == null && from == null && to == null) {
      return null;
    }

    int offset = 0;
    PageRequest.StartingPoint startingPoint;

    // TODO : support other starting points
    if (from != null) {
      if(from.equals("start")) {
        startingPoint = PageRequest.StartingPoint.Beginning;
      } else {
        offset = Integer.parseInt(from);
        startingPoint = PageRequest.StartingPoint.OffsetStart;
      }
    } else if (to != null ) {
      if (to.equals("end")) {
        startingPoint = PageRequest.StartingPoint.End;
      } else {
        offset = Integer.parseInt(to);
        startingPoint = PageRequest.StartingPoint.OffsetEnd;
      }
    } else {
      startingPoint = PageRequest.StartingPoint.Beginning;
    }

    // TODO : support predicate and comparator
    return new PageRequestImpl(startingPoint,
        pageSize == null ? DEFAULT_PAGE_SIZE : Integer.parseInt(pageSize), offset, null, null);
  }

  @Override
  public RequestBody getBody() {
    return m_body;
  }

  /**
   * Obtain the result post processor for the request.
   *
   * @return the result post processor
   */
  protected ResultPostProcessor getResultPostProcessor() {
    return m_renderer.getResultPostProcessor(this);
  }

  /**
   * Obtain the predicate compiler which is used to compile the query string into
   * a predicate.
   *
   * @return the predicate compiler
   */
  protected PredicateCompiler getPredicateCompiler() {
    return new PredicateCompiler();
  }

  /**
   * Check to see if 'minimal_response=true' is specified in the query string.
   *
   * @return true if 'minimal_response=true' is specified, false otherwise
   */
  private boolean isMinimal() {
    String minimal = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_MINIMAL);
    return minimal != null && minimal.equalsIgnoreCase("true");
  }

  /**
   * Parse the query string and compile it into a predicate.
   * The query string may have already been extracted from the http body.
   * If the query string didn't exist in the body use the query string in the URL.
   *
   * @throws InvalidQueryException  if unable to parse a non-null query string into a predicate
   */
  private void parseQueryPredicate() throws InvalidQueryException {
    String queryString = m_body.getQueryString();
    if (queryString == null) {
      String uri = getURI();
      int qsBegin = uri.indexOf("?");

      queryString = (qsBegin == -1) ? null : uri.substring(qsBegin + 1);
    }

    if (queryString != null) {
      try {
        Collection<String> ignoredProperties = null;
        switch (getRequestType()) {
          case PUT:
            ignoredProperties = m_resource.getResourceDefinition().getUpdateDirectives();
            break;
          case POST:
            ignoredProperties = m_resource.getResourceDefinition().getCreateDirectives();
            break;
          case GET:
            ignoredProperties = m_resource.getResourceDefinition().getReadDirectives();
            break;
          case DELETE:
            ignoredProperties = m_resource.getResourceDefinition().getDeleteDirectives();
            break;
          default:
            break;
        }


        m_predicate = (ignoredProperties == null)
            ? getPredicateCompiler().compile(URLDecoder.decode(queryString, "UTF-8"))
            : getPredicateCompiler().compile(URLDecoder.decode(queryString, "UTF-8"), ignoredProperties);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Unable to decode URI: " + e, e);
      }
    }
  }

  /**
   * Parse the query string for the {@link QueryLexer#QUERY_FORMAT} property and obtain
   * a renderer from the associated resource definition based on this property value.
   */
  private void parseRenderer() {
    String rendererName = isMinimal() ? "minimal" :
        m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_FORMAT);
    m_renderer = m_resource.getResourceDefinition().
        getRenderer(rendererName);
  }

  @Override
  public SortRequest getSortRequest() {
    String sortByParams = m_uriInfo.getQueryParameters().getFirst(QueryLexer.QUERY_SORT);
    if (sortByParams != null && !sortByParams.isEmpty()) {
      String[] params = sortByParams.split(",");
      List<SortRequestProperty> properties = new ArrayList<>();
      if (params.length > 0) {
        for (String property : params) {
          SortRequest.Order order = SortRequest.Order.ASC;
          String propertyId = property;
          int idx = property.indexOf(".");
          if (idx != -1) {
            order = SortRequest.Order.valueOf(property.substring(idx + 1).toUpperCase());
            propertyId = property.substring(0, idx);
          }
          properties.add(new SortRequestProperty(propertyId, order));
        }
      }

      return new SortRequestImpl(properties);
    }
    return null;
  }

  /**
   * Obtain the underlying request handler for the request.
   *
   * @return  the request handler
   */
  protected abstract RequestHandler getRequestHandler();

  @Override
  public String getRemoteAddress() {
    return m_remoteAddress;
  }
}
