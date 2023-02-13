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
public class ComponentPackageTaskNextVersionReq implements Serializable {

    private static final long serialVersionUID = -4208405145801966204L;

    private String appId;

    private String componentType;

    private String componentName;
}
