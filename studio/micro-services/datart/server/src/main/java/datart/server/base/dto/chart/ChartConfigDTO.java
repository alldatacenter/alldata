package datart.server.base.dto.chart;

import lombok.Data;

@Data
public class ChartConfigDTO {

    private ChartDetailConfigDTO chartConfig = new ChartDetailConfigDTO();

    private String chartGraphId;

    private Boolean aggregation;

}
