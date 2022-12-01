/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */

package org.apache.atlas.web.filters;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.util.EventListener;
import java.util.Map;


/**
 */
public class NullServletContext implements ServletContext {



    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
    }


    public boolean setInitParameter(String name, String value) {
        return false;
    }


    public void setAttribute(String name, Object object) {
    }


    public void removeAttribute(String name) {
    }


    public void log(String message, Throwable throwable) {
    }


    public void log(Exception exception, String msg) {
    }


    public void log(String msg) {
    }


    public String getVirtualServerName() {
        return null;
    }


    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }


    public Enumeration<Servlet> getServlets() {
        return null;
    }


    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }


    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }


    public Enumeration<String> getServletNames() {
        return null;
    }


    public String getServletContextName() {
        return null;
    }


    public Servlet getServlet(String name) throws ServletException {
        return null;
    }


    public String getServerInfo() {
        return null;
    }


    public Set<String> getResourcePaths(String path) {
        return null;
    }


    public InputStream getResourceAsStream(String path) {
        return null;
    }


    public URL getResource(String path) throws MalformedURLException {
        return null;
    }


    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }


    public String getRealPath(String path) {
        return null;
    }


    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }


    public int getMinorVersion() {
        return 0;
    }


    public String getMimeType(String file) {
        return null;
    }


    public int getMajorVersion() {
        return 0;
    }


    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }


    public Enumeration<String> getInitParameterNames() {
        return null;
    }


    public String getInitParameter(String name) {
        return null;
    }


    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }


    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }


    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }


    public int getEffectiveMinorVersion() {
        return 0;
    }


    public int getEffectiveMajorVersion() {
        return 0;
    }


    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }


    public String getContextPath() {
        return null;
    }


    public ServletContext getContext(String uripath) {
        return null;
    }


    public ClassLoader getClassLoader() {
        return null;
    }


    public Enumeration<String> getAttributeNames() {
        return null;
    }


    public Object getAttribute(String name) {
        return null;
    }


    public void declareRoles(String... roleNames) {
    }


    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        return null;
    }


    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        return null;
    }


    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        return null;
    }


    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }


    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        return null;
    }


    public javax.servlet.ServletRegistration.Dynamic addServlet(
            String servletName, String className) {
        return null;
    }


    public void addListener(Class<? extends EventListener> listenerClass) {
    }


    public <T extends EventListener> void addListener(T t) {
    }


    public void addListener(String className) {
    }


    public Dynamic addFilter(String filterName,
                             Class<? extends Filter> filterClass) {
        return null;
    }


    public Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }


    public Dynamic addFilter(String filterName, String className) {
        return null;
    }


}
