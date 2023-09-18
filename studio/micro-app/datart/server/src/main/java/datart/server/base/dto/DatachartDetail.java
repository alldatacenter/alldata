package datart.server.base.dto;

import datart.core.entity.Datachart;
import datart.core.entity.Variable;
import datart.core.entity.View;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatachartDetail extends Datachart {

    private String parentId;

    private Double index;

    private View view;

    private List<Variable> queryVariables;

    private boolean download;

}
