package com.alibaba.tesla.appmanager.server.addon.task.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AddonInstanceTaskVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.server.addon.Addon;
import com.alibaba.tesla.appmanager.server.addon.AddonManager;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonRes;
import com.alibaba.tesla.appmanager.server.addon.utils.AddonInstanceIdGenUtil;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Addon Instance Task 执行节点
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class AddonInstanceTaskRunnerNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        AddonManager addonManager = BeanUtil.getBean(AddonManager.class);
        AddonInstanceRepository addonInstanceRepository = BeanUtil.getBean(AddonInstanceRepository.class);
        assert addonManager != null && addonInstanceRepository != null;
        String requestStr = globalVariable.getString(AddonInstanceTaskVariableKey.APPLY_REQUEST);
        if (StringUtils.isEmpty(requestStr)) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "empty APPLY_REQUEST parameter in dag variables");
        }
        ApplyAddonInstanceReq request = JSONObject.parseObject(requestStr, ApplyAddonInstanceReq.class);

        // 进行 addon 申请，高耗时操作，禁止一切事务
        Addon addon = addonManager.getAddon(ComponentTypeEnum.RESOURCE_ADDON, request.getAddonId());
        ApplyAddonRes res = addon.apply(request);
        ComponentSchema addonSchema = res.getComponentSchema();
        String signature = res.getSignature();

        // 创建 addon instance
        AddonInstanceDO addonInstance = addonInstanceRepository.getByCondition(AddonInstanceQueryCondition.builder()
                .namespaceId(request.getNamespaceId())
                .addonId(request.getAddonId())
                .addonName(request.getAddonName())
                .addonAttrs(request.getAddonAttrs())
                .build());
        if (addonInstance == null) {
            addonInstance = AddonInstanceDO.builder()
                    .addonInstanceId(AddonInstanceIdGenUtil.genInstanceId())
                    .namespaceId(request.getNamespaceId())
                    .addonId(request.getAddonId())
                    .addonVersion(DefaultConstant.AUTO_VERSION)
                    .addonName(request.getAddonName())
                    .addonAttrs(JSONObject.toJSONString(request.getAddonAttrs()))
                    .addonExt(SchemaUtil.toYamlMapStr(addonSchema))
                    .signature(signature)
                    // TODO: 没有必要单独存储 dataOutput，已经存储到 addonExt 中了，暂时存储 null，后续删除该字段
                    .dataOutput(null)
                    .build();
            addonInstanceRepository.insert(addonInstance);
        } else {
            addonInstance.setAddonExt(SchemaUtil.toYamlMapStr(addonSchema));
            addonInstance.setSignature(signature);
            addonInstanceRepository.updateByPrimaryKey(addonInstance);
        }
        log.info("addon instance has applied|addonInstaceId={}|namespaceId={}|addonId={}|" +
                        "addonName={}|addonAttrs={}|signature={}", addonInstance.getAddonInstanceId(),
                request.getNamespaceId(), request.getAddonId(), request.getAddonName(),
                JSONObject.toJSONString(request.getAddonAttrs()), signature);
        return DagInstNodeRunRet.builder().build();
    }
}
