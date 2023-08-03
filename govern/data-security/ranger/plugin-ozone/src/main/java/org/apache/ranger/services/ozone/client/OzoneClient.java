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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.ozone.client.OzoneBucket;
import org.apache.hadoop.ozone.client.OzoneClientFactory;
import org.apache.hadoop.ozone.client.OzoneKey;
import org.apache.hadoop.ozone.client.OzoneVolume;
import org.apache.ranger.plugin.client.BaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class OzoneClient extends BaseClient {

    private static final Logger LOG = LoggerFactory.getLogger(OzoneClient.class);

    private static final String ERR_MSG = "You can still save the repository and start creating "
            + "policies, but you would not be able to use autocomplete for "
            + "resource names. Check ranger_admin.log for more info.";

    private Configuration conf;
    private org.apache.hadoop.ozone.client.OzoneClient ozoneClient = null;

    public OzoneClient(String serviceName, Map<String,String> connectionProperties) throws Exception{
        super(serviceName,connectionProperties, "ozone-client");
        conf = new Configuration();
        Set<String> rangerInternalPropertyKeys = getConfigHolder().getRangerInternalPropertyKeys();
        for (Map.Entry<String, String> entry: connectionProperties.entrySet())  {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!rangerInternalPropertyKeys.contains(key) && value != null) {
                conf.set(key, value);
            }
        }
        ozoneClient = OzoneClientFactory.getRpcClient(new OzoneConfiguration(conf));
    }

    public void close() {
        try {
            ozoneClient.close();
        } catch (IOException e) {
            LOG.error("Unable to close Ozone Client connection", e);
        }

    }

    public List<String> getVolumeList(String volumePrefix) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> OzoneClient getVolume volumePrefix : " + volumePrefix);
        }

        List<String> ret = new ArrayList<String>();
        try {
            if (ozoneClient != null) {
                Iterator<? extends OzoneVolume> ozoneVolList = ozoneClient.getObjectStore().listVolumesByUser(conf.get("username"), volumePrefix, null);
                if (ozoneVolList != null) {
                    while (ozoneVolList.hasNext()) {
                        ret.add(ozoneVolList.next().getName());
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to get Volume List");
            if (LOG.isDebugEnabled()) {
                LOG.debug("<== OzoneClient.getVolumeList() Error : " , e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneClient.getVolumeList(): " + ret);
        }
        return ret;
    }

    public List<String> getBucketList(String bucketPrefix, List<String> finalvolumeList) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> OzoneClient getBucketList bucketPrefix : " + bucketPrefix);
        }
        List<String> ret = new ArrayList<String>();
        try {
            if (ozoneClient != null) {
                if (finalvolumeList != null && !finalvolumeList.isEmpty()) {
                    for (String ozoneVol : finalvolumeList) {
                        Iterator<? extends OzoneBucket> ozoneBucketList = ozoneClient.getObjectStore().getVolume(ozoneVol).listBuckets(bucketPrefix);
                        if (ozoneBucketList != null) {
                            while (ozoneBucketList.hasNext()) {
                                ret.add(ozoneBucketList.next().getName());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to get Volume List");
            if (LOG.isDebugEnabled()) {
                LOG.debug("<== OzoneClient.getVolumeList() Error : " , e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneClient.getVolumeList(): " + ret);
        }
        return ret;
    }

    public List<String> getKeyList(String keyPrefix, List<String> finalvolumeList, List<String> finalbucketList) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> OzoneClient getKeyList keyPrefix : " + keyPrefix);
        }
        List<String> ret = new ArrayList<String>();
        try {
            if (ozoneClient != null) {
                if (finalvolumeList != null && !finalvolumeList.isEmpty()) {
                    for (String ozoneVol : finalvolumeList) {
                        Iterator<? extends OzoneBucket> ozoneBucketList = ozoneClient.getObjectStore().getVolume(ozoneVol).listBuckets(null);
                        if (ozoneBucketList != null) {
                            while (ozoneBucketList.hasNext()) {
                                String bucketName = ozoneBucketList.next().getName();
                                if (finalbucketList.contains(bucketName)) {
                                    Iterator<? extends OzoneKey> ozoneKeyList = ozoneBucketList.next().listKeys(keyPrefix);
                                    if (ozoneKeyList != null) {
                                        while (ozoneKeyList.hasNext()) {
                                            ret.add(ozoneKeyList.next().getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to get Volume List");
            if (LOG.isDebugEnabled()) {
                LOG.debug("<== OzoneClient.getVolumeList() Error : " , e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== OzoneClient.getVolumeList(): " + ret);
        }
        return ret;
    }

    public static Map<String, Object> connectionTest(String serviceName,
                                                     Map<String, String> connectionProperties) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        OzoneClient connectionObj = null;
        boolean connectivityStatus = false;
        List<String> testResult = null;

        try {
            connectionObj = new OzoneClient(serviceName,	connectionProperties);
            if (connectionObj != null) {
                testResult = connectionObj.getVolumeList(null);
                if (testResult != null && testResult.size() != 0) {
                    connectivityStatus = true;
                }
                if (connectivityStatus) {
                    String successMsg = "ConnectionTest Successful";
                    generateResponseDataMap(connectivityStatus, successMsg, successMsg,
                            null, null, responseData);
                } else {
                    String failureMsg = "Unable to retrieve any volumes using given parameters.";
                    generateResponseDataMap(connectivityStatus, failureMsg, failureMsg + ERR_MSG,
                            null, null, responseData);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (connectionObj != null) {
                connectionObj.close();
            }
        }

        return responseData;
    }
}
