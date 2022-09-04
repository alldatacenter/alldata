package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageCreateByStreamReq {

    private String appId;

    private String packageVersion;

    private String packageCreator;

    private InputStream body;

    private boolean force;

    private boolean resetVersion;
}
