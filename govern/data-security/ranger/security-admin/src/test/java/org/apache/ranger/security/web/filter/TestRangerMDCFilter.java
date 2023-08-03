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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.security.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class TestRangerMDCFilter {

    @Test
    public void testRequestContainRequestIdHeader() throws ServletException, IOException {
        HttpServletRequest  mockReq   = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockRes   = Mockito.mock(HttpServletResponse.class);
        FilterChain         mockChain = Mockito.mock(FilterChain.class);
        RangerMDCFilter     filter    = new RangerMDCFilter();

        Mockito.when(mockReq.getHeader(RangerMDCFilter.DEFAULT_REQUEST_ID_HEADER_NAME)).thenReturn("test");
        Mockito.when(mockReq.getMethod()).thenReturn("GET");

        filter.doFilter(mockReq, mockRes, mockChain);

        Mockito.verify(mockChain).doFilter(mockReq, mockRes);
    }

    @Test
    public void testRequestNotContainRequestIdHeader() throws ServletException, IOException {
        HttpServletRequest  mockReq   = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockRes   = Mockito.mock(HttpServletResponse.class);
        FilterChain         mockChain = Mockito.mock(FilterChain.class);
        RangerMDCFilter     filter    = new RangerMDCFilter();

        Mockito.when(mockReq.getHeader(RangerMDCFilter.DEFAULT_REQUEST_ID_HEADER_NAME)).thenReturn(null);
        Mockito.when(mockReq.getMethod()).thenReturn("GET");

        filter.doFilter(mockReq, mockRes, mockChain);

        Mockito.verify(mockChain).doFilter(mockReq, mockRes);
    }
}
