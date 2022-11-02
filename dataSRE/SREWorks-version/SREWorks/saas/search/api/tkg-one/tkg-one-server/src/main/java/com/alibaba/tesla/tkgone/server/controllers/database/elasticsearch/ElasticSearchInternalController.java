package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.ErrorUtil;
import com.alibaba.tesla.tkgone.server.consumer.AbstractConsumer;
import com.alibaba.tesla.tkgone.server.domain.ConsumerExample;
import com.alibaba.tesla.tkgone.server.domain.ConsumerMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangjinghua
 */
@Slf4j
@RestController
@RequestMapping(value = "/database/elasticsearch/")
public class ElasticSearchInternalController extends BaseController {

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    ConsumerMapper consumerMapper;

    @RequestMapping(value = "/upsertInternalStatistics", method = RequestMethod.GET)
    public TeslaBaseResult upsertInternalStatistics() {
        ElasticSearchUpsertService.InternalStatistics statistics = elasticSearchUpsertService.getInternalStatistics();
        fillConsumerInfo(statistics);
        return buildSucceedResult(statistics);
    }

    private void fillConsumerInfo(ElasticSearchUpsertService.InternalStatistics statistics) {
        try {
            ConsumerExample condition = new ConsumerExample();
            condition.createCriteria().andStatusEqualTo(AbstractConsumer.STATUS_SUCCESS).andEnableEqualTo("true");
            statistics.setConsumerOkNum(((Number)consumerMapper.countByExample(condition)).intValue());

            condition.clear();
            condition.createCriteria().andStatusEqualTo(AbstractConsumer.STATUS_FAILED).andEnableEqualTo("true");
            statistics.setConsumerFailedNum(((Number)consumerMapper.countByExample(condition)).intValue());

            condition.clear();
            condition.createCriteria().andEnableEqualTo("false");
            statistics.setConsumerDisabledNum(((Number)consumerMapper.countByExample(condition)).intValue());
        } catch (Throwable e) {
            ErrorUtil.log(log, e, "count consumer info failed");
            statistics.setConsumerOkNum(-1);
            statistics.setConsumerFailedNum(-1);
            statistics.setConsumerDisabledNum(-1);
        }
    }

}
