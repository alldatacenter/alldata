
package com.platform.service.dto;

import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Data
public class LogErrorDTO implements Serializable {

    private Long id;

    private String username;

    private String description;

    private String method;

    private String params;

    private String browser;

    private String requestIp;

    private String address;

    private Timestamp createTime;
}