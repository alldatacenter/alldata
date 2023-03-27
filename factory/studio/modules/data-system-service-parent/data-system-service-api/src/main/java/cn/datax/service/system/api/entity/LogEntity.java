package cn.datax.service.system.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Data
@Accessors(chain = true)
@TableName("sys_log")
public class LogEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 请求IP
     */
    private String remoteAddr;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 方法类名
     */
    private String className;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 请求耗时
     */
    private String time;

    /**
     * 浏览器名称
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 错误类型
     */
    private String exCode;

    /**
     * 错误信息
     */
    private String exMsg;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
