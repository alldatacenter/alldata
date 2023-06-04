package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatachartCreateParam extends VizCreateParam {

    private String parentId;

    private Double index;

    private String name;

    private String viewId;

    private String config;

    private String description;

}
