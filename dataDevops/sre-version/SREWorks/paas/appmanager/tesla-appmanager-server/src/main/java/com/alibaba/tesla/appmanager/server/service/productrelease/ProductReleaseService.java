package com.alibaba.tesla.appmanager.server.service.productrelease;

import com.alibaba.tesla.appmanager.common.enums.ProductReleaseTaskStatusEnum;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CheckProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CreateProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.ListProductReleaseTaskAppPackageTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.ListProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CheckProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CreateProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDO;
import com.alibaba.tesla.appmanager.server.service.productrelease.business.ProductReleaseBO;

import java.util.List;

/**
 * 产品发布版本服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ProductReleaseService {

    /**
     * 获取当前系统中的全部 Schduler 列表
     *
     * @return Scheduler List 对象
     */
    List<ProductReleaseSchedulerDO> listScheduler();

    /**
     * 获取当前系统中的指定 Scheduler
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     * @return Scheduler 对象
     */
    ProductReleaseSchedulerDO getScheduler(String productId, String releaseId);

    /**
     * 根据 productId 和 releaseId 获取对应产品、发布版本及相关引用的全量信息
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     * @return 全量信息
     */
    ProductReleaseBO get(String productId, String releaseId);

    /**
     * 获取指定 productId + releaseId + appId 的 launch yaml 文件内容
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     * @param appId     应用 ID
     * @return launch yaml 文件内容
     */
    String getLaunchYaml(String productId, String releaseId, String appId);

    /**
     * 创建产品发布版本任务实例
     *
     * @param request 请求内容
     * @return 任务 ID 及应用包 ID 内容
     */
    CreateProductReleaseTaskRes createProductReleaseTask(CreateProductReleaseTaskReq request);

    /**
     * 检查当前是否存在冲突的产品发布版本运行项任务
     *
     * @param request 请求内容
     * @return 检测结果
     */
    CheckProductReleaseTaskRes checkProductReleaseTask(CheckProductReleaseTaskReq request);

    /**
     * 根据过滤条件获取产品发布版本任务列表
     *
     * @param request 请求内容
     * @return 过滤出的列表
     */
    List<ProductReleaseTaskDO> listProductReleaseTask(ListProductReleaseTaskReq request);

    /**
     * 更新 ProductRelease Task
     *
     * @param task 任务
     * @return 更新成功行数
     */
    int updateProductReleaseTask(ProductReleaseTaskDO task);

    /**
     * 根据过滤条件获取产品发布版本任务关联的应用包任务列表
     *
     * @param request 请求内容
     * @return 过滤出的列表
     */
    List<ProductReleaseTaskAppPackageTaskRelDO> listProductReleaseTaskAppPackageTask(
            ListProductReleaseTaskAppPackageTaskReq request);

    /**
     * 将产品发布版本任务设置状态
     *
     * @param taskId     任务 ID
     * @param fromStatus From 状态, 可为 null
     * @param toStatus   To 状态
     */
    void markProductReleaseTaskStatus(
            String taskId, ProductReleaseTaskStatusEnum fromStatus, ProductReleaseTaskStatusEnum toStatus);
}
