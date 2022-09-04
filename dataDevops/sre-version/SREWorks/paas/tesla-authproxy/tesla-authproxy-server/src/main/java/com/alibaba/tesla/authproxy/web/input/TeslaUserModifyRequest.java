package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author tandong
 * @Description:TODO
 * @date 2019/3/21 12:07
 */
@Data
public class TeslaUserModifyRequest {

    @NotEmpty(message = "loginName can't be empty")
    String loginName;

    @NotEmpty(message = "password can't be empty")
    String password;

    String email;

    String nickName;

    String phone;

    String avatar;
}
