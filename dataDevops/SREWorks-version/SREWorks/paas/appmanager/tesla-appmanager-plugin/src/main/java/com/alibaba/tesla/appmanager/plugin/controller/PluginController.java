package com.alibaba.tesla.appmanager.plugin.controller;

import com.alibaba.tesla.appmanager.api.provider.PluginProvider;
import com.alibaba.tesla.appmanager.domain.dto.PluginMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.PluginQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Plugin 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/plugins")
@RestController
public class PluginController extends BaseController {


    @Autowired
    private PluginProvider pluginProvider;

    /**
     * @api {get} /plugins 获取已安装的插件列表
     * @apiName GetPluginList
     * @apiGroup 插件API
     */
    @GetMapping
    public TeslaBaseResult list(@ModelAttribute PluginQueryReq request) {
        return buildSucceedResult(null);
    }

    /**
     * @api {post} /plugins 新增插件
     * @apiName CreatePlugin
     * @apiGroup 插件API
     */
    @PostMapping
    public TeslaBaseResult create(@RequestParam("file") MultipartFile file) throws IOException {

        PluginMetaDTO pluginMeta = pluginProvider.create(file);

        return buildSucceedResult(pluginMeta);
    }



//    /**
//     * @api {get} /plugins/:pluginName 获取插件信息
//     * @apiName getPlugin
//     * @apiGroup 获取插件
//     */
//    @GetMapping
//    public TeslaBaseResult get(@PathVariable String pluginName) {
//        return buildSucceedResult(null);
//    }
//
//    /**
//     * @api {get} /plugins/:pluginName/frontend/:frontendType 获取插件信息
//     * @apiName getPlugin
//     * @apiGroup 获取插件的前端
//     */
//    @GetMapping
//    public TeslaBaseResult getFrontend(@PathVariable String pluginName, @PathVariable String frontendType) {
//        return buildSucceedResult(null);
//    }

}