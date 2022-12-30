//package com.platform.website.controller;
//
//import com.platform.website.dao.UserBehaviorMapper;
//import com.platform.website.module.DataBean;
//import com.platform.website.module.SeriesBean;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import javax.annotation.Resource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//@Controller
//public class HighChartsController {
//
//  @Resource
//  UserBehaviorMapper userBehaviorMapper;
//
//  @RequestMapping({"/hourly"})
//  @ResponseBody
//  public DataBean showLineChart1() {
//    List<SeriesBean> list = new ArrayList<SeriesBean>();
//    userBehaviorMapper.getHourlyUserStats();
//    list.add(new SeriesBean("Tokyo", "#3366cc",
//        new double[]{7.0, 6.9, 9.5, 14.5, 18.2, 21.5, 25.2, 26.5, 23.3, 18.3, 13.9, 9.6}));
//    list.add(new SeriesBean("New York", "#8BBC21",
//        new double[]{0.2, 0.8, 5.7, 11.3, 17.0, 22.0, 24.8, 24.1, 20.1, 14.1, 8.6, 2.5}));
//    list.add(new SeriesBean("London", "#ff33ff",
//        new double[]{3.9, 4.2, 5.7, 8.5, 12.9, 15.2, 15.0, 16.6, 14.2, 10.3, 6.6, 4.8}));
//
//    String[] categories = new String[]{"9 Jan '13", "8 Feb '13", "5 Mar '13", "12 Apr '13",
//        "14 May '13", "21 Jun '13", "30 Jul '13", "8 Aug '13", "5 Sep '13", "17 Oct '13",
//        "23 Nov '13", "5 Dec '13"};
//    return new DataBean("chart1-container", "LineChart Title", "Y Values (%)", "Run Dates", Arrays
//        .asList(categories), list);
//  }
//
//
//}
