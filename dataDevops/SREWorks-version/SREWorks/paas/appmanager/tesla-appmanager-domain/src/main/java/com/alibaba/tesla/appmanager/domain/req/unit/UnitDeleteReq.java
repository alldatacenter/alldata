package com.alibaba.tesla.appmanager.domain.req.unit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 单元删除请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitDeleteReq implements Serializable {

    private static final long serialVersionUID = -4971559531453545837L;

    /**
     * 单元唯一标识
     */
    private String unitId;
}
