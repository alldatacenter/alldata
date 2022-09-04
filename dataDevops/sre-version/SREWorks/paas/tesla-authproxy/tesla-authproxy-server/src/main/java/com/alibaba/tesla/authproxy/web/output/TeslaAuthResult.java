package com.alibaba.tesla.authproxy.web.output;

import lombok.Data;

/**
 * header校验返回结果
 *
 */
@Data
public class TeslaAuthResult {

    private boolean checkSuccess;

    private String empId;

}
