package com.datasophon.api.controller;


import com.datasophon.api.utils.HttpUtils;
import com.datasophon.api.enums.Status;
import com.datasophon.api.security.Authenticator;
import com.datasophon.api.service.SessionService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;
import org.apache.commons.httpclient.HttpStatus;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.datasophon.api.enums.Status.IP_IS_EMPTY;


@RestController
@RequestMapping("")
public class LoginController{

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);


    @Autowired
    private SessionService sessionService;

    @Autowired
    private Authenticator authenticator;


    /**
     * login
     *
     * @param userName     user name
     * @param userPassword user password
     * @param request      request
     * @param response     response
     * @return login result
     */

    @RequestMapping("/login")
    public Result login(@RequestParam(value = "username") String userName,
                        @RequestParam(value = "password") String userPassword,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        logger.info("login user name: {} ", userName);

        //user name check
        if (StringUtils.isEmpty(userName)) {
            return Result.error(Status.USER_NAME_NULL.getCode(),
                    Status.USER_NAME_NULL.getMsg());
        }

        // user ip check
        String ip = HttpUtils.getClientIpAddress(request);
        if (StringUtils.isEmpty(ip)) {
            return Result.error(IP_IS_EMPTY.getCode(), IP_IS_EMPTY.getMsg());
        }

        // verify username and password
        Result result = authenticator.authenticate(userName, userPassword, ip);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            return result;
        }

        response.setStatus(HttpStatus.SC_OK);
        Map<String, String> cookieMap = (Map<String, String>) result.getData();
        for (Map.Entry<String, String> cookieEntry : cookieMap.entrySet()) {
            Cookie cookie = new Cookie(cookieEntry.getKey(), cookieEntry.getValue());
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        return result;
    }

    /**
     * sign out
     *
     * @param loginUser login user
     * @param request   request
     * @return sign out result
     */
    @PostMapping(value = "/signOut")
    public Result signOut(@RequestAttribute(value = Constants.SESSION_USER) UserInfoEntity loginUser,
                          HttpServletRequest request) {
        logger.info("login user:{} sign out", loginUser.getUsername());
        String ip = HttpUtils.getClientIpAddress(request);
        sessionService.signOut(ip, loginUser);
        //clear session
        request.removeAttribute(Constants.SESSION_USER);
        return Result.success();
    }
}
