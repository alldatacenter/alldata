package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.datasophon.api.utils.HttpUtils;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.UserInfoEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.SessionMapper;
import com.datasophon.dao.entity.SessionEntity;
import com.datasophon.api.service.SessionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


@Service("sessionService")
public class SessionServiceImpl extends ServiceImpl<SessionMapper, SessionEntity> implements SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private SessionMapper sessionMapper;

    /**
     * get user session from request
     *
     * @param request request
     * @return session
     */
    @Override
    public SessionEntity getSession(HttpServletRequest request) {
        String sessionId = request.getHeader(Constants.SESSION_ID);

        if (StringUtils.isBlank(sessionId)) {
            Cookie cookie = WebUtils.getCookie(request, Constants.SESSION_ID);

            if (cookie != null) {
                sessionId = cookie.getValue();
            }
        }

        if (StringUtils.isBlank(sessionId)) {
            return null;
        }

        String ip = HttpUtils.getClientIpAddress(request);
        logger.debug("get session: {}, ip: {}", sessionId, ip);

        return sessionMapper.selectById(sessionId);
    }


    /**
     * create session
     *
     * @param user user
     * @param ip   ip
     * @return session string
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(UserInfoEntity user, String ip) {
        SessionEntity session = null;

        // logined
        List<SessionEntity> sessionList = sessionMapper.queryByUserId(user.getId());

        Date now = new Date();

        /**
         * if you have logged in and are still valid, return directly
         */
        if (CollectionUtils.isNotEmpty(sessionList)) {
            // is session list greater 1 ， delete other ，get one
            if (sessionList.size() > 1) {
                for (int i = 1; i < sessionList.size(); i++) {
                    sessionMapper.deleteById(sessionList.get(i).getId());
                }
            }
            session = sessionList.get(0);
            if (now.getTime() - session.getLastLoginTime().getTime() <= Constants.SESSION_TIME_OUT * 1000) {
                /**
                 * updateProcessInstance the latest login time
                 */
                session.setLastLoginTime(now);
                sessionMapper.updateById(session);

                return session.getId();

            } else {
                /**
                 * session expired, then delete this session first
                 */
                sessionMapper.deleteById(session.getId());
            }
        }

        // assign new session
        session = new SessionEntity();

        session.setId(UUID.randomUUID().toString());
        session.setIp(ip);
        session.setUserId(user.getId());
        session.setLastLoginTime(now);

        sessionMapper.insertSession(session);

        return session.getId();
    }

    /**
     * sign out
     * remove ip restrictions
     *
     * @param ip        no use
     * @param loginUser login user
     */
    @Override
    public void signOut(String ip, UserInfoEntity loginUser) {
        try {
            /**
             * query session by user id and ip
             */
            SessionEntity session = sessionMapper.queryByUserIdAndIp(loginUser.getId(), ip);

            //delete session
            sessionMapper.deleteById(session.getId());
        } catch (Exception e) {
            logger.warn("userId : {} , ip : {} , find more one session", loginUser.getId(), ip);
        }
    }

}
