package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.StageProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.StageDTO;
import com.alibaba.tesla.appmanager.domain.req.stage.StageCreateReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageQueryReq;
import com.alibaba.tesla.appmanager.domain.req.stage.StageUpdateReq;
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
 * Stage 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping
@RestController
public class StageController extends AppManagerBaseController {

    @Autowired
    private StageProvider stageProvider;

    @GetMapping("stages")
    public TeslaBaseResult list(@ModelAttribute StageQueryReq request,
                                HttpServletRequest r, OAuth2Authentication auth) {
        Pagination<StageDTO> result = stageProvider.list(request);
        return buildSucceedResult(result);
    }

    @GetMapping("namespaces/{namespaceId}/stages")
    public TeslaBaseResult listInNamespace(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            @ModelAttribute StageQueryReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        Pagination<StageDTO> result = stageProvider.list(namespaceId, request);
        return buildSucceedResult(result);
    }

    @PostMapping("namespaces/{namespaceId}/stages")
    public TeslaBaseResult createInNamespace(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            @RequestBody StageCreateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        StageDTO stage = stageProvider.create(namespaceId, request, getOperator(auth));
        return buildSucceedResult(stage);
    }

    @PutMapping("namespaces/{namespaceId}/stages/{stageId}")
    public TeslaBaseResult updateInNamespace(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            @PathVariable("stageId") @NotEmpty String stageId,
            @Valid @RequestBody StageUpdateReq request,
            BindingResult validator, OAuth2Authentication auth) {
        if (validator.hasErrors()) {
            return buildValidationResult(validator);
        }
        StageDTO stage = stageProvider.update(namespaceId, stageId, request, getOperator(auth));
        return buildSucceedResult(stage);
    }

    @DeleteMapping("namespaces/{namespaceId}/stages/{stageId}")
    public TeslaBaseResult deleteInNamespace(
            @PathVariable("namespaceId") @NotEmpty String namespaceId,
            @PathVariable("stageId") @NotEmpty String stageId,
            OAuth2Authentication auth) {
        stageProvider.delete(namespaceId, stageId, getOperator(auth));
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
