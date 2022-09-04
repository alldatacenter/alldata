package com.alibaba.tesla.authproxy.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.component.cookie.ResponseCookie;
import com.alibaba.tesla.authproxy.constants.AuthProxyConstants;
import com.alibaba.tesla.authproxy.exceptions.ClientUserArgsException;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.CookieUtil;
import com.alibaba.tesla.authproxy.util.TeslaJwtUtil;
import com.alibaba.tesla.authproxy.util.UserUtil;
import com.alibaba.tesla.authproxy.util.audit.*;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.authproxy.web.input.TeslaUserChangePwdRequest;
import com.alibaba.tesla.authproxy.web.input.TeslaUserCreateRequest;
import com.alibaba.tesla.authproxy.web.input.TeslaUserListRequest;
import com.alibaba.tesla.authproxy.web.input.TeslaUserModifyRequest;
import com.alibaba.tesla.authproxy.web.output.TeslaUserInfoResult;
import com.alibaba.tesla.authproxy.web.output.TeslaUserLangListResult;
import com.alibaba.tesla.common.base.enums.TeslaRegion;
import com.alibaba.tesla.common.utils.TeslaMessageDigest;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;

import com.alibaba.tesla.web.constant.HttpHeaderNames;

import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.*;

/**
 * 用户 Controller
 */
@RestController
@Slf4j
public class UserController extends BaseController {

    private Gson gson = new GsonBuilder().serializeNulls().create();

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private TeslaAppService teslaAppService;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private AuditUtil auditUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取登录用户信息
     */
    @RequestMapping(value = "auth/user/loginUser", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getLoginUserInfo(HttpServletRequest request, HttpServletResponse response) {
        UserDO userDo = this.getLoginUser(request);
        Map<String, Object> result = buildResult(userDo);
        if (null == userDo) {
            return result;
        }

        // 当拿到用户后进行 Language 相关的 Cookie 逻辑
        String aliyunLanguage = concatAliyunLanguageCookie(request);
        if (aliyunLanguage.length() == 0) {
            if (null == userDo.getLang() || userDo.getLang().length() == 0) {
                userDo.setLang(authProperties.getDefaultLanguage());
                teslaUserService.save(userDo);
            }
            setAliyunLanguageCookie(response, userDo.getLang());
        } else if (null == userDo.getLang() || !aliyunLanguage.equals(userDo.getLang())) {
            userDo.setLang(aliyunLanguage);
            teslaUserService.save(userDo);
        }
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
            String.format("authproxy - auth user getLoginUserInfo, result=%s",
                gson.toJson(result)),
            AuditReasonEnum.AUTHORIZED);
        return result;
    }

    /**
     * 内部系统间获取用户信息的接口
     *
     * @param request 请求中需要包含 X-Auth-User 和 X-Auth-UserId 两个头
     * @return 返回用户详细信息，同 /info 接口
     */
    @GetMapping(value = "auth/user/detail")
    @ResponseBody
    public TeslaResult getUserDetail(HttpServletRequest request) throws ClientUserArgsException {
        String authUser = request.getHeader("X-Auth-User");
        String authUserId = request.getHeader("X-Auth-UserId");
        String appId = request.getHeader("X-Auth-App");
        if (appId == null) {
            appId = "UNKNOWN";
        }

        if (authUser == null || authUserId == null) {
            throw new ClientUserArgsException("Invalid request, no auth headers");
        }
        UserDO userDo = teslaUserService.getUserByLoginName(authUser);
        switch (authProperties.getEnvironment()) {
            case Constants.ENVIRONMENT_INTERNAL:
                assert userDo.getEmpId().equals(authUserId);
                break;
            default:
                assert userDo.getAliyunPk().equals(authUserId);
                break;
        }
        TeslaUserInfoResult userInfoResult = new TeslaUserInfoResult(userDo, appId, authProperties.getEnvironment());
        return buildSuccessTeslaResult(userInfoResult);
    }

