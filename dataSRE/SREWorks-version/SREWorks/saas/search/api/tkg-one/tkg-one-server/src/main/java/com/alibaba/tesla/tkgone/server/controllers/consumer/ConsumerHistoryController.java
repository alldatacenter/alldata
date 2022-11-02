package com.alibaba.tesla.tkgone.server.controllers.consumer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.domain.ConsumerHistoryExample;
import com.alibaba.tesla.tkgone.server.domain.ConsumerHistoryMapper;
import com.alibaba.tesla.web.controller.BaseController;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jialiang.tjl
 */
@RestController
@RequestMapping("/consumer/history")
public class ConsumerHistoryController extends BaseController {

    @Autowired
    ConsumerHistoryMapper consumerHistoryMapper;

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public TeslaBaseResult getByName(String name, Integer page, Integer size) {
        if (page == null || page == 0) { page = 1; }
        if (size == null || size == 0) { size = 10; }
        int from = (page - 1) * size;
        ConsumerHistoryExample consumerHistoryExample = new ConsumerHistoryExample();
        consumerHistoryExample.createCriteria().andNameEqualTo(name);
        consumerHistoryExample.setOrderByClause("id desc");
        JSONObject retJson = new JSONObject();
        retJson.put("items",
            consumerHistoryMapper.selectByExampleWithRowbounds(consumerHistoryExample, new RowBounds(from, size)));
        retJson.put("total", consumerHistoryMapper.countByExample(consumerHistoryExample));
        return buildSucceedResult(retJson);
    }
}









