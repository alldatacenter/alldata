package com.platform.dts.core.route.strategy;

import com.platform.dts.core.biz.model.ReturnT;
import com.platform.dts.core.biz.model.TriggerParam;
import com.platform.dts.core.route.ExecutorRouter;

import java.util.List;

/**
 * Created by AllDataDC
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList){
        return new ReturnT<String>(addressList.get(0));
    }

}