    /**
     * 权代服务V2版本新提供接口，建议不要使用/loginUser接口了
     */
    @RequestMapping(value = "auth/user/info", method = RequestMethod.GET)
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult getUserInfo(HttpServletRequest request,
        @RequestParam(required = false) String appId,
        HttpServletResponse response) {
        if (StringUtils.isEmpty(appId)) {
            appId = authProperties.getDefaultAppId();
        }
        UserDO userDo = this.getLoginUser(request);
        log.info("getUserInfo, return {}", JSONObject.toJSONString(userDo));
        // 没有登录返回登录地址
        if (null == userDo) {
            String loginUrl;
            try {
                loginUrl = getLoginUrl(appId);
            } catch (UnsupportedEncodingException e) {
                log.error("服务异常", e);
                return TeslaResultBuilder.failureResult(500, "服务异常", "URLEncoder.encode has error");
            }
            return TeslaResultBuilder.failureResult(TeslaResult.NOAUTH, loginUrl);
        }

        if (TeslaRegion.PRIVATE.equals(authProperties.getTeslaSwitch())) {
            //写入tesla token cookie
            String teslaToken = TeslaJwtUtil.create(userDo.getEmpId(), userDo.getLoginName(),
                Objects.toString(userDo.getBucId()), userDo.getEmail(),
                UserUtil.getUserId(userDo), userDo.getNickName(), userDo.getAliyunPk(), TeslaJwtUtil.JWT_TOKEN_TIMEOUT,
                authProperties.getOauth2JwtSecret());
            if (Objects.equals(authProperties.getNetworkProtocol(), "https")) {
                ResponseCookie responseCookie = ResponseCookie.builder().name(AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN)
                    .value(teslaToken)
                    .maxAge(Duration.ofSeconds(24 * 60 * 60))
                    .domain(authProperties.getCookieDomain())
                    .sameSite("None")
                    .secure(true)
                    .path("/")
                    .build();
                response.addHeader("Set-Cookie", responseCookie.toString());
            } else {
                CookieUtil.setCookie(response, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN, teslaToken, 0);
            }
        }

        // 当拿到用户后进行 Language 相关的 Cookie 逻辑
        String aliyunLanguage = concatAliyunLanguageCookie(request);
        if (aliyunLanguage.length() == 0) {
            if (null == userDo.getLang() || userDo.getLang().length() == 0) {
                userDo.setLang(authProperties.getDefaultLanguage());
                teslaUserService.save(userDo);
            }
            setAliyunLanguageCookie(response, userDo.getLang());
        } else if (null == userDo.getLang() || !aliyunLanguage.equals(userDo.getLang())) {
            userDo.setLang(aliyunLanguage);
            teslaUserService.save(userDo);
        }

        // 转换最终输出结果
        TeslaUserInfoResult userInfoResult = new TeslaUserInfoResult(userDo, appId, authProperties.getEnvironment());
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
            String.format("authproxy - auth user getUserInfo, param=%s, result=%s",
                String.format("appId=%s", appId), gson.toJson(userInfoResult)), AuditReasonEnum.AUTHORIZED);
        return TeslaResultBuilder.successResult(userInfoResult);
    }

    /**
     * @param request
     * @param userName
     * @return
     */
    @RequestMapping(value = "auth/user/list", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult list(HttpServletRequest request, @RequestParam String userName) {
        List<UserDO> list = teslaUserService.selectByName(userName);
        return buildSuccessTeslaResult(list);
    }

    @RequestMapping(value = "auth/user/get", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult get(String empId) {
        return buildSuccessTeslaResult(teslaUserService.getUserByEmpId(empId));
    }

    /**
     * 用户语言列表获取 API
     */
    @RequestMapping(value = "auth/user/lang", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult accountLangList() {
        TeslaUserLangListResult result = new TeslaUserLangListResult();
        result.setLangs(Arrays.asList(authProperties.getAvailableLanguages().split(",")));
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    /**
     * 创建tesla用户
     *
     * @param request
     * @param createRequest
     * @param bindingResult
     * @return
     */
    @PostMapping("auth/tesla/user/create")
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult create(HttpServletRequest request,
        @Valid @RequestBody TeslaUserCreateRequest createRequest,
        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return TeslaResultBuilder.failureResult(400, null, bindingResult.getFieldError().getDefaultMessage());
        }
        if (!isAdminUser(request)) {
            return TeslaResultBuilder.noPermissionResult(null);
        }
        UserDO old = teslaUserService.getUserByLoginName(createRequest.getLoginName());
        if (null != old) {
            return TeslaResultBuilder.errorResult(new Exception("The same login name already exists, Please change."));
        }
        UserDO userDo = new UserDO();
        userDo.setEmail(createRequest.getEmail());
        userDo.setPhone(createRequest.getPhone());
        userDo.setNickName(createRequest.getNickName());
        userDo.setLoginName(createRequest.getLoginName());
        userDo.setEmpId(createRequest.getLoginName());
        userDo.setLoginPwd(createRequest.getPassword());
        userDo.setAliyunPk(createRequest.getLoginName());
        userDo.setIsFirstLogin(Byte.valueOf("0"));
        userDo.setIsLocked(Byte.valueOf("0"));
        userDo.setStatus(0);
        userDo.setIsImmutable(Byte.valueOf("0"));
        userDo.setLang(authProperties.getDefaultLanguage());
        userDo.setAvatar(createRequest.getAvatar());
        log.info(JSONObject.toJSONString(userDo));
        int ret = teslaUserService.insert(userDo);
        return TeslaResultBuilder.successResult(ret);
    }

    @PostMapping("auth/tesla/user/modify")
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult modify(HttpServletRequest request,
        @RequestBody TeslaUserModifyRequest modifyRequest) throws Exception {
        String empId = request.getHeader(HttpHeaderNames.X_EMPL_ID);
        UserDO userDo = teslaUserService.getUserByEmpId(empId);
        userDo.setGmtModified(new Date());
        userDo.setEmail(modifyRequest.getEmail());
        userDo.setPhone(modifyRequest.getPhone());
        userDo.setNickName(modifyRequest.getNickName());
        userDo.setLoginName(modifyRequest.getLoginName());
        if (StringUtils.isNotEmpty(modifyRequest.getPassword())) {
            userMapper.updateLoginPassword(
                userDo.getLoginName(), TeslaMessageDigest.getMD5(modifyRequest.getPassword()), new Date());
        }
        userDo.setAvatar(modifyRequest.getAvatar());
        int ret = teslaUserService.update(userDo);
        return TeslaResultBuilder.successResult(ret);
    }

    @DeleteMapping("auth/tesla/user/delete")
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult delete(HttpServletRequest request, Long id) {
        if (id == 1L) {
            return TeslaResultBuilder.noPermissionResult(null);
        }
        if (!isAdminUser(request)) {
            return TeslaResultBuilder.noPermissionResult(null);
        }
        userMapper.deleteByPrimaryKey(id);
        return TeslaResultBuilder.successResult("OK");
    }

    /**
     * 查询某个应用下所有已经开启的服务元数据（含部分开启）
     *
     * @param userRequest
     * @return
     */
    @PostMapping("auth/tesla/user/list")
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult list(HttpServletRequest request,
        @Valid @RequestBody TeslaUserListRequest userRequest,
        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return TeslaResultBuilder.failureResult(400, null, bindingResult.getFieldError().getDefaultMessage());
        }
        PageInfo<UserDO> pageData = teslaUserService.listUserWithPage(userRequest.getPage(), userRequest.getSize(),
            userRequest.getLoginName());
        Map<String, Object> ret = new HashMap<>();
        ret.put("list", pageData.getList());
        ret.put("total", pageData.getTotal());
        return TeslaResultBuilder.successResult(ret);
    }

    /**
     * 修改用户密码
     *
     * @param changePwdRequest
     * @return
     */
    @PostMapping("auth/tesla/user/change/password")
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult changePassword(HttpServletRequest request,
        @Valid @RequestBody TeslaUserChangePwdRequest changePwdRequest,
        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            TeslaResultBuilder.successResult(400, bindingResult.getFieldError().getDefaultMessage());
        }
        if (!isAdminUser(request)) {
            return TeslaResultBuilder.noPermissionResult(null);
        }
        UserDO loginUser = this.getLoginUser(request);
        teslaUserService.changePassword(loginUser, changePwdRequest.getOldPassword(),
            changePwdRequest.getNewPassword());
        return TeslaResultBuilder.successResult(null);
    }

    /**
     * 判断是否为管理员用户
     *
     * @param request
     * @return
     */
    private boolean isAdminUser(HttpServletRequest request) {
        String empId = request.getHeader(HttpHeaderNames.X_EMPL_ID);
        return "999999999".equals(empId);
    }

    /**
     * 获取当前的登录地址
     *
     * @param appId 应用 ID
     */
    private String getLoginUrl(String appId) throws UnsupportedEncodingException {
        AppDO appDo = teslaAppService.getByAppId(appId);
        if (null == appDo.getLoginUrl() || appDo.getLoginUrl().length() == 0) {
            String backUrl = URLEncoder.encode(authProperties.getCallbackUrl(), "UTF-8");
            return authProperties.getLoginUrl() + "&BACK_URL=" + backUrl;
        } else {
            return appDo.getLoginUrl();
        }
    }

    private String concatAliyunLanguageCookie(HttpServletRequest request) {
        Cookie aliyunCookieLang = CookieUtil.getCookie(request, Constants.ALIYUN_COOKIE_LANG);
        Cookie aliyunCookieCountry = CookieUtil.getCookie(request, Constants.ALIYUN_COOKIE_TERRITORY);
        if (null == aliyunCookieLang || null == aliyunCookieCountry) {
            return "";
        }
        if (null == aliyunCookieLang.getValue() || aliyunCookieLang.getValue().length() == 0 ||
            null == aliyunCookieCountry.getValue() || aliyunCookieCountry.getValue().length() == 0) {
            return "";
        }
        return aliyunCookieLang.getValue() + "_" + aliyunCookieCountry.getValue();
    }

    private void setAliyunLanguageCookie(HttpServletResponse response, String aliyunLanguage) {
        String topDomain = authProperties.getCookieDomain();
        String[] langs = aliyunLanguage.split("_");
        if (langs.length != 2) {
            log.error("Aliyun language cookie value not valid, do nothing {}", aliyunLanguage);
            return;
        }
        CookieUtil.setCookie(response, Constants.ALIYUN_COOKIE_LANG, langs[0], 0, topDomain);
        CookieUtil.setCookie(response, Constants.ALIYUN_COOKIE_TERRITORY, langs[1], 0, topDomain);
    }
}
