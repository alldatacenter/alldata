package cn.datax.service.data.market.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务集成调用日志表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Data
public class ServiceLogVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    private String serviceId;
    private String serviceName;
    private String callerId;
    private String callerIp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime callerDate;
    private String callerHeader;
    private String callerParam;
    private String callerSoap;
    private Long time;
    private String msg;
}
