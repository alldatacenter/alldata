package com.alibaba.tesla.tkgone.server.controllers.consumer;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.domain.ConsumerNode;
import com.alibaba.tesla.tkgone.server.domain.ConsumerNodeExample;
import com.alibaba.tesla.tkgone.server.domain.ConsumerNodeMapper;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @author jialiang.tjl
 */
@RestController
@RequestMapping("/consumer/node")
public class ConsumerNodeController extends BaseController {

    @Autowired
    ConsumerNodeMapper consumerNodeMapper;

    @RequestMapping(value = "/list/all", method = RequestMethod.GET)
    public TeslaBaseResult listAll() {

        return buildSucceedResult(consumerNodeMapper.selectByExample(new ConsumerNodeExample()));

    }

    @RequestMapping(value = "/list/invalid", method = RequestMethod.GET)
    public TeslaBaseResult listInvalid() {

        ConsumerNodeExample consumerNodeExample = new ConsumerNodeExample();
        consumerNodeExample.createCriteria().andGmtModifiedGreaterThanOrEqualTo(
            new Date(System.currentTimeMillis() - Constant.CONSUMER_NODE_HEARTBEAT_INVALID_TIME_INTERVAL * 1000));
        return buildSucceedResult(consumerNodeMapper.selectByExample(consumerNodeExample));

    }

    @RequestMapping(value = "/list/working", method = RequestMethod.GET)
    public TeslaBaseResult listWorking() {

        ConsumerNodeExample consumerNodeExample = new ConsumerNodeExample();
        consumerNodeExample.createCriteria()
            .andGmtModifiedGreaterThanOrEqualTo(
                new Date(System.currentTimeMillis() - Constant.CONSUMER_NODE_HEARTBEAT_INVALID_TIME_INTERVAL * 1000))
            .andEnableEqualTo("true");
        return buildSucceedResult(consumerNodeMapper.selectByExample(consumerNodeExample));

    }

    @RequestMapping(value = "/setEnable/{id}/{enable}", method = RequestMethod.PUT)
    public TeslaBaseResult setEnable(@PathVariable Long id, @PathVariable Boolean enable) {

        ConsumerNode consumerNode = new ConsumerNode();
        consumerNode.setEnable(enable.toString());
        consumerNode.setId(id);
        return buildSucceedResult(consumerNodeMapper.updateByPrimaryKeySelective(consumerNode));

    }

    @RequestMapping(value = "/setEnableByHost/{host}/{enable}", method = RequestMethod.PUT)
    public TeslaBaseResult setEnableByHost(@PathVariable String host, @PathVariable Boolean enable) {

        ConsumerNode consumerNode = new ConsumerNode();
        consumerNode.setEnable(enable.toString());
        ConsumerNodeExample consumerNodeExample = new ConsumerNodeExample();
        consumerNodeExample.createCriteria().andHostEqualTo(host);
        return buildSucceedResult(consumerNodeMapper.updateByExampleSelective(consumerNode, consumerNodeExample));

    }

}









