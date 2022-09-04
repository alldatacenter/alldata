package com.alibaba.tesla.appmanager.server.repository.domain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDO {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String userId;

    private String profile;

    private String namespaceId;

    private String stageId;

    public JSONObject getJsonByProfile() {
        if (StringUtils.isEmpty(profile)) {
            return new JSONObject();
        }

        try {
            return JSON.parseObject(profile);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}