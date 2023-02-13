package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.SwitchViewUserService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.ao.UserGetConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserGetResultAO;
import com.alibaba.tesla.authproxy.util.CookieUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

/**
 * 获取个人信息 Profile Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("profile")
@Slf4j
public class UserProfileController extends BaseController {

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 获取当前用户的 Profile
     */
    @GetMapping
    @ResponseBody
    public TeslaBaseResult retrieve(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("语言")
        @Pattern(regexp = Constants.DEFAULT_LOCALE_REGEX)
        @RequestHeader(value = "X-Locale", required = false, defaultValue = "zh_CN")
            String locale,
        @ApiParam("应用 ID")
        @RequestHeader(value = "X-Biz-App", required = false, defaultValue = "")
            String appId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        HttpServletRequest request
    ) {
        UserGetConditionAO condition = UserGetConditionAO.builder()
            .locale(locale)
            .tenantId(tenantId)
            .empId(empId)
            .appId(appId)
            .fromEmpId(request.getHeader("X-From-EmpId"))
            .fromAuthUser(request.getHeader("X-From-Auth-User"))
            .fromBucId(request.getHeader("X-From-Buc-Id"))
            .build();
        UserGetResultAO user = teslaUserService.getEnhancedUser(condition);
        return TeslaResultFactory.buildSucceedResult(user);
    }

    /**
     * 更新当前用户的 Profile 信息
     */
    @PostMapping(value = "lang")
    @ResponseBody
    public TeslaBaseResult changeLang(HttpServletRequest request, HttpServletResponse response,
                                      @Valid @RequestBody UpdateTeslaUserParam param) throws Exception {
        String empId = request.getHeader("X-EmpId");
        if (StringUtils.isEmpty(empId)) {
            return TeslaResultFactory.buildValidationErrorResult("X-EmpId", "required header");
        }
        String lang = param.getLang();
        String[] langs = lang.split("_");
        if (langs.length != 2) {
            return TeslaResultFactory.buildValidationErrorResult(
                "lang", "Cannot write user language into cookie/db, lang split length not 2");
        }

        UserDO user = teslaUserService.getUserByEmpId(empId);
        CookieUtil.setCookie(response, Constants.ALIYUN_COOKIE_LANG, langs[0], 0, authProperties.getCookieDomain());
        CookieUtil.setCookie(response, Constants.ALIYUN_COOKIE_TERRITORY, langs[1], 0,
            authProperties.getCookieDomain());
        teslaUserService.changeLanguage(user, lang);
        return TeslaResultFactory.buildSucceedResult(user);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTeslaUserParam {

        /**
         * 语言选项
         */
        private String lang;
    }
}
