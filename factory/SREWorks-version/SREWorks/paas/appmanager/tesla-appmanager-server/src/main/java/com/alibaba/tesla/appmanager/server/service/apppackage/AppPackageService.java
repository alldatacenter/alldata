package com.alibaba.tesla.appmanager.server.service.apppackage;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageVersionCountDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageCreateByStreamReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageReleaseReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageVersionCountReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ApplicationConfigurationGenerateReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.ApplicationConfigurationGenerateRes;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;

import java.util.List;

/**
 * 内部 - 应用包服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface AppPackageService {

    /**
     * 根据条件过滤应用包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<AppPackageDO> list(AppPackageQueryCondition condition);

    /**
     * 根据条件获取单个应用包
     *
     * @param condition 过滤条件
     * @return AppPackageDO
     */
    AppPackageDO get(AppPackageQueryCondition condition);

    /**
     * 统计指定 appId 列表对应的指定 tag 的 package 计数
     *
     * @param req 查询请求
     * @return 查询结果
     */
    List<AppPackageVersionCountDTO> countVersion(AppPackageVersionCountReq req);

    /**
     * 通过 Stream 创建一个应用包
     *
     * @param req 请求内容
     */
    AppPackageDO createByStream(AppPackageCreateByStreamReq req);

    /**
     * 根据应用包自动生成对应的 ApplicationConfiguration 配置
     *
     * @param req 生成请求参数
     * @return ApplicationConfiguration
     */
    ApplicationConfigurationGenerateRes generate(ApplicationConfigurationGenerateReq req);

    /**
     * 将 AppPackage 发布为一个 custom addon
     *
     * @param req
     * @return
     */
    Long releaseCustomAddonMeta(AppPackageReleaseReq req);

    /**
     * 根据条件删除包应用
     *
     * @param condition 查询条件
     */
    int delete(AppPackageQueryCondition condition);

    /**
     * 应用包版本号处理，允许传入 null 进行自动增加
     *
     * @param appId 应用 ID
     * @return 处理后的版本号
     */
    String getNextVersion(String appId);
}
