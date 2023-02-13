package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatachartUpdateParam extends VizUpdateParam {

    private String name;

    private String parentId;

    private Double index;

    private String viewId;

    private String config;

    private String description;

}