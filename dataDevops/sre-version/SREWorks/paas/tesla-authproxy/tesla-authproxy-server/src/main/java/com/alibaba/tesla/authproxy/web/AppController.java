package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.service.AppExtService;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.authproxy.web.input.RegisterAppParam;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;

/**
 * 应用接入请求Controller
 *
 * @author tandong.td@alibaba-inc.com
 * Created by tandong on 2017/7/14.
 */
@Controller
@RequestMapping("auth/app")
public class AppController extends BaseController {

    @Autowired
    TeslaAppService teslaAppService;

    @Autowired
    AppExtService appExtService;

    /**
     * 校验APP的AK信息是否合法 x-auth-app'/'x-auth-key
     *
     * @param xAuthApp 应用标识
     * @param xAuthKey 应用AccessKey
     * @return
     */
    @RequestMapping("check")
    public TeslaResult check(@RequestParam String xAuthApp, @RequestParam String xAuthKey) {
        if (StringUtils.isEmpty(xAuthApp)) {
            return TeslaResultBuilder.errorResult("Parameter error", "'xAuthApp' is not empty");
        }
        if (StringUtils.isEmpty(xAuthKey)) {
            return TeslaResultBuilder.errorResult("Parameter error", "'xAuthKey' is not empty");
        }
        AppExtDO appExt = appExtService.getByName(xAuthApp);
        if (xAuthKey.equals(appExt.getExtAppKey())) {
            return TeslaResultBuilder.successResult(true);
        } else {
            return TeslaResultBuilder.errorResult(false, "App is invalid");
        }
    }

    /**
     * 注册新APP
     * @param registerAppParam
     */
    @RequestMapping(value = "register", method = RequestMethod.POST)
    @ResponseBody
    public TeslaResult register(HttpServletRequest request, @Valid @RequestBody RegisterAppParam registerAppParam, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return TeslaResultBuilder.errorResult(null, bindingResult.getFieldError().getDefaultMessage());
        }
        if(registerAppParam.getLoginEnable() == 1 && StringUtil.isEmpty(registerAppParam.getIndexUrl())){
            return TeslaResultBuilder.errorResult(null, "indexUrl can't be empty when loginEnable = 1");
        }
        AppDO appDo = new AppDO();
        appDo.setAppId(registerAppParam.getAppId());
        appDo.setAppAccesskey(registerAppParam.getAppAccessKey());
        appDo.setIndexUrl(registerAppParam.getIndexUrl());
        appDo.setLoginEnable(registerAppParam.getLoginEnable());
        appDo.setGmtCreate(new Date());
        int ret = teslaAppService.insert(appDo, getLoginUser(request));
        if (ret == 1) {
            return TeslaResultBuilder.successResult();
        }
        return TeslaResultBuilder.failureResult();
    }

    @RequestMapping(value = "update", method = RequestMethod.POST)
    @ResponseBody
    public TeslaResult update(@Valid @RequestBody AppDO appDo, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return TeslaResultBuilder.errorResult(null, bindingResult.getFieldError().getDefaultMessage());
        }
        int ret = teslaAppService.update(appDo);
        return TeslaResultBuilder.successResult(ret);
    }
}
