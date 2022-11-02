package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.RtAppInstanceProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtAppInstanceHistoryQueryReq;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtAppInstanceQueryReq;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtComponentInstanceHistoryQueryReq;
import com.alibaba.tesla.appmanager.server.job.OrphanComponentInstanceJob;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 实时应用实例管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/realtime/app-instances")
@RestController
public class RtAppInstanceController extends AppManagerBaseController {

    @Autowired
    private RtAppInstanceProvider rtAppInstanceProvider;

    @Autowired
    private OrphanComponentInstanceJob orphanComponentInstanceJob;

    @GetMapping("")
    public TeslaBaseResult list(
            @ModelAttribute RtAppInstanceQueryReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        if (StringUtils.isEmpty(request.getClusterId())) {
            request.setClusterId(null);
        }
        if (StringUtils.isEmpty(request.getNamespaceId())) {
            request.setNamespaceId(null);
        }
        if (StringUtils.isEmpty(request.getStageId())) {
            request.setStageId(null);
        }
        Pagination<RtAppInstanceDTO> result = rtAppInstanceProvider.queryByCondition(request);
        return buildSucceedResult(result);
    }

    @GetMapping(value = "/statistics")
    public TeslaBaseResult statistics(
            RtAppInstanceQueryReq request, HttpServletRequest r, OAuth2Authentication auth) {
        request.setPagination(false);
        Pagination<RtAppInstanceDTO> result = rtAppInstanceProvider.queryByCondition(request);
        long upgradeCount = result.getItems().stream()
                .filter(instance ->
                        VersionUtil.compareTo(instance.getLatestVersion(), instance.getSimpleVersion()) > 0)
                .count();
        RtAppInstanceStatisticsDTO response = RtAppInstanceStatisticsDTO.builder()
                .deployCount(result.getTotal())
                .upgradeCount(upgradeCount)
                .build();
        return buildSucceedResult(response);
    }

    @GetMapping("{appInstanceId}")
    public TeslaBaseResult get(
            @PathVariable("appInstanceId") String appInstanceId,
            HttpServletRequest r, OAuth2Authentication auth) {
        RtAppInstanceDTO result = rtAppInstanceProvider.get(appInstanceId);
        return buildSucceedResult(result);
    }

    @DeleteMapping("{appInstanceId}")
    public TeslaBaseResult delete(
            @PathVariable("appInstanceId") String appInstanceId,
            HttpServletRequest r, OAuth2Authentication auth) {
        rtAppInstanceProvider.delete(appInstanceId);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }

    @GetMapping("{appInstanceId}/histories")
    public TeslaBaseResult listHistory(
            @PathVariable("appInstanceId") String appInstanceId,
            @ModelAttribute RtAppInstanceHistoryQueryReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        request.setAppInstanceId(appInstanceId);
        Pagination<RtAppInstanceHistoryDTO> result = rtAppInstanceProvider.queryAppInstanceHistoryByCondition(request);
        return buildSucceedResult(result);
    }

    @GetMapping("{appInstanceId}/component-instances/{componentInstanceId}")
    public TeslaBaseResult getComponentInstance(
            @PathVariable("appInstanceId") String appInstanceId,
            @PathVariable("componentInstanceId") String componentInstanceId,
            HttpServletRequest r, OAuth2Authentication auth) {
        RtComponentInstanceDTO result = rtAppInstanceProvider.getComponentInstance(appInstanceId, componentInstanceId);
        if (result == null) {
            return buildClientErrorResult("cannot find specified component instance");
        }
        return buildSucceedResult(result);
    }

    @GetMapping("{appInstanceId}/component-instances/{componentInstanceId}/histories")
    public TeslaBaseResult listHistory(
            @PathVariable("appInstanceId") String appInstanceId,
            @PathVariable("componentInstanceId") String componentInstanceId,
            @ModelAttribute RtComponentInstanceHistoryQueryReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        request.setComponentInstanceId(componentInstanceId);
        Pagination<RtComponentInstanceHistoryDTO> result = rtAppInstanceProvider
                .queryComponentInstanceHistoryByCondition(request);
        return buildSucceedResult(result);
    }

    @PostMapping("/retryOrphanComponents")
    public TeslaBaseResult orphanComponents() {
        orphanComponentInstanceJob.run();
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
