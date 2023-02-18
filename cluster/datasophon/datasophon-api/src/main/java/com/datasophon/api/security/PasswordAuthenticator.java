package com.datasophon.api.security;

import com.datasophon.api.enums.Status;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.api.service.SessionService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.SessionEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

public class PasswordAuthenticator implements Authenticator {
    private static final Logger logger = LoggerFactory.getLogger(PasswordAuthenticator.class);

    @Autowired
    private UserInfoService userService;
    @Autowired
    private SessionService sessionService;

    @Override
    public Result authenticate(String username, String password, String extra) {
        Result result = new Result();
        // verify username and password
        UserInfoEntity user = userService.queryUser(username, password);
        if (user == null) {
            result.put(Constants.CODE, Status.USER_NAME_PASSWD_ERROR.getCode());
            result.put(Constants.MSG,Status.USER_NAME_PASSWD_ERROR.getMsg());
            return result;
        }

        // create session
        String sessionId = sessionService.createSession(user, extra);
        if (sessionId == null) {
            result.put(Constants.CODE,Status.LOGIN_SESSION_FAILED.getCode());
            result.put(Constants.MSG,Status.LOGIN_SESSION_FAILED.getMsg());
            return result;
        }
        logger.info("sessionId : {}" , sessionId);
        result.put(Constants.DATA,Collections.singletonMap(Constants.SESSION_ID, sessionId));
        result.put(Constants.CODE,Status.SUCCESS.getCode());
        result.put(Constants.MSG,Status.LOGIN_SUCCESS.getMsg());
        result.put(Constants.USER_INFO,user);
        SecurityUtils.getSession().setAttribute(Constants.SESSION_USER,user);
        return result;
    }

    @Override
    public UserInfoEntity getAuthUser(HttpServletRequest request) {
        SessionEntity session = sessionService.getSession(request);
        if (session == null) {
            logger.info("session info is null ");
            return null;
        }
        //get user object from session
        return userService.getById(session.getUserId());
    }
}
