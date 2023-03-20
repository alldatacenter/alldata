
package com.platform.modules.system.service.dto;

import lombok.Getter;
import lombok.Setter;
import com.platform.base.BaseDTO;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Getter
@Setter
public class RoleDto extends BaseDTO implements Serializable {

    private Long id;

    private Set<MenuDto> menus;

    private Set<DeptDto> depts;

    private String name;

    private String dataScope;

    private Integer level;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleDto roleDto = (RoleDto) o;
        return Objects.equals(id, roleDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
