package com.alibaba.sreworks.appcenter.server.params;

import com.alibaba.sreworks.domain.DO.AppInstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInstanceUpdateParam {

    private String description;

    public void patchAppInstance(AppInstance appInstance, String operator) {
        appInstance.setGmtModified(System.currentTimeMillis() / 1000);
        appInstance.setLastModifier(operator);
        appInstance.setDescription(description);
    }
}
