package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-11 10:18:18
 */
@TableName("t_ddh_install_step")
@Data
public class InstallStepEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer id;
    /**
     *
     */
    private String stepName;
    /**
     *
     */
    private String stepDesc;

}
