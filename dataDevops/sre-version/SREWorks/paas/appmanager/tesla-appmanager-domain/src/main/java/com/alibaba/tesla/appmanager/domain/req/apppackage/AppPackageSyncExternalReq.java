package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 应用包同步到外部单元请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageSyncExternalReq implements Serializable {

    private static final long serialVersionUID = 5876845954295819748L;

    /**
     * 当前系统中的应用包 ID
     */
    private Long appPackageId;

    /**
     * 目标单元唯一标识 ID
     */
    private String unitId;
}
