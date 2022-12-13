
package com.platform.service.dto;

import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author AllDataDC
 * @date 2022-10-27
 */
@Data
public class LogSmallDTO implements Serializable {

    private String description;

    private String requestIp;

    private Long time;

    private String address;

    private String browser;

    private Timestamp createTime;
}
