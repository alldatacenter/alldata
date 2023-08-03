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

import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class OzoneResourceMgr {

    private static final Logger LOG = LoggerFactory.getLogger(OzoneResourceMgr.class);

    private static final String VOLUME     = "volume";
    private static final String BUCKET     = "bucket";
    private static final String KEY        = "key";


    public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
        Map<String, Object> ret = null;

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> OzoneResourceMgr.connectionTest ServiceName: "+ serviceName + "Configs" + configs );
        }

        try {
            ret = OzoneClient.connectionTest(serviceName, configs);
        } catch (HadoopException e) {
            LOG.error("<== OzoneResourceMgr.connectionTest Error: " + e);
            throw e;
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneResourceMgr.connectionTest Result : "+ ret  );
        }

        return ret;
    }

    public static List<String> getOzoneResources(String serviceName, String serviceType, Map<String, String> configs, ResourceLookupContext context) throws Exception  {

        String userInput                       = context.getUserInput();
        String resource	                       = context.getResourceName();
        Map<String, List<String>> resourceMap  = context.getResources();
        List<String> resultList 	           = null;
        List<String> volumeList                = null;
        List<String> bucketList	               = null;
        List<String> keyList	               = null;
        String volumePrefix                    = null;
        String bucketPrefix	                   = null;
        String keyPrefix	                   = null;


        if(LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneResourceMgr.getOzoneResources()  UserInput: \""+ userInput  + "\" resource : " + resource + " resourceMap: "  + resourceMap);
        }

        if ( userInput != null && resource != null) {
            if ( resourceMap != null  && !resourceMap.isEmpty() ) {
                volumeList = resourceMap.get(VOLUME);
                bucketList = resourceMap.get(BUCKET);
                keyList = resourceMap.get(KEY);
            }
            switch (resource.trim().toLowerCase()) {
                case VOLUME:
                    volumePrefix = userInput;
                    break;
                case BUCKET:
                    bucketPrefix = userInput;
                    break;
                case KEY:
                    keyPrefix    = userInput;
                    break;
                default:
                    break;
            }
        }

        if (serviceName != null && userInput != null) {
            try {

                if(LOG.isDebugEnabled()) {
                    LOG.debug("==> OzoneResourceMgr.getOzoneResources() UserInput: "+ userInput  + " configs: " + configs + " volumeList: "  + volumeList + " bucketList: "
                            + bucketList + " keyList: " + keyList );
                }

                final OzoneClient ozoneClient = new OzoneConnectionMgr().getOzoneConnection(serviceName, serviceType, configs);

                Callable<List<String>> callableObj = null;
                final String finalVolPrefix;
                final String finalBucketPrefix;
                final String finalKeyPrefix;

                final List<String> finalvolumeList = volumeList;
                final List<String> finalbucketList = bucketList;

                if ( ozoneClient != null) {
                    if ( volumePrefix != null
                            && !volumePrefix.isEmpty()){
                        // get the DBList for given Input
                        finalVolPrefix = volumePrefix;
                        callableObj = new Callable<List<String>>() {
                            @Override
                            public List<String> call() {
                                return ozoneClient.getVolumeList(finalVolPrefix);
                            }
                        };
                    } else if ( bucketPrefix != null
                            && !bucketPrefix.isEmpty()) {
                        // get  ColumnList for given Input
                        finalBucketPrefix = bucketPrefix;
                        callableObj = new Callable<List<String>>() {

                            @Override
                            public List<String> call() {
                                return ozoneClient.getBucketList(finalBucketPrefix,
                                        finalvolumeList);
                            }
                        };
                    } else if ( keyPrefix != null
                            && !keyPrefix.isEmpty()) {
                        // get  ColumnList for given Input
                       finalKeyPrefix = keyPrefix;

                        callableObj = new Callable<List<String>>() {
                            @Override
                            public List<String> call() {
                                return ozoneClient.getKeyList(finalKeyPrefix,
                                        finalvolumeList,
                                        finalbucketList);
                            }
                        };
                    }
                    if (callableObj != null) {
                        synchronized (ozoneClient) {
                            resultList = TimedEventUtil.timedTask(callableObj, 5,
                                    TimeUnit.SECONDS);
                        }
                    } else {
                        LOG.error("Could not initiate at timedTask");
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to get ozone resources.", e);
                throw e;
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneResourceMgr.getOzoneResources() UserInput: "+ userInput  + " configs: " + configs + " volumeList: "  + volumeList + " bucketList: "
                    + bucketList + " keyList: " + keyList + "Result :" + resultList );

        }
        return resultList;

    }

}
