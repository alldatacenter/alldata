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

package org.apache.inlong.tubemq.server.broker.web;

import static com.google.common.base.Preconditions.checkArgument;
import org.apache.inlong.tubemq.server.Server;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Broker's http server.
 */
public class WebServer implements Server {

    private String hostname = "0.0.0.0";
    private int port = 8080;
    private org.eclipse.jetty.server.Server srv;
    private TubeBroker broker;

    public WebServer(String hostname, int port, TubeBroker broker) {
        this.hostname = hostname;
        this.port = port;
        this.broker = broker;
    }

    @Override
    public void start() throws Exception {
        srv = new org.eclipse.jetty.server.Server(this.port);
        ServletContextHandler servletContext =
                new ServletContextHandler(srv, "/", ServletContextHandler.SESSIONS);

        servletContext.addServlet(new ServletHolder(new BrokerAdminServlet(broker)), "/*");
        srv.start();
        checkArgument(srv.getHandler().equals(servletContext));
    }

    @Override
    public void stop() throws Exception {
        srv.stop();
    }
}
