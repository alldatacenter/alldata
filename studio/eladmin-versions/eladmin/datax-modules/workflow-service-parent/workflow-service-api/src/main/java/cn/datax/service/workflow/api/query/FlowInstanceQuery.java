package cn.datax.service.workflow.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowInstanceQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String name;
    private String businessKey;
    private String businessCode;
    private String businessName;
}
