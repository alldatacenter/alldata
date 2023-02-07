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

package org.apache.inlong.tubemq.manager.service;

import static org.apache.inlong.tubemq.manager.controller.TubeMQResult.errorResult;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.node.request.BaseReq;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import static org.apache.inlong.tubemq.manager.service.TubeConst.DELETE_FAIL;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpResponse;
import org.apache.inlong.tubemq.manager.utils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MasterServiceImpl implements MasterService {

    private static CloseableHttpClient httpclient = HttpClients.createDefault();
    private static Gson gson = new Gson();

    @Autowired
    MasterRepository masterRepository;

    @Override
    public TubeMQResult requestMaster(String url) {

        log.info("start to request {}", url);
        HttpGet httpGet = new HttpGet(url);
        TubeMQResult defaultResult = new TubeMQResult();

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            TubeHttpResponse tubeResponse =
                    gson.fromJson(new InputStreamReader(response.getEntity().getContent(),
                            StandardCharsets.UTF_8), TubeHttpResponse.class);
            if (tubeResponse.getCode() == TubeConst.SUCCESS_CODE
                    && tubeResponse.getErrCode() == TubeConst.SUCCESS_CODE) {
                return defaultResult;
            } else {
                defaultResult = errorResult(tubeResponse.getErrMsg());
            }
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
            defaultResult = TubeMQResult.errorResult(ex.getMessage());
        }
        return defaultResult;
    }

    /**
     * query master to get node info
     *
     * @param url
     * @return query info
     */
    @Override
    public String queryMaster(String url) {
        log.info("start to request {}", url);
        HttpGet httpGet = new HttpGet(url);
        TubeMQResult defaultResult = new TubeMQResult();
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            // return result json to response
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
            defaultResult.setErrCode(-1);
            defaultResult.setResult(false);
            defaultResult.setErrMsg(ex.getMessage());
        }
        return gson.toJson(defaultResult);
    }

    @Override
    public TubeMQResult baseRequestMaster(BaseReq req) {
        if (req.getClusterId() == null) {
            return TubeMQResult.errorResult("please input clusterId");
        }
        MasterEntry masterEntry = getMasterNode(Long.valueOf(req.getClusterId()));
        if (masterEntry == null) {
            return TubeMQResult.errorResult("no such cluster");
        }
        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + "/" + TubeConst.TUBE_REQUEST_PATH + "?" + TubeConst.CONF_MOD_AUTH_TOKEN + masterEntry.getToken() + "&"
                + ConvertUtils.convertReqToQueryStr(req);
        return requestMaster(url);
    }

    @Override
    public MasterEntry getMasterNode(BaseReq req) {
        if (req.getClusterId() == null) {
            return null;
        }
        return masterRepository.findMasterEntryByClusterIdEquals(
                req.getClusterId());
    }

    @Override
    public MasterEntry getMasterNode(Long clusterId) {
        if (clusterId == null) {
            return null;
        }
        List<MasterEntry> masters = getMasterNodes(clusterId);

        for (MasterEntry masterEntry : masters) {
            if (!checkMasterNodeStatus(masterEntry.getIp(),
                    masterEntry.getWebPort()).isError()) {
                return masterEntry;
            }
        }

        throw new RuntimeException("cluster id " + clusterId + "no master node, please check");
    }

    @Override
    public List<MasterEntry> getMasterNodes(Long clusterId) {
        if (clusterId == null) {
            return null;
        }
        List<MasterEntry> masters = masterRepository.findMasterEntriesByClusterIdEquals(clusterId);
        if (CollectionUtils.isEmpty(masters)) {
            throw new RuntimeException("cluster id " + clusterId + "no master node, please check");
        }
        return masters;
    }

    @Override
    public List<MasterEntry> getMasterNodes(String masterIp) {
        if (masterIp == null) {
            return null;
        }
        List<MasterEntry> masters = masterRepository.findMasterEntryByIpEquals(masterIp);
        if (CollectionUtils.isEmpty(masters)) {
            throw new RuntimeException("master ip " + masterIp + "no master node, please check");
        }
        return masters;
    }

    @Override
    public String getQueryUrl(Map<String, String> queryBody) throws Exception {
        int clusterId = Integer.parseInt(queryBody.get("clusterId"));
        queryBody.remove("clusterId");
        MasterEntry masterEntry = getMasterNode(Long.valueOf(clusterId));
        return TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + "/" + TubeConst.TUBE_REQUEST_PATH + "?" + ConvertUtils.covertMapToQueryString(queryBody);
    }

    @Override
    public TubeMQResult checkMasterNodeStatus(String masterIp, Integer masterWebPort) {
        String url = TubeConst.SCHEMA + masterIp + ":" + masterWebPort + TubeConst.BROKER_RUN_STATUS;
        return requestMaster(url);
    }

    @Override
    public String getQueryCountUrl(Integer clusterId, String method) {
        MasterEntry masterEntry = getMasterNode(Long.valueOf(clusterId));
        return TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + method + "&" + "clusterId=" + clusterId;
    }

    @Override
    public void deleteMaster(Long clusterId) {
        Integer successCode = masterRepository.deleteByClusterId(clusterId);
        if (successCode.equals(DELETE_FAIL)) {
            throw new RuntimeException("no such master with clusterId = " + clusterId);
        }
    }

}
