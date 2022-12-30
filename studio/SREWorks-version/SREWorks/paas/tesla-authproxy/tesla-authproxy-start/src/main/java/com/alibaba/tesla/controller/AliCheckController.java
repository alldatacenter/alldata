package com.alibaba.tesla.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * 公共 Controller
 */
@RestController
public class AliCheckController {

    /**
     * 处理Aone自检请求
     *
     * @return
     */
    @RequestMapping(value = "/checkpreload.htm", method = RequestMethod.GET)
    @ResponseBody
    public String welcome() {
        return "success";
    }

    /**
     * 处理Aone淘系应用状态检测请求
     * @return
     */
    @RequestMapping(value = "/status.taobao", method = RequestMethod.GET)
    @ResponseBody
    public String status() {
        return "success";
    }

    /**
     * 首页
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public String index() {
        return "success";
    }

    /**
     * 测试页面，用于开发环境
     * @return
     */
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public ModelAndView test() {
        ModelAndView welcome = new ModelAndView("test");
        return welcome;
    }

}
