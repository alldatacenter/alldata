package cn.datax.service.data.market.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 服务集成调用日志表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Data
@Accessors(chain = true)
@TableName("market_service_log")
public class ServiceLogEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 服务id
     */
    private String serviceId;

    /**
     * 服务名称
     */
    @TableField(exist = false)
    private String serviceName;

    /**
     * 调用者id
     */
    private String callerId;

    /**
     * 调用者ip
     */
    private String callerIp;

    /**
     * 调用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime callerDate;

    /**
     * 调用请求头
     */
    private String callerHeader;

    /**
     * 调用请求参数
     */
    private String callerParam;

    /**
     * 调用报文
     */
    private String callerSoap;

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
}
