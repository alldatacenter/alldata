
package com.platform.modules.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * @author AllDataDC
 * @date 2022-10-27
 */
@Data
@AllArgsConstructor
public class MenuMetaVo implements Serializable {

    private String title;

    private String icon;

    private Boolean noCache;
}
