/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.config.interceptor;

import datart.core.common.Application;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BasicValidRequestInterceptor implements HandlerInterceptor {

    private String apiPrePath = null;

    private String contextPath = null;

    private static final String resourcePath = "/resources";

    private static final String staticPath = "/static";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!isValidRequest(request)) {
            request.getRequestDispatcher("/").forward(request, response);
            return false;
        }
        return true;
    }

    private boolean isValidRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        contextPath = getContextPath();
        if (requestURI.startsWith(contextPath)) {
            requestURI = StringUtils.removeStart(requestURI, contextPath);
            requestURI = StringUtils.prependIfMissing(requestURI, "/");
        }
        return requestURI.startsWith(getApiPrePath())
                || requestURI.equals("/")
                || requestURI.equals("/index.html")
                || requestURI.equals("/favicon.ico")
                || requestURI.equals("/manifest.json")
                || requestURI.equals("/editor.worker.js")
                || requestURI.startsWith(resourcePath)
                || requestURI.startsWith("/swagger")
                || requestURI.startsWith("/webjars")
                || requestURI.startsWith("/custom-chart-plugins")
                || requestURI.startsWith("/antd")
                || requestURI.startsWith("/v2/")
                || requestURI.startsWith("/share")
                || requestURI.startsWith(staticPath);
    }

    private String getApiPrePath() {
        if (apiPrePath == null) {
            apiPrePath = Application.getProperty("datart.server.path-prefix");
        }
        return apiPrePath;
    }

    private String getContextPath() {
        if (contextPath == null) {
            contextPath = Application.getServerPrefix();
        }
        return contextPath;
    }

}
