package com.alibaba.sreworks.health.domain.bo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SwModelField {
    Long id;

//    Date gmtCreate;
//
//    Date gmtModified;

    Long modelId;

    String field;

    String alias;

    String dim;

    String type;

//    Boolean buildIn;

    Boolean nullable;

//    String description;
}