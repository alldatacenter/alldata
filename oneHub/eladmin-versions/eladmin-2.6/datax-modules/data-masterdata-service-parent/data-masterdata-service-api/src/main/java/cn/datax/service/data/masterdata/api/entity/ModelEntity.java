package cn.datax.service.data.masterdata.api.entity;

import cn.datax.common.base.DataFlowBaseEntity;
import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 主数据模型表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "masterdata_model", resultMap = "BaseResultMap")
public class ModelEntity extends DataFlowBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 逻辑表
     */
    private String modelLogicTable;

    /**
     * 物理表
     */
    private String modelPhysicalTable;

    /**
     * 是否建模（0否，1是）
     */
    private String isSync;

    @TableField(exist = false)
    private List<ModelColumnEntity> modelColumns;
}
