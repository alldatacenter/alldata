package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DashboardCreateParam extends VizCreateParam {

    private String id;

    private String name;

    private double index;

    private String parentId;

    private String config;

}
