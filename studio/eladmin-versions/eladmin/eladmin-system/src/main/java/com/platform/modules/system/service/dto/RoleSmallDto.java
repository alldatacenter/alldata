
package com.platform.modules.system.service.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * @author AllDataDC
 * @date 2022-10-27
 */
@Data
public class RoleSmallDto implements Serializable {

    private Long id;

    private String name;

    private Integer level;

    private String dataScope;
}
