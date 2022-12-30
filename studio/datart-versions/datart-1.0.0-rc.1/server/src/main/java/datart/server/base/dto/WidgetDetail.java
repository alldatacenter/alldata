package datart.server.base.dto;

import datart.core.entity.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class WidgetDetail extends Widget {

    private List<String> viewIds;

    private String datachartId;

    private List<RelWidgetWidget> relations;

}
