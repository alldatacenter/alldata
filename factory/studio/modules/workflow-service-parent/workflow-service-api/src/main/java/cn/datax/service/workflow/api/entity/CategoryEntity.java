package cn.datax.service.workflow.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 流程分类表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("flow_category")
public class CategoryEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 分类名称
     */
    private String name;
}
