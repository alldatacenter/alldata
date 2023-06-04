package com.datasophon.api.interceptor;

import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.enums.Status;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.ClusterRoleUserService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.UserInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;

public class UserPermissionHandler implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(UserPermissionHandler.class);

    @Autowired
    private ClusterRoleUserService clusterUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        UserPermission annotation = handlerMethod.getMethod().getAnnotation(UserPermission.class);
        if (Objects.nonNull(annotation)) {
            UserInfoEntity authUser = (UserInfoEntity) request.getSession().getAttribute(Constants.SESSION_USER);
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (Objects.nonNull(authUser) && !SecurityUtils.isAdmin(authUser)) {
                logger.info("step into authrization");
                if(parameterMap.containsKey("clusterId")){
                    logger.info("find clusterId");
                    String[] clusterIds = parameterMap.get("clusterId");
                    if(clusterUserService.isClusterManager(authUser.getId(),clusterIds[0])){
                        logger.info("{} is cluster manager",authUser.getUsername());
                        return true;
                    }
                }
                throw new ServiceException(Status.USER_NO_OPERATION_PERM);
            }
        }
        return true;
    }
}
