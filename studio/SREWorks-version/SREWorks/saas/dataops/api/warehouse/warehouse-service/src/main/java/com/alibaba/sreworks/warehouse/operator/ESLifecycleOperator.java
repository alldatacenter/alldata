package com.alibaba.sreworks.warehouse.operator;

import com.alibaba.sreworks.warehouse.common.client.ESClient;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.ESLifecyclePolicyException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.AcknowledgedResponse;
import org.elasticsearch.client.indexlifecycle.*;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ES索引生命周期操作服务类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/10 20:52
 */
@Service
@Slf4j
public class ESLifecycleOperator {

    @Autowired
    ESClient esClient;

    public String createLifecyclePolicy(Integer lifecycle) throws Exception {
        String policyName = String.format(DwConstant.LIFECYCLE_POLICY_PREFIX, lifecycle);
        if (existLifecyclePolicy(policyName)) {
            log.warn(String.format("====create lifecycle policy %s exist====", policyName));
            return policyName;
        }

        Map<String, Phase> phases = new HashMap<>();
//        Map<String, LifecycleAction> hotActions = new HashMap<>();
//        hotActions.put(RolloverAction.NAME, new RolloverAction(new ByteSizeValue(50, ByteSizeUnit.GB), null, null, null));
//        phases.put("hot", new Phase("hot", TimeValue.ZERO, hotActions));

        Map<String, LifecycleAction> hotActions = new HashMap<>();
//        hotActions.put(RolloverAction.NAME, new RolloverAction(null, null, new TimeValue(rolloverHour, TimeUnit.HOURS), null));
//        hotActions.put(RolloverAction.NAME, null);
        phases.put("hot", new Phase("hot", TimeValue.ZERO, hotActions));

        Map<String, LifecycleAction> deleteActions = Collections.singletonMap(DeleteAction.NAME, new DeleteAction());
        phases.put("delete", new Phase("delete", new TimeValue(lifecycle, TimeUnit.DAYS), deleteActions));

        LifecyclePolicy policy = new LifecyclePolicy(policyName, phases);
        PutLifecyclePolicyRequest request = new PutLifecyclePolicyRequest(policy);

        RestHighLevelClient hlClient = esClient.getHighLevelClient();

        try {
            AcknowledgedResponse response = hlClient.indexLifecycle().putLifecyclePolicy(request, RequestOptions.DEFAULT);
            if (response.isAcknowledged()) {
                return policyName;
            } else {
                throw new ESLifecyclePolicyException("索引生命周期创建失败");
            }
        } catch (Exception ex) {
            throw new ESLifecyclePolicyException(String.format("索引生命周期创建失败, %s", ex));
        }
    }

    public boolean existLifecyclePolicy(String policyName) throws Exception {
        GetLifecyclePolicyRequest request = new GetLifecyclePolicyRequest();
        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        GetLifecyclePolicyResponse response = hlClient.indexLifecycle().getLifecyclePolicy(request, RequestOptions.DEFAULT);
        LifecyclePolicyMetadata policyMetadata = response.getPolicies().getOrDefault(policyName, null);

        return policyMetadata != null && policyMetadata.getName().equals(policyName);
    }

}
