package com.datasophon.api.interceptor;


import com.datasophon.api.security.Authenticator;
import com.datasophon.api.service.SessionService;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.UserInfoMapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * login interceptor, must login first
 */
public class LoginHandlerInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoginHandlerInterceptor.class);

  @Autowired
  private UserInfoMapper userMapper;

  @Autowired
  private Authenticator authenticator;

  /**
   * Intercept the execution of a handler. Called after HandlerMapping determined
   * @param request   current HTTP request
   * @param response  current HTTP response
   * @param handler   chosen handler to execute, for type and/or instance evaluation
   * @return boolean true or false
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // get token
    String token = request.getHeader("token");
    UserInfoEntity user = null;
    if (StringUtils.isEmpty(token)){
      user = authenticator.getAuthUser(request);
      // if user is null
      if (user == null) {
        response.setStatus(HttpStatus.SC_UNAUTHORIZED);
        logger.info("user does not exist");
        return false;
      }
    }else {
      user = userMapper.queryUserByToken(token);
      if (user == null) {
        response.setStatus(HttpStatus.SC_UNAUTHORIZED);
        logger.info("user token has expired");
        return false;
      }
    }
    request.getSession().setAttribute(Constants.SESSION_USER,user);
    request.setAttribute(Constants.SESSION_USER, user);
    return true;
  }

}
