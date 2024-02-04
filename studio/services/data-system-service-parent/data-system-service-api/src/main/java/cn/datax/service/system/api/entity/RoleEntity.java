package cn.datax.service.system.api.entity;

import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "sys_role", autoResultMap = true)
public class RoleEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）
     */
    private String dataScope;

    @TableField(exist = false)
    private List<MenuEntity> menus;

    @TableField(exist = false)
    private List<DeptEntity> depts;

}
