package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.ClusterProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterCreateReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterQueryReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

/**
 * Cluster 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/clusters")
@RestController
public class ClusterController extends AppManagerBaseController {

    @Autowired
    private ClusterProvider clusterProvider;

    @GetMapping
    public TeslaBaseResult queryByCondition(
            @Valid @ModelAttribute ClusterQueryReq request,
            BindingResult validator, HttpServletRequest r, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(clusterProvider.queryByCondition(request));
    }

    @PostMapping
    public TeslaBaseResult create(
            @Valid @RequestBody ClusterCreateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(clusterProvider.create(request, getOperator(auth)));
    }

    @PutMapping("{clusterId}")
    public TeslaBaseResult update(
            @PathVariable("clusterId") @NotEmpty String clusterId,
            @Valid @RequestBody ClusterUpdateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(clusterProvider.update(clusterId, request, getOperator(auth)));
    }

    @DeleteMapping("{clusterId}")
    public TeslaBaseResult delete(
            @PathVariable("clusterId") @NotEmpty String clusterId, OAuth2Authentication auth) {
        clusterProvider.delete(clusterId, getOperator(auth));
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
