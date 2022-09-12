/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.simplemvc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.web.simplemvc.conf.ConfigFileParser;
import org.apache.inlong.tubemq.server.master.web.simplemvc.conf.WebConfig;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebApiServlet extends HttpServlet {
    public static final int MAX_MULTIPART_POST_DATA_SIZE = 67108864; // 64M, could be bigger if needed
    private static final Logger logger = LoggerFactory.getLogger(WebFilter.class);
    private static final String DEFAULT_CONFIG_PATH = "/WEB-INF/simple-mvc.xml";
    private static final String DEFAULT_LOG_CONFIG_PATH = "/WEB-INF/log4j.xml";
    private MultipartConfigElement multipartConfig;
    private String configFilePath;
    private File configFile;
    private WebConfig config;
    private RequestDispatcher dispatcher;

    public WebApiServlet(WebConfig config) {
        this.config = config;
    }

    protected void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req instanceof MultipartHttpServletRequest) {
            // We need to manually fix config if it's "form-data" by jetty's requirement,
            // as this jetty is an embedded one, there's no XML config to read
            req.setAttribute(Request.MULTIPART_CONFIG_ELEMENT, multipartConfig);
        }

        String charset = (req.getCharacterEncoding() == null)
                ? TBaseConstants.META_DEFAULT_CHARSET_NAME : req.getCharacterEncoding();
        resp.setCharacterEncoding(charset);
        RequestContext context = new RequestContext(this.config, req, resp);
        if (this.config.containsType(context.requestType())) {
            if (dispatcher == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                try {
                    dispatcher.processRequest(context);
                } catch (Throwable t) {
                    logger.error("", t);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            resp.flushBuffer();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            initLogSystem(config);
            if (this.config == null) {
                if (this.configFilePath == null) {
                    String filePath = config.getInitParameter("configFile");
                    if (TStringUtils.isEmpty(filePath)) {
                        filePath = DEFAULT_CONFIG_PATH;
                    }
                    this.configFilePath = filePath;
                }
                URL configFileURL =
                        config.getServletContext().getResource(this.configFilePath);
                if (configFileURL == null) {
                    throw new ServletException(new StringBuilder(256)
                            .append("can not found config file:")
                            .append(this.configFilePath).toString());
                }
                this.configFile = new File(configFileURL.toURI());
                ConfigFileParser configParser = new ConfigFileParser(this.configFile);
                this.config = configParser.parse();
            }
            checkConfig(this.config, config.getServletContext());
            this.dispatcher = new RequestDispatcher(this.config);
            this.dispatcher.init();
            this.multipartConfig = new MultipartConfigElement(null, 0,
                    MAX_MULTIPART_POST_DATA_SIZE, MAX_MULTIPART_POST_DATA_SIZE);
        } catch (ServletException se) {
            throw se;
        } catch (Throwable t) {
            logger.error("Dispatcher start failed!", t);
            throw new ServletException(t);
        }
    }

    private void checkConfig(WebConfig config, ServletContext servletContext) throws Exception {
        URL resourcesURL =
                servletContext.getResource(config.getResourcePath());
        if (resourcesURL == null) {
            throw new ServletException(new StringBuilder(256)
                    .append("Invalid resources path:")
                    .append(config.getResourcePath()).toString());
        }
        config.setResourcePath(resourcesURL.getPath());
        URL templatesURL = servletContext.getResource(config.getTemplatePath());
        if (templatesURL == null) {
            throw new ServletException(new StringBuilder(256)
                    .append("Invalid templates path:")
                    .append(config.getTemplatePath()).toString());
        }
        config.setTemplatePath(templatesURL.getPath());

        if (TStringUtils.isNotEmpty(config.getVelocityConfigFilePath())) {
            URL velocityConfigFilePath =
                    servletContext.getResource(config.getVelocityConfigFilePath());
            if (velocityConfigFilePath != null) {
                config.setVelocityConfigFilePath(velocityConfigFilePath.getPath());
            } else {
                logger.warn(new StringBuilder(256)
                        .append("Invalid velocity config file path:")
                        .append(config.getVelocityConfigFilePath()).toString());
                config.setVelocityConfigFilePath(null);
            }
        }
    }

    private void initLogSystem(ServletConfig config) throws Exception {
        String filePath = config.getInitParameter("logConfigFile");
        if (TStringUtils.isEmpty(filePath) && !this.config.isStandalone()) {
            filePath = DEFAULT_LOG_CONFIG_PATH;
        }
        if (TStringUtils.isNotEmpty(filePath)) {
            ServletContext servletContext = config.getServletContext();
            if (servletContext != null) {
                URL logConfigFileURL = servletContext.getResource(filePath);
                if (logConfigFileURL != null) {
                    PropertyConfigurator.configure(logConfigFileURL);
                }
            }
        }
    }
}
