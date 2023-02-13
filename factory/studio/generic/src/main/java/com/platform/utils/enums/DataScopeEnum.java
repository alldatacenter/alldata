
package com.platform.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 数据权限枚举
 * </p>
 * @author AllDataDC
 * @date 2023-01-27
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    /* 全部的数据权限 */
    ALL("全部", "全部的数据权限"),

    /* 自己部门的数据权限 */
    THIS_LEVEL("本级", "自己部门的数据权限"),

    /* 自定义的数据权限 */
    CUSTOMIZE("自定义", "自定义的数据权限");

    private final String value;
    private final String description;

    public static DataScopeEnum find(String val) {
        for (DataScopeEnum dataScopeEnum : DataScopeEnum.values()) {
            if (dataScopeEnum.getValue().equals(val)) {
                return dataScopeEnum;
            }
        }
        return null;
    }

}
