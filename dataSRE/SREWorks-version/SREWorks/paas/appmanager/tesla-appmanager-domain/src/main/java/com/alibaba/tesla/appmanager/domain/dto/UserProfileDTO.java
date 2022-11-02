package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/09/28.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    /**
     * 用户ID
     */
    private String userId;

    private String namespaceId;

    private String stageId;

    /**
     * 用户Profile
     */
    private JSONObject profile;
}
