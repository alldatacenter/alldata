package com.alibaba.tesla.authproxy.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * TESLA服务元数据
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class ServiceMetaDO implements Serializable {

    long id;

    String category;

    String serviceCode;

    String serviceName;

    String memo;

    String owners;

    String createUser;

    Date gmtCreate;

    Date gmtModified;

    int permissionMetaCount;

    int grantPermissionCount;

    /**
     * 是否为开放服务 1-是（默认），0-否
     */
    int isOpen = 1;

}
