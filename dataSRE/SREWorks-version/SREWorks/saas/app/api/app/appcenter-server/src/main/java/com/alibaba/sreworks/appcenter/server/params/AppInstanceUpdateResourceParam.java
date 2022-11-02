package com.alibaba.sreworks.appcenter.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DTO.AppInstanceDetail;
import com.alibaba.sreworks.domain.DTO.Resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInstanceUpdateResourceParam {

    private Resource resource;

    public void patchAppInstance(AppInstance appInstance, String operator) {
        appInstance.setGmtModified(System.currentTimeMillis() / 1000);
        appInstance.setLastModifier(operator);
        AppInstanceDetail detail = appInstance.detail();
        detail.setResource(resource);
        appInstance.setDetail(JSONObject.toJSONString(detail));
    }

}
