package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 集群框架表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@Data
@TableName("t_ddh_frame_info")
public class FrameInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 框架名称
     */
    private String frameName;
    /**
     * 框架编码
     */
    private String frameCode;
    /**
     * 框架版本
     */
    private String frameVersion;

    @TableField(exist = false)
    private List<FrameServiceEntity> frameServiceList;

}
