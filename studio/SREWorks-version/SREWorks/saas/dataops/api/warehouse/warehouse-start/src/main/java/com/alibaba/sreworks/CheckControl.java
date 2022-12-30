package com.alibaba.sreworks;

import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/checkpreload.htm")
public class CheckControl extends BaseController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String check() {
        return "success";
    }

}