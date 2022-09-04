package com.alibaba.tesla.appmanager.server.service.unit;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppGetReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 单元服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface UnitService {

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
     * 通用单元 HTTP 请求转发
     * <p>
     * 要求 request 中的 URL 必须遵循 /units/{unitId}/? 的形式
     *
     * @param unitId     目标单元 ID
     * @param method     Http 方法
     * @param requestUri Request URI (目标单元)
     * @param request    Http 请求
     * @param response   Http 响应
     */
    void proxy(String unitId, String method, String requestUri,
               HttpServletRequest request, HttpServletResponse response);

    /**
     * 根据 unitId 获取指定的单元信息
     *
     * @param condition 查询条件
     * @return 单元信息 DO
     */
    UnitDO get(UnitQueryCondition condition);

    /**
     * 根据条件过滤单元信息
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<UnitDO> list(UnitQueryCondition condition);

    /**
     * 创建单元
     *
     * @param unit 单元对象
     */
    UnitDO create(UnitDO unit);

    /**
     * 更新单元
     *
     * @param unit 单元对象
     */
    UnitDO update(UnitQueryCondition condition, UnitDO unit);

    /**
     * 删除单元
     *
     * @param condition 查询条件
     */
    int delete(UnitQueryCondition condition);
}
