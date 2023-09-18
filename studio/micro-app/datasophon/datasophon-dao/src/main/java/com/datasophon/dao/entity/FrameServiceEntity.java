package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群框架版本服务表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@TableName("t_ddh_frame_service")
@Data
public class FrameServiceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 框架id
     */
    private Integer frameId;
    /**
     * 服务名称
     */
    private String serviceName;

    private String label;
    /**
     * 服务版本
     */
    private String serviceVersion;
    /**
     * 服务描述
     */
    private String serviceDesc;

    private String packageName;

    private String dependencies;

    private String serviceJson;

    private String serviceJsonMd5;

    private String serviceConfig;

    private String frameCode;

    private String configFileJson;

    private String configFileJsonMd5;

    private String decompressPackageName;

    @TableField(exist = false)
    private Boolean installed;

    private Integer sortNum;

   

}
