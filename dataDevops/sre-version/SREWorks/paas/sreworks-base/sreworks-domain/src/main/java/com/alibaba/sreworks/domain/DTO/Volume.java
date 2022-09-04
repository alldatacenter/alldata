package com.alibaba.sreworks.domain.DTO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Volume {

    private String name;

    private String storageClassName;

    private String storage;

    private String path;

}
