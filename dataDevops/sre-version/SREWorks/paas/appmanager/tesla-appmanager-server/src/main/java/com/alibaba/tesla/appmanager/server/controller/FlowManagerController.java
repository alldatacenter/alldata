package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.FlowManagerProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

/**
 * Flow 管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/flow-manager")
@RestController
public class FlowManagerController extends AppManagerBaseController {

    @Autowired
    private FlowManagerProvider flowManagerProvider;

    // 升级 Flow 包
    @PostMapping(value = "/upgrade")
    @ResponseBody
    public TeslaBaseResult upgrade(
            @RequestParam("file") MultipartFile file,
            OAuth2Authentication auth
    ) throws Exception {
        flowManagerProvider.upgrade(file.getInputStream(), getOperator(auth));
        return buildSucceedResult(new HashMap<String, String>());
    }
}
