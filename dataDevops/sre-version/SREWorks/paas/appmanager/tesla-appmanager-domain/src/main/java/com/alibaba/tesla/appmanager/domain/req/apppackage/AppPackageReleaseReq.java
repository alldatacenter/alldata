package com.alibaba.tesla.appmanager.domain.req.apppackage;

import java.io.Serializable;
import java.util.List;

import com.alibaba.tesla.appmanager.domain.schema.CustomAddonWorkloadSpec.Parameter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName:AppPackageReleaseReq
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-11-11
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageReleaseReq implements Serializable{
    private static final long serialVersionUID = -7584200387030685472L;
    /**
     * AppPackage主键
     */
    private Long appPackageId;

    private String addonId;

    private String addonVersion;

    private List<Parameter> parameterValues;
}
