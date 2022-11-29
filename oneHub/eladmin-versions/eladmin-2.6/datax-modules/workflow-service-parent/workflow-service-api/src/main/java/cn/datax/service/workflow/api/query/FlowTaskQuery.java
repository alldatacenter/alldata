package cn.datax.service.workflow.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTaskQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String taskId;
    private String userId;
    private List<String> groupIds;
    private String name;
    private String businessKey;
    private String businessCode;
    private String businessName;
    private String taskDefinitionKey;
    private String processInstanceId;
}
