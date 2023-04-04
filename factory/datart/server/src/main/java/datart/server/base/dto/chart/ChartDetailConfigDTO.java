package datart.server.base.dto.chart;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartDetailConfigDTO {

    private List<ChartDataConfigDTO> datas = new ArrayList<>();

    private List<ChartStyleConfigDTO> styles = new ArrayList<>();

    private List<ChartStyleConfigDTO> settings = new ArrayList<>();
}
