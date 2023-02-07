
package com.platform.modules.system.domain.vo;

import lombok.Data;

/**
 * 修改密码的 Vo 类
 * @author AllDataDC
 * @date 2022-10-27 13:59:49
 */
@Data
public class UserPassVo {

    private String oldPass;

    private String newPass;
}
