package com.datasophon.api.security;


import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;

import javax.servlet.http.HttpServletRequest;

public interface Authenticator {
    /**
     * Verifying legality via username and password
     *
     * @param username user name
     * @param password user password
     * @param extra    extra info
     * @return result object
     */
    Result authenticate(String username, String password, String extra);

    /**
     * Get authenticated user
     *
     * @param request http servlet request
     * @return user
     */
    UserInfoEntity getAuthUser(HttpServletRequest request);
}
