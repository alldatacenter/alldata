package com.alibaba.sreworks.plugin.server.controllers;

import java.util.List;

import com.alibaba.sreworks.plugin.server.DTO.*;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.plugin.server.services.ClusterService;

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
public class ClusterController {

    @Autowired
    ClusterService clusterService;

    @RequestMapping(value = "list", method = RequestMethod.POST)
    public List<Cluster> list(@RequestBody JSONObject jsonObject) throws Exception {
        return clusterService.list(
            jsonObject.getObject("account", Account.class)
        );
    }

    @RequestMapping(value = "get", method = RequestMethod.POST)
    public Cluster get(@RequestBody JSONObject jsonObject) throws Exception {
        return clusterService.get(
            jsonObject.getObject("account", Account.class),
            jsonObject.getString("name")
        );
    }

    @RequestMapping(value = "getKubeConfig", method = RequestMethod.POST)
    public String getKubeConfig(@RequestBody JSONObject jsonObject) throws Exception {
        return clusterService.getKubeConfig(
            jsonObject.getObject("account", Account.class),
            jsonObject.getString("name")
        );
    }

    @RequestMapping(value = "getIngressHost", method = RequestMethod.POST)
    public String getIngressHost(@RequestBody JSONObject jsonObject) throws Exception {
        return clusterService.getIngressHost(
            jsonObject.getObject("account", Account.class),
            jsonObject.getString("name"),
            jsonObject.getString("subIngressHost")
        );
    }

    @RequestMapping(value = "getComponent", method = RequestMethod.POST)
    public List<Component> getComponent(@RequestBody JSONObject jsonObject) throws Exception {
        return clusterService.getComponent(
            jsonObject.getObject("account", Account.class),
            jsonObject.getString("name")
        );
    }
}
