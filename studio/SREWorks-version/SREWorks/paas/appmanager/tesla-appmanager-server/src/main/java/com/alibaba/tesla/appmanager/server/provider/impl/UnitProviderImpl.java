package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.UnitProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.UnitDTO;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppGetReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitCreateReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitQueryReq;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.UnitDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.UnitRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 单元服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class UnitProviderImpl implements UnitProvider {

    @Autowired
    private UnitRepository unitRepository;

    @Resource
    private UnitDtoConvert unitConvert;

    @Autowired
    private UnitService unitService;

    /**
     * 同步指定包到目标单元中的环境
     *
     * @param unitId       单元 ID
     * @param appPackageId 应用包 ID
     * @return
     */
    @Override
    public JSONObject syncRemote(String unitId, Long appPackageId) throws IOException, URISyntaxException {
        return unitService.syncRemote(unitId, appPackageId);
    }

    /**
     * 启动单元环境部署
     *
     * @param unitId    单元 ID
     * @param launchReq 实际部署请求
     * @return
     */
    @Override
    public JSONObject launchDeployment(String unitId, DeployAppLaunchReq launchReq)
            throws IOException, URISyntaxException {
        return unitService.launchDeployment(unitId, launchReq);
    }

    /**
     * 查询单元环境部署详情
     *
     * @param unitId 但愿 ID
     * @param getReq 查询请求
     * @return
     */
    @Override
    public JSONObject getDeployment(String unitId, DeployAppGetReq getReq) {
        return unitService.getDeployment(unitId, getReq);
    }

    /**
     * 根据条件查询 Units
     *
     * @param request 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<UnitDTO> queryByCondition(UnitQueryReq request) {
        UnitQueryCondition condition = new UnitQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<UnitDO> results = unitService.list(condition);
        return Pagination.transform(results, item -> unitConvert.to(item));
    }

    /**
     * 获取指定查询条件对应的数据 (仅允许 unitId 参数)
     *
     * @param request 请求数据
     * @return 单条记录，如果存不在则返回 null
     */
    @Override
    public UnitDTO get(UnitQueryReq request) {
        UnitQueryCondition condition = UnitQueryCondition.builder()
                .unitId(request.getUnitId())
                .build();
        return unitConvert.to(unitService.get(condition));
    }

    /**
     * 创建 Unit
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    @Override
    public UnitDTO create(UnitCreateReq request, String operator) {
        JSONObject extra = new JSONObject();
        if (request.getExtra() != null) {
            extra = request.getExtra();
        }
        UnitDO unitDO = UnitDO.builder()
                .unitId(request.getUnitId())
                .unitName(request.getUnitName())
                .proxyIp(request.getProxyIp())
                .proxyPort(request.getProxyPort())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .username(request.getUsername())
                .password(request.getPassword())
                .operatorEndpoint(request.getOperatorEndpoint())
                .extra(extra.toJSONString())
                .build();
        return unitConvert.to(unitService.create(unitDO));
    }

    /**
     * 更新指定的 Unit (仅允许 unitId 参数)
     *
     * @param request  请求更新数据
     * @param operator 操作人
     * @return 更新后的数据内容
     */
    @Override
    public UnitDTO update(UnitUpdateReq request, String operator) {
        JSONObject extra = new JSONObject();
        if (request.getExtra() != null) {
            extra = request.getExtra();
        }
        UnitDO unitDO = UnitDO.builder()
                .unitName(request.getUnitName())
                .proxyIp(request.getProxyIp())
                .proxyPort(request.getProxyPort())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .username(request.getUsername())
                .password(request.getPassword())
                .operatorEndpoint(request.getOperatorEndpoint())
                .extra(extra.toJSONString())
                .build();
        UnitQueryCondition condition = UnitQueryCondition.builder()
                .unitId(request.getUnitId())
                .build();
        return unitConvert.to(unitService.update(condition, unitDO));
    }

    /**
     * 删除指定的 Unit
     *
     * @param request  请求删除数据
     * @param operator 操作人
     */
    @Override
    public int delete(UnitDeleteReq request, String operator) {
        UnitQueryCondition condition = UnitQueryCondition.builder()
                .unitId(request.getUnitId())
                .build();
        return unitRepository.deleteByCondition(condition);
    }
}
