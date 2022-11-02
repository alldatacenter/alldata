package com.alibaba.tesla.appmanager.server.addon.res;

import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyAddonRes implements Serializable {

    /**
     * Addon 申请产出对象
     */
    private ComponentSchema componentSchema;

    /**
     * 签名
     */
    private String signature;
}
