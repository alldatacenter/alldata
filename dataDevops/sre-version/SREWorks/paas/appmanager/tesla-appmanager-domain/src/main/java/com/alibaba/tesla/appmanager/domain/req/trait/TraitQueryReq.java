package com.alibaba.tesla.appmanager.domain.req.trait;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Trait 查询 Request
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TraitQueryReq extends BaseRequest {

    private String name;

    private String className;

    private String definitionRef;
}
