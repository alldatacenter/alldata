
package com.platform.modules.system.domain;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.platform.base.BaseEntity;
import com.platform.utils.enums.DataScopeEnum;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * 角色
 * @author AllDataDC
 * @date 2023-01-27
 */
@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class Role extends BaseEntity implements Serializable {

    @Id
    @Column(name = "role_id")
    @NotNull(groups = {Update.class})
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "ID", hidden = true)
    private Long id;

    @JSONField(serialize = false)
    @ManyToMany(mappedBy = "roles")
    @ApiModelProperty(value = "用户", hidden = true)
    private Set<User> users;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sys_roles_menus",
            joinColumns = {@JoinColumn(name = "role_id",referencedColumnName = "role_id")},
            inverseJoinColumns = {@JoinColumn(name = "menu_id",referencedColumnName = "menu_id")})
    @ApiModelProperty(value = "菜单", hidden = true)
    private Set<Menu> menus;

    @ManyToMany
    @JoinTable(name = "sys_roles_depts",
            joinColumns = {@JoinColumn(name = "role_id",referencedColumnName = "role_id")},
            inverseJoinColumns = {@JoinColumn(name = "dept_id",referencedColumnName = "dept_id")})
    @ApiModelProperty(value = "部门", hidden = true)
    private Set<Dept> depts;

    @NotBlank
    @ApiModelProperty(value = "名称", hidden = true)
    private String name;

    @ApiModelProperty(value = "数据权限，全部 、 本级 、 自定义")
    private String dataScope = DataScopeEnum.THIS_LEVEL.getValue();

    @Column(name = "level")
    @ApiModelProperty(value = "级别，数值越小，级别越大")
    private Integer level = 3;

    @ApiModelProperty(value = "描述")
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
