package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppPackageTagDO {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Long appPackageId;

    private String tag;

    private String appId;
}