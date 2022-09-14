package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.RtAppInstanceDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionConstant;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 实时应用实例 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class RtAppInstanceDtoConvert extends BaseDtoConvert<RtAppInstanceDTO, RtAppInstanceDO> {

    public RtAppInstanceDtoConvert() {
        super(RtAppInstanceDTO.class, RtAppInstanceDO.class);
    }

    @Override
    public RtAppInstanceDTO to(RtAppInstanceDO appInstance) {
        if (appInstance == null) {
            return null;
        }

        // 获取缓存中的 name/description
        AppOptionService appOptionService = SpringBeanUtil.getBean(AppOptionService.class);
        String appId = appInstance.getAppId();
        List<AppOptionDO> options = appOptionService.getCachedOptions(appId);
        String appName = appId;
        String appDescription = appId;
        String appCategory = "";
        String appLogoImg = "";
        String appNameCn = "";
        String appNavLink = "";
        for (AppOptionDO option : options) {
            if (AppOptionConstant.APP_NAME_KEY.equals(option.getKey())) {
                appName = option.getValue();
            } else if (AppOptionConstant.APP_DESCRIPTION_KEY.equals(option.getKey())) {
                appDescription = option.getValue();
            } else if (AppOptionConstant.APP_CATEGORY.equals(option.getKey())) {
                appCategory = option.getValue();
            } else if (AppOptionConstant.APP_LOGO_IMG.equals(option.getKey())) {
                appLogoImg = option.getValue();
            } else if (AppOptionConstant.APP_NAME_CN.equals(option.getKey())) {
                appNameCn = option.getValue();
            } else if (AppOptionConstant.APP_NAV_LINK.equals(option.getKey())) {
                appNavLink = option.getValue();
            }
        }
        String version = appInstance.getVersion();
        String latestVersion = appInstance.getLatestVersion();
        return RtAppInstanceDTO.builder()
                .appInstanceId(appInstance.getAppInstanceId())
                .appInstanceName(appInstance.getAppInstanceName())
                .appId(appInstance.getAppId())
                .clusterId(appInstance.getClusterId())
                .namespaceId(appInstance.getNamespaceId())
                .stageId(appInstance.getStageId())
                .version(version)
                .simpleVersion(VersionUtil.clear(version))
                .status(appInstance.getStatus())
                .visit(appInstance.getVisit())
                .upgrade(appInstance.getUpgrade())
                .latestVersion(latestVersion)
                .latestSimpleVersion(StringUtils.isNotEmpty(latestVersion) ? VersionUtil.clear(latestVersion) : "")
                .options(RtAppInstanceDTO.Options.builder()
                        .name(appName)
                        .description(appDescription)
                        .category(appCategory)
                        .logoImg(appLogoImg)
                        .nameCn(appNameCn)
                        .navLink(appNavLink)
                        .build())
                .gmtCreate(appInstance.getGmtCreate())
                .gmtModified(appInstance.getGmtModified())
                .build();
    }
}
