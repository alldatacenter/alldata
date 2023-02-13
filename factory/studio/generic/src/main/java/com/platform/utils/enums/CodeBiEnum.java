
package com.platform.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 验证码业务场景
 * </p>
 * @author AllDataDC
 * @date 2023-01-27
 */
@Getter
@AllArgsConstructor
public enum CodeBiEnum {

    /* 旧邮箱修改邮箱 */
    ONE(1, "旧邮箱修改邮箱"),

    /* 通过邮箱修改密码 */
    TWO(2, "通过邮箱修改密码");

    private final Integer code;
    private final String description;

    public static CodeBiEnum find(Integer code) {
        for (CodeBiEnum value : CodeBiEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

}
