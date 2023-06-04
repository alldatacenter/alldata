package datart.server.base.dto.chart;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartStyleConfigDTO {

    private String key;

    private String label;

    private Object value;

    private List<ChartStyleConfigDTO> rows = new ArrayList<>();

}
