package com.alibaba.tesla.productops.params;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeCloneParam {

    private String sourceNodeTypePath;

    private String targetNodeTypePath;

}
