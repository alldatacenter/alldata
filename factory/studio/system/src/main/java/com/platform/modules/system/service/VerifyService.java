
package com.platform.modules.system.service;

import com.platform.domain.vo.EmailVo;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
public interface VerifyService {

    /**
     * 发送验证码
     * @param email /
     * @param key /
     * @return /
     */
    EmailVo sendEmail(String email, String key);


    /**
     * 验证
     * @param code /
     * @param key /
     */
    void validated(String key, String code);
}
