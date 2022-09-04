package com.alibaba.tesla.authproxy;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.constants.AuthProxyConstants;
import com.alibaba.tesla.authproxy.exceptions.TeslaJwtException;
import com.alibaba.tesla.authproxy.interceptor.*;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.aas.AasUserInfo;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.AuthUtil;
import com.alibaba.tesla.authproxy.util.CookieUtil;
import com.alibaba.tesla.authproxy.util.TeslaJwtUtil;
import com.alibaba.tesla.authproxy.util.UserUtil;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.common.utils.TeslaResult;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Title: BaseController.java<／p>
 * <p>Description: Controller基类，处理返回值，获取登录用户，注入所有Controller公共依赖类 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@Slf4j
public class BaseController {

    @Autowired
    public AuthPolicy authPolicy;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private TeslaAppService teslaAppService;

    @Autowired
    AuthUtil authUtil;

    /**
     * 获取登录用户信息
     */
    public UserDO getLoginUser(HttpServletRequest request) {
        LoginInterceptor loginIterceptor = authPolicy.getLoginServiceManager();
        Cookie loginUserIdCookie;

        // 如果是 AAS / Aliyun 的登录方式
        if (loginIterceptor instanceof AasLoginInterceptor || loginIterceptor instanceof AliyunLoginInterceptor) {
            // 对于经过拦截器的请求，那么存在 REQUEST_ATTR_USER 属性
            UserDO userDo = (UserDO)request.getAttribute(Constants.REQUEST_ATTR_USER);
            if (userDo != null) {
                return userDo;
            }
            // 对于内部调用，没有 REQUEST_ATTR_USER 属性，从 Header 中获取
            String loginName = request.getHeader("X-Auth-User");
            return teslaUserService.getUserByLoginName(loginName);
        } else if (loginIterceptor instanceof SsoLoginInterceptor) {
            /**
             * 优先从cookie中获取用户信息
             */
            Cookie teslaTokenCookie = CookieUtil.getCookie(request, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN);
            if (teslaTokenCookie != null) {
                String teslaToken = teslaTokenCookie.getValue();
                Claims claims = null;
                try {
                    claims = TeslaJwtUtil.verify(teslaToken, authProperties.getOauth2JwtSecret());
                    String empId = claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, String.class);
                    UserDO userDo = teslaUserService.getUserByEmpId(empId);
                    if (userDo != null) {
                        log.info("Tesla cookie auth success:{}", JSONObject.toJSONString(userDo));
                        return userDo;
                    }
                } catch (TeslaJwtException e) {
                    log.warn("Verify tesla jwt token failed! ErrMsg:{}", e.getMessage());
                }
            }

            /**
             * 使用header进行验证
             */
            UserDO userDo = authUtil.getExtAppUser(request);
            if (userDo != null) {
                log.info("Tesla header auth success:{}", JSONObject.toJSONString(userDo));
                return userDo;
            }

            // 保留原使用header中appId/Product-Name进行验证；使用原auth-proxy校验的逻辑

            /**
             * 是否开启登录验证功能，如果开启使用tesla-authproxy中提供的统一登录功能。
             * 如果不开启使用auth-proxy的登录拦截，用户登录成功之后将用户的bucId写入request的Header中
             * 这里直接取Header中的bucId返回，后续调用查询菜单，权限验证使用bucId
             */
            String appId = request.getParameter("appId");
            if (StringUtils.isEmpty(appId)) {
                appId = request.getHeader("Product-Name");
            }

            UserDO user;
            // ====start:兼容auth/user/loginUser该接口之前没有传appId参数的接入，当不传入该参数时候，从请求头中获取用户信息 add by tandong.td ===== //
            if (StringUtils.isEmpty(appId)) {
                user = new UserDO();
                String bucId = request.getHeader("X-Buc-Id");
                String userName = request.getHeader("X-Auth-User");
                String empId = request.getHeader("X-Empid");
                String email = request.getHeader("X-Email-Addr");

                log.info("参数AppId为空，使用请求头用的cookie获取用户信息，Auth proxy give login user bucid is {}", bucId);
                if (StringUtils.isEmpty(bucId)) {
                    user.setBucId(0L);
                } else {
                    user.setBucId(Long.valueOf(bucId));
                }
                user.setTenantId(Constants.DEFAULT_TENANT_ID);
                user.setNickName(userName);
                user.setEmpId(empId);
                user.setLoginName(userName);
                user.setEmail(email);
                user.setLang(authProperties.getDefaultLanguage());
                user.setUserId(UserUtil.getUserId(user));
                return user;
            }
            // =====end:兼容auth/user/loginUser该接口之前没有传appId参数的接入，当不传入该参数时候，从请求头中获取用户信息 add by tandong.td==== //
            AppDO appDo = teslaAppService.getByAppId(appId);
            if (null == appDo) {
                throw new ApplicationException(TeslaResult.FAILURE, "应用[" + appId + "]不存在");
            }
            //loginEnable == 1使用了权代服务的登录拦截，则从cookie中获取用户信息，否则从nginx转发的请求头中获取
            if (appDo.getLoginEnable() == 1) {
                loginUserIdCookie = CookieUtil.getCookie(request, Constants.COOKIE_SSO_LOGIN_USER_ID);
                if (null == loginUserIdCookie) {
                    throw new ApplicationException(TeslaResult.NOAUTH, "Auth failure, Please login.");
                }
                user = teslaUserService.getUserByBucId(loginUserIdCookie.getValue());
            } else {
                //使用auth-proxy登录控制
                user = new UserDO();
                String bucId = request.getHeader("X-Buc-Id");
                String userName = request.getHeader("X-Auth-User");
                String empId = request.getHeader("X-Empid");
                String email = request.getHeader("X-Email-Addr");

                log.info("没有开启loginEnable，使用request请求头参数[X-Buc-Id]获取用户信息，X-Buc-Id is {}", bucId);
                if (StringUtils.isEmpty(bucId)) {
                    //start 兼容后端直接带请求头的调用权代接口的情况，如果不存在buc id则根据工号查询实时查询BUC。
                    String userBucId = authPolicy.getAuthServiceManager().getUserId(empId);
                    user.setBucId(Long.valueOf(userBucId));
                    //end 兼容后端直接带请求头的调用权代接口的情况，如果不存在buc id则根据工号查询实时查询BUC。
                } else {
                    user.setBucId(Long.valueOf(bucId));
                }
                user.setTenantId(Constants.DEFAULT_TENANT_ID);
                user.setNickName(userName);
                user.setEmpId(empId);
                user.setLoginName(userName);
                user.setEmail(email);
                user.setLang(authProperties.getDefaultLanguage());
                user.setUserId(UserUtil.getUserId(user));
            }
            return user;
        } else if (loginIterceptor instanceof DataBaseLoginInterceptor) {
            //兼容请求头中放用户信息
            UserDO headerUserDo = authUtil.getExtAppUser(request);
            if (null != headerUserDo) {
                return headerUserDo;
            }

            //兼容网关自带app
            String userName = request.getHeader("X-Auth-User");
            String empId = request.getHeader("X-Empid");
            if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(empId)) {
                return teslaUserService.getUserByLoginName(userName);
            }

