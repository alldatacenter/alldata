package com.alibaba.tesla.action.params;

import lombok.Data;
import org.springframework.stereotype.Component;


/**
 * @author huibang.lhb@alibaba-inc.com
 * Created by xunyan on 03/01/2019.
 */

@Data
@Component
public class QueryParam {

    private String appCode;

    private String entityType;

    private String entityValue;

    private String actionType;

    private String elementId;

    private String status;

    private Long createTime;

    private Long startTime;

    private Long endTime;

    private String node;

    private String empld;

    private Long actionId;


}