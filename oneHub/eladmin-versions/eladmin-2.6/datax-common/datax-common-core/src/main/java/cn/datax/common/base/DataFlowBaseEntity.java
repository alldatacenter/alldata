package cn.datax.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class DataFlowBaseEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流状态（1待提交，2已退回，3审核中，4通过，5不通过，6已撤销）
     */
    @TableField(value = "flow_status", fill = FieldFill.INSERT)
    private String flowStatus;

    /**
     * 流程实例ID
     */
    @TableField(value = "process_instance_id")
    private String processInstanceId;
}
