package com.alibaba.tesla.productops.controllers;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsApp;
import com.alibaba.tesla.productops.common.JsonUtil;
import com.alibaba.tesla.productops.params.AppInitParam;
import com.alibaba.tesla.productops.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/frontend/apps")
public class AppController extends BaseController {

    @Autowired
    ProductopsAppRepository productopsAppRepository;

    @Autowired
    ProductopsElementRepository productopsElementRepository;

    @Autowired
    ProductopsNodeRepository productopsNodeRepository;

    @Autowired
    ProductopsTabRepository productopsTabRepository;

    @Autowired
    ProductopsNodeElementRepository productopsNodeElementRepository;

    @GetMapping(value = "/{appId}")
    public TeslaBaseResult get(@PathVariable String appId, String stageId) {
        return buildSucceedResult(productopsAppRepository.findFirstByAppIdAndStageId(appId, stageId));
    }

    @GetMapping(value = "/{appId}/exists")
    public TeslaBaseResult exists(@PathVariable String appId, String stageId) {
        boolean exists = productopsAppRepository.existsByAppIdAndStageId(appId, stageId);
        return buildSucceedResult(JsonUtil.map(
            "exists", exists
        ));
    }

    @PostMapping(value = "init")
    public TeslaBaseResult init(@RequestBody AppInitParam param, String stageId) {
        System.out.println(stageId);
        ProductopsApp app = ProductopsApp.builder()
            .gmtCreate(System.currentTimeMillis())
            .gmtModified(System.currentTimeMillis())
            .lastModifier(getUserEmployeeId())
            .stageId(stageId)
            .appId(param.getAppId())
            .templateName(param.getTemplateName())
            .environments(JSONObject.toJSONString(param.getEnvironments()))
            .version(param.getVersion())
            .config(JSONObject.toJSONString(param.getConfig()))
            .build();
        return buildSucceedResult(productopsAppRepository.saveAndFlush(app));
    }

    @DeleteMapping(value = "/{appId}")
    public TeslaBaseResult delete(@PathVariable String appId) {
        Long elements = productopsElementRepository.deleteByAppId(appId);
        Long nodeCounts = productopsNodeRepository.deleteByAppId(appId);
        Long nodeElements =  productopsNodeElementRepository.deleteByAppId(appId);
        Long tabs = productopsTabRepository.deleteByAppId(appId);
        Long apps = productopsAppRepository.deleteByAppId(appId);
        JSONObject res = new JSONObject();
        res.put("deleteNodes", nodeCounts);
        res.put("deleteElements", elements);
        res.put("deleteNodeElements", nodeElements);
        res.put("deleteTabs", tabs);
        res.put("deleteApps", apps);
        res.put("appId", appId);
        return buildSucceedResult(res);
    }

}
