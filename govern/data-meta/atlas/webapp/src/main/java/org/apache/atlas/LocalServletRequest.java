/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class LocalServletRequest implements HttpServletRequest {
    private final String payload;

    LocalServletRequest(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String getAuthType() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Cookie[] getCookies() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long getDateHeader(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getHeader(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int getIntHeader(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getMethod() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getPathInfo() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getPathTranslated() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getContextPath() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getQueryString() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRemoteUser() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Principal getUserPrincipal() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRequestedSessionId() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRequestURI() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getServletPath() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public HttpSession getSession(boolean create) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public HttpSession getSession() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String changeSessionId() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void logout() throws ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Object getAttribute(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getCharacterEncoding() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int getContentLength() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public long getContentLengthLong() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getContentType() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getParameter(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Enumeration<String> getParameterNames() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String[] getParameterValues(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getProtocol() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getScheme() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getServerName() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int getServerPort() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRemoteAddr() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRemoteHost() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void setAttribute(String name, Object o) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void removeAttribute(String name) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Locale getLocale() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isSecure() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getRealPath(String path) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int getRemotePort() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getLocalName() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public String getLocalAddr() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public int getLocalPort() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public ServletContext getServletContext() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isAsyncStarted() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean isAsyncSupported() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new IllegalStateException("Not supported");
    }
}
