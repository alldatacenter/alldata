package com.alibaba.tesla.appmanager.definition.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefinitionSchemaDO implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String jsonSchema;

    private static final long serialVersionUID = 1L;
}