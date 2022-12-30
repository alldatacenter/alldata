package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.*;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.PrivateAuthService;
import com.alibaba.tesla.authproxy.service.PrivateSmsService;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.authproxy.util.audit.*;
import com.alibaba.tesla.authproxy.web.common.PrivateBaseController;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.authproxy.web.input.PrivateSmsGatewayTestParam;
import com.alibaba.tesla.authproxy.web.input.PrivateSmsRegisterParam;
import com.alibaba.tesla.authproxy.web.output.PrivateSmsConfigResult;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * 专有云 - 短信网关相关 API
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("auth/private/gateway")
@Slf4j
public class PrivateGatewayController extends PrivateBaseController {

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private PrivateSmsService privateSmsService;

    @Autowired
    private PrivateAuthService authService;

    @Autowired
    private AuditUtil auditUtil;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    @GetMapping("sms")
    @ResponseBody
    public TeslaResult smsConfig(HttpServletRequest request) throws PrivateAuthForbidden,
        AuthProxyThirdPartyError, PrivateAuthNotLogin {
        // 检验是否拥有云账号管理权限（限制首次登录用户访问）
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 获取配置
        PrivateSmsConfigResult result = privateSmsService.getConfig();
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - get sms gateway config, result=%s", gson.toJson(result)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    @PostMapping(value = "sms/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult smsRegister(@Valid @RequestBody PrivateSmsRegisterParam param, BindingResult bindingResult,
                                   HttpServletRequest request) throws PrivateAuthNotLogin, PrivateAuthForbidden,
        AuthProxyThirdPartyError, PrivateValidationError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        // 检验是否拥有云账号管理权限（限制首次登录用户访问）
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);
        try {
            privateSmsService.register(param.getEndpoint(), param.getToken());
        } catch (PrivateSmsSignatureForbidden e) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - register sms gateway config failed, sms signature forbidden, param=%s",
                            gson.toJson(param)), AuditReasonEnum.AUTHORIZED);
            return PrivateResultBuilder.buildExtBadRequestResult(locale.msg("private.sms.check_failed", e.getMessage()));
        }
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - register sms gateway config, param=%s", gson.toJson(param)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(param);
    }

    @GetMapping("testsend")
    @ResponseBody
    public TeslaResult testSendGet(HttpServletRequest request, HttpServletResponse response) {
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        if (StringUtil.isEmpty(signature) || StringUtil.isEmpty(timestamp) || StringUtil.isEmpty(nonce)) {
            log.warn("[TEST_GATEWAY_GET] Invalid request, signature={}, timestamp={}, nonce={}",
                    signature, timestamp, nonce);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return PrivateResultBuilder.buildExtBadRequestResult("BAD_USER_PARAMETER");
        }

        String testToken = "TEST_TOKEN";
        String trueSignature = buildSignature(testToken, timestamp, nonce);
        log.info("[TEST_GATEWAY_GET] signature={}, timestamp={}, nonce={}, trueSignature={}",
                signature, timestamp, nonce, trueSignature);
        if (signature.equals(trueSignature)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return PrivateResultBuilder.buildExtSuccessResult();
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return PrivateResultBuilder.buildExtBadRequestResult("FORBIDDEN");
        }
    }

    @PostMapping(value = "testsend", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult testSendPost(@RequestBody PrivateSmsGatewayTestParam param, HttpServletResponse response) {
        String testToken = "TEST_TOKEN";
        String phone = param.getPhone();
        String aliyunId = param.getAliyunId();
        String code = param.getCode();
        String content = param.getContent();
        String signature = param.getSignature();
        String timestamp = String.valueOf(param.getTimestamp());
        String nonce = String.valueOf(param.getNonce());
        String trueSignature = buildSignature(testToken, timestamp, nonce);
        if (!signature.equals(trueSignature)) {
            return PrivateResultBuilder.buildExtBadRequestResult("Invalid signature");
        }
        log.info("[TEST_GATEWAY_POST] phone={}, aliyunId={}, code={}, content={}, signature={}, " +
                        "trueSignature={}, timestamp={}, nonce={}", phone, aliyunId, code, content, signature,
                trueSignature, timestamp, nonce);
        response.setStatus(HttpServletResponse.SC_OK);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 根据 token / timestamp / nonce 来构造对应的签名
     *
     * @param token     Token
     * @param timestamp Timestamp UNIX 时间戳
     * @param nonce     Nonce
     * @return 计算出来的签名
     */
    private String buildSignature(String token, String timestamp, String nonce) {
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder arrStr = new StringBuilder();
        for (String item : arr) {
            arrStr.append(item);
        }
        return DigestUtils.sha1Hex(arrStr.toString());
    }

}
