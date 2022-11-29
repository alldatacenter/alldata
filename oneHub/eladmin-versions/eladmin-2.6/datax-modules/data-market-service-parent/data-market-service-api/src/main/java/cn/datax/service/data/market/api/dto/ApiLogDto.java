package cn.datax.service.data.market.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApiLogDto implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String apiId;
    private String callerId;
    private String callerIp;
    private String callerUrl;
    private String callerParams;
    private Integer callerSize;
    private LocalDateTime callerDate;
    private Long time;
    private String msg;
    private String status;
}
