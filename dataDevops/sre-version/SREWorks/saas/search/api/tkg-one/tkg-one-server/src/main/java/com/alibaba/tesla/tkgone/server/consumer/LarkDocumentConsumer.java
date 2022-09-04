package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.LarkDocumentHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author yangjinghua
 */
@Service
public class LarkDocumentConsumer extends AbstractConsumer implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        int concurrentExecNum = consumerConfigService.getSingleConsumerLarkDocumentConcurrentExecNum();
        int effectiveInterval = consumerConfigService.getLarkDocumentEffectiveInterval();
        afterPropertiesSet(concurrentExecNum, effectiveInterval, ConsumerSourceType.larkDocument);
    }

    @Override
    int consumerDataByConsumerDto(ConsumerDto consumerDto) throws Exception {

        String xAuthToken = consumerDto.getSourceInfoJson().getString("xAuthToken");
        String groupName = consumerDto.getSourceInfoJson().getString("groupName");
        String repoName = consumerDto.getSourceInfoJson().getString("repoName");
        String tocPath = consumerDto.getSourceInfoJson().getString("tocPath");
        boolean isPartition = consumerDto.getSourceInfoJson().getBooleanValue("isPartition");

        LarkDocumentHelper larkDocumentHelper = new LarkDocumentHelper(xAuthToken, groupName, repoName, tocPath);
        List<JSONObject> jsonObjectList;
        String partition = isPartition ? Tools.currentDataString() : null;
        int alreadyFetchDataSize = 0;
        do {
            jsonObjectList = larkDocumentHelper.fetchData();
            alreadyFetchDataSize += saveToBackendStore(consumerDto, jsonObjectList, consumerDto.getImportConfigArray(),
                    partition);
        } while (!CollectionUtils.isEmpty(jsonObjectList));
        return alreadyFetchDataSize;
    }

}
