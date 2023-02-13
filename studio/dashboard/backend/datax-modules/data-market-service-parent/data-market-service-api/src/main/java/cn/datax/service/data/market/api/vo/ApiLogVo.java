package cn.datax.service.data.market.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApiLogVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String apiId;
    private String apiName;
    private String callerId;
    private String callerIp;
    private String callerUrl;
    private Integer callerSize;
    private String callerParams;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime callerDate;
    private Long time;
    private String msg;
    private String status;
}
