package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.UnitDTO;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppGetReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitCreateReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitQueryReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitUpdateReq;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 单元服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface UnitProvider {

    /**
     * 同步指定包到目标单元中的环境
     *
     * @param unitId       单元 ID
     * @param appPackageId 应用包 ID
     * @return
     */
    JSONObject syncRemote(String unitId, Long appPackageId) throws IOException, URISyntaxException;

    /**
     * 启动单元环境部署
     *
     * @param unitId    单元 ID
     * @param launchReq 实际部署请求
     * @return
     */
    JSONObject launchDeployment(String unitId, DeployAppLaunchReq launchReq)
            throws IOException, URISyntaxException;

    /**
     * 查询单元环境部署详情
     *
     * @param unitId 但愿 ID
     * @param getReq 查询请求
     * @return
     */
    JSONObject getDeployment(String unitId, DeployAppGetReq getReq);

    /**
     * 根据条件查询单元
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<UnitDTO> queryByCondition(UnitQueryReq request);

    /**
     * 获取指定查询条件对应的数据 (仅允许 unitId 参数)
     *
     * @param request 请求数据
     * @return 单条记录，如果存不在则返回 null
     */
    UnitDTO get(UnitQueryReq request);

    /**
     * 创建 Unit
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    UnitDTO create(UnitCreateReq request, String operator);

    /**
     * 更新指定的 Unit
     *
     * @param request  请求更新数据
     * @param operator 操作人
     * @return 更新后的数据内容
     */
    UnitDTO update(UnitUpdateReq request, String operator);

    /**
     * 删除指定的 Unit
     *
     * @param request  请求删除数据
     * @param operator 操作人
     */
    int delete(UnitDeleteReq request, String operator);
}
