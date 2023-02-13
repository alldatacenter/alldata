package com.alibaba.tesla.appmanager.definition.controller;

import com.alibaba.tesla.appmanager.api.provider.DefinitionSchemaProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.DefinitionSchemaDTO;
import com.alibaba.tesla.appmanager.domain.req.DefinitionSchemaQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * Definition Schema 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/definition-schemas")
@RestController
public class DefinitionSchemaController extends BaseController {

    @Autowired
    private DefinitionSchemaProvider definitionSchemaProvider;

    // 应用并更新 Schema
    @PostMapping("")
    @ResponseBody
    public TeslaBaseResult apply(@RequestBody DefinitionSchemaDTO request) {
        definitionSchemaProvider.apply(request, "SYSTEM");
        return buildSucceedResult(new HashMap<String, String>());
    }

    // 获取 Schema 列表
    @GetMapping("")
    public TeslaBaseResult list(@ModelAttribute DefinitionSchemaQueryReq request) {
        Pagination<DefinitionSchemaDTO> results = definitionSchemaProvider.list(request, "SYSTEM");
        return buildSucceedResult(results);
    }

    // 获取指定的 Schema
    @GetMapping("/{name}")
    @ResponseBody
    public TeslaBaseResult get(@PathVariable("name") String name) {
        DefinitionSchemaQueryReq req = DefinitionSchemaQueryReq.builder().name(name).build();
        DefinitionSchemaDTO response = definitionSchemaProvider.get(req, "SYSTEM");
        return buildSucceedResult(response);
    }

    // 删除指定的 Schema
    @DeleteMapping("/{name}")
    @ResponseBody
    public TeslaBaseResult delete(@PathVariable("name") String name) {
        DefinitionSchemaQueryReq req = DefinitionSchemaQueryReq.builder().name(name).build();
        int response = definitionSchemaProvider.delete(req, "SYSTEM");
        return buildSucceedResult(response);
    }
}
