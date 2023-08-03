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

import org.apache.inlong.manager.web.WebBaseTest;
import org.apache.inlong.manager.web.utils.InlongRequestWrapper;

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

import java.io.BufferedReader;
import java.io.IOException;

public class HttpServletRequestFilterTest extends WebBaseTest {

    @Test
    public void testRepeatableRead() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String bodyStr = "test body str";
        req.setContent(bodyStr.getBytes());
        Servlet servlet = new HttpServlet() {
        };
        MockHttpServletResponse res = new MockHttpServletResponse();
        HttpServletRequestFilter filter = new HttpServletRequestFilter();
        FilterChecker checker = new FilterChecker();
        MockFilterChain filterChain = new MockFilterChain(servlet, filter, checker);
        Assertions.assertDoesNotThrow(() -> filterChain.doFilter(req, res));
    }

    class FilterChecker implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Assertions.assertInstanceOf(InlongRequestWrapper.class, request);
            String first = "";
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                first += line;
            }

            String second = "";
            BufferedReader reader2 = request.getReader();
            while ((line = reader2.readLine()) != null) {
                second += line;
            }

            Assertions.assertEquals(first, second);
        }
    }

}