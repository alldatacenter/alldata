package datart.core.entity.poi;

import datart.core.data.provider.Column;
import lombok.Data;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class POISettings {

    private Map<Integer, ColumnSetting> columnSetting = new HashMap<>();

    private Map<Integer, List<Column>> headerRows = new HashMap<>();

    private List<CellRangeAddress> mergeCells = new ArrayList<>();
}
