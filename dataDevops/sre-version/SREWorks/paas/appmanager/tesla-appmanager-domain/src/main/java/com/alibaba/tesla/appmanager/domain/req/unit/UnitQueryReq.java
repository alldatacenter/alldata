package com.alibaba.tesla.appmanager.domain.req.unit;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 单元查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UnitQueryReq extends BaseRequest {

    /**
     * 单元唯一标识
     */
    private String unitId;

    /**
     * 单元名称
     */
    private String unitName;

    /**
     * 类型
     */
    private String category;
}
