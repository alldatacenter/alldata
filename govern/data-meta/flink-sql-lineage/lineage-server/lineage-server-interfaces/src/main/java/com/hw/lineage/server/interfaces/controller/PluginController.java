package com.hw.lineage.server.interfaces.controller;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.plugin.CreatePluginCmd;
import com.hw.lineage.server.application.command.plugin.UpdatePluginCmd;
import com.hw.lineage.server.application.dto.PluginDTO;
import com.hw.lineage.server.application.service.PluginService;
import com.hw.lineage.server.domain.query.plugin.PluginCheck;
import com.hw.lineage.server.domain.query.plugin.PluginQuery;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @description: PluginController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Plugins API")
@RequestMapping("/plugins")
public class PluginController {

    @Resource
    private PluginService pluginService;

    @GetMapping("/{pluginId}")
    public Result<PluginDTO> queryPlugin(@PathVariable("pluginId") Long pluginId) {
        PluginDTO pluginDTO = pluginService.queryPlugin(pluginId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, pluginDTO);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('system:plugin:list')")
    public Result<PageInfo<PluginDTO>> queryPlugins(PluginQuery pluginQuery) {
        PageInfo<PluginDTO> pageInfo = pluginService.queryPlugins(pluginQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('system:plugin:add')")
    public Result<Long> createPlugin(@Valid @RequestBody CreatePluginCmd command) {
        Long pluginId = pluginService.createPlugin(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, pluginId);
    }

    @GetMapping("/exist")
    public Result<Boolean> checkPluginExist(@Valid PluginCheck pluginCheck) {
        return Result.success(ResultMessage.CHECK_SUCCESS, pluginService.checkPluginExist(pluginCheck));
    }

    @PutMapping("/{pluginId}")
    @PreAuthorize("hasAuthority('system:plugin:edit')")
    public Result<Boolean> updatePlugin(@PathVariable("pluginId") Long pluginId,
                                        @Valid @RequestBody UpdatePluginCmd command) {
        command.setPluginId(pluginId);
        pluginService.updatePlugin(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @PutMapping("/{pluginId}/default")
    public Result<Boolean> defaultPlugin(@PathVariable("pluginId") Long pluginId) {
        pluginService.defaultPlugin(pluginId);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{pluginId}")
    @PreAuthorize("hasAuthority('system:plugin:delete')")
    public Result<Boolean> deletePlugin(@PathVariable("pluginId") Long pluginId) {
        pluginService.deletePlugin(pluginId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

}
