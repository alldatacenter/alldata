package com.alibaba.tesla.appmanager.dynamicscript.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroovyHandlerItem {

    private String kind;

    private String name;

    private GroovyHandler groovyHandler;
}
