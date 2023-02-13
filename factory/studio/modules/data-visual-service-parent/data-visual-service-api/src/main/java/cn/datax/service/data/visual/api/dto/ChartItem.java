package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChartItem implements Serializable {

    private static final long serialVersionUID=1L;

    private String col;
    private String alias;
    /**
     * 指标聚合函数类型
     */
    private String aggregateType;
    /**
     * 指标柱状、折线图表类型
     */
    private String seriesType;
}
