package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.AppPackageTagUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.*;
import com.alibaba.tesla.appmanager.domain.res.apppackage.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * 应用包 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface AppPackageProvider {

    /**
     * 查询应用包
     */
    Pagination<AppPackageDTO> list(AppPackageQueryReq request, String operator);

    /**
     * 查询应用包（单个）
     */
    AppPackageDTO get(AppPackageQueryReq request, String operator);

    /**
     * 直接创建 AppPackage
     */
    AppPackageDTO create(AppPackageCreateReq request, String operator);

    /**
     * 删除应用包
     */
    void delete(Long appPackageId, String operator);

    /**
     * 为指定的应用包增加 Tag
     */
    void addTag(AppPackageTagUpdateReq request);

    /**
     * 导入应用包
     *
     * @param request  导入请求
     * @param body     请求流式数据
     * @param operator 操作人
     * @return AppPackageDTO
     */
    AppPackageDTO importPackage(AppPackageImportReq request, InputStream body, String operator);

    /**
     * 根据应用包自动生成对应的 ApplicationConfiguration 配置
     *
     * @param req 生成请求参数
     * @return ApplicationConfiguration
     */
    ApplicationConfigurationGenerateRes generate(ApplicationConfigurationGenerateReq req);

    /**
     * 生成 URL
     *
     * @param appPackageId 应用包 ID
     * @param operator     操作人
     * @return URL 结果
     */
    AppPackageUrlRes generateUrl(Long appPackageId, String operator);

    /**
     * 同步当前系统中的应用包到指定的外部环境中
     */
    AppPackageSyncExternalRes syncExternal(AppPackageSyncExternalReq request, String operator)
        throws IOException, URISyntaxException;

    /**
     * 发布应用包为 custom addon
     */
    AppPackageReleaseRes releaseAsCustomAddon(AppPackageReleaseReq request);
}
