package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;

/**
 * @author tandong
 * @Description:TODO
 * @date 2019/3/21 12:16
 */
@Data
public class TeslaUserChangePwdRequest {

    String oldPassword;

    String newPassword;

    String smsCode;

}
