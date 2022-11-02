package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.RtComponentInstanceProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceDTO;
import com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance.RtComponentInstanceQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 实时组件实例管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/realtime/component-instances")
@RestController
public class RtComponentInstanceController extends AppManagerBaseController {

    @Autowired
    private RtComponentInstanceProvider rtComponentInstanceProvider;

    @GetMapping("")
    public TeslaBaseResult list(
            @ModelAttribute RtComponentInstanceQueryReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        if (request.getPageSize() > DefaultConstant.MAX_PAGE_SIZE) {
            return buildClientErrorResult(String.format("maximum pageSize is %d", DefaultConstant.MAX_PAGE_SIZE));
        }

        Pagination<RtComponentInstanceDTO> result = rtComponentInstanceProvider.queryByCondition(request);
        return buildSucceedResult(result);
    }
}
