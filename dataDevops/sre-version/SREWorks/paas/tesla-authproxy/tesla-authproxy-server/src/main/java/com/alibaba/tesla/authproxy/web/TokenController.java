package com.alibaba.tesla.authproxy.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.exceptions.ClientUserArgsException;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.AliyunOauth2Util;
import com.alibaba.tesla.authproxy.util.TeslaJwtUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;

import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * OAuth2 相关的 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Controller
@RequestMapping("oauth2/teslaToken")
@Slf4j
public class TokenController extends BaseController {

    @Autowired
    private AliyunOauth2Util oauth2Util;

    @Autowired
    private AuthProperties authProperties;
    @Autowired
    private TeslaUserService teslaUserService;

    @ResponseBody
    public TeslaBaseResult token(HttpServletRequest request,
        @RequestParam(value = "empId", required = false) String empId,
        @RequestParam(value = "loginName", required = false) String loginName) {
        UserDO userDo = this.getLoginUser(request);
        String tokenAdmin = authProperties.getTokenAdmin();
        if (StringUtils.isNotBlank(tokenAdmin)) {
            List admins = Collections.arrayToList(StringUtils.split(tokenAdmin, ","));
            if (!admins.contains(userDo.getLoginName())) {
                return TeslaResultFactory.buildExceptionResult(new ClientUserArgsException("Not be teslaTokenAdmin!"));
            }
        } else {
            return TeslaResultFactory.buildExceptionResult(new ClientUserArgsException("TeslaTokenAdmin is null!"));
        }
        UserDO userDO;
        if (StringUtils.isNotBlank(empId)) {
            userDO = teslaUserService.getUserByEmpId(empId);
        } else if (StringUtils.isNotBlank(loginName)){
            userDO = teslaUserService.getUserByLoginName(loginName);
        }else{
            return TeslaResultFactory.buildExceptionResult(new ClientUserArgsException("EmpId or loginName is necessary"));
        }
        if (userDO == null) {
            return TeslaResultFactory.buildExceptionResult(new ClientUserArgsException("Invalid empId or loginName"));
        }
        empId = userDO.getEmpId();
        loginName = userDO.getLoginName();
        String bucId = userDO.getBucId().toString();
        String email = userDO.getEmail();
        String userId = userDO.getUserId();
        String nickName = userDO.getNickName();
        String teslaToken = TeslaJwtUtil.create(empId, loginName, bucId, email, userId, nickName,
            TeslaJwtUtil.JWT_TOKEN_TIMEOUT, authProperties.getOauth2JwtSecret());
        Map<String,Object> res = new HashMap<>(8);
        res.put("token",teslaToken);
        return TeslaResultFactory.buildSucceedResult(res);
    }
}
