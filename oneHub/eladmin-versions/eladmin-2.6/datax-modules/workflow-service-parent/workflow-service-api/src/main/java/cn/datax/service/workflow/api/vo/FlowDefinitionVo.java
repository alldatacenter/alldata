package cn.datax.service.workflow.api.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class FlowDefinitionVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String name;
    private String key;
    private String description;
    private Integer version;
    private String deploymentId;
    private String resourceName;
    private String diagramResourceName;
    private Integer suspensionState;
    private String tenantId;
}
