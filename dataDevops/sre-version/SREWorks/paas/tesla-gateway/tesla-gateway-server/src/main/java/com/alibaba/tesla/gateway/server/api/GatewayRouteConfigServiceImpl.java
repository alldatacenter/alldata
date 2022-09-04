package com.alibaba.tesla.gateway.server.api;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.api.GatewayRouteConfigService;
import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.common.exception.OperatorRouteException;
import com.alibaba.tesla.gateway.common.utils.ServerNameCheckUtil;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.domain.req.FunctionServerReq;
import com.alibaba.tesla.gateway.domain.req.RouteInfoQueryReq;
import com.alibaba.tesla.gateway.server.dtoconvert.RouteInfoDtoConvert;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoRepository;
import com.alibaba.tesla.gateway.server.util.RouteIdGenerator;
import com.alibaba.tesla.gateway.server.util.TeslaRouteOrderUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class GatewayRouteConfigServiceImpl implements GatewayRouteConfigService {

    @Autowired
    private RouteInfoDtoConvert routeInfoDtoConvert;

    @Resource
    private RouteInfoRepository routeInfoRepository;



    @Override
    public List<RouteInfoDTO> getByAppId(String appId) throws OperatorRouteException {
        Assert.notNull(appId, "appId not null");
        return getAll().stream().filter(
            routeInfoDO -> Objects.equals(routeInfoDO.getAppId(), appId)).collect(Collectors.toList());
    }

    @Override
    public RouteInfoDTO getByRouteId(String routeId) throws OperatorRouteException {
        Optional<RouteInfoDTO> routeInfoDto = getAll().stream().filter(
            routeInfoDTO -> Objects.equals(routeInfoDTO.getRouteId(), routeId))
            .findFirst();
        return routeInfoDto.orElse(null);

    }

    @Override
    public RouteInfoDTO insertRoute(RouteInfoDTO routeInfoDTO) throws OperatorRouteException {
        try {
            //todo
            validator(routeInfoDTO);
            if(StringUtils.isBlank(routeInfoDTO.getRouteId())){
                String routeId = RouteIdGenerator.gen(routeInfoDTO);
                routeInfoDTO.setRouteId(routeId);
            }
            RouteInfoDO infoDO = this.routeInfoDtoConvert.from(routeInfoDTO);
            if(routeInfoDTO.getOrder() <= 0){
                TeslaRouteOrderUtil.setDefaultOrder(infoDO);
            }

            if(StringUtils.isNotBlank(infoDO.getUrl()) && infoDO.getUrl().startsWith(ServerNameCheckUtil.SERVER_NAME_PREFIX)){
                ServerNameCheckUtil.check(infoDO.getUrl());
            }
            List<RouteInfoDO> routeInfos = this.routeInfoRepository.listAll();

            boolean exist = routeInfos.stream()
                .anyMatch(route -> Objects.equals(route.getRouteId(), infoDO.getRouteId()));
            if(exist){
                throw new OperatorRouteException("route config exist, routeId=" + infoDO.getRouteId());
            }
            infoDO.setGmtCreate(new Date());
            infoDO.setGmtModified(new Date());
            routeInfos.add(infoDO);
            boolean result = this.routeInfoRepository.saveAll(routeInfos);
            log.info("insertRoute||routeId={}||routeInfo={}||res={}", infoDO.getRouteId(), infoDO.toString(), result);
            List<RouteInfoDO> routeInfoDOS = this.routeInfoRepository.listAll();
            if (CollectionUtils.isEmpty(routeInfoDOS)) {
                throw new RuntimeException("route is empty.");
            }
            Optional<RouteInfoDO> any = routeInfoDOS.stream().filter(
                routeInfoDO -> Objects.equals(routeInfoDO.getRouteId(), routeInfoDTO.getRouteId())).findAny();
            if (!any.isPresent()) {
                throw new RuntimeException(String.format("route insert failed, routeId=%s", routeInfoDTO.getRouteId()));
            }
            return this.routeInfoDtoConvert.to(infoDO);
        } catch (Throwable e) {
            log.error("error, ", e);
            throw new OperatorRouteException(e);
        }
    }

    @Override
    public void deleteRoute(String  routeId) throws OperatorRouteException {
        try {
            Assert.notNull(routeId, "routeId not null");
            List<RouteInfoDO> routeInfoDOS = this.routeInfoRepository.listAll();

            if(CollectionUtils.isEmpty(routeInfoDOS)){
                throw new OperatorRouteException("routeId not exit, routeId=" + routeId);
            }

            Optional<RouteInfoDO> oldRoute = routeInfoDOS.stream().filter(
                routeInfo -> Objects.equals(routeId, routeInfo.getRouteId()))
                .findFirst();

            if(!oldRoute.isPresent()){
                throw new OperatorRouteException("routeId not exit, routeId=" + routeId);
            }

            log.info("insertRoute||routeId={}||routeInfo={}", oldRoute.get().getRouteId(), oldRoute.get().toString());

            List<RouteInfoDO> newRouteInfos = routeInfoDOS.stream()
                .filter(routeInfo -> !Objects.equals(routeId, routeInfo.getRouteId()))
                .collect(Collectors.toList());
            this.routeInfoRepository.saveAll(newRouteInfos);
        } catch (Exception e) {
            throw new OperatorRouteException(e);
        }

    }

    @Override
    public void updateRoute(RouteInfoDTO routeInfoDTO) throws OperatorRouteException {
        try {
            validator(routeInfoDTO);
            //todo
            //校验
            RouteInfoDO routeInfo = this.routeInfoDtoConvert.from(routeInfoDTO);

            if(routeInfo.getOrder() <= 0){
                TeslaRouteOrderUtil.setDefaultOrder(routeInfo);
            }

            List<RouteInfoDO> routeInfoDOList = this.routeInfoRepository.listAll();
            if(CollectionUtils.isEmpty(routeInfoDOList)){
                throw new OperatorRouteException("routeId not exit, routeId=" + routeInfo.getRouteId());
            }

            Optional<RouteInfoDO> oldRoute = routeInfoDOList.stream().filter(
                route -> Objects.equals(routeInfo.getRouteId(), route.getRouteId()))
                .findFirst();
            if(!oldRoute.isPresent()){
                throw new OperatorRouteException("routeId not exit, routeId=" + routeInfo.getRouteId());
            }
            log.info("updateRoute||routeId={}||oldRoute={}||newRoute={}", routeInfo.getRouteId(), oldRoute.toString(), routeInfo.toString());

            List<RouteInfoDO> newRouteInfos = routeInfoDOList.stream()
                .filter(route -> !Objects.equals(route.getRouteId(), routeInfo.getRouteId()))
                .collect(Collectors.toList());

            routeInfo.setGmtModified(new Date());
            //订正数据
            if(null == routeInfo.getGmtCreate()){
                routeInfo.setGmtCreate(new Date());
            }
            newRouteInfos.add(routeInfo);
            this.routeInfoRepository.saveAll(newRouteInfos);
        } catch (Exception e) {
            throw new OperatorRouteException(e);
        }
    }

    @Override
    public List<RouteInfoDTO> getAll() throws OperatorRouteException {
        try {
            List<RouteInfoDTO> infoDTOS = this.routeInfoDtoConvert.to(this.routeInfoRepository.listAll());
            for (RouteInfoDTO infoDTO : infoDTOS) {
                if(StringUtils.isBlank(infoDTO.getName())){
                    //todo
                    //临时方案
                    infoDTO.setName(infoDTO.getRouteId());
                }
            }
            return infoDTOS;
        } catch (Exception e) {
            throw new OperatorRouteException(e);
        }
    }

    @Override
    public List<RouteInfoDTO> getByCondition(RouteInfoQueryReq queryReq) throws OperatorRouteException {
        Assert.notNull(queryReq, "type not be null");
        return getAll().stream().filter(
            routeInfo -> {
                if(Objects.equals(queryReq.getServerType(), ServerTypeEnum.PAAS.name())){
                    return true;
                }
                return Objects.equals(queryReq.getAppId(), routeInfo.getAppId());
            })
            .filter(routeInfo -> queryReq.getServerType().equals(routeInfo.getServerType()))
            .filter(routeInfo -> {
                if(Objects.equals(queryReq.getServerType(), ServerTypeEnum.PAAS.name())){
                    return true;
                }
                return Objects.equals(queryReq.getForwardEnv(), routeInfo.getForwardEnv());
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<RouteInfoDTO> listFunctionServer(FunctionServerReq req) throws OperatorRouteException {
        return this.getAll().stream().filter(RouteInfoDTO::isEnableFunction).collect(Collectors.toList());
    }

    @Override
    public void batchImportRoute(List<RouteInfoDTO> infoDTOS) throws Exception {
        if (CollectionUtils.isEmpty(infoDTOS)){
            return;
        }

        List<RouteInfoDO> allRoutes = this.routeInfoRepository.listAll();

        for (RouteInfoDTO routeInfoDTO : infoDTOS) {
            if(StringUtils.isBlank(routeInfoDTO.getRouteId())){
                throw new IllegalArgumentException("routeId should not empty, routeName=" + routeInfoDTO.getName());
            }
            RouteInfoDO infoDO = this.routeInfoDtoConvert.from(routeInfoDTO);
            if(routeInfoDTO.getOrder() <= 0){
                TeslaRouteOrderUtil.setDefaultOrder(infoDO);
            }

            if(StringUtils.isNotBlank(infoDO.getUrl()) && infoDO.getUrl().startsWith(ServerNameCheckUtil.SERVER_NAME_PREFIX)){
                ServerNameCheckUtil.check(infoDO.getUrl());
            }

            Iterator<RouteInfoDO> iterator = allRoutes.iterator();
            while (iterator.hasNext()){
                RouteInfoDO routeInfoDO = iterator.next();
                if (Objects.equals(routeInfoDTO.getRouteId(), routeInfoDO.getRouteId())){
                    //相同的移除
                    log.info("actionName=batchImportRoute||routeId={}||step=removeRoute||routeInfo={}", routeInfoDO.getRouteId(), JSONObject.toJSONString(routeInfoDO));
                    iterator.remove();
                }
            }
            infoDO.setGmtCreate(new Date());
            infoDO.setGmtModified(new Date());
            allRoutes.add(infoDO);
            log.info("actionName=batchImportRoute||step=insertRoute||routeId={}||routeInfo={}", infoDO.getRouteId(), infoDO.toString());
        }

        this.routeInfoRepository.saveAll(allRoutes);

    }

    private void validator(RouteInfoDTO routeInfo){
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();

        Set<ConstraintViolation<RouteInfoDTO>> set = validator.validate(routeInfo);
        for (ConstraintViolation<RouteInfoDTO> constraintViolation : set) {
            throw new IllegalArgumentException(constraintViolation.getMessage());
        }
    }
}
