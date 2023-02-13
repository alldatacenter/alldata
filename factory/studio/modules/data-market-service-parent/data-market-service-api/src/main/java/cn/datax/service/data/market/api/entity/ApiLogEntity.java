package cn.datax.service.data.market.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("market_api_log")
public class ApiLogEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    /**
     * 调用api
     */
    private String apiId;
    /**
     * api名称
     */
    @TableField(exist = false)
    private String apiName;
    /**
     * 调用者id
     */
    private String callerId;
    /**
     * 调用者ip
     */
    private String callerIp;
    /**
     * 调用url
     */
    private String callerUrl;
    /**
     * 调用参数
     */
    private String callerParams;
    /**
     * 调用数据量
     */
    private Integer callerSize;
    /**
     * 调用耗时
     */
    private Long time;
    /**
     * 信息记录
     */
    private String msg;
    /**
     * 状态：0:失败，1：成功
     */
    private String status;
    /**
     * 调用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime callerDate;
}
