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

package org.apache.inlong.tubemq.server.master.web;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.REQUEST;
import java.util.EnumSet;
import org.apache.inlong.tubemq.server.Server;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.web.action.layout.Default;
import org.apache.inlong.tubemq.server.master.web.action.screen.Master;
import org.apache.inlong.tubemq.server.master.web.action.screen.Tubeweb;
import org.apache.inlong.tubemq.server.master.web.action.screen.Webapi;
import org.apache.inlong.tubemq.server.master.web.action.screen.cluster.ClusterManager;
import org.apache.inlong.tubemq.server.master.web.action.screen.config.BrokerDetail;
import org.apache.inlong.tubemq.server.master.web.action.screen.config.BrokerList;
import org.apache.inlong.tubemq.server.master.web.action.screen.config.TopicDetail;
import org.apache.inlong.tubemq.server.master.web.action.screen.config.TopicList;
import org.apache.inlong.tubemq.server.master.web.action.screen.consume.Detail;
import org.apache.inlong.tubemq.server.master.web.simplemvc.WebApiServlet;
import org.apache.inlong.tubemq.server.master.web.simplemvc.WebFilter;
import org.apache.inlong.tubemq.server.master.web.simplemvc.conf.WebConfig;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer implements Server {

    private final MasterConfig masterConfig;
    private org.eclipse.jetty.server.Server srv;
    private TMaster master;

    public WebServer(final MasterConfig masterConfig, TMaster master) {
        this.masterConfig = masterConfig;
        this.master = master;
    }

    @Override
    public void start() throws Exception {
        WebConfig webConfig = new WebConfig();
        webConfig.setActionPackage("org.apache.inlong.tubemq.server.master.web.action");
        webConfig.setResourcePath("/");
        webConfig.setVelocityConfigFilePath("/velocity.properties");
        webConfig.setStandalone(true);
        registerActions(webConfig);
        registerTools(webConfig);
        srv = new org.eclipse.jetty.server.Server(masterConfig.getWebPort());
        ServletContextHandler servletContext = new ServletContextHandler(srv,
                        "/", ServletContextHandler.SESSIONS);
        servletContext.addFilter(new FilterHolder(
                new MasterStatusCheckFilter(master)), "/*", EnumSet.of(REQUEST, ASYNC));
        servletContext.addFilter(new FilterHolder(
                new UserAuthFilter()), "/*", EnumSet.of(REQUEST, ASYNC));
        servletContext.addFilter(new FilterHolder(
                new WebFilter()), "/*", EnumSet.of(REQUEST, ASYNC));
        // This Servlet processes WebAPI requests
        ServletHolder servletHolder = new ServletHolder(new WebApiServlet(webConfig));
        servletHolder.setInitParameter("dirAllowed", "false");
        servletContext.addServlet(servletHolder, "/");
        // This is Pass-Through for static resources requests
        ServletHolder staticHolder = new ServletHolder(new DefaultServlet());
        staticHolder.setInitParameter("dirAllowed", "false");
        servletContext.addServlet(staticHolder, "/assets/*");
        servletContext.setResourceBase(masterConfig.getWebResourcePath());
        srv.start();
        if (!srv.getHandler().equals(servletContext)) {
            throw new Exception("servletContext is not a handler!");
        }
    }

    @Override
    public void stop() throws Exception {
        srv.stop();
    }

    private void registerActions(WebConfig config) {
        config.registerAction(new Detail(this.master));
        config.registerAction(new BrokerDetail(this.master));
        config.registerAction(new TopicDetail(this.master));
        config.registerAction(new TopicList(this.master));
        config.registerAction(new ClusterManager(this.master));
        config.registerAction(new BrokerList(this.master));
        config.registerAction(new Master(this.master));
        config.registerAction(new Webapi(this.master));
        config.registerAction(new Tubeweb(this.master));
        config.registerAction(new Default(this.master));
    }

    private void registerTools(WebConfig config) {
        config.registerTool("dateTool", new DateTool());
        config.registerTool("numericTool", new NumberTool());
    }
}

