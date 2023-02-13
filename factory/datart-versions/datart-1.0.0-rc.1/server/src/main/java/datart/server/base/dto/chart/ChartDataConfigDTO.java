package datart.server.base.dto.chart;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartDataConfigDTO {

    private String label;

    private String key;

    private Boolean required;

    private String type;

    private List<ChartColumn> rows = new ArrayList<>();
}
