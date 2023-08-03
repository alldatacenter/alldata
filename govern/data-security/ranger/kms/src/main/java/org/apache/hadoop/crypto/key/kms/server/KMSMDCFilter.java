/**
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
package org.apache.hadoop.crypto.key.kms.server;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.delegation.web.HttpUserGroupInformation;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet filter that captures context of the HTTP request to be use in the
 * scope of KMS calls on the server side.
 */
@InterfaceAudience.Private
public class KMSMDCFilter implements Filter {
	
	static final String RANGER_KMS_REST_API_PATH = "/kms/api/status";
  private static class Data {
    private UserGroupInformation ugi;
    private String method;
    private String url;

    private Data(UserGroupInformation ugi, String method, String url) {
      this.ugi = ugi;
      this.method = method;
      this.url = url;
    }
  }

  private static final ThreadLocal<Data> DATA_TL = new ThreadLocal<Data>();

  public static UserGroupInformation getUgi() {
    return DATA_TL.get().ugi;
  }

  public static String getMethod() {
    return DATA_TL.get().method;
  }

  public static String getURL() {
    return DATA_TL.get().url;
  }

  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		try {
			String path = ((HttpServletRequest) request).getRequestURI();
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

			if (path.startsWith(RANGER_KMS_REST_API_PATH)) {
				chain.doFilter(request, resp);
			} else {
				DATA_TL.remove();
				UserGroupInformation ugi = HttpUserGroupInformation.get();
				String method = ((HttpServletRequest) request).getMethod();
				StringBuffer requestURL = ((HttpServletRequest) request).getRequestURL();
				String queryString = ((HttpServletRequest) request).getQueryString();
				if (queryString != null) {
					requestURL.append("?").append(queryString);
				}
				DATA_TL.set(new Data(ugi, method, requestURL.toString()));
				chain.doFilter(request, resp);
			}
		} finally {
			DATA_TL.remove();

		}
	}

  @Override
  public void destroy() {
  }
}
