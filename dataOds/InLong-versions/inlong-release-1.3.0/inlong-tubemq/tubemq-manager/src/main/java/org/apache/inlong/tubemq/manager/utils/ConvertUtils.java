/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.manager.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.inlong.tubemq.manager.service.TubeConst.OP_MODIFY;
import static org.apache.inlong.tubemq.manager.service.TubeConst.REBALANCE_GROUP;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.tubemq.manager.controller.cluster.vo.ClusterVo;
import org.apache.inlong.tubemq.manager.controller.topic.request.RebalanceConsumerReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.RebalanceGroupReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;

@Slf4j
public class ConvertUtils {

    private static final Gson gson = new Gson();

    public static String convertReqToQueryStr(Object req) {
        List<String> queryList = new ArrayList<>();
        try {
            List<Field[]> fieldsList = getAllFileds(req);
            getQueryFields(req, queryList, fieldsList);
        } catch (Exception e) {
            log.error("exception occurred while parsing object {}", gson.toJson(req), e);
            return StringUtils.EMPTY;
        }
        return StringUtils.join(queryList, "&");
    }

    private static void getQueryFields(Object req, List<String> queryList, List<Field[]> fieldsList)
            throws IllegalAccessException, UnsupportedEncodingException {
        for (Object fields : fieldsList) {
            Field[] f = (Field[]) fields;
            for (Field field : f) {
                field.setAccessible(true);
                Object o = field.get(req);
                String value;
                // convert list to json string
                if (o == null) {
                    continue;
                }
                if (o instanceof List) {
                    value = gson.toJson(o);
                } else {
                    value = o.toString();
                }
                queryList.add(field.getName() + "=" + URLEncoder.encode(
                        value, UTF_8.toString()));
            }
        }
    }

    private static List<Field[]> getAllFileds(Object req) throws ClassNotFoundException {
        Class<?> clz = ClassUtils.getClass(ClassUtils.getName(req));
        Field[] declaredFields = clz.getDeclaredFields();
        List<Field[]> fieldsList = new ArrayList<>();
        fieldsList.add(declaredFields);
        List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(clz);
        allSuperclasses.forEach(clazz -> fieldsList.add(clazz.getDeclaredFields()));
        return fieldsList;
    }

    public static RebalanceConsumerReq convertToRebalanceConsumerReq(RebalanceGroupReq req, String consumerId) {
        RebalanceConsumerReq consumerReq = new RebalanceConsumerReq();
        consumerReq.setConsumerId(consumerId);
        consumerReq.setConfModAuthToken(req.getConfModAuthToken());
        consumerReq.setGroupName(req.getGroupName());
        consumerReq.setModifyUser(req.getModifyUser());
        consumerReq.setReJoinWait(req.getReJoinWait());
        consumerReq.setType(OP_MODIFY);
        consumerReq.setMethod(REBALANCE_GROUP);
        return consumerReq;
    }

    public static String covertMapToQueryString(Map<String, String> requestMap) throws Exception {
        List<String> queryList = new ArrayList<>();

        for (Map.Entry<String, String> entry : requestMap.entrySet()) {
            queryList.add(entry.getKey() + "=" + URLEncoder.encode(
                    entry.getValue(), UTF_8.toString()));
        }
        return StringUtils.join(queryList, "&");
    }

    public static ClusterVo convertToClusterVo(ClusterEntry clusterEntry,
                                               List<MasterEntry> masterEntries, ClusterVo clusterVo) {
        ClusterVo cluster = new ClusterVo();
        cluster.setClusterId(clusterEntry.getClusterId());
        cluster.setMasterEntries(masterEntries);
        cluster.setClusterName(clusterEntry.getClusterName());
        cluster.setReloadBrokerSize(clusterEntry.getReloadBrokerSize());
        cluster.setBrokerCount(clusterVo.getBrokerCount());
        cluster.setTopicCount(clusterVo.getTopicCount());
        cluster.setPartitionCount(clusterVo.getPartitionCount());
        cluster.setConsumerGroupCount(clusterVo.getConsumerGroupCount());
        cluster.setConsumerCount(clusterVo.getConsumerCount());
        cluster.setStoreCount(clusterVo.getStoreCount());
        return cluster;
    }

}
