package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.NamespaceProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceCreateReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceQueryReq;
import com.alibaba.tesla.appmanager.domain.req.namespace.NamespaceUpdateReq;
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
 * Namespace 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/namespaces")
@RestController
public class NamespaceController extends AppManagerBaseController {

    @Autowired
    private NamespaceProvider namespaceProvider;

    @GetMapping
    public TeslaBaseResult queryByCondition(
            @Valid @ModelAttribute NamespaceQueryReq request,
            BindingResult validator, HttpServletRequest r, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(namespaceProvider.queryByCondition(request));
    }

    @PostMapping
    public TeslaBaseResult create(
            @Valid @RequestBody NamespaceCreateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(namespaceProvider.create(request, getOperator(auth)));
    }

    @PutMapping("{namespaceId}")
    public TeslaBaseResult update(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            @Valid @RequestBody NamespaceUpdateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        return buildSucceedResult(namespaceProvider.update(namespaceId, request, getOperator(auth)));
    }

    @DeleteMapping("{namespaceId}")
    public TeslaBaseResult delete(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            OAuth2Authentication auth) {
        namespaceProvider.delete(namespaceId, getOperator(auth));
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
