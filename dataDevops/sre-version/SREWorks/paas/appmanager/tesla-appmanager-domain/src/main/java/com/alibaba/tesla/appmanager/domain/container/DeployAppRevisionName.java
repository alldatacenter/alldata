package com.alibaba.tesla.appmanager.domain.container;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.google.common.base.Enums;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 部署 App - RevisionName 解析器
 * <p>
 * MICROSERVICE|testservice|1.0.1+20200810000000
 * CONFIG|productops|1.0.1+20200810000000
 * RESOURCE_ADDON|mysql@mysql-A|5.7
 * TRAIT_ADDON|microservice@testservice@stage.flyadmin.alibaba.com|1.0.0
 * CUSTOM_ADDON|mongodb@mongodb-A|5.1
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class DeployAppRevisionName {

    private static final String SPLITTER = "\\|";
    private static final Integer SPLIT_LIMIT = 3;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 组件版本
     */
    private String version;

    /**
     * 是否是镜像 revision
     */
    private boolean mirrorFlag;

    /**
     * 传入 revisionName 进行解析分解
     */
    private DeployAppRevisionName(String revisionName, boolean mirrorFlag) {
        String[] splitNames = revisionName.split(SPLITTER, SPLIT_LIMIT);
        assert splitNames.length == SPLIT_LIMIT;
        this.componentType = Enums.getIfPresent(ComponentTypeEnum.class, splitNames[0]).orNull();
        if (this.componentType == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("cannot parse revisionName %s", revisionName));
        }
        this.componentName = splitNames[1];
        this.version = splitNames[2];
        this.mirrorFlag = mirrorFlag;
    }

    /**
     * 根据 revisionName 生成一个 Container 容器对象
     *
     * @param revisionName 修订名称
     * @return DeployAppRevisionContainer
     */
    public static DeployAppRevisionName valueOf(String revisionName) {
        if (revisionName.startsWith(DefaultConstant.MIRROR_COMPONENT_PREFIX)) {
            return new DeployAppRevisionName(
                revisionName.substring(DefaultConstant.MIRROR_COMPONENT_PREFIX.length()), true);
        } else {
            return new DeployAppRevisionName(revisionName, false);
        }
    }

    /**
     * 输出当前的 revisionName
     *
     * @returne revisionName
     */
    public String revisionName() {
        return String.join("|", Arrays.asList(componentType.toString(), componentName, version));
    }

    /**
     * 判断当前是否为空版本号（触发自动新建 Component Package 流程）
     *
     * @return true or false
     */
    public boolean isEmptyVersion() {
        return "_".equals(version);
    }

    /**
     * 如果当前是 Addon 类型，那么获取当前的 addon id (componentName 中 @ 前面的内容)
     *
     * @return addonId
     */
    public String addonId() {
        assert componentType.equals(ComponentTypeEnum.RESOURCE_ADDON)
            || componentType.equals(ComponentTypeEnum.TRAIT_ADDON)
            || componentType.equals(ComponentTypeEnum.CUSTOM_ADDON);
        String[] array = componentName.split("@", 2);
        assert array.length == 2;
        return array[0];
    }

    /**
     * 如果当前是 Addon 类型，那么获取当前的 addon name (componentName 中 @ 后面的内容)
     *
     * @return addonName
     */
    public String addonName() {
        assert componentType.equals(ComponentTypeEnum.RESOURCE_ADDON)
            || componentType.equals(ComponentTypeEnum.TRAIT_ADDON)
            || componentType.equals(ComponentTypeEnum.CUSTOM_ADDON);
        String[] array = componentName.split("@", 2);
        assert array.length == 2;
        return array[1];
    }
}
