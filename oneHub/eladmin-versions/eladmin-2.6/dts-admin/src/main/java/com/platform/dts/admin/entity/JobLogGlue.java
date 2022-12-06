package com.platform.dts.admin.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * xxl-job log for glue, used to track job code process
 *
 * @author AllDataDC 2022/11/19 17:57:46
 */
@Data
public class JobLogGlue {

    private int id;

    @ApiModelProperty("任务主键ID")
    private int jobId;

    @ApiModelProperty("GLUE类型\t#com.xxl.job.core.glue.GlueTypeEnum")
    private String glueType;

    @ApiModelProperty("GLUE源代码")
    private String glueSource;

    @ApiModelProperty("GLUE备注")
    private String glueRemark;

    private Date addTime;

    private Date updateTime;

}
