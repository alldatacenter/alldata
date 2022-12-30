package com.elasticsearch.cloud.monitor.metric.common.rule.condition;

import java.io.Serializable;

public interface Condition extends Serializable {

    void validate() throws Exception;

}
