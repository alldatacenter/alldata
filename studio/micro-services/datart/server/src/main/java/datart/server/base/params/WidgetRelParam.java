package datart.server.base.params;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WidgetRelParam extends BaseUpdateParam {

    private String sourceId;

    private String targetId;

    private String config;

}
