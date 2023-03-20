
package com.platform.modules.system.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
* @author AllDataDC
* @date 2023-01-27 16:32:18
*/
@Data
@NoArgsConstructor
public class JobSmallDto implements Serializable {

    private Long id;

    private String name;
}