            String loginUserName = "";
            loginUserIdCookie = CookieUtil.getCookie(request, Constants.COOKIE_DATABASE_LOGIN_USER_ID);
            log.info("DataBaseLoginInterceptor validate cookie {}={}", Constants.COOKIE_DATABASE_LOGIN_USER_ID,
                loginUserIdCookie);
            if (null != loginUserIdCookie && !StringUtils.isEmpty(loginUserIdCookie.getValue())) {
                loginUserName = loginUserIdCookie.getValue();
            } else {
                Cookie userToken = CookieUtil.getCookie(request, Constants.COOKIE_DATABASE_LOGIN_TOKEN);
                if (null == userToken || StringUtils.isEmpty(userToken.getValue())) {
                    throw new ApplicationException(TeslaResult.NOAUTH, "Auth failure, Please login.");
                }
                Claims claims = null;
                try {
                    claims = TeslaJwtUtil.verify(userToken.getValue(), authProperties.getOauth2JwtSecret());
                } catch (TeslaJwtException e) {
                    throw new ApplicationException(TeslaResult.NOAUTH, "Tesla jwt token verify failure, Please login.");
                }
                loginUserName = String.valueOf(claims.get(Constants.COOKIE_DATABASE_LOGIN_USER_ID));
            }
            UserDO userDo = teslaUserService.getUserByLoginName(loginUserName);
            if (null == userDo) {
                log.error("Auth failure Get user from database return null. cookie = {}.", loginUserName);
                throw new ApplicationException(TeslaResult.NOAUTH, "Auth failure, Please login.");
            }
            return userDo;
        } else {
            throw new ApplicationException(TeslaResult.FAILURE, "login.policyClass is error");
        }
    }

    //public AasUserInfo loadUserInfoByTicket(String ticket) throws Exception {
    //
    //    log.info("调用aas的http接口校验ticket，ak为{},{}", authProperties.getAasKey(), authProperties.getAasSecret());
    //
    //    AliyunidClient aliyunidClient = new AliyunidClient(authProperties.getAasKey(), authProperties.getAasSecret());
    //    List<OAuthPair> params = new ArrayList<OAuthPair>();
    //    params.add(new OAuthPair("oauth_ticket", ticket));
    //
    //    String resp = aliyunidClient.callApi(authProperties.getAasLoadTicketUrl(), params);
    //
    //    /**
    //     * 请求出错
    //     */
    //    if (resp.contains("errorCode")) {
    //        log.error("AAS调用失败:{}", resp);
    //        throw new Exception(resp);
    //    }
    //
    //    AasUserInfo responseUser = JSONObject.parseObject(resp, AasUserInfo.class);
    //    /**
    //     * 用户不存在，Ticket校验失败
    //     */
    //    if (null == responseUser || null == responseUser.getAliyunID() || responseUser.getAliyunID().length() == 0) {
    //        log.error("Login Ticket[{}]校验失败。aas返回数据[{}]", ticket, resp);
    //    }
    //    log.info("AAS返回数据[{}]", resp);
    //    return responseUser;
    //}

    /**
     * 构造分页信息
     *
     * @param pageSize
     * @param currentPageNum
     * @return
     */
    public PageBounds buildPageBounds(Integer pageSize, Integer currentPageNum, Boolean isContainsTotalCount) {
        if (null == pageSize) {
            pageSize = 10;
        }
        if (null == currentPageNum) {
            currentPageNum = 1;
        }
        if (null == isContainsTotalCount) {
            isContainsTotalCount = true;
        }
        PageBounds pageBounds = new PageBounds();
        pageBounds.setPage(currentPageNum);
        pageBounds.setLimit(pageSize);
        pageBounds.setContainsTotalCount(isContainsTotalCount);
        return pageBounds;
    }

    /**
     * 构造分页数据集
     *
     * @param pageSize
     * @param currentPageNum
     * @param ret
     * @return
     */
    public Map<String, Object> buildResult(Integer pageSize, Integer currentPageNum, PageList<?> ret) {

        if (null == pageSize) {
            pageSize = 10;
        }
        if (null == currentPageNum) {
            currentPageNum = 1;
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 200);
        result.put("rows", ret);
        if (null == ret || null == ret.getPaginator()) {
            result.put("total", 0);
        } else {
            result.put("total", ret.getPaginator().getTotalCount());
        }
        return result;
    }

    public Map<String, Object> buildResult(Object data) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

    public Map<String, Object> buildResult(String code, Object data) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

    public Map<String, Object> buildResult(String code, Object data, String message) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        result.put("data", data);
        result.put("message", message);
        return result;
    }

    public Map<String, Object> buildResult() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 200);
        result.put("data", null);
        result.put("message", "success");
        return result;
    }

    public Map<String, Object> buildErrorResult(String code, String errorMsg) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        result.put("data", null);
        result.put("message", errorMsg);
        return result;
    }

    public Map<String, Object> buildErrorResult(int code, String errorMsg) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        result.put("data", null);
        result.put("message", errorMsg);
        return result;
    }

    public Map<String, Object> buildErrorResult(String errorMsg) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 500);
        result.put("data", null);
        result.put("message", errorMsg);
        return result;
    }

    public Map<String, Object> buildSuccessResult(String msg) {
        Map<String, Object> result = new HashMap<>(8);
        result.put("code", 200);
        result.put("data", null);
        result.put("message", msg);
        return result;
    }

    public TeslaResult buildSuccessTeslaResult(Object data) {
        TeslaResult result = new TeslaResult();
        result.setCode(TeslaResult.SUCCESS);
        result.setData(data);
        result.setMessage("SUCCESS");
        return result;
    }

    public TeslaResult buildFailedTeslaResult(String message) {
        TeslaResult result = new TeslaResult();
        result.setCode(TeslaResult.BAD_REQUEST);
        result.setData(null);
        result.setMessage(message);
        return result;
    }

    public TeslaResult buildValidationResult(BindingResult result) {
        Map<String, String> errorMap = new HashMap<>(10);
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            errorMap.put(error.getField(), error.getDefaultMessage());
        }
        return PrivateResultBuilder.buildExtValidationErrorResult(errorMap);
    }

}
