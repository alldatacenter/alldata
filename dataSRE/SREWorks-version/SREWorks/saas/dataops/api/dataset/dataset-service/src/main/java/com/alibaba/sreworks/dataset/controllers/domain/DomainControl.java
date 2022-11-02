package com.alibaba.sreworks.dataset.controllers.domain;

import com.alibaba.sreworks.dataset.api.domain.DomainService;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainCreateReq;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 数据域Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/domain/")
@Api(tags = "数据域")
public class DomainControl extends BaseController {

    @Autowired
    DomainService domainService;

    @ApiOperation(value = "查询数据域(根据数据域ID)")
    @RequestMapping(value = "/getDomainById", method = RequestMethod.GET)
    public TeslaBaseResult getDomainById(@RequestParam(name = "domainId") Integer domainId) {
        return buildSucceedResult(domainService.getDomainById(domainId));
    }

    @ApiOperation(value = "查询数据域(根据数据主题ID)")
    @RequestMapping(value = "/getDomainsBySubject", method = RequestMethod.GET)
    public TeslaBaseResult getDomainsBySubject(@RequestParam(name = "subjectId") Integer subjectId) {
        return buildSucceedResult(domainService.getDomainsBySubject(subjectId));
    }

    @ApiOperation(value = "查询所有数据域(兼容全量接口)")
    @RequestMapping(value = "/getDomains", method = RequestMethod.GET)
    public TeslaBaseResult getDomains(@RequestParam(name = "subjectId", required = false) Integer subjectId) {
        return buildSucceedResult(domainService.getDomains(subjectId));
    }

    @ApiOperation(value = "新增数据域")
    @RequestMapping(value = "/createDomain", method = RequestMethod.POST)
    public TeslaBaseResult createDomain(@RequestBody DataDomainCreateReq req) throws Exception {
        return buildSucceedResult(domainService.addDomain(req));
    }

    @ApiOperation(value = "更新数据域(根据数据域ID)")
    @RequestMapping(value = "/updateDomain", method = RequestMethod.POST)
    public TeslaBaseResult updateDomain(@RequestBody DataDomainUpdateReq req) throws Exception {
        return buildSucceedResult(domainService.updateDomain(req));
    }

    @ApiOperation(value = "删除数据域")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "domainId", value = "数据域ID", paramType = "query")
    })
    @RequestMapping(value = "/deleteDomainById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteDomainById(@RequestParam(name = "domainId") Integer domainId) throws Exception {
        return buildSucceedResult(domainService.deleteDomainById(domainId));
    }

}
