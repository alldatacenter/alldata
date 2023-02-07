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

package org.apache.inlong.tubemq.manager.controller.cluster;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.cluster.dto.ClusterDto;
import org.apache.inlong.tubemq.manager.controller.cluster.request.AddClusterReq;
import org.apache.inlong.tubemq.manager.controller.cluster.request.DeleteClusterReq;
import org.apache.inlong.tubemq.manager.controller.cluster.request.SwitchClusterReq;
import org.apache.inlong.tubemq.manager.controller.cluster.vo.ClusterVo;
import org.apache.inlong.tubemq.manager.controller.group.result.ConsumerGroupInfoRes;
import org.apache.inlong.tubemq.manager.controller.group.result.ConsumerInfoRes;
import org.apache.inlong.tubemq.manager.controller.topic.result.TopicQueryRes;
import org.apache.inlong.tubemq.manager.controller.topic.result.TopicViewRes;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.apache.inlong.tubemq.manager.service.interfaces.ClusterService;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.utils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.apache.inlong.tubemq.manager.service.TubeConst.SUCCESS_CODE;

@RestController
@RequestMapping(path = "/v1/cluster")
@Slf4j
public class ClusterController {

    private final Gson gson = new Gson();
    private final TubeMQResult result = new TubeMQResult();

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private MasterService masterService;

