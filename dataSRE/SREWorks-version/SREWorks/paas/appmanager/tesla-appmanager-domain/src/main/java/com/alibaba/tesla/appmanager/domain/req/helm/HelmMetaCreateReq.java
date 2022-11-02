package com.alibaba.tesla.appmanager.domain.req.helm;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.PackageTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

/**
 * HELM组件创建请求
 *
 * @author fangzong.lyj@alibaba-inc.com
 * @date 2021/12/27 10:46
 */
@Data
public class HelmMetaCreateReq {
    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Helm 包标识 ID
     */
    private String helmPackageId;

    /**
     * Helm 名称
     */
    private String name;

    /**
     * 包类型
     */
    private PackageTypeEnum packageType;

    /**
     * Helm 扩展信息
     */
    private JSONObject helmExt;

    /**
     * 构建 Options 信息
     */
    private String options;

    /**
     * 描述信息
     */
    private String description;

    public void checkReq() {
        if (StringUtils.isEmpty(appId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "appId 缺失");
        }

        if (StringUtils.isEmpty(helmPackageId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "helmPackageId 缺失");
        }

        if (StringUtils.isEmpty(name)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "name 缺失");
        }

        if (Objects.isNull(packageType)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "packageType 缺失");
        }

        if (CollectionUtils.isEmpty(helmExt)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "helmExt 缺失");
        } else {
            // TODO 必填KEY的检查
        }
    }
}
