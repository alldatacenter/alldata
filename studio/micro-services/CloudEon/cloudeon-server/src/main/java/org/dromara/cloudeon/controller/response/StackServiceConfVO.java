package org.dromara.cloudeon.controller.response;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class StackServiceConfVO {
    private List<ServiceConfVO> confs;
    private List<String> customFileNames;
    Map<String, List<String>> fileGroupMap = new HashMap<>();

}
