package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警组表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@TableName("t_ddh_alert_group")
@Data
public class AlertGroupEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 告警组名称
     */
    private String alertGroupName;
    /**
     * 告警组类别
     */
    private String alertGroupCategory;

    private Date createTime;

    @TableField(exist = false)
    private Integer alertQuotaNum;

    @TableField(exist = false)
    private Integer clusterId;

}
