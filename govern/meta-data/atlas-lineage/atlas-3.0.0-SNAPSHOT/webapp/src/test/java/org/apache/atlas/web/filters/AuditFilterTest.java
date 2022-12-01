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
package org.apache.atlas.web.filters;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.apache.commons.configuration.Configuration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
/**
 * This is the test class to test Audit filter functionality
 *
 */
public class AuditFilterTest { 

    public static final String ACTIVE_SERVER_ADDRESS = "http://localhost:21000/";
    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Configuration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testVerifyExcludedOperations() {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        when(configuration.getStringArray(AtlasRepositoryConfiguration.AUDIT_EXCLUDED_OPERATIONS)).thenReturn(new String[]{"GET:Version", "GET:Ping"});
        AuditFilter auditFilter = new AuditFilter();
        assertTrue(auditFilter.isOperationExcludedFromAudit("GET", "Version", configuration));
        assertTrue(auditFilter.isOperationExcludedFromAudit("get", "Version", configuration));
        assertTrue(auditFilter.isOperationExcludedFromAudit("GET", "Ping", configuration));
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Types", configuration));
    }

    @Test
    public void testVerifyNotExcludedOperations() {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        when(configuration.getStringArray(AtlasRepositoryConfiguration.AUDIT_EXCLUDED_OPERATIONS)).thenReturn(new String[]{"Version", "Ping"});
        AuditFilter auditFilter = new AuditFilter();
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Version", configuration));
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Ping", configuration));
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Types", configuration));
    }

    @Test
    public void testAudit() throws IOException, ServletException {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("api/atlas/types"));
        when(servletRequest.getMethod()).thenReturn("GET");
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.doFilter(servletRequest, servletResponse, filterChain);
        verify(filterChain).doFilter(servletRequest, servletResponse);

        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Version", configuration));
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Ping", configuration));
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Types", configuration));
    }

    @Test
    public void testAuditWithExcludedOperation() throws IOException, ServletException {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        when(configuration.getStringArray(AtlasRepositoryConfiguration.AUDIT_EXCLUDED_OPERATIONS)).thenReturn(new String[]{"GET:Version", "GET:Ping"});
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("api/atlas/version"));
        when(servletRequest.getMethod()).thenReturn("GET");
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.doFilter(servletRequest, servletResponse, filterChain);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void testAuditWithExcludedOperationInIncorrectFormat() throws IOException, ServletException {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        when(configuration.getStringArray(AtlasRepositoryConfiguration.AUDIT_EXCLUDED_OPERATIONS)).thenReturn(new String[]{"Version", "Ping"});
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("api/atlas/version"));
        when(servletRequest.getMethod()).thenReturn("GET");
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.doFilter(servletRequest, servletResponse, filterChain);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void testNullConfig() {
        AtlasRepositoryConfiguration.resetExcludedOperations();
        AuditFilter auditFilter = new AuditFilter();
        assertFalse(auditFilter.isOperationExcludedFromAudit("GET", "Version", null));
    }

}