    @PostMapping("")
    public @ResponseBody TubeMQResult clusterMethodProxy(@RequestParam String method, @RequestBody String req) {
        switch (method) {
            case TubeConst.ADD:
                return addNewCluster(gson.fromJson(req, AddClusterReq.class));
            case TubeConst.DELETE:
                return deleteCluster(gson.fromJson(req, DeleteClusterReq.class));
            case TubeConst.MODIFY:
                return changeCluster(gson.fromJson(req, ClusterDto.class));
            case TubeConst.SWITCH:
                return masterService.baseRequestMaster(gson.fromJson(req, SwitchClusterReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    /**
     * change cluster info
     *
     * @param clusterDto
     * @return
     */
    private TubeMQResult changeCluster(ClusterDto clusterDto) {
        if (!clusterDto.legal()) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        return clusterService.modifyCluster(clusterDto);
    }

    /**
     * add a new cluster, should provide a master node
     */
    public TubeMQResult addNewCluster(AddClusterReq req) {
        // 1. validate params
        if (!req.legal()) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        List<MasterEntry> masterEntries = req.getMasterEntries();
        for (MasterEntry masterEntry : masterEntries) {
            TubeMQResult checkResult = masterService.checkMasterNodeStatus(masterEntry.getIp(),
                    masterEntry.getWebPort());
            if (checkResult.getErrCode() != SUCCESS_CODE) {
                return TubeMQResult.errorResult("please check master ip and webPort");
            }
        }

        // 2. add cluster and master node
        clusterService.addClusterAndMasterNode(req);
        return new TubeMQResult();
    }

    /**
     * query cluster info, if no clusterId is passed, return all clusters
     *
     * @param clusterId
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public TubeMQResult queryCluster(@RequestParam(required = false) Integer clusterId,
            @RequestParam(required = false) String clusterName, @RequestParam(required = false) String masterIp) {
        TubeMQResult result = new TubeMQResult();
        if (clusterId == null && clusterName == null && masterIp == null) {
            return queryAllClusterVo();
        }
        if (clusterId != null) {
            ClusterEntry clusterEntry = clusterService.getOneCluster(clusterId);
            if (clusterEntry == null) {
                return TubeMQResult.errorResult("no such cluster with id " + clusterId);
            }
            List<MasterEntry> masterNodes = masterService.getMasterNodes(clusterEntry.getClusterId());
            ClusterVo allCount = getCountInCluster(clusterId);
            result.setData(Lists.newArrayList(ConvertUtils.convertToClusterVo(clusterEntry, masterNodes, allCount)));
            return result;
        }
        if (clusterName != null) {
            result = queryClusterByClusterName(clusterName);
            return result;
        }
        if (masterIp != null) {
            result = queryClusterByMasterIp(masterIp);
            return result;
        }
        return result;
    }

    /**
     * get all cluster info
     *
     * @return
     */
    private TubeMQResult queryAllClusterVo() {
        TubeMQResult result = new TubeMQResult();
        List<ClusterEntry> allClusters = clusterService.getAllClusters();
        List<ClusterVo> clusterVos = Lists.newArrayList();
        for (ClusterEntry cluster : allClusters) {
            List<MasterEntry> masterNodes = masterService.getMasterNodes(cluster.getClusterId());
            ClusterVo allCount = getCountInCluster(Integer.valueOf((int) cluster.getClusterId()));
            ClusterVo clusterVo = ConvertUtils.convertToClusterVo(cluster, masterNodes, allCount);
            clusterVos.add(clusterVo);
        }
        result.setData(clusterVos);
        return result;
    }

    /**
     * delete a new cluster
     */
    public TubeMQResult deleteCluster(DeleteClusterReq req) {
        // 1. validate params
        if (req.getClusterId() == null || StringUtils.isEmpty(req.getToken())) {
            return TubeMQResult.errorResult("please input clusterId and token");
        }
        // 2. delete cluster
        MasterEntry masterNode = masterService.getMasterNode(req.getClusterId());
        if (!req.getToken().equals(masterNode.getToken())) {
            return TubeMQResult.errorResult("please enter the correct token");
        }
        clusterService.deleteCluster(req.getClusterId());
        return new TubeMQResult();
    }

    /**
     * query cluster info
     */
    @RequestMapping(value = "/query", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryInfo(
            @RequestParam Map<String, String> queryBody) throws Exception {
        String url = masterService.getQueryUrl(queryBody);
        return masterService.queryMaster(url);
    }

    /**
     * get all count
     *
     * @param clusterId
     */
    public ClusterVo getCountInCluster(Integer clusterId) {
        ClusterVo clusterVo = new ClusterVo();
        int brokerSize = getBrokerSize(clusterId);
        ClusterVo countVo = getTopicAndPartitionCount(clusterId);
        int consumerGroupCount = getConsumerGroupCount(clusterId);
        int consumerCount = getConsumerCount(clusterId);
        int storeCount = getStoreCount(clusterId);
        clusterVo.setBrokerCount(brokerSize);
        clusterVo.setTopicCount(countVo.getTopicCount());
        clusterVo.setPartitionCount(countVo.getPartitionCount());
        clusterVo.setConsumerGroupCount(consumerGroupCount);
        clusterVo.setConsumerCount(consumerCount);
        clusterVo.setStoreCount(storeCount);
        return clusterVo;
    }

    /**
     * query borker size
     *
     * @param clusterId
     */
    public int getBrokerSize(Integer clusterId) {
        String queryUrl = masterService.getQueryCountUrl(clusterId, TubeConst.BROKER_RUN_STATUS);
        String queryData = masterService.queryMaster(queryUrl);
        JsonObject jsonObject = gson.fromJson(queryData, JsonObject.class);
        JsonElement count = jsonObject.get("count");
        return gson.fromJson(count, int.class);
    }

    /**
     * query topic and partition count
     *
     * @param clusterId
     */
    public ClusterVo getTopicAndPartitionCount(Integer clusterId) {
        ClusterVo clusterVo = new ClusterVo();
        String url = masterService.getQueryCountUrl(clusterId, TubeConst.TOPIC_CONFIG_INFO);
        String queryMaster = masterService.queryMaster(url);
        JsonObject jsonObject = gson.fromJson(queryMaster, JsonObject.class);
        JsonElement data = jsonObject.get("data");
        JsonElement dataCount = jsonObject.get("count");
        Integer topicSize = gson.fromJson(dataCount, Integer.class);
        JsonArray jsonData = gson.fromJson(data, JsonArray.class);
        int partitionCount = 0;
        List<TopicQueryRes> topicQueryResList = gson.fromJson(jsonData.toString(),
                new TypeToken<List<TopicQueryRes>>() {
                }.getType());
        for (TopicQueryRes topicQueryRes : topicQueryResList) {
            String totalCfgNumPart = topicQueryRes.getTotalCfgNumPart();
            partitionCount = partitionCount + (int) Math.ceil(Double.parseDouble(totalCfgNumPart));
        }
        clusterVo.setTopicCount(topicSize);
        clusterVo.setPartitionCount(partitionCount);
        return clusterVo;
    }

    /**
     * query Consumer group count
     *
     * @param clusterId
     * @return
     */
    public int getConsumerGroupCount(Integer clusterId) {
        String queryUrl = masterService.getQueryCountUrl(clusterId, TubeConst.QUERY_CONSUMER_GROUP_INFO);
        int consumerGroupCount = 0;
        String groupData = masterService.queryMaster(queryUrl);
        JsonObject jsonObject = gson.fromJson(groupData, JsonObject.class);
        JsonElement data = jsonObject.get("data");
        JsonArray jsonData = gson.fromJson(data, JsonArray.class);
        List<ConsumerGroupInfoRes> groupList = gson.fromJson(jsonData.toString(),
                new TypeToken<List<ConsumerGroupInfoRes>>() {
                }.getType());
        for (ConsumerGroupInfoRes groupInfoRes : groupList) {
            consumerGroupCount = consumerGroupCount + (int) Math.ceil(groupInfoRes.getGroupCount());
        }
        return consumerGroupCount;
    }

    /**
     * query consumer count
     */
    public int getConsumerCount(Integer clusterId) {
        String queryUrl = masterService.getQueryCountUrl(clusterId, TubeConst.QUERY_CONSUMER_INFO);
        String queryMaster = masterService.queryMaster(queryUrl);
        JsonObject jsonObject = gson.fromJson(queryMaster, JsonObject.class);
        JsonElement data = jsonObject.get("data");
        JsonArray jsonData = gson.fromJson(data, JsonArray.class);
        int consumerCount = 0;
        List<ConsumerInfoRes> topicViewResList = gson.fromJson(jsonData.toString(),
                new TypeToken<List<ConsumerInfoRes>>() {
                }.getType());
        for (ConsumerInfoRes consumerInfoRes : topicViewResList) {
            consumerCount = (int) Math.ceil(consumerInfoRes.getConsumerNum());
        }
        return consumerCount;
    }

    /**
     * query store count
     */
    public int getStoreCount(Integer clusterId) {
        String queryUrl = masterService.getQueryCountUrl(clusterId, TubeConst.TOPIC_VIEW);
        JsonObject jsonObject = gson.fromJson(masterService.queryMaster(queryUrl), JsonObject.class);
        JsonElement getData = jsonObject.get("data");
        JsonArray fromJson = gson.fromJson(getData, JsonArray.class);
        int storeCount = 0;
        List<TopicViewRes> topicViewResList = gson.fromJson(fromJson.toString(),
                new TypeToken<List<TopicViewRes>>() {
                }.getType());
        for (TopicViewRes topicViewRes : topicViewResList) {
            storeCount = storeCount + (int) Math.ceil(topicViewRes.getTotalCfgNumStore());
        }
        return storeCount;
    }

    /**
     * query cluster by cluster name
     */
    public TubeMQResult queryClusterByClusterName(String clusterName) {
        ClusterEntry clusterEntry = clusterService.getOneCluster(clusterName);
        if (clusterEntry == null) {
            return TubeMQResult.errorResult("no such cluster with name " + clusterName);
        }
        List<MasterEntry> masterNodes = masterService.getMasterNodes(clusterEntry.getClusterId());
        ClusterVo allCount = getCountInCluster((int) clusterEntry.getClusterId());
        result.setData(Lists.newArrayList(ConvertUtils.convertToClusterVo(clusterEntry, masterNodes, allCount)));
        return result;
    }

    /**
     * query cluster by cluster masterIp
     */
    public TubeMQResult queryClusterByMasterIp(String masterIp) {
        List<ClusterEntry> clusterEntryList = Lists.newArrayList();
        List<MasterEntry> masterNodes = masterService.getMasterNodes(masterIp);
        if (CollectionUtils.isEmpty(masterNodes)) {
            return TubeMQResult.errorResult("no such cluster with ip " + masterIp);
        }
        for (MasterEntry masterNode : masterNodes) {
            ClusterEntry cluster = clusterService.getOneCluster(masterNode.getClusterId());
            clusterEntryList.add(cluster);
        }
        List<ClusterVo> clusterVos = Lists.newArrayList();
        for (ClusterEntry clusterEntry : clusterEntryList) {
            ClusterVo allCount = getCountInCluster((int) clusterEntry.getClusterId());
            ClusterVo clusterVo = ConvertUtils.convertToClusterVo(clusterEntry, masterNodes, allCount);
            clusterVos.add(clusterVo);
        }
        result.setData(clusterVos);
        return result;
    }

}
