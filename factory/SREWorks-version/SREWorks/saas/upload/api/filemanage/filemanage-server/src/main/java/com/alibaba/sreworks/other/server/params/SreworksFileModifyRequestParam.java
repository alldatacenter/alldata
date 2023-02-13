package com.alibaba.sreworks.other.server.params;

import lombok.Data;

@Data
public class SreworksFileModifyRequestParam {

    private Long id;

    private String alias;

    private String type;

    private String description;

}
