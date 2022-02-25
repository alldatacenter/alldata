/*
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
package org.apache.ambari.server.security.unsecured.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/connection_info")
public class ConnectionInfo {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionInfo.class);
    private static HashMap<String,String> response= new HashMap<>();
    private static Configuration conf;

    @Inject
    public static void init(Configuration instance){
        conf = instance;
        response.put(Configuration.SRVR_TWO_WAY_SSL.getKey(),String.valueOf(conf.isTwoWaySsl()));
    }

    @GET @ApiIgnore // until documented
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String,String> connectionType() {
        return response;
    }
}
