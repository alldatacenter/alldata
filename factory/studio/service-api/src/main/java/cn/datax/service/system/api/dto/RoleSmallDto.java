
package cn.datax.service.system.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Data
public class RoleSmallDto implements Serializable {

    private Long id;

    private String name;

    private Integer level;

    private String dataScope;
}
