package com.alibaba.tesla.appmanager.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DefinitionSchema 查询 Request
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefinitionSchemaQueryReq implements Serializable {

    /**
     * 唯一标识
     */
    private String name;
}
