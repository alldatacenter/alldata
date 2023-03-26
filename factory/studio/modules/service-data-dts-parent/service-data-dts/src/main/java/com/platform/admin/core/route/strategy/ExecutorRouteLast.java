package com.platform.admin.core.route.strategy;

import com.platform.core.biz.model.ReturnT;
import com.platform.core.biz.model.TriggerParam;
import com.platform.admin.core.route.ExecutorRouter;

import java.util.List;

public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<String>(addressList.get(addressList.size()-1));
    }

}
