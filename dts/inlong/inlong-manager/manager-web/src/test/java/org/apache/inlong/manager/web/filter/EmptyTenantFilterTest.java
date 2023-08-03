/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.web.filter;

import org.apache.inlong.common.util.BasicAuth;
import org.apache.inlong.manager.web.WebBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

public class EmptyTenantFilterTest extends WebBaseTest {

    @Test
    public void testNoTenant() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Servlet servlet = new HttpServlet() {
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequestFilter httpServletRequestFilter = new HttpServletRequestFilter();
        EmptyTenantFilter emptyTenantFilter = new EmptyTenantFilter();
        FilterChecker checker = new FilterChecker(BasicAuth.DEFAULT_TENANT);
        MockFilterChain filterChain =
                new MockFilterChain(servlet, httpServletRequestFilter, emptyTenantFilter, checker);
        Assertions.assertDoesNotThrow(() -> filterChain.doFilter(req, res));
    }

    @Test
    public void testWithTenant() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String testTenant = "testTenant";
        req.addHeader(BasicAuth.BASIC_AUTH_TENANT_HEADER, testTenant);
        Servlet servlet = new HttpServlet() {
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequestFilter httpServletRequestFilter = new HttpServletRequestFilter();
        EmptyTenantFilter emptyTenantFilter = new EmptyTenantFilter();
        FilterChecker checker = new FilterChecker(testTenant);
        MockFilterChain filterChain =
                new MockFilterChain(servlet, httpServletRequestFilter, emptyTenantFilter, checker);
        Assertions.assertDoesNotThrow(() -> filterChain.doFilter(req, res));
    }

    class FilterChecker implements Filter {

        String targetTenant;

        public FilterChecker(String targetTenant) {
            this.targetTenant = targetTenant;
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String tenant = httpServletRequest.getHeader(BasicAuth.BASIC_AUTH_TENANT_HEADER);
            Assertions.assertEquals(targetTenant, tenant);
        }
    }
}