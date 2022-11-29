package cn.datax.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class DataScopeBaseEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人所属部门
     */
    @TableField(value = "create_dept", fill = FieldFill.INSERT)
    private String createDept;
}
