package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.DeployAppAttrDTO;
import com.alibaba.tesla.appmanager.domain.dto.DeployAppDTO;
import com.alibaba.tesla.appmanager.domain.dto.DeployComponentAttrDTO;
import com.alibaba.tesla.appmanager.domain.req.deploy.*;
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;

/**
 * App 部署单服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployAppProvider {

    /**
     * 根据条件过滤查询部署单的内容
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 分页的 DeployAppDTO 对象
     */
    Pagination<DeployAppDTO> list(DeployAppListReq request, String operator);

    /**
     * 查询指定部署单的状态
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployAppDTO 对象
     */
    DeployAppDTO get(DeployAppGetReq request, String operator);

    /**
     * 查询指定部署单的详细属性
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployAppAttrDTO 对象
     */
    DeployAppAttrDTO getAttr(DeployAppGetAttrReq request, String operator);

    /**
     * 查询指定 Component 部署单的详细属性
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployComponentAttrDTO 对象
     */
    DeployComponentAttrDTO getComponentAttr(DeployAppGetComponentAttrReq request, String operator);

    /**
     * 发起一次 AppPackage 层面的部署
     *
     * @param request 请求
     * @return 响应
     */
    DeployAppPackageLaunchRes launch(DeployAppLaunchReq request, String creator);

    /**
     * 发起一次 AppPackage 层面的部署
     *
     * @param request 请求
     * @return 响应
     */
    DeployAppPackageLaunchRes fastLaunch(FastDeployAppLaunchReq request, String creator);

    /**
     * 重试指定部署单
     *
     * @param request  请求
     * @param operator 操作人
     */
    void retry(DeployAppRetryReq request, String operator);

    /**
     * 终止指定部署单
     *
     * @param request  请求
     * @param operator 操作人
     */
    void terminate(DeployAppTerminateReq request, String operator);

    /**
     * 生成部署单的Yaml
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 部署单 Yaml
     */
    String buildAppConfig(DeployAppBuildAppConfigReq request, String operator);

    /**
     * 重放指定部署单，生成一个新的部署单
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 部署单 ID
     */
    DeployAppPackageLaunchRes replay(DeployAppReplayReq request, String operator);
}
