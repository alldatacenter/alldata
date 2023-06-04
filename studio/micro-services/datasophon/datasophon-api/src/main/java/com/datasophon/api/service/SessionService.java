package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.dao.entity.SessionEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import javax.servlet.http.HttpServletRequest;

/**
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-16 11:40:00
 */
public interface SessionService extends IService<SessionEntity> {
    SessionEntity getSession(HttpServletRequest request);

    String createSession(UserInfoEntity user, String ip);

    void signOut(String ip, UserInfoEntity loginUser);
}

