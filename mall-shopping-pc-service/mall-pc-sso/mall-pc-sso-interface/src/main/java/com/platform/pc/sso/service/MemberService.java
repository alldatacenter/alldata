package com.platform.pc.sso.service;

/**
 * @author wulinhao
 */
public interface MemberService {

    /**
     * 头像上传
     * @param userId
     * @param token
     * @param imgData
     * @return
     */
    String imageUpload(Long userId,String token,String imgData);
}
