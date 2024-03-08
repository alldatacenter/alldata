
package com.platform.modules.system.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.platform.base.BaseDTO;

import java.io.Serializable;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Getter
@Setter
@NoArgsConstructor
public class JobDto extends BaseDTO implements Serializable {

    private Long id;

    private Integer jobSort;

    private String name;

    private Boolean enabled;

    public JobDto(String name, Boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
}