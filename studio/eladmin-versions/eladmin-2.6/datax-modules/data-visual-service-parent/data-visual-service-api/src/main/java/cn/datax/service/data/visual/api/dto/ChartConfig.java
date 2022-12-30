package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class ChartConfig implements Serializable {

    private static final long serialVersionUID=1L;

    @NotBlank(message = "数据集不能为空")
    private String dataSetId;
    @NotBlank(message = "图表类型不能为空")
    private String chartType;
    private List<ChartItem> rows;
    private List<ChartItem> columns;
    @NotEmpty(message = "指标不能为空")
    private List<ChartItem> measures;
}
