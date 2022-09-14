package com.alibaba.tesla.appmanager.trait.repository.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TraitDO {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String className;

    private String definitionRef;

    private String label;

    private String traitDefinition;
}