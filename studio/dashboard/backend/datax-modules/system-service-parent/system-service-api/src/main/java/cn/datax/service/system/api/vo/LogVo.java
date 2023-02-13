package cn.datax.service.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LogVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String module;
    private String title;
    private String userId;
    private String userName;
    private String remoteAddr;
    private String requestUri;
    private String className;
    private String methodName;
    private String params;
    private String time;
    private String browser;
    private String os;
    private String exCode;
    private String exMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
