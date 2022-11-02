package com.alibaba.tesla.gateway.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * QPS查询参数结构体
 * @author tandong.td
 */
@Data
public class QpsRequest implements Serializable {

    /**
     * 路由path
     */
    @NotBlank(message = "path can't be empty")
    private String path;

    /**
     * 开始时间
     */
    @NotNull(message = "startTime can't be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "endTime can't be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

}
