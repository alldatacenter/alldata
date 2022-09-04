package com.alibaba.tesla.gateway.server.dtoconvert;

import com.alibaba.tesla.gateway.common.enums.RouteTypeEnum;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class RouteInfoDtoConvert implements BaseDtoConvert<RouteInfoDTO, RouteInfoDO> {
    @Override
    public RouteInfoDTO to(RouteInfoDO routeInfoDO) {
        return RouteInfoDTO.builder()
            .appId(routeInfoDO.getAppId())
            .authCheck(routeInfoDO.isAuthCheck())
            .authHeader(routeInfoDO.isAuthHeader())
            .authLogin(routeInfoDO.isAuthLogin())
            .description(routeInfoDO.getDescription())
            .enable(routeInfoDO.isEnable())
            .gmtCreate(routeInfoDO.getGmtCreate())
            .gmtModified(routeInfoDO.getGmtModified())
            .order(routeInfoDO.getOrder())
            .path(routeInfoDO.getPath())
            .stageId(routeInfoDO.getStageId())
            .host(routeInfoDO.getHost())
            .rateLimit(routeInfoDO.getRateLimit())
            .routeId(routeInfoDO.getRouteId())
            .routeType(RouteTypeEnum.toTypeEnum(routeInfoDO.getRouteType()))
            .serverType(routeInfoDO.getServerType())
            .timeout(routeInfoDO.getTimeout())
            .url(routeInfoDO.getUrl())
            .name(routeInfoDO.getName())
            .forwardEnv(routeInfoDO.getForwardEnv())
            .authIgnorePath(routeInfoDO.getAuthIgnorePath())
            .enableSwaggerDoc(routeInfoDO.isEnableSwaggerDoc())
            .docUri(routeInfoDO.getDocUri())
            .authRedirect(routeInfoDO.getAuthRedirect())
            .blackListConf(routeInfoDO.getBlackListConf())
            .enableFunction(routeInfoDO.isEnableFunction())
            .build();
    }

    @Override
    public RouteInfoDO from(RouteInfoDTO dto) {
        return RouteInfoDO.builder()
            .timeout(dto.getTimeout())
            .serverType(dto.getServerType())
            .routeId(dto.getRouteId())
            .routeType(RouteTypeEnum.toType(dto.getRouteType()))
            .host(dto.getHost())
            .rateLimit(dto.getRateLimit())
            .path(dto.getPath())
            .stageId(dto.getStageId())
            .order(dto.getOrder())
            .gmtModified(dto.getGmtModified())
            .gmtCreate(dto.getGmtCreate())
            .enable(dto.isEnable())
            .description(dto.getDescription())
            .authLogin(dto.isAuthLogin())
            .authHeader(dto.isAuthHeader())
            .authCheck(dto.isAuthCheck())
            .appId(dto.getAppId())
            .url(dto.getUrl())
            .name(dto.getName())
            .forwardEnv(dto.getForwardEnv())
            .authIgnorePath(dto.getAuthIgnorePath())
            .enableSwaggerDoc(dto.isEnableSwaggerDoc())
            .docUri(dto.getDocUri())
            .authRedirect(dto.getAuthRedirect())
            .blackListConf(dto.getBlackListConf())
            .enableFunction(dto.isEnableFunction())
            .build();
    }

}
