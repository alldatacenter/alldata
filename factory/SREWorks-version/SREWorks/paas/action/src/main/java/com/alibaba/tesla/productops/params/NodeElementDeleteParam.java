package com.alibaba.tesla.productops.params;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class NodeElementDeleteParam {

    private String nodeTypePath;

    private String elementId;

}
