package com.alibaba.tesla.appmanager.domain.dto;

import lombok.Data;

/**
 * Definition Schema DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
public class DefinitionSchemaDTO {

    /**
     * Schema 唯一标识
     */
    private String name;

    /**
     * Json Schema
     */
    private String jsonSchema;
}
