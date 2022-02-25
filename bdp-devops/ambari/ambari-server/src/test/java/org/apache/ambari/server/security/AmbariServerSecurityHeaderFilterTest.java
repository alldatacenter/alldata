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

package org.apache.ambari.server.security;

import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.configuration.Configuration;

public class AmbariServerSecurityHeaderFilterTest extends AbstractSecurityHeaderFilterTest {

  private static final Map<String, String> PROPERTY_NAME_MAP;
  private static final Map<String, String> DEFAULT_PROPERTY_VALUE_MAP;

  static {

    Map<String, String> map;
    map = new HashMap<>();
    map.put(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE.getKey());
    map.put(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE.getKey());
    map.put(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE.getKey());
    map.put(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, Configuration.HTTP_X_CONTENT_TYPE_HEADER_VALUE.getKey());
    map.put(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, Configuration.HTTP_CACHE_CONTROL_HEADER_VALUE.getKey());
    map.put(AbstractSecurityHeaderFilter.PRAGMA_HEADER, Configuration.HTTP_PRAGMA_HEADER_VALUE.getKey());

    PROPERTY_NAME_MAP = Collections.unmodifiableMap(map);

    map = new HashMap<>();
    map.put(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE.getDefaultValue());
    map.put(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE.getDefaultValue());
    map.put(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE.getDefaultValue());
    map.put(AbstractSecurityHeaderFilter.X_CONTENT_TYPE_HEADER, Configuration.HTTP_X_CONTENT_TYPE_HEADER_VALUE.getDefaultValue());
    map.put(AbstractSecurityHeaderFilter.CACHE_CONTROL_HEADER, Configuration.HTTP_CACHE_CONTROL_HEADER_VALUE.getDefaultValue());
    map.put(AbstractSecurityHeaderFilter.PRAGMA_HEADER, Configuration.HTTP_PRAGMA_HEADER_VALUE.getDefaultValue());
    DEFAULT_PROPERTY_VALUE_MAP = Collections.unmodifiableMap(map);
  }

  public AmbariServerSecurityHeaderFilterTest() {
    super(AmbariServerSecurityHeaderFilter.class, PROPERTY_NAME_MAP, DEFAULT_PROPERTY_VALUE_MAP);
  }

  @Override
  protected void expectHttpServletRequestMock(HttpServletRequest request) {
    expect(request.getAttribute(AbstractSecurityHeaderFilter.DENY_HEADER_OVERRIDES_FLAG)).andReturn(null);
  }
}