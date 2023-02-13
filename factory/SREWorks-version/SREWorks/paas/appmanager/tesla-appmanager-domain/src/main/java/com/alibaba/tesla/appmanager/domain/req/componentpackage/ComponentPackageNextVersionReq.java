package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageNextVersionReq implements Serializable {

    private static final long serialVersionUID = -3134923778775030529L;

    private String appId;

    private String componentType;

    private String componentName;
}
