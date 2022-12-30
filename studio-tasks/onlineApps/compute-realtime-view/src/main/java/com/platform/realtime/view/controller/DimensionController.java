package com.platform.realtime.view.controller;

import com.platform.realtime.view.module.*;
import com.platform.realtime.view.module.*;
import com.platform.realtime.view.service.IAdProvinceTop3Service;
import com.platform.realtime.view.service.IAdService;
import com.platform.realtime.view.service.IAdTrendService;
import com.platform.realtime.view.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DimensionController {

    @Autowired
    private IAdService adService;

    @Autowired
    private IAdProvinceTop3Service adProvinceTop3Service;

    @Autowired
    private IAdTrendService adTrendService;

    @Autowired
    private ITaskService taskService;


    @RequestMapping(value = "/stats/getDimensions", method = RequestMethod.GET)
    @ResponseBody
    public Object getDimensions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<AdUserClickCount> adUserClickCounts = adService.findByDate(sdf.format(date));
        return Message.ok(adUserClickCounts);
    }


    @RequestMapping(value = "/stats/adProvinceTop3", method = RequestMethod.GET)
    @ResponseBody
    public Object getAdProvinceTop3(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<AdProvinceTop3> adProvinceTop3s = adProvinceTop3Service.findByDate(sdf.format(date));
        return Message.ok(adProvinceTop3s);
    }

    @RequestMapping(value = "/stats/adTrend", method = RequestMethod.GET)
    @ResponseBody
    public Object getAdTrend( HttpServletRequest request, HttpServletResponse response) throws IOException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<AdClickTrend> adProvinceTop3s = adTrendService.findByDate(sdf.format(date));
        return Message.ok(adProvinceTop3s);
    }

    @RequestMapping(value = "/stats/getTask", method = RequestMethod.GET)
    @ResponseBody
    public Object getTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<Task> tasks = taskService.findAllTasks();
        List<String> taskNameList = tasks.stream().map(x -> x.getTaskName()).collect(Collectors.toList());
        System.out.println(taskNameList);
        return Message.ok(taskNameList);
    }


    @RequestMapping(value = "/stats/pageSplitConvert", method = RequestMethod.GET)
    @ResponseBody
    public Object getAdTrend(@RequestParam("taskName") String taskName,
                             HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<Map<String, Object>> mapList = taskService.getConvertRate(taskName);
        System.out.println(mapList);
        return Message.ok(mapList);
    }


    @RequestMapping(value = "/stats/areaTop3Product", method = RequestMethod.GET)
    @ResponseBody
    public Object getAreaTop3Product(@RequestParam("taskName") String taskName,
                                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<AreaTop3Product> top3ProductList = taskService.getAreaTop3Product(taskName);
        System.out.println(top3ProductList);
        return Message.ok(top3ProductList);
    }

    @RequestMapping(value = "/stats/visitSessionAggrStat", method = RequestMethod.GET)
    @ResponseBody
    public Object getVisitSessionAggrStat(@RequestParam("taskName") String taskName,
                                          HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        SessionAggrStat sessionAggrStat = taskService.getSessionAggrStat(taskName);
        System.out.println(sessionAggrStat);
        return Message.ok(sessionAggrStat);
    }

    @RequestMapping(value = "/stats/top10Category", method = RequestMethod.GET)
    @ResponseBody
    public Object getTop10Category(@RequestParam("taskName") String taskName,
                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<Top10Category> top10Categories = taskService.getTop10Category(taskName);
        System.out.println(top10Categories);
        return Message.ok(top10Categories);
    }

    @RequestMapping(value = "/stats/top10Session", method = RequestMethod.GET)
    @ResponseBody
    public Object getTop10Session(@RequestParam("taskName") String taskName,
                                  HttpServletRequest request, HttpServletResponse response) throws IOException {
        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        List<Top10Session> top10Sessions = taskService.getTop10Session(taskName);
        System.out.println(top10Sessions);
        return Message.ok(top10Sessions
        );
    }



    @RequestMapping(value = "/stats/startMockData", method = RequestMethod.GET)
    @ResponseBody
    public Object startMockData(HttpServletRequest request, HttpServletResponse response) throws IOException {

        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        boolean bl = taskService.updateMockData("true");
        boolean b2 = taskService.runAdSparkTask();
        System.out.println(bl + "," + b2);
        return Message.ok(bl && b2);
    }


    @RequestMapping(value = "/stats/stopMockData", method = RequestMethod.GET)
    @ResponseBody
    public Object stopMockData(HttpServletRequest request, HttpServletResponse response) throws IOException {

        //result.code=200 result.data=List<{"key":"value","key2":"value2"}>
        boolean bl = taskService.updateMockData("false");
        System.out.println(bl);
        return Message.ok(bl);
    }


}
