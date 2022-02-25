/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableSet;

/**
 * The purpose of this helper is to get remote address from an HTTP request
 */
public class RequestUtils {

  private static Set<String> headersToCheck= ImmutableSet.copyOf(Arrays.asList(
    "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"));

  /**
   * Returns remote address
   * @param request contains the details of http request
   * @return
   */
  public static String getRemoteAddress(HttpServletRequest request) {
    String ip = null;
    for (String header : headersToCheck) {
      ip = request.getHeader(header);
      if (!isRemoteAddressUnknown(ip)) {
        break;
      }
    }
    if (isRemoteAddressUnknown(ip)) {
      ip = request.getRemoteAddr();
    }
    if (containsMultipleRemoteAddresses(ip)) {
       ip = ip.substring(0, ip.indexOf(","));
    }
    return ip;
  }

  /**
   * Returns remote address by using {@link HttpServletRequest} from {@link RequestContextHolder}
   * @return
   */
  public static String getRemoteAddress() {

    if(hasValidRequest()) {
      return getRemoteAddress(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    return null;
  }

  /**
   * Checks whether ip address is null, empty or unknown
   * @param ip
   * @return
   */
  private static boolean isRemoteAddressUnknown(String ip) {
    return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
  }

  /**
   * Checks if ip contains multiple IP addresses
   */
  private static boolean containsMultipleRemoteAddresses(String ip) {
    return ip != null && ip.indexOf(",") > 0;
  }

  /**
   * Checks if RequestContextHolder contains a valid HTTP request
   * @return
   */
  private static boolean hasValidRequest() {
    return RequestContextHolder.getRequestAttributes() != null &&
      RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes &&
      ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest() != null;
  }

  /**
   * Posts an informational message the the supplied {@link Logger} showing the request headers and
   * query parameters
   * <p>
   * For example:
   * <pre>
   * ##### HEADERS ########
   * X-Requested-By = ambari
   * ...
   * Accept-Encoding = gzip, deflate
   * Accept-Language = en-US,en;q=0.9
   * ######################
   * ##### PARAMETERS #####
   * _ = 1543700737939
   * ######################
   * </pre>
   *
   * @param request the {@link HttpServletRequest} to log
   * @param logger  the {@link Logger}
   */
  public static void logRequestHeadersAndQueryParams(HttpServletRequest request, Logger logger) {
    if (logger != null) {
      StringBuilder builder;

      builder = new StringBuilder();
      builder.append("\n##### HEADERS #######");
      Enumeration<String> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String name = headerNames.nextElement();
        Enumeration<String> values = request.getHeaders(name);
        while (values.hasMoreElements()) {
          String value = values.nextElement();
          builder.append("\n\t");
          builder.append(name);
          builder.append(" = ");
          builder.append(value);
        }
      }
      builder.append("\n#####################");
      builder.append("\n##### PARAMETERS ####");
      MultiValueMap<String, String> queryParams = getQueryStringParameters(request);
      if (queryParams != null) {
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
          String name = entry.getKey();
          List<String> values = entry.getValue();
          for (String value : values) {
            builder.append("\n\t");
            builder.append(name);
            builder.append(" = ");
            builder.append(value);
          }
        }
      }
      builder.append("\n#####################");

      logger.info(builder.toString());
    }
  }

  /**
   * Returns a {@link MultiValueMap} of the parameters parsed from the request's query string.  The
   * returned map will not contain any parameters that may be in the body of the request as form data.
   * <p>
   * This implementation manually parses the query string rather than use {@link HttpServletRequest#getParameterValues(String)}
   * so that the message body remains intact and available.  Calling {@link HttpServletRequest#getParameterValues(String)}
   * could interfere with processing the body of this request later since the body is parsed to find
   * any form parameters.
   *
   * @param request the {@link HttpServletRequest}
   * @return a Map of query parameters to values
   */
  public static MultiValueMap<String, String> getQueryStringParameters(HttpServletRequest request) {
    // Manually parse the query string rather than use HttpServletRequest#getParameter so that
    // the message body remains intact and available.  Calling HttpServletRequest#getParameter
    // could interfere with processing the body of this request later since the body needs to be
    // parsed to find any form parameters.
    String queryString = request.getQueryString();
    return (StringUtils.isEmpty(queryString))
        ? null
        : UriComponentsBuilder.newInstance().query(queryString).build().getQueryParams();
  }

  /**
   * Returns a {@link List} of values parsed from the request's query string for the given parameter
   * name.
   *
   * @param request the {@link HttpServletRequest}
   * @return a List of values for the specified query parameter; or <code>null</code> if the
   * requested parameter is not present
   * @see #getQueryStringParameters(HttpServletRequest)
   */
  public static List<String> getQueryStringParameterValues(HttpServletRequest request, String parameterName) {
    MultiValueMap<String, String> valueMap = getQueryStringParameters(request);
    return ((valueMap == null) || !valueMap.containsKey(parameterName))
        ? null
        : valueMap.get(parameterName);
  }

  /**
   * Returns the value parsed from the request's query string for the given parameter name.
   * <p>
   * If more than one value for the given parameter name is found, the first value will be retured.
   *
   * @param request the {@link HttpServletRequest}
   * @return the value for the specified query parameter; or <code>null</code> if the
   * requested parameter is not present
   * @see #getQueryStringParameters(HttpServletRequest)
   */
  public static String getQueryStringParameterValue(HttpServletRequest request, String parameterName) {
    MultiValueMap<String, String> valueMap = getQueryStringParameters(request);
    return ((valueMap == null) || !valueMap.containsKey(parameterName))
        ? null
        : valueMap.getFirst(parameterName);
  }

}
