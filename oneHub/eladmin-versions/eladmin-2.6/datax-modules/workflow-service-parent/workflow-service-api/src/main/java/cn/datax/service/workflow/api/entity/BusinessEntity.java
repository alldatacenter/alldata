package cn.datax.service.workflow.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 业务流程配置表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("flow_business")
public class BusinessEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务名称
     */
    private String businessName;

    /**
     * 业务组件
     */
    private String businessComponent;

    /**
     * 业务审核用户组
     */
    private String businessAuditGroup;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 消息模板
     */
    private String businessTempalte;
}
