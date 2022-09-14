package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.app.TimeSeriesDataService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangjinghua
 */
@RestController
@RequestMapping("/timeSeriesData/")
public class TimeSeriesDataController extends BaseController {

    @Autowired
    private TimeSeriesDataService timeSeriesDataService;

    @RequestMapping(value = "/saveData", method = RequestMethod.POST)
    public TeslaBaseResult saveData(@RequestBody JSONObject inJson) {
        timeSeriesDataService.saveData(inJson);
        return buildSucceedResult("ok");
    }

    @RequestMapping(value = "/saveDatas", method = RequestMethod.POST)
    public TeslaBaseResult saveDatas(@RequestBody JSONArray inArray) {
        timeSeriesDataService.saveDatas(inArray);
        return buildSucceedResult("ok");
    }

    @RequestMapping(value = "/getMetric", method = RequestMethod.POST)
    public TeslaBaseResult getMetric(@RequestBody JSONObject jsonObject) throws Exception {
        return buildSucceedResult(timeSeriesDataService.getMetric(jsonObject));
    }

}
