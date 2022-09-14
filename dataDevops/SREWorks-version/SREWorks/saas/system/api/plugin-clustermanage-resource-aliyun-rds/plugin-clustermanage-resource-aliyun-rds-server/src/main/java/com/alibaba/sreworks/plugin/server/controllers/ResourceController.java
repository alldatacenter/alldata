package com.alibaba.sreworks.plugin.server.controllers;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.plugin.server.DTO.Account;
import com.alibaba.sreworks.plugin.server.DTO.Instance;
import com.alibaba.sreworks.plugin.server.DTO.UsageDetail;
import com.alibaba.sreworks.plugin.server.services.ResourceService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
public class ResourceController {

    @Autowired
    ResourceService resourceService;

    @RequestMapping(value = "list", method = RequestMethod.POST)
    public List<Instance> list(@RequestBody JSONObject jsonObject) throws Exception {
        return resourceService.list(
            jsonObject.getObject("account", Account.class),
            jsonObject.getObject("engine", String.class)
        );
    }

    @RequestMapping(value = "getUsageDetail", method = RequestMethod.POST)
    public UsageDetail getUsageDetail(@RequestBody JSONObject jsonObject) throws Exception {
        return resourceService.getUsageDetail(
            jsonObject.getObject("account", Account.class),
            jsonObject.getString("name")
        );
    }

}
