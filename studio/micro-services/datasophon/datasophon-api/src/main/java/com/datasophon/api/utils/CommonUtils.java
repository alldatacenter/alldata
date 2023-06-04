package com.datasophon.api.utils;

import com.datasophon.common.model.HostInfo;
import com.datasophon.common.enums.InstallState;
import com.datasophon.dao.enums.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);

    private CommonUtils() {}

    public static void updateInstallState(InstallState installState, HostInfo hostInfo) {
        hostInfo.setInstallState(installState);
        hostInfo.setInstallStateCode(installState.getValue());
    }

    public static RoleType convertRoleType(String roleType) {
        if (roleType == null || "".equals(roleType.trim())) {
            logger.error("Convert role type failed, roleType is null.");
            return null;
        }
        try {
            return RoleType.valueOf(roleType.toUpperCase());
        } catch (Exception e) {
            logger.error("Unsupported role type:{}", roleType);
            return null;
        }
    }
}
