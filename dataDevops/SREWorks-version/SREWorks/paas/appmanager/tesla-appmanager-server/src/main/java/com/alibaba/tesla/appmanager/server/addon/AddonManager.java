package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.AddonUtil;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonSchema;
import com.alibaba.tesla.appmanager.server.event.loader.AddonLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.AddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDO;
import com.github.pagehelper.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addon 管理器
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
@Slf4j
public class AddonManager implements ApplicationListener<AddonLoadedEvent> {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AddonMetaRepository addonMetaRepository;

    @Autowired
    private CustomAddonMetaRepository customAddonMetaRepository;

    private final Map<String, Addon> addonMap = new ConcurrentHashMap<>();

    /**
     * 获取 Addon 对象
     *
     * @param componentType 组件类型
     * @param addonId Addon ID
     * @return Addon 对象，如果不存在则抛出异常
     */
    public Addon getAddon(ComponentTypeEnum componentType, String addonId) {
        Addon addon = this.addonMap.get(AddonUtil.combineAddonKey(componentType, addonId));
        if (addon == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find addon by componentType %s and addonId %s", componentType, addonId));
        }
        return addon;
    }

    public CustomAddonSchema getCustomAddonSchema(String addonId, String addonVersion) {
        AddonMetaQueryCondition addonMetaQueryCondition = AddonMetaQueryCondition.builder()
                .addonId(addonId)
                .addonVersion(addonVersion)
                .build();
        Page<CustomAddonMetaDO> metaDOPage = customAddonMetaRepository.selectByCondition(
                addonMetaQueryCondition);
        if (CollectionUtils.isEmpty(metaDOPage)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("actionName=getCustomAddonSchema|addonId=%s|addonVersion=%s metaDOPage is NULL!!!", addonId, addonVersion));
        }
        CustomAddonMetaDO customAddonMetaDO = metaDOPage.get(0);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .objectName("customAddonMetaDO")
                .checkObject(customAddonMetaDO)
                .actionName("getCustomAddonSchema")
                .build());
        return SchemaUtil.toSchema(CustomAddonSchema.class, customAddonMetaDO.getAddonSchema());
    }

    /**
     * 注册 Addon
     *
     * @param key   Map Key
     * @param addon 附加组件实例
     */
    private void register(String key, Addon addon) {
        addonMap.put(key, addon);

        // 更新当前插件信息到数据库中
        ComponentTypeEnum addonType = addon.getAddonType();
        String addonId = addon.getAddonId();
        AddonMetaDO meta = addonMetaRepository.get(addonType, addonId);
        if (meta == null) {
            meta = AddonMetaDO.builder()
                    .addonType(addonType.toString())
                    .addonId(addonId)
                    .addonVersion(DefaultConstant.AUTO_VERSION)
                    .addonLabel(addon.getAddonLabel())
                    .addonDescription(addon.getAddonDescription())
                    .addonSchema(SchemaUtil.toYamlMapStr(addon.getAddonSchema()))
                    .addonConfigSchema(addon.getAddonConfigSchema())
                    .build();
            addonMetaRepository.insert(meta);
            log.info("the addon {}|{} has inserted into database", addonType, addonId);
        } else {
            // addon version 已废弃
            meta.setAddonVersion(DefaultConstant.AUTO_VERSION);
            meta.setAddonType(addon.getAddonType().toString());
            meta.setAddonLabel(addon.getAddonLabel());
            meta.setAddonDescription(addon.getAddonDescription());
            meta.setAddonSchema(SchemaUtil.toYamlMapStr(addon.getAddonSchema()));
            meta.setAddonConfigSchema(addon.getAddonConfigSchema());
            addonMetaRepository.updateByPrimaryKey(meta);
            log.info("the addon {}|{} has updated its contents", addonType, addonId);
        }
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(AddonLoadedEvent event) {
        String key = event.getKey();
        Object bean;
        try {
            bean = context.getBean(event.getBeanName());
        } catch (Exception e) {
            String message = String.format("cannot get bean now, failed to load addon|beanName=%s|key=%s",
                    event.getBeanName(), event.getKey());
            log.error(message);
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, message);
        }
        register(key, (Addon) bean);
        log.info("addon {} has registered", key);
    }
}
