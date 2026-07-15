package cn.datax.service.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志信息表 实体VO
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Data
public class LoginLogVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String opOs;
    private String opBrowser;
    private String opIp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime opDate;
    private String userId;
    private String userName;
}
