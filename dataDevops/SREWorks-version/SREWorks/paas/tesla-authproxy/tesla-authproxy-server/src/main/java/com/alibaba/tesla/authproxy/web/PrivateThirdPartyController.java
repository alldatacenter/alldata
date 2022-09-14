package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.outbound.acs.AcsClientFactory;
import com.alibaba.tesla.authproxy.web.common.PrivateBaseController;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.authproxy.web.input.PrivateThirdPartyPermissionParam;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

/**
 * 专有云 - 第三方系统服务 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("auth/private/thirdparty")
@Slf4j
public class PrivateThirdPartyController extends PrivateBaseController {

    @Autowired
    private AcsClientFactory acsClientFactory;

    ///**
    // * 创建 ADS 数据库权限 API
    // */
    //@PostMapping(value = "ads/permission", consumes = MediaType.APPLICATION_JSON_VALUE)
    //@ResponseBody
    //public TeslaResult permissionAds(@Valid @RequestBody PrivateThirdPartyPermissionParam param,
    //                                 BindingResult bindingResult) {
    //    if (bindingResult.hasErrors()) {
    //        return buildValidationResult(bindingResult);
    //    }
    //
    //    long aliyunPk = Long.valueOf(param.getPk());
    //    AkClient akClient = acsClientFactory.getAkClient();
    //    try {
    //        akClient.updateServiceStatus(aliyunPk, "ads", ServiceStatus.ENABLED);
    //    } catch (IOException e) {
    //        return PrivateResultBuilder.buildExtBadRequestResult(String.format("Enable ads service status failed, " +
    //            "message=%s", e.getMessage()));
    //    }
    //    return PrivateResultBuilder.buildExtSuccessResult();
    //}
    //
    ///**
    // * 创建 OTS 数据库权限 API
    // */
    //@PostMapping(value = "ots/permission", consumes = MediaType.APPLICATION_JSON_VALUE)
    //@ResponseBody
    //public TeslaResult permissionOts(@Valid @RequestBody PrivateThirdPartyPermissionParam param,
    //                                 BindingResult bindingResult) {
    //    if (bindingResult.hasErrors()) {
    //        return buildValidationResult(bindingResult);
    //    }
    //
    //    long aliyunPk = Long.valueOf(param.getPk());
    //    AkClient akClient = acsClientFactory.getAkClient();
    //    try {
    //        akClient.updateServiceStatus(aliyunPk, "ots", ServiceStatus.ENABLED);
    //    } catch (IOException e) {
    //        return PrivateResultBuilder.buildExtBadRequestResult(String.format("Enable ots service status failed, " +
    //            "message=%s", e.getMessage()));
    //    }
    //    return PrivateResultBuilder.buildExtSuccessResult();
    //}

}
