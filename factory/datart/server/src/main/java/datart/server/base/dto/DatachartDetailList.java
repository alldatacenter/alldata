package datart.server.base.dto;

import datart.core.entity.Datachart;
import datart.core.entity.Variable;
import datart.core.entity.View;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DatachartDetailList {

    private List<Datachart> datacharts;

    private List<View> views;

    private Map<String, List<Variable>> viewVariables;

    private List<Variable> orgVariables;

}
