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

package org.apache.ranger.services.ozone.client;

import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class OzoneConnectionMgr {
    private static final Logger LOG = LoggerFactory.getLogger(OzoneConnectionMgr.class);

    protected ConcurrentMap<String, OzoneClient>    ozoneConnectionCache;
    protected ConcurrentMap<String, Boolean>        repoConnectStatusMap;


    public	OzoneConnectionMgr() {
        ozoneConnectionCache = new ConcurrentHashMap<String, OzoneClient>();
        repoConnectStatusMap = new ConcurrentHashMap<String, Boolean>();
    }


    public OzoneClient getOzoneConnection(final String serviceName, final String serviceType, final Map<String,String> configs) {
        OzoneClient ozoneClient  = null;

        if (serviceType != null) {
            // get it from the cache
            ozoneClient = ozoneConnectionCache.get(serviceName);
            if (ozoneClient == null) {
                if (configs != null) {

                    final Callable<OzoneClient> connectHive = new Callable<OzoneClient>() {
                        @Override
                        public OzoneClient call() throws Exception {
                            return new OzoneClient(serviceName, configs);
                        }
                    };
                    try {
                        ozoneClient = TimedEventUtil.timedTask(connectHive, 5, TimeUnit.SECONDS);
                    } catch(Exception e){
                        LOG.error("Error connecting ozone repository : "+
                                serviceName +" using config : "+ configs, e);
                    }

                    OzoneClient oldClient = null;
                    if (ozoneClient != null) {
                        oldClient = ozoneConnectionCache.putIfAbsent(serviceName, ozoneClient);
                    } else {
                        oldClient = ozoneConnectionCache.get(serviceName);
                    }

                    if (oldClient != null) {
                        // in the meantime someone else has put a valid client into the cache, let's use that instead.
                        if (ozoneClient != null) {
                            ozoneClient.close();
                        }
                        ozoneClient = oldClient;
                    }
                    repoConnectStatusMap.put(serviceName, true);
                } else {
                    LOG.error("Connection Config not defined for asset :"
                            + serviceName, new Throwable());
                }
            } else {
                try {
                    ozoneClient.getVolumeList(null);
                } catch(Exception e) {
                    ozoneConnectionCache.remove(serviceName);
                    /*
                     * There is a possibility that some other thread is also using this connection that we are going to close but
                     * presumably the connection is bad which is why we are closing it, so damage should not be much.
                     */
                    ozoneClient.close();
                    ozoneClient = getOzoneConnection(serviceName,serviceType,configs);
                }
            }
        } else {
            LOG.error("Asset not found with name "+serviceName, new Throwable());
        }
        return ozoneClient;
    }
}
