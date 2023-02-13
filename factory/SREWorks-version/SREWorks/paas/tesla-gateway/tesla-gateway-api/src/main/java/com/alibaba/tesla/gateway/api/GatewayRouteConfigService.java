package com.alibaba.tesla.gateway.api;

import com.alibaba.tesla.gateway.common.exception.OperatorRouteException;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.domain.req.FunctionServerReq;
import com.alibaba.tesla.gateway.domain.req.RouteInfoQueryReq;

import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface GatewayRouteConfigService {

    /**
     * 根据appId查询
     * @param appId appId
     * @throws OperatorRouteException exception
     * @return
     */
    List<RouteInfoDTO> getByAppId(String appId) throws OperatorRouteException;

    /**
     * 根据routeId查询
     * @param routeId route id
     * @throws OperatorRouteException exception
     * @return
     */
    RouteInfoDTO getByRouteId(String routeId) throws OperatorRouteException;

    /**
     * 插入路由
     * @param routeInfoDTO route info
     * @throws OperatorRouteException exception
     * @return info
     */
    RouteInfoDTO insertRoute(RouteInfoDTO routeInfoDTO) throws OperatorRouteException;

    /**
     * 删除路由
     * @param routeId
     * @throws OperatorRouteException exception
     */
    void deleteRoute(String routeId) throws OperatorRouteException;

    /**
     * 更新路由
     * @param routeInfoDTO route info
     * @throws OperatorRouteException exception
     */
    void updateRoute(RouteInfoDTO routeInfoDTO) throws OperatorRouteException;

    /**
     * 获取存储的所有路由
     * @throws OperatorRouteException exception
     * @return list
     */
    List<RouteInfoDTO> getAll() throws OperatorRouteException;

    /**
     * 根据类型查询路由配置
     * @param queryReq {@link RouteInfoQueryReq}
     * @return
     * @throws OperatorRouteException exception
     */
    List<RouteInfoDTO> getByCondition(RouteInfoQueryReq queryReq) throws OperatorRouteException;

    /**
     * 查询函数服务
     * @param req
     * @return
     */
    List<RouteInfoDTO> listFunctionServer(FunctionServerReq req) throws OperatorRouteException;

    /**
     * 批量导入
     * @param infoDTOS
     * @throws Exception 抛异常
     */
    void batchImportRoute(List<RouteInfoDTO> infoDTOS) throws Exception;
}
