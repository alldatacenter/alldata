package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 请求总次数查询参数
 * @author tandong.td
 */
@Data
public class CountRequest implements Serializable {

    public static final String DIMENSION_MIN = "min";
    public static final String DIMENSION_HOUR = "hour";
    public static final String DIMENSION_DAY = "day";
    public static final String DIMENSION_WEEK = "week";
    public static final String DIMENSION_MONTH = "month";

    /**
     * 请求path
     */
    @NotBlank(message = "path can't be empty")
    private String path;

    /**
     * 时间维度
     * min - 分钟
     * hour - 小时
     * day - 天
     * week - 周
     * month - 月
     */
    @NotBlank(message = "dimension can't be empty")
    //@StringValue(values = {"min", "hour", "day", "week", "month"}, message = "Valid values for the dimension are min, hour, day, week or month")
    private String dimension;
}
